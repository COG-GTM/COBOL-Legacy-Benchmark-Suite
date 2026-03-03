"""User, Role, and Permission models replacing COBOL SECMGR data structures.

Maps SECMGR concepts:
  - SEC-USER-ID        -> User.user_id
  - SEC-RESOURCE-NAME  -> Permission.resource
  - SEC-ACCESS-TYPE    -> Permission.access_type
  - AUTHFILE table     -> Role/Permission models
  - AUDITLOG table     -> AuditLogEntry model

Authorization levels from SECMGR:
  - 'V' (Validate)  -> User authentication
  - 'A' (Authorize) -> Role-based resource access check
  - 'L' (Audit)     -> Security event logging
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import List, Optional


class AccessType(Enum):
    """Access types for resource authorization.

    Maps to SEC-ACCESS-TYPE values used in SECMGR AUTHFILE lookups.
    """

    READ = "READ"
    WRITE = "WRITE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    EXECUTE = "EXECUTE"
    ADMIN = "ADMIN"


@dataclass
class Permission:
    """Permission model mapping to SECMGR AUTHFILE record.

    Represents a single authorization entry:
      AUTHFILE.USER_ID + AUTHFILE.RESOURCE + AUTHFILE.ACCESS_TYPE

    Attributes:
        resource: Resource name (SEC-RESOURCE-NAME, PIC X(8)).
        access_type: Type of access granted (SEC-ACCESS-TYPE).
    """

    resource: str
    access_type: AccessType


@dataclass
class Role:
    """Role model for grouping permissions.

    Extends SECMGR's flat AUTHFILE model into a hierarchical
    role-based access control (RBAC) structure.

    Attributes:
        name: Role identifier.
        description: Human-readable role description.
        permissions: List of permissions granted to this role.
    """

    name: str
    description: str = ""
    permissions: List[Permission] = field(default_factory=list)

    def has_permission(self, resource: str, access_type: AccessType) -> bool:
        """Check if role grants access to a resource.

        Mirrors SECMGR P200-CHECK-AUTH:
          SELECT COUNT(*) FROM AUTHFILE
          WHERE USER_ID = :SEC-USER-ID
            AND RESOURCE = :SEC-RESOURCE-NAME
            AND ACCESS_TYPE = :SEC-ACCESS-TYPE
        """
        return any(
            p.resource == resource and p.access_type == access_type
            for p in self.permissions
        )


@dataclass
class User:
    """User model mapping to SECMGR security context.

    Attributes:
        user_id: Unique user identifier (SEC-USER-ID, PIC X(8)).
        username: Login username.
        hashed_password: Bcrypt-hashed password.
        roles: List of assigned roles.
        is_active: Whether the user account is active.
        created_at: Account creation timestamp.
        last_login: Last successful login timestamp.
    """

    user_id: str
    username: str
    hashed_password: str = ""
    roles: List[Role] = field(default_factory=list)
    is_active: bool = True
    created_at: Optional[datetime] = None
    last_login: Optional[datetime] = None

    def has_permission(self, resource: str, access_type: AccessType) -> bool:
        """Check if user has permission to access a resource.

        Checks across all assigned roles, mirroring SECMGR's
        AUTHFILE lookup.
        """
        return any(role.has_permission(resource, access_type) for role in self.roles)


@dataclass
class AuditLogEntry:
    """Audit log entry mapping to SECMGR AUDITLOG table.

    Maps SECMGR P300-LOG-ACCESS INSERT:
      AUDITLOG(TIMESTAMP, USER_ID, TERMINAL_ID,
               TRANS_ID, PROGRAM, ACCESS_TYPE)

    Attributes:
        timestamp: Event timestamp (WS-TIMESTAMP).
        user_id: User who performed the action (WS-USER-ID).
        terminal_id: Terminal/client identifier (WS-TERMINAL-ID).
        transaction_id: Transaction identifier (WS-TRANSACTION-ID).
        resource: Resource accessed (WS-PROGRAM-NAME).
        access_type: Type of access (WS-ACCESS-TYPE).
        event_type: Type of security event.
        details: Additional event details.
        success: Whether the action succeeded.
    """

    timestamp: datetime
    user_id: str
    terminal_id: str = ""
    transaction_id: str = ""
    resource: str = ""
    access_type: str = ""
    event_type: str = ""
    details: str = ""
    success: bool = True


class UserStore(ABC):
    """Abstract interface for user persistence.

    Allows the database layer to be plugged in later,
    similar to how SECMGR accesses AUTHFILE via DB2.
    """

    @abstractmethod
    def get_user_by_id(self, user_id: str) -> Optional[User]:
        """Retrieve a user by their unique ID."""
        ...

    @abstractmethod
    def get_user_by_username(self, username: str) -> Optional[User]:
        """Retrieve a user by their login username."""
        ...

    @abstractmethod
    def save_user(self, user: User) -> bool:
        """Persist a user record."""
        ...


class AuditLogStore(ABC):
    """Abstract interface for audit log persistence.

    Maps to SECMGR's INSERT INTO AUDITLOG operation.
    """

    @abstractmethod
    def save_audit_log(self, entry: AuditLogEntry) -> bool:
        """Persist an audit log entry.

        Mirrors SECMGR P300-LOG-ACCESS INSERT INTO AUDITLOG.
        """
        ...


class InMemoryUserStore(UserStore):
    """In-memory user store for testing and development.

    Not intended for production use.
    """

    def __init__(self) -> None:
        self._users_by_id: dict[str, User] = {}
        self._users_by_username: dict[str, User] = {}

    def get_user_by_id(self, user_id: str) -> Optional[User]:
        return self._users_by_id.get(user_id)

    def get_user_by_username(self, username: str) -> Optional[User]:
        return self._users_by_username.get(username)

    def save_user(self, user: User) -> bool:
        self._users_by_id[user.user_id] = user
        self._users_by_username[user.username] = user
        return True


class InMemoryAuditLogStore(AuditLogStore):
    """In-memory audit log store for testing and development."""

    def __init__(self) -> None:
        self._entries: List[AuditLogEntry] = []

    def save_audit_log(self, entry: AuditLogEntry) -> bool:
        self._entries.append(entry)
        return True

    @property
    def entries(self) -> List[AuditLogEntry]:
        """Access stored audit log entries."""
        return list(self._entries)
