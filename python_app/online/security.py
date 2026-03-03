"""Security module - replaces SECMGR.cbl.

Provides JWT/OAuth2 authentication and authorization replacing
RACF/CICS security. Maps SECMGR functions: V(alidate), A(uthorize), L(og).

COBOL program flow (EVALUATE LS-SEC-FUNCTION):
- V: Validate user credentials (P100-VALIDATE-USER)
- A: Authorize access to resource (P200-CHECK-AUTH)
- L: Log security event (P300-LOG-ACCESS)
"""

import logging
from datetime import datetime, timedelta, timezone
from typing import Any

from jose import JWTError, jwt
from passlib.context import CryptContext
from pydantic import BaseModel, Field

from python_app.common.config import SecurityConfig

logger = logging.getLogger("portfolio.online.security")

# Password hashing context
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class TokenData(BaseModel):
    """JWT token payload data."""

    user_id: str
    role: str = "USER"
    exp: datetime | None = None


class UserCredentials(BaseModel):
    """User credentials for authentication."""

    user_id: str = Field(max_length=8)
    password: str


class AuthorizationRequest(BaseModel):
    """Authorization check request."""

    user_id: str = Field(max_length=8)
    resource: str = Field(max_length=8)
    access_type: str = Field(max_length=8)


class SecurityManager:
    """Security manager replacing SECMGR.cbl.

    Provides:
    - P100-VALIDATE-USER: Credential validation with attempt tracking
    - P200-CHECK-AUTH: Resource-level authorization
    - P300-LOG-ACCESS: Security event logging
    """

    MAX_ATTEMPTS = 3  # WS-MAX-ATTEMPTS from SECMGR.cbl

    def __init__(self, config: SecurityConfig | None = None) -> None:
        self.config = config or SecurityConfig()
        self.login_attempts: dict[str, int] = {}
        self.access_log: list[dict[str, Any]] = []
        # In-memory user store (would be DB-backed in production)
        self.users: dict[str, dict[str, Any]] = {}
        # In-memory authorization rules (replaces AUTHFILE DB2 table)
        self.auth_rules: dict[str, set[str]] = {}

    def register_user(
        self,
        user_id: str,
        password: str,
        role: str = "USER",
    ) -> None:
        """Register a user (setup helper, not in original COBOL)."""
        self.users[user_id] = {
            "password_hash": pwd_context.hash(password),
            "role": role,
            "status": "ACTIVE",
        }

    def add_auth_rule(self, user_id: str, resource: str, access_type: str) -> None:
        """Add authorization rule (replaces AUTHFILE INSERT)."""
        key = f"{user_id}:{resource}"
        if key not in self.auth_rules:
            self.auth_rules[key] = set()
        self.auth_rules[key].add(access_type)

    def validate_user(self, user_id: str, password: str) -> dict[str, Any]:
        """Validate user credentials - replaces P100-VALIDATE-USER.

        COBOL: Checks against security file, tracks login attempts,
        locks after WS-MAX-ATTEMPTS (3) failures.
        """
        # Check attempt count
        attempts = self.login_attempts.get(user_id, 0)
        if attempts >= self.MAX_ATTEMPTS:
            self.log_access(user_id, "LOGIN", "LOCKED", "Max attempts exceeded")
            return {
                "valid": False,
                "reason": "Account locked - max attempts exceeded",
                "attempts": attempts,
            }

        user = self.users.get(user_id)
        if user is None:
            self.login_attempts[user_id] = attempts + 1
            self.log_access(user_id, "LOGIN", "FAIL", "User not found")
            return {"valid": False, "reason": "Invalid credentials", "attempts": attempts + 1}

        if user["status"] != "ACTIVE":
            self.log_access(user_id, "LOGIN", "FAIL", f"Account status: {user['status']}")
            return {"valid": False, "reason": "Account not active", "attempts": attempts}

        if not pwd_context.verify(password, user["password_hash"]):
            self.login_attempts[user_id] = attempts + 1
            self.log_access(user_id, "LOGIN", "FAIL", "Invalid password")
            return {"valid": False, "reason": "Invalid credentials", "attempts": attempts + 1}

        # Successful login - reset attempts
        self.login_attempts[user_id] = 0
        self.log_access(user_id, "LOGIN", "OK", "Login successful")

        # Generate JWT token
        token = self.create_token(user_id, user.get("role", "USER"))
        return {"valid": True, "token": token, "role": user.get("role", "USER")}

    def check_auth(self, user_id: str, resource: str, access_type: str) -> dict[str, Any]:
        """Check authorization - replaces P200-CHECK-AUTH.

        COBOL: SELECT FROM AUTHFILE WHERE USER_ID = :user AND
               RESOURCE = :resource AND ACCESS_TYPE = :access.
        """
        key = f"{user_id}:{resource}"
        allowed_types = self.auth_rules.get(key, set())

        authorized = access_type in allowed_types or "ALL" in allowed_types

        self.log_access(
            user_id,
            f"AUTH:{resource}",
            "OK" if authorized else "DENY",
            f"Access type: {access_type}",
        )

        return {
            "authorized": authorized,
            "user_id": user_id,
            "resource": resource,
            "access_type": access_type,
        }

    def log_access(
        self,
        user_id: str,
        event_type: str,
        status: str,
        message: str = "",
    ) -> None:
        """Log security event - replaces P300-LOG-ACCESS.

        COBOL: INSERT INTO AUDITLOG (AUDIT-TIMESTAMP, SEC-USER-ID, ...).
        """
        entry = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "user_id": user_id,
            "event_type": event_type,
            "status": status,
            "message": message,
        }
        self.access_log.append(entry)
        logger.info(
            "SECURITY: user=%s event=%s status=%s %s",
            user_id, event_type, status, message,
        )

    def create_token(self, user_id: str, role: str = "USER") -> str:
        """Create JWT token (replaces CICS session management)."""
        expire = datetime.now(timezone.utc) + timedelta(minutes=self.config.jwt_expiration_minutes)
        payload = {
            "sub": user_id,
            "role": role,
            "exp": expire,
        }
        return jwt.encode(payload, self.config.jwt_secret_key, algorithm=self.config.jwt_algorithm)

    def verify_token(self, token: str) -> TokenData | None:
        """Verify JWT token and extract payload."""
        try:
            payload = jwt.decode(
                token,
                self.config.jwt_secret_key,
                algorithms=[self.config.jwt_algorithm],
            )
            return TokenData(
                user_id=payload.get("sub", ""),
                role=payload.get("role", "USER"),
            )
        except JWTError as exc:
            logger.warning("Token verification failed: %s", exc)
            return None

    def get_access_log(self, user_id: str | None = None, limit: int = 100) -> list[dict[str, Any]]:
        """Get access log entries."""
        entries = self.access_log
        if user_id:
            entries = [e for e in entries if e["user_id"] == user_id]
        return entries[-limit:]
