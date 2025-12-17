"""
Transaction Validator - Python implementation of TRNVAL00.cbl

This module implements the transaction validation logic from the COBOL
program TRNVAL00, which validates incoming financial transactions before
they are processed by the position update program (POSUPD00).

Original COBOL Program: src/programs/batch/TRNVAL00.cbl

Key Functions:
- Validate transaction record format and required fields
- Check business rules (valid account, fund, transaction type)
- Verify position balance for sell transactions
- Generate validation reports and error files

Return Codes:
- 0: All transactions valid
- 4: Some transactions had warnings
- 8: Some transactions had errors
- 12: Critical error, processing aborted
"""

import json
import logging
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import List, Optional, Dict, Set, Callable

from migration.python.models.transaction import (
    TransactionType,
    TransactionStatus,
    TransactionKey,
    TransactionData,
    TransactionAudit,
    TransactionRecord,
    ValidatedTransaction,
    ValidationError,
)
from migration.python.models.position import PositionRecord
from migration.python.database.orm_models import PortfolioMaster, TransactionHistory
from migration.python.database.session import DatabaseManager

# Configure logging
logger = logging.getLogger(__name__)


# Error codes from data dictionary
class ErrorCode:
    """
    Standard error codes from the data dictionary.
    
    Corresponds to error codes defined in ERRHAND.cpy:
    - E001: Invalid Account Number
    - E002: Invalid Fund ID
    - E003: Invalid Transaction Type
    - E004: Insufficient Position Balance
    - W001: Zero Dollar Transaction (Warning)
    - W002: Duplicate Transaction ID (Warning)
    """
    INVALID_ACCOUNT = 'E001'
    INVALID_FUND = 'E002'
    INVALID_TRANS_TYPE = 'E003'
    INSUFFICIENT_BALANCE = 'E004'
    INVALID_DATE = 'E005'
    INVALID_AMOUNT = 'E006'
    INVALID_QUANTITY = 'E007'
    INVALID_PRICE = 'E008'
    MISSING_REQUIRED = 'E009'
    ZERO_AMOUNT = 'W001'
    DUPLICATE_TRANS = 'W002'
    AMOUNT_MISMATCH = 'W003'


@dataclass
class ValidationStatistics:
    """
    Statistics collected during validation processing.
    
    Corresponds to the counters maintained in TRNVAL00's WORKING-STORAGE:
    - WS-RECORDS-READ
    - WS-RECORDS-VALID
    - WS-RECORDS-INVALID
    - WS-RECORDS-WARNING
    """
    records_read: int = 0
    records_valid: int = 0
    records_invalid: int = 0
    records_warning: int = 0
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    
    @property
    def total_processed(self) -> int:
        """Total records processed."""
        return self.records_valid + self.records_invalid
    
    @property
    def validation_rate(self) -> float:
        """Percentage of valid records."""
        if self.total_processed == 0:
            return 0.0
        return (self.records_valid / self.total_processed) * 100
    
    @property
    def elapsed_seconds(self) -> Optional[float]:
        """Elapsed processing time in seconds."""
        if self.start_time is None or self.end_time is None:
            return None
        return (self.end_time - self.start_time).total_seconds()


@dataclass
class ValidationConfig:
    """
    Configuration for transaction validation.
    
    Allows customization of validation rules and behavior.
    """
    # Validation toggles
    check_account_exists: bool = True
    check_fund_exists: bool = True
    check_position_balance: bool = True
    check_duplicate_transactions: bool = True
    
    # Tolerance settings
    amount_tolerance: Decimal = Decimal('0.01')  # Allow 1 cent difference
    
    # Valid values (can be loaded from reference data)
    valid_accounts: Optional[Set[str]] = None
    valid_funds: Optional[Set[str]] = None
    
    # Processing options
    stop_on_first_error: bool = False
    max_errors: int = 1000


class TransactionValidator:
    """
    Transaction Validator - Python implementation of TRNVAL00.cbl
    
    This class validates incoming financial transactions according to
    business rules defined in the original COBOL program.
    
    The validation process follows the same flow as TRNVAL00:
    1. Initialize processing (open files, set counters)
    2. Read and validate each transaction
    3. Write valid transactions to output
    4. Write invalid transactions to error file
    5. Generate validation report
    6. Terminate processing (close files, return code)
    
    Usage:
        validator = TransactionValidator(db_manager)
        results = validator.validate_transactions('input.json')
        
        # Or validate from database
        results = validator.validate_from_database(process_date='20241215')
        
        # Check results
        for result in results:
            if result.is_valid:
                print(f"Valid: {result.transaction.key.composite_key}")
            else:
                for error in result.errors:
                    print(f"Error {error.code}: {error.message}")
    """
    
    def __init__(self, db_manager: Optional[DatabaseManager] = None,
                 config: Optional[ValidationConfig] = None):
        """
        Initialize the transaction validator.
        
        Args:
            db_manager: Database manager for position lookups
            config: Validation configuration
        """
        self.db_manager = db_manager
        self.config = config or ValidationConfig()
        self.statistics = ValidationStatistics()
        self._position_cache: Dict[str, PositionRecord] = {}
        self._processed_keys: Set[str] = set()
        
        # Custom validation rules (can be extended)
        self._custom_validators: List[Callable[[TransactionRecord], List[ValidationError]]] = []
    
    def add_custom_validator(self, validator: Callable[[TransactionRecord], List[ValidationError]]):
        """
        Add a custom validation function.
        
        Args:
            validator: Function that takes a TransactionRecord and returns
                      a list of ValidationError objects
        """
        self._custom_validators.append(validator)
    
    def validate_transactions(self, input_file: str,
                              output_file: Optional[str] = None,
                              error_file: Optional[str] = None) -> List[ValidatedTransaction]:
        """
        Validate transactions from a JSON input file.
        
        This method implements the main processing loop from TRNVAL00:
        - 1000-PROCESS-INITIALIZE
        - 2000-PROCESS-TRANSACTIONS (loop)
        - 3000-PROCESS-TERMINATE
        
        Args:
            input_file: Path to JSON file containing transactions
            output_file: Optional path for valid transactions output
            error_file: Optional path for invalid transactions output
            
        Returns:
            List of ValidatedTransaction objects
        """
        logger.info(f"Starting transaction validation from {input_file}")
        self._initialize_processing()
        
        results: List[ValidatedTransaction] = []
        
        try:
            # Read input file
            input_path = Path(input_file)
            if not input_path.exists():
                raise FileNotFoundError(f"Input file not found: {input_file}")
            
            with open(input_path, 'r') as f:
                transactions_data = json.load(f)
            
            # Handle both list and dict with 'transactions' key
            if isinstance(transactions_data, dict):
                transactions_data = transactions_data.get('transactions', [])
            
            # Process each transaction
            for trans_dict in transactions_data:
                self.statistics.records_read += 1
                
                try:
                    transaction = TransactionRecord.from_dict(trans_dict)
                    validated = self._validate_single_transaction(transaction)
                    results.append(validated)
                    
                    if validated.is_valid:
                        self.statistics.records_valid += 1
                    else:
                        self.statistics.records_invalid += 1
                    
                    if validated.has_warnings:
                        self.statistics.records_warning += 1
                    
                    # Check if we should stop
                    if (self.config.stop_on_first_error and 
                        self.statistics.records_invalid > 0):
                        logger.warning("Stopping on first error as configured")
                        break
                    
                    if self.statistics.records_invalid >= self.config.max_errors:
                        logger.warning(f"Max errors ({self.config.max_errors}) reached")
                        break
                        
                except Exception as e:
                    logger.error(f"Error parsing transaction: {e}")
                    self.statistics.records_invalid += 1
            
            # Write output files if specified
            if output_file:
                self._write_valid_transactions(results, output_file)
            if error_file:
                self._write_error_transactions(results, error_file)
                
        except Exception as e:
            logger.error(f"Critical error during validation: {e}")
            raise
        finally:
            self._terminate_processing()
        
        return results
    
    def validate_from_database(self, process_date: Optional[str] = None,
                               portfolio_id: Optional[str] = None,
                               status: str = 'P') -> List[ValidatedTransaction]:
        """
        Validate transactions from the database.
        
        Args:
            process_date: Filter by transaction date (YYYYMMDD)
            portfolio_id: Filter by portfolio ID
            status: Filter by transaction status (default: Pending)
            
        Returns:
            List of ValidatedTransaction objects
        """
        if self.db_manager is None:
            raise RuntimeError("Database manager required for database validation")
        
        logger.info("Starting transaction validation from database")
        self._initialize_processing()
        
        results: List[ValidatedTransaction] = []
        
        try:
            with self.db_manager.session_scope() as session:
                # Build query
                query = session.query(TransactionHistory).filter(
                    TransactionHistory.status == status
                )
                
                if process_date:
                    query = query.filter(TransactionHistory.trans_date == process_date)
                if portfolio_id:
                    query = query.filter(TransactionHistory.portfolio_id == portfolio_id)
                
                # Process each transaction
                for db_trans in query.all():
                    self.statistics.records_read += 1
                    
                    # Convert database record to TransactionRecord
                    transaction = self._db_to_transaction_record(db_trans)
                    validated = self._validate_single_transaction(transaction)
                    results.append(validated)
                    
                    # Update database record with validation results
                    if validated.is_valid:
                        db_trans.validation_status = 'V'
                        self.statistics.records_valid += 1
                    else:
                        db_trans.validation_status = 'I'
                        db_trans.validation_errors = json.dumps([
                            {'code': e.code, 'message': e.message, 'field': e.field}
                            for e in validated.errors
                        ])
                        self.statistics.records_invalid += 1
                    
                    if validated.has_warnings:
                        self.statistics.records_warning += 1
                
                # Commit validation status updates
                session.commit()
                
        except Exception as e:
            logger.error(f"Critical error during database validation: {e}")
            raise
        finally:
            self._terminate_processing()
        
        return results
    
    def validate_single(self, transaction: TransactionRecord) -> ValidatedTransaction:
        """
        Validate a single transaction.
        
        Args:
            transaction: TransactionRecord to validate
            
        Returns:
            ValidatedTransaction with validation results
        """
        return self._validate_single_transaction(transaction)
    
    def _initialize_processing(self):
        """
        Initialize processing - corresponds to 1000-PROCESS-INITIALIZE.
        
        Resets statistics and caches for a new validation run.
        """
        self.statistics = ValidationStatistics()
        self.statistics.start_time = datetime.now()
        self._position_cache.clear()
        self._processed_keys.clear()
        logger.info("Transaction validation initialized")
    
    def _terminate_processing(self):
        """
        Terminate processing - corresponds to 3000-PROCESS-TERMINATE.
        
        Finalizes statistics and logs summary.
        """
        self.statistics.end_time = datetime.now()
        
        logger.info(
            f"Transaction validation complete: "
            f"Read={self.statistics.records_read}, "
            f"Valid={self.statistics.records_valid}, "
            f"Invalid={self.statistics.records_invalid}, "
            f"Warnings={self.statistics.records_warning}"
        )
        
        if self.statistics.elapsed_seconds:
            logger.info(f"Processing time: {self.statistics.elapsed_seconds:.2f} seconds")
    
    def _validate_single_transaction(self, transaction: TransactionRecord) -> ValidatedTransaction:
        """
        Validate a single transaction - corresponds to 2000-VALIDATE-TRANSACTION.
        
        Applies all validation rules in sequence:
        - 2100-VALIDATE-FORMAT
        - 2200-VALIDATE-BUSINESS-RULES
        - 2300-VALIDATE-POSITION-BALANCE
        
        Args:
            transaction: TransactionRecord to validate
            
        Returns:
            ValidatedTransaction with validation results
        """
        result = ValidatedTransaction(
            transaction=transaction,
            is_valid=True,
            errors=[],
            warnings=[]
        )
        
        # 2100-VALIDATE-FORMAT
        self._validate_format(transaction, result)
        
        # 2200-VALIDATE-BUSINESS-RULES
        self._validate_business_rules(transaction, result)
        
        # 2300-VALIDATE-POSITION-BALANCE (for sells)
        if transaction.data.trans_type == TransactionType.SELL:
            self._validate_position_balance(transaction, result)
        
        # Check for duplicate transactions
        if self.config.check_duplicate_transactions:
            self._check_duplicate(transaction, result)
        
        # Run custom validators
        for validator in self._custom_validators:
            custom_errors = validator(transaction)
            for error in custom_errors:
                if error.is_error:
                    result.add_error(error.code, error.message, error.field)
                else:
                    result.add_warning(error.code, error.message, error.field)
        
        # Track processed keys for duplicate detection
        self._processed_keys.add(transaction.key.composite_key)
        
        return result
    
    def _validate_format(self, transaction: TransactionRecord, 
                         result: ValidatedTransaction):
        """
        Validate transaction format - corresponds to 2100-VALIDATE-FORMAT.
        
        Checks:
        - Required fields are present
        - Date format is valid
        - Numeric fields are valid
        """
        # Check required key fields
        if not transaction.key.date or len(transaction.key.date) != 8:
            result.add_error(
                ErrorCode.INVALID_DATE,
                "Transaction date must be 8 characters (YYYYMMDD)",
                "date"
            )
        else:
            # Validate date is a valid date
            try:
                datetime.strptime(transaction.key.date, "%Y%m%d")
            except ValueError:
                result.add_error(
                    ErrorCode.INVALID_DATE,
                    f"Invalid date format: {transaction.key.date}",
                    "date"
                )
        
        if not transaction.key.portfolio_id.strip():
            result.add_error(
                ErrorCode.MISSING_REQUIRED,
                "Portfolio ID is required",
                "portfolio_id"
            )
        
        # Check required data fields
        if not transaction.data.investment_id.strip():
            result.add_error(
                ErrorCode.MISSING_REQUIRED,
                "Investment ID is required",
                "investment_id"
            )
        
        # Validate numeric fields
        if transaction.data.quantity < 0:
            result.add_error(
                ErrorCode.INVALID_QUANTITY,
                "Quantity cannot be negative",
                "quantity"
            )
        
        if transaction.data.price < 0:
            result.add_error(
                ErrorCode.INVALID_PRICE,
                "Price cannot be negative",
                "price"
            )
        
        if transaction.data.amount < 0:
            result.add_error(
                ErrorCode.INVALID_AMOUNT,
                "Amount cannot be negative",
                "amount"
            )
        
        # Check for zero amount (warning)
        if transaction.data.amount == 0:
            result.add_warning(
                ErrorCode.ZERO_AMOUNT,
                "Transaction has zero amount",
                "amount"
            )
        
        # Verify amount matches quantity * price (within tolerance)
        calculated_amount = transaction.data.quantity * transaction.data.price
        amount_diff = abs(transaction.data.amount - calculated_amount)
        if amount_diff > self.config.amount_tolerance:
            result.add_warning(
                ErrorCode.AMOUNT_MISMATCH,
                f"Amount ({transaction.data.amount}) does not match "
                f"quantity * price ({calculated_amount})",
                "amount"
            )
    
    def _validate_business_rules(self, transaction: TransactionRecord,
                                  result: ValidatedTransaction):
        """
        Validate business rules - corresponds to 2200-VALIDATE-BUSINESS-RULES.
        
        Checks:
        - Transaction type is valid
        - Account exists (if configured)
        - Fund/Investment exists (if configured)
        """
        # Validate transaction type
        if not TransactionType.is_valid(transaction.data.trans_type.value):
            result.add_error(
                ErrorCode.INVALID_TRANS_TYPE,
                f"Invalid transaction type: {transaction.data.trans_type.value}",
                "trans_type"
            )
        
        # Validate account exists
        if self.config.check_account_exists and self.config.valid_accounts:
            portfolio_id = transaction.key.portfolio_id.strip()
            if portfolio_id not in self.config.valid_accounts:
                result.add_error(
                    ErrorCode.INVALID_ACCOUNT,
                    f"Invalid account number: {portfolio_id}",
                    "portfolio_id"
                )
        
        # Validate fund/investment exists
        if self.config.check_fund_exists and self.config.valid_funds:
            investment_id = transaction.data.investment_id.strip()
            if investment_id not in self.config.valid_funds:
                result.add_error(
                    ErrorCode.INVALID_FUND,
                    f"Invalid fund ID: {investment_id}",
                    "investment_id"
                )
    
    def _validate_position_balance(self, transaction: TransactionRecord,
                                    result: ValidatedTransaction):
        """
        Validate position balance for sell transactions.
        
        Corresponds to 2300-VALIDATE-POSITION-BALANCE in TRNVAL00.
        
        For sell transactions, verifies that the portfolio has sufficient
        position balance to complete the sale.
        """
        if not self.config.check_position_balance:
            return
        
        if self.db_manager is None:
            logger.warning("Position balance check skipped - no database manager")
            return
        
        portfolio_id = transaction.key.portfolio_id.strip()
        investment_id = transaction.data.investment_id.strip()
        cache_key = f"{portfolio_id}:{investment_id}"
        
        # Check cache first
        if cache_key in self._position_cache:
            position = self._position_cache[cache_key]
            available_quantity = position.data.quantity
        else:
            # Look up position in database
            with self.db_manager.session_scope() as session:
                db_position = session.query(PortfolioMaster).filter(
                    PortfolioMaster.portfolio_id == portfolio_id,
                    PortfolioMaster.investment_id == investment_id,
                    PortfolioMaster.status == 'A'
                ).first()
                
                if db_position:
                    available_quantity = Decimal(str(db_position.quantity))
                    # Cache the position
                    position = PositionRecord.from_dict({
                        'portfolio_id': db_position.portfolio_id,
                        'date': db_position.position_date,
                        'investment_id': db_position.investment_id,
                        'quantity': str(db_position.quantity),
                        'cost_basis': str(db_position.cost_basis),
                        'market_value': str(db_position.market_value),
                        'currency': db_position.currency,
                        'status': db_position.status
                    })
                    self._position_cache[cache_key] = position
                else:
                    available_quantity = Decimal('0')
        
        # Check if sufficient balance
        if transaction.data.quantity > available_quantity:
            result.add_error(
                ErrorCode.INSUFFICIENT_BALANCE,
                f"Insufficient position balance: trying to sell "
                f"{transaction.data.quantity} but only {available_quantity} available",
                "quantity"
            )
    
    def _check_duplicate(self, transaction: TransactionRecord,
                         result: ValidatedTransaction):
        """
        Check for duplicate transactions.
        
        A transaction is considered duplicate if another transaction
        with the same composite key has already been processed.
        """
        key = transaction.key.composite_key
        if key in self._processed_keys:
            result.add_warning(
                ErrorCode.DUPLICATE_TRANS,
                f"Duplicate transaction key: {key}",
                "key"
            )
    
    def _write_valid_transactions(self, results: List[ValidatedTransaction],
                                   output_file: str):
        """
        Write valid transactions to output file.
        
        Args:
            results: List of validation results
            output_file: Path to output file
        """
        valid_transactions = [
            r.transaction.to_dict() 
            for r in results 
            if r.is_valid
        ]
        
        with open(output_file, 'w') as f:
            json.dump({'transactions': valid_transactions}, f, indent=2)
        
        logger.info(f"Wrote {len(valid_transactions)} valid transactions to {output_file}")
    
    def _write_error_transactions(self, results: List[ValidatedTransaction],
                                   error_file: str):
        """
        Write invalid transactions to error file.
        
        Args:
            results: List of validation results
            error_file: Path to error file
        """
        error_records = []
        for r in results:
            if not r.is_valid:
                error_records.append({
                    'transaction': r.transaction.to_dict(),
                    'errors': [
                        {'code': e.code, 'message': e.message, 'field': e.field}
                        for e in r.errors
                    ],
                    'warnings': [
                        {'code': w.code, 'message': w.message, 'field': w.field}
                        for w in r.warnings
                    ]
                })
        
        with open(error_file, 'w') as f:
            json.dump({'errors': error_records}, f, indent=2)
        
        logger.info(f"Wrote {len(error_records)} error records to {error_file}")
    
    def _db_to_transaction_record(self, db_trans: TransactionHistory) -> TransactionRecord:
        """
        Convert database TransactionHistory to TransactionRecord.
        
        Args:
            db_trans: Database transaction record
            
        Returns:
            TransactionRecord object
        """
        return TransactionRecord.from_dict({
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
            'status': db_trans.status,
            'process_date': db_trans.process_date.isoformat() if db_trans.process_date else None,
            'process_user': db_trans.process_user
        })
    
    def get_return_code(self) -> int:
        """
        Get the return code based on validation results.
        
        Corresponds to the return code logic in TRNVAL00:
        - 0: All transactions valid
        - 4: Some transactions had warnings only
        - 8: Some transactions had errors
        - 12: Critical error (not implemented here)
        
        Returns:
            Return code (0, 4, 8, or 12)
        """
        if self.statistics.records_invalid > 0:
            return 8
        elif self.statistics.records_warning > 0:
            return 4
        else:
            return 0
    
    def get_statistics(self) -> ValidationStatistics:
        """
        Get validation statistics.
        
        Returns:
            ValidationStatistics object
        """
        return self.statistics
    
    def generate_report(self) -> str:
        """
        Generate a validation report.
        
        Returns:
            Formatted report string
        """
        report_lines = [
            "=" * 60,
            "TRANSACTION VALIDATION REPORT",
            "=" * 60,
            f"Report Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            "",
            "PROCESSING STATISTICS",
            "-" * 40,
            f"Records Read:     {self.statistics.records_read:>10}",
            f"Records Valid:    {self.statistics.records_valid:>10}",
            f"Records Invalid:  {self.statistics.records_invalid:>10}",
            f"Records Warning:  {self.statistics.records_warning:>10}",
            "",
            f"Validation Rate:  {self.statistics.validation_rate:>9.2f}%",
            "",
        ]
        
        if self.statistics.elapsed_seconds:
            report_lines.extend([
                "TIMING",
                "-" * 40,
                f"Start Time:       {self.statistics.start_time.strftime('%H:%M:%S')}",
                f"End Time:         {self.statistics.end_time.strftime('%H:%M:%S')}",
                f"Elapsed:          {self.statistics.elapsed_seconds:>9.2f} seconds",
                "",
            ])
        
        report_lines.extend([
            "RETURN CODE",
            "-" * 40,
            f"Return Code:      {self.get_return_code():>10}",
            "",
            "=" * 60,
            "END OF REPORT",
            "=" * 60,
        ])
        
        return "\n".join(report_lines)
