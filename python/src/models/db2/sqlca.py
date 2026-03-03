"""
Pydantic v2 models for COBOL SQLCA copybook (SQL Communication Area).

Source: src/copybook/db2/SQLCA.cpy
"""

from pydantic import BaseModel, Field


class SqlStatusCodes(BaseModel):
    """
    SQL Status Codes -- maps to COBOL 01-level SQL-STATUS-CODES.

    Source: src/copybook/db2/SQLCA.cpy
    """

    model_config = {"from_attributes": True}

    sql_success: str = Field(
        default="00000",
        max_length=5,
        description="Success. COBOL: SQL-SUCCESS PIC X(5) VALUE '00000'.",
    )
    sql_not_found: str = Field(
        default="02000",
        max_length=5,
        description="Not found. COBOL: SQL-NOT-FOUND PIC X(5) VALUE '02000'.",
    )
    sql_dup_key: str = Field(
        default="23505",
        max_length=5,
        description="Duplicate key. COBOL: SQL-DUP-KEY PIC X(5) VALUE '23505'.",
    )
    sql_deadlock: str = Field(
        default="40001",
        max_length=5,
        description="Deadlock. COBOL: SQL-DEADLOCK PIC X(5) VALUE '40001'.",
    )
    sql_timeout: str = Field(
        default="40003",
        max_length=5,
        description="Timeout. COBOL: SQL-TIMEOUT PIC X(5) VALUE '40003'.",
    )
    sql_connection_error: str = Field(
        default="08001",
        max_length=5,
        description="Connection error. COBOL: SQL-CONNECTION-ERROR PIC X(5) VALUE '08001'.",
    )
    sql_db_error: str = Field(
        default="58004",
        max_length=5,
        description="Database error. COBOL: SQL-DB-ERROR PIC X(5) VALUE '58004'.",
    )
