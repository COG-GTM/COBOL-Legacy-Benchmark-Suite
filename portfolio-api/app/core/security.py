"""Password hashing and JWT token utilities.

Replaces COBOL SECMGR credential management:
  - Password verification -> bcrypt via passlib
  - Session tokens -> JWT via python-jose
  - P100-VALIDATE-USER -> token-based authentication
"""

import uuid
from datetime import datetime, timedelta, timezone
from typing import Any

from jose import JWTError, jwt
from passlib.context import CryptContext

from app.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    """Hash a plaintext password using bcrypt."""
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a plaintext password against a bcrypt hash."""
    return pwd_context.verify(plain_password, hashed_password)


def create_access_token(
    user_id: str,
    username: str,
    roles: list[str],
    expires_delta: timedelta | None = None,
) -> str:
    """Generate a JWT access token.

    Args:
        user_id: User's UUID string.
        username: User's login name.
        roles: List of role names.
        expires_delta: Optional custom expiry duration.

    Returns:
        Encoded JWT token string.
    """
    now = datetime.now(timezone.utc)
    expire = now + (expires_delta or timedelta(minutes=settings.JWT_EXPIRY_MINUTES))

    payload: dict[str, Any] = {
        "sub": user_id,
        "username": username,
        "roles": roles,
        "type": "access",
        "jti": str(uuid.uuid4()),
        "iat": now,
        "exp": expire,
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


def create_refresh_token(
    user_id: str,
    expires_delta: timedelta | None = None,
) -> str:
    """Generate a JWT refresh token.

    Args:
        user_id: User's UUID string.
        expires_delta: Optional custom expiry duration.

    Returns:
        Encoded JWT refresh token string.
    """
    now = datetime.now(timezone.utc)
    expire = now + (
        expires_delta or timedelta(days=settings.JWT_REFRESH_EXPIRY_DAYS)
    )

    payload: dict[str, Any] = {
        "sub": user_id,
        "type": "refresh",
        "jti": str(uuid.uuid4()),
        "iat": now,
        "exp": expire,
    }
    return jwt.encode(
        payload, settings.JWT_REFRESH_SECRET, algorithm=settings.JWT_ALGORITHM
    )


def decode_access_token(token: str) -> dict[str, Any]:
    """Decode and validate a JWT access token.

    Raises:
        JWTError: If the token is invalid or expired.
        ValueError: If the token is not an access token.
    """
    payload = jwt.decode(
        token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM]
    )
    if payload.get("type") != "access":
        raise ValueError("Token is not an access token")
    return payload


def decode_refresh_token(token: str) -> dict[str, Any]:
    """Decode and validate a JWT refresh token.

    Raises:
        JWTError: If the token is invalid or expired.
        ValueError: If the token is not a refresh token.
    """
    payload = jwt.decode(
        token, settings.JWT_REFRESH_SECRET, algorithms=[settings.JWT_ALGORITHM]
    )
    if payload.get("type") != "refresh":
        raise ValueError("Token is not a refresh token")
    return payload
