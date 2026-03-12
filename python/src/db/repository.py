"""Repository layer for the Investment Portfolio Management System.

Provides CRUD operations that replace COBOL VSAM READ/WRITE/REWRITE/DELETE
and DB2 embedded SQL access patterns.  Each repository class implements
key-based access matching the original VSAM KSDS key structures:

  PORTMSTR  KSDS key = portfolio_id(8) + account_type(2) + branch_id(2)
  TRANHIST  KSDS key = date(8) + time(6) + portfolio_id(8) + seq(6)
  POSHIST   KSDS key = portfolio_id(8) + position_date(8) + investment_id(10)

Query methods translate DB2 embedded SQL from:
  - src/programs/online/INQPORT.cbl  (portfolio position queries)
  - src/programs/online/INQHIST.cbl  (transaction history queries)
  - src/programs/batch/POSUPD00.cbl  (position updates)
  - src/programs/batch/TRNVAL00.cbl  (transaction validation queries)
"""

from __future__ import annotations

import datetime
import logging
from typing import Sequence

from sqlalchemy import select
from sqlalchemy.orm import Session

from python.src.db.tables import (
    ErrorLog,
    InvestmentPosition,
    PortfolioMaster,
    PositionHistory,
    TransactionHistory,
)

logger = logging.getLogger(__name__)


# =====================================================================
# PortfolioRepository
# Replaces VSAM PORTMSTR READ/WRITE/REWRITE/DELETE and DB2
# PORTFOLIO_MASTER embedded SQL.
# =====================================================================
class PortfolioRepository:
    """CRUD operations for :class:`PortfolioMaster`.

    VSAM PORTMSTR key access: portfolio_id + account_type + branch_id
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    # -- Create ----------------------------------------------------------

    def create(self, portfolio: PortfolioMaster) -> PortfolioMaster:
        """Insert a new portfolio record (VSAM WRITE equivalent)."""
        self._session.add(portfolio)
        self._session.flush()
        logger.debug("Portfolio created: %s", portfolio.portfolio_id)
        return portfolio

    # -- Read ------------------------------------------------------------

    def get_by_id(self, portfolio_id: str) -> PortfolioMaster | None:
        """Retrieve a portfolio by primary key (VSAM READ by key)."""
        return self._session.get(PortfolioMaster, portfolio_id)

    def get_by_vsam_key(
        self,
        portfolio_id: str,
        account_type: str,
        branch_id: str,
    ) -> PortfolioMaster | None:
        """Retrieve a portfolio using the full VSAM KSDS composite key.

        PORTMSTR key = portfolio_id(8) + account_type(2) + branch_id(2)
        """
        stmt = select(PortfolioMaster).where(
            PortfolioMaster.portfolio_id == portfolio_id,
            PortfolioMaster.account_type == account_type,
            PortfolioMaster.branch_id == branch_id,
        )
        return self._session.execute(stmt).scalar_one_or_none()

    # -- Update ----------------------------------------------------------

    def update(self, portfolio: PortfolioMaster) -> PortfolioMaster:
        """Update an existing portfolio record (VSAM REWRITE equivalent)."""
        merged = self._session.merge(portfolio)
        self._session.flush()
        logger.debug("Portfolio updated: %s", merged.portfolio_id)
        return merged

    # -- Delete ----------------------------------------------------------

    def delete(self, portfolio: PortfolioMaster) -> None:
        """Delete a portfolio record (VSAM DELETE equivalent)."""
        self._session.delete(portfolio)
        self._session.flush()
        logger.debug("Portfolio deleted: %s", portfolio.portfolio_id)

    # -- List / Query ----------------------------------------------------

    def list_by_client(self, client_id: str) -> Sequence[PortfolioMaster]:
        """List portfolios for a client.

        Uses IDX_PORT_MASTER_CLIENT index (CLIENT_ID, STATUS).
        """
        stmt = (
            select(PortfolioMaster)
            .where(PortfolioMaster.client_id == client_id)
            .order_by(PortfolioMaster.portfolio_id)
        )
        return self._session.execute(stmt).scalars().all()

    def list_by_branch(self, branch_id: str) -> Sequence[PortfolioMaster]:
        """List portfolios for a branch.

        Uses ix_portfolio_master_branch index.
        """
        stmt = (
            select(PortfolioMaster)
            .where(PortfolioMaster.branch_id == branch_id)
            .order_by(PortfolioMaster.portfolio_id)
        )
        return self._session.execute(stmt).scalars().all()

    def list_by_status(self, status: str) -> Sequence[PortfolioMaster]:
        """List portfolios by status code (A=Active, C=Closed, S=Suspended)."""
        stmt = (
            select(PortfolioMaster)
            .where(PortfolioMaster.status == status)
            .order_by(PortfolioMaster.portfolio_id)
        )
        return self._session.execute(stmt).scalars().all()


# =====================================================================
# PositionRepository
# Replaces VSAM POSHIST READ/WRITE/REWRITE and DB2
# INVESTMENT_POSITIONS embedded SQL.
# =====================================================================
class PositionRepository:
    """CRUD operations for :class:`InvestmentPosition`.

    VSAM POSHIST key access: portfolio_id + position_date + investment_id
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    # -- Create ----------------------------------------------------------

    def create(self, position: InvestmentPosition) -> InvestmentPosition:
        """Insert a new position record (VSAM WRITE)."""
        self._session.add(position)
        self._session.flush()
        logger.debug(
            "Position created: %s/%s/%s",
            position.portfolio_id,
            position.investment_id,
            position.position_date,
        )
        return position

    # -- Read ------------------------------------------------------------

    def get_by_key(
        self,
        portfolio_id: str,
        investment_id: str,
        position_date: datetime.date,
    ) -> InvestmentPosition | None:
        """Retrieve a position by composite PK (VSAM READ by key)."""
        return self._session.get(
            InvestmentPosition,
            (portfolio_id, investment_id, position_date),
        )

    # -- Update ----------------------------------------------------------

    def update(self, position: InvestmentPosition) -> InvestmentPosition:
        """Update an existing position record (VSAM REWRITE)."""
        merged = self._session.merge(position)
        self._session.flush()
        logger.debug(
            "Position updated: %s/%s/%s",
            merged.portfolio_id,
            merged.investment_id,
            merged.position_date,
        )
        return merged

    # -- List / Query ----------------------------------------------------

    def list_by_portfolio(
        self, portfolio_id: str
    ) -> Sequence[InvestmentPosition]:
        """List all positions for a portfolio.

        Matches VSAM POSHIST sequential read by portfolio_id prefix.
        """
        stmt = (
            select(InvestmentPosition)
            .where(InvestmentPosition.portfolio_id == portfolio_id)
            .order_by(
                InvestmentPosition.position_date,
                InvestmentPosition.investment_id,
            )
        )
        return self._session.execute(stmt).scalars().all()

    def list_by_date_range(
        self,
        start_date: datetime.date,
        end_date: datetime.date,
        portfolio_id: str | None = None,
    ) -> Sequence[InvestmentPosition]:
        """List positions within a date range.

        Uses IDX_POSITIONS_DATE index (POSITION_DATE, PORTFOLIO_ID).
        """
        stmt = select(InvestmentPosition).where(
            InvestmentPosition.position_date >= start_date,
            InvestmentPosition.position_date <= end_date,
        )
        if portfolio_id is not None:
            stmt = stmt.where(InvestmentPosition.portfolio_id == portfolio_id)
        stmt = stmt.order_by(
            InvestmentPosition.position_date,
            InvestmentPosition.portfolio_id,
            InvestmentPosition.investment_id,
        )
        return self._session.execute(stmt).scalars().all()


# =====================================================================
# TransactionRepository
# Replaces VSAM TRANHIST READ/WRITE and DB2 TRANSACTION_HISTORY
# embedded SQL.
# =====================================================================
class TransactionRepository:
    """CRUD operations for :class:`TransactionHistory`.

    VSAM TRANHIST key access: date + time + portfolio_id + sequence
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    # -- Create ----------------------------------------------------------

    def create(self, txn: TransactionHistory) -> TransactionHistory:
        """Insert a new transaction record (VSAM WRITE)."""
        self._session.add(txn)
        self._session.flush()
        logger.debug("Transaction created: %s", txn.transaction_id)
        return txn

    # -- Read ------------------------------------------------------------

    def get_by_id(self, transaction_id: str) -> TransactionHistory | None:
        """Retrieve a transaction by primary key."""
        return self._session.get(TransactionHistory, transaction_id)

    # -- List / Query ----------------------------------------------------

    def list_by_portfolio(
        self, portfolio_id: str
    ) -> Sequence[TransactionHistory]:
        """List all transactions for a portfolio.

        Uses IDX_TRANS_HIST_PORT index (PORTFOLIO_ID, TRANSACTION_DATE).
        """
        stmt = (
            select(TransactionHistory)
            .where(TransactionHistory.portfolio_id == portfolio_id)
            .order_by(
                TransactionHistory.transaction_date,
                TransactionHistory.transaction_time,
            )
        )
        return self._session.execute(stmt).scalars().all()

    def list_by_date_range(
        self,
        start_date: datetime.date,
        end_date: datetime.date,
        portfolio_id: str | None = None,
    ) -> Sequence[TransactionHistory]:
        """List transactions within a date range.

        Uses IDX_TRANS_HIST_DATE index (TRANSACTION_DATE, PORTFOLIO_ID).
        Matches VSAM TRANHIST sequential read by date prefix.
        """
        stmt = select(TransactionHistory).where(
            TransactionHistory.transaction_date >= start_date,
            TransactionHistory.transaction_date <= end_date,
        )
        if portfolio_id is not None:
            stmt = stmt.where(TransactionHistory.portfolio_id == portfolio_id)
        stmt = stmt.order_by(
            TransactionHistory.transaction_date,
            TransactionHistory.transaction_time,
            TransactionHistory.portfolio_id,
        )
        return self._session.execute(stmt).scalars().all()


# =====================================================================
# AuditRepository
# Replaces POSHIST DB2 table access for position history / audit data.
# =====================================================================
class AuditRepository:
    """CRUD operations for :class:`PositionHistory` (audit / position history).

    Maps to POSHIST.sql table with composite PK
    (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME).
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    # -- Create ----------------------------------------------------------

    def create(self, record: PositionHistory) -> PositionHistory:
        """Insert a new position-history / audit record."""
        self._session.add(record)
        self._session.flush()
        logger.debug(
            "Audit record created: %s/%s/%s",
            record.account_no,
            record.portfolio_id,
            record.trans_date,
        )
        return record

    # -- List / Query ----------------------------------------------------

    def list_by_date_range(
        self,
        start_date: datetime.date,
        end_date: datetime.date,
        portfolio_id: str | None = None,
    ) -> Sequence[PositionHistory]:
        """List audit records within a date range.

        Uses POSHIST_IX2 (PROCESS_DATE, PROGRAM_ID) for date scans.
        """
        stmt = select(PositionHistory).where(
            PositionHistory.trans_date >= start_date,
            PositionHistory.trans_date <= end_date,
        )
        if portfolio_id is not None:
            stmt = stmt.where(PositionHistory.portfolio_id == portfolio_id)
        stmt = stmt.order_by(
            PositionHistory.trans_date,
            PositionHistory.trans_time,
            PositionHistory.account_no,
        )
        return self._session.execute(stmt).scalars().all()


# =====================================================================
# ErrorLogRepository
# Replaces ERRLOG DB2 table access.
# =====================================================================
class ErrorLogRepository:
    """CRUD operations for :class:`ErrorLog`.

    Maps to ERRLOG.sql table with composite PK
    (ERROR_TIMESTAMP, PROGRAM_ID).
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    # -- Create ----------------------------------------------------------

    def create(self, error: ErrorLog) -> ErrorLog:
        """Insert a new error log record."""
        self._session.add(error)
        self._session.flush()
        logger.debug(
            "Error logged: %s [%s] %s",
            error.program_id,
            error.error_code,
            error.error_message[:50],
        )
        return error

    # -- List / Query ----------------------------------------------------

    def list_recent(
        self,
        limit: int = 100,
        severity_min: int | None = None,
    ) -> Sequence[ErrorLog]:
        """List the most recent error log entries.

        Uses ERRLOG_IX1 (PROCESS_DATE, ERROR_SEVERITY) for ordering.
        Optionally filter by minimum severity level.
        """
        stmt = select(ErrorLog)
        if severity_min is not None:
            stmt = stmt.where(ErrorLog.error_severity >= severity_min)
        stmt = stmt.order_by(
            ErrorLog.error_timestamp.desc(),
        ).limit(limit)
        return self._session.execute(stmt).scalars().all()
