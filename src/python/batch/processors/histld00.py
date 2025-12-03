"""
History Loader (HISTLD00)

Python implementation of the COBOL HISTLD00 program.
Loads transaction history into the database for reporting.

Processing Flow:
1. Read history records from VSAM file (or in-memory list)
2. Transform records to database format
3. Insert into POSHIST table
4. Commit at checkpoint intervals

This is the final step in the batch processing pipeline:
TRNVAL00 -> POSUPD00 -> HISTLD00
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Any, Iterator, List, Optional

from sqlalchemy.exc import IntegrityError

from ..checkpoint.storage import CheckpointStorage
from ..database.connection import DatabaseConnection
from ..database.models import ErrorLog, PositionHistory
from ..models.batch_control import ReturnCode
from ..models.history import HistoryRecord
from ..models.transaction import TransactionRecord
from .base import BatchProcessor, ProcessingResult

logger = logging.getLogger(__name__)


class HistoryLoader(BatchProcessor):
    """
    History Loader batch processor.
    
    Corresponds to COBOL program HISTLD00.
    Loads history records into the POSHIST database table.
    """
    
    PROGRAM_ID = "HISTLD00"
    COMMIT_FREQ = 1000
    MAX_ERRORS = 100
    MAX_RESTARTS = 3
    
    def __init__(
        self,
        history_records: Optional[List[HistoryRecord]] = None,
        transactions: Optional[List[TransactionRecord]] = None,
        db_connection: Optional[DatabaseConnection] = None,
        checkpoint_storage: Optional[CheckpointStorage] = None,
        restart: bool = False,
    ):
        """
        Initialize history loader.
        
        Args:
            history_records: List of history records to load
            transactions: List of transactions (alternative input)
            db_connection: Database connection (required for loading)
            checkpoint_storage: Storage backend for checkpoints
            restart: If True, attempt to restart from last checkpoint
        """
        super().__init__(checkpoint_storage=checkpoint_storage, restart=restart)
        self.history_records = history_records or []
        self.transactions = transactions or []
        self.db_connection = db_connection
        self._record_iter: Optional[Iterator] = None
        self._current_index = 0
        self._skip_to_index = 0
        self._pending_records: List[PositionHistory] = []
        self._commit_count = 0
    
    def initialize(self) -> None:
        """
        Initialize history loading.
        
        Corresponds to 1000-INITIALIZE in COBOL:
        - Open history file
        - Connect to DB2
        - Initialize checkpoints
        """
        logger.info("Opening transaction history file")
        
        if self.history_records:
            self._record_iter = iter(self.history_records)
        elif self.transactions:
            self._record_iter = iter(self.transactions)
        else:
            self._record_iter = iter([])
        
        self._current_index = 0
        self._pending_records = []
        self._commit_count = 0
        
        if self.db_connection:
            logger.info("Connected to database")
        else:
            logger.warning("No database connection - records will not be persisted")
    
    def read_next_record(self) -> Optional[Any]:
        """
        Read the next history record.
        
        Corresponds to 2100-READ-HISTORY in COBOL.
        
        Returns:
            Next record or None if end of input
        """
        if self._record_iter is None:
            return None
        
        try:
            record = next(self._record_iter)
            self._current_index += 1
            
            if self._current_index <= self._skip_to_index:
                return self.read_next_record()
            
            return record
        except StopIteration:
            return None
    
    def process_record(self, record: Any) -> bool:
        """
        Load a single record to the database.
        
        Corresponds to 2200-LOAD-TO-DB2 in COBOL.
        
        Args:
            record: History record or transaction to load
            
        Returns:
            True if record was loaded successfully
        """
        try:
            db_record = self._transform_to_db_record(record)
            self._pending_records.append(db_record)
            self._commit_count += 1
            
            if self._commit_count >= self.COMMIT_FREQ:
                self._commit_pending_records()
            
            self.result.records_written += 1
            return True
            
        except Exception as e:
            logger.warning(f"Error loading record: {e}")
            self._log_error(record, str(e))
            return False
    
    def _transform_to_db_record(self, record: Any) -> PositionHistory:
        """
        Transform input record to database model.
        
        Handles both HistoryRecord and TransactionRecord inputs.
        
        Corresponds to field mapping in 2200-LOAD-TO-DB2:
        - TH-ACCOUNT-NO -> PH-ACCOUNT-NO
        - TH-PORTFOLIO-ID -> PH-PORTFOLIO-ID
        - etc.
        """
        now = datetime.utcnow()
        
        if isinstance(record, TransactionRecord):
            return PositionHistory(
                account_no=record.portfolio_id.strip()[:8],
                portfolio_id=record.portfolio_id.strip(),
                trans_date=self._format_date(record.date),
                trans_time=self._format_time(record.time),
                trans_type=record.transaction_type.value,
                security_id=record.investment_id.strip(),
                quantity=record.quantity,
                price=record.price,
                amount=record.amount,
                fees=Decimal("0"),
                total_amount=record.amount,
                cost_basis=Decimal("0"),
                gain_loss=Decimal("0"),
                process_date=now.strftime("%Y-%m-%d"),
                process_time=now.strftime("%H:%M:%S"),
                program_id=self.PROGRAM_ID,
                user_id="SYSTEM",
                audit_timestamp=now,
            )
        elif isinstance(record, HistoryRecord):
            return PositionHistory(
                account_no=record.portfolio_id.strip()[:8],
                portfolio_id=record.portfolio_id.strip(),
                trans_date=self._format_date(record.date),
                trans_time=self._format_time(record.time),
                trans_type=record.record_type.value,
                security_id="",
                quantity=Decimal("0"),
                price=Decimal("0"),
                amount=Decimal("0"),
                fees=Decimal("0"),
                total_amount=Decimal("0"),
                cost_basis=Decimal("0"),
                gain_loss=Decimal("0"),
                process_date=now.strftime("%Y-%m-%d"),
                process_time=now.strftime("%H:%M:%S"),
                program_id=self.PROGRAM_ID,
                user_id="SYSTEM",
                audit_timestamp=now,
            )
        else:
            raise ValueError(f"Unsupported record type: {type(record)}")
    
    def _format_date(self, date_str: str) -> str:
        """Format date string to YYYY-MM-DD."""
        date_str = date_str.strip()
        if len(date_str) == 8:
            return f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:8]}"
        return date_str
    
    def _format_time(self, time_str: str) -> str:
        """Format time string to HH:MM:SS."""
        time_str = time_str.strip()
        if len(time_str) == 6:
            return f"{time_str[:2]}:{time_str[2:4]}:{time_str[4:6]}"
        return time_str
    
    def _commit_pending_records(self) -> None:
        """
        Commit pending records to database.
        
        Corresponds to 2300-CHECK-COMMIT in COBOL.
        """
        if not self._pending_records:
            return
        
        if not self.db_connection:
            logger.warning("No database connection - skipping commit")
            self._pending_records = []
            self._commit_count = 0
            return
        
        try:
            with self.db_connection.transaction() as session:
                for record in self._pending_records:
                    try:
                        session.add(record)
                        session.flush()
                    except IntegrityError:
                        session.rollback()
                        logger.debug(f"Duplicate record skipped: {record.account_no}")
                        continue
            
            logger.info(f"Committed {len(self._pending_records)} records")
            self._pending_records = []
            self._commit_count = 0
            
        except Exception as e:
            logger.error(f"Error committing records: {e}")
            raise
    
    def _log_error(self, record: Any, error_message: str) -> None:
        """Log error to database."""
        if self.db_connection:
            try:
                with self.db_connection.transaction() as session:
                    error_log = ErrorLog.create_error(
                        program_id=self.PROGRAM_ID,
                        error_type="D",
                        error_severity=3,
                        error_code="E200",
                        error_message=error_message[:200],
                        user_id="SYSTEM",
                        additional_info=str(record)[:500],
                    )
                    session.add(error_log)
            except Exception as e:
                logger.error(f"Failed to log error to database: {e}")
    
    def get_record_key(self, record: Any) -> str:
        """Get record key for checkpoint tracking."""
        if isinstance(record, TransactionRecord):
            return record.key.to_string()
        elif isinstance(record, HistoryRecord):
            return record.key.to_string()
        return str(self._current_index)
    
    def skip_to_key(self, key: str) -> None:
        """Skip to a specific record for restart."""
        records = self.history_records or self.transactions
        for i, rec in enumerate(records):
            rec_key = self.get_record_key(rec)
            if rec_key == key:
                self._skip_to_index = i + 1
                logger.info(f"Restart: skipping to index {self._skip_to_index}")
                return
        logger.warning(f"Restart key not found: {key}")
    
    def terminate(self) -> None:
        """
        Terminate history loading.
        
        Corresponds to 3000-TERMINATE in COBOL:
        - Final commit
        - Close files
        - Disconnect from DB2
        - Display statistics
        """
        logger.info("Performing final commit")
        self._commit_pending_records()
        
        logger.info("Closing history files")
        
        if self.result.records_error > 0:
            self.result.return_code = ReturnCode.WARNING.value


def load_history(
    history_records: Optional[List[HistoryRecord]] = None,
    transactions: Optional[List[TransactionRecord]] = None,
    db_connection: Optional[DatabaseConnection] = None,
    checkpoint_storage: Optional[CheckpointStorage] = None,
    restart: bool = False,
) -> ProcessingResult:
    """
    Convenience function to load history records.
    
    Args:
        history_records: List of history records to load
        transactions: List of transactions (alternative input)
        db_connection: Database connection (required for loading)
        checkpoint_storage: Optional checkpoint storage
        restart: If True, attempt restart from last checkpoint
        
    Returns:
        ProcessingResult with statistics
    """
    loader = HistoryLoader(
        history_records=history_records,
        transactions=transactions,
        db_connection=db_connection,
        checkpoint_storage=checkpoint_storage,
        restart=restart,
    )
    return loader.run()
