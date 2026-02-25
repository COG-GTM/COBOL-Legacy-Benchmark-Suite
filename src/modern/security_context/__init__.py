"""
Modern Security Context Propagation System

This module provides a modernized implementation of the SECMGR security manager,
replacing CICS-based context capture with a modern approach using:
- JWT tokens for user authentication
- Request correlation IDs for distributed tracing
- Dependency injection for explicit context passing
- Expanded audit logging for modern identifiers

The three-phase security model is preserved:
1. Validation - Verify user identity (JWT signature, token validity)
2. Authorization - Check user permissions for requested resource
3. Audit - Log all access attempts with full context
"""

from .context import SecurityContext, SecurityContextBuilder
from .middleware import SecurityContextMiddleware, get_security_context
from .models import AuditLogEntry, AuthorizationRecord, UserIdentity
from .security_manager import SecurityManager, SecurityPhase, SecurityResponse
from .extractors import (
    ContextExtractor,
    JWTExtractor,
    OAuth2Extractor,
    SessionExtractor,
    HeaderExtractor,
)

__all__ = [
    "SecurityContext",
    "SecurityContextBuilder",
    "SecurityContextMiddleware",
    "get_security_context",
    "AuditLogEntry",
    "AuthorizationRecord",
    "UserIdentity",
    "SecurityManager",
    "SecurityPhase",
    "SecurityResponse",
    "ContextExtractor",
    "JWTExtractor",
    "OAuth2Extractor",
    "SessionExtractor",
    "HeaderExtractor",
]
