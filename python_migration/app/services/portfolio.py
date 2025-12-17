"""
Portfolio Service - migrated from INQPORT.cbl and related programs.
Handles portfolio inquiries and position management.
"""

from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import and_
from sqlalchemy.orm import Session

from app.models.database import (
    AuditLog,
    PortfolioMaster,
    PositionMaster,
)
from app.models.domain import (
    AuditAction,
    AuditStatus,
    AuditType,
)
from app.utils.exceptions import (
    PortfolioNotFoundError,
    PositionNotFoundError,
    ValidationError,
)


class PortfolioService:
    """
    Portfolio service for managing portfolios and positions.
    Replaces INQPORT.cbl functionality.
    """

    def __init__(self, db: Session):
        self.db = db

    def get_portfolio(self, portfolio_id: str) -> PortfolioMaster:
        """
        Get portfolio by ID.
        Replaces P200-GET-POSITION in INQPORT.cbl.
        """
        portfolio = self.db.query(PortfolioMaster).filter(
            PortfolioMaster.portfolio_id == portfolio_id.upper()
        ).first()

        if not portfolio:
            raise PortfolioNotFoundError(f"Portfolio not found: {portfolio_id}")

        return portfolio

    def get_portfolio_by_account(self, account_no: str) -> PortfolioMaster:
        """
        Get portfolio by account number.
        """
        portfolio = self.db.query(PortfolioMaster).filter(
            PortfolioMaster.client_id == account_no
        ).first()

        if not portfolio:
            raise PortfolioNotFoundError(f"Portfolio not found for account: {account_no}")

        return portfolio

    def get_positions(
        self,
        portfolio_id: str,
        position_date: date | None = None,
        status: str | None = None,
    ) -> list[PositionMaster]:
        """
        Get positions for a portfolio.
        Replaces VSAM file read in INQPORT.cbl.
        """
        query = self.db.query(PositionMaster).filter(
            PositionMaster.portfolio_id == portfolio_id.upper()
        )

        if position_date:
            query = query.filter(PositionMaster.position_date == position_date)

        if status:
            query = query.filter(PositionMaster.status == status)

        return query.all()

    def get_position(
        self,
        portfolio_id: str,
        investment_id: str,
        position_date: date | None = None,
    ) -> PositionMaster:
        """
        Get a specific position.
        """
        query = self.db.query(PositionMaster).filter(
            and_(
                PositionMaster.portfolio_id == portfolio_id.upper(),
                PositionMaster.investment_id == investment_id.upper(),
            )
        )

        if position_date:
            query = query.filter(PositionMaster.position_date == position_date)
        else:
            query = query.order_by(PositionMaster.position_date.desc())

        position = query.first()

        if not position:
            raise PositionNotFoundError(
                f"Position not found: {portfolio_id}/{investment_id}"
            )

        return position

    def get_portfolio_summary(self, portfolio_id: str) -> dict:
        """
        Get portfolio summary with total values.
        """
        portfolio = self.get_portfolio(portfolio_id)
        positions = self.get_positions(portfolio_id, status="A")

        total_cost_basis = sum(p.cost_basis or Decimal("0") for p in positions)
        total_market_value = sum(p.market_value or Decimal("0") for p in positions)
        total_gain_loss = total_market_value - total_cost_basis

        return {
            "portfolio_id": portfolio.portfolio_id,
            "portfolio_name": portfolio.portfolio_name,
            "client_id": portfolio.client_id,
            "status": portfolio.status,
            "position_count": len(positions),
            "total_cost_basis": float(total_cost_basis),
            "total_market_value": float(total_market_value),
            "total_gain_loss": float(total_gain_loss),
            "cash_balance": float(portfolio.cash_balance or Decimal("0")),
            "last_maint_date": portfolio.last_maint_date.isoformat() if portfolio.last_maint_date else None,
        }

    def create_portfolio(
        self,
        portfolio_id: str,
        client_id: str,
        portfolio_name: str,
        user_id: str,
        account_type: str = "IN",
        branch_id: str = "01",
        currency_code: str = "USD",
    ) -> PortfolioMaster:
        """
        Create a new portfolio.
        """
        if not portfolio_id.upper().startswith("PORT"):
            raise ValidationError("Portfolio ID must start with 'PORT'")

        existing = self.db.query(PortfolioMaster).filter(
            PortfolioMaster.portfolio_id == portfolio_id.upper()
        ).first()

        if existing:
            raise ValidationError(f"Portfolio already exists: {portfolio_id}")

        portfolio = PortfolioMaster(
            portfolio_id=portfolio_id.upper(),
            account_type=account_type,
            branch_id=branch_id,
            client_id=client_id,
            portfolio_name=portfolio_name,
            currency_code=currency_code,
            status="A",
            total_value=Decimal("0"),
            cash_balance=Decimal("0"),
            open_date=date.today(),
            last_maint_date=datetime.utcnow(),
            last_maint_user=user_id,
        )

        self.db.add(portfolio)
        self._log_audit(
            user_id=user_id,
            action=AuditAction.CREATE,
            portfolio_id=portfolio_id,
            message=f"Created portfolio: {portfolio_name}",
        )

        return portfolio

    def update_position(
        self,
        portfolio_id: str,
        investment_id: str,
        quantity: Decimal,
        cost_basis: Decimal,
        market_value: Decimal,
        user_id: str,
        position_date: date | None = None,
    ) -> PositionMaster:
        """
        Update or create a position.
        Replaces position update logic from POSUPD00.cbl.
        """
        position_date = position_date or date.today()

        try:
            position = self.get_position(portfolio_id, investment_id, position_date)
            before_image = f"qty={position.quantity},cost={position.cost_basis}"
            position.quantity = quantity
            position.cost_basis = cost_basis
            position.market_value = market_value
            position.last_maint_date = datetime.utcnow()
            position.last_maint_user = user_id
            after_image = f"qty={quantity},cost={cost_basis}"

            self._log_audit(
                user_id=user_id,
                action=AuditAction.UPDATE,
                portfolio_id=portfolio_id,
                before_image=before_image,
                after_image=after_image,
                message=f"Updated position: {investment_id}",
            )
        except PositionNotFoundError:
            position = PositionMaster(
                portfolio_id=portfolio_id.upper(),
                investment_id=investment_id.upper(),
                position_date=position_date,
                quantity=quantity,
                cost_basis=cost_basis,
                market_value=market_value,
                currency_code="USD",
                status="A",
                last_maint_date=datetime.utcnow(),
                last_maint_user=user_id,
            )
            self.db.add(position)

            self._log_audit(
                user_id=user_id,
                action=AuditAction.CREATE,
                portfolio_id=portfolio_id,
                message=f"Created position: {investment_id}",
            )

        return position

    def _log_audit(
        self,
        user_id: str,
        action: AuditAction,
        portfolio_id: str | None = None,
        account_no: str | None = None,
        before_image: str | None = None,
        after_image: str | None = None,
        message: str | None = None,
    ) -> None:
        """Log an audit entry."""
        audit = AuditLog(
            timestamp=datetime.utcnow(),
            system_id="PORTMGMT",
            user_id=user_id,
            program="PORTSVC",
            audit_type=AuditType.TRANSACTION.value,
            action=action.value,
            status=AuditStatus.SUCCESS.value,
            portfolio_id=portfolio_id,
            account_no=account_no,
            before_image=before_image,
            after_image=after_image,
            message=message,
        )
        self.db.add(audit)
