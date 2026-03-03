"""
Pydantic v2 models for COBOL DBPROC copybook (DB2 Standard Procedures).

Source: src/copybook/db2/DBPROC.cpy

Note: The DBPROC copybook contains both data definitions and procedural code
(CONNECT-TO-DB2, DISCONNECT-FROM-DB2, etc.). Only the data definitions are
modeled here; the procedural logic would be implemented in service classes.
"""

from pydantic import BaseModel, Field


class Db2ErrorMessage(BaseModel):
    """DB2 error message structure from DB2-ERROR-MESSAGE (level 05)."""

    model_config = {"from_attributes": True}

    db2_sqlcode_txt: str = Field(
        max_length=6,
        description="SQL code text. COBOL: DB2-SQLCODE-TXT PIC X(6).",
    )
    db2_state: str = Field(
        max_length=5,
        description="SQL state. COBOL: DB2-STATE PIC X(5).",
    )
    db2_error_text: str = Field(
        max_length=70,
        description="Error text. COBOL: DB2-ERROR-TEXT PIC X(70).",
    )


class Db2ErrorHandling(BaseModel):
    """
    DB2 Error Handling -- maps to COBOL 01-level DB2-ERROR-HANDLING.

    Source: src/copybook/db2/DBPROC.cpy
    """

    model_config = {"from_attributes": True}

    db2_error_message: Db2ErrorMessage = Field(
        description="DB2 error message (DB2-ERROR-MESSAGE).",
    )
    db2_save_status: str = Field(
        max_length=5,
        description="Saved SQL state. COBOL: DB2-SAVE-STATUS PIC X(5).",
    )
    db2_retry_count: int = Field(
        default=0,
        description="Current retry count. COBOL: DB2-RETRY-COUNT PIC S9(4) COMP VALUE 0.",
    )
    db2_max_retries: int = Field(
        default=3,
        description="Maximum retries. COBOL: DB2-MAX-RETRIES PIC S9(4) COMP VALUE 3.",
    )
    db2_retry_wait: int = Field(
        default=100,
        description="Retry wait time (ms). COBOL: DB2-RETRY-WAIT PIC S9(4) COMP VALUE 100.",
    )
