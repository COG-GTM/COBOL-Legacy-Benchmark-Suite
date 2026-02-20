"""Portfolio Online Inquiry Main Handler - migrated from INQONLN.cbl.

Handles portfolio inquiry transactions, manages screen interactions,
processes portfolio lookups, and interfaces with DB2 for history.
Replaces CICS transaction processing with a request/response pattern.
"""

import logging
from dataclasses import dataclass
from typing import Optional

from portfolio_management.models.online import InquiryCommArea, InquiryFunction
from portfolio_management.models.common import ReturnCode
from portfolio_management.online.security_manager import SecurityManager
from portfolio_management.online.portfolio_inquiry import PortfolioInquiryHandler
from portfolio_management.online.history_inquiry import HistoryInquiryHandler

logger = logging.getLogger(__name__)

PROGRAM_ID = "INQONLN"


@dataclass
class InquiryRequest:
    function: str = ""
    account_no: str = ""
    user_id: str = ""


@dataclass
class InquiryResponse:
    return_code: int = 0
    message: str = ""
    data: Optional[dict] = None


class InquiryMainHandler:
    def __init__(
        self,
        security_mgr: Optional[SecurityManager] = None,
        portfolio_handler: Optional[PortfolioInquiryHandler] = None,
        history_handler: Optional[HistoryInquiryHandler] = None,
    ):
        self._security_mgr = security_mgr or SecurityManager()
        self._portfolio_handler = portfolio_handler or PortfolioInquiryHandler()
        self._history_handler = history_handler or HistoryInquiryHandler()
        self._commarea = InquiryCommArea()

    def process_request(self, request: InquiryRequest) -> InquiryResponse:
        if not self._validate_request(request):
            return InquiryResponse(
                return_code=ReturnCode.ERROR,
                message="Invalid request",
            )

        rc = self._security_mgr.validate_user(request.user_id)
        if rc != ReturnCode.SUCCESS:
            return InquiryResponse(
                return_code=ReturnCode.ERROR,
                message="Security validation failed",
            )

        self._security_mgr.log_access(
            request.user_id, PROGRAM_ID, request.function
        )

        if request.function == InquiryFunction.PORTFOLIO:
            return self._handle_portfolio_inquiry(request)
        elif request.function == InquiryFunction.HISTORY:
            return self._handle_history_inquiry(request)
        elif request.function == InquiryFunction.MENU:
            return InquiryResponse(
                return_code=ReturnCode.SUCCESS,
                message="Menu displayed",
                data={"screen": "MENU"},
            )
        elif request.function == InquiryFunction.EXIT:
            return InquiryResponse(
                return_code=ReturnCode.SUCCESS,
                message="Session ended",
            )
        else:
            return InquiryResponse(
                return_code=ReturnCode.ERROR,
                message=f"Unknown function: {request.function}",
            )

    def _validate_request(self, request: InquiryRequest) -> bool:
        if not request.function:
            return False
        if request.function in (InquiryFunction.PORTFOLIO, InquiryFunction.HISTORY):
            if not request.account_no:
                return False
        return True

    def _handle_portfolio_inquiry(self, request: InquiryRequest) -> InquiryResponse:
        result = self._portfolio_handler.get_position(request.account_no)
        if result is None:
            return InquiryResponse(
                return_code=ReturnCode.WARNING,
                message=f"No position found for account {request.account_no}",
            )

        return InquiryResponse(
            return_code=ReturnCode.SUCCESS,
            message="Position retrieved",
            data=result,
        )

    def _handle_history_inquiry(self, request: InquiryRequest) -> InquiryResponse:
        records = self._history_handler.get_history(request.account_no)
        if not records:
            return InquiryResponse(
                return_code=ReturnCode.WARNING,
                message=f"No history found for account {request.account_no}",
            )

        return InquiryResponse(
            return_code=ReturnCode.SUCCESS,
            message=f"Retrieved {len(records)} history records",
            data={"records": records},
        )
