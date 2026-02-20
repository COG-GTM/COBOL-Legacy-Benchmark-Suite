"""Online system models - migrated from INQCOM.cpy, ERRHND.cpy, DB2REQ.cpy."""

from dataclasses import dataclass
from enum import Enum


class InquiryFunction(str, Enum):
    MENU = "MENU"
    PORTFOLIO = "INQP"
    HISTORY = "INQH"
    EXIT = "EXIT"


@dataclass
class InquiryCommArea:
    function: str = InquiryFunction.MENU
    account_no: str = ""
    response_code: int = 0
    error_msg: str = ""


class ErrorSeverity(str, Enum):
    FATAL = "F"
    WARNING = "W"
    INFO = "I"


class ErrorAction(str, Enum):
    RETURN = "R"
    CONTINUE = "C"
    ABEND = "A"


@dataclass
class OnlineErrorHandling:
    program: str = ""
    paragraph: str = ""
    sqlcode: int = 0
    cics_resp: int = 0
    cics_resp2: int = 0
    severity: str = ErrorSeverity.INFO
    message: str = ""
    action: str = ErrorAction.CONTINUE
    trace_id: str = ""
    timestamp: str = ""


class DB2RequestType(str, Enum):
    CONNECT = "C"
    DISCONNECT = "D"
    STATUS = "S"


@dataclass
class DB2RequestArea:
    request_type: str = DB2RequestType.CONNECT
    response_code: int = 0
    connection_token: str = ""
    sqlcode: int = 0
    error_msg: str = ""
