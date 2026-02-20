"""System Statistics Report Generator - migrated from RPTSTA00.cbl.

Generates system performance and statistics report including processing
statistics, performance metrics, resource utilization, and trend analysis.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "RPTSTA00"
REPORT_WIDTH = 132


class StatisticsReportGenerator:
    def __init__(self):
        self._report_lines: list[str] = []
        self._page_number = 0

    def generate(
        self,
        db2_stats: Optional[dict] = None,
        batch_stats: Optional[dict] = None,
        report_date: Optional[str] = None,
    ) -> int:
        if report_date is None:
            report_date = datetime.now().strftime("%Y-%m-%d")

        if db2_stats is None:
            db2_stats = {}
        if batch_stats is None:
            batch_stats = {}

        self._report_lines = []
        self._page_number = 0

        self._write_report_header(report_date)
        self._write_processing_statistics(batch_stats)
        self._write_performance_metrics(db2_stats)
        self._write_resource_utilization(db2_stats, batch_stats)
        self._write_trend_analysis(batch_stats)
        self._write_report_footer()

        return ReturnCode.SUCCESS

    def _write_report_header(self, report_date: str) -> None:
        self._page_number += 1
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(f"{'SYSTEM STATISTICS REPORT':^{REPORT_WIDTH}}")
        self._write_line(f"{'Report Date: ' + report_date:^{REPORT_WIDTH}}")
        self._write_line("=" * REPORT_WIDTH)
        self._write_line("")

    def _write_processing_statistics(self, batch_stats: dict) -> None:
        self._write_line("SECTION 1: PROCESSING STATISTICS")
        self._write_line("-" * REPORT_WIDTH)

        self._write_line(
            f"  Total Jobs Processed:      {batch_stats.get('total_jobs', 0):>10}"
        )
        self._write_line(
            f"  Successful Jobs:           {batch_stats.get('successful_jobs', 0):>10}"
        )
        self._write_line(
            f"  Failed Jobs:               {batch_stats.get('failed_jobs', 0):>10}"
        )
        self._write_line(
            f"  Total Records Processed:   {batch_stats.get('total_records', 0):>10}"
        )
        self._write_line(
            f"  Total Errors:              {batch_stats.get('total_errors', 0):>10}"
        )
        self._write_line("")

    def _write_performance_metrics(self, db2_stats: dict) -> None:
        self._write_line("SECTION 2: PERFORMANCE METRICS")
        self._write_line("-" * REPORT_WIDTH)

        self._write_line(
            f"  DB2 Rows Read:             {db2_stats.get('rows_read', 0):>10}"
        )
        self._write_line(
            f"  DB2 Rows Inserted:         {db2_stats.get('rows_inserted', 0):>10}"
        )
        self._write_line(
            f"  DB2 Rows Updated:          {db2_stats.get('rows_updated', 0):>10}"
        )
        self._write_line(
            f"  DB2 Rows Deleted:          {db2_stats.get('rows_deleted', 0):>10}"
        )
        self._write_line(
            f"  DB2 Commits:               {db2_stats.get('commits', 0):>10}"
        )
        self._write_line(
            f"  DB2 Rollbacks:             {db2_stats.get('rollbacks', 0):>10}"
        )

        elapsed = db2_stats.get("elapsed_time", 0.0)
        self._write_line(f"  Elapsed Time (seconds):    {elapsed:>10.2f}")
        self._write_line("")

    def _write_resource_utilization(self, db2_stats: dict, batch_stats: dict) -> None:
        self._write_line("SECTION 3: RESOURCE UTILIZATION")
        self._write_line("-" * REPORT_WIDTH)

        total_io = (
            db2_stats.get("rows_read", 0)
            + db2_stats.get("rows_inserted", 0)
            + db2_stats.get("rows_updated", 0)
            + db2_stats.get("rows_deleted", 0)
        )
        self._write_line(f"  Total I/O Operations:      {total_io:>10}")
        self._write_line(
            f"  Total File Operations:     {batch_stats.get('file_operations', 0):>10}"
        )

        elapsed = db2_stats.get("elapsed_time", 0.0)
        if elapsed > 0:
            throughput = total_io / elapsed
            self._write_line(f"  Throughput (ops/sec):       {throughput:>10.2f}")
        self._write_line("")

    def _write_trend_analysis(self, batch_stats: dict) -> None:
        self._write_line("SECTION 4: TREND ANALYSIS")
        self._write_line("-" * REPORT_WIDTH)

        history = batch_stats.get("history", [])
        if history:
            for entry in history[-5:]:
                self._write_line(
                    f"  {entry.get('date', 'N/A')}: "
                    f"Jobs={entry.get('jobs', 0)}, "
                    f"Records={entry.get('records', 0)}, "
                    f"Errors={entry.get('errors', 0)}"
                )
        else:
            self._write_line("  No historical data available")
        self._write_line("")

    def _write_report_footer(self) -> None:
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(
            f"Report Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
        )
        self._write_line("=" * REPORT_WIDTH)

    def _write_line(self, text: str) -> None:
        self._report_lines.append(text)

    def save_report(self, file_path: str) -> int:
        try:
            with open(file_path, "w") as f:
                for line in self._report_lines:
                    f.write(line + "\n")
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving statistics report: %s", e)
            return ReturnCode.ERROR

    def get_report_text(self) -> str:
        return "\n".join(self._report_lines)
