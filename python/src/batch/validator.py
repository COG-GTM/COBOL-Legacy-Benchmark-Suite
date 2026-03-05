"""
Transaction validator translated from COBOL program TRNVAL00.cbl.

Validates incoming transaction records:
- Check for duplicate transactions
- Validate against business rules
- Report validation errors
"""

import logging
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.constants import (
    VALID_CURRENCIES,
    PositionStatus,
    ReturnCode,
    TransactionType,
)
from src.db.repository import (
    PortfolioRepository,
    PositionRepository,
    TransactionRepository,
)
from src.db.tables import TransactionHistory

logger = logging.getLogger(__name__)


class TransactionValidator:
    """
    Validate incoming transaction records.
    Translates TRNVAL00.cbl validation logic.
    """

    def __init__(self, session: Session):
        self.session = session
        self.portfolio_repo = PortfolioRepository(session)
        self.position_repo = PositionRepository(session)
        self.trn_repo = TransactionRepository(session)
        self.errors: list[str] = []
        self.records_validated: int = 0
        self.records_rejected: int = 0

    def validate_transaction(self, transaction: TransactionHistory) -> ReturnCode:
        """
        Validate a single transaction record.
        Translates TRNVAL00.cbl 2000-VALIDATE-TRANSACTION.
        """
        self.records_validated += 1
        errors: list[str] = []

        # 2100-CHECK-REQUIRED-FIELDS
        errors.extend(self._check_required_fields(transaction))

        # 2200-CHECK-DUPLICATE
        if self._check_duplicate(transaction):
            errors.append(f"Duplicate transaction: {transaction.portfolio_id}/{transaction.trn_date}/{transaction.sequence_no}")

        # 2300-VALIDATE-PORTFOLIO
        errors.extend(self._validate_portfolio(transaction))

        # 2400-VALIDATE-BUSINESS-RULES
        errors.extend(self._validate_business_rules(transaction))

        if errors:
            self.records_rejected += 1
            self.errors.extend(errors)
            logger.warning("Transaction validation failed: %s", "; ".join(errors))
            return ReturnCode.ERROR

        return ReturnCode.SUCCESS

    def validate_batch(self, transactions: list[TransactionHistory]) -> ReturnCode:
        """Validate a batch of transactions."""
        max_rc = ReturnCode.SUCCESS
        for trn in transactions:
            rc = self.validate_transaction(trn)
            if rc.value > max_rc.value:
                max_rc = rc
        return max_rc

    def _check_required_fields(self, trn: TransactionHistory) -> list[str]:
        """Translates TRNVAL00.cbl 2100-CHECK-REQUIRED-FIELDS."""
        errors = []

        if not trn.portfolio_id or not trn.portfolio_id.strip():
            errors.append("Portfolio ID is required")
        if not trn.investment_id or not trn.investment_id.strip():
            errors.append("Investment ID is required")

        valid_types = {e.value for e in TransactionType}
        if trn.trn_type not in valid_types:
            errors.append(f"Invalid transaction type: {trn.trn_type}")

        quantity = Decimal(str(trn.quantity))
        if quantity <= 0:
            errors.append(f"Quantity must be positive: {quantity}")

        price = Decimal(str(trn.price))
        if price < 0:
            errors.append(f"Price must be non-negative: {price}")

        if trn.currency_code not in VALID_CURRENCIES:
            errors.append(f"Invalid currency: {trn.currency_code}")

        return errors

    def _check_duplicate(self, trn: TransactionHistory) -> bool:
        """Translates TRNVAL00.cbl 2200-CHECK-DUPLICATE."""
        return self.trn_repo.check_duplicate(
            trn.trn_date, trn.trn_time, trn.portfolio_id, trn.sequence_no
        )

    def _validate_portfolio(self, trn: TransactionHistory) -> list[str]:
        """Translates TRNVAL00.cbl 2300-VALIDATE-PORTFOLIO."""
        errors = []
        portfolio = self.portfolio_repo.get_by_id(trn.portfolio_id)
        if portfolio is None:
            errors.append(f"Portfolio not found: {trn.portfolio_id}")
        elif portfolio.status != "A":
            errors.append(f"Portfolio {trn.portfolio_id} is not active (status={portfolio.status})")
        return errors

    def _validate_business_rules(self, trn: TransactionHistory) -> list[str]:
        """Translates TRNVAL00.cbl 2400-VALIDATE-BUSINESS-RULES."""
        errors = []

        if trn.trn_type == TransactionType.SELL.value:
            # Check sufficient quantity
            position = self.position_repo.get_latest(trn.portfolio_id, trn.investment_id)
            if position is None:
                errors.append(f"No position found for sell: {trn.investment_id}")
            elif position.status != PositionStatus.ACTIVE.value:
                errors.append(f"Position not active for sell: {trn.investment_id}")
            else:
                sell_qty = Decimal(str(trn.quantity))
                held_qty = Decimal(str(position.quantity))
                if sell_qty > held_qty:
                    errors.append(
                        f"Insufficient quantity for sell: have {held_qty}, selling {sell_qty}"
                    )

        return errors

    def get_summary(self) -> dict:
        """Get validation summary."""
        return {
            "records_validated": self.records_validated,
            "records_rejected": self.records_rejected,
            "records_accepted": self.records_validated - self.records_rejected,
            "errors": self.errors,
        }
