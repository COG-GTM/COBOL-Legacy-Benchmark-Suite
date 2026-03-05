"""
Portfolio CRUD service translated from COBOL programs:
- PORTMSTR.cbl (Main portfolio master file operations)
- PORTADD.cbl (Add new portfolio)
- PORTUPDT.cbl (Update existing portfolio)
- PORTDEL.cbl (Delete/close portfolio)
- PORTINQ.cbl (Portfolio inquiry)
"""

import logging
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.audit import write_audit_record
from src.common.constants import (
    AuditAction,
    AuditType,
    PortfolioStatus,
)
from src.common.error_handler import ValidationError
from src.db.repository import PortfolioRepository, PositionRepository
from src.db.tables import PortfolioMaster
from src.portfolio.validation import validate_portfolio_create, validate_portfolio_update

logger = logging.getLogger(__name__)


class PortfolioService:
    """
    Portfolio management operations.
    Translates PORTMSTR.cbl EVALUATE TRUE dispatcher and sub-programs.
    """

    def __init__(self, session: Session):
        self.session = session
        self.repo = PortfolioRepository(session)
        self.position_repo = PositionRepository(session)

    # -------------------------------------------------------------------
    # PORTADD.cbl: Add new portfolio
    # -------------------------------------------------------------------
    def create(
        self,
        portfolio_id: str,
        client_id: str,
        client_name: str,
        portfolio_name: str = "",
        account_type: str = "IN",
        branch_id: str = "00",
        currency_code: str = "USD",
        risk_level: str = "M",
        client_type: str = "I",
        user: str = "SYSTEM",
    ) -> PortfolioMaster:
        """
        Create a new portfolio.
        Translates PORTADD.cbl:
        1000-VALIDATE-INPUT → validate_portfolio_create()
        2000-CHECK-DUPLICATE → repo.exists()
        3000-WRITE-RECORD → repo.create()
        4000-WRITE-AUDIT → write_audit_record()
        """
        # 1000-VALIDATE-INPUT
        validate_portfolio_create(
            portfolio_id=portfolio_id,
            client_id=client_id,
            client_name=client_name,
            account_type=account_type,
            currency_code=currency_code,
            risk_level=risk_level,
        )

        # 2000-CHECK-DUPLICATE
        if self.repo.exists(portfolio_id):
            raise ValidationError(
                f"Portfolio {portfolio_id} already exists",
                field="portfolio_id",
                error_code="DUP1",
                program="PORTADD",
            )

        # 3000-WRITE-RECORD
        portfolio = PortfolioMaster(
            portfolio_id=portfolio_id,
            client_id=client_id,
            client_name=client_name,
            client_type=client_type,
            portfolio_name=portfolio_name or f"Portfolio {portfolio_id}",
            account_type=account_type,
            branch_id=branch_id,
            currency_code=currency_code,
            risk_level=risk_level,
            status=PortfolioStatus.ACTIVE.value,
            total_value=Decimal("0.00"),
            cash_balance=Decimal("0.00"),
            account_number=portfolio_id,
            open_date=date.today(),
            last_maint_date=datetime.now(),
            last_maint_user=user,
        )
        self.repo.create(portfolio)

        # 4000-WRITE-AUDIT
        write_audit_record(
            session=self.session,
            audit_type=AuditType.TRANSACTION,
            action=AuditAction.CREATE,
            user_id=user,
            program="PORTADD",
            key_info=portfolio_id,
            after_image=f"client={client_id} type={account_type}",
            message=f"Portfolio {portfolio_id} created",
        )

        logger.info("Portfolio created: %s for client %s", portfolio_id, client_id)
        return portfolio

    # -------------------------------------------------------------------
    # PORTUPDT.cbl: Update existing portfolio
    # -------------------------------------------------------------------
    def update(
        self,
        portfolio_id: str,
        user: str = "SYSTEM",
        **kwargs,
    ) -> PortfolioMaster:
        """
        Update an existing portfolio.
        Translates PORTUPDT.cbl:
        1000-READ-CURRENT → repo.get_by_id()
        2000-VALIDATE-CHANGES → validate_portfolio_update()
        3000-REWRITE-RECORD → repo.update()
        4000-WRITE-AUDIT → write_audit_record()
        """
        # 1000-READ-CURRENT
        portfolio = self.repo.get_by_id(portfolio_id)
        if portfolio is None:
            raise ValidationError(
                f"Portfolio {portfolio_id} not found",
                field="portfolio_id",
                error_code="NF01",
                program="PORTUPDT",
            )

        before_image = f"status={portfolio.status} risk={portfolio.risk_level}"

        # 2000-VALIDATE-CHANGES
        validate_portfolio_update(portfolio, **kwargs)

        # 3000-REWRITE-RECORD
        updatable_fields = {
            "portfolio_name", "currency_code", "risk_level", "status",
            "client_name", "client_type", "cash_balance", "total_value",
            "branch_id",
        }
        for key, value in kwargs.items():
            if key in updatable_fields and value is not None:
                setattr(portfolio, key, value)

        portfolio.last_maint_date = datetime.now()
        portfolio.last_maint_user = user
        self.repo.update(portfolio)

        # 4000-WRITE-AUDIT
        after_image = f"status={portfolio.status} risk={portfolio.risk_level}"
        write_audit_record(
            session=self.session,
            audit_type=AuditType.TRANSACTION,
            action=AuditAction.UPDATE,
            user_id=user,
            program="PORTUPDT",
            key_info=portfolio_id,
            before_image=before_image,
            after_image=after_image,
            message=f"Portfolio {portfolio_id} updated",
        )

        logger.info("Portfolio updated: %s", portfolio_id)
        return portfolio

    # -------------------------------------------------------------------
    # PORTDEL.cbl: Delete/close portfolio
    # -------------------------------------------------------------------
    def delete(
        self,
        portfolio_id: str,
        user: str = "SYSTEM",
        reason: str = "",
    ) -> PortfolioMaster:
        """
        Close a portfolio (soft delete).
        Translates PORTDEL.cbl:
        1000-READ-PORTFOLIO → repo.get_by_id()
        2000-VERIFY-POSITIONS → repo.has_open_positions()
        3000-UPDATE-STATUS → set status to CLOSED
        4000-WRITE-AUDIT → write_audit_record()
        """
        portfolio = self.repo.get_by_id(portfolio_id)
        if portfolio is None:
            raise ValidationError(
                f"Portfolio {portfolio_id} not found",
                field="portfolio_id",
                error_code="NF01",
                program="PORTDEL",
            )

        # 2000-VERIFY-POSITIONS
        if self.repo.has_open_positions(portfolio_id):
            raise ValidationError(
                f"Cannot close portfolio {portfolio_id}: has open positions",
                field="portfolio_id",
                error_code="OP01",
                program="PORTDEL",
            )

        # 3000-UPDATE-STATUS
        portfolio.status = PortfolioStatus.CLOSED.value
        portfolio.close_date = date.today()
        portfolio.last_maint_date = datetime.now()
        portfolio.last_maint_user = user
        self.repo.update(portfolio)

        # 4000-WRITE-AUDIT
        write_audit_record(
            session=self.session,
            audit_type=AuditType.TRANSACTION,
            action=AuditAction.DELETE,
            user_id=user,
            program="PORTDEL",
            key_info=portfolio_id,
            message=f"Portfolio {portfolio_id} closed. Reason: {reason}",
        )

        logger.info("Portfolio closed: %s", portfolio_id)
        return portfolio

    # -------------------------------------------------------------------
    # PORTINQ.cbl: Portfolio inquiry
    # -------------------------------------------------------------------
    def get_by_id(self, portfolio_id: str) -> PortfolioMaster | None:
        """Read and return portfolio details. Translates PORTINQ.cbl."""
        return self.repo.get_by_id(portfolio_id)

    def list_by_client(self, client_id: str) -> list[PortfolioMaster]:
        return self.repo.list_by_client(client_id)

    def list_by_branch(self, branch_id: str) -> list[PortfolioMaster]:
        return self.repo.list_by_branch(branch_id)

    def list_all(
        self,
        client_id: str | None = None,
        branch_id: str | None = None,
        status: str | None = None,
        limit: int = 100,
        offset: int = 0,
    ) -> list[PortfolioMaster]:
        return self.repo.list_all(
            client_id=client_id,
            branch_id=branch_id,
            status=status,
            limit=limit,
            offset=offset,
        )
