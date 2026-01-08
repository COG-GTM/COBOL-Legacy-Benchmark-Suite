"""
Audit Logger - Migrated from COBOL SECMGR audit functionality.

This module implements security audit logging for tracking
user activities, access attempts, and security events.

Original COBOL Program: src/programs/online/SECMGR.cbl (audit section)
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional, Dict, Any
from enum import Enum

logger = logging.getLogger(__name__)


class AuditEventType(str, Enum):
    """Audit event types - maps to COBOL SEC-AUDIT-TYPE"""
    LOGIN_SUCCESS = 'LOGIN_SUCCESS'
    LOGIN_FAILURE = 'LOGIN_FAILURE'
    LOGOUT = 'LOGOUT'
    SESSION_EXPIRED = 'SESSION_EXPIRED'
    ACCESS_GRANTED = 'ACCESS_GRANTED'
    ACCESS_DENIED = 'ACCESS_DENIED'
    DATA_VIEW = 'DATA_VIEW'
    DATA_MODIFY = 'DATA_MODIFY'
    REPORT_GENERATE = 'REPORT_GENERATE'
    ADMIN_ACTION = 'ADMIN_ACTION'
    SECURITY_ALERT = 'SECURITY_ALERT'


class AuditSeverity(str, Enum):
    """Audit severity levels"""
    INFO = 'INFO'
    WARNING = 'WARNING'
    ERROR = 'ERROR'
    CRITICAL = 'CRITICAL'


@dataclass
class AuditEvent:
    """
    Audit event record - maps to COBOL SEC-AUDIT-RECORD
    
    Original COBOL structure:
    01  SEC-AUDIT-RECORD.
        05  SEC-AUDIT-TIMESTAMP  PIC X(26).
        05  SEC-AUDIT-USER-ID    PIC X(08).
        05  SEC-AUDIT-TYPE       PIC X(20).
        05  SEC-AUDIT-RESOURCE   PIC X(50).
        05  SEC-AUDIT-ACTION     PIC X(20).
        05  SEC-AUDIT-RESULT     PIC X(10).
        05  SEC-AUDIT-IP-ADDR    PIC X(15).
        05  SEC-AUDIT-DETAILS    PIC X(200).
    """
    timestamp: datetime
    user_id: str
    event_type: AuditEventType
    resource: str
    action: str
    result: str
    ip_address: str = ''
    details: str = ''
    severity: AuditSeverity = AuditSeverity.INFO
    session_id: str = ''
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary"""
        return {
            'timestamp': self.timestamp.isoformat(),
            'user_id': self.user_id,
            'event_type': self.event_type.value,
            'resource': self.resource,
            'action': self.action,
            'result': self.result,
            'ip_address': self.ip_address,
            'details': self.details,
            'severity': self.severity.value,
            'session_id': self.session_id
        }


class AuditLogger:
    """
    Audit Logger - Migrated from COBOL SECMGR.
    
    Provides:
    - Security event logging
    - Access attempt tracking
    - Compliance audit trail
    - Security alerting
    
    Original COBOL program flow:
    1. 4000-AUDIT-ACCESS: Log access attempts
    2. 4100-WRITE-AUDIT: Write audit record
    3. 4200-ALERT-SECURITY: Generate security alerts
    """
    
    def __init__(self, max_events: int = 10000):
        """
        Initialize the audit logger.
        
        Args:
            max_events: Maximum events to keep in memory
        """
        self.max_events = max_events
        self._events: List[AuditEvent] = []
        self._alert_handlers: List[callable] = []
        
        logger.info("AuditLogger initialized")
    
    def log_event(
        self,
        event_type: AuditEventType,
        user_id: str,
        resource: str,
        action: str,
        result: str,
        ip_address: str = '',
        details: str = '',
        severity: AuditSeverity = AuditSeverity.INFO,
        session_id: str = ''
    ) -> AuditEvent:
        """
        Log an audit event.
        Implements COBOL SECMGR 4100-WRITE-AUDIT paragraph.
        
        Args:
            event_type: Type of event
            user_id: User identifier
            resource: Resource being accessed
            action: Action performed
            result: Result of action (SUCCESS, FAILURE, etc.)
            ip_address: Client IP address
            details: Additional details
            severity: Event severity
            session_id: Session identifier
            
        Returns:
            Created AuditEvent
        """
        event = AuditEvent(
            timestamp=datetime.now(),
            user_id=user_id,
            event_type=event_type,
            resource=resource,
            action=action,
            result=result,
            ip_address=ip_address,
            details=details,
            severity=severity,
            session_id=session_id
        )
        
        self._events.append(event)
        
        # Trim old events if needed
        if len(self._events) > self.max_events:
            self._events = self._events[-self.max_events:]
        
        # Log to standard logger
        log_message = (
            f"AUDIT: {event_type.value} | User: {user_id} | "
            f"Resource: {resource} | Action: {action} | Result: {result}"
        )
        
        if severity == AuditSeverity.CRITICAL:
            logger.critical(log_message)
        elif severity == AuditSeverity.ERROR:
            logger.error(log_message)
        elif severity == AuditSeverity.WARNING:
            logger.warning(log_message)
        else:
            logger.info(log_message)
        
        # Check for security alerts
        if severity in [AuditSeverity.ERROR, AuditSeverity.CRITICAL]:
            self._trigger_alert(event)
        
        return event
    
    def log_login_success(
        self,
        user_id: str,
        ip_address: str = '',
        session_id: str = ''
    ):
        """Log successful login"""
        self.log_event(
            event_type=AuditEventType.LOGIN_SUCCESS,
            user_id=user_id,
            resource='AUTH',
            action='LOGIN',
            result='SUCCESS',
            ip_address=ip_address,
            session_id=session_id
        )
    
    def log_login_failure(
        self,
        user_id: str,
        reason: str,
        ip_address: str = ''
    ):
        """Log failed login"""
        self.log_event(
            event_type=AuditEventType.LOGIN_FAILURE,
            user_id=user_id,
            resource='AUTH',
            action='LOGIN',
            result='FAILURE',
            ip_address=ip_address,
            details=reason,
            severity=AuditSeverity.WARNING
        )
    
    def log_logout(
        self,
        user_id: str,
        session_id: str = ''
    ):
        """Log logout"""
        self.log_event(
            event_type=AuditEventType.LOGOUT,
            user_id=user_id,
            resource='AUTH',
            action='LOGOUT',
            result='SUCCESS',
            session_id=session_id
        )
    
    def log_access_denied(
        self,
        user_id: str,
        resource: str,
        action: str,
        reason: str = ''
    ):
        """Log access denied"""
        self.log_event(
            event_type=AuditEventType.ACCESS_DENIED,
            user_id=user_id,
            resource=resource,
            action=action,
            result='DENIED',
            details=reason,
            severity=AuditSeverity.WARNING
        )
    
    def log_data_access(
        self,
        user_id: str,
        resource: str,
        action: str,
        session_id: str = ''
    ):
        """Log data access"""
        self.log_event(
            event_type=AuditEventType.DATA_VIEW,
            user_id=user_id,
            resource=resource,
            action=action,
            result='SUCCESS',
            session_id=session_id
        )
    
    def log_security_alert(
        self,
        user_id: str,
        alert_type: str,
        details: str,
        ip_address: str = ''
    ):
        """Log security alert"""
        self.log_event(
            event_type=AuditEventType.SECURITY_ALERT,
            user_id=user_id,
            resource='SECURITY',
            action=alert_type,
            result='ALERT',
            ip_address=ip_address,
            details=details,
            severity=AuditSeverity.CRITICAL
        )
    
    def get_events(
        self,
        user_id: str = None,
        event_type: AuditEventType = None,
        start_time: datetime = None,
        end_time: datetime = None,
        limit: int = 100
    ) -> List[AuditEvent]:
        """
        Query audit events.
        
        Args:
            user_id: Filter by user ID
            event_type: Filter by event type
            start_time: Filter by start time
            end_time: Filter by end time
            limit: Maximum events to return
            
        Returns:
            List of matching AuditEvent objects
        """
        events = self._events.copy()
        
        if user_id:
            events = [e for e in events if e.user_id == user_id]
        
        if event_type:
            events = [e for e in events if e.event_type == event_type]
        
        if start_time:
            events = [e for e in events if e.timestamp >= start_time]
        
        if end_time:
            events = [e for e in events if e.timestamp <= end_time]
        
        # Sort by timestamp descending
        events.sort(key=lambda e: e.timestamp, reverse=True)
        
        return events[:limit]
    
    def get_login_history(self, user_id: str, limit: int = 10) -> List[AuditEvent]:
        """Get login history for a user"""
        events = [
            e for e in self._events
            if e.user_id == user_id and e.event_type in [
                AuditEventType.LOGIN_SUCCESS,
                AuditEventType.LOGIN_FAILURE
            ]
        ]
        events.sort(key=lambda e: e.timestamp, reverse=True)
        return events[:limit]
    
    def get_security_alerts(self, limit: int = 50) -> List[AuditEvent]:
        """Get recent security alerts"""
        events = [
            e for e in self._events
            if e.severity in [AuditSeverity.ERROR, AuditSeverity.CRITICAL]
        ]
        events.sort(key=lambda e: e.timestamp, reverse=True)
        return events[:limit]
    
    def register_alert_handler(self, handler: callable):
        """Register a handler for security alerts"""
        self._alert_handlers.append(handler)
    
    def _trigger_alert(self, event: AuditEvent):
        """Trigger security alert handlers"""
        for handler in self._alert_handlers:
            try:
                handler(event)
            except Exception as e:
                logger.error(f"Alert handler error: {e}")


# Global audit logger instance
_audit_logger: Optional[AuditLogger] = None


def get_audit_logger() -> AuditLogger:
    """Get or create global audit logger"""
    global _audit_logger
    if _audit_logger is None:
        _audit_logger = AuditLogger()
    return _audit_logger
