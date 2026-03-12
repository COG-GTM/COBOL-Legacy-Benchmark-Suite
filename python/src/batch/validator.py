"""
Transaction validator translated from COBOL program TRNVAL00.cbl.

Replaces:
  - TRNVAL00.cbl 1000-VALIDATE-TRANSACTION: Main validation dispatch
  - TRNVAL00.cbl 2000-CHECK-DUPLICATE: Duplicate transaction detection
  - TRNVAL00.cbl 3000-VALIDATE-FIELDS: Field-level validation
  - TRNVAL00.cbl 4000-VALIDATE-BUSINESS: Business rule validation

Validates incoming transaction records before processing.
"""

import logging
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.constants import (
    ReturnCode,
    TransactionType,
)
from src.common.error_handler import ValidationError
from src.db.repository import PortfolioRepository, PositionRepository, TransactionRepository
from src.models.transaction import TransactionRecord
from src.portfolio.validation import (
    validate_amount,
    validate_portfolio_id,
)

logger = logging.getLogger(__name__)


class ValidationResult:
    """Result of validating a single transaction."""

    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []

    @property
    def is_valid(self) -> bool:
        return len(self.errors) == 0

    @property
    def return_code(self) -> ReturnCode:
        if self.errors:
            return ReturnCode.ERROR
        if self.warnings:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def add_error(self, message: str) -> None:
        self.errors.append(message)

    def add_warning(self, message: str) -> None:
        self.warnings.append(message)


class TransactionValidator:
    """
    Transaction validation service.

    Translates TRNVAL00.cbl paragraph structure:
      1000-VALIDATE-TRANSACTION -> validate()
      2000-CHECK-DUPLICATE      -> _check_duplicate()
      3000-VALIDATE-FIELDS      -> _validate_fields()
      4000-VALIDATE-BUSINESS    -> _validate_business_rules()
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._portfolio_repo = PortfolioRepository(session)
        self._position_repo = PositionRepository(session)
        self._transaction_repo = TransactionRepository(session)
        # Batch counters
        self.total_validated = 0
        self.total_passed = 0
        self.total_failed = 0

    def validate(self, record: TransactionRecord) -> ValidationResult:
        """
        Validate a single transaction record.

        Translates TRNVAL00.cbl 1000-VALIDATE-TRANSACTION:
          PERFORM 2000-CHECK-DUPLICATE
          IF WS-VALID
            PERFORM 3000-VALIDATE-FIELDS
          IF WS-VALID
            PERFORM 4000-VALIDATE-BUSINESS
        """
        self.total_validated += 1
        result = ValidationResult()

        # 2000-CHECK-DUPLICATE
        self._check_duplicate(record, result)

        # 3000-VALIDATE-FIELDS
        if result.is_valid:
            self._validate_fields(record, result)

        # 4000-VALIDATE-BUSINESS
        if result.is_valid:
            self._validate_business_rules(record, result)

        if result.is_valid:
            self.total_passed += 1
        else:
            self.total_failed += 1
            logger.warning(
                "Transaction validation failed for %s: %s",
                record.portfolio_id,
                "; ".join(result.errors),
            )

        return result

    def validate_batch(
        self, records: list[TransactionRecord]
    ) -> list[tuple[TransactionRecord, ValidationResult]]:
        """Validate a batch of transaction records."""
        results: list[tuple[TransactionRecord, ValidationResult]] = []
        for record in records:
            result = self.validate(record)
            results.append((record, result))
        return results

    def _check_duplicate(self, record: TransactionRecord, result: ValidationResult) -> None:
        """
        Check for duplicate transactions.

        Translates TRNVAL00.cbl 2000-CHECK-DUPLICATE.
        """
        if record.transaction_id:
            existing = self._transaction_repo.get_by_id(record.transaction_id)
            if existing is not None:
                result.add_error(
                    f"Duplicate transaction ID: {record.transaction_id}"
                )

    def _validate_fields(self, record: TransactionRecord, result: ValidationResult) -> None:
        """
        Validate individual fields.

        Translates TRNVAL00.cbl 3000-VALIDATE-FIELDS.
        """
        # Portfolio ID format
        try:
            validate_portfolio_id(record.portfolio_id)
        except ValidationError as exc:
            result.add_error(str(exc))

        # Investment ID required for buy/sell
        if record.trn_type in (TransactionType.BUY, TransactionType.SELL):
            if not record.investment_id or not record.investment_id.strip():
                result.add_error("Investment ID is required for buy/sell transactions")

        # Quantity must be positive
        if record.quantity <= Decimal("0"):
            result.add_error(f"Quantity must be positive: {record.quantity}")

        # Price must be non-negative for buy/sell
        if record.trn_type in (TransactionType.BUY, TransactionType.SELL):
            if record.price < Decimal("0"):
                result.add_error(f"Price must be non-negative: {record.price}")

        # Amount range check
        try:
            validate_amount(record.amount)
        except ValidationError as exc:
            result.add_error(str(exc))

        # Transaction type validity
        valid_types = {t.value for t in TransactionType}
        if record.trn_type not in valid_types:
            result.add_error(f"Invalid transaction type: {record.trn_type}")

    def _validate_business_rules(
        self, record: TransactionRecord, result: ValidationResult
    ) -> None:
        """
        Validate business rules.

        Translates TRNVAL00.cbl 4000-VALIDATE-BUSINESS:
          - Portfolio must exist and be active
          - Sell: sufficient quantity must be available
          - Buy: verify investment ID validity
        """
        # Portfolio must exist
        portfolio = self._portfolio_repo.get_by_id(record.portfolio_id)
        if portfolio is None:
            result.add_error(f"Portfolio not found: {record.portfolio_id}")
            return

        # Portfolio must be active
        if portfolio.status != "A":
            result.add_error(
                f"Portfolio {record.portfolio_id} is not active (status={portfolio.status})"
            )
            return

        # Sell validation: check sufficient units
        if record.trn_type == TransactionType.SELL:
            position = self._position_repo.get_latest_position(
                record.portfolio_id, record.investment_id
            )
            if position is None:
                result.add_error(
                    f"No position found for investment {record.investment_id} "
                    f"in portfolio {record.portfolio_id}"
                )
            elif record.quantity > position.quantity:
                result.add_error(
                    f"Insufficient units: have {position.quantity}, selling {record.quantity}"
                )
