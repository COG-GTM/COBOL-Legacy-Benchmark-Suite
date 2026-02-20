"""DB2 SQL Error Handler - migrated from DB2ERR.cbl.

Handles DB2 errors, diagnoses error conditions, and retrieves error history.
"""

import logging
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Optional

logger = logging.getLogger(__name__)

MAX_ERROR_HISTORY = 100


class DB2ErrorCategory(str, Enum):
    DEADLOCK = "DEADLOCK"
    TIMEOUT = "TIMEOUT"
    CONNECTION = "CONNECTION"
    DUPLICATE_KEY = "DUPLICATE"
    NOT_FOUND = "NOT_FOUND"
    OTHER = "OTHER"


@dataclass
class DB2ErrorEntry:
    timestamp: str = ""
    sqlcode: int = 0
    sqlstate: str = ""
    program_id: str = ""
    error_text: str = ""
    category: str = DB2ErrorCategory.OTHER
    retry_flag: bool = False


@dataclass
class DB2DiagnosisResult:
    category: str = DB2ErrorCategory.OTHER
    retryable: bool = False
    description: str = ""
    recommended_action: str = ""


class DB2ErrorHandler:
    def __init__(self):
        self._error_history: list[DB2ErrorEntry] = []
        self._error_count = 0

    def log_error(
        self,
        sqlcode: int,
        sqlstate: str,
        program_id: str,
        error_text: str,
    ) -> int:
        diagnosis = self._diagnose_sqlcode(sqlcode, sqlstate)

        entry = DB2ErrorEntry(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            sqlcode=sqlcode,
            sqlstate=sqlstate,
            program_id=program_id,
            error_text=error_text,
            category=diagnosis.category,
            retry_flag=diagnosis.retryable,
        )

        if len(self._error_history) >= MAX_ERROR_HISTORY:
            self._error_history.pop(0)
        self._error_history.append(entry)
        self._error_count += 1

        logger.error(
            "DB2 Error - SQLCODE: %d, STATE: %s, Program: %s, Category: %s - %s",
            sqlcode,
            sqlstate,
            program_id,
            diagnosis.category,
            error_text,
        )

        return 0

    def diagnose(self, sqlcode: int, sqlstate: str) -> DB2DiagnosisResult:
        return self._diagnose_sqlcode(sqlcode, sqlstate)

    def _diagnose_sqlcode(self, sqlcode: int, sqlstate: str) -> DB2DiagnosisResult:
        if sqlcode == -911 or sqlstate == "40001":
            return DB2DiagnosisResult(
                category=DB2ErrorCategory.DEADLOCK,
                retryable=True,
                description="Deadlock or timeout detected",
                recommended_action="Retry the operation",
            )

        if sqlcode == -913 or sqlstate == "40003":
            return DB2DiagnosisResult(
                category=DB2ErrorCategory.TIMEOUT,
                retryable=True,
                description="Lock timeout occurred",
                recommended_action="Retry with backoff",
            )

        if sqlcode == -803 or sqlstate == "23505":
            return DB2DiagnosisResult(
                category=DB2ErrorCategory.DUPLICATE_KEY,
                retryable=False,
                description="Duplicate key violation",
                recommended_action="Check for existing record",
            )

        if sqlcode == 100 or sqlstate == "02000":
            return DB2DiagnosisResult(
                category=DB2ErrorCategory.NOT_FOUND,
                retryable=False,
                description="No data found",
                recommended_action="Verify query criteria",
            )

        if sqlstate.startswith("08"):
            return DB2DiagnosisResult(
                category=DB2ErrorCategory.CONNECTION,
                retryable=True,
                description="Connection error",
                recommended_action="Reconnect to database",
            )

        return DB2DiagnosisResult(
            category=DB2ErrorCategory.OTHER,
            retryable=False,
            description=f"SQL error: SQLCODE={sqlcode}, STATE={sqlstate}",
            recommended_action="Investigate error details",
        )

    def retrieve_history(self, program_id: Optional[str] = None) -> list[DB2ErrorEntry]:
        if program_id is None:
            return list(self._error_history)
        return [e for e in self._error_history if e.program_id == program_id]

    @property
    def error_count(self) -> int:
        return self._error_count
