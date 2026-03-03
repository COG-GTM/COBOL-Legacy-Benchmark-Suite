"""
Pydantic v2 models for COBOL DB2REQ copybook (DB2 Request Area).

Source: src/copybook/online/DB2REQ.cpy
"""

from pydantic import BaseModel, Field, field_validator


class Db2ErrorInfo(BaseModel):
    """DB2 error info from DB2-ERROR-INFO (level 05)."""

    model_config = {"from_attributes": True}

    db2_sqlcode: int = Field(
        description="SQL return code. COBOL: DB2-SQLCODE PIC S9(9) COMP.",
    )
    db2_error_msg: str = Field(
        default="",
        max_length=80,
        description="Error message. COBOL: DB2-ERROR-MSG PIC X(80).",
    )


class Db2RequestArea(BaseModel):
    """
    DB2 Request Area -- maps to COBOL 01-level DB2-REQUEST-AREA.

    Source: src/copybook/online/DB2REQ.cpy
    """

    model_config = {"from_attributes": True}

    db2_request_type: str = Field(
        max_length=1,
        description=(
            "Request type: C=Connect, D=Disconnect, S=Status. "
            "COBOL: DB2-REQUEST-TYPE PIC X. "
            "88-level values: C, D, S."
        ),
    )
    db2_response_code: int = Field(
        description="Response code. COBOL: DB2-RESPONSE-CODE PIC S9(8) COMP.",
    )
    db2_connection_token: str = Field(
        max_length=16,
        description="Connection token. COBOL: DB2-CONNECTION-TOKEN PIC X(16).",
    )
    db2_error_info: Db2ErrorInfo = Field(
        description="Error information (DB2-ERROR-INFO).",
    )

    @field_validator("db2_request_type")
    @classmethod
    def validate_request_type(cls, v: str) -> str:
        valid = {"C", "D", "S"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"db2_request_type must be one of {valid}")
        return v
