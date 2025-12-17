"""
History Loader - Python implementation of HISTLD00.cbl

This module implements the history loading logic from the COBOL
program HISTLD00, which loads transaction and position history
from VSAM files into DB2 tables for reporting and analysis.

Original COBOL Program: src/programs/batch/HISTLD00.cbl

Key Functions:
- Read position and transaction records from operational files
- Transform records for historical storage
- Load records into history tables (DB2/PostgreSQL)
- Maintain data integrity with checkpoint/restart
- Generate load statistics and reports

Return Codes:
- 0: All records loaded successfully
- 4: Some records had warnings
- 8: Some records had errors
- 12: Critical error, processing aborted
"""

import json
import logging
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from pathlib import Path
from typing import List, Optional, Dict, Generator, Any

from sqlalchemy import text
from sqlalchemy.orm import Session

from migration.python.models.transaction import TransactionRecord
from migration.python.models.position import PositionRecord
from migration.python.models.history import (
    HistoryRecordType,
    HistoryActionCode,
    HistoryRecord,
)
from migration.python.database.orm_models import (
    PortfolioMaster,
    TransactionHistory,
    PositionHistory,
)
from migration.python.database.session import DatabaseManager

# Configure logging
logger = logging.getLogger(__name__)


@dataclass
class LoadStatistics:
    """
    Statistics collected during history loading.
    
    Corresponds to the counters maintained in HISTLD00's WORKING-STORAGE:
    - WS-RECORDS-READ
    - WS-RECORDS-LOADED
    - WS-RECORDS-REJECTED
    - WS-RECORDS-DUPLICATE
    """
    positions_read: int = 0
    positions_loaded: int = 0
    positions_rejected: int = 0
    positions_duplicate: int = 0
    transactions_read: int = 0
    transactions_loaded: int = 0
    transactions_rejected: int = 0
    transactions_duplicate: int = 0
    checkpoints_taken: int = 0
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    last_checkpoint_time: Optional[datetime] = None
    last_checkpoint_key: Optional[str] = None
    
    @property
    def total_read(self) -> int:
        """Total records read."""
        return self.positions_read + self.transactions_read
    
    @property
    def total_loaded(self) -> int:
        """Total records loaded."""
        return self.positions_loaded + self.transactions_loaded
    
    @property
    def total_rejected(self) -> int:
        """Total records rejected."""
        return self.positions_rejected + self.transactions_rejected
    
    @property
    def elapsed_seconds(self) -> Optional[float]:
        """Elapsed processing time in seconds."""
        if self.start_time is None or self.end_time is None:
            return None
        return (self.end_time - self.start_time).total_seconds()


@dataclass
class HistoryLoaderConfig:
    """
    Configuration for history loading.
    """
    # Processing options
    load_positions: bool = True
    load_transactions: bool = True
    skip_duplicates: bool = True
    
    # Checkpoint settings
    checkpoint_interval: int = 1000  # Records between checkpoints
    enable_checkpoints: bool = True
    
    # Batch settings
    batch_size: int = 100  # Records per batch insert
    
    # Date range filter
    start_date: Optional[str] = None  # YYYYMMDD
    end_date: Optional[str] = None    # YYYYMMDD
    
    # Processing user
    process_user: str = 'HISTLD'
    batch_id: Optional[str] = None


class HistoryLoader:
    """
    History Loader - Python implementation of HISTLD00.cbl
    
    This class loads transaction and position history from operational
    storage into historical tables for reporting and analysis.
    
    The loading process follows the same flow as HISTLD00:
    1. Initialize processing (open files/connections, set counters)
    2. Load position history records
    3. Load transaction history records
    4. Take periodic checkpoints for restart capability
    5. Generate load report
    6. Terminate processing (close files, return code)
    
    Usage:
        loader = HistoryLoader(db_manager)
        loader.load_history(process_date='20241215')
        
        # Or load from specific date range
        loader.load_history(start_date='20241201', end_date='20241215')
        
        # Check statistics
        stats = loader.get_statistics()
        print(f"Loaded {stats.total_loaded} records")
    """
    
    def __init__(self, db_manager: DatabaseManager,
                 config: Optional[HistoryLoaderConfig] = None):
        """
        Initialize the history loader.
        
        Args:
            db_manager: Database manager for database operations
            config: History loader configuration
        """
        self.db_manager = db_manager
        self.config = config or HistoryLoaderConfig()
        self.statistics = LoadStatistics()
        self._checkpoint_data: Dict[str, Any] = {}
    
    def load_history(self, process_date: Optional[str] = None,
                     start_date: Optional[str] = None,
                     end_date: Optional[str] = None,
                     portfolio_id: Optional[str] = None) -> LoadStatistics:
        """
        Load history records from operational tables to history tables.
        
        This method implements the main processing loop from HISTLD00:
        - 1000-PROCESS-INITIALIZE
        - 2000-LOAD-POSITION-HISTORY
        - 3000-LOAD-TRANSACTION-HISTORY
        - 4000-PROCESS-TERMINATE
        
        Args:
            process_date: Single date to process (YYYYMMDD)
            start_date: Start of date range (YYYYMMDD)
            end_date: End of date range (YYYYMMDD)
            portfolio_id: Filter by portfolio ID
            
        Returns:
            LoadStatistics with processing results
        """
        # Use config dates if not provided
        if start_date is None:
            start_date = self.config.start_date
        if end_date is None:
            end_date = self.config.end_date
        
        # If process_date provided, use it as both start and end
        if process_date:
            start_date = process_date
            end_date = process_date
        
        # Default to today if no dates specified
        if start_date is None:
            start_date = datetime.now().strftime("%Y%m%d")
        if end_date is None:
            end_date = start_date
        
        logger.info(f"Starting history load for date range {start_date} to {end_date}")
        self._initialize_processing()
        
        try:
            # 2000-LOAD-POSITION-HISTORY
            if self.config.load_positions:
                self._load_position_history(start_date, end_date, portfolio_id)
            
            # 3000-LOAD-TRANSACTION-HISTORY
            if self.config.load_transactions:
                self._load_transaction_history(start_date, end_date, portfolio_id)
            
        except Exception as e:
            logger.error(f"Critical error during history load: {e}")
            raise
        finally:
            self._terminate_processing()
        
        return self.statistics
    
    def load_from_file(self, positions_file: Optional[str] = None,
                       transactions_file: Optional[str] = None) -> LoadStatistics:
        """
        Load history records from JSON files.
        
        Args:
            positions_file: Path to JSON file with position records
            transactions_file: Path to JSON file with transaction records
            
        Returns:
            LoadStatistics with processing results
        """
        logger.info("Starting history load from files")
        self._initialize_processing()
        
        try:
            if positions_file and self.config.load_positions:
                self._load_positions_from_file(positions_file)
            
            if transactions_file and self.config.load_transactions:
                self._load_transactions_from_file(transactions_file)
            
        except Exception as e:
            logger.error(f"Critical error during file load: {e}")
            raise
        finally:
            self._terminate_processing()
        
        return self.statistics
    
    def _initialize_processing(self):
        """
        Initialize processing - corresponds to 1000-PROCESS-INITIALIZE.
        """
        self.statistics = LoadStatistics()
        self.statistics.start_time = datetime.now()
        self._checkpoint_data.clear()
        
        # Generate batch ID if not provided
        if self.config.batch_id is None:
            self.config.batch_id = f"HIST{datetime.now().strftime('%Y%m%d%H%M%S')}"
        
        logger.info(f"History load initialized with batch ID: {self.config.batch_id}")
    
    def _terminate_processing(self):
        """
        Terminate processing - corresponds to 4000-PROCESS-TERMINATE.
        """
        self.statistics.end_time = datetime.now()
        
        logger.info(
            f"History load complete: "
            f"Positions={self.statistics.positions_loaded}/{self.statistics.positions_read}, "
            f"Transactions={self.statistics.transactions_loaded}/{self.statistics.transactions_read}, "
            f"Checkpoints={self.statistics.checkpoints_taken}"
        )
    
    def _load_position_history(self, start_date: str, end_date: str,
                                portfolio_id: Optional[str] = None):
        """
        Load position history records - corresponds to 2000-LOAD-POSITION-HISTORY.
        
        Reads position records from PortfolioMaster and creates
        historical snapshots in PositionHistory.
        
        Args:
            start_date: Start date (YYYYMMDD)
            end_date: End date (YYYYMMDD)
            portfolio_id: Optional portfolio filter
        """
        logger.info("Loading position history records")
        
        batch: List[PositionHistory] = []
        records_since_checkpoint = 0
        
        with self.db_manager.session_scope() as session:
            # Build query for positions
            query = session.query(PortfolioMaster).filter(
                PortfolioMaster.position_date >= start_date,
                PortfolioMaster.position_date <= end_date
            )
            
            if portfolio_id:
                query = query.filter(PortfolioMaster.portfolio_id == portfolio_id)
            
            query = query.order_by(
                PortfolioMaster.portfolio_id,
                PortfolioMaster.position_date,
                PortfolioMaster.investment_id
            )
            
            for position in query.yield_per(self.config.batch_size):
                self.statistics.positions_read += 1
                records_since_checkpoint += 1
                
                # Check for duplicate
                if self.config.skip_duplicates:
                    existing = session.query(PositionHistory).filter(
                        PositionHistory.portfolio_id == position.portfolio_id,
                        PositionHistory.position_date == position.position_date,
                        PositionHistory.investment_id == position.investment_id,
                        PositionHistory.action_code == 'A'
                    ).first()
                    
                    if existing:
                        self.statistics.positions_duplicate += 1
                        continue
                
                # Create history record
                try:
                    history_record = self._create_position_history(position)
                    batch.append(history_record)
                    
                    # Flush batch if full
                    if len(batch) >= self.config.batch_size:
                        self._flush_position_batch(session, batch)
                        batch.clear()
                    
                except Exception as e:
                    logger.error(f"Error creating position history: {e}")
                    self.statistics.positions_rejected += 1
                
                # Take checkpoint if needed
                if (self.config.enable_checkpoints and 
                    records_since_checkpoint >= self.config.checkpoint_interval):
                    self._take_checkpoint(session, 'POSITION', position.portfolio_id)
                    records_since_checkpoint = 0
            
            # Flush remaining batch
            if batch:
                self._flush_position_batch(session, batch)
            
            session.commit()
    
    def _load_transaction_history(self, start_date: str, end_date: str,
                                   portfolio_id: Optional[str] = None):
        """
        Load transaction history records - corresponds to 3000-LOAD-TRANSACTION-HISTORY.
        
        Reads transaction records from TransactionHistory (operational)
        and creates historical records for reporting.
        
        Args:
            start_date: Start date (YYYYMMDD)
            end_date: End date (YYYYMMDD)
            portfolio_id: Optional portfolio filter
        """
        logger.info("Loading transaction history records")
        
        batch: List[PositionHistory] = []
        records_since_checkpoint = 0
        
        with self.db_manager.session_scope() as session:
            # Build query for transactions
            query = session.query(TransactionHistory).filter(
                TransactionHistory.trans_date >= start_date,
                TransactionHistory.trans_date <= end_date,
                TransactionHistory.status == 'D'  # Only completed transactions
            )
            
            if portfolio_id:
                query = query.filter(TransactionHistory.portfolio_id == portfolio_id)
            
            query = query.order_by(
                TransactionHistory.trans_date,
                TransactionHistory.trans_time,
                TransactionHistory.portfolio_id
            )
            
            for transaction in query.yield_per(self.config.batch_size):
                self.statistics.transactions_read += 1
                records_since_checkpoint += 1
                
                # Check for duplicate
                if self.config.skip_duplicates:
                    existing = session.query(PositionHistory).filter(
                        PositionHistory.portfolio_id == transaction.portfolio_id,
                        PositionHistory.position_date == transaction.trans_date,
                        PositionHistory.investment_id == transaction.investment_id,
                        PositionHistory.record_type == 'TR'
                    ).first()
                    
                    if existing:
                        self.statistics.transactions_duplicate += 1
                        continue
                
                # Create history record from transaction
                try:
                    history_record = self._create_transaction_history(transaction)
                    batch.append(history_record)
                    
                    # Flush batch if full
                    if len(batch) >= self.config.batch_size:
                        self._flush_transaction_batch(session, batch)
                        batch.clear()
                    
                except Exception as e:
                    logger.error(f"Error creating transaction history: {e}")
                    self.statistics.transactions_rejected += 1
                
                # Take checkpoint if needed
                if (self.config.enable_checkpoints and 
                    records_since_checkpoint >= self.config.checkpoint_interval):
                    self._take_checkpoint(session, 'TRANSACTION', 
                                         f"{transaction.trans_date}{transaction.trans_time}")
                    records_since_checkpoint = 0
            
            # Flush remaining batch
            if batch:
                self._flush_transaction_batch(session, batch)
            
            session.commit()
    
    def _create_position_history(self, position: PortfolioMaster) -> PositionHistory:
        """
        Create a PositionHistory record from a PortfolioMaster record.
        
        Args:
            position: Source position record
            
        Returns:
            PositionHistory record
        """
        # Calculate unrealized gain/loss
        quantity = Decimal(str(position.quantity))
        cost_basis = Decimal(str(position.cost_basis))
        market_value = Decimal(str(position.market_value))
        
        unrealized_gl = market_value - cost_basis
        unrealized_gl_pct = Decimal('0')
        if cost_basis != 0:
            unrealized_gl_pct = (unrealized_gl / cost_basis * 100).quantize(
                Decimal('0.0001'))
        
        return PositionHistory(
            portfolio_id=position.portfolio_id,
            position_date=position.position_date,
            investment_id=position.investment_id,
            quantity=float(quantity),
            cost_basis=float(cost_basis),
            market_value=float(market_value),
            currency=position.currency,
            unrealized_gain_loss=float(unrealized_gl),
            unrealized_gain_loss_pct=float(unrealized_gl_pct),
            record_type='PS',  # Position snapshot
            action_code='A',   # Add
            reason_code='SNAP',
            process_user=self.config.process_user,
            batch_id=self.config.batch_id
        )
    
    def _create_transaction_history(self, transaction: TransactionHistory) -> PositionHistory:
        """
        Create a PositionHistory record from a TransactionHistory record.
        
        Args:
            transaction: Source transaction record
            
        Returns:
            PositionHistory record
        """
        return PositionHistory(
            portfolio_id=transaction.portfolio_id,
            position_date=transaction.trans_date,
            investment_id=transaction.investment_id,
            quantity=float(transaction.quantity),
            cost_basis=float(transaction.amount),
            market_value=float(transaction.amount),
            currency=transaction.currency,
            record_type='TR',  # Transaction
            action_code='C',   # Change (transaction causes change)
            reason_code=transaction.trans_type,
            after_image=json.dumps({
                'trans_type': transaction.trans_type,
                'quantity': str(transaction.quantity),
                'price': str(transaction.price),
                'amount': str(transaction.amount)
            }),
            process_user=self.config.process_user,
            batch_id=self.config.batch_id
        )
    
    def _flush_position_batch(self, session: Session, batch: List[PositionHistory]):
        """
        Flush a batch of position history records to the database.
        
        Args:
            session: Database session
            batch: List of PositionHistory records
        """
        for record in batch:
            session.add(record)
            self.statistics.positions_loaded += 1
        
        session.flush()
        logger.debug(f"Flushed {len(batch)} position history records")
    
    def _flush_transaction_batch(self, session: Session, batch: List[PositionHistory]):
        """
        Flush a batch of transaction history records to the database.
        
        Args:
            session: Database session
            batch: List of PositionHistory records
        """
        for record in batch:
            session.add(record)
            self.statistics.transactions_loaded += 1
        
        session.flush()
        logger.debug(f"Flushed {len(batch)} transaction history records")
    
    def _take_checkpoint(self, session: Session, record_type: str, key: str):
        """
        Take a checkpoint for restart capability.
        
        Corresponds to the checkpoint logic in HISTLD00 that allows
        the job to restart from the last successful checkpoint.
        
        Args:
            session: Database session
            record_type: Type of record being processed
            key: Last processed key
        """
        session.commit()
        
        self.statistics.checkpoints_taken += 1
        self.statistics.last_checkpoint_time = datetime.now()
        self.statistics.last_checkpoint_key = f"{record_type}:{key}"
        
        self._checkpoint_data = {
            'record_type': record_type,
            'last_key': key,
            'positions_loaded': self.statistics.positions_loaded,
            'transactions_loaded': self.statistics.transactions_loaded,
            'timestamp': datetime.now().isoformat()
        }
        
        logger.info(f"Checkpoint taken at {record_type}:{key}")
    
    def _load_positions_from_file(self, positions_file: str):
        """
        Load position history from a JSON file.
        
        Args:
            positions_file: Path to JSON file
        """
        logger.info(f"Loading positions from {positions_file}")
        
        input_path = Path(positions_file)
        if not input_path.exists():
            raise FileNotFoundError(f"Positions file not found: {positions_file}")
        
        with open(input_path, 'r') as f:
            data = json.load(f)
        
        positions_data = data.get('positions', data) if isinstance(data, dict) else data
        
        batch: List[PositionHistory] = []
        
        with self.db_manager.session_scope() as session:
            for pos_dict in positions_data:
                self.statistics.positions_read += 1
                
                try:
                    history_record = PositionHistory(
                        portfolio_id=pos_dict['portfolio_id'],
                        position_date=pos_dict['date'],
                        investment_id=pos_dict['investment_id'],
                        quantity=float(pos_dict['quantity']),
                        cost_basis=float(pos_dict['cost_basis']),
                        market_value=float(pos_dict['market_value']),
                        currency=pos_dict.get('currency', 'USD'),
                        record_type='PS',
                        action_code='A',
                        reason_code='FILE',
                        process_user=self.config.process_user,
                        batch_id=self.config.batch_id
                    )
                    batch.append(history_record)
                    
                    if len(batch) >= self.config.batch_size:
                        self._flush_position_batch(session, batch)
                        batch.clear()
                        
                except Exception as e:
                    logger.error(f"Error loading position: {e}")
                    self.statistics.positions_rejected += 1
            
            if batch:
                self._flush_position_batch(session, batch)
            
            session.commit()
    
    def _load_transactions_from_file(self, transactions_file: str):
        """
        Load transaction history from a JSON file.
        
        Args:
            transactions_file: Path to JSON file
        """
        logger.info(f"Loading transactions from {transactions_file}")
        
        input_path = Path(transactions_file)
        if not input_path.exists():
            raise FileNotFoundError(f"Transactions file not found: {transactions_file}")
        
        with open(input_path, 'r') as f:
            data = json.load(f)
        
        trans_data = data.get('transactions', data) if isinstance(data, dict) else data
        
        batch: List[PositionHistory] = []
        
        with self.db_manager.session_scope() as session:
            for trans_dict in trans_data:
                self.statistics.transactions_read += 1
                
                try:
                    history_record = PositionHistory(
                        portfolio_id=trans_dict['portfolio_id'],
                        position_date=trans_dict['date'],
                        investment_id=trans_dict['investment_id'],
                        quantity=float(trans_dict['quantity']),
                        cost_basis=float(trans_dict['amount']),
                        market_value=float(trans_dict['amount']),
                        currency=trans_dict.get('currency', 'USD'),
                        record_type='TR',
                        action_code='C',
                        reason_code=trans_dict.get('trans_type', 'UNK'),
                        after_image=json.dumps(trans_dict),
                        process_user=self.config.process_user,
                        batch_id=self.config.batch_id
                    )
                    batch.append(history_record)
                    
                    if len(batch) >= self.config.batch_size:
                        self._flush_transaction_batch(session, batch)
                        batch.clear()
                        
                except Exception as e:
                    logger.error(f"Error loading transaction: {e}")
                    self.statistics.transactions_rejected += 1
            
            if batch:
                self._flush_transaction_batch(session, batch)
            
            session.commit()
    
    def get_return_code(self) -> int:
        """
        Get the return code based on loading results.
        
        Returns:
            Return code (0, 4, 8, or 12)
        """
        total_rejected = self.statistics.total_rejected
        total_read = self.statistics.total_read
        
        if total_read == 0:
            return 4  # Warning: no records to process
        
        rejection_rate = total_rejected / total_read if total_read > 0 else 0
        
        if rejection_rate > 0.1:  # More than 10% rejected
            return 8
        elif total_rejected > 0:
            return 4
        else:
            return 0
    
    def get_statistics(self) -> LoadStatistics:
        """Get loading statistics."""
        return self.statistics
    
    def generate_report(self) -> str:
        """
        Generate a history load report.
        
        Returns:
            Formatted report string
        """
        report_lines = [
            "=" * 60,
            "HISTORY LOAD REPORT",
            "=" * 60,
            f"Report Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            f"Batch ID: {self.config.batch_id}",
            "",
            "POSITION HISTORY STATISTICS",
            "-" * 40,
            f"Positions Read:       {self.statistics.positions_read:>10}",
            f"Positions Loaded:     {self.statistics.positions_loaded:>10}",
            f"Positions Rejected:   {self.statistics.positions_rejected:>10}",
            f"Positions Duplicate:  {self.statistics.positions_duplicate:>10}",
            "",
            "TRANSACTION HISTORY STATISTICS",
            "-" * 40,
            f"Transactions Read:    {self.statistics.transactions_read:>10}",
            f"Transactions Loaded:  {self.statistics.transactions_loaded:>10}",
            f"Transactions Rejected:{self.statistics.transactions_rejected:>10}",
            f"Transactions Duplicate:{self.statistics.transactions_duplicate:>9}",
            "",
            "TOTALS",
            "-" * 40,
            f"Total Read:           {self.statistics.total_read:>10}",
            f"Total Loaded:         {self.statistics.total_loaded:>10}",
            f"Total Rejected:       {self.statistics.total_rejected:>10}",
            "",
            "CHECKPOINT STATISTICS",
            "-" * 40,
            f"Checkpoints Taken:    {self.statistics.checkpoints_taken:>10}",
        ]
        
        if self.statistics.last_checkpoint_key:
            report_lines.append(
                f"Last Checkpoint:      {self.statistics.last_checkpoint_key}"
            )
        
        report_lines.append("")
        
        if self.statistics.elapsed_seconds:
            report_lines.extend([
                "TIMING",
                "-" * 40,
                f"Start Time:           {self.statistics.start_time.strftime('%H:%M:%S')}",
                f"End Time:             {self.statistics.end_time.strftime('%H:%M:%S')}",
                f"Elapsed:              {self.statistics.elapsed_seconds:>9.2f} seconds",
                "",
            ])
        
        report_lines.extend([
            "RETURN CODE",
            "-" * 40,
            f"Return Code:          {self.get_return_code():>10}",
            "",
            "=" * 60,
            "END OF REPORT",
            "=" * 60,
        ])
        
        return "\n".join(report_lines)
