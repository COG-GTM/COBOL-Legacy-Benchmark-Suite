"""Tests for JWT token generation, validation, and auth middleware."""

import time

import jwt
import pytest

from python.src.core.auth.middleware import AuthMiddleware, AuthResult
from python.src.core.auth.models import (
    AccessType,
    AuditLogEntry,
    InMemoryAuditLogStore,
    InMemoryUserStore,
    Permission,
    Role,
    User,
)
from python.src.core.auth.utils import (
    TokenManager,
    hash_password,
    verify_password,
)


# --- Password Hashing Tests ---


class TestPasswordHashing:
    """Test bcrypt password hashing utilities."""

    def test_hash_password(self):
        hashed = hash_password("secret123")
        assert hashed != "secret123"
        assert hashed.startswith("$2b$")

    def test_verify_correct_password(self):
        hashed = hash_password("mypassword")
        assert verify_password("mypassword", hashed) is True

    def test_verify_wrong_password(self):
        hashed = hash_password("mypassword")
        assert verify_password("wrongpassword", hashed) is False

    def test_different_hashes_for_same_password(self):
        hash1 = hash_password("same")
        hash2 = hash_password("same")
        assert hash1 != hash2  # Salt should differ


# --- Token Manager Tests ---


class TestTokenManager:
    """Test JWT token generation and validation."""

    @pytest.fixture
    def token_manager(self):
        return TokenManager(
            secret_key="test-secret-key-for-unit-tests",
            access_token_expire_minutes=30,
            refresh_token_expire_days=7,
        )

    def test_create_access_token(self, token_manager):
        token = token_manager.create_access_token(
            user_id="USER001",
            username="testuser",
            roles=["admin", "viewer"],
        )
        assert isinstance(token, str)
        assert len(token) > 0

    def test_validate_access_token(self, token_manager):
        token = token_manager.create_access_token(
            user_id="USER001",
            username="testuser",
            roles=["admin"],
        )
        payload = token_manager.validate_access_token(token)

        assert payload["sub"] == "USER001"
        assert payload["username"] == "testuser"
        assert payload["roles"] == ["admin"]
        assert payload["type"] == "access"

    def test_create_refresh_token(self, token_manager):
        token = token_manager.create_refresh_token(user_id="USER001")
        assert isinstance(token, str)

    def test_validate_refresh_token(self, token_manager):
        token = token_manager.create_refresh_token(user_id="USER001")
        payload = token_manager.validate_refresh_token(token)

        assert payload["sub"] == "USER001"
        assert payload["type"] == "refresh"

    def test_access_token_rejected_as_refresh(self, token_manager):
        token = token_manager.create_access_token(
            user_id="USER001",
            username="testuser",
        )
        with pytest.raises(ValueError, match="not a refresh token"):
            token_manager.validate_refresh_token(token)

    def test_refresh_token_rejected_as_access(self, token_manager):
        token = token_manager.create_refresh_token(user_id="USER001")
        with pytest.raises(ValueError, match="not an access token"):
            token_manager.validate_access_token(token)

    def test_expired_token(self):
        tm = TokenManager(
            secret_key="test-key",
            access_token_expire_minutes=0,  # Immediate expiry
        )
        token = tm.create_access_token(
            user_id="USER001",
            username="testuser",
        )
        # Token created with 0-minute TTL expires immediately
        time.sleep(1)
        with pytest.raises(jwt.ExpiredSignatureError):
            tm.validate_token(token)

    def test_invalid_token(self, token_manager):
        with pytest.raises(jwt.InvalidTokenError):
            token_manager.validate_token("not-a-valid-token")

    def test_wrong_secret_key(self, token_manager):
        token = token_manager.create_access_token(
            user_id="USER001",
            username="testuser",
        )
        other_tm = TokenManager(secret_key="different-key")
        with pytest.raises(jwt.InvalidTokenError):
            other_tm.validate_token(token)

    def test_extra_claims(self, token_manager):
        token = token_manager.create_access_token(
            user_id="USER001",
            username="testuser",
            extra_claims={"department": "finance"},
        )
        payload = token_manager.validate_token(token)
        assert payload["department"] == "finance"


# --- Models Tests ---


class TestModels:
    """Test User, Role, Permission models."""

    def test_permission(self):
        perm = Permission(resource="PORTFOLIO", access_type=AccessType.READ)
        assert perm.resource == "PORTFOLIO"
        assert perm.access_type == AccessType.READ

    def test_role_has_permission(self):
        role = Role(
            name="viewer",
            permissions=[
                Permission(resource="PORTFOLIO", access_type=AccessType.READ),
            ],
        )
        assert role.has_permission("PORTFOLIO", AccessType.READ) is True
        assert role.has_permission("PORTFOLIO", AccessType.WRITE) is False
        assert role.has_permission("OTHER", AccessType.READ) is False

    def test_user_has_permission_through_role(self):
        role = Role(
            name="editor",
            permissions=[
                Permission(resource="PORTFOLIO", access_type=AccessType.READ),
                Permission(resource="PORTFOLIO", access_type=AccessType.WRITE),
            ],
        )
        user = User(
            user_id="U001",
            username="editor1",
            roles=[role],
        )
        assert user.has_permission("PORTFOLIO", AccessType.READ) is True
        assert user.has_permission("PORTFOLIO", AccessType.WRITE) is True
        assert user.has_permission("PORTFOLIO", AccessType.DELETE) is False

    def test_user_no_roles_no_permission(self):
        user = User(user_id="U002", username="noroles")
        assert user.has_permission("PORTFOLIO", AccessType.READ) is False


class TestInMemoryUserStore:
    """Test InMemoryUserStore."""

    def test_save_and_retrieve_by_id(self):
        store = InMemoryUserStore()
        user = User(user_id="U001", username="testuser")
        store.save_user(user)

        retrieved = store.get_user_by_id("U001")
        assert retrieved is not None
        assert retrieved.username == "testuser"

    def test_retrieve_by_username(self):
        store = InMemoryUserStore()
        user = User(user_id="U001", username="testuser")
        store.save_user(user)

        retrieved = store.get_user_by_username("testuser")
        assert retrieved is not None
        assert retrieved.user_id == "U001"

    def test_not_found(self):
        store = InMemoryUserStore()
        assert store.get_user_by_id("MISSING") is None
        assert store.get_user_by_username("MISSING") is None


class TestInMemoryAuditLogStore:
    """Test InMemoryAuditLogStore."""

    def test_save_and_retrieve(self):
        store = InMemoryAuditLogStore()
        from datetime import datetime, timezone

        entry = AuditLogEntry(
            timestamp=datetime.now(timezone.utc),
            user_id="U001",
            event_type="LOGIN",
        )
        store.save_audit_log(entry)
        assert len(store.entries) == 1
        assert store.entries[0].user_id == "U001"


# --- Auth Middleware Tests ---


class TestAuthMiddleware:
    """Test AuthMiddleware mirroring SECMGR patterns."""

    @pytest.fixture
    def setup(self):
        """Create middleware with test user."""
        token_manager = TokenManager(
            secret_key="test-middleware-secret",
            access_token_expire_minutes=30,
        )
        user_store = InMemoryUserStore()
        audit_store = InMemoryAuditLogStore()

        # Create test user with roles
        role = Role(
            name="portfolio_viewer",
            permissions=[
                Permission(resource="PORTFOLIO", access_type=AccessType.READ),
                Permission(resource="HISTORY", access_type=AccessType.READ),
            ],
        )
        user = User(
            user_id="U001",
            username="testuser",
            hashed_password=hash_password("correct-password"),
            roles=[role],
            is_active=True,
        )
        user_store.save_user(user)

        middleware = AuthMiddleware(
            token_manager=token_manager,
            user_store=user_store,
            audit_store=audit_store,
        )

        return {
            "middleware": middleware,
            "token_manager": token_manager,
            "user_store": user_store,
            "audit_store": audit_store,
            "user": user,
        }

    def test_authenticate_success(self, setup):
        """P100: Successful user validation."""
        result = setup["middleware"].authenticate("testuser", "correct-password")

        assert result.success is True
        assert result.response_code == 0
        assert result.user is not None
        assert result.user.user_id == "U001"
        assert result.token != ""
        assert result.refresh_token != ""

    def test_authenticate_wrong_password(self, setup):
        """P100: Failed validation - wrong password (response code 8)."""
        result = setup["middleware"].authenticate("testuser", "wrong-password")

        assert result.success is False
        assert result.response_code == 8
        assert result.error_info == "User validation failed"

    def test_authenticate_unknown_user(self, setup):
        """P100: Failed validation - user not found (response code 8)."""
        result = setup["middleware"].authenticate("unknown", "password")

        assert result.success is False
        assert result.response_code == 8
        assert result.error_info == "User validation failed"

    def test_authenticate_inactive_user(self, setup):
        """P100: Failed validation - disabled account."""
        inactive_user = User(
            user_id="U002",
            username="inactive",
            hashed_password=hash_password("password"),
            is_active=False,
        )
        setup["user_store"].save_user(inactive_user)

        result = setup["middleware"].authenticate("inactive", "password")
        assert result.success is False
        assert result.response_code == 8

    def test_authorize_success(self, setup):
        """P200: Successful authorization check."""
        auth_result = setup["middleware"].authenticate(
            "testuser", "correct-password"
        )
        token = auth_result.token

        result = setup["middleware"].authorize(
            token=token,
            resource="PORTFOLIO",
            access_type=AccessType.READ,
        )

        assert result.success is True
        assert result.response_code == 0

    def test_authorize_denied(self, setup):
        """P200: Access denied (response code 8)."""
        auth_result = setup["middleware"].authenticate(
            "testuser", "correct-password"
        )
        token = auth_result.token

        result = setup["middleware"].authorize(
            token=token,
            resource="PORTFOLIO",
            access_type=AccessType.DELETE,
        )

        assert result.success is False
        assert result.response_code == 8
        assert result.error_info == "Access denied"

    def test_authorize_expired_token(self, setup):
        """P200: Expired token returns code 8."""
        tm = TokenManager(
            secret_key="test-middleware-secret",
            access_token_expire_minutes=0,
        )
        token = tm.create_access_token(
            user_id="U001", username="testuser"
        )
        time.sleep(1)

        result = setup["middleware"].authorize(
            token=token,
            resource="PORTFOLIO",
            access_type=AccessType.READ,
        )

        assert result.success is False
        assert result.response_code == 8
        assert "expired" in result.error_info.lower()

    def test_authorize_invalid_token(self, setup):
        """P200: Invalid token returns code 8."""
        result = setup["middleware"].authorize(
            token="garbage-token",
            resource="PORTFOLIO",
            access_type=AccessType.READ,
        )

        assert result.success is False
        assert result.response_code == 8

    def test_audit_trail(self, setup):
        """P300: Verify audit log entries are created."""
        setup["middleware"].authenticate("testuser", "correct-password")

        entries = setup["audit_store"].entries
        assert len(entries) >= 1

        login_entry = entries[-1]
        assert login_entry.user_id == "U001"
        assert login_entry.event_type == "LOGIN_SUCCESS"
        assert login_entry.success is True

    def test_failed_login_audit(self, setup):
        """P300: Failed login creates audit entry."""
        setup["middleware"].authenticate("testuser", "wrong-password")

        entries = setup["audit_store"].entries
        assert len(entries) >= 1

        fail_entry = entries[-1]
        assert fail_entry.event_type == "LOGIN_FAILED"
        assert fail_entry.success is False

    def test_validate_token(self, setup):
        """Test token validation convenience method."""
        auth_result = setup["middleware"].authenticate(
            "testuser", "correct-password"
        )

        result = setup["middleware"].validate_token(auth_result.token)
        assert result.success is True
        assert result.user.user_id == "U001"

    def test_refresh_access_token(self, setup):
        """Test access token refresh flow."""
        auth_result = setup["middleware"].authenticate(
            "testuser", "correct-password"
        )

        result = setup["middleware"].refresh_access_token(
            auth_result.refresh_token
        )
        assert result.success is True
        assert result.token != ""
        # Validate the refreshed token is a valid access token
        payload = setup["token_manager"].validate_access_token(result.token)
        assert payload["sub"] == "U001"
        assert payload["username"] == "testuser"

    def test_refresh_with_invalid_token(self, setup):
        """Test refresh with invalid token."""
        result = setup["middleware"].refresh_access_token("bad-token")
        assert result.success is False
        assert result.response_code == 8
