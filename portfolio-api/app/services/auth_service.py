"""Authentication service replacing COBOL SECMGR P100-VALIDATE-USER.

Handles credential verification, token issuance, refresh token rotation,
and refresh token blacklisting for logout.
"""

import logging
from datetime import datetime, timezone

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_refresh_token,
    verify_password,
)
from app.models.user import User

logger = logging.getLogger(__name__)

# In-memory blacklist for refresh tokens (C1 scope).
# A persistent store (Redis/DB) can replace this in C3.
_refresh_token_blacklist: set[str] = set()


def is_token_blacklisted(token: str) -> bool:
    return token in _refresh_token_blacklist


def blacklist_token(token: str) -> None:
    _refresh_token_blacklist.add(token)


async def authenticate_user(
    db: AsyncSession,
    username: str,
    password: str,
) -> User | None:
    """Verify credentials and return user if valid.

    Mirrors SECMGR P100-VALIDATE-USER:
      - Look up user by username
      - Verify password against bcrypt hash
      - Return None on failure (generic, no enumeration)
    """
    result = await db.execute(select(User).where(User.username == username))
    user = result.scalar_one_or_none()

    if user is None:
        return None

    if user.status != "active":
        return None

    if not verify_password(password, user.password_hash):
        return None

    return user


async def login(
    db: AsyncSession,
    username: str,
    password: str,
) -> dict | None:
    """Authenticate user and issue JWT tokens.

    Returns dict with access_token and refresh_token on success, None on failure.
    """
    user = await authenticate_user(db, username, password)
    if user is None:
        return None

    access_token = create_access_token(
        user_id=user.id,
        username=user.username,
        roles=user.roles,
    )
    refresh_token = create_refresh_token(user_id=user.id)

    # Update last_login timestamp
    await db.execute(
        update(User)
        .where(User.id == user.id)
        .values(last_login=datetime.now(timezone.utc))
    )
    await db.commit()

    logger.info("User authenticated: %s", user.username)

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
    }


async def refresh_tokens(
    db: AsyncSession,
    refresh_token: str,
) -> dict | None:
    """Validate refresh token and issue new token pair (full rotation).

    Returns new access_token + refresh_token, or None if invalid.
    """
    if is_token_blacklisted(refresh_token):
        return None

    try:
        payload = decode_refresh_token(refresh_token)
    except Exception:
        return None

    user_id = payload.get("sub")
    if not user_id:
        return None

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if user is None or user.status != "active":
        return None

    # Blacklist old refresh token (rotation)
    blacklist_token(refresh_token)

    new_access_token = create_access_token(
        user_id=user.id,
        username=user.username,
        roles=user.roles,
    )
    new_refresh_token = create_refresh_token(user_id=user.id)

    return {
        "access_token": new_access_token,
        "refresh_token": new_refresh_token,
        "token_type": "bearer",
    }


async def logout(refresh_token: str) -> bool:
    """Blacklist the refresh token to invalidate the session."""
    blacklist_token(refresh_token)
    return True
