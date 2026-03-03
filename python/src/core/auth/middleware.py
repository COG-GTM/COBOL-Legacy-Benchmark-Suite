"""JWT/OAuth2 authentication middleware replacing COBOL SECMGR.

Implements the three SECMGR request types as middleware operations:
  SEC-VALIDATE ('V') -> authenticate() - Verify user credentials
  SEC-AUTHORIZE ('A') -> authorize() - Check resource access
  SEC-AUDIT ('L')     -> _audit_event() - Log security events

Response codes mirror SECMGR:
  0  -> Success (SEC-RESPONSE-CODE = 0)
  8  -> Auth failure (validation/authorization denied)
  12 -> System failure (unable to verify)
"""

import logging
from datetime import datetime, timezone
from typing import Optional

import jwt

from python.src.core.auth.models import (
    AccessType,
    AuditLogEntry,
    AuditLogStore,
    InMemoryAuditLogStore,
    User,
    UserStore,
)
from python.src.core.auth.utils import TokenManager, verify_password
from python.src.core.error_handling.exceptions import SecurityError

logger = logging.getLogger("clbs.auth.middleware")


class AuthResult:
    """Authentication/authorization result mirroring SECMGR response.

    Attributes:
        success: Whether the operation succeeded.
        response_code: SECMGR-style response code (0, 8, or 12).
        user: Authenticated user (if successful).
        error_info: Error message (if failed, maps to SEC-ERROR-INFO).
        token: JWT token (if login succeeded).
        refresh_token: JWT refresh token (if login succeeded).
    """

    def __init__(
        self,
        success: bool,
        response_code: int = 0,
        user: Optional[User] = None,
        error_info: str = "",
        token: str = "",
        refresh_token: str = "",
    ) -> None:
        self.success = success
        self.response_code = response_code
        self.user = user
        self.error_info = error_info
        self.token = token
        self.refresh_token = refresh_token


class AuthMiddleware:
    """Authentication and authorization middleware replacing SECMGR.

    Provides the three security operations from SECMGR:
    1. User validation / login (P100-VALIDATE-USER)
    2. Resource authorization (P200-CHECK-AUTH)
    3. Audit logging (P300-LOG-ACCESS)

    Attributes:
        token_manager: JWT token manager for credential handling.
        user_store: Persistent store for user records.
        audit_store: Persistent store for audit log entries.
    """

    def __init__(
        self,
        token_manager: TokenManager,
        user_store: UserStore,
        audit_store: Optional[AuditLogStore] = None,
    ) -> None:
        """Initialize auth middleware.

        Args:
            token_manager: JWT token manager instance.
            user_store: User persistence backend.
            audit_store: Audit log persistence backend.
                Defaults to in-memory store.
        """
        self._token_manager = token_manager
        self._user_store = user_store
        self._audit_store = audit_store or InMemoryAuditLogStore()

    def authenticate(
        self,
        username: str,
        password: str,
        terminal_id: str = "",
    ) -> AuthResult:
        """P100: Validate user credentials and issue tokens.

        Mirrors SECMGR P100-VALIDATE-USER:
        - Verifies user exists and credentials match
        - Returns tokens on success
        - Returns response code 8 on auth failure
        - Returns response code 12 on system failure

        Args:
            username: Login username.
            password: Plaintext password.
            terminal_id: Client/terminal identifier (WS-TERMINAL-ID).

        Returns:
            AuthResult with tokens on success, error info on failure.
        """
        try:
            user = self._user_store.get_user_by_username(username)

            if user is None:
                self._audit_event(
                    user_id=username,
                    terminal_id=terminal_id,
                    event_type="LOGIN_FAILED",
                    resource="AUTH",
                    details="User not found",
                    success=False,
                )
                return AuthResult(
                    success=False,
                    response_code=8,
                    error_info="User validation failed",
                )

            if not user.is_active:
                self._audit_event(
                    user_id=user.user_id,
                    terminal_id=terminal_id,
                    event_type="LOGIN_FAILED",
                    resource="AUTH",
                    details="Account disabled",
                    success=False,
                )
                return AuthResult(
                    success=False,
                    response_code=8,
                    error_info="User validation failed",
                )

            if not verify_password(password, user.hashed_password):
                self._audit_event(
                    user_id=user.user_id,
                    terminal_id=terminal_id,
                    event_type="LOGIN_FAILED",
                    resource="AUTH",
                    details="Invalid password",
                    success=False,
                )
                return AuthResult(
                    success=False,
                    response_code=8,
                    error_info="User validation failed",
                )

            # Generate tokens
            role_names = [role.name for role in user.roles]
            access_token = self._token_manager.create_access_token(
                user_id=user.user_id,
                username=user.username,
                roles=role_names,
            )
            refresh_token = self._token_manager.create_refresh_token(
                user_id=user.user_id,
            )

            self._audit_event(
                user_id=user.user_id,
                terminal_id=terminal_id,
                event_type="LOGIN_SUCCESS",
                resource="AUTH",
                details="User authenticated successfully",
                success=True,
            )

            logger.info(
                "User authenticated: %s",
                user.user_id,
                extra={"user_id": user.user_id, "terminal_id": terminal_id},
            )

            return AuthResult(
                success=True,
                response_code=0,
                user=user,
                token=access_token,
                refresh_token=refresh_token,
            )

        except Exception as exc:
            logger.error(
                "Authentication system error: %s",
                str(exc),
                extra={"username": username},
            )
            return AuthResult(
                success=False,
                response_code=12,
                error_info="Unable to obtain user credentials",
            )

    def authorize(
        self,
        token: str,
        resource: str,
        access_type: AccessType,
        terminal_id: str = "",
    ) -> AuthResult:
        """P200: Check user authorization for a resource.

        Mirrors SECMGR P200-CHECK-AUTH:
        - Validates JWT token
        - Looks up user and checks permissions
        - Returns response code 0 on success
        - Returns response code 8 on access denied
        - Returns response code 12 on system failure

        Args:
            token: JWT access token.
            resource: Resource name to check (SEC-RESOURCE-NAME).
            access_type: Requested access type (SEC-ACCESS-TYPE).
            terminal_id: Client/terminal identifier.

        Returns:
            AuthResult indicating authorization status.
        """
        try:
            payload = self._token_manager.validate_access_token(token)
            user_id = payload["sub"]

            user = self._user_store.get_user_by_id(user_id)
            if user is None:
                return AuthResult(
                    success=False,
                    response_code=8,
                    error_info="Access denied",
                )

            if user.has_permission(resource, access_type):
                self._audit_event(
                    user_id=user_id,
                    terminal_id=terminal_id,
                    event_type="ACCESS_GRANTED",
                    resource=resource,
                    access_type=access_type.value,
                    details=f"Access granted to {resource}",
                    success=True,
                )
                return AuthResult(
                    success=True,
                    response_code=0,
                    user=user,
                )
            else:
                self._audit_event(
                    user_id=user_id,
                    terminal_id=terminal_id,
                    event_type="ACCESS_DENIED",
                    resource=resource,
                    access_type=access_type.value,
                    details=f"Access denied to {resource}",
                    success=False,
                )
                return AuthResult(
                    success=False,
                    response_code=8,
                    error_info="Access denied",
                )

        except jwt.ExpiredSignatureError:
            return AuthResult(
                success=False,
                response_code=8,
                error_info="Token expired",
            )
        except (jwt.InvalidTokenError, ValueError) as exc:
            return AuthResult(
                success=False,
                response_code=8,
                error_info=f"Invalid token: {exc}",
            )
        except Exception as exc:
            logger.error(
                "Authorization check failed: %s",
                str(exc),
            )
            return AuthResult(
                success=False,
                response_code=12,
                error_info="Authorization check failed",
            )

    def validate_token(self, token: str) -> AuthResult:
        """Validate a JWT token and return the associated user.

        Convenience method for middleware integration.

        Args:
            token: JWT access token string.

        Returns:
            AuthResult with the authenticated user on success.
        """
        try:
            payload = self._token_manager.validate_access_token(token)
            user_id = payload["sub"]

            user = self._user_store.get_user_by_id(user_id)
            if user is None:
                return AuthResult(
                    success=False,
                    response_code=8,
                    error_info="User not found",
                )

            return AuthResult(
                success=True,
                response_code=0,
                user=user,
            )

        except jwt.ExpiredSignatureError:
            return AuthResult(
                success=False,
                response_code=8,
                error_info="Token expired",
            )
        except (jwt.InvalidTokenError, ValueError) as exc:
            return AuthResult(
                success=False,
                response_code=8,
                error_info=f"Invalid token: {exc}",
            )

    def refresh_access_token(
        self,
        refresh_token: str,
    ) -> AuthResult:
        """Refresh an access token using a refresh token.

        Args:
            refresh_token: JWT refresh token.

        Returns:
            AuthResult with new access token on success.
        """
        try:
            payload = self._token_manager.validate_refresh_token(refresh_token)
            user_id = payload["sub"]

            user = self._user_store.get_user_by_id(user_id)
            if user is None:
                return AuthResult(
                    success=False,
                    response_code=8,
                    error_info="User not found",
                )

            role_names = [role.name for role in user.roles]
            new_access_token = self._token_manager.create_access_token(
                user_id=user.user_id,
                username=user.username,
                roles=role_names,
            )

            return AuthResult(
                success=True,
                response_code=0,
                user=user,
                token=new_access_token,
            )

        except jwt.ExpiredSignatureError:
            return AuthResult(
                success=False,
                response_code=8,
                error_info="Refresh token expired",
            )
        except (jwt.InvalidTokenError, ValueError) as exc:
            return AuthResult(
                success=False,
                response_code=8,
                error_info=f"Invalid refresh token: {exc}",
            )

    def _audit_event(
        self,
        user_id: str,
        event_type: str,
        resource: str = "",
        access_type: str = "",
        terminal_id: str = "",
        transaction_id: str = "",
        details: str = "",
        success: bool = True,
    ) -> None:
        """P300: Log security event to audit store.

        Mirrors SECMGR P300-LOG-ACCESS:
        - Records timestamp, user, terminal, transaction, resource, access type
        - INSERT INTO AUDITLOG

        Args:
            user_id: User performing the action (WS-USER-ID).
            event_type: Type of security event.
            resource: Resource being accessed (WS-PROGRAM-NAME).
            access_type: Type of access (WS-ACCESS-TYPE).
            terminal_id: Client identifier (WS-TERMINAL-ID).
            transaction_id: Transaction identifier (WS-TRANSACTION-ID).
            details: Additional event details.
            success: Whether the action succeeded.
        """
        entry = AuditLogEntry(
            timestamp=datetime.now(timezone.utc),
            user_id=user_id,
            terminal_id=terminal_id,
            transaction_id=transaction_id,
            resource=resource,
            access_type=access_type,
            event_type=event_type,
            details=details,
            success=success,
        )

        try:
            self._audit_store.save_audit_log(entry)
        except Exception as exc:
            # Matches SECMGR: if audit INSERT fails, log error
            # but don't fail the main operation
            logger.error(
                "Audit logging failed: %s",
                str(exc),
                extra={"user_id": user_id, "event_type": event_type},
            )
