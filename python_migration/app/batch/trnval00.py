"""
Transaction Validation Batch Processor - migrated from TRNVAL00.cbl.

Original COBOL Program: TRNVAL00.cbl
Purpose: Validates incoming financial transactions before processing

Key Functions:
- P100-INIT: Initialize program, open files
- P200-PROCESS: Main processing loop
- P300-VALIDATE-TRANSACTION: Validate individual transaction
- P400-WRITE-OUTPUT: Write validated transactions
- P900-TERMINATE: Close files, write statistics
- 9000-ERROR-ROUTINE: Error handling

Validation Rules:
- Portfolio ID must exist
- Transaction type must be valid (BU/SL/TR/FE)
- Quantity must be positive
- Price must be positive
- Amount must match quantity * price
- Currency must be valid
"""

from datetime import date, datetime
from decimal import Decimal

import pandas as pd
from sqlalchemy.orm import Session

from app.batch.base import BatchProcessor
from app.models.database import PortfolioMaster, TransactionHistory
from app.models.domain import TransactionStatus
from app.utils.exceptions import ValidationError


class TransactionValidator(BatchProcessor):
    """
    Transaction validation batch processor.
    Replaces TRNVAL00.cbl functionality.
    """

    VALID_TRANSACTION_TYPES = {"BU", "SL", "TR", "FE"}
    VALID_CURRENCIES = {"USD", "EUR", "GBP", "JPY", "CAD", "CHF"}

    def __init__(
        self,
        db: Session,
        input_data: pd.DataFrame | list[dict] | None = None,
        process_date: date | None = None,
    ):
        super().__init__(
            db=db,
            job_name="TRNVAL",
            program_name="TRNVAL00",
            process_date=process_date,
        )

        if input_data is None:
            self.input_df = pd.DataFrame()
        elif isinstance(input_data, pd.DataFrame):
            self.input_df = input_data
        else:
            self.input_df = pd.DataFrame(input_data)

        self.valid_transactions: list[dict] = []
        self.invalid_transactions: list[dict] = []
        self.portfolio_cache: dict[str, bool] = {}

    def initialize(self) -> None:
        """
        Initialize validation processing.
        Replaces P100-INIT in TRNVAL00.cbl.
        """
        self.logger.info(
            "Initializing transaction validation",
            input_count=len(self.input_df),
        )

        self._load_portfolio_cache()

    def process(self) -> None:
        """
        Main processing loop.
        Replaces P200-PROCESS in TRNVAL00.cbl.
        """
        for idx, row in self.input_df.iterrows():
            self.records_read += 1

            if self.restart_key and str(idx) <= self.restart_key:
                continue

            try:
                transaction = self._validate_transaction(row.to_dict())
                self.valid_transactions.append(transaction)
                self.records_processed += 1

                if self.should_checkpoint():
                    self.checkpoint(str(idx))

            except ValidationError as e:
                self.error_logger.log_validation_error(
                    message=str(e),
                    field=e.details,
                )
                self.invalid_transactions.append({
                    "record": row.to_dict(),
                    "error": str(e),
                })
                if not self.increment_error_count():
                    raise

        self._write_valid_transactions()

    def terminate(self) -> None:
        """
        Terminate validation processing.
        Replaces P900-TERMINATE in TRNVAL00.cbl.
        """
        self.logger.info(
            "Transaction validation complete",
            records_read=self.records_read,
            records_valid=len(self.valid_transactions),
            records_invalid=len(self.invalid_transactions),
        )

        if self.invalid_transactions:
            self.return_code = max(self.return_code, 4)

    def _load_portfolio_cache(self) -> None:
        """Load portfolio IDs into cache for validation."""
        portfolios = self.db.query(PortfolioMaster.portfolio_id).all()
        self.portfolio_cache = {p.portfolio_id: True for p in portfolios}

    def _validate_transaction(self, record: dict) -> dict:
        """
        Validate a single transaction record.
        Replaces P300-VALIDATE-TRANSACTION in TRNVAL00.cbl.

        Validation rules from COBOL:
        - IF TRN-PORTFOLIO-ID = SPACES -> INVALID
        - IF TRN-TYPE NOT = 'BU' 'SL' 'TR' 'FE' -> INVALID
        - IF TRN-QUANTITY <= 0 -> INVALID
        - IF TRN-PRICE <= 0 -> INVALID
        - IF TRN-AMOUNT NOT = TRN-QUANTITY * TRN-PRICE -> INVALID
        """
        portfolio_id = str(record.get("portfolio_id", "")).strip().upper()
        if not portfolio_id:
            raise ValidationError(
                "Portfolio ID is required",
                details="portfolio_id",
                program="TRNVAL00",
            )

        if portfolio_id not in self.portfolio_cache:
            raise ValidationError(
                f"Portfolio not found: {portfolio_id}",
                details="portfolio_id",
                program="TRNVAL00",
            )

        transaction_type = str(record.get("transaction_type", "")).strip().upper()
        if transaction_type not in self.VALID_TRANSACTION_TYPES:
            raise ValidationError(
                f"Invalid transaction type: {transaction_type}",
                details="transaction_type",
                program="TRNVAL00",
            )

        investment_id = str(record.get("investment_id", "")).strip().upper()
        if not investment_id:
            raise ValidationError(
                "Investment ID is required",
                details="investment_id",
                program="TRNVAL00",
            )

        try:
            quantity = Decimal(str(record.get("quantity", 0)))
        except (ValueError, TypeError):
            raise ValidationError(
                "Invalid quantity format",
                details="quantity",
                program="TRNVAL00",
            )

        if quantity <= 0:
            raise ValidationError(
                f"Quantity must be positive: {quantity}",
                details="quantity",
                program="TRNVAL00",
            )

        try:
            price = Decimal(str(record.get("price", 0)))
        except (ValueError, TypeError):
            raise ValidationError(
                "Invalid price format",
                details="price",
                program="TRNVAL00",
            )

        if price <= 0:
            raise ValidationError(
                f"Price must be positive: {price}",
                details="price",
                program="TRNVAL00",
            )

        expected_amount = quantity * price
        provided_amount = record.get("amount")

        if provided_amount is not None:
            try:
                provided_amount = Decimal(str(provided_amount))
                tolerance = Decimal("0.01")
                if abs(provided_amount - expected_amount) > tolerance:
                    raise ValidationError(
                        f"Amount mismatch: expected {expected_amount}, got {provided_amount}",
                        details="amount",
                        program="TRNVAL00",
                    )
            except (ValueError, TypeError):
                raise ValidationError(
                    "Invalid amount format",
                    details="amount",
                    program="TRNVAL00",
                )

        currency = str(record.get("currency", "USD")).strip().upper()
        if currency not in self.VALID_CURRENCIES:
            raise ValidationError(
                f"Invalid currency: {currency}",
                details="currency",
                program="TRNVAL00",
            )

        fees = Decimal(str(record.get("fees", 0)))
        if fees < 0:
            raise ValidationError(
                f"Fees cannot be negative: {fees}",
                details="fees",
                program="TRNVAL00",
            )

        return {
            "portfolio_id": portfolio_id,
            "investment_id": investment_id,
            "transaction_type": transaction_type,
            "quantity": quantity,
            "price": price,
            "amount": expected_amount,
            "fees": fees,
            "total_amount": expected_amount + fees,
            "currency": currency,
            "transaction_date": record.get("transaction_date", date.today()),
            "transaction_time": record.get("transaction_time", datetime.utcnow().time()),
        }

    def _write_valid_transactions(self) -> None:
        """
        Write validated transactions to database.
        Replaces P400-WRITE-OUTPUT in TRNVAL00.cbl.
        """
        for txn in self.valid_transactions:
            transaction_id = self._generate_transaction_id()

            transaction = TransactionHistory(
                transaction_id=transaction_id,
                portfolio_id=txn["portfolio_id"],
                transaction_date=txn["transaction_date"],
                transaction_time=txn["transaction_time"],
                investment_id=txn["investment_id"],
                transaction_type=txn["transaction_type"],
                quantity=txn["quantity"],
                price=txn["price"],
                amount=txn["amount"],
                fees=txn["fees"],
                total_amount=txn["total_amount"],
                currency_code=txn["currency"],
                status=TransactionStatus.PENDING.value,
                process_date=datetime.utcnow(),
                process_user="TRNVAL00",
            )

            self.db.add(transaction)
            self.records_written += 1

    def _generate_transaction_id(self) -> str:
        """Generate unique transaction ID."""
        now = datetime.utcnow()
        timestamp = now.strftime("%Y%m%d%H%M%S")
        sequence = str(self.records_written + 1).zfill(6)
        return f"{timestamp}{sequence}"

    def get_results(self) -> dict:
        """Get validation results."""
        return {
            "records_read": self.records_read,
            "records_valid": len(self.valid_transactions),
            "records_invalid": len(self.invalid_transactions),
            "return_code": self.return_code,
            "invalid_transactions": self.invalid_transactions,
        }
