"""Tests for SecurityContext and SecurityContextBuilder."""

import pytest
from datetime import datetime, timezone

from ..context import (
    SecurityContext,
    SecurityContextBuilder,
    set_current_context,
    get_current_context,
    clear_current_context,
)


class TestSecurityContext:
    """Tests for the SecurityContext dataclass."""

    def test_create_context_with_required_fields(self):
        """Test creating context with only required fields."""
        context = SecurityContext(
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="550e8400-e29b-41d4-a716-446655440000",
        )

        assert context.user_id == "user@example.com"
        assert context.client_id == "192.168.1.1"
        assert context.request_id == "550e8400-e29b-41d4-a716-446655440000"
        assert context.trace_id is None
        assert context.session_id is None
        assert context.user_agent is None
        assert context.claims == {}

    def test_create_context_with_all_fields(self):
        """Test creating context with all fields populated."""
        timestamp = datetime.now(timezone.utc)
        claims = {"roles": ["admin"], "email": "admin@example.com"}

        context = SecurityContext(
            user_id="admin@example.com",
            client_id="10.0.0.1",
            request_id="req-123",
            trace_id="trace-456",
            session_id="session-789",
            user_agent="Mozilla/5.0",
            timestamp=timestamp,
            claims=claims,
        )

        assert context.user_id == "admin@example.com"
        assert context.client_id == "10.0.0.1"
        assert context.request_id == "req-123"
        assert context.trace_id == "trace-456"
        assert context.session_id == "session-789"
        assert context.user_agent == "Mozilla/5.0"
        assert context.timestamp == timestamp
        assert context.claims == claims

    def test_context_is_immutable(self):
        """Test that context is immutable (frozen dataclass)."""
        context = SecurityContext(
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="req-123",
        )

        with pytest.raises(AttributeError):
            context.user_id = "other@example.com"

    def test_context_requires_user_id(self):
        """Test that user_id is required."""
        with pytest.raises(ValueError, match="user_id is required"):
            SecurityContext(
                user_id="",
                client_id="192.168.1.1",
                request_id="req-123",
            )

    def test_context_requires_client_id(self):
        """Test that client_id is required."""
        with pytest.raises(ValueError, match="client_id is required"):
            SecurityContext(
                user_id="user@example.com",
                client_id="",
                request_id="req-123",
            )

    def test_context_requires_request_id(self):
        """Test that request_id is required."""
        with pytest.raises(ValueError, match="request_id is required"):
            SecurityContext(
                user_id="user@example.com",
                client_id="192.168.1.1",
                request_id="",
            )

    def test_to_dict(self):
        """Test serialization to dictionary."""
        timestamp = datetime(2024, 1, 15, 10, 30, 0, tzinfo=timezone.utc)
        context = SecurityContext(
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="req-123",
            trace_id="trace-456",
            timestamp=timestamp,
            claims={"role": "user"},
        )

        result = context.to_dict()

        assert result["user_id"] == "user@example.com"
        assert result["client_id"] == "192.168.1.1"
        assert result["request_id"] == "req-123"
        assert result["trace_id"] == "trace-456"
        assert result["timestamp"] == "2024-01-15T10:30:00+00:00"
        assert result["claims"] == {"role": "user"}

    def test_from_dict(self):
        """Test deserialization from dictionary."""
        data = {
            "user_id": "user@example.com",
            "client_id": "192.168.1.1",
            "request_id": "req-123",
            "trace_id": "trace-456",
            "session_id": "session-789",
            "user_agent": "TestAgent/1.0",
            "timestamp": "2024-01-15T10:30:00+00:00",
            "claims": {"role": "admin"},
        }

        context = SecurityContext.from_dict(data)

        assert context.user_id == "user@example.com"
        assert context.client_id == "192.168.1.1"
        assert context.request_id == "req-123"
        assert context.trace_id == "trace-456"
        assert context.session_id == "session-789"
        assert context.user_agent == "TestAgent/1.0"
        assert context.claims == {"role": "admin"}

    def test_roundtrip_serialization(self):
        """Test that to_dict and from_dict are inverse operations."""
        original = SecurityContext(
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="req-123",
            trace_id="trace-456",
            claims={"key": "value"},
        )

        restored = SecurityContext.from_dict(original.to_dict())

        assert restored.user_id == original.user_id
        assert restored.client_id == original.client_id
        assert restored.request_id == original.request_id
        assert restored.trace_id == original.trace_id
        assert restored.claims == original.claims


class TestSecurityContextBuilder:
    """Tests for the SecurityContextBuilder."""

    def test_build_with_required_fields(self):
        """Test building context with required fields."""
        context = (
            SecurityContextBuilder()
            .with_user_id("user@example.com")
            .with_client_id("192.168.1.1")
            .with_request_id("req-123")
            .build()
        )

        assert context.user_id == "user@example.com"
        assert context.client_id == "192.168.1.1"
        assert context.request_id == "req-123"

    def test_build_generates_request_id_if_not_provided(self):
        """Test that request_id is auto-generated if not provided."""
        context = (
            SecurityContextBuilder()
            .with_user_id("user@example.com")
            .with_client_id("192.168.1.1")
            .with_request_id()
            .build()
        )

        assert context.request_id is not None
        assert len(context.request_id) == 36

    def test_build_with_all_fields(self):
        """Test building context with all fields."""
        timestamp = datetime.now(timezone.utc)

        context = (
            SecurityContextBuilder()
            .with_user_id("user@example.com")
            .with_client_id("192.168.1.1")
            .with_request_id("req-123")
            .with_trace_id("trace-456")
            .with_session_id("session-789")
            .with_user_agent("TestAgent/1.0")
            .with_timestamp(timestamp)
            .with_claims({"role": "admin"})
            .build()
        )

        assert context.user_id == "user@example.com"
        assert context.client_id == "192.168.1.1"
        assert context.request_id == "req-123"
        assert context.trace_id == "trace-456"
        assert context.session_id == "session-789"
        assert context.user_agent == "TestAgent/1.0"
        assert context.timestamp == timestamp
        assert context.claims == {"role": "admin"}

    def test_add_claim(self):
        """Test adding individual claims."""
        context = (
            SecurityContextBuilder()
            .with_user_id("user@example.com")
            .with_client_id("192.168.1.1")
            .add_claim("role", "admin")
            .add_claim("department", "engineering")
            .build()
        )

        assert context.claims["role"] == "admin"
        assert context.claims["department"] == "engineering"

    def test_build_fails_without_user_id(self):
        """Test that build fails without user_id."""
        with pytest.raises(ValueError, match="user_id is required"):
            SecurityContextBuilder().with_client_id("192.168.1.1").build()

    def test_build_fails_without_client_id(self):
        """Test that build fails without client_id."""
        with pytest.raises(ValueError, match="client_id is required"):
            SecurityContextBuilder().with_user_id("user@example.com").build()

    def test_fluent_interface(self):
        """Test that builder methods return self for chaining."""
        builder = SecurityContextBuilder()

        assert builder.with_user_id("user") is builder
        assert builder.with_client_id("client") is builder
        assert builder.with_request_id("req") is builder
        assert builder.with_trace_id("trace") is builder
        assert builder.with_session_id("session") is builder
        assert builder.with_user_agent("agent") is builder
        assert builder.with_claims({}) is builder
        assert builder.add_claim("key", "value") is builder


class TestContextVars:
    """Tests for context variable management."""

    def test_set_and_get_current_context(self):
        """Test setting and getting current context."""
        context = SecurityContext(
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="req-123",
        )

        set_current_context(context)
        retrieved = get_current_context()

        assert retrieved is context

        clear_current_context()

    def test_get_current_context_returns_none_when_not_set(self):
        """Test that get_current_context returns None when not set."""
        clear_current_context()
        assert get_current_context() is None

    def test_clear_current_context(self):
        """Test clearing the current context."""
        context = SecurityContext(
            user_id="user@example.com",
            client_id="192.168.1.1",
            request_id="req-123",
        )

        set_current_context(context)
        clear_current_context()

        assert get_current_context() is None
