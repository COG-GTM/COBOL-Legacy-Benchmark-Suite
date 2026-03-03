"""Transaction Validator — batch processing program.

Replaces: TRNVAL00 (src/programs/batch/TRNVAL00.cbl)

Reads input transaction records, validates each record against
business rules, and writes valid/invalid records to separate outputs.
This is the first step in the batch pipeline.

Original COBOL flow:
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-PROCESS-FILE (loop)
      2100-READ-RECORD
      2200-VALIDATE-RECORD
      2300-WRITE-VALID / 2400-WRITE-INVALID
    3000-FINALIZE

Return codes (gating):
  RC 0 = success (all records valid)
  RC 4 = warnings (some invalid records, but processing can continue)
  RC 8 = error (too many invalid records)
  RC 12+ = severe error
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from decimal import Decimal

from sqlalchemy.orm import Session

from portfolio_manager.models.copybook_models import (
    CurrencyCode,
    TransactionRecord,
    TransactionType,
)
from portfolio_manager.services.error_handler import (
    ErrorProcessor,
)

logger = logging.getLogger(__name__)


@dataclass
class ValidationResult:
    """Result of validating a single transaction record."""

    valid: bool = True
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


@dataclass
class BatchResult:
    """Result of the entire validation batch run."""

    records_read: int = 0
    records_valid: int = 0
    records_invalid: int = 0
    records_warning: int = 0
    return_code: int = 0
    error_messages: list[str] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Validation rules (from TRNVAL00.cbl paragraphs 2200-VALIDATE-RECORD)
# ---------------------------------------------------------------------------

VALID_TRANSACTION_TYPES = {t.value for t in TransactionType}
VALID_CURRENCIES = {c.value for c in CurrencyCode}
MAX_QUANTITY = Decimal("99999999999.9999")
MAX_PRICE = Decimal("99999999999.9999")
MAX_AMOUNT = Decimal("9999999999999.99")
MAX_ERROR_PERCENT = 10  # percent threshold for RC=8


def validate_transaction(record: TransactionRecord) -> ValidationResult:
    """Validate a single transaction record.

    Replaces TRNVAL00 paragraph 2200-VALIDATE-RECORD.

    Checks:
      - 2210-VALIDATE-KEY: Key fields are non-empty
      - 2220-VALIDATE-TYPE: Transaction type is valid (BU/SL/TR/FE)
      - 2230-VALIDATE-AMOUNTS: Quantity, price, amount are within range
      - 2240-VALIDATE-CURRENCY: Currency code is valid
      - 2250-CROSS-VALIDATE: Amount ≈ quantity * price (within tolerance)
    """
    result = ValidationResult()

    # 2210-VALIDATE-KEY
    if not record.portfolio_id or not record.portfolio_id.strip():
        result.valid = False
        result.errors.append("Portfolio ID is required")

    if not record.trn_date or len(record.trn_date) != 8:
        result.valid = False
        result.errors.append("Transaction date must be 8 characters (YYYYMMDD)")
    else:
        _validate_date(record.trn_date, "Transaction date", result)

    if not record.investment_id or not record.investment_id.strip():
        result.valid = False
        result.errors.append("Investment ID is required")

    # 2220-VALIDATE-TYPE
    if record.transaction_type.value not in VALID_TRANSACTION_TYPES:
        result.valid = False
        result.errors.append(
            f"Invalid transaction type: {record.transaction_type.value}"
        )

    # 2230-VALIDATE-AMOUNTS
    if record.quantity < 0 or record.quantity > MAX_QUANTITY:
        result.valid = False
        result.errors.append(f"Quantity out of range: {record.quantity}")

    if record.price < 0 or record.price > MAX_PRICE:
        result.valid = False
        result.errors.append(f"Price out of range: {record.price}")

    if abs(record.amount) > MAX_AMOUNT:
        result.valid = False
        result.errors.append(f"Amount out of range: {record.amount}")

    # 2240-VALIDATE-CURRENCY
    if record.currency.value not in VALID_CURRENCIES:
        result.valid = False
        result.errors.append(f"Invalid currency: {record.currency.value}")

    # 2250-CROSS-VALIDATE: amount should be approximately qty * price
    if record.quantity > 0 and record.price > 0:
        expected = record.quantity * record.price
        tolerance = expected * Decimal("0.01")  # 1% tolerance
        if abs(record.amount - expected) > tolerance:
            result.warnings.append(
                f"Amount ({record.amount}) differs from qty*price ({expected})"
            )

    return result


def _validate_date(date_str: str, field_name: str, result: ValidationResult) -> None:
    """Validate a date string in YYYYMMDD format."""
    from datetime import date as date_type

    try:
        year = int(date_str[:4])
        month = int(date_str[4:6])
        day = int(date_str[6:8])
    except (ValueError, IndexError):
        result.valid = False
        result.errors.append(f"{field_name} is not a valid date: {date_str}")
        return

    # Use stdlib to catch impossible calendar dates (Feb 30, etc.)
    try:
        date_type(year, month, day)
    except ValueError:
        result.valid = False
        result.errors.append(f"{field_name} has invalid date components: {date_str}")
        return

    if not (1900 <= year <= 2099):
        result.valid = False
        result.errors.append(f"{field_name} has invalid date components: {date_str}")


# ---------------------------------------------------------------------------
# Main batch processing (replaces TRNVAL00 0000-MAIN-PROCESS)
# ---------------------------------------------------------------------------


class TransactionValidator:
    """Transaction validation batch processor.

    Replaces TRNVAL00 (src/programs/batch/TRNVAL00.cbl).
    """

    PROGRAM_ID = "TRNVAL00"

    def __init__(self, session: Session | None = None):
        self._session = session
        self._error_processor = ErrorProcessor(session)

    def run(
        self,
        transactions: list[TransactionRecord],
        max_error_percent: int = MAX_ERROR_PERCENT,
    ) -> tuple[BatchResult, list[TransactionRecord], list[TransactionRecord]]:
        """Run the transaction validation batch.

        Replaces 0000-MAIN-PROCESS:
          1000-INITIALIZE -> setup counters
          2000-PROCESS-FILE -> loop through records
          3000-FINALIZE -> compute return code

        Args:
            transactions: List of transaction records to validate.
            max_error_percent: Maximum percentage of invalid records
                             before returning RC=8.

        Returns:
            Tuple of (BatchResult, valid_records, invalid_records).
        """
        batch = BatchResult()
        valid_records: list[TransactionRecord] = []
        invalid_records: list[TransactionRecord] = []

        logger.info(
            "%s: Starting validation of %d transactions",
            self.PROGRAM_ID,
            len(transactions),
        )

        # 2000-PROCESS-FILE
        for record in transactions:
            batch.records_read += 1
            result = validate_transaction(record)

            if result.valid:
                batch.records_valid += 1
                valid_records.append(record)

                if result.warnings:
                    batch.records_warning += 1
                    for warning in result.warnings:
                        logger.warning(
                            "%s: Record %s/%s warning: %s",
                            self.PROGRAM_ID,
                            record.portfolio_id,
                            record.trn_date,
                            warning,
                        )
            else:
                batch.records_invalid += 1
                invalid_records.append(record)

                for error in result.errors:
                    batch.error_messages.append(
                        f"Record {record.portfolio_id}/{record.trn_date}: {error}"
                    )
                    self._error_processor.process_error(
                        program_id=self.PROGRAM_ID,
                        category="VL",
                        error_code="E008",
                        severity=2,
                        error_text=error,
                        details=f"portfolio={record.portfolio_id} date={record.trn_date}",
                    )

        # 3000-FINALIZE — compute return code
        if batch.records_read == 0:
            batch.return_code = 4  # warning: no records
            logger.warning("%s: No records to validate", self.PROGRAM_ID)
        elif batch.records_invalid == 0:
            batch.return_code = 0  # success
        elif batch.records_read > 0:
            error_pct = (batch.records_invalid / batch.records_read) * 100
            if error_pct > max_error_percent:
                batch.return_code = 8  # too many errors
            else:
                batch.return_code = 4  # some errors, but within threshold

        logger.info(
            "%s: Complete — read=%d valid=%d invalid=%d warnings=%d RC=%d",
            self.PROGRAM_ID,
            batch.records_read,
            batch.records_valid,
            batch.records_invalid,
            batch.records_warning,
            batch.return_code,
        )

        return batch, valid_records, invalid_records
