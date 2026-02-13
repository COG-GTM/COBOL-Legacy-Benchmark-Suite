"""DB2 Request Model - migrated from COBOL copybook DB2REQ.cpy

Source: src/copybook/online/DB2REQ.cpy
COBOL Record: DB2-REQUEST-AREA

COBOL Data Type Mapping:
    PIC X           -> str (single character)
    PIC X(16)       -> str (fixed-length character, 16 bytes)
    PIC S9(8) COMP  -> int (binary fullword, signed)
    PIC S9(9) COMP  -> int (binary fullword, signed)
    PIC X(80)       -> str (fixed-length character, 80 bytes)
    88-level conditions -> Enum or validated string constants

In the Python migration, DB2 connection management is handled by
SQLAlchemy's connection pooling. This model is preserved for
compatibility and to support connection health monitoring.
"""
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class DB2RequestType(str, Enum):
    CONNECT = "C"
    DISCONNECT = "D"
    STATUS = "S"


class DB2Request(BaseModel):
    """Pydantic model for DB2 request area.

    Mapped from COBOL copybook DB2REQ.cpy:
        01  DB2-REQUEST-AREA.
            05 DB2-REQUEST-TYPE      PIC X.          -> request_type
                88 DB2-CONNECT           VALUE 'C'.
                88 DB2-DISCONNECT        VALUE 'D'.
                88 DB2-STATUS            VALUE 'S'.
            05 DB2-RESPONSE-CODE     PIC S9(8) COMP. -> response_code
            05 DB2-CONNECTION-TOKEN  PIC X(16).      -> connection_token
            05 DB2-ERROR-INFO.
                10 DB2-SQLCODE       PIC S9(9) COMP. -> sqlcode
                10 DB2-ERROR-MSG     PIC X(80).      -> error_message

    In the Python migration, direct DB2 connection management is replaced
    by SQLAlchemy's connection pooling and session management. This model
    is preserved to maintain the same monitoring and error reporting
    interface used by the online programs (DB2ONLN, DB2RECV).
    """

    request_type: DB2RequestType = Field(
        ..., description="C=Connect, D=Disconnect, S=Status"
    )
    response_code: int = Field(
        default=0,
        description="Response code (COBOL: PIC S9(8) COMP)",
    )
    connection_token: Optional[str] = Field(
        default=None,
        max_length=16,
        description="Connection token (COBOL: PIC X(16))",
    )
    sqlcode: int = Field(
        default=0,
        description="SQL return code (COBOL: PIC S9(9) COMP)",
    )
    error_message: Optional[str] = Field(
        default=None,
        max_length=80,
        description="Error message (COBOL: PIC X(80))",
    )

    @property
    def is_success(self) -> bool:
        return self.response_code == 0 and self.sqlcode == 0

    @property
    def is_connect_request(self) -> bool:
        return self.request_type == DB2RequestType.CONNECT

    @property
    def is_disconnect_request(self) -> bool:
        return self.request_type == DB2RequestType.DISCONNECT

    @property
    def is_status_request(self) -> bool:
        return self.request_type == DB2RequestType.STATUS
