"""DB2 Recovery Manager for Online Programs - migrated from DB2RECV.cbl.

Handles DB2 connection failures, implements retry logic, manages
transaction rollback, and provides recovery status tracking.
"""

import logging
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "DB2RECV"
MAX_RETRIES = 3
RETRY_INTERVAL_SECONDS = 2


@dataclass
class RecoveryStatus:
    recovery_type: str = ""
    attempts: int = 0
    success: bool = False
    last_error: str = ""
    timestamp: str = ""


class DB2RecoveryManager:
    def __init__(self):
        self._recovery_history: list[RecoveryStatus] = []

    def recover_connection(self, connection_factory: Optional[type] = None) -> int:
        status = RecoveryStatus(
            recovery_type="CONNECTION",
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
        )

        for attempt in range(1, MAX_RETRIES + 1):
            status.attempts = attempt
            try:
                if connection_factory is not None:
                    connection_factory()
                status.success = True
                logger.info("Connection recovered on attempt %d", attempt)
                self._recovery_history.append(status)
                return ReturnCode.SUCCESS
            except Exception as e:
                status.last_error = str(e)
                logger.warning("Connection recovery attempt %d failed: %s", attempt, e)
                if attempt < MAX_RETRIES:
                    time.sleep(RETRY_INTERVAL_SECONDS)

        logger.error("Connection recovery failed after %d attempts", MAX_RETRIES)
        self._recovery_history.append(status)
        return ReturnCode.ERROR

    def recover_transaction(self, rollback_func: Optional[type] = None) -> int:
        status = RecoveryStatus(
            recovery_type="TRANSACTION",
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            attempts=1,
        )

        try:
            if rollback_func is not None:
                rollback_func()
            status.success = True
            logger.info("Transaction rolled back successfully")
            self._recovery_history.append(status)
            return ReturnCode.SUCCESS
        except Exception as e:
            status.last_error = str(e)
            logger.error("Transaction recovery failed: %s", e)
            self._recovery_history.append(status)
            return ReturnCode.ERROR

    def recover_cursor(self, cursor_manager: Optional[object] = None, cursor_name: str = "") -> int:
        status = RecoveryStatus(
            recovery_type="CURSOR",
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            attempts=1,
        )

        try:
            if cursor_manager is not None and hasattr(cursor_manager, "close"):
                cursor_manager.close(cursor_name)
            status.success = True
            logger.info("Cursor %s recovered", cursor_name)
            self._recovery_history.append(status)
            return ReturnCode.SUCCESS
        except Exception as e:
            status.last_error = str(e)
            logger.error("Cursor recovery failed: %s", e)
            self._recovery_history.append(status)
            return ReturnCode.ERROR

    def get_recovery_history(self) -> list[RecoveryStatus]:
        return list(self._recovery_history)
