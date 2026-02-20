"""Process Recovery Handler - migrated from RCVPRC00.cbl.

Handles recovery for failed processes with restart, bypass, or terminate
actions based on process restartability.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "RCVPRC00"


class RecoveryAction:
    RESTART = "R"
    BYPASS = "B"
    TERMINATE = "T"
    MANUAL = "M"


class ProcessRecoveryHandler:
    def __init__(self):
        self._recovery_log: list[dict] = []
        self._max_retries = 3

    def evaluate_failure(
        self,
        process_id: str,
        return_code: int,
        error_desc: str,
        restartable: bool,
        restart_count: int,
    ) -> str:
        if return_code >= 16:
            logger.error(
                "Critical failure for %s (RC=%d): %s - Manual intervention required",
                process_id,
                return_code,
                error_desc,
            )
            self._log_recovery(process_id, RecoveryAction.MANUAL, return_code, error_desc)
            return RecoveryAction.MANUAL

        if not restartable:
            logger.warning(
                "Process %s is not restartable (RC=%d): %s - Bypassing",
                process_id,
                return_code,
                error_desc,
            )
            self._log_recovery(process_id, RecoveryAction.BYPASS, return_code, error_desc)
            return RecoveryAction.BYPASS

        if restart_count >= self._max_retries:
            logger.error(
                "Max retries exceeded for %s (%d >= %d) - Terminating",
                process_id,
                restart_count,
                self._max_retries,
            )
            self._log_recovery(process_id, RecoveryAction.TERMINATE, return_code, error_desc)
            return RecoveryAction.TERMINATE

        logger.info(
            "Process %s will be restarted (attempt %d of %d)",
            process_id,
            restart_count + 1,
            self._max_retries,
        )
        self._log_recovery(process_id, RecoveryAction.RESTART, return_code, error_desc)
        return RecoveryAction.RESTART

    def perform_recovery(
        self,
        process_id: str,
        action: str,
        recovery_program: Optional[str] = None,
        recovery_parm: Optional[str] = None,
    ) -> int:
        if action == RecoveryAction.RESTART:
            return self._restart_process(process_id)
        elif action == RecoveryAction.BYPASS:
            return self._bypass_process(process_id)
        elif action == RecoveryAction.TERMINATE:
            return self._terminate_process(process_id)
        elif action == RecoveryAction.MANUAL:
            logger.info("Manual intervention required for %s", process_id)
            return ReturnCode.SEVERE
        else:
            logger.error("Unknown recovery action: %s", action)
            return ReturnCode.ERROR

    def _restart_process(self, process_id: str) -> int:
        logger.info("Restarting process %s", process_id)
        return ReturnCode.SUCCESS

    def _bypass_process(self, process_id: str) -> int:
        logger.warning("Bypassing process %s", process_id)
        return ReturnCode.WARNING

    def _terminate_process(self, process_id: str) -> int:
        logger.error("Terminating process %s", process_id)
        return ReturnCode.ERROR

    def _log_recovery(
        self, process_id: str, action: str, return_code: int, error_desc: str
    ) -> None:
        self._recovery_log.append(
            {
                "timestamp": datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
                "process_id": process_id,
                "action": action,
                "return_code": return_code,
                "error_desc": error_desc,
            }
        )

    def get_recovery_log(self) -> list[dict]:
        return list(self._recovery_log)
