"""
Position Manager - Python implementation of POSUPD00.cbl

This module implements the position update logic from the COBOL
program POSUPD00, which updates portfolio positions based on
validated transactions.

Original COBOL Program: src/programs/batch/POSUPD00.cbl

Key Functions:
- Process validated transactions (buys, sells, transfers, fees)
- Update portfolio position quantities and cost basis
- Calculate realized gains/losses for sell transactions
- Maintain audit trail of position changes
- Generate position update reports

Return Codes:
- 0: All updates successful
- 4: Some updates had warnings
- 8: Some updates had errors
- 12: Critical error, processing aborted
"""

import json
import logging
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import List, Optional, Dict, Tuple

from migration.python.models.transaction import (
    TransactionType,
    TransactionStatus,
    TransactionRecord,
    ValidatedTransaction,
)
from migration.python.models.position import (
    PositionStatus,
    PositionKey,
    PositionData,
    PositionAudit,
    PositionRecord,
)
from migration.python.models.history import (
    HistoryRecordType,
    HistoryActionCode,
    HistoryRecord,
)
from migration.python.database.orm_models import PortfolioMaster, TransactionHistory, PositionHistory
from migration.python.database.session import DatabaseManager

# Configure logging
logger = logging.getLogger(__name__)


@dataclass
class PositionUpdateResult:
    """
    Result of a single position update operation.
    
    Contains the transaction that triggered the update, the position
    before and after the update, and any realized gain/loss.
    """
    transaction: TransactionRecord
    position_before: Optional[PositionRecord]
    position_after: PositionRecord
    realized_gain_loss: Decimal = Decimal('0')
    is_new_position: bool = False
    success: bool = True
    error_message: str = ''
    
    @property
    def position_change(self) -> Decimal:
        """Calculate the change in position quantity."""
        before_qty = self.position_before.data.quantity if self.position_before else Decimal('0')
        return self.position_after.data.quantity - before_qty


@dataclass
class PositionUpdateStatistics:
    """
    Statistics collected during position update processing.
    
    Corresponds to the counters maintained in POSUPD00's WORKING-STORAGE:
    - WS-TRANS-READ
    - WS-TRANS-PROCESSED
    - WS-POSITIONS-CREATED
    - WS-POSITIONS-UPDATED
    - WS-ERRORS
    """
    transactions_read: int = 0
    transactions_processed: int = 0
    positions_created: int = 0
    positions_updated: int = 0
    positions_closed: int = 0
    buys_processed: int = 0
    sells_processed: int = 0
    transfers_processed: int = 0
    fees_processed: int = 0
    errors: int = 0
    warnings: int = 0
    total_realized_gain_loss: Decimal = Decimal('0')
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    
    @property
    def elapsed_seconds(self) -> Optional[float]:
        """Elapsed processing time in seconds."""
        if self.start_time is None or self.end_time is None:
            return None
        return (self.end_time - self.start_time).total_seconds()


@dataclass
class PositionManagerConfig:
    """
    Configuration for position update processing.
    """
    # Processing options
    create_history_records: bool = True
    update_transaction_status: bool = True
    
    # Cost basis calculation method
    # FIFO = First In First Out
    # LIFO = Last In First Out  
    # AVG = Average Cost
    cost_basis_method: str = 'AVG'
    
    # Rounding settings
    quantity_precision: int = 4
    amount_precision: int = 2
    
    # Processing user
    process_user: str = 'BATCH'


class PositionManager:
    """
    Position Manager - Python implementation of POSUPD00.cbl
    
    This class updates portfolio positions based on validated transactions,
    following the same business logic as the original COBOL program.
    
    The update process follows the same flow as POSUPD00:
    1. Initialize processing (open files, set counters)
    2. Read validated transactions
    3. For each transaction:
       - Look up existing position
       - Apply transaction (buy/sell/transfer/fee)
       - Update position record
       - Create history record
    4. Generate update report
    5. Terminate processing (close files, return code)
    
    Usage:
        manager = PositionManager(db_manager)
        results = manager.process_transactions(validated_transactions)
        
        # Or process from file
        results = manager.process_from_file('validated_transactions.json')
        
        # Check results
        for result in results:
            if result.success:
                print(f"Updated: {result.position_after.key.composite_key}")
            else:
                print(f"Error: {result.error_message}")
    """
    
    def __init__(self, db_manager: DatabaseManager,
                 config: Optional[PositionManagerConfig] = None):
        """
        Initialize the position manager.
        
        Args:
            db_manager: Database manager for position operations
            config: Position manager configuration
        """
        self.db_manager = db_manager
        self.config = config or PositionManagerConfig()
        self.statistics = PositionUpdateStatistics()
        self._position_cache: Dict[str, PositionRecord] = {}
        self._history_records: List[HistoryRecord] = []
    
    def process_transactions(self, transactions: List[TransactionRecord],
                             process_date: Optional[str] = None) -> List[PositionUpdateResult]:
        """
        Process a list of validated transactions.
        
        This method implements the main processing loop from POSUPD00:
        - 1000-PROCESS-INITIALIZE
        - 2000-PROCESS-TRANSACTIONS (loop)
        - 3000-PROCESS-TERMINATE
        
        Args:
            transactions: List of validated TransactionRecord objects
            process_date: Processing date (defaults to today)
            
        Returns:
            List of PositionUpdateResult objects
        """
        if process_date is None:
            process_date = datetime.now().strftime("%Y%m%d")
        
        logger.info(f"Starting position update processing for {len(transactions)} transactions")
        self._initialize_processing()
        
        results: List[PositionUpdateResult] = []
        
        try:
            for transaction in transactions:
                self.statistics.transactions_read += 1
                
                try:
                    result = self._process_single_transaction(transaction, process_date)
                    results.append(result)
                    
                    if result.success:
                        self.statistics.transactions_processed += 1
                        self._update_type_counter(transaction.data.trans_type)
                        
                        if result.is_new_position:
                            self.statistics.positions_created += 1
                        else:
                            self.statistics.positions_updated += 1
                        
                        if result.position_after.data.status == PositionStatus.CLOSED:
                            self.statistics.positions_closed += 1
                        
                        self.statistics.total_realized_gain_loss += result.realized_gain_loss
                    else:
                        self.statistics.errors += 1
                        
                except Exception as e:
                    logger.error(f"Error processing transaction: {e}")
                    results.append(PositionUpdateResult(
                        transaction=transaction,
                        position_before=None,
                        position_after=PositionRecord.create_new(
                            transaction.key.portfolio_id,
                            transaction.data.investment_id,
                            Decimal('0'),
                            Decimal('0'),
                            self.config.process_user
                        ),
                        success=False,
                        error_message=str(e)
                    ))
                    self.statistics.errors += 1
            
            # Persist all changes to database
            self._persist_changes(results)
            
        except Exception as e:
            logger.error(f"Critical error during position update: {e}")
            raise
        finally:
            self._terminate_processing()
        
        return results
    
    def process_from_file(self, input_file: str,
                          process_date: Optional[str] = None) -> List[PositionUpdateResult]:
        """
        Process transactions from a JSON file.
        
        Args:
            input_file: Path to JSON file containing validated transactions
            process_date: Processing date (defaults to today)
            
        Returns:
            List of PositionUpdateResult objects
        """
        logger.info(f"Loading transactions from {input_file}")
        
        input_path = Path(input_file)
        if not input_path.exists():
            raise FileNotFoundError(f"Input file not found: {input_file}")
        
        with open(input_path, 'r') as f:
            data = json.load(f)
        
        # Handle both list and dict with 'transactions' key
        if isinstance(data, dict):
            transactions_data = data.get('transactions', [])
        else:
            transactions_data = data
        
        transactions = [TransactionRecord.from_dict(t) for t in transactions_data]
        
        return self.process_transactions(transactions, process_date)
    
    def process_from_database(self, process_date: Optional[str] = None,
                               portfolio_id: Optional[str] = None) -> List[PositionUpdateResult]:
        """
        Process validated transactions from the database.
        
        Args:
            process_date: Filter by transaction date (YYYYMMDD)
            portfolio_id: Filter by portfolio ID
            
        Returns:
            List of PositionUpdateResult objects
        """
        logger.info("Loading validated transactions from database")
        
        transactions = []
        
        with self.db_manager.session_scope() as session:
            query = session.query(TransactionHistory).filter(
                TransactionHistory.validation_status == 'V',
                TransactionHistory.status == 'P'  # Pending
            )
            
            if process_date:
                query = query.filter(TransactionHistory.trans_date == process_date)
            if portfolio_id:
                query = query.filter(TransactionHistory.portfolio_id == portfolio_id)
            
            for db_trans in query.all():
                transactions.append(TransactionRecord.from_dict({
                    'date': db_trans.trans_date,
                    'time': db_trans.trans_time,
                    'portfolio_id': db_trans.portfolio_id,
                    'sequence_no': db_trans.sequence_no,
                    'investment_id': db_trans.investment_id,
                    'trans_type': db_trans.trans_type,
                    'quantity': str(db_trans.quantity),
                    'price': str(db_trans.price),
                    'amount': str(db_trans.amount),
                    'currency': db_trans.currency,
                    'status': db_trans.status
                }))
        
        return self.process_transactions(transactions, process_date)
    
    def _initialize_processing(self):
        """
        Initialize processing - corresponds to 1000-PROCESS-INITIALIZE.
        """
        self.statistics = PositionUpdateStatistics()
        self.statistics.start_time = datetime.now()
        self._position_cache.clear()
        self._history_records.clear()
        logger.info("Position update processing initialized")
    
    def _terminate_processing(self):
        """
        Terminate processing - corresponds to 3000-PROCESS-TERMINATE.
        """
        self.statistics.end_time = datetime.now()
        
        logger.info(
            f"Position update complete: "
            f"Read={self.statistics.transactions_read}, "
            f"Processed={self.statistics.transactions_processed}, "
            f"Created={self.statistics.positions_created}, "
            f"Updated={self.statistics.positions_updated}, "
            f"Closed={self.statistics.positions_closed}, "
            f"Errors={self.statistics.errors}"
        )
        
        if self.statistics.total_realized_gain_loss != 0:
            logger.info(f"Total realized gain/loss: {self.statistics.total_realized_gain_loss}")
    
    def _process_single_transaction(self, transaction: TransactionRecord,
                                     process_date: str) -> PositionUpdateResult:
        """
        Process a single transaction - corresponds to 2000-PROCESS-TRANSACTION.
        
        Routes to appropriate handler based on transaction type:
        - 2100-PROCESS-BUY
        - 2200-PROCESS-SELL
        - 2300-PROCESS-TRANSFER
        - 2400-PROCESS-FEE
        
        Args:
            transaction: TransactionRecord to process
            process_date: Processing date
            
        Returns:
            PositionUpdateResult
        """
        portfolio_id = transaction.key.portfolio_id.strip()
        investment_id = transaction.data.investment_id.strip()
        
        # Look up existing position
        position_before = self._get_position(portfolio_id, investment_id, process_date)
        is_new = position_before is None
        
        # Create working copy of position
        if position_before:
            # Deep copy for before image
            position_before_copy = PositionRecord.from_dict(position_before.to_dict())
            position = PositionRecord.from_dict(position_before.to_dict())
        else:
            position_before_copy = None
            position = None
        
        # Process based on transaction type
        realized_gain_loss = Decimal('0')
        
        if transaction.data.trans_type == TransactionType.BUY:
            position = self._process_buy(transaction, position, process_date)
        elif transaction.data.trans_type == TransactionType.SELL:
            position, realized_gain_loss = self._process_sell(transaction, position, process_date)
        elif transaction.data.trans_type == TransactionType.TRANSFER:
            position = self._process_transfer(transaction, position, process_date)
        elif transaction.data.trans_type == TransactionType.FEE:
            position = self._process_fee(transaction, position, process_date)
        else:
            return PositionUpdateResult(
                transaction=transaction,
                position_before=position_before_copy,
                position_after=position or PositionRecord.create_new(
                    portfolio_id, investment_id, Decimal('0'), Decimal('0'),
                    self.config.process_user, process_date
                ),
                success=False,
                error_message=f"Unknown transaction type: {transaction.data.trans_type}"
            )
        
        # Create history record if configured
        if self.config.create_history_records:
            self._create_history_record(
                portfolio_id,
                position_before_copy,
                position,
                transaction
            )
        
        # Update cache
        cache_key = f"{portfolio_id}:{investment_id}"
        self._position_cache[cache_key] = position
        
        return PositionUpdateResult(
            transaction=transaction,
            position_before=position_before_copy,
            position_after=position,
            realized_gain_loss=realized_gain_loss,
            is_new_position=is_new,
            success=True
        )
    
    def _process_buy(self, transaction: TransactionRecord,
                     position: Optional[PositionRecord],
                     process_date: str) -> PositionRecord:
        """
        Process a buy transaction - corresponds to 2100-PROCESS-BUY.
        
        For buys:
        - Add quantity to position
        - Add cost to cost basis
        - Create new position if none exists
        
        Args:
            transaction: Buy transaction
            position: Existing position (or None)
            process_date: Processing date
            
        Returns:
            Updated PositionRecord
        """
        portfolio_id = transaction.key.portfolio_id.strip()
        investment_id = transaction.data.investment_id.strip()
        quantity = transaction.data.quantity
        price = transaction.data.price
        
        if position is None:
            # Create new position
            position = PositionRecord.create_new(
                portfolio_id=portfolio_id,
                investment_id=investment_id,
                quantity=quantity,
                price=price,
                user=self.config.process_user,
                date=process_date
            )
        else:
            # Update existing position
            position.apply_buy(
                quantity=quantity,
                price=price,
                user=self.config.process_user
            )
        
        return position
    
    def _process_sell(self, transaction: TransactionRecord,
                      position: Optional[PositionRecord],
                      process_date: str) -> Tuple[PositionRecord, Decimal]:
        """
        Process a sell transaction - corresponds to 2200-PROCESS-SELL.
        
        For sells:
        - Reduce quantity from position
        - Reduce cost basis proportionally
        - Calculate realized gain/loss
        - Close position if quantity reaches zero
        
        Args:
            transaction: Sell transaction
            position: Existing position
            process_date: Processing date
            
        Returns:
            Tuple of (updated PositionRecord, realized gain/loss)
        """
        if position is None:
            raise ValueError(
                f"Cannot sell from non-existent position: "
                f"{transaction.key.portfolio_id}:{transaction.data.investment_id}"
            )
        
        quantity = transaction.data.quantity
        price = transaction.data.price
        
        position, realized_gain_loss = position.apply_sell(
            quantity=quantity,
            price=price,
            user=self.config.process_user
        )
        
        return position, realized_gain_loss
    
    def _process_transfer(self, transaction: TransactionRecord,
                          position: Optional[PositionRecord],
                          process_date: str) -> PositionRecord:
        """
        Process a transfer transaction - corresponds to 2300-PROCESS-TRANSFER.
        
        Transfers are treated as buys (transfer in) or sells (transfer out)
        depending on the sign of the quantity.
        
        Args:
            transaction: Transfer transaction
            position: Existing position (or None)
            process_date: Processing date
            
        Returns:
            Updated PositionRecord
        """
        portfolio_id = transaction.key.portfolio_id.strip()
        investment_id = transaction.data.investment_id.strip()
        quantity = transaction.data.quantity
        price = transaction.data.price
        
        # Positive quantity = transfer in (like a buy)
        # Negative quantity = transfer out (like a sell)
        if quantity >= 0:
            if position is None:
                position = PositionRecord.create_new(
                    portfolio_id=portfolio_id,
                    investment_id=investment_id,
                    quantity=quantity,
                    price=price,
                    user=self.config.process_user,
                    date=process_date
                )
            else:
                position.apply_buy(
                    quantity=quantity,
                    price=price,
                    user=self.config.process_user
                )
        else:
            if position is None:
                raise ValueError(
                    f"Cannot transfer out from non-existent position: "
                    f"{portfolio_id}:{investment_id}"
                )
            position.apply_sell(
                quantity=abs(quantity),
                price=price,
                user=self.config.process_user
            )
        
        return position
    
    def _process_fee(self, transaction: TransactionRecord,
                     position: Optional[PositionRecord],
                     process_date: str) -> PositionRecord:
        """
        Process a fee transaction - corresponds to 2400-PROCESS-FEE.
        
        Fees increase the cost basis without changing quantity.
        
        Args:
            transaction: Fee transaction
            position: Existing position
            process_date: Processing date
            
        Returns:
            Updated PositionRecord
        """
        portfolio_id = transaction.key.portfolio_id.strip()
        investment_id = transaction.data.investment_id.strip()
        
        if position is None:
            # Create position with zero quantity but fee as cost basis
            position = PositionRecord.create_new(
                portfolio_id=portfolio_id,
                investment_id=investment_id,
                quantity=Decimal('0'),
                price=Decimal('0'),
                user=self.config.process_user,
                date=process_date
            )
            position.data.cost_basis = transaction.data.amount
        else:
            # Add fee to cost basis
            position.data.cost_basis += transaction.data.amount
            position.audit = PositionAudit(
                last_maint_date=datetime.now(),
                last_maint_user=self.config.process_user
            )
        
        return position
    
    def _get_position(self, portfolio_id: str, investment_id: str,
                      process_date: str) -> Optional[PositionRecord]:
        """
        Get position from cache or database.
        
        Args:
            portfolio_id: Portfolio identifier
            investment_id: Investment identifier
            process_date: Position date
            
        Returns:
            PositionRecord or None if not found
        """
        cache_key = f"{portfolio_id}:{investment_id}"
        
        # Check cache first
        if cache_key in self._position_cache:
            return self._position_cache[cache_key]
        
        # Look up in database
        with self.db_manager.session_scope() as session:
            db_position = session.query(PortfolioMaster).filter(
                PortfolioMaster.portfolio_id == portfolio_id,
                PortfolioMaster.investment_id == investment_id,
                PortfolioMaster.status == 'A'
            ).first()
            
            if db_position:
                position = PositionRecord.from_dict({
                    'portfolio_id': db_position.portfolio_id,
                    'date': db_position.position_date,
                    'investment_id': db_position.investment_id,
                    'quantity': str(db_position.quantity),
                    'cost_basis': str(db_position.cost_basis),
                    'market_value': str(db_position.market_value),
                    'currency': db_position.currency,
                    'status': db_position.status,
                    'last_maint_date': db_position.last_maint_date.isoformat() 
                        if db_position.last_maint_date else None,
                    'last_maint_user': db_position.last_maint_user
                })
                self._position_cache[cache_key] = position
                return position
        
        return None
    
    def _create_history_record(self, portfolio_id: str,
                                position_before: Optional[PositionRecord],
                                position_after: PositionRecord,
                                transaction: TransactionRecord):
        """
        Create a history record for the position change.
        
        Args:
            portfolio_id: Portfolio identifier
            position_before: Position before update (or None for new)
            position_after: Position after update
            transaction: Transaction that caused the change
        """
        before_image = position_before.to_dict() if position_before else None
        after_image = position_after.to_dict()
        
        if position_before is None:
            history = HistoryRecord.create_add_record(
                portfolio_id=portfolio_id,
                record_type=HistoryRecordType.POSITION,
                after_image=after_image,
                user=self.config.process_user,
                reason_code=transaction.data.trans_type.value
            )
        else:
            history = HistoryRecord.create_change_record(
                portfolio_id=portfolio_id,
                record_type=HistoryRecordType.POSITION,
                before_image=before_image,
                after_image=after_image,
                user=self.config.process_user,
                reason_code=transaction.data.trans_type.value
            )
        
        self._history_records.append(history)
    
    def _persist_changes(self, results: List[PositionUpdateResult]):
        """
        Persist all position changes to the database.
        
        Args:
            results: List of position update results
        """
        logger.info("Persisting position changes to database")
        
        with self.db_manager.session_scope() as session:
            for result in results:
                if not result.success:
                    continue
                
                position = result.position_after
                portfolio_id = position.key.portfolio_id.strip()
                investment_id = position.key.investment_id.strip()
                
                # Find or create database record
                db_position = session.query(PortfolioMaster).filter(
                    PortfolioMaster.portfolio_id == portfolio_id,
                    PortfolioMaster.investment_id == investment_id
                ).first()
                
                if db_position is None:
                    db_position = PortfolioMaster(
                        portfolio_id=portfolio_id,
                        position_date=position.key.date,
                        investment_id=investment_id
                    )
                    session.add(db_position)
                
                # Update fields
                db_position.quantity = float(position.data.quantity)
                db_position.cost_basis = float(position.data.cost_basis)
                db_position.market_value = float(position.data.market_value)
                db_position.currency = position.data.currency
                db_position.status = position.data.status.value
                db_position.last_maint_date = datetime.now()
                db_position.last_maint_user = self.config.process_user
                
                # Update transaction status if configured
                if self.config.update_transaction_status:
                    trans = result.transaction
                    db_trans = session.query(TransactionHistory).filter(
                        TransactionHistory.trans_date == trans.key.date,
                        TransactionHistory.trans_time == trans.key.time,
                        TransactionHistory.portfolio_id == trans.key.portfolio_id.strip(),
                        TransactionHistory.sequence_no == trans.key.sequence_no.strip()
                    ).first()
                    
                    if db_trans:
                        db_trans.status = 'D'  # Done
                        db_trans.process_date = datetime.now()
                        db_trans.process_user = self.config.process_user
            
            # Persist history records
            if self.config.create_history_records:
                for history in self._history_records:
                    db_history = PositionHistory(
                        portfolio_id=history.key.portfolio_id.strip(),
                        position_date=history.key.date,
                        investment_id=history.data.after_image.get('investment_id', '').strip()
                            if history.data.after_image else '',
                        quantity=float(history.data.after_image.get('quantity', 0))
                            if history.data.after_image else 0,
                        cost_basis=float(history.data.after_image.get('cost_basis', 0))
                            if history.data.after_image else 0,
                        market_value=float(history.data.after_image.get('market_value', 0))
                            if history.data.after_image else 0,
                        record_type=history.data.record_type.value,
                        action_code=history.data.action_code.value,
                        reason_code=history.data.reason_code.strip(),
                        before_image=json.dumps(history.data.before_image)
                            if history.data.before_image else None,
                        after_image=json.dumps(history.data.after_image)
                            if history.data.after_image else None,
                        process_user=self.config.process_user
                    )
                    session.add(db_history)
            
            session.commit()
        
        logger.info(f"Persisted {len(results)} position updates and {len(self._history_records)} history records")
    
    def _update_type_counter(self, trans_type: TransactionType):
        """Update the appropriate transaction type counter."""
        if trans_type == TransactionType.BUY:
            self.statistics.buys_processed += 1
        elif trans_type == TransactionType.SELL:
            self.statistics.sells_processed += 1
        elif trans_type == TransactionType.TRANSFER:
            self.statistics.transfers_processed += 1
        elif trans_type == TransactionType.FEE:
            self.statistics.fees_processed += 1
    
    def get_return_code(self) -> int:
        """
        Get the return code based on processing results.
        
        Returns:
            Return code (0, 4, 8, or 12)
        """
        if self.statistics.errors > 0:
            return 8
        elif self.statistics.warnings > 0:
            return 4
        else:
            return 0
    
    def get_statistics(self) -> PositionUpdateStatistics:
        """Get processing statistics."""
        return self.statistics
    
    def generate_report(self) -> str:
        """
        Generate a position update report.
        
        Returns:
            Formatted report string
        """
        report_lines = [
            "=" * 60,
            "POSITION UPDATE REPORT",
            "=" * 60,
            f"Report Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            "",
            "PROCESSING STATISTICS",
            "-" * 40,
            f"Transactions Read:      {self.statistics.transactions_read:>10}",
            f"Transactions Processed: {self.statistics.transactions_processed:>10}",
            f"Positions Created:      {self.statistics.positions_created:>10}",
            f"Positions Updated:      {self.statistics.positions_updated:>10}",
            f"Positions Closed:       {self.statistics.positions_closed:>10}",
            f"Errors:                 {self.statistics.errors:>10}",
            "",
            "TRANSACTION BREAKDOWN",
            "-" * 40,
            f"Buys Processed:         {self.statistics.buys_processed:>10}",
            f"Sells Processed:        {self.statistics.sells_processed:>10}",
            f"Transfers Processed:    {self.statistics.transfers_processed:>10}",
            f"Fees Processed:         {self.statistics.fees_processed:>10}",
            "",
            "FINANCIAL SUMMARY",
            "-" * 40,
            f"Total Realized G/L:     {self.statistics.total_realized_gain_loss:>10.2f}",
            "",
        ]
        
        if self.statistics.elapsed_seconds:
            report_lines.extend([
                "TIMING",
                "-" * 40,
                f"Start Time:             {self.statistics.start_time.strftime('%H:%M:%S')}",
                f"End Time:               {self.statistics.end_time.strftime('%H:%M:%S')}",
                f"Elapsed:                {self.statistics.elapsed_seconds:>9.2f} seconds",
                "",
            ])
        
        report_lines.extend([
            "RETURN CODE",
            "-" * 40,
            f"Return Code:            {self.get_return_code():>10}",
            "",
            "=" * 60,
            "END OF REPORT",
            "=" * 60,
        ])
        
        return "\n".join(report_lines)
