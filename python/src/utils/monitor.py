"""
System monitoring utility translated from COBOL program UTLMON00.cbl.

Monitors system health and performance:
- Resource utilization tracking
- Threshold monitoring
- Alert generation
"""

import logging
from datetime import datetime
from decimal import Decimal

from sqlalchemy import func, select, text
from sqlalchemy.orm import Session

from src.common.constants import AlertLevel, ResourceType
from src.db.tables import (
    AuditLog,
    BatchControl,
    ErrorLog,
    InvestmentPosition,
    PortfolioMaster,
    TransactionHistory,
)

logger = logging.getLogger(__name__)


class SystemMonitor:
    """System monitoring utility. Translates UTLMON00.cbl."""

    def __init__(self, session: Session):
        self.session = session
        self.thresholds: dict[str, Decimal] = {
            "error_rate": Decimal("10.00"),
            "batch_duration": Decimal("3600.00"),
        }
        self.alerts: list[dict] = []

    def collect_metrics(self) -> dict:
        """
        Collect system metrics.
        Translates 2100-COLLECT-METRICS.
        """
        metrics = {
            "timestamp": datetime.now().isoformat(),
            "database": self._get_db_metrics(),
            "tables": self._get_table_stats(),
            "batch": self._get_batch_metrics(),
            "errors": self._get_error_metrics(),
        }
        return metrics

    def check_thresholds(self, metrics: dict) -> list[dict]:
        """
        Check metrics against thresholds.
        Translates 2200-CHECK-THRESHOLDS.
        """
        self.alerts = []

        # Check error rate
        error_metrics = metrics.get("errors", {})
        recent_errors = error_metrics.get("recent_error_count", 0)
        if recent_errors > int(self.thresholds["error_rate"]):
            self.alerts.append({
                "level": AlertLevel.WARNING,
                "resource": ResourceType.DB2,
                "message": f"High error rate: {recent_errors} errors in last hour",
                "timestamp": datetime.now().isoformat(),
            })

        # Check batch status
        batch_metrics = metrics.get("batch", {})
        failed_jobs = batch_metrics.get("failed_jobs", 0)
        if failed_jobs > 0:
            self.alerts.append({
                "level": AlertLevel.CRITICAL,
                "resource": ResourceType.DB2,
                "message": f"{failed_jobs} batch job(s) in error state",
                "timestamp": datetime.now().isoformat(),
            })

        return self.alerts

    def health_check(self) -> dict:
        """
        Perform health check.
        Translates 3000-CHECK-STATUS logic.
        """
        status = "healthy"
        checks = {}

        # Database connectivity
        try:
            self.session.execute(text("SELECT 1"))
            checks["database"] = "ok"
        except Exception as e:
            checks["database"] = f"error: {e}"
            status = "unhealthy"

        # Table accessibility
        try:
            self.session.scalar(select(func.count()).select_from(PortfolioMaster))
            checks["tables"] = "ok"
        except Exception as e:
            checks["tables"] = f"error: {e}"
            status = "degraded"

        return {
            "status": status,
            "timestamp": datetime.now().isoformat(),
            "checks": checks,
        }

    def _get_db_metrics(self) -> dict:
        """Get database metrics. Translates 2140-GET-DB2-METRICS."""
        try:
            self.session.execute(text("SELECT 1"))
            return {"status": "connected", "response_time_ms": 0}
        except Exception as e:
            return {"status": "error", "error": str(e)}

    def _get_table_stats(self) -> dict:
        """Get table row counts."""
        stats = {}
        for table_cls in [PortfolioMaster, InvestmentPosition, TransactionHistory, AuditLog, ErrorLog]:
            try:
                count = self.session.scalar(select(func.count()).select_from(table_cls)) or 0
                stats[table_cls.__tablename__] = count
            except Exception:
                stats[table_cls.__tablename__] = -1
        return stats

    def _get_batch_metrics(self) -> dict:
        """Get batch processing metrics."""
        try:
            total = self.session.scalar(select(func.count()).select_from(BatchControl)) or 0
            active = self.session.scalar(
                select(func.count()).select_from(BatchControl).where(BatchControl.status == "A")
            ) or 0
            failed = self.session.scalar(
                select(func.count()).select_from(BatchControl).where(BatchControl.status == "E")
            ) or 0
            return {"total_jobs": total, "active_jobs": active, "failed_jobs": failed}
        except Exception:
            return {"total_jobs": 0, "active_jobs": 0, "failed_jobs": 0}

    def _get_error_metrics(self) -> dict:
        """Get recent error metrics."""
        try:
            from datetime import timedelta

            one_hour_ago = datetime.now() - timedelta(hours=1)
            recent = self.session.scalar(
                select(func.count()).select_from(ErrorLog).where(ErrorLog.timestamp >= one_hour_ago)
            ) or 0
            total = self.session.scalar(select(func.count()).select_from(ErrorLog)) or 0
            return {"total_errors": total, "recent_error_count": recent}
        except Exception:
            return {"total_errors": 0, "recent_error_count": 0}
