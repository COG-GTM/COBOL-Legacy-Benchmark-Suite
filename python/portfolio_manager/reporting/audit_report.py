"""Audit Report Generator.

Replaces: RPTAUD00 (src/programs/batch/RPTAUD00.cbl)

Generates audit trail reports showing system activity,
user actions, and security events.

Original COBOL flow (RPTAUD00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-PROCESS-RECORDS (read audit log)
    3000-PRINT-SUMMARY
    4000-FINALIZE
"""

from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import date, datetime

from jinja2 import Template
from sqlalchemy import select
from sqlalchemy.orm import Session

from portfolio_manager.models.database import AuditLog

logger = logging.getLogger(__name__)

AUDIT_REPORT_TEMPLATE = Template(
    """\
<!DOCTYPE html>
<html>
<head>
    <title>Audit Trail Report - {{ report_date }}</title>
    <style>
        body { font-family: monospace; margin: 20px; }
        h1 { text-align: center; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 6px; text-align: left; }
        th { background-color: #f2f2f2; }
        .fail { color: red; font-weight: bold; }
        .warn { color: orange; }
        .footer { margin-top: 30px; font-size: 0.9em; color: #666; }
        .summary { margin-top: 20px; }
    </style>
</head>
<body>
    <h1>AUDIT TRAIL REPORT</h1>
    <p>Report Date: {{ report_date }} | Generated: {{ generated_at }}</p>
    {% if date_from %}<p>Period: {{ date_from }} to {{ date_to }}</p>{% endif %}

    <table>
        <thead>
            <tr>
                <th>Timestamp</th>
                <th>User</th>
                <th>Program</th>
                <th>Type</th>
                <th>Action</th>
                <th>Status</th>
                <th>Portfolio</th>
                <th>Message</th>
            </tr>
        </thead>
        <tbody>
        {% for row in rows %}
            <tr>
                <td>{{ row.timestamp }}</td>
                <td>{{ row.user_id }}</td>
                <td>{{ row.program }}</td>
                <td>{{ row.audit_type }}</td>
                <td>{{ row.action }}</td>
                <td class="
                    {%- if row.status == 'FAIL' %} fail
                    {%- elif row.status == 'WARN' %} warn
                    {%- endif %}">
                    {{ row.status }}
                </td>
                <td>{{ row.portfolio_id or '' }}</td>
                <td>{{ row.message or '' }}</td>
            </tr>
        {% endfor %}
        </tbody>
    </table>

    <div class="summary">
        <h3>Summary</h3>
        <p>Total Records: {{ stats.total_records }}</p>
        <p>Success: {{ stats.success_count }} |
           Failures: {{ stats.failure_count }} |
           Warnings: {{ stats.warning_count }}</p>
        <p>Unique Users: {{ stats.unique_users }}</p>
    </div>

    <div class="footer">
        <p>End of Report | Program: RPTAUD00 (Python)</p>
    </div>
</body>
</html>
"""
)

TEXT_AUDIT_TEMPLATE = Template(
    """\
================================================================================
                           AUDIT TRAIL REPORT
================================================================================
Report Date: {{ report_date }}    Generated: {{ generated_at }}
{% if date_from %}Period: {{ date_from }} to {{ date_to }}{% endif %}
--------------------------------------------------------------------------------
Timestamp            User     Program  Type Action   Status Portfolio Message
-------------------- -------- -------- ---- -------- ------ --------- -------------------
{% for row in rows %}
{{ "%-20s" | format(row.timestamp|string) }} {{ "%-8s" | format(row.user_id) }}\
 {{ "%-8s" | format(row.program) }} {{ "%-4s" | format(row.audit_type) }}\
 {{ "%-8s" | format(row.action) }} {{ "%-6s" | format(row.status) }}\
 {{ "%-9s" | format(row.portfolio_id or '') }} {{ (row.message or '')[:19] }}
{% endfor %}
--------------------------------------------------------------------------------
SUMMARY: Total={{ stats.total_records }} Success={{ stats.success_count }}\
 Fail={{ stats.failure_count }} Warn={{ stats.warning_count }}\
 Users={{ stats.unique_users }}
================================================================================
"""
)


@dataclass
class AuditReportStats:
    """Summary statistics for audit report."""

    total_records: int = 0
    success_count: int = 0
    failure_count: int = 0
    warning_count: int = 0
    unique_users: int = 0


class AuditReportGenerator:
    """Generate audit trail reports.

    Replaces RPTAUD00 (src/programs/batch/RPTAUD00.cbl).
    """

    PROGRAM_ID = "RPTAUD00"

    def __init__(self, session: Session):
        self._session = session

    def generate(
        self,
        date_from: date | None = None,
        date_to: date | None = None,
        user_id: str | None = None,
        output_format: str = "html",
    ) -> str:
        """Generate the audit report.

        Args:
            date_from: Start date filter.
            date_to: End date filter.
            user_id: Filter by specific user.
            output_format: 'html' or 'text'.

        Returns:
            Rendered report string.
        """
        logger.info(
            "%s: Generating audit report from=%s to=%s",
            self.PROGRAM_ID,
            date_from,
            date_to,
        )

        query = select(AuditLog).order_by(AuditLog.timestamp.desc())

        if date_from:
            start_dt = datetime.combine(date_from, datetime.min.time())
            query = query.where(AuditLog.timestamp >= start_dt)
        if date_to:
            end_dt = datetime.combine(date_to, datetime.max.time())
            query = query.where(AuditLog.timestamp <= end_dt)
        if user_id:
            query = query.where(AuditLog.user_id == user_id)

        records = self._session.execute(query).scalars().all()

        # Accumulate stats
        stats = AuditReportStats()
        users: set[str] = set()

        for rec in records:
            stats.total_records += 1
            users.add(rec.user_id)
            if rec.status == "SUCC":
                stats.success_count += 1
            elif rec.status == "FAIL":
                stats.failure_count += 1
            elif rec.status == "WARN":
                stats.warning_count += 1

        stats.unique_users = len(users)

        template_vars = {
            "report_date": date.today(),
            "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "date_from": date_from,
            "date_to": date_to,
            "rows": records,
            "stats": stats,
        }

        template = (
            AUDIT_REPORT_TEMPLATE
            if output_format == "html"
            else TEXT_AUDIT_TEMPLATE
        )

        report = template.render(**template_vars)

        logger.info(
            "%s: Report generated — %d records",
            self.PROGRAM_ID,
            stats.total_records,
        )

        return report
