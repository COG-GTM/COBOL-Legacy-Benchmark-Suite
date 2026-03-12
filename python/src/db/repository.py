"""
Repository layer providing CRUD operations translated from COBOL VSAM I/O
and DB2 embedded SQL.

Replaces:
  - VSAM READ/WRITE/REWRITE/DELETE/START/READNEXT operations
  - DB2 SELECT/INSERT/UPDATE/DELETE embedded SQL
  - Key-based access patterns from VSAM KSDS definitions:
    * PORTMSTR key: portfolio_id(8)
    * TRANHIST key: date(8) + time(6) + portfolio(8) + seq(6)
    * POSHIST key: portfolio(8) + date(8) + investment(10)
"""

import logging
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import and_, func, select
from sqlalchemy.orm import Session

from src.db.tables import (
    AuditLog,
    BatchControl,
    Checkpoint,
    ErrorLog,
    InvestmentPosition,
    MarketData,
    PortfolioMaster,
    TransactionHistory,
)

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Portfolio Repository  (replaces PORTMSTR.cbl VSAM I/O + DB2 PORTFOLIO_MASTER)
# ---------------------------------------------------------------------------
class PortfolioRepository:
    """CRUD operations for the PORTFOLIO_MASTER table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def get_by_id(self, portfolio_id: str) -> PortfolioMaster | None:
        """
        Read portfolio by primary key.

        Translates PORTMSTR.cbl 2000-READ-PORT:
          READ PORTMSTR-FILE INTO PORT-RECORD KEY IS PORT-ID
        """
        return self._session.get(PortfolioMaster, portfolio_id)

    def create(self, portfolio: PortfolioMaster) -> PortfolioMaster:
        """
        Write new portfolio record.

        Translates PORTMSTR.cbl 1000-CREATE-PORT:
          WRITE PORT-RECORD FROM WS-PORT-RECORD
        """
        self._session.add(portfolio)
        self._session.flush()
        logger.info("Created portfolio: %s", portfolio.portfolio_id)
        return portfolio

    def update(self, portfolio: PortfolioMaster) -> PortfolioMaster:
        """
        Update existing portfolio record.

        Translates PORTMSTR.cbl 3000-UPDATE-PORT:
          REWRITE PORT-RECORD FROM WS-PORT-RECORD
        """
        self._session.merge(portfolio)
        self._session.flush()
        logger.info("Updated portfolio: %s", portfolio.portfolio_id)
        return portfolio

    def delete(self, portfolio_id: str) -> bool:
        """
        Delete portfolio record.

        Translates PORTMSTR.cbl 4000-DELETE-PORT:
          DELETE PORTMSTR-FILE RECORD
        """
        portfolio = self.get_by_id(portfolio_id)
        if portfolio is None:
            return False
        self._session.delete(portfolio)
        self._session.flush()
        logger.info("Deleted portfolio: %s", portfolio_id)
        return True

    def list_by_client(self, client_id: str) -> list[PortfolioMaster]:
        """
        List portfolios for a client.

        Translates VSAM START/READNEXT by client alternate index.
        """
        stmt = (
            select(PortfolioMaster)
            .where(PortfolioMaster.client_id == client_id)
            .order_by(PortfolioMaster.portfolio_id)
        )
        return list(self._session.execute(stmt).scalars().all())

    def list_by_branch(self, branch_id: str) -> list[PortfolioMaster]:
        """List portfolios for a branch."""
        stmt = (
            select(PortfolioMaster)
            .where(PortfolioMaster.branch_id == branch_id)
            .order_by(PortfolioMaster.portfolio_id)
        )
        return list(self._session.execute(stmt).scalars().all())

    def list_by_status(self, status: str) -> list[PortfolioMaster]:
        """List portfolios by status code."""
        stmt = (
            select(PortfolioMaster)
            .where(PortfolioMaster.status == status)
            .order_by(PortfolioMaster.portfolio_id)
        )
        return list(self._session.execute(stmt).scalars().all())

    def list_all(
        self,
        offset: int = 0,
        limit: int = 100,
        status: str | None = None,
        branch_id: str | None = None,
        client_id: str | None = None,
    ) -> list[PortfolioMaster]:
        """List portfolios with optional filters and pagination."""
        stmt = select(PortfolioMaster)
        if status is not None:
            stmt = stmt.where(PortfolioMaster.status == status)
        if branch_id is not None:
            stmt = stmt.where(PortfolioMaster.branch_id == branch_id)
        if client_id is not None:
            stmt = stmt.where(PortfolioMaster.client_id == client_id)
        stmt = stmt.order_by(PortfolioMaster.portfolio_id).offset(offset).limit(limit)
        return list(self._session.execute(stmt).scalars().all())

    def count(self, status: str | None = None) -> int:
        """Count portfolios with optional status filter."""
        stmt = select(func.count()).select_from(PortfolioMaster)
        if status is not None:
            stmt = stmt.where(PortfolioMaster.status == status)
        result = self._session.execute(stmt).scalar()
        return result if result is not None else 0


# ---------------------------------------------------------------------------
# Position Repository  (replaces POSUPD00.cbl VSAM I/O + DB2 INVESTMENT_POSITIONS)
# ---------------------------------------------------------------------------
class PositionRepository:
    """CRUD operations for the INVESTMENT_POSITIONS table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def get(
        self, portfolio_id: str, investment_id: str, position_date: date
    ) -> InvestmentPosition | None:
        """
        Read position by composite key.

        Translates VSAM POSHIST key: portfolio(8) + date(8) + investment(10).
        """
        return self._session.get(
            InvestmentPosition, (portfolio_id, investment_id, position_date)
        )

    def create(self, position: InvestmentPosition) -> InvestmentPosition:
        """Write new position record."""
        self._session.add(position)
        self._session.flush()
        return position

    def update(self, position: InvestmentPosition) -> InvestmentPosition:
        """Update existing position record (VSAM REWRITE)."""
        self._session.merge(position)
        self._session.flush()
        return position

    def delete(
        self, portfolio_id: str, investment_id: str, position_date: date
    ) -> bool:
        """Delete position record."""
        position = self.get(portfolio_id, investment_id, position_date)
        if position is None:
            return False
        self._session.delete(position)
        self._session.flush()
        return True

    def list_by_portfolio(
        self, portfolio_id: str, position_date: date | None = None
    ) -> list[InvestmentPosition]:
        """
        List positions for a portfolio, optionally filtered by date.

        Translates VSAM START/READNEXT on POSHIST by portfolio key prefix.
        """
        stmt = select(InvestmentPosition).where(
            InvestmentPosition.portfolio_id == portfolio_id
        )
        if position_date is not None:
            stmt = stmt.where(InvestmentPosition.position_date == position_date)
        stmt = stmt.order_by(
            InvestmentPosition.investment_id, InvestmentPosition.position_date
        )
        return list(self._session.execute(stmt).scalars().all())

    def list_by_investment(self, investment_id: str) -> list[InvestmentPosition]:
        """List all positions for a specific investment."""
        stmt = (
            select(InvestmentPosition)
            .where(InvestmentPosition.investment_id == investment_id)
            .order_by(InvestmentPosition.portfolio_id, InvestmentPosition.position_date)
        )
        return list(self._session.execute(stmt).scalars().all())

    def get_latest_position(
        self, portfolio_id: str, investment_id: str
    ) -> InvestmentPosition | None:
        """Get the most recent position for a portfolio/investment pair."""
        stmt = (
            select(InvestmentPosition)
            .where(
                and_(
                    InvestmentPosition.portfolio_id == portfolio_id,
                    InvestmentPosition.investment_id == investment_id,
                )
            )
            .order_by(InvestmentPosition.position_date.desc())
            .limit(1)
        )
        return self._session.execute(stmt).scalar_one_or_none()

    def get_portfolio_total_value(self, portfolio_id: str, position_date: date) -> Decimal:
        """Sum market values for all positions in a portfolio on a given date."""
        stmt = select(func.sum(InvestmentPosition.market_value)).where(
            and_(
                InvestmentPosition.portfolio_id == portfolio_id,
                InvestmentPosition.position_date == position_date,
                InvestmentPosition.status == "A",
            )
        )
        result = self._session.execute(stmt).scalar()
        return Decimal(str(result)) if result is not None else Decimal("0.00")


# ---------------------------------------------------------------------------
# Transaction Repository  (replaces TRANHIST VSAM I/O + DB2 TRANSACTION_HISTORY)
# ---------------------------------------------------------------------------
class TransactionRepository:
    """CRUD operations for the TRANSACTION_HISTORY table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def get_by_id(self, transaction_id: str) -> TransactionHistory | None:
        """Read transaction by primary key."""
        return self._session.get(TransactionHistory, transaction_id)

    def create(self, transaction: TransactionHistory) -> TransactionHistory:
        """Write new transaction record."""
        self._session.add(transaction)
        self._session.flush()
        return transaction

    def update(self, transaction: TransactionHistory) -> TransactionHistory:
        """Update transaction record."""
        self._session.merge(transaction)
        self._session.flush()
        return transaction

    def list_by_portfolio(
        self,
        portfolio_id: str,
        start_date: date | None = None,
        end_date: date | None = None,
        trn_type: str | None = None,
        status: str | None = None,
    ) -> list[TransactionHistory]:
        """
        List transactions for a portfolio with optional filters.

        Translates VSAM TRANHIST key browse and DB2 query with WHERE clauses.
        """
        stmt = select(TransactionHistory).where(
            TransactionHistory.portfolio_id == portfolio_id
        )
        if start_date is not None:
            stmt = stmt.where(TransactionHistory.trn_date >= start_date)
        if end_date is not None:
            stmt = stmt.where(TransactionHistory.trn_date <= end_date)
        if trn_type is not None:
            stmt = stmt.where(TransactionHistory.trn_type == trn_type)
        if status is not None:
            stmt = stmt.where(TransactionHistory.status == status)
        stmt = stmt.order_by(
            TransactionHistory.trn_date.desc(), TransactionHistory.trn_time.desc()
        )
        return list(self._session.execute(stmt).scalars().all())

    def list_pending(self) -> list[TransactionHistory]:
        """List all pending transactions for batch processing."""
        stmt = (
            select(TransactionHistory)
            .where(TransactionHistory.status == "P")
            .order_by(TransactionHistory.trn_date, TransactionHistory.trn_time)
        )
        return list(self._session.execute(stmt).scalars().all())

    def count_by_status(self, status: str) -> int:
        """Count transactions by status."""
        stmt = select(func.count()).select_from(TransactionHistory).where(
            TransactionHistory.status == status
        )
        result = self._session.execute(stmt).scalar()
        return result if result is not None else 0

    def get_portfolio_transaction_total(
        self, portfolio_id: str, trn_type: str
    ) -> Decimal:
        """Sum transaction amounts for a portfolio by type."""
        stmt = select(func.sum(TransactionHistory.amount)).where(
            and_(
                TransactionHistory.portfolio_id == portfolio_id,
                TransactionHistory.trn_type == trn_type,
                TransactionHistory.status == "D",
            )
        )
        result = self._session.execute(stmt).scalar()
        return Decimal(str(result)) if result is not None else Decimal("0.00")


# ---------------------------------------------------------------------------
# Audit Repository
# ---------------------------------------------------------------------------
class AuditRepository:
    """CRUD operations for the AUDIT_LOG table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def create(self, audit: AuditLog) -> AuditLog:
        """Write audit log record."""
        self._session.add(audit)
        self._session.flush()
        return audit

    def list_by_date_range(
        self, start: datetime, end: datetime
    ) -> list[AuditLog]:
        """List audit records within a date range."""
        stmt = (
            select(AuditLog)
            .where(and_(AuditLog.timestamp >= start, AuditLog.timestamp <= end))
            .order_by(AuditLog.timestamp.desc())
        )
        return list(self._session.execute(stmt).scalars().all())

    def list_by_portfolio(self, portfolio_id: str) -> list[AuditLog]:
        """List audit records for a specific portfolio."""
        stmt = (
            select(AuditLog)
            .where(AuditLog.portfolio_id == portfolio_id)
            .order_by(AuditLog.timestamp.desc())
        )
        return list(self._session.execute(stmt).scalars().all())


# ---------------------------------------------------------------------------
# Error Repository
# ---------------------------------------------------------------------------
class ErrorRepository:
    """CRUD operations for the ERROR_LOG table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def create(self, error: ErrorLog) -> ErrorLog:
        """Write error log record."""
        self._session.add(error)
        self._session.flush()
        return error

    def list_by_severity(self, min_severity: int = 0) -> list[ErrorLog]:
        """List errors at or above a given severity."""
        stmt = (
            select(ErrorLog)
            .where(ErrorLog.severity >= min_severity)
            .order_by(ErrorLog.timestamp.desc())
        )
        return list(self._session.execute(stmt).scalars().all())


# ---------------------------------------------------------------------------
# Batch Control Repository
# ---------------------------------------------------------------------------
class BatchControlRepository:
    """CRUD operations for the BATCH_CONTROL table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def get_by_id(self, batch_id: str) -> BatchControl | None:
        """Read batch control record."""
        return self._session.get(BatchControl, batch_id)

    def create(self, batch: BatchControl) -> BatchControl:
        """Create batch control record."""
        self._session.add(batch)
        self._session.flush()
        return batch

    def update(self, batch: BatchControl) -> BatchControl:
        """Update batch control record."""
        self._session.merge(batch)
        self._session.flush()
        return batch

    def list_by_status(self, status: str) -> list[BatchControl]:
        """List batch records by status."""
        stmt = (
            select(BatchControl)
            .where(BatchControl.batch_status == status)
            .order_by(BatchControl.schedule_date)
        )
        return list(self._session.execute(stmt).scalars().all())


# ---------------------------------------------------------------------------
# Checkpoint Repository
# ---------------------------------------------------------------------------
class CheckpointRepository:
    """CRUD operations for the CHECKPOINT table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def get_by_id(self, checkpoint_id: str) -> Checkpoint | None:
        """Read checkpoint record."""
        return self._session.get(Checkpoint, checkpoint_id)

    def create(self, checkpoint: Checkpoint) -> Checkpoint:
        """Create checkpoint record."""
        self._session.add(checkpoint)
        self._session.flush()
        return checkpoint

    def update(self, checkpoint: Checkpoint) -> Checkpoint:
        """Update checkpoint record."""
        self._session.merge(checkpoint)
        self._session.flush()
        return checkpoint

    def get_latest_for_batch(self, batch_id: str) -> Checkpoint | None:
        """Get most recent checkpoint for a batch job."""
        stmt = (
            select(Checkpoint)
            .where(Checkpoint.batch_id == batch_id)
            .order_by(Checkpoint.save_time.desc())
            .limit(1)
        )
        return self._session.execute(stmt).scalar_one_or_none()


# ---------------------------------------------------------------------------
# Market Data Repository
# ---------------------------------------------------------------------------
class MarketDataRepository:
    """CRUD operations for the MARKET_DATA table."""

    def __init__(self, session: Session) -> None:
        self._session = session

    def get(self, investment_id: str, price_date: date) -> MarketData | None:
        """Get market data for an investment on a specific date."""
        return self._session.get(MarketData, (investment_id, price_date))

    def create(self, market_data: MarketData) -> MarketData:
        """Create market data record."""
        self._session.add(market_data)
        self._session.flush()
        return market_data

    def get_latest_price(self, investment_id: str) -> MarketData | None:
        """Get most recent market data for an investment."""
        stmt = (
            select(MarketData)
            .where(MarketData.investment_id == investment_id)
            .order_by(MarketData.price_date.desc())
            .limit(1)
        )
        return self._session.execute(stmt).scalar_one_or_none()
