"""System Monitoring Utility - migrated from UTLMON00.cbl.

Monitors system health and performance including resource utilization
tracking, performance metrics collection, threshold monitoring, and
alert generation.
"""

import logging
import os
from dataclasses import dataclass
from datetime import datetime
from typing import Optional


logger = logging.getLogger(__name__)

PROGRAM_ID = "UTLMON00"


@dataclass
class PerformanceMetrics:
    timestamp: str = ""
    cpu_usage: float = 0.0
    memory_usage: float = 0.0
    disk_usage: float = 0.0
    active_connections: int = 0
    transactions_per_second: float = 0.0
    error_rate: float = 0.0


@dataclass
class Alert:
    timestamp: str = ""
    severity: str = ""
    metric: str = ""
    value: float = 0.0
    threshold: float = 0.0
    message: str = ""


@dataclass
class MonitoringThresholds:
    cpu_warning: float = 70.0
    cpu_critical: float = 90.0
    memory_warning: float = 75.0
    memory_critical: float = 90.0
    disk_warning: float = 80.0
    disk_critical: float = 95.0
    error_rate_warning: float = 5.0
    error_rate_critical: float = 10.0


class SystemMonitor:
    def __init__(self, thresholds: Optional[MonitoringThresholds] = None):
        self._thresholds = thresholds or MonitoringThresholds()
        self._metrics_history: list[PerformanceMetrics] = []
        self._alerts: list[Alert] = []
        self._monitoring = False

    def collect_metrics(self) -> PerformanceMetrics:
        metrics = PerformanceMetrics(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
        )

        try:
            load = os.getloadavg()
            cpu_count = os.cpu_count() or 1
            metrics.cpu_usage = (load[0] / cpu_count) * 100
        except (OSError, AttributeError):
            metrics.cpu_usage = 0.0

        try:
            with open("/proc/meminfo", "r") as f:
                meminfo = {}
                for line in f:
                    parts = line.split(":")
                    if len(parts) == 2:
                        key = parts[0].strip()
                        val = parts[1].strip().split()[0]
                        meminfo[key] = int(val)

                total = meminfo.get("MemTotal", 1)
                available = meminfo.get("MemAvailable", total)
                metrics.memory_usage = ((total - available) / total) * 100
        except (FileNotFoundError, ValueError, KeyError):
            metrics.memory_usage = 0.0

        try:
            import shutil
            usage = shutil.disk_usage("/")
            metrics.disk_usage = (usage.used / usage.total) * 100
        except Exception:
            metrics.disk_usage = 0.0

        self._metrics_history.append(metrics)
        return metrics

    def check_thresholds(self, metrics: PerformanceMetrics) -> list[Alert]:
        alerts = []

        if metrics.cpu_usage >= self._thresholds.cpu_critical:
            alerts.append(self._create_alert("CRITICAL", "CPU", metrics.cpu_usage, self._thresholds.cpu_critical))
        elif metrics.cpu_usage >= self._thresholds.cpu_warning:
            alerts.append(self._create_alert("WARNING", "CPU", metrics.cpu_usage, self._thresholds.cpu_warning))

        if metrics.memory_usage >= self._thresholds.memory_critical:
            alerts.append(self._create_alert("CRITICAL", "MEMORY", metrics.memory_usage, self._thresholds.memory_critical))
        elif metrics.memory_usage >= self._thresholds.memory_warning:
            alerts.append(self._create_alert("WARNING", "MEMORY", metrics.memory_usage, self._thresholds.memory_warning))

        if metrics.disk_usage >= self._thresholds.disk_critical:
            alerts.append(self._create_alert("CRITICAL", "DISK", metrics.disk_usage, self._thresholds.disk_critical))
        elif metrics.disk_usage >= self._thresholds.disk_warning:
            alerts.append(self._create_alert("WARNING", "DISK", metrics.disk_usage, self._thresholds.disk_warning))

        if metrics.error_rate >= self._thresholds.error_rate_critical:
            alerts.append(self._create_alert("CRITICAL", "ERROR_RATE", metrics.error_rate, self._thresholds.error_rate_critical))
        elif metrics.error_rate >= self._thresholds.error_rate_warning:
            alerts.append(self._create_alert("WARNING", "ERROR_RATE", metrics.error_rate, self._thresholds.error_rate_warning))

        self._alerts.extend(alerts)
        return alerts

    def _create_alert(
        self, severity: str, metric: str, value: float, threshold: float
    ) -> Alert:
        alert = Alert(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            severity=severity,
            metric=metric,
            value=value,
            threshold=threshold,
            message=f"{metric} at {value:.1f}% exceeds {severity.lower()} threshold of {threshold:.1f}%",
        )

        log_fn = logger.critical if severity == "CRITICAL" else logger.warning
        log_fn("ALERT [%s]: %s", severity, alert.message)

        return alert

    def generate_report(self) -> str:
        lines = [
            "=" * 80,
            f"{'SYSTEM MONITORING REPORT':^80}",
            f"{'Generated: ' + datetime.now().strftime('%Y-%m-%d %H:%M:%S'):^80}",
            "=" * 80,
            "",
        ]

        if self._metrics_history:
            latest = self._metrics_history[-1]
            lines.extend([
                "CURRENT METRICS:",
                f"  CPU Usage:     {latest.cpu_usage:>8.1f}%",
                f"  Memory Usage:  {latest.memory_usage:>8.1f}%",
                f"  Disk Usage:    {latest.disk_usage:>8.1f}%",
                "",
            ])

        if self._alerts:
            lines.extend([
                "ACTIVE ALERTS:",
                "-" * 80,
            ])
            for alert in self._alerts[-10:]:
                lines.append(f"  [{alert.severity}] {alert.timestamp} - {alert.message}")
            lines.append("")

        lines.extend([
            f"Total Metrics Collected: {len(self._metrics_history)}",
            f"Total Alerts Generated: {len(self._alerts)}",
            "=" * 80,
        ])

        return "\n".join(lines)

    def get_metrics_history(self) -> list[PerformanceMetrics]:
        return list(self._metrics_history)

    def get_alerts(self) -> list[Alert]:
        return list(self._alerts)
