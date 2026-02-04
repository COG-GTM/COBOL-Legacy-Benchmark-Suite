"""Tests for the SecurityManager and three-phase security model."""

import pytest
from datetime import datetime, timezone, timedelta

from ..context import SecurityContext, SecurityContextBuilder
from ..models import (
    AccessType,
    AuditStatus,
    AuthorizationRecord,
    UserIdentity,
)
from ..security_manager import (
    SecurityManager,
    SecurityPhase,
    SecurityResponse,
    DefaultTokenValidator,
    InMemoryAuthorizationStore,
    InMemoryAuditLogger,
    create_security_manager,
)


class TestSecurityResponse:
    """Tests for SecurityResponse."""

    def test_ok_response(self):
        """Test creating a successful response."""
        response = SecurityResponse.ok(
            phase=SecurityPhase.VALIDATE,
            details={"user_id": "user123"},
        )

        assert response.success is True
        assert response.response_code == 0
        assert response.phase == SecurityPhase.VALIDATE
        assert response.details["user_id"] == "user123"

    def test_error_response(self):
        """Test creating an error response."""
        response = SecurityResponse.error(
            phase=SecurityPhase.AUTHORIZE,
            code=8,
            message="Access denied",
            details={"resource": "INQONLN"},
        )

        assert response.success is False
        assert response.response_code == 8
        assert response.error_info == "Access denied"
        assert response.phase == SecurityPhase.AUTHORIZE


class TestDefaultTokenValidator:
    """Tests for the DefaultTokenValidator."""

    def test_validate_valid_context(self):
        """Test validating a valid context."""
        validator = DefaultTokenValidator(verify_expiration=False)
        context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "user123"},
        )

        is_valid, error = validator.validate(context)

        assert is_valid is True
        assert error is None

    def test_validate_missing_user_id(self):
        """Test that validation fails without user_id."""
        validator = DefaultTokenValidator()

        context = SecurityContextBuilder()
        context._user_id = ""
        context._client_id = "192.168.1.1"
        context._request_id = "req-123"

        with pytest.raises(ValueError):
            context.build()

    def test_validate_expired_token(self):
        """Test that validation fails for expired tokens."""
        validator = DefaultTokenValidator(verify_expiration=True)
        past = int((datetime.now(timezone.utc) - timedelta(hours=1)).timestamp())

        context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "user123", "exp": past},
        )

        is_valid, error = validator.validate(context)

        assert is_valid is False
        assert "expired" in error.lower()

    def test_validate_valid_expiration(self):
        """Test that validation passes for non-expired tokens."""
        validator = DefaultTokenValidator(verify_expiration=True)
        future = int((datetime.now(timezone.utc) + timedelta(hours=1)).timestamp())

        context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "user123", "exp": future},
        )

        is_valid, error = validator.validate(context)

        assert is_valid is True
        assert error is None

    def test_validate_issuer(self):
        """Test issuer validation."""
        validator = DefaultTokenValidator(verify_issuer="https://auth.example.com")

        valid_context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "user123", "iss": "https://auth.example.com"},
        )

        invalid_context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-456",
            claims={"sub": "user123", "iss": "https://other.example.com"},
        )

        is_valid, _ = validator.validate(valid_context)
        assert is_valid is True

        is_valid, error = validator.validate(invalid_context)
        assert is_valid is False
        assert "issuer" in error.lower()

    def test_validate_user_id_mismatch(self):
        """Test that validation fails when user_id doesn't match claims."""
        validator = DefaultTokenValidator()

        context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "different_user"},
        )

        is_valid, error = validator.validate(context)

        assert is_valid is False
        assert "mismatch" in error.lower()


class TestInMemoryAuthorizationStore:
    """Tests for the InMemoryAuthorizationStore."""

    def test_check_permission_by_user_id(self):
        """Test checking permission by user ID."""
        store = InMemoryAuthorizationStore()
        store.add_record(
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="INQONLN",
                access_types=[AccessType.READ],
            )
        )

        assert store.check_permission("user123", "INQONLN", AccessType.READ) is True
        assert store.check_permission("user123", "INQONLN", AccessType.WRITE) is False
        assert store.check_permission("other_user", "INQONLN", AccessType.READ) is False

    def test_check_permission_by_role(self):
        """Test checking permission by role."""
        store = InMemoryAuthorizationStore()
        store.add_record(
            AuthorizationRecord(
                role="admin",
                resource_pattern="*",
                access_types=[AccessType.READ, AccessType.WRITE, AccessType.DELETE],
            )
        )

        identity = UserIdentity(user_id="user123", roles=["admin"])

        assert (
            store.check_permission("user123", "INQONLN", AccessType.READ, identity)
            is True
        )
        assert (
            store.check_permission("user123", "INQPORT", AccessType.WRITE, identity)
            is True
        )

    def test_check_permission_wildcard_resource(self):
        """Test wildcard resource pattern matching."""
        store = InMemoryAuthorizationStore()
        store.add_record(
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="INQ*",
                access_types=[AccessType.READ],
            )
        )

        assert store.check_permission("user123", "INQONLN", AccessType.READ) is True
        assert store.check_permission("user123", "INQPORT", AccessType.READ) is True
        assert store.check_permission("user123", "RPTPOS", AccessType.READ) is False

    def test_check_permission_respects_priority(self):
        """Test that higher priority rules take precedence."""
        store = InMemoryAuthorizationStore()

        store.add_record(
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="*",
                access_types=[AccessType.READ],
                priority=0,
            )
        )

        store.add_record(
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="ADMIN*",
                access_types=[],
                priority=10,
            )
        )

        assert store.check_permission("user123", "INQONLN", AccessType.READ) is True

    def test_check_permission_disabled_record(self):
        """Test that disabled records are ignored."""
        store = InMemoryAuthorizationStore()
        store.add_record(
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="*",
                access_types=[AccessType.READ],
                enabled=False,
            )
        )

        assert store.check_permission("user123", "INQONLN", AccessType.READ) is False

    def test_check_permission_expired_record(self):
        """Test that expired records are ignored."""
        store = InMemoryAuthorizationStore()
        past = datetime.now(timezone.utc) - timedelta(hours=1)

        store.add_record(
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="*",
                access_types=[AccessType.READ],
                expires_at=past,
            )
        )

        assert store.check_permission("user123", "INQONLN", AccessType.READ) is False


class TestInMemoryAuditLogger:
    """Tests for the InMemoryAuditLogger."""

    def test_log_entry(self):
        """Test logging an audit entry."""
        from ..models import AuditLogEntry

        logger = InMemoryAuditLogger()
        entry = AuditLogEntry(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            resource_name="INQONLN",
            access_type=AccessType.READ,
        )

        result = logger.log(entry)

        assert result is True
        assert len(logger.entries) == 1
        assert logger.entries[0] == entry

    def test_get_entries_by_user(self):
        """Test filtering entries by user."""
        from ..models import AuditLogEntry

        logger = InMemoryAuditLogger()

        logger.log(
            AuditLogEntry(
                user_id="user123",
                client_id="192.168.1.1",
                request_id="req-1",
                resource_name="INQONLN",
                access_type=AccessType.READ,
            )
        )
        logger.log(
            AuditLogEntry(
                user_id="user456",
                client_id="192.168.1.2",
                request_id="req-2",
                resource_name="INQPORT",
                access_type=AccessType.READ,
            )
        )

        entries = logger.get_entries(user_id="user123")

        assert len(entries) == 1
        assert entries[0].user_id == "user123"

    def test_get_entries_by_resource(self):
        """Test filtering entries by resource."""
        from ..models import AuditLogEntry

        logger = InMemoryAuditLogger()

        logger.log(
            AuditLogEntry(
                user_id="user123",
                client_id="192.168.1.1",
                request_id="req-1",
                resource_name="INQONLN",
                access_type=AccessType.READ,
            )
        )
        logger.log(
            AuditLogEntry(
                user_id="user123",
                client_id="192.168.1.1",
                request_id="req-2",
                resource_name="INQPORT",
                access_type=AccessType.READ,
            )
        )

        entries = logger.get_entries(resource="INQONLN")

        assert len(entries) == 1
        assert entries[0].resource_name == "INQONLN"


class TestSecurityManager:
    """Tests for the SecurityManager three-phase security model."""

    @pytest.fixture
    def security_manager(self):
        """Create a configured security manager for testing."""
        auth_store = InMemoryAuthorizationStore()
        auth_store.add_record(
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="INQONLN",
                access_types=[AccessType.READ],
            )
        )
        auth_store.add_record(
            AuthorizationRecord(
                role="admin",
                resource_pattern="*",
                access_types=[AccessType.READ, AccessType.WRITE, AccessType.DELETE],
            )
        )

        return SecurityManager(
            token_validator=DefaultTokenValidator(verify_expiration=False),
            authorization_store=auth_store,
            audit_logger=InMemoryAuditLogger(),
        )

    @pytest.fixture
    def valid_context(self):
        """Create a valid security context for testing."""
        return SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "user123"},
        )

    @pytest.mark.asyncio
    async def test_validate_user_success(self, security_manager, valid_context):
        """Test successful user validation (Phase 1)."""
        result = await security_manager.validate_user(valid_context)

        assert result.success is True
        assert result.phase == SecurityPhase.VALIDATE
        assert result.response_code == 0

    @pytest.mark.asyncio
    async def test_validate_user_failure(self, security_manager):
        """Test failed user validation."""
        context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "different_user"},
        )

        result = await security_manager.validate_user(context)

        assert result.success is False
        assert result.phase == SecurityPhase.VALIDATE
        assert result.response_code == 8

    @pytest.mark.asyncio
    async def test_authorize_access_success(self, security_manager, valid_context):
        """Test successful authorization (Phase 2)."""
        result = await security_manager.authorize_access(
            valid_context, "INQONLN", AccessType.READ
        )

        assert result.success is True
        assert result.phase == SecurityPhase.AUTHORIZE
        assert result.response_code == 0

    @pytest.mark.asyncio
    async def test_authorize_access_denied(self, security_manager, valid_context):
        """Test denied authorization."""
        result = await security_manager.authorize_access(
            valid_context, "INQONLN", AccessType.WRITE
        )

        assert result.success is False
        assert result.phase == SecurityPhase.AUTHORIZE
        assert result.response_code == 8
        assert "denied" in result.error_info.lower()

    @pytest.mark.asyncio
    async def test_log_access_success(self, security_manager, valid_context):
        """Test successful audit logging (Phase 3)."""
        result = await security_manager.log_access(
            valid_context,
            "INQONLN",
            AccessType.READ,
            status=AuditStatus.SUCCESS,
        )

        assert result.success is True
        assert result.phase == SecurityPhase.AUDIT
        assert "audit_id" in result.details

    @pytest.mark.asyncio
    async def test_check_security_full_success(self, security_manager, valid_context):
        """Test complete three-phase security check with success."""
        result = await security_manager.check_security(
            valid_context, "INQONLN", AccessType.READ
        )

        assert result.success is True
        assert result.phase == SecurityPhase.AUDIT

        entries = security_manager.audit_logger.get_entries(user_id="user123")
        assert len(entries) == 1
        assert entries[0].status == AuditStatus.SUCCESS

    @pytest.mark.asyncio
    async def test_check_security_validation_failure(self, security_manager):
        """Test three-phase check with validation failure."""
        context = SecurityContext(
            user_id="user123",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "wrong_user"},
        )

        result = await security_manager.check_security(
            context, "INQONLN", AccessType.READ
        )

        assert result.success is False
        assert result.phase == SecurityPhase.VALIDATE

        entries = security_manager.audit_logger.get_entries()
        assert len(entries) == 1
        assert entries[0].status == AuditStatus.FAILURE

    @pytest.mark.asyncio
    async def test_check_security_authorization_failure(
        self, security_manager, valid_context
    ):
        """Test three-phase check with authorization failure."""
        result = await security_manager.check_security(
            valid_context, "RESTRICTED_RESOURCE", AccessType.WRITE
        )

        assert result.success is False
        assert result.phase == SecurityPhase.AUTHORIZE

        entries = security_manager.audit_logger.get_entries()
        assert len(entries) == 1
        assert entries[0].status == AuditStatus.DENIED

    @pytest.mark.asyncio
    async def test_check_security_with_identity(self, security_manager):
        """Test three-phase check with user identity for role-based access."""
        context = SecurityContext(
            user_id="admin_user",
            client_id="192.168.1.1",
            request_id="req-123",
            claims={"sub": "admin_user"},
        )
        identity = UserIdentity(user_id="admin_user", roles=["admin"])

        result = await security_manager.check_security(
            context, "ANY_RESOURCE", AccessType.DELETE, identity=identity
        )

        assert result.success is True


class TestCreateSecurityManager:
    """Tests for the factory function."""

    def test_create_with_defaults(self):
        """Test creating security manager with default settings."""
        manager = create_security_manager()

        assert manager.token_validator is not None
        assert manager.authorization_store is not None
        assert manager.audit_logger is not None

    def test_create_with_authorization_records(self):
        """Test creating security manager with initial authorization records."""
        records = [
            AuthorizationRecord(
                user_id="user123",
                resource_pattern="*",
                access_types=[AccessType.READ],
            )
        ]

        manager = create_security_manager(authorization_records=records)

        assert manager.authorization_store.check_permission(
            "user123", "INQONLN", AccessType.READ
        )
