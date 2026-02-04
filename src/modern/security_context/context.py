"""
SecurityContext - Modern replacement for CICS context capture.

This module replaces the CICS ASSIGN commands that captured:
- USERID (now user_id from JWT/OAuth2)
- TERMID (now client_id from IP/device/session)
- TRANSID (now request_id as UUID/correlation ID)

Original COBOL (SECMGR.cbl lines 105-112):
    EXEC CICS ASSIGN
              USERID(WS-USER-ID)
              TERMID(WS-TERMINAL-ID)
              TRANSID(WS-TRANSACTION-ID)
    END-EXEC.
"""

from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Optional
from contextvars import ContextVar

_current_context: ContextVar[Optional["SecurityContext"]] = ContextVar(
    "security_context", default=None
)


@dataclass(frozen=True)
class SecurityContext:
    """
    Immutable security context that holds all authentication and tracing information.

    This replaces the implicit CICS context capture with explicit, typed context.
    The context is immutable to prevent accidental modification during request processing.

    Attributes:
        user_id: Authenticated user identifier (replaces CICS USERID).
                 Can be email, username, or any unique identifier.
                 Max 255 chars (expanded from original 8 chars).

        client_id: Client/terminal identifier (replaces CICS TERMID).
                   Can be IP address, device ID, or session ID.
                   Max 255 chars (expanded from original 4 chars).

        request_id: Request correlation ID (replaces CICS TRANSID).
                    UUID format for distributed tracing.
                    36 chars (expanded from original 4 chars).

        trace_id: Optional OpenTelemetry trace ID for distributed systems.
        session_id: Optional session identifier for stateful interactions.
        user_agent: Optional HTTP User-Agent for audit purposes.
        timestamp: When the context was created.
        claims: Additional JWT claims or user attributes.
    """

    user_id: str
    client_id: str
    request_id: str
    trace_id: Optional[str] = None
    session_id: Optional[str] = None
    user_agent: Optional[str] = None
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    claims: dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        if not self.user_id:
            raise ValueError("user_id is required")
        if not self.client_id:
            raise ValueError("client_id is required")
        if not self.request_id:
            raise ValueError("request_id is required")

    def to_dict(self) -> dict[str, Any]:
        """Convert context to dictionary for serialization."""
        return {
            "user_id": self.user_id,
            "client_id": self.client_id,
            "request_id": self.request_id,
            "trace_id": self.trace_id,
            "session_id": self.session_id,
            "user_agent": self.user_agent,
            "timestamp": self.timestamp.isoformat(),
            "claims": self.claims,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> SecurityContext:
        """Create context from dictionary."""
        timestamp = data.get("timestamp")
        if isinstance(timestamp, str):
            timestamp = datetime.fromisoformat(timestamp)
        elif timestamp is None:
            timestamp = datetime.now(timezone.utc)

        return cls(
            user_id=data["user_id"],
            client_id=data["client_id"],
            request_id=data["request_id"],
            trace_id=data.get("trace_id"),
            session_id=data.get("session_id"),
            user_agent=data.get("user_agent"),
            timestamp=timestamp,
            claims=data.get("claims", {}),
        )


class SecurityContextBuilder:
    """
    Builder pattern for constructing SecurityContext instances.

    Provides a fluent interface for building context from various sources.
    """

    def __init__(self) -> None:
        self._user_id: Optional[str] = None
        self._client_id: Optional[str] = None
        self._request_id: Optional[str] = None
        self._trace_id: Optional[str] = None
        self._session_id: Optional[str] = None
        self._user_agent: Optional[str] = None
        self._timestamp: Optional[datetime] = None
        self._claims: dict[str, Any] = {}

    def with_user_id(self, user_id: str) -> SecurityContextBuilder:
        """Set the user identifier."""
        self._user_id = user_id
        return self

    def with_client_id(self, client_id: str) -> SecurityContextBuilder:
        """Set the client/terminal identifier."""
        self._client_id = client_id
        return self

    def with_request_id(self, request_id: Optional[str] = None) -> SecurityContextBuilder:
        """Set or generate the request correlation ID."""
        self._request_id = request_id or str(uuid.uuid4())
        return self

    def with_trace_id(self, trace_id: str) -> SecurityContextBuilder:
        """Set the distributed trace ID."""
        self._trace_id = trace_id
        return self

    def with_session_id(self, session_id: str) -> SecurityContextBuilder:
        """Set the session identifier."""
        self._session_id = session_id
        return self

    def with_user_agent(self, user_agent: str) -> SecurityContextBuilder:
        """Set the user agent string."""
        self._user_agent = user_agent
        return self

    def with_timestamp(self, timestamp: datetime) -> SecurityContextBuilder:
        """Set the context creation timestamp."""
        self._timestamp = timestamp
        return self

    def with_claims(self, claims: dict[str, Any]) -> SecurityContextBuilder:
        """Set additional JWT claims or user attributes."""
        self._claims = claims
        return self

    def add_claim(self, key: str, value: Any) -> SecurityContextBuilder:
        """Add a single claim to the context."""
        self._claims[key] = value
        return self

    def build(self) -> SecurityContext:
        """
        Build the SecurityContext instance.

        Raises:
            ValueError: If required fields are missing.
        """
        if not self._user_id:
            raise ValueError("user_id is required")
        if not self._client_id:
            raise ValueError("client_id is required")

        return SecurityContext(
            user_id=self._user_id,
            client_id=self._client_id,
            request_id=self._request_id or str(uuid.uuid4()),
            trace_id=self._trace_id,
            session_id=self._session_id,
            user_agent=self._user_agent,
            timestamp=self._timestamp or datetime.now(timezone.utc),
            claims=self._claims,
        )


def set_current_context(context: SecurityContext) -> None:
    """Set the current security context for the current async context."""
    _current_context.set(context)


def get_current_context() -> Optional[SecurityContext]:
    """Get the current security context from the current async context."""
    return _current_context.get()


def clear_current_context() -> None:
    """Clear the current security context."""
    _current_context.set(None)
