"""Error Handling model - converted from ERRHAND.cpy.

COBOL Original:
01  ERR-CATEGORIES.
    05  ERR-CAT-VSAM        PIC X(2) VALUE 'VS'.
    05  ERR-CAT-VALID       PIC X(2) VALUE 'VL'.
    05  ERR-CAT-PROC        PIC X(2) VALUE 'PR'.
    05  ERR-CAT-SYSTEM      PIC X(2) VALUE 'SY'.

01  ERR-RETURN-CODES.
    05  ERR-SUCCESS         PIC S9(4) COMP VALUE +0.
    05  ERR-WARNING         PIC S9(4) COMP VALUE +4.
    05  ERR-ERROR           PIC S9(4) COMP VALUE +8.
    05  ERR-SEVERE          PIC S9(4) COMP VALUE +12.
    05  ERR-TERMINAL        PIC S9(4) COMP VALUE +16.

01  ERR-MESSAGE.
    05  ERR-TIMESTAMP.
        10  ERR-DATE        PIC X(10).
        10  ERR-TIME        PIC X(8).
    05  ERR-PROGRAM         PIC X(8).
    05  ERR-CATEGORY        PIC X(2).
    05  ERR-CODE            PIC X(4).
    05  ERR-SEVERITY        PIC S9(4) COMP.
    05  ERR-TEXT            PIC X(80).
    05  ERR-DETAILS         PIC X(256).

01  ERR-VSAM-STATUSES.
    05  ERR-VSAM-SUCCESS    PIC X(2) VALUE '00'.
    05  ERR-VSAM-DUPKEY     PIC X(2) VALUE '22'.
    05  ERR-VSAM-NOTFND     PIC X(2) VALUE '23'.
    05  ERR-VSAM-EOF        PIC X(2) VALUE '10'.
"""

from datetime import datetime
from enum import Enum, IntEnum

from pydantic import BaseModel, Field


class ErrorCategory(str, Enum):
    """Error categories - maps to ERR-CATEGORIES in COBOL."""

    VSAM = "VS"  # ERR-CAT-VSAM - File/database errors
    VALIDATION = "VL"  # ERR-CAT-VALID - Validation errors
    PROCESSING = "PR"  # ERR-CAT-PROC - Processing errors
    SYSTEM = "SY"  # ERR-CAT-SYSTEM - System errors


class ReturnCode(IntEnum):
    """Return codes - maps to ERR-RETURN-CODES in COBOL."""

    SUCCESS = 0  # ERR-SUCCESS
    WARNING = 4  # ERR-WARNING
    ERROR = 8  # ERR-ERROR
    SEVERE = 12  # ERR-SEVERE
    TERMINAL = 16  # ERR-TERMINAL


class VSAMStatus(str, Enum):
    """VSAM file status codes - maps to ERR-VSAM-STATUSES in COBOL."""

    SUCCESS = "00"  # ERR-VSAM-SUCCESS
    DUPLICATE_KEY = "22"  # ERR-VSAM-DUPKEY
    NOT_FOUND = "23"  # ERR-VSAM-NOTFND
    END_OF_FILE = "10"  # ERR-VSAM-EOF


class ErrorTimestamp(BaseModel):
    """Error timestamp structure - maps to ERR-TIMESTAMP in COBOL."""

    date: str = Field(max_length=10, description="Error date")
    time: str = Field(max_length=8, description="Error time")

    @classmethod
    def now(cls) -> "ErrorTimestamp":
        """Create timestamp for current date/time."""
        now = datetime.now()
        return cls(
            date=now.strftime("%Y-%m-%d"),
            time=now.strftime("%H:%M:%S"),
        )


class ErrorMessage(BaseModel):
    """Error message structure - maps to ERR-MESSAGE in COBOL.

    This model represents a standardized error message format used
    throughout the application for consistent error handling.
    """

    timestamp: ErrorTimestamp = Field(default_factory=ErrorTimestamp.now)
    program: str = Field(max_length=8, description="Program name")
    category: ErrorCategory = Field(description="Error category")
    code: str = Field(max_length=4, description="Error code")
    severity: ReturnCode = Field(description="Error severity")
    text: str = Field(max_length=80, description="Error text")
    details: str = Field(default="", max_length=256, description="Error details")

    @property
    def is_success(self) -> bool:
        """Check if this is a success (no error)."""
        return self.severity == ReturnCode.SUCCESS

    @property
    def is_warning(self) -> bool:
        """Check if this is a warning."""
        return self.severity == ReturnCode.WARNING

    @property
    def is_error(self) -> bool:
        """Check if this is an error."""
        return self.severity >= ReturnCode.ERROR

    @property
    def is_severe(self) -> bool:
        """Check if this is a severe error."""
        return self.severity >= ReturnCode.SEVERE

    @property
    def is_terminal(self) -> bool:
        """Check if this is a terminal error."""
        return self.severity == ReturnCode.TERMINAL

    def to_flat_dict(self) -> dict:
        """Convert to flat dictionary for database operations."""
        return {
            "error_date": self.timestamp.date,
            "error_time": self.timestamp.time,
            "program": self.program,
            "category": self.category.value,
            "code": self.code,
            "severity": self.severity.value,
            "text": self.text,
            "details": self.details,
        }

    @classmethod
    def create_vsam_error(
        cls,
        program: str,
        status: VSAMStatus,
        details: str = "",
    ) -> "ErrorMessage":
        """Factory method to create a VSAM error message."""
        error_texts = {
            VSAMStatus.SUCCESS: "Operation successful",
            VSAMStatus.DUPLICATE_KEY: "Duplicate record key",
            VSAMStatus.NOT_FOUND: "Record not found",
            VSAMStatus.END_OF_FILE: "End of file reached",
        }
        severity = (
            ReturnCode.SUCCESS
            if status == VSAMStatus.SUCCESS
            else ReturnCode.ERROR
        )
        return cls(
            program=program,
            category=ErrorCategory.VSAM,
            code=status.value,
            severity=severity,
            text=error_texts.get(status, "Unknown VSAM error"),
            details=details,
        )

    @classmethod
    def create_validation_error(
        cls,
        program: str,
        code: str,
        text: str,
        details: str = "",
    ) -> "ErrorMessage":
        """Factory method to create a validation error message."""
        return cls(
            program=program,
            category=ErrorCategory.VALIDATION,
            code=code,
            severity=ReturnCode.ERROR,
            text=text,
            details=details,
        )

    @classmethod
    def create_processing_error(
        cls,
        program: str,
        code: str,
        text: str,
        details: str = "",
        severity: ReturnCode = ReturnCode.ERROR,
    ) -> "ErrorMessage":
        """Factory method to create a processing error message."""
        return cls(
            program=program,
            category=ErrorCategory.PROCESSING,
            code=code,
            severity=severity,
            text=text,
            details=details,
        )

    @classmethod
    def create_system_error(
        cls,
        program: str,
        code: str,
        text: str,
        details: str = "",
    ) -> "ErrorMessage":
        """Factory method to create a system error message."""
        return cls(
            program=program,
            category=ErrorCategory.SYSTEM,
            code=code,
            severity=ReturnCode.SEVERE,
            text=text,
            details=details,
        )
