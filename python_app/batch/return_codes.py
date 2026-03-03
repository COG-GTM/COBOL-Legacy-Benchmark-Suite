"""Return Code Handler and Analyzer - replaces RTNCDE00.cbl and RTNANA00.cbl.

RTNCDE00 functions: SET, GET, LOG, RESET
RTNANA00: Cursor-based return code analysis with frequency and trend reporting.
"""

import logging
from collections import Counter
from datetime import datetime
from typing import Any

from python_app.models.return_code import (
    RC_ERROR,
    RC_MAX_CONTINUE,
    RC_SUCCESS,
    RC_WARNING,
    ReturnCodeRecord,
    ReturnCodeStatus,
    classify_return_code,
)

logger = logging.getLogger("portfolio.batch.return_codes")


class ReturnCodeHandler:
    """Return code handler replacing RTNCDE00.cbl.

    Manages return codes with SET/GET/LOG/RESET operations.
    """

    def __init__(self, program_id: str) -> None:
        self.record = ReturnCodeRecord(program_id=program_id)
        self.history: list[dict[str, Any]] = []

    def set_code(self, code: int, message: str = "") -> None:
        """Set return code - replaces P200-SET-RETURN-CODE."""
        self.record.set_code(code)
        self.record.message = message
        logger.info(
            "RC SET: program=%s, code=%d, highest=%d, status=%s",
            self.record.program_id, code, self.record.highest_code, self.record.status,
        )

    def get_code(self) -> tuple[int, int, ReturnCodeStatus]:
        """Get current return code info - replaces P300-GET-RETURN-CODE."""
        return self.record.get_code()

    def log_code(self) -> dict[str, Any]:
        """Log return code - replaces P400-LOG-RETURN-CODE.

        COBOL: INSERT INTO RTNCODES (...) VALUES (...).
        """
        entry = {
            "timestamp": datetime.now().isoformat(),
            "program_id": self.record.program_id,
            "current_code": self.record.current_code,
            "highest_code": self.record.highest_code,
            "status": self.record.status,
            "message": self.record.message,
        }
        self.history.append(entry)
        return entry

    def reset(self) -> None:
        """Reset return code - replaces P500-RESET-RETURN-CODE."""
        self.record.current_code = RC_SUCCESS
        self.record.highest_code = RC_SUCCESS
        self.record.status = ReturnCodeStatus.SUCCESS
        self.record.message = ""

    @property
    def can_continue(self) -> bool:
        """Check if pipeline can continue (RC <= 4)."""
        return self.record.can_continue


class ReturnCodeAnalyzer:
    """Return code analyzer replacing RTNANA00.cbl.

    Provides frequency analysis and trend reporting for return codes
    across multiple programs. Replaces the cursor-based analysis
    in RTNANA00 P200-ANALYZE-CODES.
    """

    def __init__(self) -> None:
        self.entries: list[dict[str, Any]] = []

    def add_entry(
        self,
        program_id: str,
        return_code: int,
        message: str = "",
    ) -> None:
        """Add a return code entry for analysis."""
        self.entries.append({
            "timestamp": datetime.now().isoformat(),
            "program_id": program_id,
            "return_code": return_code,
            "status": classify_return_code(return_code),
            "message": message,
        })

    def analyze(self) -> dict[str, Any]:
        """Analyze return codes - replaces P200-ANALYZE-CODES.

        COBOL: Uses cursor RTNANA-CURSOR to fetch and aggregate
        return code data with FREQ-COUNT and FREQ-PERCENT.
        """
        if not self.entries:
            return {"total": 0, "by_status": {}, "by_program": {}}

        total = len(self.entries)

        # Frequency by status
        status_counts = Counter(e["status"] for e in self.entries)
        by_status = {
            status: {
                "count": count,
                "percent": round(count / total * 100, 2),
            }
            for status, count in status_counts.items()
        }

        # Frequency by program
        program_counts: dict[str, dict[str, int]] = {}
        for entry in self.entries:
            prog = entry["program_id"]
            if prog not in program_counts:
                program_counts[prog] = {"total": 0, "success": 0, "warning": 0, "error": 0}
            program_counts[prog]["total"] += 1
            rc = entry["return_code"]
            if rc == RC_SUCCESS:
                program_counts[prog]["success"] += 1
            elif rc <= RC_WARNING:
                program_counts[prog]["warning"] += 1
            else:
                program_counts[prog]["error"] += 1

        # Trend: recent vs older
        midpoint = total // 2
        recent_errors = sum(
            1 for e in self.entries[midpoint:] if e["return_code"] > RC_MAX_CONTINUE
        )
        older_errors = sum(
            1 for e in self.entries[:midpoint] if e["return_code"] > RC_MAX_CONTINUE
        )

        trend = "STABLE"
        if recent_errors > older_errors * 1.5:
            trend = "INCREASING"
        elif recent_errors < older_errors * 0.5:
            trend = "DECREASING"

        return {
            "total": total,
            "by_status": by_status,
            "by_program": program_counts,
            "trend": trend,
            "recent_error_count": recent_errors,
            "overall_success_rate": round(
                status_counts.get(ReturnCodeStatus.SUCCESS, 0) / total * 100, 2
            ),
        }

    def get_report(self) -> str:
        """Generate analysis report text - replaces P300-GENERATE-REPORT."""
        analysis = self.analyze()
        lines = [
            "=" * 60,
            " RETURN CODE ANALYSIS REPORT",
            "=" * 60,
            f" Total Entries: {analysis['total']}",
            f" Success Rate:  {analysis.get('overall_success_rate', 0):.2f}%",
            f" Error Trend:   {analysis.get('trend', 'N/A')}",
            "",
            " BY STATUS:",
        ]
        for status, data in analysis.get("by_status", {}).items():
            lines.append(f"   {status}: {data['count']} ({data['percent']:.1f}%)")

        lines.extend(["", " BY PROGRAM:"])
        for prog, counts in analysis.get("by_program", {}).items():
            lines.append(
                f"   {prog}: total={counts['total']}, "
                f"success={counts['success']}, "
                f"warning={counts['warning']}, "
                f"error={counts['error']}"
            )

        lines.append("=" * 60)
        return "\n".join(lines)
