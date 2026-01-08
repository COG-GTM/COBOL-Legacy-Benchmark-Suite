"""Transaction Validator - converted from TRNVAL00.cbl equivalent.

This module provides transaction validation functionality similar to
the COBOL TRNVAL00 batch program, using pandas for transaction processing.

COBOL Program Reference:
- Validates incoming financial transactions
- Checks data integrity and business rules
- Produces validation reports
"""

from datetime import datetime
from decimal import Decimal

import pandas as pd
from sqlalchemy.orm import Session

from app.database.models import PortfolioMaster
from app.models.error import ReturnCode
from app.models.transaction import TransactionRecord, TransactionStatus, TransactionType
from app.utils.constants import (
    TRANSACTION_TYPES,
    ValidationConstants,
)
from app.utils.error_handler import ErrorHandler
from app.utils.logging import get_logger, log_batch_end, log_batch_start

logger = get_logger(__name__)


class ValidationResult:
    """Result of transaction validation."""

    def __init__(self):
        self.is_valid = True
        self.errors: list[str] = []
        self.warnings: list[str] = []

    def add_error(self, message: str) -> None:
        """Add validation error."""
        self.is_valid = False
        self.errors.append(message)

    def add_warning(self, message: str) -> None:
        """Add validation warning."""
        self.warnings.append(message)


class TransactionValidator:
    """Transaction Validator - replaces TRNVAL00 batch program.

    This class validates incoming financial transactions against
    business rules and data integrity constraints.
    """

    PROGRAM_NAME = "TRNVAL00"

    def __init__(self, db: Session):
        self.db = db
        self.error_handler = ErrorHandler(db, self.PROGRAM_NAME)
        self.records_read = 0
        self.records_valid = 0
        self.records_invalid = 0
        self.return_code = ReturnCode.SUCCESS

    def validate_transaction(self, transaction: TransactionRecord) -> ValidationResult:
        """Validate a single transaction.

        Args:
            transaction: Transaction to validate

        Returns:
            ValidationResult with validation status and any errors
        """
        result = ValidationResult()

        self._validate_portfolio_id(transaction, result)
        self._validate_investment_id(transaction, result)
        self._validate_transaction_type(transaction, result)
        self._validate_quantity(transaction, result)
        self._validate_price(transaction, result)
        self._validate_amount(transaction, result)
        self._validate_currency(transaction, result)
        self._validate_date_time(transaction, result)

        if transaction.data.type in (TransactionType.BUY, TransactionType.SELL):
            self._validate_portfolio_exists(transaction, result)

        return result

    def _validate_portfolio_id(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate portfolio ID format."""
        portfolio_id = transaction.portfolio_id
        if not portfolio_id or len(portfolio_id.strip()) == 0:
            result.add_error("Portfolio ID is required")
        elif len(portfolio_id) > 8:
            result.add_error(f"Portfolio ID exceeds maximum length: {portfolio_id}")

    def _validate_investment_id(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate investment ID format."""
        investment_id = transaction.investment_id
        if not investment_id or len(investment_id.strip()) == 0:
            result.add_error("Investment ID is required")
        elif len(investment_id) > 10:
            result.add_error(f"Investment ID exceeds maximum length: {investment_id}")

    def _validate_transaction_type(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate transaction type."""
        try:
            TransactionType(transaction.data.type)
        except ValueError:
            result.add_error(
                f"Invalid transaction type: {transaction.data.type}. "
                f"Valid types: {list(TRANSACTION_TYPES.keys())}"
            )

    def _validate_quantity(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate quantity."""
        quantity = transaction.data.quantity
        if transaction.data.type in (TransactionType.BUY, TransactionType.SELL):
            if quantity <= 0:
                result.add_error(f"Quantity must be positive for {transaction.data.type.value}")
        if abs(quantity) > Decimal("99999999999.9999"):
            result.add_error(f"Quantity exceeds maximum value: {quantity}")

    def _validate_price(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate price."""
        price = transaction.data.price
        if transaction.data.type in (TransactionType.BUY, TransactionType.SELL):
            if price <= 0:
                result.add_error(f"Price must be positive for {transaction.data.type.value}")
        if abs(price) > Decimal("99999999999.9999"):
            result.add_error(f"Price exceeds maximum value: {price}")

    def _validate_amount(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate amount."""
        amount = transaction.data.amount
        if amount < ValidationConstants.MIN_AMOUNT:
            result.add_error(f"Amount below minimum: {amount}")
        if amount > ValidationConstants.MAX_AMOUNT:
            result.add_error(f"Amount exceeds maximum: {amount}")

        expected_amount = transaction.data.quantity * transaction.data.price
        if abs(amount - expected_amount) > Decimal("0.01"):
            result.add_warning(
                f"Amount {amount} does not match quantity * price = {expected_amount}"
            )

    def _validate_currency(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate currency code."""
        currency = transaction.data.currency
        valid_currencies = ["USD", "EUR", "GBP", "JPY", "CAD"]
        if currency not in valid_currencies:
            result.add_error(f"Invalid currency code: {currency}")

    def _validate_date_time(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate date and time formats."""
        try:
            datetime.strptime(transaction.key.date, "%Y%m%d")
        except ValueError:
            result.add_error(f"Invalid date format: {transaction.key.date}")

        try:
            datetime.strptime(transaction.key.time, "%H%M%S")
        except ValueError:
            result.add_error(f"Invalid time format: {transaction.key.time}")

    def _validate_portfolio_exists(
        self, transaction: TransactionRecord, result: ValidationResult
    ) -> None:
        """Validate that portfolio exists in database."""
        portfolio = (
            self.db.query(PortfolioMaster)
            .filter(PortfolioMaster.portfolio_id == transaction.portfolio_id)
            .first()
        )
        if not portfolio:
            result.add_error(f"Portfolio not found: {transaction.portfolio_id}")
        elif portfolio.status != "A":
            result.add_error(
                f"Portfolio {transaction.portfolio_id} is not active (status: {portfolio.status})"
            )

    def validate_batch(
        self, transactions: list[TransactionRecord]
    ) -> tuple[list[TransactionRecord], list[tuple[TransactionRecord, ValidationResult]]]:
        """Validate a batch of transactions.

        Args:
            transactions: List of transactions to validate

        Returns:
            Tuple of (valid_transactions, invalid_transactions_with_results)
        """
        log_batch_start(logger, self.PROGRAM_NAME, "TRNVAL", datetime.now().strftime("%Y%m%d"))

        valid_transactions = []
        invalid_transactions = []

        for transaction in transactions:
            self.records_read += 1
            result = self.validate_transaction(transaction)

            if result.is_valid:
                self.records_valid += 1
                valid_transactions.append(transaction)
            else:
                self.records_invalid += 1
                invalid_transactions.append((transaction, result))
                self.return_code = max(self.return_code, ReturnCode.WARNING)

        if self.records_invalid > 0:
            self.return_code = ReturnCode.WARNING

        log_batch_end(
            logger,
            self.PROGRAM_NAME,
            "TRNVAL",
            self.return_code,
            self.records_read,
            self.records_valid,
            self.records_invalid,
        )

        return valid_transactions, invalid_transactions

    def validate_dataframe(self, df: pd.DataFrame) -> pd.DataFrame:
        """Validate transactions from a pandas DataFrame.

        Args:
            df: DataFrame with transaction data

        Returns:
            DataFrame with validation results added
        """
        df = df.copy()
        df["is_valid"] = True
        df["validation_errors"] = ""

        for idx, row in df.iterrows():
            try:
                transaction = self._row_to_transaction(row)
                result = self.validate_transaction(transaction)
                df.at[idx, "is_valid"] = result.is_valid
                df.at[idx, "validation_errors"] = "; ".join(result.errors)
            except Exception as e:
                df.at[idx, "is_valid"] = False
                df.at[idx, "validation_errors"] = f"Parse error: {str(e)}"

        return df

    def _row_to_transaction(self, row: pd.Series) -> TransactionRecord:
        """Convert DataFrame row to TransactionRecord."""
        from app.models.transaction import (
            TransactionAudit,
            TransactionData,
            TransactionKey,
        )

        return TransactionRecord(
            key=TransactionKey(
                date=str(row.get("transaction_date", ""))[:8],
                time=str(row.get("transaction_time", "000000"))[:6],
                portfolio_id=str(row.get("portfolio_id", ""))[:8],
                sequence_no=str(row.get("sequence_no", "000001"))[:6],
            ),
            data=TransactionData(
                investment_id=str(row.get("investment_id", ""))[:10],
                type=TransactionType(row.get("transaction_type", "BU")),
                quantity=Decimal(str(row.get("quantity", 0))),
                price=Decimal(str(row.get("price", 0))),
                amount=Decimal(str(row.get("amount", 0))),
                currency=str(row.get("currency", "USD"))[:3],
                status=TransactionStatus.PENDING,
            ),
            audit=TransactionAudit(),
        )

    def get_statistics(self) -> dict:
        """Get validation statistics."""
        return {
            "records_read": self.records_read,
            "records_valid": self.records_valid,
            "records_invalid": self.records_invalid,
            "return_code": self.return_code,
            "error_stats": self.error_handler.get_stats(),
        }
