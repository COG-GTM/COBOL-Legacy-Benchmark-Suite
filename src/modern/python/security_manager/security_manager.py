"""
Security Manager - Modern Python implementation of COBOL SECMGR.

This module implements the three-phase security model from the original COBOL program:
1. Validation ('V'): Verify user credentials
2. Authorization ('A'): Check resource permissions
3. Audit ('L'): Log access attempts

The implementation preserves the sequential validation -> authorization -> audit pattern
where each phase must complete successfully before proceeding to the next.
"""

import uuid
import logging
from abc import ABC, abstractmethod
from datetime import datetime
from typing import Optional, List
from dataclasses import dataclass
from enum import Enum

from sqlalchemy.orm import Session
from sqlalchemy.exc import SQLAlchemyError

from .models import AuthFile, AuditLog
from .context import SecurityContext, get_current_context
from .exceptions import (
    SecurityException,
    ValidationException,
    AuthorizationException,
    AuditException,
    ContextException
)

logger = logging.getLogger(__name__)


class ResponseCode(Enum):
    """
    Response codes matching COBOL SEC-RESPONSE-CODE values.
    
    Original COBOL values:
    - 0: Success
    - 8: Denied (validation failed, access denied)
    - 12: Error (system error, DB error)
    """
    SUCCESS = 0
    DENIED = 8
    ERROR = 12


@dataclass
class SecurityResponse:
    """
    Response structure matching COBOL SECURITY-REQUEST-AREA output fields.
    
    Maps to:
    - SEC-RESPONSE-CODE: PIC S9(8) COMP
    - SEC-ERROR-INFO: PIC X(80)
    """
    success: bool
    response_code: int
    error_info: Optional[str] = None
    
    def to_dict(self) -> dict:
        return {
            "success": self.success,
            "response_code": self.response_code,
            "error_info": self.error_info
        }
    
    @classmethod
    def ok(cls) -> "SecurityResponse":
        """Create a success response (COBOL: MOVE 0 TO SEC-RESPONSE-CODE)."""
        return cls(success=True, response_code=ResponseCode.SUCCESS.value)
    
    @classmethod
    def denied(cls, error_info: str) -> "SecurityResponse":
        """Create a denied response (COBOL: MOVE 8 TO SEC-RESPONSE-CODE)."""
        return cls(success=False, response_code=ResponseCode.DENIED.value, error_info=error_info)
    
    @classmethod
    def error(cls, error_info: str) -> "SecurityResponse":
        """Create an error response (COBOL: MOVE 12 TO SEC-RESPONSE-CODE)."""
        return cls(success=False, response_code=ResponseCode.ERROR.value, error_info=error_info)


class SecurityHandler(ABC):
    """
    Abstract base class for security handlers in the chain of responsibility.
    
    Implements the chain-of-responsibility pattern to maintain the sequential
    validation -> authorization -> audit flow from the original COBOL.
    """
    
    def __init__(self):
        self._next_handler: Optional["SecurityHandler"] = None
    
    def set_next(self, handler: "SecurityHandler") -> "SecurityHandler":
        """Set the next handler in the chain."""
        self._next_handler = handler
        return handler
    
    @abstractmethod
    def handle(self, context: SecurityContext, session: Session) -> SecurityResponse:
        """Process the security request."""
        pass
    
    def _handle_next(self, context: SecurityContext, session: Session) -> SecurityResponse:
        """Pass to the next handler if available."""
        if self._next_handler:
            return self._next_handler.handle(context, session)
        return SecurityResponse.ok()


class ValidationHandler(SecurityHandler):
    """
    User validation handler.
    
    Maps to COBOL P100-VALIDATE-USER:
    - Gets CICS user ID via EXEC CICS ASSIGN USERID
    - Compares with SEC-USER-ID from request
    - Returns 0 if match, 8 if mismatch, 12 if can't get credentials
    
    In the modern implementation, validation checks that the context
    contains valid user credentials that match the expected user.
    """
    
    def __init__(self, expected_user_id: Optional[str] = None):
        super().__init__()
        self.expected_user_id = expected_user_id
    
    def handle(self, context: SecurityContext, session: Session) -> SecurityResponse:
        """
        Validate user credentials.
        
        COBOL equivalent:
            IF SEC-RESPONSE-CODE = DFHRESP(NORMAL)
               IF SEC-USER-ID = WS-USER-ID
                  MOVE 0 TO SEC-RESPONSE-CODE
               ELSE
                  MOVE 'User validation failed' TO SEC-ERROR-INFO
                  MOVE 8 TO SEC-RESPONSE-CODE
               END-IF
            ELSE
               MOVE 'Unable to obtain user credentials' TO SEC-ERROR-INFO
               MOVE 12 TO SEC-RESPONSE-CODE
            END-IF
        """
        logger.info(f"Validating user: {context.user_id}")
        
        try:
            context.validate()
        except ContextException as e:
            logger.warning(f"Context validation failed: {e.message}")
            return SecurityResponse.error(e.error_info)
        
        if self.expected_user_id and context.user_id != self.expected_user_id:
            logger.warning(f"User validation failed: expected {self.expected_user_id}, got {context.user_id}")
            return SecurityResponse.denied("User validation failed")
        
        logger.info(f"User {context.user_id} validated successfully")
        return self._handle_next(context, session)


class AuthorizationHandler(SecurityHandler):
    """
    Authorization check handler.
    
    Maps to COBOL P200-CHECK-AUTH:
    - Queries AUTHFILE table: SELECT COUNT(*) WHERE USER_ID, RESOURCE, ACCESS_TYPE match
    - Returns 0 if count > 0, 8 if no match (access denied), 12 if SQL error
    """
    
    def __init__(self, resource: str, access_type: str):
        super().__init__()
        self.resource = resource[:8] if len(resource) > 8 else resource
        self.access_type = access_type[:8] if len(access_type) > 8 else access_type
    
    def handle(self, context: SecurityContext, session: Session) -> SecurityResponse:
        """
        Check user authorization for the requested resource.
        
        COBOL equivalent:
            EXEC SQL
                 SELECT COUNT(*)
                 INTO :WS-DB2-AREA
                 FROM AUTHFILE
                 WHERE USER_ID = :SEC-USER-ID
                   AND RESOURCE = :SEC-RESOURCE-NAME
                   AND ACCESS_TYPE = :SEC-ACCESS-TYPE
            END-EXEC
            
            EVALUATE SQLCODE
                WHEN 0
                     IF WS-DB2-AREA > 0
                        MOVE 0 TO SEC-RESPONSE-CODE
                     ELSE
                        MOVE 'Access denied' TO SEC-ERROR-INFO
                        MOVE 8 TO SEC-RESPONSE-CODE
                     END-IF
                WHEN OTHER
                     MOVE 'Authorization check failed' TO SEC-ERROR-INFO
                     MOVE 12 TO SEC-RESPONSE-CODE
            END-EVALUATE
        """
        logger.info(f"Checking authorization: user={context.user_id}, resource={self.resource}, access_type={self.access_type}")
        
        context.program_name = self.resource
        context.access_type = self.access_type
        
        try:
            count = session.query(AuthFile).filter(
                AuthFile.user_id == context.user_id,
                AuthFile.resource == self.resource,
                AuthFile.access_type == self.access_type
            ).count()
            
            if count > 0:
                logger.info(f"Authorization granted for user {context.user_id}")
                return self._handle_next(context, session)
            else:
                logger.warning(f"Access denied for user {context.user_id} to {self.resource}/{self.access_type}")
                return SecurityResponse.denied("Access denied")
                
        except SQLAlchemyError as e:
            logger.error(f"Authorization check failed: {e}")
            return SecurityResponse.error("Authorization check failed")


class AuditHandler(SecurityHandler):
    """
    Audit logging handler.
    
    Maps to COBOL P300-LOG-ACCESS:
    - Gets timestamp, user ID, terminal ID, transaction ID from CICS
    - Inserts into AUDITLOG table
    - Returns 0 on success, 12 on SQL error
    
    Note: The original COBOL uses synchronous logging that blocks the transaction.
    This implementation supports both sync and async logging modes.
    """
    
    def __init__(self, async_mode: bool = False):
        super().__init__()
        self.async_mode = async_mode
    
    def handle(self, context: SecurityContext, session: Session) -> SecurityResponse:
        """
        Log access attempt to audit trail.
        
        COBOL equivalent:
            MOVE FUNCTION CURRENT-DATE TO WS-TIMESTAMP
            
            EXEC CICS ASSIGN
                      USERID(WS-USER-ID)
                      TERMID(WS-TERMINAL-ID)
                      TRANSID(WS-TRANSACTION-ID)
            END-EXEC
            
            EXEC SQL
                 INSERT INTO AUDITLOG
                 (TIMESTAMP, USER_ID, TERMINAL_ID, 
                  TRANS_ID, PROGRAM, ACCESS_TYPE)
                 VALUES
                 (:WS-TIMESTAMP, :WS-USER-ID, :WS-TERMINAL-ID,
                  :WS-TRANSACTION-ID, :WS-PROGRAM-NAME, 
                  :WS-ACCESS-TYPE)
            END-EXEC
            
            IF SQLCODE = 0
               MOVE 0 TO SEC-RESPONSE-CODE
            ELSE
               MOVE 'Audit logging failed' TO SEC-ERROR-INFO
               MOVE 12 TO SEC-RESPONSE-CODE
            END-IF
        """
        logger.info(f"Logging access: user={context.user_id}, program={context.program_name}")
        
        if self.async_mode:
            return self._log_async(context, session)
        else:
            return self._log_sync(context, session)
    
    def _log_sync(self, context: SecurityContext, session: Session) -> SecurityResponse:
        """Synchronous audit logging (matches original COBOL behavior)."""
        try:
            audit_entry = AuditLog(
                id=str(uuid.uuid4()),
                timestamp=context.timestamp or datetime.utcnow(),
                user_id=context.user_id,
                terminal_id=context.terminal_id,
                trans_id=context.transaction_id,
                program=context.program_name or "",
                access_type=context.access_type or ""
            )
            
            session.add(audit_entry)
            session.commit()
            
            logger.info(f"Audit log entry created: {audit_entry.id}")
            return self._handle_next(context, session)
            
        except SQLAlchemyError as e:
            logger.error(f"Audit logging failed: {e}")
            session.rollback()
            return SecurityResponse.error("Audit logging failed")
    
    def _log_async(self, context: SecurityContext, session: Session) -> SecurityResponse:
        """
        Asynchronous audit logging.
        
        Trade-off: Async logging may lose audit records if the system crashes
        before persistence, but it avoids blocking the transaction.
        
        In production, this would publish to a message queue (e.g., Kafka, RabbitMQ)
        for reliable async processing.
        """
        import threading
        
        def _async_log():
            try:
                audit_entry = AuditLog(
                    id=str(uuid.uuid4()),
                    timestamp=context.timestamp or datetime.utcnow(),
                    user_id=context.user_id,
                    terminal_id=context.terminal_id,
                    trans_id=context.transaction_id,
                    program=context.program_name or "",
                    access_type=context.access_type or ""
                )
                session.add(audit_entry)
                session.commit()
                logger.info(f"Async audit log entry created: {audit_entry.id}")
            except SQLAlchemyError as e:
                logger.error(f"Async audit logging failed: {e}")
        
        thread = threading.Thread(target=_async_log, daemon=True)
        thread.start()
        
        logger.info("Audit logging queued asynchronously")
        return self._handle_next(context, session)


class SecurityManager:
    """
    Main Security Manager class.
    
    Provides the primary interface for security operations, matching the
    COBOL SECMGR program's functionality:
    
    1. validate_user(): Maps to P100-VALIDATE-USER
    2. authorize_access(): Maps to P200-CHECK-AUTH
    3. audit_access(): Maps to P300-LOG-ACCESS
    4. check_security(): Performs all three phases in sequence
    
    Usage:
        manager = SecurityManager(session_factory)
        
        # Individual operations
        response = manager.validate_user(context)
        response = manager.authorize_access(context, "INQONLN", "READ")
        response = manager.audit_access(context)
        
        # Full security check (validation -> authorization -> audit)
        response = manager.check_security(context, "INQONLN", "READ")
    """
    
    def __init__(self, session_factory, async_audit: bool = False):
        """
        Initialize the Security Manager.
        
        Args:
            session_factory: SQLAlchemy session factory
            async_audit: If True, use asynchronous audit logging
        """
        self.session_factory = session_factory
        self.async_audit = async_audit
    
    def validate_user(
        self,
        context: SecurityContext,
        expected_user_id: Optional[str] = None
    ) -> SecurityResponse:
        """
        Validate user credentials.
        
        Maps to COBOL SEC-REQUEST-TYPE = 'V' (P100-VALIDATE-USER).
        
        Args:
            context: Security context with user information
            expected_user_id: Optional expected user ID for comparison
            
        Returns:
            SecurityResponse with response_code:
            - 0: Validation successful
            - 8: User validation failed (mismatch)
            - 12: Unable to obtain user credentials
        """
        with self.session_factory() as session:
            handler = ValidationHandler(expected_user_id)
            return handler.handle(context, session)
    
    def authorize_access(
        self,
        context: SecurityContext,
        resource: str,
        access_type: str
    ) -> SecurityResponse:
        """
        Check user authorization for a resource.
        
        Maps to COBOL SEC-REQUEST-TYPE = 'A' (P200-CHECK-AUTH).
        
        Args:
            context: Security context with user information
            resource: Resource name (e.g., 'INQONLN')
            access_type: Access type (e.g., 'READ', 'WRITE')
            
        Returns:
            SecurityResponse with response_code:
            - 0: Authorization granted
            - 8: Access denied
            - 12: Authorization check failed (DB error)
        """
        with self.session_factory() as session:
            handler = AuthorizationHandler(resource, access_type)
            return handler.handle(context, session)
    
    def audit_access(self, context: SecurityContext) -> SecurityResponse:
        """
        Log access attempt to audit trail.
        
        Maps to COBOL SEC-REQUEST-TYPE = 'L' (P300-LOG-ACCESS).
        
        Args:
            context: Security context with access information
            
        Returns:
            SecurityResponse with response_code:
            - 0: Audit logging successful
            - 12: Audit logging failed
        """
        with self.session_factory() as session:
            handler = AuditHandler(async_mode=self.async_audit)
            return handler.handle(context, session)
    
    def check_security(
        self,
        context: SecurityContext,
        resource: str,
        access_type: str,
        expected_user_id: Optional[str] = None
    ) -> SecurityResponse:
        """
        Perform full security check: validation -> authorization -> audit.
        
        Maps to COBOL P050-SECURITY-CHECK in INQONLN.cbl which calls SECMGR
        three times with SEC-REQUEST-TYPE values 'V', 'A', and 'L'.
        
        The chain-of-responsibility pattern ensures each phase must complete
        successfully before proceeding to the next. Failures at any phase
        terminate the chain and return the error response.
        
        Args:
            context: Security context with user information
            resource: Resource name (e.g., 'INQONLN')
            access_type: Access type (e.g., 'READ', 'WRITE')
            expected_user_id: Optional expected user ID for validation
            
        Returns:
            SecurityResponse with response_code:
            - 0: All phases successful
            - 8: Validation or authorization denied
            - 12: System error in any phase
        """
        with self.session_factory() as session:
            validation_handler = ValidationHandler(expected_user_id)
            authorization_handler = AuthorizationHandler(resource, access_type)
            audit_handler = AuditHandler(async_mode=self.async_audit)
            
            validation_handler.set_next(authorization_handler)
            authorization_handler.set_next(audit_handler)
            
            return validation_handler.handle(context, session)
    
    def add_authorization(
        self,
        user_id: str,
        resource: str,
        access_type: str
    ) -> bool:
        """
        Add an authorization rule to the AUTHFILE table.
        
        This is an administrative function not present in the original COBOL
        but useful for managing authorization rules.
        
        Args:
            user_id: User identifier
            resource: Resource name
            access_type: Access type
            
        Returns:
            True if successful, False otherwise
        """
        with self.session_factory() as session:
            try:
                auth_entry = AuthFile(
                    user_id=user_id[:8],
                    resource=resource[:8],
                    access_type=access_type[:8]
                )
                session.merge(auth_entry)
                session.commit()
                logger.info(f"Authorization added: {user_id}/{resource}/{access_type}")
                return True
            except SQLAlchemyError as e:
                logger.error(f"Failed to add authorization: {e}")
                session.rollback()
                return False
    
    def remove_authorization(
        self,
        user_id: str,
        resource: str,
        access_type: str
    ) -> bool:
        """
        Remove an authorization rule from the AUTHFILE table.
        
        Args:
            user_id: User identifier
            resource: Resource name
            access_type: Access type
            
        Returns:
            True if successful, False otherwise
        """
        with self.session_factory() as session:
            try:
                session.query(AuthFile).filter(
                    AuthFile.user_id == user_id[:8],
                    AuthFile.resource == resource[:8],
                    AuthFile.access_type == access_type[:8]
                ).delete()
                session.commit()
                logger.info(f"Authorization removed: {user_id}/{resource}/{access_type}")
                return True
            except SQLAlchemyError as e:
                logger.error(f"Failed to remove authorization: {e}")
                session.rollback()
                return False
    
    def get_audit_log(
        self,
        user_id: Optional[str] = None,
        start_time: Optional[datetime] = None,
        end_time: Optional[datetime] = None,
        limit: int = 100
    ) -> List[dict]:
        """
        Retrieve audit log entries.
        
        Args:
            user_id: Filter by user ID (optional)
            start_time: Filter by start time (optional)
            end_time: Filter by end time (optional)
            limit: Maximum number of entries to return
            
        Returns:
            List of audit log entries as dictionaries
        """
        with self.session_factory() as session:
            try:
                query = session.query(AuditLog)
                
                if user_id:
                    query = query.filter(AuditLog.user_id == user_id[:8])
                if start_time:
                    query = query.filter(AuditLog.timestamp >= start_time)
                if end_time:
                    query = query.filter(AuditLog.timestamp <= end_time)
                
                query = query.order_by(AuditLog.timestamp.desc()).limit(limit)
                
                return [
                    {
                        "id": entry.id,
                        "timestamp": entry.timestamp.isoformat(),
                        "user_id": entry.user_id,
                        "terminal_id": entry.terminal_id,
                        "trans_id": entry.trans_id,
                        "program": entry.program,
                        "access_type": entry.access_type
                    }
                    for entry in query.all()
                ]
            except SQLAlchemyError as e:
                logger.error(f"Failed to retrieve audit log: {e}")
                return []
