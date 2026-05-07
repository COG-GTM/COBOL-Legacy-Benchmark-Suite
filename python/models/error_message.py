"""Error-message structure from ERRHAND.cpy."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum, IntEnum


class ErrorCategory(str, Enum):
    """ERR-CAT-* values from ERRHAND.cpy."""

    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


class ErrorSeverity(IntEnum):
    """Standard return codes from ERRHAND.cpy mapped to severity levels."""

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    TERMINAL = 16


# VSAM file status codes from ERRHAND.cpy / ERR-VSAM-STATUSES
VSAM_SUCCESS = "00"
VSAM_DUPKEY = "22"
VSAM_NOTFND = "23"
VSAM_EOF = "10"

VSAM_MESSAGES = {
    VSAM_DUPKEY: "Duplicate record key",
    VSAM_NOTFND: "Record not found",
}
VSAM_OTHER_MESSAGE = "Unexpected VSAM error"


@dataclass
class ErrorMessage:
    """Mirror of ERR-MESSAGE from ERRHAND.cpy.

    Field lengths in the COBOL copybook:
        ERR-DATE        PIC X(10)
        ERR-TIME        PIC X(8)
        ERR-PROGRAM     PIC X(8)
        ERR-CATEGORY    PIC X(2)
        ERR-CODE        PIC X(4)
        ERR-SEVERITY    PIC S9(4) COMP
        ERR-TEXT        PIC X(80)
        ERR-DETAILS     PIC X(256)
    """

    program: str = ""
    category: str = ErrorCategory.SYSTEM.value
    code: str = "0000"
    severity: int = ErrorSeverity.ERROR
    text: str = ""
    details: str = ""
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    @property
    def err_date(self) -> str:
        """Formatted ERR-DATE (YYYY-MM-DD, 10 chars)."""
        return self.timestamp.strftime("%Y-%m-%d")

    @property
    def err_time(self) -> str:
        """Formatted ERR-TIME (HH:MM:SS, 8 chars)."""
        return self.timestamp.strftime("%H:%M:%S")

    def __post_init__(self) -> None:
        # Enforce COBOL field-length truncation so that downstream serializers
        # (logs, JSON fixtures) match the legacy fixed-width semantics.
        self.program = self.program[:8]
        self.category = self.category[:2]
        self.code = self.code[:4]
        self.text = self.text[:80]
        self.details = self.details[:256]
