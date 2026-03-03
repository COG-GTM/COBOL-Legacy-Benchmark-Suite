"""System Statistics Report module - replaces RPTSTA00.cbl.

Generates system statistics reports showing processing metrics,
error rates, and performance data.

COBOL program flow:
- 0000-MAIN: Initialize -> Process -> Terminate
- 1000-INITIALIZE: Open files, print headers
- 2000-PROCESS: Read statistics, format sections
- 2100-PROCESSING-STATS: Transaction processing stats
- 2200-ERROR-STATS: Error analysis section
- 2300-PERFORMANCE-STATS: Performance metrics
- 3000-TERMINATE: Print summary, close files
"""

import logging
from datetime import datetime
from typing import Any

from python_app.common.db2 import StatisticsCollector
from python_app.common.errors import ErrorHandler
from python_app.models.return_code import RC_SUCCESS

logger = logging.getLogger("portfolio.batch.rptsta00")


class StatisticsReportGenerator:
    """System statistics report generator replacing RPTSTA00.cbl."""

    REPORT_TITLE = "SYSTEM STATISTICS REPORT"
    LINE_WIDTH = 132

    def __init__(self) -> None:
        self.error_handler = ErrorHandler("RPTSTA00")
        self.stats = StatisticsCollector("RPTSTA00")
        self.report_lines: list[str] = []

    def initialize(self, report_date: str = "") -> None:
        """Initialize report - replaces 1000-INITIALIZE."""
        self.stats.initialize()
        self.report_lines.clear()

        if not report_date:
            report_date = datetime.now().strftime("%Y%m%d")

        self.report_lines.extend([
            "=" * self.LINE_WIDTH,
            f" {self.REPORT_TITLE}".center(self.LINE_WIDTH),
            f" Report Date: {report_date}".center(self.LINE_WIDTH),
            "=" * self.LINE_WIDTH,
        ])

    def processing_stats(self, stats: dict[str, Any]) -> None:
        """Format processing statistics section - replaces 2100-PROCESSING-STATS."""
        self.report_lines.extend([
            "",
            " PROCESSING STATISTICS",
            "-" * self.LINE_WIDTH,
            f"   Transactions Read:      {stats.get('transactions_read', 0):>12,}",
            f"   Transactions Valid:     {stats.get('transactions_valid', 0):>12,}",
            f"   Transactions Error:     {stats.get('transactions_error', 0):>12,}",
            f"   Positions Updated:      {stats.get('positions_updated', 0):>12,}",
            f"   Positions Inserted:     {stats.get('positions_inserted', 0):>12,}",
            f"   History Records Loaded: {stats.get('history_loaded', 0):>12,}",
            f"   Reports Generated:      {stats.get('reports_generated', 0):>12,}",
        ])

    def error_stats(self, stats: dict[str, Any]) -> None:
        """Format error statistics section - replaces 2200-ERROR-STATS."""
        total_errors = stats.get("total_errors", 0)
        total_records = stats.get("total_records", 1)
        error_rate = (total_errors / total_records * 100) if total_records > 0 else 0

        self.report_lines.extend([
            "",
            " ERROR STATISTICS",
            "-" * self.LINE_WIDTH,
            f"   Total Errors:           {total_errors:>12,}",
            f"   Validation Errors:      {stats.get('validation_errors', 0):>12,}",
            f"   Database Errors:        {stats.get('database_errors', 0):>12,}",
            f"   Processing Errors:      {stats.get('processing_errors', 0):>12,}",
            f"   Error Rate:             {error_rate:>11.2f}%",
        ])

    def performance_stats(self, stats: dict[str, Any]) -> None:
        """Format performance statistics section - replaces 2300-PERFORMANCE-STATS."""
        self.report_lines.extend([
            "",
            " PERFORMANCE STATISTICS",
            "-" * self.LINE_WIDTH,
            f"   Elapsed Time (sec):     {stats.get('elapsed_seconds', 0):>12.2f}",
            f"   Records/Second:         {stats.get('records_per_second', 0):>12.2f}",
            f"   Commits:                {stats.get('commits', 0):>12,}",
            f"   Rollbacks:              {stats.get('rollbacks', 0):>12,}",
            f"   DB Connections Used:    {stats.get('connections_used', 0):>12,}",
        ])

    def process_batch(
        self,
        processing: dict[str, Any],
        errors: dict[str, Any],
        performance: dict[str, Any],
        report_date: str = "",
    ) -> int:
        """Generate the full statistics report - replaces 0000-MAIN."""
        self.initialize(report_date)
        self.processing_stats(processing)
        self.error_stats(errors)
        self.performance_stats(performance)
        return self.terminate()

    def terminate(self) -> int:
        """Print summary and finalize - replaces 3000-TERMINATE."""
        self.report_lines.extend([
            "",
            "=" * self.LINE_WIDTH,
            f" Report generated: {datetime.now().isoformat()}",
            "=" * self.LINE_WIDTH,
        ])

        self.stats.terminate()
        logger.info("RPTSTA00 complete")
        return RC_SUCCESS

    def get_report(self) -> str:
        """Get the formatted report as a string."""
        return "\n".join(self.report_lines)

    def get_report_data(self) -> dict[str, Any]:
        """Get report data as structured dict (for JSON API)."""
        return {
            "title": self.REPORT_TITLE,
            "generated_at": datetime.now().isoformat(),
        }
