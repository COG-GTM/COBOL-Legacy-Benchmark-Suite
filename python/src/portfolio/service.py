"""
Portfolio CRUD service translated from COBOL programs:
  - PORTMSTR.cbl: Main portfolio master file operations
  - PORTADD.cbl: Add new portfolio
  - PORTUPDT.cbl: Update existing portfolio
  - PORTDEL.cbl: Delete/close portfolio
  - PORTINQ.cbl: Portfolio inquiry

Each COBOL paragraph becomes a private method.
Each COBOL CALL becomes a method call or service injection.
All VSAM I/O replaced with SQLAlchemy queries via PortfolioRepository.
"""

import logging
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.audit import AuditService
from src.common.constants import (
    AuditAction,
    PortfolioStatus,
)
from src.common.error_handler import DuplicateError, NotFoundError, ValidationError
from src.db.repository import PortfolioRepository, PositionRepository, TransactionRepository
from src.db.tables import PortfolioMaster
from src.models.portfolio import PortfolioRecord
from src.portfolio.validation import (
    validate_account_no,
    validate_account_type,
    validate_currency_code,
    validate_dates,
    validate_portfolio_for_closure,
    validate_portfolio_id,
    validate_risk_level,
    validate_status,
)

logger = logging.getLogger(__name__)


class PortfolioService:
    """
    Portfolio CRUD service.

    Translates PORTMSTR.cbl dispatch (CREATE-PORT, READ-PORT, UPDATE-PORT, DELETE-PORT)
    and related programs (PORTADD, PORTUPDT, PORTDEL, PORTINQ).
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._repo = PortfolioRepository(session)
        self._position_repo = PositionRepository(session)
        self._transaction_repo = TransactionRepository(session)
        self._audit = AuditService(session)

    # ------------------------------------------------------------------
    # CREATE - translates PORTADD.cbl
    # ------------------------------------------------------------------
    def create(self, record: PortfolioRecord, user_id: str = "SYSTEM") -> PortfolioMaster:
        """
        Add a new portfolio.

        Translates PORTADD.cbl:
          1000-VALIDATE-INPUT    -> _validate_create_input()
          2000-CHECK-DUPLICATE   -> repository.get_by_id()
          3000-WRITE-RECORD      -> repository.create()
          4000-WRITE-AUDIT       -> audit.log_portfolio_change()

        Raises:
            ValidationError: If input validation fails.
            DuplicateError: If portfolio already exists (VSAM status 22 / SQLCODE -803).
        """
        self._validate_create_input(record)

        # 2000-CHECK-DUPLICATE
        existing = self._repo.get_by_id(record.portfolio_id)
        if existing is not None:
            raise DuplicateError(f"Portfolio already exists: {record.portfolio_id}")

        # 3000-WRITE-RECORD
        now = datetime.now()
        portfolio = PortfolioMaster(
            portfolio_id=record.portfolio_id,
            account_no=record.account_no,
            account_type=record.account_type.value,
            branch_id=record.branch_id,
            client_id=record.client_id,
            portfolio_name=record.portfolio_name,
            currency_code=record.currency_code.value,
            risk_level=record.risk_level.value,
            client_name=record.client_name,
            client_type=record.client_type.value,
            status=PortfolioStatus.ACTIVE.value,
            open_date=record.open_date,
            close_date=None,
            create_date=date.today(),
            total_value=Decimal("0.00"),
            cash_balance=record.cash_balance,
            last_maint_date=now,
            last_maint_user=user_id,
            last_trans_date=None,
        )
        self._repo.create(portfolio)

        # 4000-WRITE-AUDIT
        self._audit.log_portfolio_change(
            user_id=user_id,
            action=AuditAction.CREATE,
            portfolio_id=record.portfolio_id,
            after_image=f"Created: {record.portfolio_name}",
        )

        logger.info("Portfolio created: %s", record.portfolio_id)
        return portfolio

    # ------------------------------------------------------------------
    # READ / INQUIRY - translates PORTINQ.cbl
    # ------------------------------------------------------------------
    def get_by_id(self, portfolio_id: str) -> PortfolioMaster:
        """
        Read portfolio by ID.

        Translates PORTINQ.cbl / PORTMSTR.cbl 2000-READ-PORT:
          READ PORTMSTR-FILE INTO PORT-RECORD KEY IS PORT-ID
          IF FILE-STATUS = '23' -> NotFoundError

        Raises:
            NotFoundError: If portfolio not found (VSAM status 23).
        """
        portfolio = self._repo.get_by_id(portfolio_id)
        if portfolio is None:
            raise NotFoundError(f"Portfolio not found: {portfolio_id}")
        return portfolio

    def list_by_client(self, client_id: str) -> list[PortfolioMaster]:
        """List all portfolios for a client."""
        return self._repo.list_by_client(client_id)

    def list_by_branch(self, branch_id: str) -> list[PortfolioMaster]:
        """List all portfolios for a branch."""
        return self._repo.list_by_branch(branch_id)

    def list_all(
        self,
        offset: int = 0,
        limit: int = 100,
        status: str | None = None,
        branch_id: str | None = None,
        client_id: str | None = None,
    ) -> list[PortfolioMaster]:
        """List portfolios with optional filters."""
        return self._repo.list_all(
            offset=offset, limit=limit, status=status, branch_id=branch_id, client_id=client_id,
        )

    # ------------------------------------------------------------------
    # UPDATE - translates PORTUPDT.cbl
    # ------------------------------------------------------------------
    def update(
        self, portfolio_id: str, updates: dict[str, object], user_id: str = "SYSTEM"
    ) -> PortfolioMaster:
        """
        Update an existing portfolio.

        Translates PORTUPDT.cbl:
          1000-READ-CURRENT      -> get_by_id()
          2000-VALIDATE-CHANGES  -> _validate_update()
          3000-REWRITE-RECORD    -> repository.update()
          4000-WRITE-AUDIT       -> audit.log_portfolio_change()

        Raises:
            NotFoundError: If portfolio not found.
            ValidationError: If validation fails.
        """
        portfolio = self.get_by_id(portfolio_id)
        before_image = f"Status={portfolio.status}, Name={portfolio.portfolio_name}"

        # 2000-VALIDATE-CHANGES
        self._validate_update(portfolio, updates)

        # 3000-REWRITE-RECORD - apply updates
        allowed_fields = {
            "portfolio_name", "currency_code", "risk_level", "status",
            "client_name", "client_type", "account_type", "branch_id",
            "close_date", "cash_balance",
        }
        for field, value in updates.items():
            if field in allowed_fields and hasattr(portfolio, field):
                setattr(portfolio, field, value)

        portfolio.last_maint_date = datetime.now()
        portfolio.last_maint_user = user_id
        self._repo.update(portfolio)

        # 4000-WRITE-AUDIT
        after_image = f"Status={portfolio.status}, Name={portfolio.portfolio_name}"
        self._audit.log_portfolio_change(
            user_id=user_id,
            action=AuditAction.UPDATE,
            portfolio_id=portfolio_id,
            before_image=before_image,
            after_image=after_image,
        )

        logger.info("Portfolio updated: %s", portfolio_id)
        return portfolio

    # ------------------------------------------------------------------
    # DELETE / CLOSE - translates PORTDEL.cbl
    # ------------------------------------------------------------------
    def delete(self, portfolio_id: str, user_id: str = "SYSTEM") -> PortfolioMaster:
        """
        Close/delete a portfolio.

        Translates PORTDEL.cbl:
          1000-READ-PORTFOLIO     -> get_by_id()
          2000-VERIFY-POSITIONS   -> check open positions
          3000-UPDATE-STATUS      -> set status to CLOSED
          4000-WRITE-AUDIT        -> audit.log_portfolio_change()

        Does not physically delete; sets status to CLOSED per business rules.

        Raises:
            NotFoundError: If portfolio not found.
            ValidationError: If portfolio has open positions/pending transactions.
        """
        portfolio = self.get_by_id(portfolio_id)

        # 2000-VERIFY-POSITIONS
        open_positions = self._position_repo.list_by_portfolio(portfolio_id)
        has_open = any(p.status == "A" for p in open_positions)
        pending_count = self._transaction_repo.count_by_status("P")
        validate_portfolio_for_closure(has_open, pending_count > 0)

        # 3000-UPDATE-STATUS
        before_image = f"Status={portfolio.status}"
        portfolio.status = PortfolioStatus.CLOSED.value
        portfolio.close_date = date.today()
        portfolio.last_maint_date = datetime.now()
        portfolio.last_maint_user = user_id
        self._repo.update(portfolio)

        # 4000-WRITE-AUDIT
        self._audit.log_portfolio_change(
            user_id=user_id,
            action=AuditAction.DELETE,
            portfolio_id=portfolio_id,
            before_image=before_image,
            after_image=f"Status={PortfolioStatus.CLOSED.value}",
        )

        logger.info("Portfolio closed: %s", portfolio_id)
        return portfolio

    # ------------------------------------------------------------------
    # Private validation methods
    # ------------------------------------------------------------------
    def _validate_create_input(self, record: PortfolioRecord) -> None:
        """
        Validate input for portfolio creation.

        Translates PORTADD.cbl 1000-VALIDATE-INPUT paragraph.
        """
        validate_portfolio_id(record.portfolio_id)
        if record.account_no:
            validate_account_no(record.account_no)
        validate_account_type(record.account_type.value)
        validate_risk_level(record.risk_level.value)
        validate_currency_code(record.currency_code.value)
        validate_dates(record.open_date, record.close_date)

    def _validate_update(
        self, portfolio: PortfolioMaster, updates: dict[str, object]
    ) -> None:
        """
        Validate update changes.

        Translates PORTUPDT.cbl 2000-VALIDATE-CHANGES paragraph.
        """
        if portfolio.status == PortfolioStatus.CLOSED.value:
            raise ValidationError("Cannot update a closed portfolio", field="status")

        if "risk_level" in updates:
            validate_risk_level(str(updates["risk_level"]))
        if "currency_code" in updates:
            validate_currency_code(str(updates["currency_code"]))
        if "status" in updates:
            validate_status(str(updates["status"]))
