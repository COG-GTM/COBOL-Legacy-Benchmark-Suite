"""
Transaction Validator - Migrated from COBOL TRNVAL00 program.

This module implements the transaction validation logic from the original
COBOL program, including all business rules for validating financial
transactions before processing.

Original COBOL Program: src/programs/batch/TRNVAL00.cbl (referenced in architecture)
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from typing import List, Optional, Tuple
from enum import Enum

from ..models.transaction import TransactionRecord, TransactionType, TransactionStatus
from ..database.connection import session_scope

logger = logging.getLogger(__name__)


class ValidationErrorCode(str, Enum):
    """Validation error codes - maps to COBOL error codes"""
    E001 = "E001"  # Invalid Account Number
    E002 = "E002"  # Invalid Fund ID
    E003 = "E003"  # Invalid Transaction Type
    E004 = "E004"  # Insufficient Position Balance
    E005 = "E005"  # Invalid Date
    E006 = "E006"  # Invalid Price
    E007 = "E007"  # Invalid Quantity
    E008 = "E008"  # Invalid Amount
    W001 = "W001"  # Zero Dollar Transaction (Warning)
    W002 = "W002"  # Duplicate Transaction ID (Warning)


@dataclass
class ValidationError:
    """Represents a validation error"""
    code: ValidationErrorCode
    message: str
    field: str
    value: str
    is_warning: bool = False


@dataclass
class ValidationResult:
    """Result of transaction validation"""
    transaction: TransactionRecord
    is_valid: bool
    errors: List[ValidationError] = field(default_factory=list)
    warnings: List[ValidationError] = field(default_factory=list)

    @property
    def has_errors(self) -> bool:
        return len(self.errors) > 0

    @property
    def has_warnings(self) -> bool:
        return len(self.warnings) > 0


@dataclass
class BatchValidationResult:
    """Result of batch validation"""
    total_records: int = 0
    valid_records: int = 0
    invalid_records: int = 0
    warning_records: int = 0
    validated_transactions: List[TransactionRecord] = field(default_factory=list)
    validation_results: List[ValidationResult] = field(default_factory=list)
    return_code: int = 0  # 0=success, 4=warnings, 8=errors, 12=critical

    def add_result(self, result: ValidationResult):
        """Add a validation result"""
        self.total_records += 1
        self.validation_results.append(result)
        
        if result.is_valid:
            self.valid_records += 1
            self.validated_transactions.append(result.transaction)
            if result.has_warnings:
                self.warning_records += 1
                self.return_code = max(self.return_code, 4)
        else:
            self.invalid_records += 1
            self.return_code = max(self.return_code, 8)


class TransactionValidator:
    """
    Transaction Validator - Migrated from COBOL TRNVAL00.
    
    Validates incoming financial transactions according to business rules:
    - Account number validation
    - Fund ID validation
    - Transaction date validation (not future)
    - Quantity validation (non-zero for BUY/SELL)
    - Price validation (positive for BUY/SELL)
    - Amount validation (non-zero for FEE)
    
    Original COBOL program flow:
    1. 0000-MAIN: Main control
    2. 1000-INITIALIZE: Open files, initialize counters
    3. 2000-PROCESS: Read and validate transactions
    4. 3000-TERMINATE: Close files, display statistics
    """
    
    def __init__(self, valid_accounts: set = None, valid_funds: set = None):
        """
        Initialize the transaction validator.
        
        Args:
            valid_accounts: Set of valid account numbers (for validation)
            valid_funds: Set of valid fund IDs (for validation)
        """
        self.valid_accounts = valid_accounts or set()
        self.valid_funds = valid_funds or set()
        self.processed_transaction_ids = set()
        
        # Statistics (maps to COBOL WS-COUNTERS)
        self.records_read = 0
        self.records_valid = 0
        self.records_invalid = 0
        self.records_warning = 0
        
        logger.info("TransactionValidator initialized")
    
    def validate_transactions(self, transactions: List[TransactionRecord]) -> BatchValidationResult:
        """
        Validate a list of transactions.
        Implements COBOL 2000-PROCESS paragraph logic.
        
        Args:
            transactions: List of TransactionRecord objects to validate
            
        Returns:
            BatchValidationResult containing validated transactions and statistics
        """
        logger.info(f"Starting validation of {len(transactions)} transactions")
        
        result = BatchValidationResult()
        
        for transaction in transactions:
            self.records_read += 1
            validation_result = self.validate_single_transaction(transaction)
            result.add_result(validation_result)
            
            if validation_result.is_valid:
                self.records_valid += 1
                if validation_result.has_warnings:
                    self.records_warning += 1
            else:
                self.records_invalid += 1
        
        self._display_statistics(result)
        return result
    
    def validate_single_transaction(self, transaction: TransactionRecord) -> ValidationResult:
        """
        Validate a single transaction.
        Implements all validation rules from COBOL TRNVAL00.
        
        Args:
            transaction: TransactionRecord to validate
            
        Returns:
            ValidationResult with validation status and any errors/warnings
        """
        errors = []
        warnings = []
        
        # E001: Account Number Validation
        account_error = self._validate_account(transaction)
        if account_error:
            errors.append(account_error)
        
        # E002: Fund ID Validation
        fund_error = self._validate_fund(transaction)
        if fund_error:
            errors.append(fund_error)
        
        # E003: Transaction Type Validation
        type_error = self._validate_transaction_type(transaction)
        if type_error:
            errors.append(type_error)
        
        # E005: Date Validation
        date_error = self._validate_date(transaction)
        if date_error:
            errors.append(date_error)
        
        # E006: Price Validation
        price_error = self._validate_price(transaction)
        if price_error:
            errors.append(price_error)
        
        # E007: Quantity Validation
        quantity_error = self._validate_quantity(transaction)
        if quantity_error:
            errors.append(quantity_error)
        
        # E008: Amount Validation
        amount_error = self._validate_amount(transaction)
        if amount_error:
            errors.append(amount_error)
        
        # W001: Zero Dollar Transaction Warning
        zero_warning = self._check_zero_amount(transaction)
        if zero_warning:
            warnings.append(zero_warning)
        
        # W002: Duplicate Transaction Warning
        duplicate_warning = self._check_duplicate(transaction)
        if duplicate_warning:
            warnings.append(duplicate_warning)
        
        # Track processed transaction IDs
        self.processed_transaction_ids.add(transaction.transaction_id)
        
        is_valid = len(errors) == 0
        
        return ValidationResult(
            transaction=transaction,
            is_valid=is_valid,
            errors=errors,
            warnings=warnings
        )
    
    def _validate_account(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Validate account number - E001"""
        portfolio_id = transaction.portfolio_id
        
        # Check format
        if not portfolio_id or len(portfolio_id) > 8:
            return ValidationError(
                code=ValidationErrorCode.E001,
                message="Invalid account number format (must be 1-8 characters)",
                field="portfolio_id",
                value=portfolio_id or ""
            )
        
        # Check if account exists (if we have a list of valid accounts)
        if self.valid_accounts and portfolio_id not in self.valid_accounts:
            return ValidationError(
                code=ValidationErrorCode.E001,
                message="Account number does not exist",
                field="portfolio_id",
                value=portfolio_id
            )
        
        return None
    
    def _validate_fund(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Validate fund ID - E002"""
        investment_id = transaction.investment_id
        
        # Check format
        if not investment_id or len(investment_id) > 10:
            return ValidationError(
                code=ValidationErrorCode.E002,
                message="Invalid fund ID format (must be 1-10 characters)",
                field="investment_id",
                value=investment_id or ""
            )
        
        # Check if fund exists (if we have a list of valid funds)
        if self.valid_funds and investment_id not in self.valid_funds:
            return ValidationError(
                code=ValidationErrorCode.E002,
                message="Fund ID does not exist",
                field="investment_id",
                value=investment_id
            )
        
        return None
    
    def _validate_transaction_type(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Validate transaction type - E003"""
        try:
            # TransactionType enum will raise ValueError if invalid
            if isinstance(transaction.transaction_type, str):
                TransactionType(transaction.transaction_type)
            return None
        except ValueError:
            return ValidationError(
                code=ValidationErrorCode.E003,
                message="Invalid transaction type (must be BU, SL, TR, or FE)",
                field="transaction_type",
                value=str(transaction.transaction_type)
            )
    
    def _validate_date(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Validate transaction date - E005"""
        date_str = transaction.date
        
        # Check format
        if not date_str or len(date_str) != 8 or not date_str.isdigit():
            return ValidationError(
                code=ValidationErrorCode.E005,
                message="Invalid date format (must be YYYYMMDD)",
                field="date",
                value=date_str or ""
            )
        
        # Check if date is valid and not in future
        try:
            trans_date = datetime.strptime(date_str, '%Y%m%d')
            if trans_date > datetime.now():
                return ValidationError(
                    code=ValidationErrorCode.E005,
                    message="Transaction date cannot be in the future",
                    field="date",
                    value=date_str
                )
        except ValueError:
            return ValidationError(
                code=ValidationErrorCode.E005,
                message="Invalid date value",
                field="date",
                value=date_str
            )
        
        return None
    
    def _validate_price(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Validate price for BUY/SELL transactions - E006"""
        if transaction.transaction_type in [TransactionType.BUY, TransactionType.SELL]:
            if transaction.price <= 0:
                return ValidationError(
                    code=ValidationErrorCode.E006,
                    message="Price must be greater than zero for BUY/SELL transactions",
                    field="price",
                    value=str(transaction.price)
                )
        return None
    
    def _validate_quantity(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Validate quantity for BUY/SELL transactions - E007"""
        if transaction.transaction_type in [TransactionType.BUY, TransactionType.SELL]:
            if transaction.quantity == 0:
                return ValidationError(
                    code=ValidationErrorCode.E007,
                    message="Quantity cannot be zero for BUY/SELL transactions",
                    field="quantity",
                    value=str(transaction.quantity)
                )
        return None
    
    def _validate_amount(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Validate amount for FEE transactions - E008"""
        if transaction.transaction_type == TransactionType.FEE:
            if transaction.amount == 0:
                return ValidationError(
                    code=ValidationErrorCode.E008,
                    message="Amount cannot be zero for FEE transactions",
                    field="amount",
                    value=str(transaction.amount)
                )
        return None
    
    def _check_zero_amount(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Check for zero dollar transaction - W001 (Warning)"""
        if transaction.transaction_type != TransactionType.FEE:
            calculated_amount = transaction.quantity * transaction.price
            if calculated_amount == 0:
                return ValidationError(
                    code=ValidationErrorCode.W001,
                    message="Zero dollar transaction",
                    field="amount",
                    value=str(calculated_amount),
                    is_warning=True
                )
        return None
    
    def _check_duplicate(self, transaction: TransactionRecord) -> Optional[ValidationError]:
        """Check for duplicate transaction ID - W002 (Warning)"""
        if transaction.transaction_id in self.processed_transaction_ids:
            return ValidationError(
                code=ValidationErrorCode.W002,
                message="Duplicate transaction ID",
                field="transaction_id",
                value=transaction.transaction_id,
                is_warning=True
            )
        return None
    
    def _display_statistics(self, result: BatchValidationResult):
        """Display processing statistics - maps to COBOL 3400-DISPLAY-STATS"""
        logger.info("=" * 60)
        logger.info("TRNVAL00 Processing Statistics:")
        logger.info(f"  Records Read:    {result.total_records}")
        logger.info(f"  Records Valid:   {result.valid_records}")
        logger.info(f"  Records Invalid: {result.invalid_records}")
        logger.info(f"  Records Warning: {result.warning_records}")
        logger.info(f"  Return Code:     {result.return_code}")
        logger.info("=" * 60)
    
    def validate_from_file(self, input_file: str) -> BatchValidationResult:
        """
        Validate transactions from a file.
        
        Args:
            input_file: Path to input file containing transactions
            
        Returns:
            BatchValidationResult
        """
        import json
        
        logger.info(f"Reading transactions from file: {input_file}")
        
        transactions = []
        with open(input_file, 'r') as f:
            data = json.load(f)
            for record in data:
                transactions.append(TransactionRecord.from_dict(record))
        
        return self.validate_transactions(transactions)
    
    def reset_statistics(self):
        """Reset validation statistics"""
        self.records_read = 0
        self.records_valid = 0
        self.records_invalid = 0
        self.records_warning = 0
        self.processed_transaction_ids.clear()
