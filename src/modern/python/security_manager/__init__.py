"""
Security Manager - Modern Python implementation of COBOL SECMGR.

This package provides a modernized version of the COBOL SECMGR security manager
program, implementing the three-phase security model:

1. Validation: Verify user credentials
2. Authorization: Check resource permissions  
3. Audit: Log access attempts

Usage:
    from security_manager import SecurityManager, SecurityContext
    from security_manager.models import get_engine, get_session_factory, init_db
    
    # Initialize database
    engine = get_engine("sqlite:///security.db")
    init_db(engine)
    session_factory = get_session_factory(engine)
    
    # Create security manager
    manager = SecurityManager(session_factory)
    
    # Create security context
    context = SecurityContext(user_id="TESTUSER")
    
    # Perform security check
    response = manager.check_security(context, "INQONLN", "READ")
    
    if response.success:
        print("Access granted")
    else:
        print(f"Access denied: {response.error_info}")

For REST API usage, run:
    uvicorn security_manager.api:app --reload
"""

from .models import AuthFile, AuditLog, get_engine, get_session_factory, init_db
from .context import SecurityContext, SecurityContextManager, get_current_context, set_current_context
from .exceptions import (
    SecurityException,
    ValidationException,
    AuthorizationException,
    AuditException,
    ContextException
)
from .security_manager import (
    SecurityManager,
    SecurityResponse,
    ResponseCode,
    SecurityHandler,
    ValidationHandler,
    AuthorizationHandler,
    AuditHandler
)

__version__ = "1.0.0"
__all__ = [
    "AuthFile",
    "AuditLog",
    "get_engine",
    "get_session_factory",
    "init_db",
    "SecurityContext",
    "SecurityContextManager",
    "get_current_context",
    "set_current_context",
    "SecurityException",
    "ValidationException",
    "AuthorizationException",
    "AuditException",
    "ContextException",
    "SecurityManager",
    "SecurityResponse",
    "ResponseCode",
    "SecurityHandler",
    "ValidationHandler",
    "AuthorizationHandler",
    "AuditHandler",
]
