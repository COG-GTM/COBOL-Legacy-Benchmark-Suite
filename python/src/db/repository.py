"""
Repository layer providing CRUD operations translated from COBOL VSAM I/O
and DB2 embedded SQL patterns.

Replaces:
- VSAM READ/WRITE/REWRITE/DELETE/START/READNEXT
- DB2 SELECT/INSERT/UPDATE/DELETE statements
"""

import logging
from datetime import date, datetime

from sqlalchemy import and_, func, select
from sqlalchemy.orm import Session

from src.db.tables import (
    AuditLog,
    BatchControl,
    ErrorLog,
    InvestmentPosition,
    PortfolioMaster,
    ProcessSequence,
    TransactionHistory,
    UserAuth,
)

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Portfolio Repository (replaces VSAM PORTMSTR I/O)
# ---------------------------------------------------------------------------
class PortfolioRepository:
    """CRUD for PORTFOLIO_MASTER. Replaces VSAM PORTMSTR KSDS operations."""

    def __init__(self, session: Session):
        self.session = session

    def create(self, portfolio: PortfolioMaster) -> PortfolioMaster:
        """VSAM WRITE equivalent."""
        self.session.add(portfolio)
        self.session.flush()
        logger.debug("Portfolio created: %s", portfolio.portfolio_id)
        return portfolio

    def get_by_id(self, portfolio_id: str) -> PortfolioMaster | None:
        """VSAM READ by primary key."""
        return self.session.get(PortfolioMaster, portfolio_id)

    def update(self, portfolio: PortfolioMaster) -> PortfolioMaster:
        """VSAM REWRITE equivalent."""
        self.session.merge(portfolio)
        self.session.flush()
        logger.debug("Portfolio updated: %s", portfolio.portfolio_id)
        return portfolio

    def delete(self, portfolio_id: str) -> bool:
        """VSAM DELETE equivalent."""
        portfolio = self.get_by_id(portfolio_id)
        if portfolio:
            self.session.delete(portfolio)
            self.session.flush()
            logger.debug("Portfolio deleted: %s", portfolio_id)
            return True
        return False

    def list_by_client(self, client_id: str) -> list[PortfolioMaster]:
        """DB2 SELECT WHERE CLIENT_ID = ?"""
        stmt = select(PortfolioMaster).where(PortfolioMaster.client_id == client_id)
        return list(self.session.scalars(stmt).all())

    def list_by_branch(self, branch_id: str) -> list[PortfolioMaster]:
        """DB2 SELECT WHERE BRANCH_ID = ?"""
        stmt = select(PortfolioMaster).where(PortfolioMaster.branch_id == branch_id)
        return list(self.session.scalars(stmt).all())

    def list_by_status(self, status: str) -> list[PortfolioMaster]:
        """DB2 SELECT WHERE STATUS = ?"""
        stmt = select(PortfolioMaster).where(PortfolioMaster.status == status)
        return list(self.session.scalars(stmt).all())

    def list_all(
        self,
        client_id: str | None = None,
        branch_id: str | None = None,
        status: str | None = None,
        limit: int = 100,
        offset: int = 0,
    ) -> list[PortfolioMaster]:
        """List portfolios with optional filters."""
        stmt = select(PortfolioMaster)
        if client_id:
            stmt = stmt.where(PortfolioMaster.client_id == client_id)
        if branch_id:
            stmt = stmt.where(PortfolioMaster.branch_id == branch_id)
        if status:
            stmt = stmt.where(PortfolioMaster.status == status)
        stmt = stmt.limit(limit).offset(offset)
        return list(self.session.scalars(stmt).all())

    def count(self) -> int:
        stmt = select(func.count()).select_from(PortfolioMaster)
        return self.session.scalar(stmt) or 0

    def has_open_positions(self, portfolio_id: str) -> bool:
        """Check if portfolio has active positions (business rule from PORTDEL.cbl)."""
        stmt = select(func.count()).select_from(InvestmentPosition).where(
            and_(
                InvestmentPosition.portfolio_id == portfolio_id,
                InvestmentPosition.status == "A",
            )
        )
        return (self.session.scalar(stmt) or 0) > 0

    def exists(self, portfolio_id: str) -> bool:
        return self.get_by_id(portfolio_id) is not None


# ---------------------------------------------------------------------------
# Position Repository (replaces VSAM POSHIST I/O)
# ---------------------------------------------------------------------------
class PositionRepository:
    """CRUD for INVESTMENT_POSITIONS. Replaces VSAM POSHIST KSDS operations."""

    def __init__(self, session: Session):
        self.session = session

    def create(self, position: InvestmentPosition) -> InvestmentPosition:
        self.session.add(position)
        self.session.flush()
        return position

    def get(
        self, portfolio_id: str, investment_id: str, position_date: date
    ) -> InvestmentPosition | None:
        """VSAM READ by composite key."""
        return self.session.get(
            InvestmentPosition, (portfolio_id, investment_id, position_date)
        )

    def get_latest(
        self, portfolio_id: str, investment_id: str
    ) -> InvestmentPosition | None:
        """Get latest position for an investment."""
        stmt = (
            select(InvestmentPosition)
            .where(
                and_(
                    InvestmentPosition.portfolio_id == portfolio_id,
                    InvestmentPosition.investment_id == investment_id,
                    InvestmentPosition.status == "A",
                )
            )
            .order_by(InvestmentPosition.position_date.desc())
            .limit(1)
        )
        return self.session.scalar(stmt)

    def update(self, position: InvestmentPosition) -> InvestmentPosition:
        self.session.merge(position)
        self.session.flush()
        return position

    def list_by_portfolio(
        self, portfolio_id: str, active_only: bool = True
    ) -> list[InvestmentPosition]:
        """VSAM START/READNEXT by portfolio key prefix."""
        stmt = select(InvestmentPosition).where(
            InvestmentPosition.portfolio_id == portfolio_id
        )
        if active_only:
            stmt = stmt.where(InvestmentPosition.status == "A")
        stmt = stmt.order_by(InvestmentPosition.position_date.desc())
        return list(self.session.scalars(stmt).all())

    def list_all_active(self) -> list[InvestmentPosition]:
        stmt = (
            select(InvestmentPosition)
            .where(InvestmentPosition.status == "A")
            .order_by(InvestmentPosition.portfolio_id, InvestmentPosition.investment_id)
        )
        return list(self.session.scalars(stmt).all())


# ---------------------------------------------------------------------------
# Transaction Repository (replaces VSAM TRANHIST I/O)
# ---------------------------------------------------------------------------
class TransactionRepository:
    """CRUD for TRANSACTION_HISTORY. Replaces VSAM TRANHIST KSDS operations."""

    def __init__(self, session: Session):
        self.session = session

    def create(self, transaction: TransactionHistory) -> TransactionHistory:
        """VSAM WRITE + DB2 INSERT equivalent."""
        self.session.add(transaction)
        self.session.flush()
        return transaction

    def get_by_id(self, transaction_id: int) -> TransactionHistory | None:
        return self.session.get(TransactionHistory, transaction_id)

    def list_by_portfolio(
        self,
        portfolio_id: str,
        start_date: date | None = None,
        end_date: date | None = None,
        limit: int = 100,
    ) -> list[TransactionHistory]:
        """
        Translates INQHIST.cbl DB2 query:
        SELECT ... FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC
        """
        stmt = select(TransactionHistory).where(
            TransactionHistory.portfolio_id == portfolio_id
        )
        if start_date:
            stmt = stmt.where(TransactionHistory.trn_date >= start_date)
        if end_date:
            stmt = stmt.where(TransactionHistory.trn_date <= end_date)
        stmt = stmt.order_by(TransactionHistory.trn_date.desc()).limit(limit)
        return list(self.session.scalars(stmt).all())

    def list_pending(self) -> list[TransactionHistory]:
        """Get pending transactions for batch processing."""
        stmt = (
            select(TransactionHistory)
            .where(TransactionHistory.status == "P")
            .order_by(TransactionHistory.trn_date, TransactionHistory.trn_time)
        )
        return list(self.session.scalars(stmt).all())

    def check_duplicate(
        self, trn_date: date, trn_time: str, portfolio_id: str, sequence_no: str
    ) -> bool:
        """Check for duplicate transaction (from TRNVAL00.cbl)."""
        stmt = select(func.count()).select_from(TransactionHistory).where(
            and_(
                TransactionHistory.trn_date == trn_date,
                TransactionHistory.trn_time == trn_time,
                TransactionHistory.portfolio_id == portfolio_id,
                TransactionHistory.sequence_no == sequence_no,
            )
        )
        return (self.session.scalar(stmt) or 0) > 0

    def bulk_create(self, transactions: list[TransactionHistory]) -> int:
        """Batch insert for HISTLD00.cbl history loading."""
        self.session.add_all(transactions)
        self.session.flush()
        return len(transactions)


# ---------------------------------------------------------------------------
# Audit Repository
# ---------------------------------------------------------------------------
class AuditRepository:
    """CRUD for audit log. Replaces AUDPROC.cbl file writes."""

    def __init__(self, session: Session):
        self.session = session

    def create(self, record: AuditLog) -> AuditLog:
        self.session.add(record)
        self.session.flush()
        return record

    def list_by_date_range(
        self,
        start: datetime,
        end: datetime,
        audit_type: str | None = None,
    ) -> list[AuditLog]:
        stmt = select(AuditLog).where(
            and_(AuditLog.timestamp >= start, AuditLog.timestamp <= end)
        )
        if audit_type:
            stmt = stmt.where(AuditLog.audit_type == audit_type)
        stmt = stmt.order_by(AuditLog.timestamp.desc())
        return list(self.session.scalars(stmt).all())

    def list_by_user(self, user_id: str) -> list[AuditLog]:
        stmt = (
            select(AuditLog)
            .where(AuditLog.user_id == user_id)
            .order_by(AuditLog.timestamp.desc())
        )
        return list(self.session.scalars(stmt).all())


# ---------------------------------------------------------------------------
# Error Log Repository
# ---------------------------------------------------------------------------
class ErrorLogRepository:
    """CRUD for error log. Replaces ERRPROC.cbl file writes."""

    def __init__(self, session: Session):
        self.session = session

    def create(self, record: ErrorLog) -> ErrorLog:
        self.session.add(record)
        self.session.flush()
        return record

    def list_by_severity(self, min_severity: int = 0) -> list[ErrorLog]:
        stmt = (
            select(ErrorLog)
            .where(ErrorLog.severity >= min_severity)
            .order_by(ErrorLog.timestamp.desc())
        )
        return list(self.session.scalars(stmt).all())


# ---------------------------------------------------------------------------
# Batch Control Repository
# ---------------------------------------------------------------------------
class BatchControlRepository:
    """CRUD for batch control records. Replaces BCHCTL VSAM I/O."""

    def __init__(self, session: Session):
        self.session = session

    def create(self, record: BatchControl) -> BatchControl:
        self.session.add(record)
        self.session.flush()
        return record

    def get(self, job_name: str, process_date: str) -> BatchControl | None:
        return self.session.get(BatchControl, (job_name, process_date))

    def update(self, record: BatchControl) -> BatchControl:
        self.session.merge(record)
        self.session.flush()
        return record

    def list_by_date(self, process_date: str) -> list[BatchControl]:
        stmt = (
            select(BatchControl)
            .where(BatchControl.process_date == process_date)
            .order_by(BatchControl.sequence_no)
        )
        return list(self.session.scalars(stmt).all())

    def list_all(self) -> list[BatchControl]:
        stmt = select(BatchControl).order_by(
            BatchControl.process_date.desc(), BatchControl.sequence_no
        )
        return list(self.session.scalars(stmt).all())


# ---------------------------------------------------------------------------
# Process Sequence Repository
# ---------------------------------------------------------------------------
class ProcessSequenceRepository:
    """CRUD for process sequence. Replaces PRCSEQ VSAM I/O."""

    def __init__(self, session: Session):
        self.session = session

    def create(self, record: ProcessSequence) -> ProcessSequence:
        self.session.add(record)
        self.session.flush()
        return record

    def get(self, process_id: str, sequence_type: str) -> ProcessSequence | None:
        return self.session.get(ProcessSequence, (process_id, sequence_type))

    def list_by_type(self, sequence_type: str) -> list[ProcessSequence]:
        stmt = (
            select(ProcessSequence)
            .where(ProcessSequence.sequence_type == sequence_type)
            .order_by(ProcessSequence.sequence_no)
        )
        return list(self.session.scalars(stmt).all())


# ---------------------------------------------------------------------------
# User Auth Repository (for SECMGR.cbl authorization checks)
# ---------------------------------------------------------------------------
class UserAuthRepository:
    """Authorization checks. Translates SECMGR P200-CHECK-AUTH DB2 query."""

    def __init__(self, session: Session):
        self.session = session

    def check_access(self, user_id: str, resource: str, access_type: str) -> bool:
        """
        Translates SECMGR.cbl P200-CHECK-AUTH:
        SELECT COUNT(*) FROM AUTHFILE
        WHERE USER_ID = ? AND RESOURCE = ? AND ACCESS_TYPE = ?
        """
        stmt = select(func.count()).select_from(UserAuth).where(
            and_(
                UserAuth.user_id == user_id,
                UserAuth.resource == resource,
                UserAuth.access_type == access_type,
                UserAuth.granted.is_(True),
            )
        )
        return (self.session.scalar(stmt) or 0) > 0

    def grant_access(
        self, user_id: str, resource: str, access_type: str
    ) -> UserAuth:
        record = UserAuth(
            user_id=user_id,
            resource=resource,
            access_type=access_type,
            granted=True,
        )
        self.session.merge(record)
        self.session.flush()
        return record
