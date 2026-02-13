"""Error Handling Model - migrated from COBOL copybook ERRHND.cpy and ERRHAND.cpy

Sources:
    src/copybook/online/ERRHND.cpy  (Online Error Handling)
    src/copybook/common/ERRHAND.cpy (Standard Error Handling Definitions)

COBOL Data Type Mapping:
    PIC X(n)        -> str (fixed-length character)
    PIC S9(9) COMP  -> int (binary fullword, signed)
    PIC S9(8) COMP  -> int (binary fullword, signed)
    PIC S9(4) COMP  -> int (binary halfword, signed)
    PIC X(2) VALUE  -> str constant (class-level defaults)
    88-level conditions -> Enum or validated string constants

This module combines both the online error handling structure (ERRHND.cpy)
and the standard error handling definitions (ERRHAND.cpy) into a unified
Python error model.
"""
from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field
from sqlalchemy import Column, String, Integer, DateTime, Index
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class ErrorSeverity(str, Enum):
    FATAL = "F"
    WARNING = "W"
    INFO = "I"


class ErrorAction(str, Enum):
    RETURN = "R"
    CONTINUE = "C"
    ABEND = "A"


class ErrorCategory(str, Enum):
    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


class ReturnCode(int, Enum):
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    TERMINAL = 16


class VsamStatus(str, Enum):
    SUCCESS = "00"
    DUPLICATE_KEY = "22"
    NOT_FOUND = "23"
    END_OF_FILE = "10"


VSAM_STATUS_MESSAGES = {
    VsamStatus.DUPLICATE_KEY: "Duplicate record key",
    VsamStatus.NOT_FOUND: "Record not found",
}


class ErrorLogORM(Base):
    """SQLAlchemy ORM model for error log records (DB2 ERRLOG table)."""

    __tablename__ = "errlog"

    error_timestamp = Column(DateTime, primary_key=True, nullable=False)
    program_id = Column(String(8), primary_key=True, nullable=False)
    error_type = Column(String(1), nullable=False)
    error_severity = Column(Integer, nullable=False)
    error_code = Column(String(8), nullable=False)
    error_message = Column(String(200), nullable=False)
    process_date = Column(String(10), nullable=False)
    process_time = Column(String(8), nullable=False)
    user_id = Column(String(8), nullable=False)
    additional_info = Column(String(500))

    __table_args__ = (
        Index("idx_errlog_date_severity", "process_date", error_severity.desc()),
    )


class ErrorHandling(BaseModel):
    """Pydantic model for online error handling.

    Mapped from COBOL copybook ERRHND.cpy:
        01  ERROR-HANDLING.
            05 ERR-PROGRAM        PIC X(8).          -> program
            05 ERR-PARAGRAPH      PIC X(30).         -> paragraph
            05 ERR-SQLCODE        PIC S9(9) COMP.    -> sqlcode
            05 ERR-CICS-RESP      PIC S9(8) COMP.    -> cics_resp
            05 ERR-CICS-RESP2     PIC S9(8) COMP.    -> cics_resp2
            05 ERR-SEVERITY       PIC X.             -> severity
                88 ERR-FATAL          VALUE 'F'.
                88 ERR-WARNING        VALUE 'W'.
                88 ERR-INFO           VALUE 'I'.
            05 ERR-MESSAGE        PIC X(80).         -> message
            05 ERR-ACTION         PIC X.             -> action
                88 ERR-RETURN         VALUE 'R'.
                88 ERR-CONTINUE       VALUE 'C'.
                88 ERR-ABEND          VALUE 'A'.
            05 ERR-TRACE.
                10 ERR-TRACE-ID   PIC X(16).         -> trace_id
                10 ERR-TIMESTAMP  PIC X(26).         -> timestamp
    """

    program: str = Field(
        ..., max_length=8, description="Program ID (COBOL: PIC X(8))"
    )
    paragraph: Optional[str] = Field(
        default=None,
        max_length=30,
        description="Paragraph name (COBOL: PIC X(30))",
    )
    sqlcode: int = Field(
        default=0, description="SQL return code (COBOL: PIC S9(9) COMP)"
    )
    cics_resp: int = Field(
        default=0,
        description="CICS response code (COBOL: PIC S9(8) COMP)",
    )
    cics_resp2: int = Field(
        default=0,
        description="CICS response 2 code (COBOL: PIC S9(8) COMP)",
    )
    severity: ErrorSeverity = Field(
        ..., description="F=Fatal, W=Warning, I=Info"
    )
    message: str = Field(
        ..., max_length=80, description="Error message (COBOL: PIC X(80))"
    )
    action: ErrorAction = Field(
        ..., description="R=Return, C=Continue, A=Abend"
    )
    trace_id: Optional[str] = Field(
        default=None,
        max_length=16,
        description="Trace ID (COBOL: PIC X(16))",
    )
    timestamp: Optional[str] = Field(
        default=None,
        max_length=26,
        description="Error timestamp (COBOL: PIC X(26))",
    )

    @property
    def is_fatal(self) -> bool:
        return self.severity == ErrorSeverity.FATAL

    @property
    def should_abend(self) -> bool:
        return self.action == ErrorAction.ABEND


class ErrorMessage(BaseModel):
    """Pydantic model for standard error message structure.

    Mapped from COBOL copybook ERRHAND.cpy:
        01  ERR-MESSAGE.
            05  ERR-TIMESTAMP.
                10  ERR-DATE      PIC X(10).         -> error_date
                10  ERR-TIME      PIC X(8).          -> error_time
            05  ERR-PROGRAM       PIC X(8).          -> program
            05  ERR-CATEGORY      PIC X(2).          -> category
            05  ERR-CODE          PIC X(4).          -> code
            05  ERR-SEVERITY      PIC S9(4) COMP.    -> severity_code
            05  ERR-TEXT           PIC X(80).         -> text
            05  ERR-DETAILS       PIC X(256).        -> details
    """

    error_date: Optional[str] = Field(
        default=None, max_length=10, description="Error date"
    )
    error_time: Optional[str] = Field(
        default=None, max_length=8, description="Error time"
    )
    program: str = Field(
        ..., max_length=8, description="Program ID (COBOL: PIC X(8))"
    )
    category: ErrorCategory = Field(
        ..., description="VS=VSAM, VL=Validation, PR=Processing, SY=System"
    )
    code: str = Field(
        ..., max_length=4, description="Error code (COBOL: PIC X(4))"
    )
    severity_code: int = Field(
        ...,
        description="Severity as return code (COBOL: PIC S9(4) COMP). 0=Success, 4=Warning, 8=Error, 12=Severe, 16=Terminal",
    )
    text: str = Field(
        ..., max_length=80, description="Error text (COBOL: PIC X(80))"
    )
    details: Optional[str] = Field(
        default=None,
        max_length=256,
        description="Error details (COBOL: PIC X(256))",
    )

    @classmethod
    def create(
        cls,
        program: str,
        category: ErrorCategory,
        code: str,
        severity_code: int,
        text: str,
        details: Optional[str] = None,
    ) -> "ErrorMessage":
        now = datetime.now()
        return cls(
            error_date=now.strftime("%Y-%m-%d"),
            error_time=now.strftime("%H:%M:%S"),
            program=program,
            category=category,
            code=code,
            severity_code=severity_code,
            text=text,
            details=details,
        )
