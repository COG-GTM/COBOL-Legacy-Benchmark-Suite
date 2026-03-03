"""Statistics Report Generator.

Replaces: RPTSTA00 (src/programs/batch/RPTSTA00.cbl)

Generates system statistics and performance reports covering
batch processing metrics, error rates, and throughput.

Original COBOL flow (RPTSTA00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-GATHER-STATISTICS
      2100-READ-ERROR-LOG
      2200-READ-RETURN-CODES
      2300-CALCULATE-METRICS
    3000-PRINT-REPORT
    4000-FINALIZE
"""

from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import date, datetime

from jinja2 import Template
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from portfolio_manager.models.database import (
    ErrorLog,
    PositionHistory,
    ReturnCodeLog,
    TransactionHistory,
)

logger = logging.getLogger(__name__)

STATS_REPORT_TEMPLATE = Template(
    """\
<!DOCTYPE html>
<html>
<head>
    <title>System Statistics Report - {{ report_date }}</title>
    <style>
        body { font-family: monospace; margin: 20px; }
        h1, h2 { text-align: center; }
        table { border-collapse: collapse; width: 80%; margin: 10px auto; }
        th, td { border: 1px solid #ddd; padding: 8px; }
        th { background-color: #f2f2f2; text-align: left; }
        td:last-child { text-align: right; }
        .section { margin-top: 30px; }
        .footer { margin-top: 30px; font-size: 0.9em; color: #666; text-align: center; }
    </style>
</head>
<body>
    <h1>SYSTEM STATISTICS REPORT</h1>
    <p style="text-align:center">Report Date: {{ report_date }} | Generated: {{ generated_at }}</p>

    <div class="section">
        <h2>Transaction Processing</h2>
        <table>
            <tr><th>Total Transactions</th><td>{{ stats.total_transactions }}</td></tr>
            <tr><th>Transactions Today</th><td>{{ stats.transactions_today }}</td></tr>
            <tr><th>Position History Records</th><td>{{ stats.total_history }}</td></tr>
        </table>
    </div>

    <div class="section">
        <h2>Error Summary</h2>
        <table>
            <tr><th>Total Errors</th><td>{{ stats.total_errors }}</td></tr>
            <tr><th>Info (Sev 1)</th><td>{{ stats.errors_info }}</td></tr>
            <tr><th>Warning (Sev 2)</th><td>{{ stats.errors_warning }}</td></tr>
            <tr><th>Error (Sev 3)</th><td>{{ stats.errors_error }}</td></tr>
            <tr><th>Severe (Sev 4)</th><td>{{ stats.errors_severe }}</td></tr>
        </table>
    </div>

    <div class="section">
        <h2>Return Code Distribution</h2>
        <table>
            <tr><th>RC 0 (Success)</th><td>{{ stats.rc_0 }}</td></tr>
            <tr><th>RC 4 (Warning)</th><td>{{ stats.rc_4 }}</td></tr>
            <tr><th>RC 8 (Error)</th><td>{{ stats.rc_8 }}</td></tr>
            <tr><th>RC 12+ (Severe)</th><td>{{ stats.rc_12_plus }}</td></tr>
        </table>
    </div>

    {% if error_details %}
    <div class="section">
        <h2>Recent Errors (Last 20)</h2>
        <table>
            <tr><th>Timestamp</th><th>Program</th><th>Code</th><th>Severity</th><th>Message</th></tr>
            {% for err in error_details %}
            <tr>
                <td>{{ err.error_timestamp }}</td>
                <td>{{ err.program_id }}</td>
                <td>{{ err.error_code }}</td>
                <td>{{ err.error_severity }}</td>
                <td>{{ err.error_message[:60] }}</td>
            </tr>
            {% endfor %}
        </table>
    </div>
    {% endif %}

    <div class="footer">
        <p>End of Report | Program: RPTSTA00 (Python)</p>
    </div>
</body>
</html>
"""
)


@dataclass
class SystemStats:
    """System statistics data."""

    total_transactions: int = 0
    transactions_today: int = 0
    total_history: int = 0
    total_errors: int = 0
    errors_info: int = 0
    errors_warning: int = 0
    errors_error: int = 0
    errors_severe: int = 0
    rc_0: int = 0
    rc_4: int = 0
    rc_8: int = 0
    rc_12_plus: int = 0


class StatisticsReportGenerator:
    """Generate system statistics reports.

    Replaces RPTSTA00 (src/programs/batch/RPTSTA00.cbl).
    """

    PROGRAM_ID = "RPTSTA00"

    def __init__(self, session: Session):
        self._session = session

    def generate(
        self,
        report_date: date | None = None,
        output_format: str = "html",
    ) -> str:
        """Generate the statistics report.

        Replaces 0000-MAIN-PROCESS.

        Args:
            report_date: Report date (defaults to today).
            output_format: 'html' or 'text'.

        Returns:
            Rendered report string.
        """
        rpt_date = report_date or date.today()

        logger.info("%s: Generating statistics report", self.PROGRAM_ID)

        stats = self._gather_statistics(rpt_date)
        error_details = self._get_recent_errors()

        template_vars = {
            "report_date": rpt_date,
            "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "stats": stats,
            "error_details": error_details,
        }

        report = STATS_REPORT_TEMPLATE.render(**template_vars)

        logger.info(
            "%s: Report generated — txns=%d errors=%d",
            self.PROGRAM_ID,
            stats.total_transactions,
            stats.total_errors,
        )

        return report

    def _gather_statistics(self, report_date: date) -> SystemStats:
        """Gather system statistics from database.

        Replaces RPTSTA00 paragraph 2000-GATHER-STATISTICS.
        """
        stats = SystemStats()

        # Transaction counts
        stats.total_transactions = (
            self._session.execute(
                select(func.count()).select_from(TransactionHistory)
            ).scalar()
            or 0
        )

        stats.transactions_today = (
            self._session.execute(
                select(func.count())
                .select_from(TransactionHistory)
                .where(TransactionHistory.transaction_date == report_date)
            ).scalar()
            or 0
        )

        stats.total_history = (
            self._session.execute(
                select(func.count()).select_from(PositionHistory)
            ).scalar()
            or 0
        )

        # Error counts by severity
        error_counts = self._session.execute(
            select(ErrorLog.error_severity, func.count())
            .group_by(ErrorLog.error_severity)
        ).all()

        for severity, count in error_counts:
            stats.total_errors += count
            if severity == 1:
                stats.errors_info = count
            elif severity == 2:
                stats.errors_warning = count
            elif severity == 3:
                stats.errors_error = count
            elif severity == 4:
                stats.errors_severe = count

        # Return code distribution
        rc_counts = self._session.execute(
            select(ReturnCodeLog.return_code, func.count())
            .group_by(ReturnCodeLog.return_code)
        ).all()

        for rc, count in rc_counts:
            if rc == 0:
                stats.rc_0 = count
            elif rc == 4:
                stats.rc_4 = count
            elif rc == 8:
                stats.rc_8 = count
            elif rc >= 12:
                stats.rc_12_plus += count

        return stats

    def _get_recent_errors(self, limit: int = 20) -> list[ErrorLog]:
        """Get recent error log entries.

        Replaces RPTSTA00 paragraph 2100-READ-ERROR-LOG.
        """
        return list(
            self._session.execute(
                select(ErrorLog)
                .order_by(ErrorLog.error_timestamp.desc())
                .limit(limit)
            )
            .scalars()
            .all()
        )
