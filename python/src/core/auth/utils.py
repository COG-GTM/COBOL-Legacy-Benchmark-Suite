"""Token generation, validation, and password hashing utilities.

Replaces CICS/RACF credential management from SECMGR:
  - Password verification -> bcrypt hashing
  - Session tokens -> JWT via PyJWT
  - User validation (P100) -> token-based authentication
"""

import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Optional

import bcrypt
import jwt

logger = logging.getLogger("clbs.auth.utils")


def hash_password(password: str) -> str:
    """Hash a plaintext password using bcrypt.

    Args:
        password: Plaintext password to hash.

    Returns:
        Bcrypt hash string.
    """
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password.encode("utf-8"), salt)
    return hashed.decode("utf-8")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a plaintext password against a bcrypt hash.

    Mirrors SECMGR P100-VALIDATE-USER credential verification.

    Args:
        plain_password: Plaintext password to verify.
        hashed_password: Stored bcrypt hash.

    Returns:
        True if the password matches the hash.
    """
    return bcrypt.checkpw(
        plain_password.encode("utf-8"),
        hashed_password.encode("utf-8"),
    )


class TokenManager:
    """JWT token manager replacing CICS session management.

    Handles token generation, validation, and refresh,
    mirroring SECMGR's user validation (SEC-VALIDATE 'V')
    and authorization (SEC-AUTHORIZE 'A') flows.

    Attributes:
        secret_key: Secret key for signing JWT tokens.
        algorithm: JWT signing algorithm (default HS256).
        access_token_expire_minutes: Access token TTL in minutes.
        refresh_token_expire_days: Refresh token TTL in days.
    """

    def __init__(
        self,
        secret_key: str,
        algorithm: str = "HS256",
        access_token_expire_minutes: int = 30,
        refresh_token_expire_days: int = 7,
    ) -> None:
        self._secret_key = secret_key
        self._algorithm = algorithm
        self._access_token_expire_minutes = access_token_expire_minutes
        self._refresh_token_expire_days = refresh_token_expire_days

    def create_access_token(
        self,
        user_id: str,
        username: str,
        roles: Optional[list[str]] = None,
        extra_claims: Optional[dict[str, Any]] = None,
    ) -> str:
        """Generate a JWT access token.

        Replaces CICS session token / RACF credential validation.

        Args:
            user_id: User's unique identifier (SEC-USER-ID).
            username: User's login name.
            roles: List of role names for the user.
            extra_claims: Additional JWT claims.

        Returns:
            Encoded JWT token string.
        """
        now = datetime.now(timezone.utc)
        expire = now + timedelta(minutes=self._access_token_expire_minutes)

        payload: dict[str, Any] = {
            "sub": user_id,
            "username": username,
            "roles": roles or [],
            "type": "access",
            "iat": now,
            "exp": expire,
        }

        if extra_claims:
            payload.update(extra_claims)

        token = jwt.encode(payload, self._secret_key, algorithm=self._algorithm)
        logger.debug(
            "Access token created for user %s",
            user_id,
            extra={"user_id": user_id, "expires": expire.isoformat()},
        )
        return token

    def create_refresh_token(
        self,
        user_id: str,
    ) -> str:
        """Generate a JWT refresh token.

        Args:
            user_id: User's unique identifier.

        Returns:
            Encoded JWT refresh token string.
        """
        now = datetime.now(timezone.utc)
        expire = now + timedelta(days=self._refresh_token_expire_days)

        payload: dict[str, Any] = {
            "sub": user_id,
            "type": "refresh",
            "iat": now,
            "exp": expire,
        }

        token = jwt.encode(payload, self._secret_key, algorithm=self._algorithm)
        logger.debug(
            "Refresh token created for user %s",
            user_id,
            extra={"user_id": user_id, "expires": expire.isoformat()},
        )
        return token

    def validate_token(self, token: str) -> dict[str, Any]:
        """Validate and decode a JWT token.

        Mirrors SECMGR P100-VALIDATE-USER:
          - Verifies token signature and expiration
          - Returns decoded claims on success
          - Raises appropriate errors on failure

        Args:
            token: JWT token string to validate.

        Returns:
            Decoded token payload dictionary.

        Raises:
            jwt.ExpiredSignatureError: If the token has expired.
            jwt.InvalidTokenError: If the token is invalid.
        """
        payload = jwt.decode(
            token,
            self._secret_key,
            algorithms=[self._algorithm],
        )
        return payload

    def validate_access_token(self, token: str) -> dict[str, Any]:
        """Validate an access token specifically.

        Args:
            token: JWT access token string.

        Returns:
            Decoded token payload.

        Raises:
            ValueError: If the token is not an access token.
            jwt.ExpiredSignatureError: If the token has expired.
            jwt.InvalidTokenError: If the token is invalid.
        """
        payload = self.validate_token(token)
        if payload.get("type") != "access":
            raise ValueError("Token is not an access token")
        return payload

    def validate_refresh_token(self, token: str) -> dict[str, Any]:
        """Validate a refresh token specifically.

        Args:
            token: JWT refresh token string.

        Returns:
            Decoded token payload.

        Raises:
            ValueError: If the token is not a refresh token.
            jwt.ExpiredSignatureError: If the token has expired.
            jwt.InvalidTokenError: If the token is invalid.
        """
        payload = self.validate_token(token)
        if payload.get("type") != "refresh":
            raise ValueError("Token is not a refresh token")
        return payload
