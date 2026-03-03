"""Transaction Validation module - replaces TRNVAL00.cbl.

Validates incoming financial transactions before position updates.
First step in the batch pipeline: TRNVAL00 -> POSUPD00 -> HISTLD00.

COBOL program flow:
- 0000-MAIN: Initialize -> Process -> Terminate
- 1000-INITIALIZE: Open files, init counters
- 2000-PROCESS: Read/validate each transaction record
- 2100-VALIDATE-TRANSACTION: Field-level validation
- 2200-WRITE-VALID/2300-WRITE-ERROR: Route records
- 3000-TERMINATE: Close files, set return code
"""

import logging
from datetime import datetime
from decimal import Decimal

from python_app.common.db2 import CommitController, StatisticsCollector
from python_app.common.errors import ErrorHandler, ErrorSeverity
from python_app.models.return_code import RC_SUCCESS, RC_WARNING, RC_ERROR
from python_app.models.transaction import TransactionRecord, TransactionType, TransactionStatus

logger = logging.getLogger("portfolio.batch.trnval00")


class TransactionValidator:
    """Transaction validation processor replacing TRNVAL00.cbl.

    Validates transactions and separates them into valid and error lists.
    Returns RC based on error count: 0=success, 4=warnings, 8+=errors.
    """

    def __init__(self) -> None:
        self.error_handler = ErrorHandler("TRNVAL00")
        self.stats = StatisticsCollector("TRNVAL00")
        self.commit_ctrl = CommitController(commit_interval=1000)
        self.valid_records: list[TransactionRecord] = []
        self.error_records: list[tuple[TransactionRecord, str]] = []
        self.records_read = 0
        self.records_valid = 0
        self.records_error = 0

    def initialize(self) -> None:
        """Initialize processing - replaces 1000-INITIALIZE."""
        self.stats.initialize()
        self.commit_ctrl.initialize()
        self.valid_records.clear()
        self.error_records.clear()
        self.records_read = 0
        self.records_valid = 0
        self.records_error = 0
        logger.info("TRNVAL00 initialized - %s", datetime.now().isoformat())

    def validate_transaction(self, record: TransactionRecord) -> tuple[bool, str]:
        """Validate a single transaction - replaces 2100-VALIDATE-TRANSACTION.

        Performs field-level validation matching COBOL checks:
        - Portfolio ID must be non-empty
        - Investment ID must be non-empty
        - Transaction type must be valid (BU/SL/TR/FE)
        - Quantity must be positive for BUY/SELL
        - Price must be positive for BUY/SELL
        - Amount must be non-zero
        - Currency must be 3 chars
        - Date must be valid format
        """
        # Validate portfolio ID
        if not record.portfolio_id or not record.portfolio_id.strip():
            return False, "Portfolio ID is required"

        # Validate investment ID
        if not record.investment_id or not record.investment_id.strip():
            return False, "Investment ID is required"

        # Validate transaction type
        try:
            TransactionType(record.type)
        except ValueError:
            return False, f"Invalid transaction type: {record.type}"

        # Validate quantity for BUY/SELL
        if record.type in (TransactionType.BUY, TransactionType.SELL):
            if record.quantity <= 0:
                return False, f"Quantity must be positive for {record.type}: {record.quantity}"
            if record.price <= 0:
                return False, f"Price must be positive for {record.type}: {record.price}"

        # Validate amount
        if record.amount == 0:
            return False, "Transaction amount cannot be zero"

        # Validate currency
        if not record.currency or len(record.currency.strip()) != 3:
            return False, f"Invalid currency code: {record.currency}"

        # Validate date format (YYYYMMDD)
        if len(record.date) != 8 or not record.date.isdigit():
            return False, f"Invalid date format: {record.date}"

        # Validate date is a real date
        try:
            datetime.strptime(record.date, "%Y%m%d")
        except ValueError:
            return False, f"Invalid date value: {record.date}"

        # Cross-field validation: amount should approximately match qty * price
        if record.type in (TransactionType.BUY, TransactionType.SELL):
            expected = record.quantity * record.price
            tolerance = abs(expected) * Decimal("0.01")  # 1% tolerance
            if abs(record.amount - expected) > tolerance:
                return False, (
                    f"Amount {record.amount} does not match "
                    f"quantity({record.quantity}) x price({record.price}) = {expected}"
                )

        return True, ""

    def process_transaction(self, record: TransactionRecord) -> bool:
        """Process a single transaction - replaces 2000-PROCESS loop body.

        Returns True if valid, False if error.
        """
        self.records_read += 1
        self.stats.update("reads")

        is_valid, error_msg = self.validate_transaction(record)

        if is_valid:
            # 2200-WRITE-VALID
            self.valid_records.append(record)
            self.records_valid += 1
            self.stats.update("valid")
            return True
        else:
            # 2300-WRITE-ERROR
            self.error_records.append((record, error_msg))
            self.records_error += 1
            self.stats.update("errors")
            self.error_handler.log_error(
                f"Validation failed: {error_msg}",
                severity=ErrorSeverity.WARNING,
                error_code="TVAL",
                details=f"Portfolio={record.portfolio_id}, Investment={record.investment_id}",
            )
            return False

    def process_batch(self, transactions: list[TransactionRecord]) -> int:
        """Process a batch of transactions - replaces 0000-MAIN.

        Returns the return code (RC):
        - 0: All records valid
        - 4: Some records had errors (warnings)
        - 8: Too many errors or processing failure
        """
        self.initialize()

        try:
            for record in transactions:
                self.process_transaction(record)

            return self.terminate()
        except Exception as exc:
            self.error_handler.log_error(
                f"Batch processing failed: {exc}",
                severity=ErrorSeverity.FATAL,
                error_code="TBAT",
                exc=exc,
            )
            return RC_ERROR

    def terminate(self) -> int:
        """Terminate processing and determine return code - replaces 3000-TERMINATE."""
        final_stats = self.stats.terminate()

        logger.info(
            "TRNVAL00 complete: read=%d, valid=%d, errors=%d",
            self.records_read, self.records_valid, self.records_error,
        )
        logger.info("Statistics: %s", final_stats)

        # Determine return code matching COBOL logic
        if self.records_error == 0:
            rc = RC_SUCCESS
        elif self.records_error <= self.records_read * 0.1:  # <= 10% errors
            rc = RC_WARNING
        else:
            rc = RC_ERROR

        logger.info("TRNVAL00 return code: %d", rc)
        return rc
