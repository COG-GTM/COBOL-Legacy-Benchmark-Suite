"""Online processing models translated from COBOL copybooks."""

from .db2_request import Db2ErrorInfo, Db2RequestArea
from .error_handler import ErrorTrace, OnlineErrorHandling
from .inquiry import InquiryCommunicationArea

__all__ = [
    # inquiry
    "InquiryCommunicationArea",
    # db2_request
    "Db2ErrorInfo",
    "Db2RequestArea",
    # error_handler
    "ErrorTrace",
    "OnlineErrorHandling",
]
