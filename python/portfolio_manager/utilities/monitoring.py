"""System Monitoring Utility.

Replaces: UTLMON00 (src/programs/utility/UTLMON00.cbl)

Monitors system health and resource utilization including:
  - Database connection pool status
  - Table sizes and row counts
  - Recent error rates
  - System performance metrics

Original COBOL flow (UTLMON00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-CHECK-RESOURCES
      2100-CHECK-VSAM-FILES
      2200-CHECK-DB2-STATUS
      2300-CHECK-CICS-STATUS
    3000-CHECK-PERFORMANCE
      3100-CHECK-ERROR-RATE
      3200-CHECK-THROUGHPUT
    4000-REPORT-STATUS
    5000-FINALIZE
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import datetime, timedelta

from sqlalchemy import func, select, text
from sqlalchemy.orm import Session

from portfolio_manager.models.database import (
    AuditLog,
    ErrorLog,
    InvestmentPosition,
    PortfolioMaster,
    PositionHistory,
    TransactionHistory,
)

logger = logging.getLogger(__name__)


@dataclass
class TableStats:
    """Statistics for a single database table."""

    table_name: str
    row_count: int = 0


@dataclass
class MonitoringResult:
    """System monitoring results."""

    timestamp: datetime = field(default_factory=datetime.now)
    db_connected: bool = False
    table_stats: list[TableStats] = field(default_factory=list)
    total_rows: int = 0
    recent_errors_1h: int = 0
    recent_errors_24h: int = 0
    error_rate_per_hour: float = 0.0
    active_portfolios: int = 0
    status: str = "UNKNOWN"
    messages: list[str] = field(default_factory=list)


class MonitoringUtility:
    """System monitoring utility.

    Replaces UTLMON00 (src/programs/utility/UTLMON00.cbl).
    """

    PROGRAM_ID = "UTLMON00"

    # Thresholds
    ERROR_RATE_WARNING = 10  # errors per hour
    ERROR_RATE_CRITICAL = 50

    def __init__(self, session: Session):
        self._session = session

    def run(self) -> MonitoringResult:
        """Run all monitoring checks.

        Replaces 0000-MAIN-PROCESS from UTLMON00.cbl.

        Returns:
            MonitoringResult with system status.
        """
        result = MonitoringResult()

        logger.info("%s: Starting system monitoring", self.PROGRAM_ID)

        try:
            # 2200-CHECK-DB2-STATUS
            result.db_connected = self._check_db_connection()

            if result.db_connected:
                # 2100-CHECK-VSAM-FILES (now table counts)
                self._check_table_stats(result)

                # 3100-CHECK-ERROR-RATE
                self._check_error_rate(result)

                # Check active portfolios
                result.active_portfolios = self._count_active_portfolios()

            # 4000-REPORT-STATUS
            self._determine_status(result)

        except Exception as exc:
            result.status = "ERROR"
            result.messages.append(f"Monitoring failed: {exc}")
            logger.error("%s: Monitoring error: %s", self.PROGRAM_ID, exc)

        logger.info(
            "%s: Complete — status=%s db=%s tables=%d errors_1h=%d",
            self.PROGRAM_ID,
            result.status,
            result.db_connected,
            len(result.table_stats),
            result.recent_errors_1h,
        )

        return result

    def _check_db_connection(self) -> bool:
        """Check database connectivity.

        Replaces UTLMON00 paragraph 2200-CHECK-DB2-STATUS.
        """
        try:
            self._session.execute(text("SELECT 1"))
            return True
        except Exception as exc:
            logger.error("%s: DB connection check failed: %s", self.PROGRAM_ID, exc)
            return False

    def _check_table_stats(self, result: MonitoringResult) -> None:
        """Check table sizes and row counts.

        Replaces UTLMON00 paragraph 2100-CHECK-VSAM-FILES.
        """
        tables = [
            ("portfolio_master", PortfolioMaster),
            ("investment_positions", InvestmentPosition),
            ("transaction_history", TransactionHistory),
            ("poshist", PositionHistory),
            ("errlog", ErrorLog),
            ("audit_log", AuditLog),
        ]

        for table_name, model in tables:
            try:
                count = (
                    self._session.execute(
                        select(func.count()).select_from(model)
                    ).scalar()
                    or 0
                )
                result.table_stats.append(
                    TableStats(table_name=table_name, row_count=count)
                )
                result.total_rows += count
            except Exception as exc:
                result.messages.append(
                    f"Could not count {table_name}: {exc}"
                )

    def _check_error_rate(self, result: MonitoringResult) -> None:
        """Check recent error rates.

        Replaces UTLMON00 paragraph 3100-CHECK-ERROR-RATE.
        """
        now = datetime.now()

        # Errors in last hour
        one_hour_ago = now - timedelta(hours=1)
        result.recent_errors_1h = (
            self._session.execute(
                select(func.count())
                .select_from(ErrorLog)
                .where(ErrorLog.error_timestamp >= one_hour_ago)
            ).scalar()
            or 0
        )

        # Errors in last 24 hours
        one_day_ago = now - timedelta(hours=24)
        result.recent_errors_24h = (
            self._session.execute(
                select(func.count())
                .select_from(ErrorLog)
                .where(ErrorLog.error_timestamp >= one_day_ago)
            ).scalar()
            or 0
        )

        # Error rate per hour (24h average)
        result.error_rate_per_hour = result.recent_errors_24h / 24.0

    def _count_active_portfolios(self) -> int:
        """Count active portfolios."""
        return (
            self._session.execute(
                select(func.count())
                .select_from(PortfolioMaster)
                .where(PortfolioMaster.status == "A")
            ).scalar()
            or 0
        )

    def _determine_status(self, result: MonitoringResult) -> None:
        """Determine overall system status.

        Replaces UTLMON00 paragraph 4000-REPORT-STATUS.
        """
        if not result.db_connected:
            result.status = "CRITICAL"
            result.messages.append("Database connection is down")
        elif result.recent_errors_1h >= self.ERROR_RATE_CRITICAL:
            result.status = "CRITICAL"
            result.messages.append(
                f"Critical error rate: {result.recent_errors_1h} errors in last hour"
            )
        elif result.recent_errors_1h >= self.ERROR_RATE_WARNING:
            result.status = "WARNING"
            result.messages.append(
                f"Elevated error rate: {result.recent_errors_1h} errors in last hour"
            )
        else:
            result.status = "HEALTHY"
            result.messages.append("All systems operating normally")
