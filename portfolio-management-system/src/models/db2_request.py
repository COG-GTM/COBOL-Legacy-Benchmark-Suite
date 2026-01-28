"""
DB2 Request data models.
Migrated from COBOL copybook: src/copybook/online/DB2REQ.cpy

Original COBOL structure:
01  DB2-REQUEST-AREA.
    05 DB2-REQUEST-TYPE        PIC X.
    05 DB2-RESPONSE-CODE       PIC S9(8) COMP.
    05 DB2-CONNECTION-TOKEN    PIC X(16).
    05 DB2-ERROR-INFO.
       10 DB2-SQLCODE          PIC S9(9) COMP.
       10 DB2-ERROR-MSG        PIC X(80).
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class DB2RequestType(str, Enum):
    """
    DB2 request type codes.
    Migrated from COBOL: DB2-REQUEST-TYPE values.
    """
    CONNECT = "C"
    DISCONNECT = "D"
    STATUS = "S"
    COMMIT = "M"
    ROLLBACK = "R"
    SAVEPOINT = "P"


class DB2ResponseCode(int, Enum):
    """
    DB2 response codes.
    Migrated from COBOL: DB2-RESPONSE-CODE values.
    """
    SUCCESS = 0
    WARNING = 4
    NOT_FOUND = 100
    CONNECTION_ERROR = -30081
    DUPLICATE_KEY = -803
    DEADLOCK = -911
    TIMEOUT = -913
    CONSTRAINT_VIOLATION = -530
    INVALID_CURSOR = -501
    AUTHORIZATION_ERROR = -551


class SQLErrorCategory(str, Enum):
    """
    SQL error categories for diagnosis.
    Migrated from DB2ERR.cbl error categorization.
    """
    DEADLOCK = "DEADLOCK"
    TIMEOUT = "TIMEOUT"
    CONNECTION = "CONNECTION"
    DUPLICATE = "DUPLICATE"
    NOT_FOUND = "NOT_FOUND"
    CONSTRAINT = "CONSTRAINT"
    AUTHORIZATION = "AUTHORIZATION"
    UNKNOWN = "UNKNOWN"


class DB2ErrorInfo(BaseModel):
    """
    Pydantic model for DB2 error information (DB2-ERROR-INFO).
    """
    sqlcode: int = Field(..., description="SQL return code")
    error_msg: Optional[str] = Field(None, max_length=80, description="Error message")
    
    @property
    def category(self) -> SQLErrorCategory:
        """Categorize the SQL error."""
        if self.sqlcode == -911:
            return SQLErrorCategory.DEADLOCK
        elif self.sqlcode == -913:
            return SQLErrorCategory.TIMEOUT
        elif self.sqlcode == -30081:
            return SQLErrorCategory.CONNECTION
        elif self.sqlcode == -803:
            return SQLErrorCategory.DUPLICATE
        elif self.sqlcode == 100:
            return SQLErrorCategory.NOT_FOUND
        elif self.sqlcode in (-530, -531, -532):
            return SQLErrorCategory.CONSTRAINT
        elif self.sqlcode in (-551, -552):
            return SQLErrorCategory.AUTHORIZATION
        else:
            return SQLErrorCategory.UNKNOWN
    
    @property
    def is_retryable(self) -> bool:
        """Check if the error is retryable."""
        return self.category in (
            SQLErrorCategory.DEADLOCK,
            SQLErrorCategory.TIMEOUT,
            SQLErrorCategory.CONNECTION
        )


class DB2Request(BaseModel):
    """
    Pydantic model for DB2 request (DB2-REQUEST-AREA input).
    Used for database operations.
    """
    request_type: DB2RequestType = Field(..., description="Request type code")
    connection_token: Optional[str] = Field(None, max_length=16, description="Connection token")


class DB2Response(BaseModel):
    """
    Pydantic model for DB2 response (DB2-REQUEST-AREA output).
    Used for database operation responses.
    """
    request_type: DB2RequestType
    response_code: int
    connection_token: Optional[str] = None
    error_info: Optional[DB2ErrorInfo] = None
    
    @classmethod
    def success(cls, request_type: DB2RequestType, connection_token: str = None) -> "DB2Response":
        """Create a success response."""
        return cls(
            request_type=request_type,
            response_code=DB2ResponseCode.SUCCESS.value,
            connection_token=connection_token,
            error_info=None
        )
    
    @classmethod
    def error(
        cls,
        request_type: DB2RequestType,
        sqlcode: int,
        error_msg: str,
        connection_token: str = None
    ) -> "DB2Response":
        """Create an error response."""
        return cls(
            request_type=request_type,
            response_code=sqlcode,
            connection_token=connection_token,
            error_info=DB2ErrorInfo(sqlcode=sqlcode, error_msg=error_msg[:80] if error_msg else None)
        )
    
    @property
    def is_success(self) -> bool:
        """Check if the response indicates success."""
        return self.response_code == DB2ResponseCode.SUCCESS.value
    
    @property
    def is_not_found(self) -> bool:
        """Check if the response indicates not found."""
        return self.response_code == DB2ResponseCode.NOT_FOUND.value


class ConnectionPoolStatus(BaseModel):
    """
    Pydantic model for connection pool status.
    Replaces DB2ONLN connection pool monitoring.
    """
    total_connections: int = Field(..., ge=0, description="Total connections in pool")
    active_connections: int = Field(..., ge=0, description="Currently active connections")
    available_connections: int = Field(..., ge=0, description="Available connections")
    max_connections: int = Field(default=100, ge=1, description="Maximum pool size")
    
    @property
    def utilization_percent(self) -> float:
        """Calculate pool utilization percentage."""
        if self.max_connections == 0:
            return 0.0
        return (self.active_connections / self.max_connections) * 100


class RecoveryRequest(BaseModel):
    """
    Pydantic model for recovery request.
    Replaces DB2RECV RECOVERY-REQUEST-AREA.
    """
    recovery_type: str = Field(..., max_length=8, description="Recovery type")
    connection_token: Optional[str] = Field(None, max_length=16)
    transaction_id: Optional[str] = Field(None, max_length=16)
    retry_count: int = Field(default=0, ge=0, le=3)
    
    @property
    def can_retry(self) -> bool:
        """Check if retry is allowed."""
        return self.retry_count < 3


class RecoveryResponse(BaseModel):
    """
    Pydantic model for recovery response.
    """
    recovery_type: str
    success: bool
    retry_count: int
    error_msg: Optional[str] = None
    new_connection_token: Optional[str] = None


class CommitStatistics(BaseModel):
    """
    Pydantic model for commit statistics.
    Replaces DB2CMT commit tracking.
    """
    total_commits: int = Field(default=0, ge=0)
    total_rollbacks: int = Field(default=0, ge=0)
    total_savepoints: int = Field(default=0, ge=0)
    last_commit_timestamp: Optional[str] = None
    last_rollback_timestamp: Optional[str] = None
