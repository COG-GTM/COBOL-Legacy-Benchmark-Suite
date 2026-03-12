"""
Security and online inquiry data models translated from COBOL copybooks.

Source copybooks:
  - src/copybook/online/INQCOM.cpy   (Online Inquiry Communication Area)
  - src/copybook/online/DB2REQ.cpy   (DB2 Request Area)
  - src/copybook/online/ERRHND.cpy   (Online Error Handling)
"""

from __future__ import annotations

import datetime
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums from INQCOM.cpy level-88 values
# ---------------------------------------------------------------------------

class InquiryFunction(str, Enum):
    """INQCOM-FUNCTION level-88 values from INQCOM.cpy."""

    MENU = "MENU"
    PORTFOLIO = "INQP"
    HISTORY = "INQH"
    EXIT = "EXIT"


# ---------------------------------------------------------------------------
# Enums from DB2REQ.cpy level-88 values
# ---------------------------------------------------------------------------

class Db2RequestType(str, Enum):
    """DB2-REQUEST-TYPE level-88 values from DB2REQ.cpy."""

    CONNECT = "C"
    DISCONNECT = "D"
    STATUS = "S"


# ---------------------------------------------------------------------------
# Enums from online ERRHND.cpy level-88 values
# ---------------------------------------------------------------------------

class OnlineErrorSeverity(str, Enum):
    """ERR-SEVERITY level-88 values from online ERRHND.cpy."""

    FATAL = "F"
    WARNING = "W"
    INFO = "I"


class OnlineErrorAction(str, Enum):
    """ERR-ACTION level-88 values from online ERRHND.cpy."""

    RETURN = "R"
    CONTINUE = "C"
    ABEND = "A"


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class InquiryRequest(BaseModel):
    """Online inquiry communication area from INQCOM.cpy  01 INQCOM-AREA.

    Field sizes from PIC clauses:
      INQCOM-FUNCTION       PIC X(4)
      INQCOM-ACCOUNT-NO     PIC X(10)
      INQCOM-RESPONSE-CODE  PIC S9(8) COMP
      INQCOM-ERROR-MSG      PIC X(80)
    """

    function: InquiryFunction
    account_no: Annotated[str, Field(max_length=10)]
    response_code: int = 0
    error_message: Annotated[str, Field(max_length=80)] = ""


class Db2Request(BaseModel):
    """DB2 request area from DB2REQ.cpy  01 DB2-REQUEST-AREA.

    Field sizes from PIC clauses:
      DB2-REQUEST-TYPE       PIC X
      DB2-RESPONSE-CODE      PIC S9(8) COMP
      DB2-CONNECTION-TOKEN   PIC X(16)
      DB2-SQLCODE            PIC S9(9) COMP
      DB2-ERROR-MSG          PIC X(80)
    """

    request_type: Db2RequestType
    response_code: int = 0
    connection_token: Annotated[str, Field(max_length=16)] = ""
    sqlcode: int = 0
    error_message: Annotated[str, Field(max_length=80)] = ""


class OnlineErrorRecord(BaseModel):
    """Online error handling from ERRHND.cpy  01 ERROR-HANDLING.

    Field sizes from PIC clauses:
      ERR-PROGRAM       PIC X(8)
      ERR-PARAGRAPH     PIC X(30)
      ERR-SQLCODE       PIC S9(9) COMP
      ERR-CICS-RESP     PIC S9(8) COMP
      ERR-CICS-RESP2    PIC S9(8) COMP
      ERR-SEVERITY      PIC X
      ERR-MESSAGE       PIC X(80)
      ERR-ACTION        PIC X
      ERR-TRACE-ID      PIC X(16)
      ERR-TIMESTAMP     PIC X(26)
    """

    program: Annotated[str, Field(max_length=8)]
    paragraph: Annotated[str, Field(max_length=30)] = ""
    sqlcode: int = 0
    cics_resp: int = 0
    cics_resp2: int = 0
    severity: OnlineErrorSeverity = OnlineErrorSeverity.INFO
    message: Annotated[str, Field(max_length=80)] = ""
    action: OnlineErrorAction = OnlineErrorAction.RETURN
    trace_id: Annotated[str, Field(max_length=16)] = ""
    timestamp: Optional[datetime.datetime] = None

    @field_validator("timestamp", mode="before")
    @classmethod
    def _parse_cobol_timestamp(cls, value: object) -> object:
        """Accept COBOL PIC X(26) timestamp strings."""
        if isinstance(value, str) and len(value) == 26:
            try:
                return datetime.datetime.fromisoformat(value.strip())
            except ValueError:
                pass
        return value


class SecurityParameters(BaseModel):
    """Combined security parameters for the online system.

    Aggregates authentication and authorization data used by the
    SECMGR program.  Not directly from a single copybook but composed
    from INQCOM and ERRHND fields.
    """

    user_id: Annotated[str, Field(max_length=8)]
    terminal_id: Annotated[str, Field(max_length=8)] = ""
    program_id: Annotated[str, Field(max_length=8)] = ""
    function: InquiryFunction = InquiryFunction.MENU
    authenticated: bool = False
    authorization_level: int = 0


class UserData(BaseModel):
    """User session data for online CICS transactions.

    Captures the user context passed through DFHCOMMAREA
    during online inquiry sessions.
    """

    user_id: Annotated[str, Field(max_length=8)]
    terminal_id: Annotated[str, Field(max_length=8)] = ""
    session_start: Optional[datetime.datetime] = None
    last_activity: Optional[datetime.datetime] = None
    current_function: InquiryFunction = InquiryFunction.MENU
    account_no: Annotated[str, Field(max_length=10)] = ""
    error_message: Annotated[str, Field(max_length=80)] = ""
