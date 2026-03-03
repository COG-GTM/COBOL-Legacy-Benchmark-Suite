"""Retry and recovery logic using tenacity.

Mirrors COBOL DB2RECV program patterns:
  P100 - Connection recovery with retry loop
         (WS-MAX-RETRIES=3, WS-RETRY-INTERVAL=2)
  P200 - Transaction rollback recovery
  P300 - Cursor recovery via error handler

Provides configurable retry decorators for database operations
and external service calls.
"""

import logging
from dataclasses import dataclass, field
from typing import Any, Callable, Optional, Sequence, Type

from tenacity import (
    RetryCallState,
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
    wait_fixed,
)

logger = logging.getLogger("clbs.recovery")


@dataclass
class RetryConfig:
    """Configuration for retry behavior matching DB2RECV WS-RECOVERY-STATS.

    Attributes:
        max_retries: Maximum retry attempts (WS-MAX-RETRIES, default 3).
        wait_seconds: Base wait interval in seconds (WS-RETRY-INTERVAL, default 2).
        exponential_backoff: Use exponential backoff instead of fixed wait.
        backoff_multiplier: Multiplier for exponential backoff.
        backoff_max: Maximum wait time for exponential backoff in seconds.
        retry_exceptions: Tuple of exception types that trigger retry.
    """

    max_retries: int = 3
    wait_seconds: float = 2.0
    exponential_backoff: bool = False
    backoff_multiplier: float = 1.0
    backoff_max: float = 60.0
    retry_exceptions: Sequence[Type[Exception]] = field(
        default_factory=lambda: (Exception,)
    )


def _before_retry_log(retry_state: RetryCallState) -> None:
    """Log retry attempts, mirroring DB2RECV's retry tracking.

    Equivalent to DB2RECV incrementing WS-RETRY-COUNT and
    storing WS-LAST-ERROR before each retry.
    """
    attempt = retry_state.attempt_number
    fn_name = getattr(retry_state.fn, "__name__", "unknown")

    if retry_state.outcome is not None and retry_state.outcome.failed:
        exc = retry_state.outcome.exception()
        logger.warning(
            "Retry attempt %d for %s: %s",
            attempt,
            fn_name,
            str(exc),
            extra={
                "retry_attempt": attempt,
                "function": fn_name,
                "error": str(exc),
            },
        )


def _after_retry_log(retry_state: RetryCallState) -> None:
    """Log final retry outcome."""
    fn_name = getattr(retry_state.fn, "__name__", "unknown")

    if retry_state.outcome is not None and retry_state.outcome.failed:
        logger.error(
            "All retries exhausted for %s after %d attempts",
            fn_name,
            retry_state.attempt_number,
            extra={
                "function": fn_name,
                "total_attempts": retry_state.attempt_number,
            },
        )


def create_retry_decorator(config: Optional[RetryConfig] = None) -> Callable:
    """Create a tenacity retry decorator from a RetryConfig.

    Mirrors DB2RECV P100-RECOVER-CONNECTION retry loop:
    - PERFORM UNTIL WS-RETRY-COUNT >= WS-MAX-RETRIES
    - P120-WAIT-INTERVAL (CICS DELAY)
    - ADD 1 TO WS-RETRY-COUNT

    Args:
        config: Retry configuration. Defaults to DB2RECV values
            (3 retries, 2 second wait).

    Returns:
        A tenacity retry decorator.
    """
    if config is None:
        config = RetryConfig()

    if config.exponential_backoff:
        wait_strategy = wait_exponential(
            multiplier=config.backoff_multiplier,
            max=config.backoff_max,
        )
    else:
        wait_strategy = wait_fixed(config.wait_seconds)

    exception_types = tuple(config.retry_exceptions)

    return retry(
        stop=stop_after_attempt(config.max_retries),
        wait=wait_strategy,
        retry=retry_if_exception_type(exception_types),
        before=_before_retry_log,
        after=_after_retry_log,
        reraise=True,
    )


def retry_database_operation(
    max_retries: int = 3,
    wait_seconds: float = 2.0,
    exponential_backoff: bool = True,
    retry_on: Optional[Sequence[Type[Exception]]] = None,
) -> Callable:
    """Decorator for database operations with retry logic.

    Mirrors DB2RECV P100-RECOVER-CONNECTION pattern with
    configurable retry parameters.

    Args:
        max_retries: Maximum number of retries (WS-MAX-RETRIES).
        wait_seconds: Base wait interval (WS-RETRY-INTERVAL).
        exponential_backoff: Use exponential backoff.
        retry_on: Exception types to retry on. Defaults to
            general Exception.

    Returns:
        A tenacity retry decorator.

    Example:
        @retry_database_operation(max_retries=3, wait_seconds=2)
        def fetch_portfolio(portfolio_id: str) -> dict:
            ...
    """
    if retry_on is None:
        retry_on = [Exception]

    config = RetryConfig(
        max_retries=max_retries,
        wait_seconds=wait_seconds,
        exponential_backoff=exponential_backoff,
        backoff_multiplier=wait_seconds,
        retry_exceptions=list(retry_on),
    )
    return create_retry_decorator(config)


def retry_external_call(
    max_retries: int = 3,
    wait_seconds: float = 1.0,
    backoff_max: float = 30.0,
    retry_on: Optional[Sequence[Type[Exception]]] = None,
) -> Callable:
    """Decorator for external service calls with retry logic.

    Uses exponential backoff by default, suitable for external APIs
    and service-to-service calls.

    Args:
        max_retries: Maximum number of retries.
        wait_seconds: Base wait interval.
        backoff_max: Maximum backoff wait time.
        retry_on: Exception types to retry on. Defaults to
            general Exception.

    Returns:
        A tenacity retry decorator.

    Example:
        @retry_external_call(max_retries=5)
        def call_pricing_service(symbol: str) -> float:
            ...
    """
    if retry_on is None:
        retry_on = [Exception]

    config = RetryConfig(
        max_retries=max_retries,
        wait_seconds=wait_seconds,
        exponential_backoff=True,
        backoff_multiplier=wait_seconds,
        backoff_max=backoff_max,
        retry_exceptions=list(retry_on),
    )
    return create_retry_decorator(config)


class RecoveryManager:
    """Manages recovery operations mirroring DB2RECV program structure.

    Provides methods for each recovery type:
    - Connection recovery (P100)
    - Transaction recovery (P200)
    - Cursor recovery (P300)

    Uses abstract callbacks so the actual DB operations can be
    plugged in by the database layer.
    """

    def __init__(
        self,
        max_retries: int = 3,
        wait_seconds: float = 2.0,
    ) -> None:
        """Initialize recovery manager with DB2RECV defaults.

        Args:
            max_retries: WS-MAX-RETRIES (default 3).
            wait_seconds: WS-RETRY-INTERVAL (default 2).
        """
        self.max_retries = max_retries
        self.wait_seconds = wait_seconds
        self._retry_count = 0
        self._last_error: Optional[Exception] = None

    @property
    def retry_count(self) -> int:
        """Current retry count (WS-RETRY-COUNT)."""
        return self._retry_count

    @property
    def last_error(self) -> Optional[Exception]:
        """Last error encountered (WS-LAST-ERROR)."""
        return self._last_error

    def recover_connection(
        self,
        connect_fn: Callable[[], Any],
    ) -> dict:
        """P100: Recover connection with retry loop.

        Mirrors P100-RECOVER-CONNECTION:
        - Attempts reconnection up to max_retries times
        - Waits between attempts (P120-WAIT-INTERVAL)
        - Returns success/failure status

        Args:
            connect_fn: Callable that attempts to establish connection.
                Should raise an exception on failure.

        Returns:
            Dictionary with status ('S'=success, 'F'=failed),
            response_code, and error info.
        """
        self._retry_count = 0
        self._last_error = None

        config = RetryConfig(
            max_retries=self.max_retries,
            wait_seconds=self.wait_seconds,
            retry_exceptions=[Exception],
        )
        retry_decorator = create_retry_decorator(config)

        try:
            result = retry_decorator(connect_fn)()
            return {
                "status": "S",
                "response_code": 0,
                "result": result,
            }
        except Exception as exc:
            self._last_error = exc
            return {
                "status": "F",
                "response_code": -1,
                "error_info": str(exc),
            }

    def recover_transaction(
        self,
        rollback_fn: Callable[[], Any],
    ) -> dict:
        """P200: Recover transaction via rollback.

        Mirrors P200-RECOVER-TRANSACTION:
        - Executes SQL ROLLBACK
        - Returns success/failure status

        Args:
            rollback_fn: Callable that performs transaction rollback.

        Returns:
            Dictionary with status and response code.
        """
        try:
            rollback_fn()
            return {
                "status": "S",
                "response_code": 0,
            }
        except Exception as exc:
            self._last_error = exc
            return {
                "status": "F",
                "response_code": -1,
                "error_info": str(exc),
            }
