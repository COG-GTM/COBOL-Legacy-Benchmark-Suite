"""Standard Return Code Handler - migrated from RTNCDE00.cbl.

Manages standardized return codes across the system, provides return code
analysis and reporting, integrates with error handling framework.
"""

import logging
from datetime import datetime

from portfolio_management.models.return_codes import ReturnCodeRequest, ReturnCodeRequestType
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "RTNCDE00"


class ReturnCodeHandler:
    def __init__(self):
        self._codes: dict[str, ReturnCodeRequest] = {}
        self._audit_log: list[dict] = []

    def process_request(self, request: ReturnCodeRequest) -> int:
        if request.request_type == ReturnCodeRequestType.INIT:
            return self._initialize(request)
        elif request.request_type == ReturnCodeRequestType.SET:
            return self._set_code(request)
        elif request.request_type == ReturnCodeRequestType.GET:
            return self._get_code(request)
        elif request.request_type == ReturnCodeRequestType.LOG:
            return self._log_code(request)
        elif request.request_type == ReturnCodeRequestType.ANAL:
            return self._analyze_codes(request)
        else:
            logger.error("Invalid request type: %s", request.request_type)
            return ReturnCode.ERROR

    def _initialize(self, request: ReturnCodeRequest) -> int:
        request.return_code = 0
        request.highest_code = 0
        request.status = "I"
        request.message = ""
        self._codes[request.program_id] = ReturnCodeRequest(
            program_id=request.program_id,
            return_code=0,
            highest_code=0,
            status="I",
        )
        logger.info("Return codes initialized for %s", request.program_id)
        return ReturnCode.SUCCESS

    def _set_code(self, request: ReturnCodeRequest) -> int:
        stored = self._codes.get(request.program_id)
        if stored is None:
            self._codes[request.program_id] = ReturnCodeRequest(
                program_id=request.program_id,
            )
            stored = self._codes[request.program_id]

        stored.return_code = request.return_code
        if request.return_code > stored.highest_code:
            stored.highest_code = request.return_code
        stored.message = request.message

        if request.return_code == 0:
            stored.status = "S"
        elif request.return_code <= 4:
            stored.status = "W"
        elif request.return_code <= 8:
            stored.status = "E"
        else:
            stored.status = "F"

        self._audit_log.append({
            "timestamp": datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            "program_id": request.program_id,
            "return_code": request.return_code,
            "status": stored.status,
            "message": request.message,
        })

        return ReturnCode.SUCCESS

    def _get_code(self, request: ReturnCodeRequest) -> int:
        stored = self._codes.get(request.program_id)
        if stored is None:
            request.return_code = 0
            request.highest_code = 0
            request.status = "N"
            return ReturnCode.WARNING

        request.return_code = stored.return_code
        request.highest_code = stored.highest_code
        request.status = stored.status
        request.message = stored.message
        return ReturnCode.SUCCESS

    def _log_code(self, request: ReturnCodeRequest) -> int:
        self._set_code(request)
        logger.info(
            "RC Log: %s RC=%d Highest=%d Status=%s - %s",
            request.program_id,
            request.return_code,
            request.highest_code,
            request.status,
            request.message,
        )
        return ReturnCode.SUCCESS

    def _analyze_codes(self, request: ReturnCodeRequest) -> int:
        if request.program_id:
            entries = [e for e in self._audit_log if e["program_id"] == request.program_id]
        else:
            entries = self._audit_log

        if not entries:
            request.message = "No return code entries found"
            return ReturnCode.WARNING

        total = len(entries)
        success = sum(1 for e in entries if e["return_code"] == 0)
        warnings = sum(1 for e in entries if 0 < e["return_code"] <= 4)
        errors = sum(1 for e in entries if 4 < e["return_code"] <= 8)
        severe = sum(1 for e in entries if e["return_code"] > 8)

        request.message = (
            f"Total: {total}, Success: {success}, "
            f"Warnings: {warnings}, Errors: {errors}, Severe: {severe}"
        )
        return ReturnCode.SUCCESS

    def get_audit_log(self) -> list[dict]:
        return list(self._audit_log)
