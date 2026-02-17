"""
Security context propagation.

Replaces the COBOL EXEC CICS ASSIGN commands that captured:
  - USERID   -> WS-USER-ID
  - TERMID   -> WS-TERMINAL-ID
  - TRANSID  -> WS-TRANSACTION-ID

In the modern implementation, context is extracted from HTTP request
headers, JWT tokens, or distributed tracing headers (OpenTelemetry).
Thread-local storage ensures context is available throughout the
request lifecycle without explicit passing.
"""

from __future__ import annotations

import threading
from dataclasses import dataclass, field
from typing import Optional

_thread_local = threading.local()


@dataclass(frozen=True)
class SecurityContext:
    """Immutable request context replacing CICS ASSIGN variables.

    Attributes:
        user_id: Authenticated user identifier (CICS USERID, max 8 chars).
        terminal_id: Client identifier such as IP or device ID (CICS TERMID, max 4 chars).
        transaction_id: Correlation ID for distributed tracing (CICS TRANSID, max 4 chars).
        program_name: Name of the calling program/service (max 8 chars).
        trace_id: Optional OpenTelemetry-compatible trace ID for distributed tracing.
    """

    user_id: str
    terminal_id: str = "HTTP"
    transaction_id: str = "REST"
    program_name: str = "UNKNOWN"
    trace_id: Optional[str] = field(default=None)

    def __post_init__(self) -> None:
        if not self.user_id or not self.user_id.strip():
            raise ValueError("user_id is required and cannot be empty")


def set_security_context(ctx: SecurityContext) -> None:
    """Store security context in thread-local storage for the current request."""
    _thread_local.security_context = ctx


def get_security_context() -> Optional[SecurityContext]:
    """Retrieve the current security context from thread-local storage."""
    return getattr(_thread_local, "security_context", None)


def clear_security_context() -> None:
    """Clear the security context after request completion."""
    _thread_local.security_context = None
