"""Tests for data models."""

import pytest
from datetime import datetime, timezone, timedelta

from ..models import (
    AccessType,
    AuditStatus,
    UserIdentity,
    AuthorizationRecord,
    AuditLogEntry,
)


class TestUserIdentity:
    """Tests for the UserIdentity model."""

    def test_create_basic_identity(self):
        """Test creating a basic user identity."""
        identity = UserIdentity(user_id="user123")

        assert identity.user_id == "user123"
        assert identity.email is None
        assert identity.roles == []
        assert identity.groups == []

    def test_create_full_identity(self):
        """Test creating a fully populated identity."""
        identity = UserIdentity(
            user_id="user123",
            email="user@example.com",
            display_name="Test User",
            roles=["admin", "user"],
            groups=["engineering", "security"],
            issuer="https://auth.example.com",
            subject="sub-123",
            audience="api.example.com",
            token_type="JWT",
        )

        assert identity.user_id == "user123"
        assert identity.email == "user@example.com"
        assert identity.display_name == "Test User"
        assert "admin" in identity.roles
        assert "engineering" in identity.groups

    def test_has_role(self):
        """Test role checking."""
        identity = UserIdentity(
            user_id="user123",
            roles=["admin", "user"],
        )

        assert identity.has_role("admin") is True
        assert identity.has_role("user") is True
        assert identity.has_role("superadmin") is False

    def test_has_any_role(self):
        """Test checking for any of multiple roles."""
        identity = UserIdentity(
            user_id="user123",
            roles=["user"],
        )

        assert identity.has_any_role(["admin", "user"]) is True
        assert identity.has_any_role(["admin", "superadmin"]) is False

    def test_has_all_roles(self):
        """Test checking for all roles."""
        identity = UserIdentity(
            user_id="user123",
            roles=["admin", "user", "developer"],
        )

        assert identity.has_all_roles(["admin", "user"]) is True
        assert identity.has_all_roles(["admin", "superadmin"]) is False

    def test_is_member_of(self):
        """Test group membership checking."""
        identity = UserIdentity(
            user_id="user123",
            groups=["engineering", "security"],
        )

        assert identity.is_member_of("engineering") is True
        assert identity.is_member_of("marketing") is False

    def test_to_dict(self):
        """Test serialization to dictionary."""
        issued = datetime(2024, 1, 15, 10, 0, 0, tzinfo=timezone.utc)
        expires = datetime(2024, 1, 15, 11, 0, 0, tzinfo=timezone.utc)

        identity = UserIdentity(
            user_id="user123",
            email="user@example.com",
            roles=["admin"],
            issued_at=issued,
            expires_at=expires,
        )

        result = identity.to_dict()

        assert result["user_id"] == "user123"
        assert result["email"] == "user@example.com"
        assert result["roles"] == ["admin"]
        assert result["issued_at"] == "2024-01-15T10:00:00+00:00"
        assert result["expires_at"] == "2024-01-15T11:00:00+00:00"


class TestAuthorizationRecord:
    """Tests for the AuthorizationRecord model."""

    def test_create_user_based_record(self):
        """Test creating a user-based authorization record."""
        record = AuthorizationRecord(
            user_id="user123",
            resource_pattern="INQONLN",
            access_types=[AccessType.READ],
        )

        assert record.user_id == "user123"
        assert record.resource_pattern == "INQONLN"
        assert AccessType.READ in record.access_types

    def test_create_role_based_record(self):
        """Test creating a role-based authorization record."""
        record = AuthorizationRecord(
            role="admin",
            resource_pattern="*",
            access_types=[AccessType.READ, AccessType.WRITE, AccessType.DELETE],
        )

        assert record.role == "admin"
        assert record.resource_pattern == "*"
        assert len(record.access_types) == 3

    def test_matches_user_by_user_id(self):
        """Test matching by user ID."""
        record = AuthorizationRecord(
            user_id="user123",
            resource_pattern="*",
            access_types=[AccessType.READ],
        )
        identity = UserIdentity(user_id="user123")

        assert record.matches_user(identity) is True

    def test_matches_user_by_role(self):
        """Test matching by role."""
        record = AuthorizationRecord(
            role="admin",
            resource_pattern="*",
            access_types=[AccessType.READ],
        )
        identity = UserIdentity(user_id="user123", roles=["admin"])

        assert record.matches_user(identity) is True

    def test_matches_user_by_group(self):
        """Test matching by group."""
        record = AuthorizationRecord(
            group="engineering",
            resource_pattern="*",
            access_types=[AccessType.READ],
        )
        identity = UserIdentity(user_id="user123", groups=["engineering"])

        assert record.matches_user(identity) is True

    def test_does_not_match_different_user(self):
        """Test that record doesn't match different user."""
        record = AuthorizationRecord(
            user_id="user123",
            resource_pattern="*",
            access_types=[AccessType.READ],
        )
        identity = UserIdentity(user_id="other_user")

        assert record.matches_user(identity) is False

    def test_allows_access(self):
        """Test access type checking."""
        record = AuthorizationRecord(
            user_id="user123",
            resource_pattern="*",
            access_types=[AccessType.READ, AccessType.WRITE],
        )

        assert record.allows_access(AccessType.READ) is True
        assert record.allows_access(AccessType.WRITE) is True
        assert record.allows_access(AccessType.DELETE) is False

    def test_is_expired(self):
        """Test expiration checking."""
        past = datetime.now(timezone.utc) - timedelta(hours=1)
        future = datetime.now(timezone.utc) + timedelta(hours=1)

        expired_record = AuthorizationRecord(
            user_id="user123",
            resource_pattern="*",
            access_types=[AccessType.READ],
            expires_at=past,
        )

        valid_record = AuthorizationRecord(
            user_id="user123",
            resource_pattern="*",
            access_types=[AccessType.READ],
            expires_at=future,
        )

        no_expiry_record = AuthorizationRecord(
            user_id="user123",
            resource_pattern="*",
            access_types=[AccessType.READ],
        )

        assert expired_record.is_expired() is True
        assert valid_record.is_expired() is False
        assert no_expiry_record.is_expired() is False


class TestAuditLogEntry:
    """Tests for the AuditLogEntry model."""

    def test_create_basic_entry(self):
        """Test creating a basic audit log entry."""
        entry = AuditLogEntry(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            resource_name="INQONLN",
            access_type=AccessType.READ,
        )

        assert entry.user_id == "user123"
        assert entry.client_id == "192.168.1.1"
        assert entry.request_id == "req-123"
        assert entry.resource_name == "INQONLN"
        assert entry.access_type == AccessType.READ
        assert entry.status == AuditStatus.SUCCESS

    def test_create_full_entry(self):
        """Test creating a fully populated audit entry."""
        timestamp = datetime.now(timezone.utc)

        entry = AuditLogEntry(
            timestamp=timestamp,
            user_id="user@example.com",
            client_id="10.0.0.1",
            request_id="550e8400-e29b-41d4-a716-446655440000",
            resource_name="INQPORT",
            access_type=AccessType.READ,
            status=AuditStatus.SUCCESS,
            trace_id="trace-123",
            session_id="session-456",
            user_agent="Mozilla/5.0",
            response_code=200,
            duration_ms=150,
            ip_address="10.0.0.1",
            metadata={"portfolio_id": "P001"},
        )

        assert entry.timestamp == timestamp
        assert entry.user_id == "user@example.com"
        assert entry.trace_id == "trace-123"
        assert entry.response_code == 200
        assert entry.duration_ms == 150
        assert entry.metadata["portfolio_id"] == "P001"

    def test_to_dict(self):
        """Test serialization to dictionary."""
        timestamp = datetime(2024, 1, 15, 10, 30, 0, tzinfo=timezone.utc)

        entry = AuditLogEntry(
            id="entry-123",
            timestamp=timestamp,
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="req-123",
            resource_name="INQONLN",
            access_type=AccessType.READ,
            status=AuditStatus.SUCCESS,
            response_code=200,
        )

        result = entry.to_dict()

        assert result["id"] == "entry-123"
        assert result["timestamp"] == "2024-01-15T10:30:00+00:00"
        assert result["user_id"] == "user@example.com"
        assert result["access_type"] == "READ"
        assert result["status"] == "SUCCESS"
        assert result["response_code"] == 200

    def test_from_dict(self):
        """Test deserialization from dictionary."""
        data = {
            "id": "entry-123",
            "timestamp": "2024-01-15T10:30:00+00:00",
            "user_id": "user@example.com",
            "client_id": "192.168.1.1",
            "request_id": "req-123",
            "resource_name": "INQONLN",
            "access_type": "READ",
            "status": "SUCCESS",
            "response_code": 200,
            "duration_ms": 150,
            "metadata": {"key": "value"},
        }

        entry = AuditLogEntry.from_dict(data)

        assert entry.id == "entry-123"
        assert entry.user_id == "user@example.com"
        assert entry.access_type == AccessType.READ
        assert entry.status == AuditStatus.SUCCESS
        assert entry.response_code == 200
        assert entry.duration_ms == 150
        assert entry.metadata == {"key": "value"}

    def test_to_sql_values(self):
        """Test generating SQL-compatible values."""
        entry = AuditLogEntry(
            id="entry-123",
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="req-123",
            resource_name="INQONLN",
            access_type=AccessType.READ,
            status=AuditStatus.SUCCESS,
        )

        values = entry.to_sql_values()

        assert values[0] == "entry-123"
        assert values[2] == "user@example.com"
        assert values[3] == "192.168.1.1"
        assert values[4] == "req-123"
        assert values[5] == "INQONLN"
        assert values[6] == "READ"
        assert values[7] == "SUCCESS"


class TestAccessType:
    """Tests for AccessType enum."""

    def test_access_type_values(self):
        """Test that access types have expected values."""
        assert AccessType.READ.value == "READ"
        assert AccessType.WRITE.value == "WRITE"
        assert AccessType.DELETE.value == "DELETE"
        assert AccessType.EXECUTE.value == "EXECUTE"
        assert AccessType.ADMIN.value == "ADMIN"


class TestAuditStatus:
    """Tests for AuditStatus enum."""

    def test_audit_status_values(self):
        """Test that audit statuses have expected values."""
        assert AuditStatus.SUCCESS.value == "SUCCESS"
        assert AuditStatus.FAILURE.value == "FAILURE"
        assert AuditStatus.DENIED.value == "DENIED"
        assert AuditStatus.ERROR.value == "ERROR"
