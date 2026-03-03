"""DB2 Recovery module - replaces DB2RECV.cbl.

Provides database connection recovery with retry logic using tenacity,
replacing the COBOL retry loop with max retries and wait intervals.

COBOL DB2RECV flow:
- P100-INITIALIZE: Set max retries (3), wait interval (2 sec)
- P200-RECOVER: Retry loop with PERFORM UNTIL RECOVERED or MAX-RETRIES
- P300-RECONNECT: Attempt DB2 reconnection
- P400-RETRY-TRANSACTION: Re-execute failed transaction
"""

import logging
from typing import Any, Callable, TypeVar

from tenacity import (
    RetryError,
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_fixed,
)

logger = logging.getLogger("portfolio.online.db2_recovery")

T = TypeVar("T")

# Constants matching COBOL DB2RECV
MAX_RETRIES = 3  # WS-MAX-RETRIES from DB2RECV.cbl
WAIT_INTERVAL = 2  # WS-WAIT-INTERVAL (seconds) from DB2RECV.cbl


def with_db_retry(
    max_retries: int = MAX_RETRIES,
    wait_seconds: int = WAIT_INTERVAL,
) -> Callable[..., Any]:
    """Decorator for database operations with retry logic.

    Replaces DB2RECV P200-RECOVER retry loop:
    COBOL: PERFORM P300-RECONNECT UNTIL WS-RECOVERED = 'Y'
           OR WS-RETRY-COUNT >= WS-MAX-RETRIES

    Usage:
        @with_db_retry()
        def my_db_operation(session):
            ...
    """
    return retry(
        stop=stop_after_attempt(max_retries),
        wait=wait_fixed(wait_seconds),
        retry=retry_if_exception_type(Exception),
        reraise=True,
        before_sleep=lambda retry_state: logger.warning(
            "DB retry attempt %d/%d after error: %s",
            retry_state.attempt_number,
            max_retries,
            retry_state.outcome.exception() if retry_state.outcome else "unknown",
        ),
    )


class DB2RecoveryManager:
    """DB2 recovery manager replacing DB2RECV.cbl.

    Provides:
    - Connection recovery with retry logic
    - Transaction retry capability
    - Recovery status tracking

    Uses tenacity.retry decorators instead of COBOL PERFORM loops.
    """

    def __init__(
        self,
        max_retries: int = MAX_RETRIES,
        wait_interval: int = WAIT_INTERVAL,
    ) -> None:
        self.max_retries = max_retries
        self.wait_interval = wait_interval
        self.recovery_count = 0
        self.failed_recoveries = 0
        self.recovery_log: list[dict[str, Any]] = []

    def recover_connection(self, connect_func: Callable[[], Any]) -> bool:
        """Attempt to recover database connection - replaces P300-RECONNECT.

        COBOL: CALL 'DB2CONN' USING LS-CONN-REQUEST
        """
        @retry(
            stop=stop_after_attempt(self.max_retries),
            wait=wait_fixed(self.wait_interval),
            retry=retry_if_exception_type(Exception),
            reraise=True,
        )
        def _attempt_reconnect() -> Any:
            return connect_func()

        try:
            _attempt_reconnect()
            self.recovery_count += 1
            self._log_recovery("CONNECTION", True, "Connection recovered")
            return True
        except RetryError as exc:
            self.failed_recoveries += 1
            self._log_recovery("CONNECTION", False, f"Recovery failed after {self.max_retries} attempts: {exc}")
            return False

    def retry_transaction(
        self,
        transaction_func: Callable[..., T],
        *args: Any,
        **kwargs: Any,
    ) -> T | None:
        """Retry a failed transaction - replaces P400-RETRY-TRANSACTION.

        COBOL: Re-executes the failed SQL operation with retry logic.
        """
        @retry(
            stop=stop_after_attempt(self.max_retries),
            wait=wait_fixed(self.wait_interval),
            retry=retry_if_exception_type(Exception),
            reraise=True,
        )
        def _attempt_transaction() -> T:
            return transaction_func(*args, **kwargs)

        try:
            result = _attempt_transaction()
            self.recovery_count += 1
            self._log_recovery("TRANSACTION", True, "Transaction retry successful")
            return result
        except RetryError as exc:
            self.failed_recoveries += 1
            self._log_recovery(
                "TRANSACTION", False,
                f"Transaction retry failed after {self.max_retries} attempts: {exc}",
            )
            return None

    def _log_recovery(self, recovery_type: str, success: bool, message: str) -> None:
        """Log a recovery attempt."""
        import datetime
        entry = {
            "timestamp": datetime.datetime.now().isoformat(),
            "type": recovery_type,
            "success": success,
            "message": message,
        }
        self.recovery_log.append(entry)
        if success:
            logger.info("DB2RECV: %s - %s", recovery_type, message)
        else:
            logger.error("DB2RECV: %s - %s", recovery_type, message)

    def get_stats(self) -> dict[str, Any]:
        """Get recovery statistics."""
        return {
            "total_recoveries": self.recovery_count,
            "failed_recoveries": self.failed_recoveries,
            "max_retries": self.max_retries,
            "wait_interval": self.wait_interval,
            "recent_log": self.recovery_log[-10:],
        }
