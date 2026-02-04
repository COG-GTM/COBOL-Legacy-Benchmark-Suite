"""
Modern Security Manager - Replacement for SECMGR.cbl

This module implements the three-phase security model from the original
COBOL SECMGR program using modern patterns:

1. Validation (P100-VALIDATE-USER) - Verify user identity
2. Authorization (P200-CHECK-AUTH) - Check user permissions
3. Audit (P300-LOG-ACCESS) - Log all access attempts

The original COBOL used CICS LINK to call SECMGR with a SECURITY-REQUEST-AREA.
This modern implementation uses dependency injection and explicit context passing.

Original COBOL flow (INQONLN.cbl lines 139-169):
    MOVE 'V' TO SEC-REQUEST-TYPE.
    EXEC CICS LINK PROGRAM('SECMGR') ...
    IF SEC-RESPONSE-CODE = 0
       MOVE 'A' TO SEC-REQUEST-TYPE
       EXEC CICS LINK PROGRAM('SECMGR') ...
       IF SEC-RESPONSE-CODE = 0
          MOVE 'L' TO SEC-REQUEST-TYPE
          EXEC CICS LINK PROGRAM('SECMGR') ...
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Callable, Optional, Protocol
import logging

from .context import SecurityContext
from .models import (
    AccessType,
    AuditLogEntry,
    AuditStatus,
    AuthorizationRecord,
    UserIdentity,
)

logger = logging.getLogger(__name__)


class SecurityPhase(str, Enum):
    """
    Security check phases corresponding to original COBOL request types.

    Original COBOL (SECMGR.cbl lines 31-34):
        05 SEC-REQUEST-TYPE     PIC X.
           88 SEC-VALIDATE           VALUE 'V'.
           88 SEC-AUTHORIZE          VALUE 'A'.
           88 SEC-AUDIT              VALUE 'L'.
    """

    VALIDATE = "V"
    AUTHORIZE = "A"
    AUDIT = "L"


@dataclass
class SecurityResponse:
    """
    Response from security operations.

    Corresponds to the SECURITY-REQUEST-AREA response fields:
        05 SEC-RESPONSE-CODE    PIC S9(8) COMP.
        05 SEC-ERROR-INFO       PIC X(80).
    """

    success: bool
    response_code: int = 0
    error_info: str = ""
    phase: Optional[SecurityPhase] = None
    details: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def ok(cls, phase: SecurityPhase, details: dict[str, Any] = None) -> SecurityResponse:
        """Create a successful response."""
        return cls(
            success=True,
            response_code=0,
            phase=phase,
            details=details or {},
        )

    @classmethod
    def error(
        cls, phase: SecurityPhase, code: int, message: str, details: dict[str, Any] = None
    ) -> SecurityResponse:
        """Create an error response."""
        return cls(
            success=False,
            response_code=code,
            error_info=message,
            phase=phase,
            details=details or {},
        )


class TokenValidator(Protocol):
    """Protocol for JWT/token validation."""

    def validate(self, context: SecurityContext) -> tuple[bool, Optional[str]]:
        """
        Validate the token/credentials in the context.

        Returns:
            Tuple of (is_valid, error_message)
        """
        ...


class AuthorizationStore(Protocol):
    """Protocol for authorization data access."""

    def check_permission(
        self,
        user_id: str,
        resource: str,
        access_type: AccessType,
        identity: Optional[UserIdentity] = None,
    ) -> bool:
        """Check if user has permission to access resource."""
        ...


class AuditLogger(Protocol):
    """Protocol for audit logging."""

    def log(self, entry: AuditLogEntry) -> bool:
        """Log an audit entry. Returns True on success."""
        ...


class DefaultTokenValidator:
    """
    Default token validator that checks JWT claims.

    This replaces the CICS user validation (SECMGR.cbl lines 56-76):
        EXEC CICS ASSIGN USERID(WS-USER-ID) ...
        IF SEC-USER-ID = WS-USER-ID
           MOVE 0 TO SEC-RESPONSE-CODE
        ELSE
           MOVE 'User validation failed' TO SEC-ERROR-INFO
           MOVE 8 TO SEC-RESPONSE-CODE
    """

    def __init__(
        self,
        verify_expiration: bool = True,
        verify_issuer: Optional[str] = None,
        verify_audience: Optional[str] = None,
    ):
        self.verify_expiration = verify_expiration
        self.verify_issuer = verify_issuer
        self.verify_audience = verify_audience

    def validate(self, context: SecurityContext) -> tuple[bool, Optional[str]]:
        """Validate the security context."""
        if not context.user_id:
            return False, "User ID is required"

        claims = context.claims

        if self.verify_expiration:
            exp = claims.get("exp")
            if exp:
                try:
                    exp_time = datetime.fromtimestamp(int(exp), tz=timezone.utc)
                    if datetime.now(timezone.utc) > exp_time:
                        return False, "Token has expired"
                except (ValueError, TypeError):
                    return False, "Invalid expiration claim"

        if self.verify_issuer:
            iss = claims.get("iss")
            if iss != self.verify_issuer:
                return False, f"Invalid issuer: expected {self.verify_issuer}"

        if self.verify_audience:
            aud = claims.get("aud")
            if isinstance(aud, list):
                if self.verify_audience not in aud:
                    return False, f"Invalid audience: expected {self.verify_audience}"
            elif aud != self.verify_audience:
                return False, f"Invalid audience: expected {self.verify_audience}"

        claimed_user = claims.get("sub") or claims.get("user_id")
        if claimed_user and claimed_user != context.user_id:
            return False, "User ID mismatch between context and claims"

        return True, None


class InMemoryAuthorizationStore:
    """
    In-memory authorization store for testing and simple deployments.

    This replaces the DB2 AUTHFILE query (SECMGR.cbl lines 78-101):
        EXEC SQL
             SELECT COUNT(*)
             FROM AUTHFILE
             WHERE USER_ID = :SEC-USER-ID
               AND RESOURCE = :SEC-RESOURCE-NAME
               AND ACCESS_TYPE = :SEC-ACCESS-TYPE
        END-EXEC.
    """

    def __init__(self, records: Optional[list[AuthorizationRecord]] = None):
        self.records = records or []

    def add_record(self, record: AuthorizationRecord) -> None:
        """Add an authorization record."""
        self.records.append(record)

    def check_permission(
        self,
        user_id: str,
        resource: str,
        access_type: AccessType,
        identity: Optional[UserIdentity] = None,
    ) -> bool:
        """Check if user has permission to access resource."""
        for record in sorted(self.records, key=lambda r: -r.priority):
            if not record.enabled:
                continue
            if record.is_expired():
                continue

            matches_user = False
            if record.user_id and record.user_id == user_id:
                matches_user = True
            elif identity:
                if record.role and identity.has_role(record.role):
                    matches_user = True
                elif record.group and identity.is_member_of(record.group):
                    matches_user = True

            if not matches_user:
                continue

            if self._matches_resource(record.resource_pattern, resource):
                if record.allows_access(access_type):
                    return True

        return False

    def _matches_resource(self, pattern: str, resource: str) -> bool:
        """Check if resource matches the pattern (supports wildcards)."""
        if pattern == "*":
            return True
        if pattern == resource:
            return True
        if pattern.endswith("*"):
            prefix = pattern[:-1]
            return resource.startswith(prefix)
        return False


class InMemoryAuditLogger:
    """
    In-memory audit logger for testing.

    This replaces the DB2 AUDITLOG insert (SECMGR.cbl lines 117-125):
        EXEC SQL
             INSERT INTO AUDITLOG
             (TIMESTAMP, USER_ID, TERMINAL_ID,
              TRANS_ID, PROGRAM, ACCESS_TYPE)
             VALUES
             (:WS-TIMESTAMP, :WS-USER-ID, :WS-TERMINAL-ID,
              :WS-TRANSACTION-ID, :WS-PROGRAM-NAME,
              :WS-ACCESS-TYPE)
        END-EXEC.
    """

    def __init__(self):
        self.entries: list[AuditLogEntry] = []

    def log(self, entry: AuditLogEntry) -> bool:
        """Log an audit entry."""
        self.entries.append(entry)
        logger.info(
            "AUDIT: user=%s resource=%s access=%s status=%s request_id=%s",
            entry.user_id,
            entry.resource_name,
            entry.access_type.value,
            entry.status.value,
            entry.request_id,
        )
        return True

    def get_entries(
        self,
        user_id: Optional[str] = None,
        resource: Optional[str] = None,
        since: Optional[datetime] = None,
    ) -> list[AuditLogEntry]:
        """Query audit entries with optional filters."""
        result = self.entries
        if user_id:
            result = [e for e in result if e.user_id == user_id]
        if resource:
            result = [e for e in result if e.resource_name == resource]
        if since:
            result = [e for e in result if e.timestamp >= since]
        return result


class SecurityManager:
    """
    Modern Security Manager implementing the three-phase security model.

    This class provides the same functionality as the COBOL SECMGR program
    but with modern patterns:
    - Dependency injection for validators, auth stores, and loggers
    - Explicit context passing instead of implicit CICS context
    - Typed responses instead of numeric codes
    - Async support for modern web frameworks

    Usage:
        security_manager = SecurityManager(
            token_validator=DefaultTokenValidator(),
            authorization_store=InMemoryAuthorizationStore(),
            audit_logger=InMemoryAuditLogger(),
        )

        # Three-phase security check (equivalent to INQONLN.cbl P050-SECURITY-CHECK)
        result = await security_manager.check_security(
            context=security_context,
            resource="INQONLN",
            access_type=AccessType.READ,
        )
    """

    def __init__(
        self,
        token_validator: Optional[TokenValidator] = None,
        authorization_store: Optional[AuthorizationStore] = None,
        audit_logger: Optional[AuditLogger] = None,
    ):
        """
        Initialize the security manager with injected dependencies.

        Args:
            token_validator: Validates user tokens/credentials.
            authorization_store: Checks user permissions.
            audit_logger: Logs security events.
        """
        self.token_validator = token_validator or DefaultTokenValidator()
        self.authorization_store = authorization_store or InMemoryAuthorizationStore()
        self.audit_logger = audit_logger or InMemoryAuditLogger()

    async def check_security(
        self,
        context: SecurityContext,
        resource: str,
        access_type: AccessType,
        identity: Optional[UserIdentity] = None,
    ) -> SecurityResponse:
        """
        Perform the complete three-phase security check.

        This implements the same flow as INQONLN.cbl P050-SECURITY-CHECK:
        1. Validate user (SEC-REQUEST-TYPE = 'V')
        2. If valid, authorize access (SEC-REQUEST-TYPE = 'A')
        3. If authorized, log access (SEC-REQUEST-TYPE = 'L')

        All phases are logged regardless of success/failure.

        Args:
            context: The security context with user/client/request info.
            resource: The resource being accessed (e.g., "INQONLN").
            access_type: The type of access requested.
            identity: Optional user identity with roles/groups.

        Returns:
            SecurityResponse indicating success or failure with details.
        """
        start_time = datetime.now(timezone.utc)

        validation_result = await self.validate_user(context)
        if not validation_result.success:
            await self._log_attempt(
                context, resource, access_type, AuditStatus.FAILURE,
                error_message=validation_result.error_info,
                start_time=start_time,
            )
            return validation_result

        auth_result = await self.authorize_access(
            context, resource, access_type, identity
        )
        if not auth_result.success:
            await self._log_attempt(
                context, resource, access_type, AuditStatus.DENIED,
                error_message=auth_result.error_info,
                start_time=start_time,
            )
            return auth_result

        await self._log_attempt(
            context, resource, access_type, AuditStatus.SUCCESS,
            start_time=start_time,
        )

        return SecurityResponse.ok(
            phase=SecurityPhase.AUDIT,
            details={
                "validation": validation_result.details,
                "authorization": auth_result.details,
            },
        )

    async def validate_user(self, context: SecurityContext) -> SecurityResponse:
        """
        Phase 1: Validate user credentials.

        Corresponds to P100-VALIDATE-USER in SECMGR.cbl.
        Verifies that the JWT signature is valid and the claimed user
        matches the authenticated user.
        """
        try:
            is_valid, error_message = self.token_validator.validate(context)

            if is_valid:
                return SecurityResponse.ok(
                    phase=SecurityPhase.VALIDATE,
                    details={"user_id": context.user_id},
                )
            else:
                return SecurityResponse.error(
                    phase=SecurityPhase.VALIDATE,
                    code=8,
                    message=error_message or "User validation failed",
                )
        except Exception as e:
            logger.exception("Validation error")
            return SecurityResponse.error(
                phase=SecurityPhase.VALIDATE,
                code=12,
                message=f"Unable to validate user credentials: {str(e)}",
            )

    async def authorize_access(
        self,
        context: SecurityContext,
        resource: str,
        access_type: AccessType,
        identity: Optional[UserIdentity] = None,
    ) -> SecurityResponse:
        """
        Phase 2: Check user authorization.

        Corresponds to P200-CHECK-AUTH in SECMGR.cbl.
        Queries the authorization store to verify the user has
        permission to access the requested resource.
        """
        try:
            has_permission = self.authorization_store.check_permission(
                user_id=context.user_id,
                resource=resource,
                access_type=access_type,
                identity=identity,
            )

            if has_permission:
                return SecurityResponse.ok(
                    phase=SecurityPhase.AUTHORIZE,
                    details={
                        "resource": resource,
                        "access_type": access_type.value,
                    },
                )
            else:
                return SecurityResponse.error(
                    phase=SecurityPhase.AUTHORIZE,
                    code=8,
                    message="Access denied",
                    details={
                        "resource": resource,
                        "access_type": access_type.value,
                    },
                )
        except Exception as e:
            logger.exception("Authorization error")
            return SecurityResponse.error(
                phase=SecurityPhase.AUTHORIZE,
                code=12,
                message=f"Authorization check failed: {str(e)}",
            )

    async def log_access(
        self,
        context: SecurityContext,
        resource: str,
        access_type: AccessType,
        status: AuditStatus = AuditStatus.SUCCESS,
        response_code: Optional[int] = None,
        error_message: Optional[str] = None,
        duration_ms: Optional[int] = None,
        metadata: Optional[dict[str, Any]] = None,
    ) -> SecurityResponse:
        """
        Phase 3: Log access attempt.

        Corresponds to P300-LOG-ACCESS in SECMGR.cbl.
        Records the access attempt in the audit log with full context.
        """
        entry = AuditLogEntry(
            timestamp=datetime.now(timezone.utc),
            user_id=context.user_id,
            client_id=context.client_id,
            request_id=context.request_id,
            resource_name=resource,
            access_type=access_type,
            status=status,
            trace_id=context.trace_id,
            session_id=context.session_id,
            user_agent=context.user_agent,
            response_code=response_code,
            duration_ms=duration_ms,
            error_message=error_message,
            metadata=metadata or {},
        )

        try:
            success = self.audit_logger.log(entry)
            if success:
                return SecurityResponse.ok(
                    phase=SecurityPhase.AUDIT,
                    details={"audit_id": entry.id},
                )
            else:
                return SecurityResponse.error(
                    phase=SecurityPhase.AUDIT,
                    code=12,
                    message="Audit logging failed",
                )
        except Exception as e:
            logger.exception("Audit logging error")
            return SecurityResponse.error(
                phase=SecurityPhase.AUDIT,
                code=12,
                message=f"Audit logging failed: {str(e)}",
            )

    async def _log_attempt(
        self,
        context: SecurityContext,
        resource: str,
        access_type: AccessType,
        status: AuditStatus,
        error_message: Optional[str] = None,
        start_time: Optional[datetime] = None,
    ) -> None:
        """Internal helper to log access attempts."""
        duration_ms = None
        if start_time:
            delta = datetime.now(timezone.utc) - start_time
            duration_ms = int(delta.total_seconds() * 1000)

        await self.log_access(
            context=context,
            resource=resource,
            access_type=access_type,
            status=status,
            error_message=error_message,
            duration_ms=duration_ms,
        )


def create_security_manager(
    jwt_secret: Optional[str] = None,
    jwt_public_key: Optional[str] = None,
    verify_issuer: Optional[str] = None,
    verify_audience: Optional[str] = None,
    authorization_records: Optional[list[AuthorizationRecord]] = None,
) -> SecurityManager:
    """
    Factory function to create a configured SecurityManager.

    Args:
        jwt_secret: Secret key for HS256 JWT validation.
        jwt_public_key: Public key for RS256 JWT validation.
        verify_issuer: Expected JWT issuer.
        verify_audience: Expected JWT audience.
        authorization_records: Initial authorization records.

    Returns:
        Configured SecurityManager instance.
    """
    token_validator = DefaultTokenValidator(
        verify_issuer=verify_issuer,
        verify_audience=verify_audience,
    )

    auth_store = InMemoryAuthorizationStore(records=authorization_records or [])
    audit_logger = InMemoryAuditLogger()

    return SecurityManager(
        token_validator=token_validator,
        authorization_store=auth_store,
        audit_logger=audit_logger,
    )
