"""
Transaction Validator (TRNVAL00)

Python implementation of the COBOL TRNVAL00 program.
Validates incoming financial transactions before processing.

Processing Flow:
1. Read transactions from input file/queue
2. Validate each transaction against business rules
3. Write valid transactions to output for position updates
4. Log errors for invalid transactions

Validation Rules (from data-dictionary.md):
- Account number must be numeric
- Transaction date must not be future date
- Share quantity must not be zero for BUY/SELL
- Amount must be non-zero for FEE
- Price must be greater than zero for BUY/SELL
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Any, Iterator, List, Optional

from ..checkpoint.storage import CheckpointStorage
from ..database.connection import DatabaseConnection
from ..database.models import ErrorLog
from ..models.batch_control import ReturnCode
from ..models.transaction import (
    TransactionData,
    TransactionKey,
    TransactionRecord,
    TransactionStatus,
    TransactionType,
)
from .base import BatchProcessor, ProcessingResult

logger = logging.getLogger(__name__)


class ValidationError:
    """Represents a validation error."""
    
    def __init__(self, code: str, message: str, field: str = ""):
        self.code = code
        self.message = message
        self.field = field
    
    def __str__(self) -> str:
        if self.field:
            return f"{self.code}: {self.message} (field: {self.field})"
        return f"{self.code}: {self.message}"


class TransactionValidator(BatchProcessor):
    """
    Transaction Validator batch processor.
    
    Corresponds to COBOL program TRNVAL00.
    Entry point for the batch processing pipeline.
    """
    
    PROGRAM_ID = "TRNVAL00"
    COMMIT_FREQ = 1000
    MAX_ERRORS = 100
    MAX_RESTARTS = 3
    
    def __init__(
        self,
        input_transactions: Optional[List[TransactionRecord]] = None,
        db_connection: Optional[DatabaseConnection] = None,
        checkpoint_storage: Optional[CheckpointStorage] = None,
        restart: bool = False,
    ):
        """
        Initialize transaction validator.
        
        Args:
            input_transactions: List of transactions to validate
            db_connection: Database connection for error logging
            checkpoint_storage: Storage backend for checkpoints
            restart: If True, attempt to restart from last checkpoint
        """
        super().__init__(checkpoint_storage=checkpoint_storage, restart=restart)
        self.input_transactions = input_transactions or []
        self.db_connection = db_connection
        self.valid_transactions: List[TransactionRecord] = []
        self.invalid_transactions: List[tuple[TransactionRecord, List[ValidationError]]] = []
        self._transaction_iter: Optional[Iterator[TransactionRecord]] = None
        self._current_index = 0
        self._skip_to_index = 0
    
    def initialize(self) -> None:
        """
        Initialize transaction validation.
        
        Corresponds to 1000-INITIALIZE in COBOL:
        - Open input files
        - Initialize counters
        - Connect to database for error logging
        """
        logger.info("Opening transaction input")
        self._transaction_iter = iter(self.input_transactions)
        self._current_index = 0
        self.valid_transactions = []
        self.invalid_transactions = []
        
        if self.db_connection:
            logger.info("Database connection established for error logging")
    
    def read_next_record(self) -> Optional[TransactionRecord]:
        """
        Read the next transaction record.
        
        Corresponds to 2100-READ-TRANSACTION in COBOL.
        
        Returns:
            Next transaction or None if end of input
        """
        if self._transaction_iter is None:
            return None
        
        try:
            record = next(self._transaction_iter)
            self._current_index += 1
            
            if self._current_index <= self._skip_to_index:
                return self.read_next_record()
            
            return record
        except StopIteration:
            return None
    
    def process_record(self, record: TransactionRecord) -> bool:
        """
        Validate a single transaction.
        
        Corresponds to 2200-VALIDATE-TRANSACTION in COBOL.
        
        Args:
            record: Transaction to validate
            
        Returns:
            True if transaction is valid
        """
        errors = self._validate_transaction(record)
        
        if errors:
            self._handle_invalid_transaction(record, errors)
            return False
        
        self._handle_valid_transaction(record)
        return True
    
    def _validate_transaction(self, record: TransactionRecord) -> List[ValidationError]:
        """
        Perform all validation checks on a transaction.
        
        Implements validation rules from data-dictionary.md.
        
        Args:
            record: Transaction to validate
            
        Returns:
            List of validation errors (empty if valid)
        """
        errors: List[ValidationError] = []
        
        if not self._validate_account_number(record):
            errors.append(ValidationError(
                code="E001",
                message="Invalid account number - must be numeric",
                field="portfolio_id"
            ))
        
        if not self._validate_transaction_date(record):
            errors.append(ValidationError(
                code="E002",
                message="Transaction date cannot be in the future",
                field="date"
            ))
        
        if not self._validate_transaction_type(record):
            errors.append(ValidationError(
                code="E003",
                message="Invalid transaction type",
                field="transaction_type"
            ))
        
        if not self._validate_quantity(record):
            errors.append(ValidationError(
                code="E004",
                message="Share quantity must not be zero for BUY/SELL",
                field="quantity"
            ))
        
        if not self._validate_price(record):
            errors.append(ValidationError(
                code="E005",
                message="Price must be greater than zero for BUY/SELL",
                field="price"
            ))
        
        if not self._validate_amount(record):
            errors.append(ValidationError(
                code="E006",
                message="Amount must be non-zero for FEE transactions",
                field="amount"
            ))
        
        if not self._validate_currency(record):
            errors.append(ValidationError(
                code="E007",
                message="Invalid currency code",
                field="currency"
            ))
        
        return errors
    
    def _validate_account_number(self, record: TransactionRecord) -> bool:
        """Validate account number is numeric."""
        portfolio_id = record.portfolio_id.strip()
        return portfolio_id.isdigit() or portfolio_id.replace("-", "").isdigit()
    
    def _validate_transaction_date(self, record: TransactionRecord) -> bool:
        """Validate transaction date is not in the future."""
        try:
            trans_date = datetime.strptime(record.date.strip(), "%Y%m%d")
            return trans_date.date() <= datetime.now().date()
        except ValueError:
            return False
    
    def _validate_transaction_type(self, record: TransactionRecord) -> bool:
        """Validate transaction type is valid."""
        try:
            return record.transaction_type in TransactionType
        except (ValueError, AttributeError):
            return False
    
    def _validate_quantity(self, record: TransactionRecord) -> bool:
        """Validate quantity for BUY/SELL transactions."""
        if record.is_buy() or record.is_sell():
            return record.quantity != Decimal("0")
        return True
    
    def _validate_price(self, record: TransactionRecord) -> bool:
        """Validate price for BUY/SELL transactions."""
        if record.is_buy() or record.is_sell():
            return record.price > Decimal("0")
        return True
    
    def _validate_amount(self, record: TransactionRecord) -> bool:
        """Validate amount for FEE transactions."""
        if record.is_fee():
            return record.amount != Decimal("0")
        return True
    
    def _validate_currency(self, record: TransactionRecord) -> bool:
        """Validate currency code."""
        valid_currencies = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF"}
        return record.currency.strip().upper() in valid_currencies
    
    def _handle_valid_transaction(self, record: TransactionRecord) -> None:
        """
        Handle a valid transaction.
        
        Corresponds to 2300-WRITE-VALID-TRANS in COBOL.
        """
        record.mark_done(user=self.PROGRAM_ID)
        self.valid_transactions.append(record)
        self.result.records_written += 1
        logger.debug(f"Valid transaction: {record.key.to_string()}")
    
    def _handle_invalid_transaction(
        self, record: TransactionRecord, errors: List[ValidationError]
    ) -> None:
        """
        Handle an invalid transaction.
        
        Corresponds to 2400-WRITE-ERROR in COBOL.
        """
        record.mark_failed(user=self.PROGRAM_ID)
        self.invalid_transactions.append((record, errors))
        
        for error in errors:
            self.result.add_error(f"{record.key.to_string()}: {error}")
            logger.warning(f"Validation error: {record.key.to_string()} - {error}")
        
        if self.db_connection:
            self._log_error_to_db(record, errors)
    
    def _log_error_to_db(
        self, record: TransactionRecord, errors: List[ValidationError]
    ) -> None:
        """Log validation errors to database."""
        try:
            with self.db_connection.transaction() as session:
                for error in errors:
                    error_log = ErrorLog.create_error(
                        program_id=self.PROGRAM_ID,
                        error_type="D",
                        error_severity=3,
                        error_code=error.code,
                        error_message=error.message,
                        user_id="SYSTEM",
                        additional_info=f"Transaction: {record.key.to_string()}, Field: {error.field}",
                    )
                    session.add(error_log)
        except Exception as e:
            logger.error(f"Failed to log error to database: {e}")
    
    def get_record_key(self, record: TransactionRecord) -> str:
        """Get transaction key for checkpoint tracking."""
        return record.key.to_string()
    
    def skip_to_key(self, key: str) -> None:
        """Skip to a specific transaction for restart."""
        for i, trans in enumerate(self.input_transactions):
            if trans.key.to_string() == key:
                self._skip_to_index = i + 1
                logger.info(f"Restart: skipping to index {self._skip_to_index}")
                return
        logger.warning(f"Restart key not found: {key}")
    
    def terminate(self) -> None:
        """
        Terminate transaction validation.
        
        Corresponds to 3000-TERMINATE in COBOL:
        - Close files
        - Final commit
        - Display statistics
        """
        logger.info("Closing transaction files")
        
        if self.result.records_error > 0:
            self.result.return_code = ReturnCode.WARNING.value
    
    def get_valid_transactions(self) -> List[TransactionRecord]:
        """Get list of valid transactions for downstream processing."""
        return self.valid_transactions
    
    def get_invalid_transactions(self) -> List[tuple[TransactionRecord, List[ValidationError]]]:
        """Get list of invalid transactions with their errors."""
        return self.invalid_transactions


def validate_transactions(
    transactions: List[TransactionRecord],
    db_connection: Optional[DatabaseConnection] = None,
    checkpoint_storage: Optional[CheckpointStorage] = None,
    restart: bool = False,
) -> tuple[ProcessingResult, List[TransactionRecord]]:
    """
    Convenience function to validate transactions.
    
    Args:
        transactions: List of transactions to validate
        db_connection: Optional database connection for error logging
        checkpoint_storage: Optional checkpoint storage
        restart: If True, attempt restart from last checkpoint
        
    Returns:
        Tuple of (ProcessingResult, list of valid transactions)
    """
    validator = TransactionValidator(
        input_transactions=transactions,
        db_connection=db_connection,
        checkpoint_storage=checkpoint_storage,
        restart=restart,
    )
    result = validator.run()
    return result, validator.get_valid_transactions()
