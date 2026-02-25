"""
Data models for the modern security context system.

This module defines the expanded audit logging schema and related models,
replacing the original COBOL AUDITLOG table structure.

Original AUDITLOG schema (from SECMGR.cbl):
- TIMESTAMP (26 chars)
- USER_ID (8 chars)
- TERMINAL_ID (4 chars)
- TRANS_ID (4 chars)
- PROGRAM (8 chars)
- ACCESS_TYPE (8 chars)

Modern schema expands these fields to accommodate:
- Email addresses and UUIDs for user identification
- IP addresses and device IDs for client identification
- UUID correlation IDs for request tracking
- Additional fields for comprehensive audit trails
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Optional
import uuid


class AccessType(str, Enum):
    """Types of access that can be audited."""

    READ = "READ"
    WRITE = "WRITE"
    DELETE = "DELETE"
    EXECUTE = "EXECUTE"
    ADMIN = "ADMIN"


class AuditStatus(str, Enum):
    """Status of the audited operation."""

    SUCCESS = "SUCCESS"
    FAILURE = "FAILURE"
    DENIED = "DENIED"
    ERROR = "ERROR"


@dataclass
class UserIdentity:
    """
    Represents an authenticated user's identity.

    This replaces the simple 8-character USERID from CICS with
    a comprehensive identity model supporting modern authentication.
    """

    user_id: str
    email: Optional[str] = None
    display_name: Optional[str] = None
    roles: list[str] = field(default_factory=list)
    groups: list[str] = field(default_factory=list)
    issuer: Optional[str] = None
    subject: Optional[str] = None
    audience: Optional[str] = None
    issued_at: Optional[datetime] = None
    expires_at: Optional[datetime] = None
    token_type: Optional[str] = None

    def has_role(self, role: str) -> bool:
        """Check if user has a specific role."""
        return role in self.roles

    def has_any_role(self, roles: list[str]) -> bool:
        """Check if user has any of the specified roles."""
        return any(role in self.roles for role in roles)

    def has_all_roles(self, roles: list[str]) -> bool:
        """Check if user has all of the specified roles."""
        return all(role in self.roles for role in roles)

    def is_member_of(self, group: str) -> bool:
        """Check if user is a member of a specific group."""
        return group in self.groups

    def to_dict(self) -> dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "user_id": self.user_id,
            "email": self.email,
            "display_name": self.display_name,
            "roles": self.roles,
            "groups": self.groups,
            "issuer": self.issuer,
            "subject": self.subject,
            "audience": self.audience,
            "issued_at": self.issued_at.isoformat() if self.issued_at else None,
            "expires_at": self.expires_at.isoformat() if self.expires_at else None,
            "token_type": self.token_type,
        }


@dataclass
class AuthorizationRecord:
    """
    Represents an authorization rule in the system.

    This replaces the AUTHFILE DB2 table structure from SECMGR.cbl
    with a more flexible authorization model.

    Original query (SECMGR.cbl lines 79-86):
        SELECT COUNT(*)
        FROM AUTHFILE
        WHERE USER_ID = :SEC-USER-ID
          AND RESOURCE = :SEC-RESOURCE-NAME
          AND ACCESS_TYPE = :SEC-ACCESS-TYPE
    """

    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    user_id: Optional[str] = None
    role: Optional[str] = None
    group: Optional[str] = None
    resource_pattern: str = ""
    access_types: list[AccessType] = field(default_factory=list)
    conditions: dict[str, Any] = field(default_factory=dict)
    priority: int = 0
    enabled: bool = True
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    expires_at: Optional[datetime] = None

    def matches_user(self, identity: UserIdentity) -> bool:
        """Check if this rule applies to the given user identity."""
        if self.user_id and self.user_id == identity.user_id:
            return True
        if self.role and identity.has_role(self.role):
            return True
        if self.group and identity.is_member_of(self.group):
            return True
        return False

    def allows_access(self, access_type: AccessType) -> bool:
        """Check if this rule allows the specified access type."""
        return access_type in self.access_types

    def is_expired(self) -> bool:
        """Check if this authorization rule has expired."""
        if self.expires_at is None:
            return False
        return datetime.now(timezone.utc) > self.expires_at


@dataclass
class AuditLogEntry:
    """
    Modern audit log entry with expanded fields.

    This replaces the original AUDITLOG table with a comprehensive
    audit trail supporting modern identifiers and additional context.

    Field mappings from original COBOL:
    - TIMESTAMP (26 chars) -> timestamp (ISO 8601 datetime)
    - USER_ID (8 chars) -> user_id (up to 255 chars)
    - TERMINAL_ID (4 chars) -> client_id (up to 255 chars)
    - TRANS_ID (4 chars) -> request_id (36 char UUID)
    - PROGRAM (8 chars) -> resource_name (up to 255 chars)
    - ACCESS_TYPE (8 chars) -> access_type (enum)

    Additional modern fields:
    - trace_id: Distributed tracing correlation
    - session_id: User session tracking
    - user_agent: Client application identification
    - response_code: HTTP status or application response code
    - duration_ms: Request processing time
    - ip_address: Client IP for security analysis
    - error_message: Details if operation failed
    - metadata: Extensible JSON for additional context
    """

    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    user_id: str = ""
    client_id: str = ""
    request_id: str = ""
    resource_name: str = ""
    access_type: AccessType = AccessType.READ
    status: AuditStatus = AuditStatus.SUCCESS
    trace_id: Optional[str] = None
    session_id: Optional[str] = None
    user_agent: Optional[str] = None
    response_code: Optional[int] = None
    duration_ms: Optional[int] = None
    ip_address: Optional[str] = None
    error_message: Optional[str] = None
    metadata: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        """Convert to dictionary for serialization or database insertion."""
        return {
            "id": self.id,
            "timestamp": self.timestamp.isoformat(),
            "user_id": self.user_id,
            "client_id": self.client_id,
            "request_id": self.request_id,
            "resource_name": self.resource_name,
            "access_type": self.access_type.value,
            "status": self.status.value,
            "trace_id": self.trace_id,
            "session_id": self.session_id,
            "user_agent": self.user_agent,
            "response_code": self.response_code,
            "duration_ms": self.duration_ms,
            "ip_address": self.ip_address,
            "error_message": self.error_message,
            "metadata": self.metadata,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> AuditLogEntry:
        """Create an AuditLogEntry from a dictionary."""
        timestamp = data.get("timestamp")
        if isinstance(timestamp, str):
            timestamp = datetime.fromisoformat(timestamp)
        elif timestamp is None:
            timestamp = datetime.now(timezone.utc)

        return cls(
            id=data.get("id", str(uuid.uuid4())),
            timestamp=timestamp,
            user_id=data.get("user_id", ""),
            client_id=data.get("client_id", ""),
            request_id=data.get("request_id", ""),
            resource_name=data.get("resource_name", ""),
            access_type=AccessType(data.get("access_type", "READ")),
            status=AuditStatus(data.get("status", "SUCCESS")),
            trace_id=data.get("trace_id"),
            session_id=data.get("session_id"),
            user_agent=data.get("user_agent"),
            response_code=data.get("response_code"),
            duration_ms=data.get("duration_ms"),
            ip_address=data.get("ip_address"),
            error_message=data.get("error_message"),
            metadata=data.get("metadata", {}),
        )

    def to_sql_values(self) -> tuple[str, ...]:
        """
        Generate SQL-compatible values tuple.

        This provides compatibility with the original DB2 INSERT pattern
        while supporting the expanded schema.
        """
        return (
            self.id,
            self.timestamp.isoformat(),
            self.user_id[:255],
            self.client_id[:255],
            self.request_id[:36],
            self.resource_name[:255],
            self.access_type.value,
            self.status.value,
            self.trace_id or "",
            self.session_id or "",
            self.user_agent or "",
            str(self.response_code) if self.response_code is not None else "",
            str(self.duration_ms) if self.duration_ms is not None else "",
            self.ip_address or "",
            self.error_message or "",
        )


# SQL DDL for the modern AUDITLOG table
AUDITLOG_DDL = """
CREATE TABLE IF NOT EXISTS AUDITLOG (
    id VARCHAR(36) PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    resource_name VARCHAR(255) NOT NULL,
    access_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    trace_id VARCHAR(64),
    session_id VARCHAR(64),
    user_agent VARCHAR(512),
    response_code INTEGER,
    duration_ms INTEGER,
    ip_address VARCHAR(45),
    error_message TEXT,
    metadata JSON,
    
    INDEX idx_auditlog_timestamp (timestamp),
    INDEX idx_auditlog_user_id (user_id),
    INDEX idx_auditlog_request_id (request_id),
    INDEX idx_auditlog_trace_id (trace_id),
    INDEX idx_auditlog_resource (resource_name)
);
"""

# SQL DDL for the modern AUTHFILE table
AUTHFILE_DDL = """
CREATE TABLE IF NOT EXISTS AUTHFILE (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255),
    role VARCHAR(64),
    group_name VARCHAR(64),
    resource_pattern VARCHAR(255) NOT NULL,
    access_types VARCHAR(255) NOT NULL,
    conditions JSON,
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    
    INDEX idx_authfile_user_id (user_id),
    INDEX idx_authfile_role (role),
    INDEX idx_authfile_resource (resource_pattern)
);
"""
