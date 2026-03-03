"""Security and authentication service.

Replaces:
  - SECMGR (src/programs/common/SECMGR.cbl) — RACF/CICS security manager

Uses JWT tokens for authentication instead of RACF security checks.
Provides OAuth2-compatible middleware for FastAPI integration.
"""

from __future__ import annotations

import logging
import os
from datetime import datetime, timedelta, timezone
from typing import Optional

from pydantic import BaseModel

logger = logging.getLogger(__name__)

# JWT settings — override via environment variables
JWT_SECRET_KEY = os.environ.get("JWT_SECRET_KEY", "dev-secret-key-change-in-production")
JWT_ALGORITHM = "HS256"
JWT_ACCESS_TOKEN_EXPIRE_MINUTES = int(
    os.environ.get("JWT_ACCESS_TOKEN_EXPIRE_MINUTES", "30")
)


class TokenData(BaseModel):
    """JWT token payload data."""

    user_id: str
    terminal_id: str = ""
    roles: list[str] = []
    exp: Optional[datetime] = None


class AuthResult(BaseModel):
    """Authentication result.

    Replaces SECMGR SEC-RESPONSE-CODE and SEC-ERROR-MSG fields.
    """

    authenticated: bool = False
    user_id: str = ""
    roles: list[str] = []
    error_message: str = ""
    response_code: int = 0


class SecurityManager:
    """Security manager service.

    Replaces SECMGR (src/programs/common/SECMGR.cbl).

    The original COBOL program performed:
      - SEC-REQUEST-TYPE 'A' (Authenticate)  -> authenticate()
      - SEC-REQUEST-TYPE 'V' (Validate)      -> validate_token()
      - SEC-REQUEST-TYPE 'R' (Refresh)        -> refresh_token()

    Uses JWT tokens instead of RACF security lookups.
    """

    def __init__(
        self,
        secret_key: str = JWT_SECRET_KEY,
        algorithm: str = JWT_ALGORITHM,
        expire_minutes: int = JWT_ACCESS_TOKEN_EXPIRE_MINUTES,
    ):
        self._secret_key = secret_key
        self._algorithm = algorithm
        self._expire_minutes = expire_minutes

    def create_access_token(
        self,
        user_id: str,
        terminal_id: str = "",
        roles: list[str] | None = None,
        expires_delta: timedelta | None = None,
    ) -> str:
        """Create a JWT access token.

        Replaces SECMGR paragraph 2000-AUTHENTICATE which sets up
        the security context in the CICS environment.

        Args:
            user_id: User identifier (was SEC-USER-ID).
            terminal_id: Terminal identifier (was SEC-TERMINAL-ID).
            roles: List of role names.
            expires_delta: Custom expiration delta.

        Returns:
            Encoded JWT token string.
        """
        import jwt

        expire = datetime.now(timezone.utc) + (
            expires_delta or timedelta(minutes=self._expire_minutes)
        )

        payload = {
            "sub": user_id,
            "terminal": terminal_id,
            "roles": roles or [],
            "exp": expire,
            "iat": datetime.now(timezone.utc),
        }

        token = jwt.encode(payload, self._secret_key, algorithm=self._algorithm)
        logger.info("Access token created for user=%s", user_id)
        return token

    def validate_token(self, token: str) -> AuthResult:
        """Validate a JWT token and return the authentication result.

        Replaces SECMGR paragraph 3000-VALIDATE which checks the
        security context against RACF.

        Args:
            token: JWT token string.

        Returns:
            AuthResult with authentication status.
        """
        import jwt

        try:
            payload = jwt.decode(
                token, self._secret_key, algorithms=[self._algorithm]
            )
            return AuthResult(
                authenticated=True,
                user_id=payload.get("sub", ""),
                roles=payload.get("roles", []),
                response_code=0,
            )
        except jwt.ExpiredSignatureError:
            logger.warning("Token expired")
            return AuthResult(
                authenticated=False,
                error_message="Token has expired",
                response_code=4,
            )
        except jwt.InvalidTokenError as exc:
            logger.warning("Invalid token: %s", exc)
            return AuthResult(
                authenticated=False,
                error_message="Invalid token",
                response_code=8,
            )

    def authenticate(
        self,
        user_id: str,
        password: str,
        terminal_id: str = "",
    ) -> tuple[AuthResult, str]:
        """Authenticate a user and return a token.

        Replaces SECMGR SEC-REQUEST-TYPE = 'A' flow.
        In production, this would validate against a user store.

        Args:
            user_id: User identifier.
            password: User password.
            terminal_id: Terminal/session identifier.

        Returns:
            Tuple of (AuthResult, token_string).
        """
        # In production, replace with actual credential validation
        # (e.g., database lookup, LDAP, OAuth2 provider)
        if not user_id or not password:
            return (
                AuthResult(
                    authenticated=False,
                    error_message="User ID and password required",
                    response_code=8,
                ),
                "",
            )

        # Generate token for authenticated user
        roles = self._get_user_roles(user_id)
        token = self.create_access_token(
            user_id=user_id,
            terminal_id=terminal_id,
            roles=roles,
        )

        result = AuthResult(
            authenticated=True,
            user_id=user_id,
            roles=roles,
            response_code=0,
        )

        logger.info("User authenticated: %s", user_id)
        return result, token

    def _get_user_roles(self, user_id: str) -> list[str]:
        """Get roles for a user.

        Replaces SECMGR paragraph 3100-CHECK-AUTHORITY which
        checks RACF profiles for user authorization.

        In production, implement actual role lookup.
        """
        # Default roles — replace with database/LDAP lookup
        return ["portfolio_viewer"]

    def check_authorization(
        self,
        auth_result: AuthResult,
        required_role: str,
    ) -> bool:
        """Check if a user has a specific role.

        Replaces SECMGR paragraph 3100-CHECK-AUTHORITY.

        Args:
            auth_result: Validated authentication result.
            required_role: Role required for the operation.

        Returns:
            True if authorized, False otherwise.
        """
        if not auth_result.authenticated:
            return False
        return required_role in auth_result.roles
