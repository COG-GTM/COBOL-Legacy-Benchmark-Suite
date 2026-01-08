"""Authentication and authorization module - replaces SECMGR.

This module provides JWT-based authentication and authorization,
replacing the COBOL SECMGR program and RACF integration.
"""

from app.auth.security import (
    SecurityManager,
    Token,
    TokenData,
    UserCreate,
    UserResponse,
    get_current_user,
    get_current_user_optional,
    require_admin,
)

__all__ = [
    "SecurityManager",
    "Token",
    "TokenData",
    "UserCreate",
    "UserResponse",
    "get_current_user",
    "get_current_user_optional",
    "require_admin",
]
