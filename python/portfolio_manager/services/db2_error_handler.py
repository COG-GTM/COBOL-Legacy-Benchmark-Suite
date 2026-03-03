"""DB2/SQL error handler with retry logic.

Replaces:
  - DB2ERR  (src/programs/common/DB2ERR.cbl)   — SQL error handler
  - DB2RECV (src/programs/online/DB2RECV.cbl)   — DB2 recovery manager
  - DB2CMT  (src/programs/common/DB2CMT.cbl)    — DB2 commit manager
  - DB2STAT (src/programs/common/DB2STAT.cbl)   — DB2 statistics

Uses the `tenacity` library for deadlock/timeout retry logic,
replacing the COBOL retry loop with DB2-MAX-RETRIES.
"""

from __future__ import annotations

import logging
from datetime import datetime
from typing import Callable, TypeVar

from sqlalchemy.exc import DBAPIError, IntegrityError, OperationalError
from sqlalchemy.orm import Session
from tenacity import (
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from portfolio_manager.models.database import ErrorLog
from portfolio_manager.services.error_handler import (
    DatabaseError,
    Severity,
)

logger = logging.getLogger(__name__)

T = TypeVar("T")

# ---------------------------------------------------------------------------
# SQL error code constants (from SQLCA.cpy + DB2ERR.cbl)
# ---------------------------------------------------------------------------

# PostgreSQL SQLSTATE codes (equivalent to DB2 SQLCODEs in original)
SQLSTATE_DEADLOCK = "40P01"  # DB2: -911
SQLSTATE_LOCK_TIMEOUT = "55P03"  # DB2: -913
SQLSTATE_CONNECTION_ERROR = "08001"  # DB2: -30081
SQLSTATE_UNIQUE_VIOLATION = "23505"  # DB2: -803
SQLSTATE_NOT_FOUND = "02000"  # DB2: +100

# PostgreSQL error codes mapped from DB2 codes
PG_DEADLOCK = "40P01"
PG_SERIALIZATION_FAILURE = "40001"


# ---------------------------------------------------------------------------
# Error diagnosis (replaces DB2ERR 2000-DIAGNOSE-ERROR)
# ---------------------------------------------------------------------------


def diagnose_sql_error(exc: DBAPIError) -> tuple[str, int, bool]:
    """Diagnose a SQL error and determine severity + retryability.

    Replaces DB2ERR paragraph 2000-DIAGNOSE-ERROR which evaluates
    SQLCODE to determine error text, return code, and retry flag.

    Args:
        exc: The SQLAlchemy DBAPI exception.

    Returns:
        Tuple of (error_message, return_code, should_retry).
    """
    sqlstate = getattr(exc.orig, "pgcode", "") if exc.orig else ""

    if sqlstate in (SQLSTATE_DEADLOCK, PG_DEADLOCK, PG_SERIALIZATION_FAILURE):
        return "Deadlock detected - retry transaction", 4, True
    elif sqlstate == SQLSTATE_LOCK_TIMEOUT:
        return "Timeout occurred - retry transaction", 4, True
    elif sqlstate == SQLSTATE_CONNECTION_ERROR:
        return "Database connection error - check availability", 12, False
    elif sqlstate == SQLSTATE_UNIQUE_VIOLATION:
        return "Duplicate key violation", 8, False
    elif isinstance(exc, IntegrityError):
        return "Data integrity violation", 8, False
    else:
        sqlcode = str(getattr(exc.orig, "pgcode", "UNKNOWN"))
        return f"Database error (SQLSTATE: {sqlcode})", 12, False


# ---------------------------------------------------------------------------
# Error severity determination (replaces DB2ERR 1100-SET-SEVERITY)
# ---------------------------------------------------------------------------


def get_error_severity(exc: DBAPIError) -> int:
    """Determine error severity from a SQL exception.

    Replaces DB2ERR paragraph 1100-SET-SEVERITY.

    Returns:
        Severity level (1=Info, 2=Warning, 3=Error, 4=Severe).
    """
    sqlstate = getattr(exc.orig, "pgcode", "") if exc.orig else ""

    if sqlstate in (SQLSTATE_DEADLOCK, PG_DEADLOCK, SQLSTATE_LOCK_TIMEOUT):
        return Severity.WARNING
    elif sqlstate == SQLSTATE_CONNECTION_ERROR:
        return Severity.SEVERE
    elif sqlstate == SQLSTATE_UNIQUE_VIOLATION:
        return Severity.INFO
    elif sqlstate == SQLSTATE_NOT_FOUND:
        return Severity.INFO
    else:
        return Severity.ERROR


# ---------------------------------------------------------------------------
# Error logging to database (replaces DB2ERR 1000-LOG-ERROR)
# ---------------------------------------------------------------------------


def log_sql_error(
    session: Session,
    program_id: str,
    exc: DBAPIError | Exception,
    additional_info: str = "",
) -> None:
    """Log a SQL error to the ERRLOG table.

    Replaces DB2ERR paragraph 1000-LOG-ERROR / 1200-INSERT-ERROR.

    Args:
        session: Active database session (may use a separate session
                 if the current transaction is in a failed state).
        program_id: ID of the program that encountered the error.
        exc: The exception that was caught.
        additional_info: Extra context about the error.
    """
    now = datetime.now()
    sqlstate = ""
    if isinstance(exc, DBAPIError) and exc.orig:
        sqlstate = getattr(exc.orig, "pgcode", "")

    severity = (
        get_error_severity(exc) if isinstance(exc, DBAPIError) else Severity.ERROR
    )

    try:
        error_record = ErrorLog(
            error_timestamp=now,
            program_id=program_id,
            error_type="D",  # Database error
            error_severity=severity,
            error_code=sqlstate or "PYERR",
            error_message=str(exc)[:200],
            process_date=now.date(),
            process_time=now.time(),
            user_id="SYSTEM",
            additional_info=additional_info[:500] if additional_info else None,
        )
        session.add(error_record)
        session.flush()
    except Exception as log_exc:
        # If we can't log to DB, fall back to Python logging
        logger.error(
            "Failed to log SQL error to database: %s | Original error: %s",
            log_exc,
            exc,
        )


# ---------------------------------------------------------------------------
# Retry decorator (replaces DB2ERR retry loop + DB2RECV recovery)
# ---------------------------------------------------------------------------


def with_db_retry(
    max_retries: int = 3,
    wait_multiplier: float = 0.1,
    wait_max: float = 2.0,
) -> Callable[[Callable[..., T]], Callable[..., T]]:
    """Decorator for database operations with automatic retry on transient errors.

    Replaces:
      - DB2ERR retry logic (DB2-MAX-RETRIES=3, DB2-RETRY-WAIT=100ms)
      - DB2RECV recovery manager for connection failures

    Args:
        max_retries: Maximum number of retry attempts (default 3, matching COBOL).
        wait_multiplier: Base wait time multiplier for exponential backoff.
        wait_max: Maximum wait time in seconds.

    Returns:
        Decorated function with retry behavior.
    """

    def decorator(func: Callable[..., T]) -> Callable[..., T]:
        @retry(
            retry=retry_if_exception_type(OperationalError),
            stop=stop_after_attempt(max_retries + 1),
            wait=wait_exponential(multiplier=wait_multiplier, max=wait_max),
            reraise=True,
        )
        def wrapper(*args: object, **kwargs: object) -> T:
            try:
                return func(*args, **kwargs)
            except OperationalError as exc:
                message, rc, should_retry = diagnose_sql_error(exc)
                logger.warning(
                    "DB operation failed (retry=%s): %s",
                    should_retry,
                    message,
                )
                if not should_retry:
                    raise DatabaseError(message) from exc
                raise  # let tenacity retry

        return wrapper  # type: ignore[return-value]

    return decorator


# ---------------------------------------------------------------------------
# DB2 recovery (replaces DB2RECV.cbl)
# ---------------------------------------------------------------------------


def attempt_recovery(
    session: Session,
    program_id: str,
    exc: DBAPIError,
) -> bool:
    """Attempt to recover from a DB2/SQL error.

    Replaces DB2RECV (src/programs/online/DB2RECV.cbl).
    The original COBOL program handled connection recovery,
    cursor cleanup, and transaction rollback.

    Args:
        session: The database session in error state.
        program_id: Program that encountered the error.
        exc: The database exception.

    Returns:
        True if recovery succeeded, False otherwise.
    """
    message, rc, should_retry = diagnose_sql_error(exc)

    logger.info(
        "Attempting DB recovery for %s: %s (rc=%d, retry=%s)",
        program_id,
        message,
        rc,
        should_retry,
    )

    try:
        # Roll back the failed transaction
        session.rollback()

        # Test that the session is still usable
        session.execute(
            __import__("sqlalchemy").text("SELECT 1")
        )

        logger.info("DB recovery successful for %s", program_id)
        return True

    except Exception as recovery_exc:
        logger.error(
            "DB recovery failed for %s: %s", program_id, recovery_exc
        )
        return False
