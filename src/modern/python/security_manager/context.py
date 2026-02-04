"""
Security context management for the Security Manager.

Replaces COBOL EXEC CICS ASSIGN commands that capture:
- USERID: Current user identifier
- TERMID: Terminal identifier
- TRANSID: Transaction identifier

In modern applications, context can be extracted from:
- HTTP headers (X-User-ID, X-Request-ID, etc.)
- JWT tokens
- Session context
- Distributed tracing (OpenTelemetry correlation IDs)
"""

import threading
import uuid
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from contextvars import ContextVar

from .exceptions import ContextException


@dataclass
class SecurityContext:
    """
    Security context containing user and request information.
    
    Maps to COBOL WS-SECURITY-AREA:
    - WS-USER-ID: PIC X(8)
    - WS-TERMINAL-ID: PIC X(4)
    - WS-TRANSACTION-ID: PIC X(4)
    - WS-PROGRAM-NAME: PIC X(8)
    - WS-ACCESS-TYPE: PIC X(8)
    - WS-TIMESTAMP: PIC X(26)
    """
    user_id: str
    terminal_id: str = "WEB"
    transaction_id: str = field(default_factory=lambda: str(uuid.uuid4())[:4].upper())
    program_name: Optional[str] = None
    access_type: Optional[str] = None
    timestamp: datetime = field(default_factory=datetime.utcnow)
    
    request_id: Optional[str] = None
    correlation_id: Optional[str] = None
    source_ip: Optional[str] = None
    user_agent: Optional[str] = None
    
    def __post_init__(self):
        if len(self.user_id) > 8:
            self.user_id = self.user_id[:8]
        if len(self.terminal_id) > 4:
            self.terminal_id = self.terminal_id[:4]
        if len(self.transaction_id) > 4:
            self.transaction_id = self.transaction_id[:4]
        
        if not self.request_id:
            self.request_id = str(uuid.uuid4())
        if not self.correlation_id:
            self.correlation_id = self.request_id
    
    def validate(self) -> bool:
        """
        Validate that required context fields are present.
        
        Maps to COBOL validation in P100-VALIDATE-USER where
        SEC-RESPONSE-CODE is set to 12 if credentials cannot be obtained.
        """
        if not self.user_id or not self.user_id.strip():
            raise ContextException(
                message="User ID is required",
                error_info="Unable to obtain user credentials"
            )
        return True
    
    def to_dict(self) -> dict:
        """Convert context to dictionary for logging and serialization."""
        return {
            "user_id": self.user_id,
            "terminal_id": self.terminal_id,
            "transaction_id": self.transaction_id,
            "program_name": self.program_name,
            "access_type": self.access_type,
            "timestamp": self.timestamp.isoformat() if self.timestamp else None,
            "request_id": self.request_id,
            "correlation_id": self.correlation_id,
            "source_ip": self.source_ip,
            "user_agent": self.user_agent
        }
    
    @classmethod
    def from_headers(cls, headers: dict) -> "SecurityContext":
        """
        Create SecurityContext from HTTP headers.
        
        Expected headers:
        - X-User-ID or Authorization (JWT)
        - X-Terminal-ID (optional, defaults to 'WEB')
        - X-Transaction-ID (optional, auto-generated)
        - X-Request-ID (optional, auto-generated)
        - X-Correlation-ID (optional, defaults to request_id)
        """
        user_id = headers.get("X-User-ID") or headers.get("x-user-id")
        
        if not user_id:
            auth_header = headers.get("Authorization") or headers.get("authorization")
            if auth_header and auth_header.startswith("Bearer "):
                user_id = cls._extract_user_from_jwt(auth_header[7:])
        
        if not user_id:
            raise ContextException(
                message="User ID not found in request headers",
                error_info="Unable to obtain user credentials"
            )
        
        return cls(
            user_id=user_id,
            terminal_id=headers.get("X-Terminal-ID", headers.get("x-terminal-id", "WEB")),
            transaction_id=headers.get("X-Transaction-ID", headers.get("x-transaction-id", str(uuid.uuid4())[:4].upper())),
            request_id=headers.get("X-Request-ID", headers.get("x-request-id")),
            correlation_id=headers.get("X-Correlation-ID", headers.get("x-correlation-id")),
            source_ip=headers.get("X-Forwarded-For", headers.get("x-forwarded-for")),
            user_agent=headers.get("User-Agent", headers.get("user-agent"))
        )
    
    @staticmethod
    def _extract_user_from_jwt(token: str) -> Optional[str]:
        """
        Extract user ID from JWT token.
        
        Note: In production, use proper JWT validation library.
        This is a simplified implementation for demonstration.
        """
        try:
            import base64
            import json
            parts = token.split(".")
            if len(parts) >= 2:
                payload = parts[1]
                padding = 4 - len(payload) % 4
                if padding != 4:
                    payload += "=" * padding
                decoded = base64.urlsafe_b64decode(payload)
                claims = json.loads(decoded)
                return claims.get("sub") or claims.get("user_id") or claims.get("username")
        except Exception:
            pass
        return None


_context_var: ContextVar[Optional[SecurityContext]] = ContextVar("security_context", default=None)


def set_current_context(context: SecurityContext) -> None:
    """Set the current security context for the current async context/thread."""
    _context_var.set(context)


def get_current_context() -> Optional[SecurityContext]:
    """Get the current security context for the current async context/thread."""
    return _context_var.get()


def clear_current_context() -> None:
    """Clear the current security context."""
    _context_var.set(None)


class SecurityContextManager:
    """
    Context manager for security context lifecycle.
    
    Usage:
        with SecurityContextManager(context):
            # Security context is available via get_current_context()
            pass
    """
    
    def __init__(self, context: SecurityContext):
        self.context = context
        self._token = None
    
    def __enter__(self) -> SecurityContext:
        self._token = _context_var.set(self.context)
        return self.context
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        _context_var.reset(self._token)
        return False
    
    async def __aenter__(self) -> SecurityContext:
        return self.__enter__()
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        return self.__exit__(exc_type, exc_val, exc_tb)
