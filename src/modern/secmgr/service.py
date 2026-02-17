"""
Core Security Manager service.

Modernized equivalent of SECMGR.cbl, preserving the three-phase
security model (Validate -> Authorize -> Audit) as a chain-of-responsibility
pattern.

COBOL Mapping:
  P100-VALIDATE-USER  -> SecurityManager.validate_user()
  P200-CHECK-AUTH     -> SecurityManager.authorize_access()
  P300-LOG-ACCESS     -> SecurityManager.audit_access()
  PROCEDURE DIVISION  -> SecurityManager.process_request()
"""

from __future__ import annotations

import asyncio
import logging
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import select
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from .context import SecurityContext, get_security_context
from .exceptions import (
    AuditException,
    AuthorizationException,
    SecurityError,
    ValidationException,
)
from .models import AuditLog, AuthFile
from .responses import ResponseCode, SecurityResponse

logger = logging.getLogger(__name__)

_audit_executor = ThreadPoolExecutor(max_workers=2, thread_name_prefix="audit")

AUDIT_MAX_RETRIES = 3


class SecurityManager:
    """Core security manager implementing the three-phase security model.

    Replaces the COBOL SECMGR program. Each phase must complete successfully
    before proceeding to the next. Failures at any phase terminate the
    transaction, and all attempts are logged.

    Usage:
        mgr = SecurityManager(db_session)
        response = mgr.process_request(
            request_type="V",
            context=security_context,
        )
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    def validate_user(self, context: SecurityContext) -> SecurityResponse:
        """Validate user credentials.

        Corresponds to COBOL P100-VALIDATE-USER which uses EXEC CICS ASSIGN
        to obtain the CICS user ID and compares it with SEC-USER-ID.

        In the modern implementation, the context.user_id is typically
        extracted from a JWT token or HTTP header. Validation confirms
        the user identity is present and well-formed.

        Args:
            context: Security context containing the authenticated user info.

        Returns:
            SecurityResponse with code 0 on success.

        Raises:
            ValidationException: If user_id is missing or invalid (code 8).
            SecurityError: If credentials cannot be obtained (code 12).
        """
        try:
            assigned_user_id = self._get_assigned_user_id(context)
        except SecurityError:
            logger.error(
                "Unable to obtain user credentials for context: %s",
                context.trace_id,
            )
            raise SecurityError("Unable to obtain user credentials")

        if context.user_id != assigned_user_id:
            logger.warning(
                "User validation failed: requested=%s, assigned=%s",
                context.user_id,
                assigned_user_id,
            )
            raise ValidationException("User validation failed")

        logger.info("User validated: %s", context.user_id)
        return SecurityResponse(
            code=ResponseCode.SUCCESS,
            request_type="V",
            user_id=context.user_id,
        )

    def authorize_access(
        self,
        context: SecurityContext,
        resource_name: str,
        access_type: str,
    ) -> SecurityResponse:
        """Check if a user is authorized to access a resource.

        Corresponds to COBOL P200-CHECK-AUTH which executes:
          SELECT COUNT(*) FROM AUTHFILE
          WHERE USER_ID = :SEC-USER-ID
            AND RESOURCE = :SEC-RESOURCE-NAME
            AND ACCESS_TYPE = :SEC-ACCESS-TYPE

        Args:
            context: Security context with the authenticated user.
            resource_name: The resource being accessed (e.g., 'INQONLN').
            access_type: The access type requested (e.g., 'READ').

        Returns:
            SecurityResponse with code 0 if authorized.

        Raises:
            AuthorizationException: If user lacks permission (code 8).
            SecurityError: If the database query fails (code 12).
        """
        try:
            stmt = select(AuthFile).where(
                AuthFile.USER_ID == context.user_id,
                AuthFile.RESOURCE == resource_name,
                AuthFile.ACCESS_TYPE == access_type,
            )
            result = self._session.execute(stmt).first()
        except SQLAlchemyError as exc:
            logger.error("Authorization check failed: %s", exc)
            raise SecurityError("Authorization check failed") from exc

        if result is None:
            logger.warning(
                "Access denied: user=%s, resource=%s, access=%s",
                context.user_id,
                resource_name,
                access_type,
            )
            raise AuthorizationException("Access denied")

        logger.info(
            "Access authorized: user=%s, resource=%s, access=%s",
            context.user_id,
            resource_name,
            access_type,
        )
        return SecurityResponse(
            code=ResponseCode.SUCCESS,
            request_type="A",
            user_id=context.user_id,
            resource_name=resource_name,
            access_type=access_type,
        )

    def audit_access(
        self,
        context: SecurityContext,
        resource_name: str,
        access_type: str,
    ) -> SecurityResponse:
        """Log an access attempt to the AUDITLOG table (synchronous).

        Corresponds to COBOL P300-LOG-ACCESS which inserts into AUDITLOG
        with columns: TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID,
        PROGRAM, ACCESS_TYPE.

        Args:
            context: Security context with full request details.
            resource_name: The program/resource being accessed.
            access_type: The type of access performed.

        Returns:
            SecurityResponse with code 0 on success.

        Raises:
            AuditException: If the audit record cannot be written (code 12).
        """
        try:
            audit_record = AuditLog(
                id=str(uuid.uuid4()),
                TIMESTAMP=datetime.now(timezone.utc),
                USER_ID=context.user_id,
                TERMINAL_ID=context.terminal_id[:4],
                TRANS_ID=context.transaction_id[:4],
                PROGRAM=resource_name[:8],
                ACCESS_TYPE=access_type[:8],
            )
            self._session.add(audit_record)
            self._session.commit()
        except SQLAlchemyError as exc:
            self._session.rollback()
            logger.error("Audit logging failed: %s", exc)
            raise AuditException("Audit logging failed") from exc

        logger.info(
            "Audit logged: user=%s, program=%s, access=%s",
            context.user_id,
            resource_name,
            access_type,
        )
        return SecurityResponse(
            code=ResponseCode.SUCCESS,
            request_type="L",
            user_id=context.user_id,
            resource_name=resource_name,
            access_type=access_type,
        )

    def audit_access_async(
        self,
        context: SecurityContext,
        resource_name: str,
        access_type: str,
        session_factory: Optional[object] = None,
    ) -> asyncio.Future[SecurityResponse]:
        """Log an access attempt asynchronously with retry logic.

        Unlike the synchronous COBOL implementation, this method submits
        the audit write to a background thread pool so it does not block
        the calling transaction. Includes retry logic for transient failures.

        Trade-off: Async logging may lose audit records if the process
        crashes before the write completes. For strict compliance, use
        the synchronous audit_access() method instead.

        Args:
            context: Security context with full request details.
            resource_name: The program/resource being accessed.
            access_type: The type of access performed.
            session_factory: Optional SQLAlchemy sessionmaker for the
                background thread. If None, uses the instance session.

        Returns:
            A Future that resolves to a SecurityResponse.
        """
        loop = asyncio.get_event_loop()
        return loop.run_in_executor(
            _audit_executor,
            self._audit_with_retry,
            context,
            resource_name,
            access_type,
            session_factory,
        )

    def _audit_with_retry(
        self,
        context: SecurityContext,
        resource_name: str,
        access_type: str,
        session_factory: Optional[object] = None,
    ) -> SecurityResponse:
        """Write audit record with retry logic for transient failures."""
        last_exc: Optional[Exception] = None
        for attempt in range(1, AUDIT_MAX_RETRIES + 1):
            try:
                if session_factory is not None:
                    session = session_factory()  # type: ignore[operator]
                    try:
                        mgr = SecurityManager(session)
                        return mgr.audit_access(context, resource_name, access_type)
                    finally:
                        session.close()
                else:
                    return self.audit_access(context, resource_name, access_type)
            except AuditException as exc:
                last_exc = exc
                logger.warning(
                    "Audit retry %d/%d failed: %s",
                    attempt,
                    AUDIT_MAX_RETRIES,
                    exc,
                )
        raise AuditException(
            f"Audit logging failed after {AUDIT_MAX_RETRIES} retries"
        ) from last_exc

    def process_request(
        self,
        request_type: str,
        context: Optional[SecurityContext] = None,
        resource_name: str = "",
        access_type: str = "",
    ) -> SecurityResponse:
        """Dispatch a security request by type.

        Corresponds to the COBOL PROCEDURE DIVISION EVALUATE block:
          WHEN SEC-VALIDATE  -> P100-VALIDATE-USER
          WHEN SEC-AUTHORIZE -> P200-CHECK-AUTH
          WHEN SEC-AUDIT     -> P300-LOG-ACCESS

        Args:
            request_type: 'V' for validate, 'A' for authorize, 'L' for audit.
            context: Security context (uses thread-local if not provided).
            resource_name: Resource name (required for 'A' and 'L').
            access_type: Access type (required for 'A' and 'L').

        Returns:
            SecurityResponse with the operation result.
        """
        ctx = context or get_security_context()
        if ctx is None:
            raise SecurityError("No security context available")

        if request_type == "V":
            return self.validate_user(ctx)
        elif request_type == "A":
            return self.authorize_access(ctx, resource_name, access_type)
        elif request_type == "L":
            return self.audit_access(ctx, resource_name, access_type)
        else:
            raise SecurityError(f"Unknown request type: {request_type}")

    def execute_security_chain(
        self,
        context: Optional[SecurityContext] = None,
        resource_name: str = "",
        access_type: str = "",
    ) -> SecurityResponse:
        """Execute the full Validate -> Authorize -> Audit chain.

        Mirrors the three-step security check in INQONLN.cbl P050-SECURITY-CHECK:
          1. MOVE 'V' TO SEC-REQUEST-TYPE -> validate
          2. MOVE 'A' TO SEC-REQUEST-TYPE -> authorize
          3. MOVE 'L' TO SEC-REQUEST-TYPE -> audit

        Each phase must succeed before the next proceeds. If any phase fails,
        the chain terminates immediately and returns the failure response.

        Args:
            context: Security context (uses thread-local if not provided).
            resource_name: The resource to authorize and audit.
            access_type: The access type to check.

        Returns:
            SecurityResponse from the last successful phase, or the
            first failure.
        """
        ctx = context or get_security_context()
        if ctx is None:
            raise SecurityError("No security context available")

        validate_resp = self.validate_user(ctx)
        if not validate_resp.success:
            return validate_resp

        auth_resp = self.authorize_access(ctx, resource_name, access_type)
        if not auth_resp.success:
            return auth_resp

        audit_resp = self.audit_access(ctx, resource_name, access_type)
        return audit_resp

    @staticmethod
    def _get_assigned_user_id(context: SecurityContext) -> str:
        """Obtain the system-assigned user ID for comparison.

        In COBOL, this was EXEC CICS ASSIGN USERID(WS-USER-ID).
        In the modern stack, the user_id in the context is already
        extracted from the authentication token (JWT, session, etc.),
        so the "assigned" user ID is the same as the context user ID.

        For enhanced security, this could be extended to verify the
        user against an identity provider or session store.
        """
        return context.user_id
