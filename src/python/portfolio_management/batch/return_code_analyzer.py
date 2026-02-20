"""Return Code Analysis Utility - migrated from RTNANA00.cbl.

Analyzes return codes across system, generates trend analysis,
identifies error patterns, and produces analysis reports.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "RTNANA00"
REPORT_WIDTH = 132


class ReturnCodeAnalyzer:
    def __init__(self):
        self._report_lines: list[str] = []
        self._return_code_data: list[dict] = []

    def load_data(self, data: list[dict]) -> int:
        self._return_code_data = list(data)
        logger.info("Loaded %d return code records", len(self._return_code_data))
        return ReturnCode.SUCCESS

    def load_from_file(self, file_path: str) -> int:
        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|")
                    if len(parts) >= 5:
                        self._return_code_data.append({
                            "timestamp": parts[0].strip(),
                            "program_id": parts[1].strip(),
                            "return_code": int(parts[2].strip()),
                            "highest_code": int(parts[3].strip()),
                            "status": parts[4].strip(),
                            "message": parts[5].strip() if len(parts) > 5 else "",
                        })
            return ReturnCode.SUCCESS
        except FileNotFoundError:
            logger.warning("Return code file not found: %s", file_path)
            return ReturnCode.WARNING
        except Exception as e:
            logger.error("Error loading return code data: %s", e)
            return ReturnCode.ERROR

    def generate_report(self, report_date: Optional[str] = None) -> int:
        if report_date is None:
            report_date = datetime.now().strftime("%Y-%m-%d")

        self._report_lines = []
        self._write_header(report_date)
        self._write_program_summary()
        self._write_trend_analysis()
        self._write_error_patterns()
        self._write_recommendations()
        self._write_footer()

        return ReturnCode.SUCCESS

    def _write_header(self, report_date: str) -> None:
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(f"{'RETURN CODE ANALYSIS REPORT':^{REPORT_WIDTH}}")
        self._write_line(f"{'Report Date: ' + report_date:^{REPORT_WIDTH}}")
        self._write_line("=" * REPORT_WIDTH)
        self._write_line("")

    def _write_program_summary(self) -> None:
        self._write_line("PROGRAM SUMMARY")
        self._write_line("-" * REPORT_WIDTH)
        self._write_line(
            f"{'Program':<10} {'Executions':>12} {'Avg RC':>10} "
            f"{'Max RC':>10} {'Success %':>12} {'Error %':>12}"
        )
        self._write_line("-" * REPORT_WIDTH)

        programs: dict[str, list[dict]] = {}
        for entry in self._return_code_data:
            prog = entry.get("program_id", "UNKNOWN")
            if prog not in programs:
                programs[prog] = []
            programs[prog].append(entry)

        for prog_id, entries in sorted(programs.items()):
            total = len(entries)
            avg_rc = sum(e["return_code"] for e in entries) / total if total > 0 else 0
            max_rc = max(e["return_code"] for e in entries) if entries else 0
            success = sum(1 for e in entries if e["return_code"] == 0)
            errors = sum(1 for e in entries if e["return_code"] > 4)
            success_pct = (success / total * 100) if total > 0 else 0
            error_pct = (errors / total * 100) if total > 0 else 0

            self._write_line(
                f"{prog_id:<10} {total:>12} {avg_rc:>10.1f} "
                f"{max_rc:>10} {success_pct:>11.1f}% {error_pct:>11.1f}%"
            )

        self._write_line("")

    def _write_trend_analysis(self) -> None:
        self._write_line("TREND ANALYSIS")
        self._write_line("-" * REPORT_WIDTH)

        if not self._return_code_data:
            self._write_line("  No data available for trend analysis")
            self._write_line("")
            return

        dates: dict[str, list[int]] = {}
        for entry in self._return_code_data:
            date = entry.get("timestamp", "")[:10]
            if date not in dates:
                dates[date] = []
            dates[date].append(entry["return_code"])

        for date, codes in sorted(dates.items()):
            avg = sum(codes) / len(codes) if codes else 0
            max_code = max(codes) if codes else 0
            self._write_line(
                f"  {date}: Count={len(codes):>6}, "
                f"Avg RC={avg:>6.1f}, Max RC={max_code:>4}"
            )

        self._write_line("")

    def _write_error_patterns(self) -> None:
        self._write_line("ERROR PATTERNS")
        self._write_line("-" * REPORT_WIDTH)

        error_entries = [e for e in self._return_code_data if e["return_code"] > 0]
        if not error_entries:
            self._write_line("  No errors found")
            self._write_line("")
            return

        rc_counts: dict[int, int] = {}
        for entry in error_entries:
            rc = entry["return_code"]
            rc_counts[rc] = rc_counts.get(rc, 0) + 1

        for rc, count in sorted(rc_counts.items()):
            self._write_line(f"  Return Code {rc:>4}: {count:>6} occurrences")

        self._write_line("")

    def _write_recommendations(self) -> None:
        self._write_line("RECOMMENDATIONS")
        self._write_line("-" * REPORT_WIDTH)

        severe_count = sum(1 for e in self._return_code_data if e["return_code"] >= 12)
        error_count = sum(1 for e in self._return_code_data if 8 <= e["return_code"] < 12)

        if severe_count > 0:
            self._write_line(f"  WARNING: {severe_count} severe errors detected - immediate investigation recommended")
        if error_count > 0:
            self._write_line(f"  ATTENTION: {error_count} errors detected - review error logs")
        if severe_count == 0 and error_count == 0:
            self._write_line("  System operating normally - no issues detected")

        self._write_line("")

    def _write_footer(self) -> None:
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(
            f"Total Records Analyzed: {len(self._return_code_data)}  |  "
            f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
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
            logger.error("Error saving analysis report: %s", e)
            return ReturnCode.ERROR

    def get_report_text(self) -> str:
        return "\n".join(self._report_lines)
