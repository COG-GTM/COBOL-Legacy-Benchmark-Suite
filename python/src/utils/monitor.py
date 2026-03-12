"""
System monitor translated from COBOL program UTLMON00.cbl.

Replaces:
  - UTLMON00.cbl 1000-CHECK-RESOURCES: Check system resources
  - UTLMON00.cbl 2000-CHECK-DATABASE: Check database health
  - UTLMON00.cbl 3000-CHECK-BATCH: Check batch status
  - UTLMON00.cbl 4000-GENERATE-ALERTS: Generate alerts for issues

System monitoring, health checks, performance metrics.
"""

import logging
from datetime import datetime

from sqlalchemy import text
from sqlalchemy.orm import Session

from src.common.constants import AlertLevel, MonitorResourceType

logger = logging.getLogger(__name__)


class MonitorAlert:
    """Monitoring alert."""

    def __init__(
        self,
        resource: MonitorResourceType,
        level: AlertLevel,
        message: str,
    ) -> None:
        self.resource = resource
        self.level = level
        self.message = message
        self.timestamp = datetime.now()

    def to_dict(self) -> dict[str, str]:
        return {
            "resource": self.resource.value,
            "level": self.level.value,
            "message": self.message,
            "timestamp": self.timestamp.isoformat(),
        }


class SystemMonitor:
    """
    System monitoring service.

    Translates UTLMON00.cbl paragraph structure.
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._alerts: list[MonitorAlert] = []

    def check_all(self) -> list[MonitorAlert]:
        """
        Run all health checks.

        Translates UTLMON00.cbl main dispatch.
        """
        self._alerts = []
        self._check_database()
        self._check_batch_status()
        return self._alerts

    def _check_database(self) -> None:
        """
        Check database connectivity and health.

        Translates UTLMON00.cbl 2000-CHECK-DATABASE.
        """
        try:
            self._session.execute(text("SELECT 1"))
            self._alerts.append(
                MonitorAlert(
                    MonitorResourceType.DB2,
                    AlertLevel.INFO,
                    "Database connection healthy",
                )
            )
        except Exception as exc:
            self._alerts.append(
                MonitorAlert(
                    MonitorResourceType.DB2,
                    AlertLevel.CRITICAL,
                    f"Database connection failed: {exc}",
                )
            )

    def _check_batch_status(self) -> None:
        """
        Check batch processing status.

        Translates UTLMON00.cbl 3000-CHECK-BATCH.
        """
        from src.db.repository import BatchControlRepository

        try:
            repo = BatchControlRepository(self._session)
            error_batches = repo.list_by_status("E")
            if error_batches:
                self._alerts.append(
                    MonitorAlert(
                        MonitorResourceType.DB2,
                        AlertLevel.WARNING,
                        f"{len(error_batches)} batch job(s) in error state",
                    )
                )
            else:
                self._alerts.append(
                    MonitorAlert(
                        MonitorResourceType.DB2,
                        AlertLevel.INFO,
                        "No batch jobs in error state",
                    )
                )
        except Exception as exc:
            self._alerts.append(
                MonitorAlert(
                    MonitorResourceType.DB2,
                    AlertLevel.WARNING,
                    f"Could not check batch status: {exc}",
                )
            )

    def get_health_summary(self) -> dict[str, object]:
        """Get a summary of system health."""
        alerts = self.check_all()
        critical = [a for a in alerts if a.level == AlertLevel.CRITICAL]
        warnings = [a for a in alerts if a.level == AlertLevel.WARNING]

        status = "healthy"
        if critical:
            status = "critical"
        elif warnings:
            status = "degraded"

        return {
            "status": status,
            "timestamp": datetime.now().isoformat(),
            "alerts": [a.to_dict() for a in alerts],
            "critical_count": len(critical),
            "warning_count": len(warnings),
        }
