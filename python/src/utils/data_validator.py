"""
Data integrity validator translated from COBOL program UTLVAL00.cbl.

Replaces:
  - UTLVAL00.cbl 1000-VALIDATE-INTEGRITY: Check referential integrity
  - UTLVAL00.cbl 2000-VALIDATE-XREF: Cross-reference validation
  - UTLVAL00.cbl 3000-VALIDATE-FORMAT: Data format validation
  - UTLVAL00.cbl 4000-VALIDATE-BALANCE: Balance validation

Data integrity validation utilities.
"""

import logging
from decimal import Decimal

from sqlalchemy import and_, func, select
from sqlalchemy.orm import Session

from src.common.constants import ValidationType
from src.db.tables import InvestmentPosition, PortfolioMaster, TransactionHistory

logger = logging.getLogger(__name__)


class ValidationIssue:
    """Data validation issue."""

    def __init__(
        self,
        validation_type: ValidationType,
        entity: str,
        entity_id: str,
        message: str,
    ) -> None:
        self.validation_type = validation_type
        self.entity = entity
        self.entity_id = entity_id
        self.message = message

    def to_dict(self) -> dict[str, str]:
        return {
            "type": self.validation_type.value,
            "entity": self.entity,
            "entity_id": self.entity_id,
            "message": self.message,
        }


class DataValidator:
    """
    Data integrity validator.

    Translates UTLVAL00.cbl paragraph structure.
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    def validate_all(self) -> list[ValidationIssue]:
        """
        Run all validation checks.

        Returns list of issues found.
        """
        issues: list[ValidationIssue] = []
        issues.extend(self._validate_integrity())
        issues.extend(self._validate_xref())
        issues.extend(self._validate_format())
        issues.extend(self._validate_balance())
        return issues

    def _validate_integrity(self) -> list[ValidationIssue]:
        """
        Check referential integrity.

        Translates UTLVAL00.cbl 1000-VALIDATE-INTEGRITY:
          - Positions must reference valid portfolios
          - Transactions must reference valid portfolios
        """
        issues: list[ValidationIssue] = []

        # Check positions with missing portfolios
        orphan_positions = self._session.execute(
            select(InvestmentPosition.portfolio_id)
            .distinct()
            .where(
                ~InvestmentPosition.portfolio_id.in_(
                    select(PortfolioMaster.portfolio_id)
                )
            )
        ).scalars().all()

        for portfolio_id in orphan_positions:
            issues.append(
                ValidationIssue(
                    ValidationType.INTEGRITY,
                    "InvestmentPosition",
                    portfolio_id,
                    f"Position references non-existent portfolio: {portfolio_id}",
                )
            )

        # Check transactions with missing portfolios
        orphan_txns = self._session.execute(
            select(TransactionHistory.portfolio_id)
            .distinct()
            .where(
                ~TransactionHistory.portfolio_id.in_(
                    select(PortfolioMaster.portfolio_id)
                )
            )
        ).scalars().all()

        for portfolio_id in orphan_txns:
            issues.append(
                ValidationIssue(
                    ValidationType.INTEGRITY,
                    "TransactionHistory",
                    portfolio_id,
                    f"Transaction references non-existent portfolio: {portfolio_id}",
                )
            )

        logger.info("Integrity validation: %d issues found", len(issues))
        return issues

    def _validate_xref(self) -> list[ValidationIssue]:
        """
        Cross-reference validation.

        Translates UTLVAL00.cbl 2000-VALIDATE-XREF:
          - Active portfolios should have positions or recent activity
        """
        issues: list[ValidationIssue] = []

        # Check active portfolios with no positions and no transactions
        active_portfolios = self._session.execute(
            select(PortfolioMaster).where(PortfolioMaster.status == "A")
        ).scalars().all()

        for portfolio in active_portfolios:
            has_positions = self._session.execute(
                select(func.count()).select_from(InvestmentPosition).where(
                    InvestmentPosition.portfolio_id == portfolio.portfolio_id
                )
            ).scalar()

            has_transactions = self._session.execute(
                select(func.count()).select_from(TransactionHistory).where(
                    TransactionHistory.portfolio_id == portfolio.portfolio_id
                )
            ).scalar()

            if not has_positions and not has_transactions:
                issues.append(
                    ValidationIssue(
                        ValidationType.XREF,
                        "PortfolioMaster",
                        portfolio.portfolio_id,
                        f"Active portfolio has no positions or transactions: {portfolio.portfolio_id}",
                    )
                )

        logger.info("Cross-reference validation: %d issues found", len(issues))
        return issues

    def _validate_format(self) -> list[ValidationIssue]:
        """
        Data format validation.

        Translates UTLVAL00.cbl 3000-VALIDATE-FORMAT:
          - Portfolio ID format (PORT + 4 digits)
          - Status codes are valid values
        """
        issues: list[ValidationIssue] = []

        # Check portfolio ID format
        portfolios = self._session.execute(
            select(PortfolioMaster)
        ).scalars().all()

        import re
        id_pattern = re.compile(r"^PORT\d{4}$")

        for portfolio in portfolios:
            if not id_pattern.match(portfolio.portfolio_id):
                issues.append(
                    ValidationIssue(
                        ValidationType.FORMAT,
                        "PortfolioMaster",
                        portfolio.portfolio_id,
                        f"Invalid portfolio ID format: {portfolio.portfolio_id}",
                    )
                )

            if portfolio.status not in ("A", "C", "S", "P"):
                issues.append(
                    ValidationIssue(
                        ValidationType.FORMAT,
                        "PortfolioMaster",
                        portfolio.portfolio_id,
                        f"Invalid status code: {portfolio.status}",
                    )
                )

        logger.info("Format validation: %d issues found", len(issues))
        return issues

    def _validate_balance(self) -> list[ValidationIssue]:
        """
        Balance validation.

        Translates UTLVAL00.cbl 4000-VALIDATE-BALANCE:
          - Portfolio total value should match sum of position market values
        """
        issues: list[ValidationIssue] = []

        active_portfolios = self._session.execute(
            select(PortfolioMaster).where(PortfolioMaster.status == "A")
        ).scalars().all()

        for portfolio in active_portfolios:
            # Sum active position market values
            position_total = self._session.execute(
                select(func.sum(InvestmentPosition.market_value)).where(
                    and_(
                        InvestmentPosition.portfolio_id == portfolio.portfolio_id,
                        InvestmentPosition.status == "A",
                    )
                )
            ).scalar()

            position_total_dec = Decimal(str(position_total)) if position_total else Decimal("0.00")
            portfolio_value = Decimal(str(portfolio.total_value))

            # Allow small rounding differences
            diff = abs(portfolio_value - position_total_dec)
            if diff > Decimal("0.01"):
                issues.append(
                    ValidationIssue(
                        ValidationType.BALANCE,
                        "PortfolioMaster",
                        portfolio.portfolio_id,
                        f"Balance mismatch: portfolio={portfolio_value}, "
                        f"positions={position_total_dec}, diff={diff}",
                    )
                )

        logger.info("Balance validation: %d issues found", len(issues))
        return issues
