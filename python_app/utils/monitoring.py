"""System Monitoring module - replaces UTLMON00.cbl.

Provides system monitoring with configurable thresholds and alerting.

COBOL program flow (EVALUATE LS-MON-FUNCTION):
- INIT: Initialize monitoring (P100-INITIALIZE)
- CHEK: Check system resources (P200-CHECK-RESOURCES)
- ALRT: Process alerts (P300-PROCESS-ALERTS)
- STAT: Display statistics (P400-DISPLAY-STATS)
"""

import logging
import os
import time
from datetime import datetime
from typing import Any

logger = logging.getLogger("portfolio.utils.monitoring")


class MonitoringThresholds:
    """Monitoring thresholds matching COBOL WS-THRESHOLDS."""

    def __init__(
        self,
        cpu_warning: float = 80.0,
        cpu_critical: float = 95.0,
        memory_warning: float = 80.0,
        memory_critical: float = 95.0,
        disk_warning: float = 85.0,
        disk_critical: float = 95.0,
        connection_warning: int = 80,
        connection_critical: int = 95,
    ) -> None:
        self.cpu_warning = cpu_warning
        self.cpu_critical = cpu_critical
        self.memory_warning = memory_warning
        self.memory_critical = memory_critical
        self.disk_warning = disk_warning
        self.disk_critical = disk_critical
        self.connection_warning = connection_warning
        self.connection_critical = connection_critical


class Alert:
    """System alert record."""

    def __init__(
        self,
        alert_type: str,
        severity: str,
        resource: str,
        value: float,
        threshold: float,
        message: str,
    ) -> None:
        self.timestamp = datetime.now().isoformat()
        self.alert_type = alert_type
        self.severity = severity
        self.resource = resource
        self.value = value
        self.threshold = threshold
        self.message = message


class SystemMonitor:
    """System monitor replacing UTLMON00.cbl.

    Monitors system resources and generates alerts when
    thresholds are exceeded.
    """

    def __init__(self, thresholds: MonitoringThresholds | None = None) -> None:
        self.thresholds = thresholds or MonitoringThresholds()
        self.alerts: list[Alert] = []
        self.check_count = 0
        self.start_time = time.time()
        self.metrics_history: list[dict[str, Any]] = []

    def initialize(self) -> None:
        """Initialize monitoring - replaces P100-INITIALIZE."""
        self.alerts.clear()
        self.check_count = 0
        self.start_time = time.time()
        self.metrics_history.clear()
        logger.info("UTLMON00 INIT: System monitoring initialized")

    def check_resources(self) -> dict[str, Any]:
        """Check system resources - replaces P200-CHECK-RESOURCES.

        COBOL: Reads system metrics from MVS control blocks.
        Python: Uses os/psutil-compatible checks.
        """
        metrics: dict[str, Any] = {
            "timestamp": datetime.now().isoformat(),
            "cpu_percent": self._get_cpu_usage(),
            "memory_percent": self._get_memory_usage(),
            "disk_percent": self._get_disk_usage(),
            "load_average": os.getloadavg() if hasattr(os, "getloadavg") else (0, 0, 0),
        }

        self.check_count += 1
        self.metrics_history.append(metrics)

        # Check thresholds and generate alerts
        self._check_threshold(
            "CPU", metrics["cpu_percent"],
            self.thresholds.cpu_warning, self.thresholds.cpu_critical,
        )
        self._check_threshold(
            "MEMORY", metrics["memory_percent"],
            self.thresholds.memory_warning, self.thresholds.memory_critical,
        )
        self._check_threshold(
            "DISK", metrics["disk_percent"],
            self.thresholds.disk_warning, self.thresholds.disk_critical,
        )

        return metrics

    def _check_threshold(
        self,
        resource: str,
        value: float,
        warning_threshold: float,
        critical_threshold: float,
    ) -> None:
        """Check a metric against thresholds and generate alerts."""
        if value >= critical_threshold:
            alert = Alert(
                alert_type="CRITICAL",
                severity="CRITICAL",
                resource=resource,
                value=value,
                threshold=critical_threshold,
                message=f"{resource} usage at {value:.1f}% (critical threshold: {critical_threshold:.1f}%)",
            )
            self.alerts.append(alert)
            logger.critical("UTLMON00: %s", alert.message)
        elif value >= warning_threshold:
            alert = Alert(
                alert_type="WARNING",
                severity="WARNING",
                resource=resource,
                value=value,
                threshold=warning_threshold,
                message=f"{resource} usage at {value:.1f}% (warning threshold: {warning_threshold:.1f}%)",
            )
            self.alerts.append(alert)
            logger.warning("UTLMON00: %s", alert.message)

    def process_alerts(self) -> list[dict[str, Any]]:
        """Process pending alerts - replaces P300-PROCESS-ALERTS."""
        alert_data = []
        for alert in self.alerts:
            alert_data.append({
                "timestamp": alert.timestamp,
                "type": alert.alert_type,
                "severity": alert.severity,
                "resource": alert.resource,
                "value": alert.value,
                "threshold": alert.threshold,
                "message": alert.message,
            })
        return alert_data

    def get_stats(self) -> dict[str, Any]:
        """Get monitoring statistics - replaces P400-DISPLAY-STATS."""
        elapsed = time.time() - self.start_time
        return {
            "check_count": self.check_count,
            "alert_count": len(self.alerts),
            "critical_alerts": sum(1 for a in self.alerts if a.severity == "CRITICAL"),
            "warning_alerts": sum(1 for a in self.alerts if a.severity == "WARNING"),
            "elapsed_seconds": round(elapsed, 2),
            "latest_metrics": self.metrics_history[-1] if self.metrics_history else None,
        }

    @staticmethod
    def _get_cpu_usage() -> float:
        """Get CPU usage percentage."""
        try:
            load = os.getloadavg()[0] if hasattr(os, "getloadavg") else 0
            cpu_count = os.cpu_count() or 1
            return min(load / cpu_count * 100, 100.0)
        except Exception:
            return 0.0

    @staticmethod
    def _get_memory_usage() -> float:
        """Get memory usage percentage."""
        try:
            with open("/proc/meminfo") as f:
                lines = f.readlines()
            mem_total = int(lines[0].split()[1])
            mem_available = int(lines[2].split()[1])
            return (1 - mem_available / mem_total) * 100 if mem_total > 0 else 0.0
        except Exception:
            return 0.0

    @staticmethod
    def _get_disk_usage() -> float:
        """Get disk usage percentage."""
        try:
            stat = os.statvfs("/")
            total = stat.f_blocks * stat.f_frsize
            free = stat.f_bfree * stat.f_frsize
            return ((total - free) / total) * 100 if total > 0 else 0.0
        except Exception:
            return 0.0
