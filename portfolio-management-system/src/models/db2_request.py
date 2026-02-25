"""
DB2 Request Data Model

Migrated from COBOL copybook: src/copybook/online/DB2REQ.cpy

Original COBOL structure:
- DB2-REQUEST-AREA: Communication area for DB2 operations
  - DB2-REQUEST-TYPE: Request type (C=Connect, D=Disconnect, S=Status)
  - DB2-RESPONSE-CODE: Response code from operation
  - DB2-CONNECTION-TOKEN: Connection token for session tracking
  - DB2-ERROR-INFO: Error information (SQLCODE and message)

This copybook is used for DB2 connection management in CICS programs.
In the Python implementation, this maps to database session management.
"""

from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator


class DB2RequestType(str, Enum):
    """
    DB2 request type codes.
    
    Migrated from COBOL 88-level conditions:
    - DB2-CONNECT    VALUE 'C'
    - DB2-DISCONNECT VALUE 'D'
    - DB2-STATUS     VALUE 'S'
    """
    CONNECT = "C"
    DISCONNECT = "D"
    STATUS = "S"


class DB2ErrorInfo(BaseModel):
    """
    DB2 error information structure.
    
    Migrated from COBOL structure:
    - 10 DB2-SQLCODE   PIC S9(9) COMP
    - 10 DB2-ERROR-MSG PIC X(80)
    """
    
    sqlcode: int = Field(
        0,
        description="SQL return code (PIC S9(9) COMP)"
    )
    error_msg: str = Field(
        "",
        max_length=80,
        description="Error message (PIC X(80))"
    )

    @property
    def is_success(self) -> bool:
        """Check if SQLCODE indicates success (0 or 100)."""
        return self.sqlcode == 0 or self.sqlcode == 100

    @property
    def is_warning(self) -> bool:
        """Check if SQLCODE indicates a warning (positive, not 100)."""
        return self.sqlcode > 0 and self.sqlcode != 100

    @property
    def is_error(self) -> bool:
        """Check if SQLCODE indicates an error (negative)."""
        return self.sqlcode < 0


class DB2Request(BaseModel):
    """
    Pydantic model for DB2 request validation.
    
    Preserves all field definitions from DB2REQ.cpy with Python type mappings.
    This model represents database connection management requests.
    """
    
    request_type: DB2RequestType = Field(
        ...,
        description="Request type: C=Connect, D=Disconnect, S=Status"
    )
    connection_token: Optional[str] = Field(
        None,
        max_length=16,
        description="Connection token for session tracking (PIC X(16))"
    )

    @field_validator("connection_token")
    @classmethod
    def strip_token(cls, v: Optional[str]) -> Optional[str]:
        """Strip whitespace from connection token."""
        if v:
            return v.strip()
        return v

    class Config:
        """Pydantic configuration."""
        use_enum_values = True


class DB2Response(BaseModel):
    """
    Pydantic model for DB2 response.
    
    Represents the response from a DB2 operation.
    """
    
    request_type: DB2RequestType = Field(
        ...,
        description="Request type that was executed"
    )
    response_code: int = Field(
        0,
        description="Response code (PIC S9(8) COMP): 0=Success, negative=Error"
    )
    connection_token: Optional[str] = Field(
        None,
        max_length=16,
        description="Connection token for session tracking"
    )
    error_info: Optional[DB2ErrorInfo] = Field(
        None,
        description="Error information if response_code != 0"
    )

    @property
    def is_success(self) -> bool:
        """Check if the response indicates success."""
        return self.response_code == 0

    @property
    def is_error(self) -> bool:
        """Check if the response indicates an error."""
        return self.response_code != 0

    class Config:
        """Pydantic configuration."""
        use_enum_values = True


class DB2RequestArea(BaseModel):
    """
    Complete DB2 Request Area model.
    
    This model represents the full DB2-REQUEST-AREA structure used for
    database connection management in CICS programs.
    
    Original COBOL structure:
    - 05 DB2-REQUEST-TYPE     PIC X
    - 05 DB2-RESPONSE-CODE    PIC S9(8) COMP
    - 05 DB2-CONNECTION-TOKEN PIC X(16)
    - 05 DB2-ERROR-INFO
       - 10 DB2-SQLCODE       PIC S9(9) COMP
       - 10 DB2-ERROR-MSG     PIC X(80)
    """
    
    db2_request_type: str = Field(
        ...,
        max_length=1,
        description="Request type (C/D/S)"
    )
    db2_response_code: int = Field(
        0,
        description="Response code from operation"
    )
    db2_connection_token: str = Field(
        "",
        max_length=16,
        description="Connection token for session tracking"
    )
    db2_sqlcode: int = Field(
        0,
        description="SQL return code"
    )
    db2_error_msg: str = Field(
        "",
        max_length=80,
        description="Error message"
    )

    @field_validator("db2_request_type")
    @classmethod
    def validate_request_type(cls, v: str) -> str:
        """Validate request type is one of C, D, S."""
        v = v.strip().upper()
        if v not in ("C", "D", "S"):
            raise ValueError("Request type must be C, D, or S")
        return v

    def to_request(self) -> DB2Request:
        """Convert DB2RequestArea to DB2Request."""
        return DB2Request(
            request_type=DB2RequestType(self.db2_request_type),
            connection_token=self.db2_connection_token if self.db2_connection_token else None,
        )

    @classmethod
    def from_response(cls, response: DB2Response) -> "DB2RequestArea":
        """Create DB2RequestArea from DB2Response."""
        return cls(
            db2_request_type=response.request_type if isinstance(response.request_type, str) else response.request_type.value,
            db2_response_code=response.response_code,
            db2_connection_token=response.connection_token or "",
            db2_sqlcode=response.error_info.sqlcode if response.error_info else 0,
            db2_error_msg=response.error_info.error_msg if response.error_info else "",
        )

    class Config:
        """Pydantic configuration."""
        json_encoders = {
            DB2RequestType: lambda v: v.value,
        }


# Common SQLCODE values for reference
class SQLCode:
    """
    Common DB2 SQLCODE values.
    
    These are standard DB2 return codes that are checked in the COBOL programs.
    """
    SUCCESS = 0
    NOT_FOUND = 100
    DUPLICATE_KEY = -803
    DEADLOCK = -911
    TIMEOUT = -913
    CONNECTION_ERROR = -30081
