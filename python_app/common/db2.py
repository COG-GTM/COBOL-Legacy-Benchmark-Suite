"""DB2 utility modules - replaces DB2CMT.cbl, DB2ERR.cbl, DB2STAT.cbl.

Provides commit control, SQL error handling, and statistics collection
using SQLAlchemy sessions instead of embedded DB2 SQL.

DB2CMT.cbl functions: INIT, CMIT, RBAK, SAVE, REST, STAT
DB2ERR.cbl functions: LOG, DIAG, RETR
DB2STAT.cbl functions: INIT, UPDT, TERM, DISP
"""

import logging
import time
from datetime import datetime
from typing import Any

from sqlalchemy.orm import Session

logger = logging.getLogger("portfolio.db2")


# ---------------------------------------------------------------------------
# DB2CMT - Commit Controller (replaces DB2CMT.cbl)
# ---------------------------------------------------------------------------

class CommitController:
    """Database commit controller replacing DB2CMT.cbl.

    Manages commit intervals, rollback, savepoints, and statistics
    for batch processing operations.

    COBOL functions:
    - P100-INITIALIZE: Reset counters
    - P200-COMMIT: Commit with interval tracking
    - P300-ROLLBACK: Rollback current transaction
    - P400-SAVEPOINT: Create savepoint
    - P500-RESTORE: Restore to savepoint
    - P600-GET-STATS: Return commit statistics
    """

    def __init__(self, commit_interval: int = 1000) -> None:
        self.commit_interval = commit_interval
        self.commit_count = 0
        self.rollback_count = 0
        self.records_since_commit = 0
        self.total_records = 0
        self.start_time = time.time()

    def initialize(self) -> None:
        """Reset all counters - replaces P100-INITIALIZE."""
        self.commit_count = 0
        self.rollback_count = 0
        self.records_since_commit = 0
        self.total_records = 0
        self.start_time = time.time()

    def should_commit(self) -> bool:
        """Check if commit interval has been reached."""
        return self.records_since_commit >= self.commit_interval

    def commit(self, session: Session) -> None:
        """Commit the current transaction - replaces P200-COMMIT.

        COBOL: EXEC SQL COMMIT WORK END-EXEC
        """
        try:
            session.commit()
            self.commit_count += 1
            self.records_since_commit = 0
            logger.debug("Commit #%d completed (%d total records)", self.commit_count, self.total_records)
        except Exception as exc:
            logger.error("Commit failed: %s", exc)
            raise

    def rollback(self, session: Session) -> None:
        """Rollback the current transaction - replaces P300-ROLLBACK.

        COBOL: EXEC SQL ROLLBACK WORK END-EXEC
        """
        try:
            session.rollback()
            self.rollback_count += 1
            logger.warning("Rollback #%d executed", self.rollback_count)
        except Exception as exc:
            logger.error("Rollback failed: %s", exc)
            raise

    def savepoint(self, session: Session, name: str = "SP1") -> None:
        """Create a savepoint - replaces P400-SAVEPOINT.

        COBOL: EXEC SQL SAVEPOINT :WS-SAVE-NAME END-EXEC
        """
        session.begin_nested()
        logger.debug("Savepoint '%s' created", name)

    def restore(self, session: Session, name: str = "SP1") -> None:
        """Restore to savepoint - replaces P500-RESTORE.

        COBOL: EXEC SQL ROLLBACK TO SAVEPOINT :WS-SAVE-NAME END-EXEC
        """
        session.rollback()
        logger.debug("Restored to savepoint '%s'", name)

    def increment_records(self, count: int = 1) -> None:
        """Track record processing count."""
        self.records_since_commit += count
        self.total_records += count

    def get_stats(self) -> dict[str, Any]:
        """Get commit statistics - replaces P600-GET-STATS."""
        elapsed = time.time() - self.start_time
        return {
            "commit_count": self.commit_count,
            "rollback_count": self.rollback_count,
            "total_records": self.total_records,
            "records_since_commit": self.records_since_commit,
            "elapsed_seconds": round(elapsed, 2),
            "records_per_second": round(self.total_records / elapsed, 2) if elapsed > 0 else 0,
        }


# ---------------------------------------------------------------------------
# DB2ERR - SQL Error Handler (replaces DB2ERR.cbl)
# ---------------------------------------------------------------------------

class SQLErrorHandler:
    """SQL error handler replacing DB2ERR.cbl.

    Provides:
    - P100-LOG-ERROR: Log SQL errors with diagnostic info
    - P200-DIAGNOSE-ERROR: Analyze SQLCODE and suggest action
    - P300-RETRIEVE-ERRORS: Retrieve error history
    """

    def __init__(self, program_id: str) -> None:
        self.program_id = program_id
        self.errors: list[dict[str, Any]] = []
        self.error_count = 0

    def log_error(
        self,
        operation: str,
        exc: Exception,
        *,
        table: str = "",
        sqlcode: int = 0,
    ) -> None:
        """Log SQL error - replaces P100-LOG-ERROR (1200-INSERT-ERROR)."""
        error_record = {
            "timestamp": datetime.now().isoformat(),
            "program_id": self.program_id,
            "operation": operation,
            "table": table,
            "sqlcode": sqlcode,
            "error_type": type(exc).__name__,
            "message": str(exc),
        }
        self.errors.append(error_record)
        self.error_count += 1
        logger.error(
            "[%s] SQL error in %s on %s: sqlcode=%d, %s",
            self.program_id, operation, table, sqlcode, exc,
        )

    def diagnose_error(self, sqlcode: int) -> dict[str, str]:
        """Diagnose SQL error - replaces P200-DIAGNOSE-ERROR.

        Maps common SQLCODE values to actions (from COBOL EVALUATE).
        """
        diagnostics: dict[int, dict[str, str]] = {
            0: {"severity": "INFO", "action": "CONTINUE", "message": "Successful"},
            100: {"severity": "INFO", "action": "CONTINUE", "message": "No data found"},
            -803: {"severity": "WARNING", "action": "SKIP", "message": "Duplicate key"},
            -911: {"severity": "WARNING", "action": "RETRY", "message": "Deadlock/timeout"},
            -501: {"severity": "ERROR", "action": "ABORT", "message": "Cursor not open"},
            -805: {"severity": "ERROR", "action": "ABORT", "message": "Package not found"},
        }
        return diagnostics.get(
            sqlcode,
            {"severity": "ERROR", "action": "ABORT", "message": f"Unknown SQLCODE: {sqlcode}"},
        )

    def get_errors(self) -> list[dict[str, Any]]:
        """Retrieve error history - replaces P300-RETRIEVE-ERRORS."""
        return list(self.errors)

    def reset(self) -> None:
        """Reset error tracking."""
        self.errors.clear()
        self.error_count = 0


# ---------------------------------------------------------------------------
# DB2STAT - Statistics Collector (replaces DB2STAT.cbl)
# ---------------------------------------------------------------------------

class StatisticsCollector:
    """DB2 statistics collector replacing DB2STAT.cbl.

    Tracks processing statistics for batch and online operations.

    COBOL functions:
    - P100-INITIALIZE: Reset all stats
    - P200-UPDATE-STATS: Increment counters
    - P300-TERMINATE: Calculate final stats
    - P400-DISPLAY-STATS: Format for output
    """

    def __init__(self, program_id: str) -> None:
        self.program_id = program_id
        self.counters: dict[str, int] = {
            "reads": 0,
            "inserts": 0,
            "updates": 0,
            "deletes": 0,
            "commits": 0,
            "rollbacks": 0,
            "errors": 0,
        }
        self.start_time = time.time()
        self.end_time: float | None = None

    def initialize(self) -> None:
        """Reset all statistics - replaces P100-INITIALIZE."""
        for key in self.counters:
            self.counters[key] = 0
        self.start_time = time.time()
        self.end_time = None

    def update(self, stat_type: str, count: int = 1) -> None:
        """Update a statistic counter - replaces P200-UPDATE-STATS."""
        if stat_type in self.counters:
            self.counters[stat_type] += count
        else:
            self.counters[stat_type] = count

    def terminate(self) -> dict[str, Any]:
        """Calculate final statistics - replaces P300-TERMINATE."""
        self.end_time = time.time()
        elapsed = self.end_time - self.start_time
        return {
            "program_id": self.program_id,
            "counters": dict(self.counters),
            "elapsed_seconds": round(elapsed, 2),
            "start_time": datetime.fromtimestamp(self.start_time).isoformat(),
            "end_time": datetime.fromtimestamp(self.end_time).isoformat(),
        }

    def display(self) -> str:
        """Format statistics for display - replaces P400-DISPLAY-STATS."""
        lines = [
            f"{'='*60}",
            f" Statistics for {self.program_id}",
            f"{'='*60}",
        ]
        for key, value in self.counters.items():
            lines.append(f"  {key.capitalize():.<30} {value:>10,}")
        elapsed = (self.end_time or time.time()) - self.start_time
        lines.append(f"  {'Elapsed (seconds)':.<30} {elapsed:>10.2f}")
        lines.append(f"{'='*60}")
        return "\n".join(lines)
