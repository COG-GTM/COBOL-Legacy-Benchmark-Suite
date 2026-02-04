"""Tests for context extractors."""

import pytest
import base64
import json
from datetime import datetime, timezone, timedelta
from unittest.mock import Mock

from ..extractors import (
    JWTExtractor,
    OAuth2Extractor,
    SessionExtractor,
    HeaderExtractor,
    CompositeExtractor,
)


class MockRequest:
    """Mock HTTP request for testing extractors."""

    def __init__(
        self,
        headers: dict = None,
        cookies: dict = None,
        client_host: str = None,
    ):
        self._headers = headers or {}
        self._cookies = cookies or {}
        self._client = Mock()
        self._client.host = client_host

    @property
    def headers(self):
        return self._headers

    @property
    def cookies(self):
        return self._cookies

    @property
    def client(self):
        return self._client


def create_jwt_token(payload: dict, header: dict = None) -> str:
    """Create a mock JWT token for testing."""
    header = header or {"alg": "HS256", "typ": "JWT"}

    def b64_encode(data: dict) -> str:
        json_bytes = json.dumps(data).encode()
        return base64.urlsafe_b64encode(json_bytes).rstrip(b"=").decode()

    header_b64 = b64_encode(header)
    payload_b64 = b64_encode(payload)
    signature = "mock_signature"

    return f"{header_b64}.{payload_b64}.{signature}"


class TestJWTExtractor:
    """Tests for the JWTExtractor."""

    def test_can_extract_with_bearer_token(self):
        """Test that extractor recognizes Bearer tokens."""
        token = create_jwt_token({"sub": "user123"})
        request = MockRequest(headers={"Authorization": f"Bearer {token}"})

        extractor = JWTExtractor(verify_signature=False)

        assert extractor.can_extract(request) is True

    def test_can_extract_without_token(self):
        """Test that extractor returns False without token."""
        request = MockRequest(headers={})

        extractor = JWTExtractor()

        assert extractor.can_extract(request) is False

    def test_extract_user_from_sub_claim(self):
        """Test extracting user ID from 'sub' claim."""
        token = create_jwt_token({"sub": "user123", "email": "user@example.com"})
        request = MockRequest(headers={"Authorization": f"Bearer {token}"})

        extractor = JWTExtractor(verify_signature=False)
        result = extractor.extract(request)

        assert result is not None
        assert result["user_id"] == "user123"

    def test_extract_user_from_email_claim(self):
        """Test extracting user ID from 'email' claim when 'sub' is missing."""
        token = create_jwt_token({"email": "user@example.com"})
        request = MockRequest(headers={"Authorization": f"Bearer {token}"})

        extractor = JWTExtractor(verify_signature=False)
        result = extractor.extract(request)

        assert result is not None
        assert result["user_id"] == "user@example.com"

    def test_extract_identity_with_roles(self):
        """Test extracting identity with roles."""
        token = create_jwt_token(
            {
                "sub": "user123",
                "email": "user@example.com",
                "name": "Test User",
                "roles": ["admin", "user"],
                "groups": ["engineering"],
            }
        )
        request = MockRequest(headers={"Authorization": f"Bearer {token}"})

        extractor = JWTExtractor(verify_signature=False)
        result = extractor.extract(request)

        assert result is not None
        identity = result["identity"]
        assert identity.user_id == "user123"
        assert identity.email == "user@example.com"
        assert identity.display_name == "Test User"
        assert "admin" in identity.roles
        assert "engineering" in identity.groups

    def test_extract_from_cookie(self):
        """Test extracting token from cookie."""
        token = create_jwt_token({"sub": "user123"})
        request = MockRequest(
            headers={},
            cookies={"auth_token": token},
        )

        extractor = JWTExtractor(
            verify_signature=False,
            cookie_name="auth_token",
        )

        assert extractor.can_extract(request) is True
        result = extractor.extract(request)
        assert result["user_id"] == "user123"

    def test_extract_with_custom_header(self):
        """Test extracting token from custom header."""
        token = create_jwt_token({"sub": "user123"})
        request = MockRequest(headers={"X-Auth-Token": f"Token {token}"})

        extractor = JWTExtractor(
            verify_signature=False,
            header_name="X-Auth-Token",
            header_prefix="Token",
        )

        result = extractor.extract(request)
        assert result["user_id"] == "user123"

    def test_extract_returns_none_for_invalid_token(self):
        """Test that extraction returns None for invalid tokens."""
        request = MockRequest(headers={"Authorization": "Bearer invalid.token"})

        extractor = JWTExtractor(verify_signature=False)
        result = extractor.extract(request)

        assert result is None

    def test_extract_timestamps(self):
        """Test extracting issued_at and expires_at timestamps."""
        now = datetime.now(timezone.utc)
        iat = int(now.timestamp())
        exp = int((now + timedelta(hours=1)).timestamp())

        token = create_jwt_token({"sub": "user123", "iat": iat, "exp": exp})
        request = MockRequest(headers={"Authorization": f"Bearer {token}"})

        extractor = JWTExtractor(verify_signature=False)
        result = extractor.extract(request)

        assert result is not None
        identity = result["identity"]
        assert identity.issued_at is not None
        assert identity.expires_at is not None


class TestSessionExtractor:
    """Tests for the SessionExtractor."""

    def test_can_extract_with_session_cookie(self):
        """Test that extractor recognizes session cookies."""
        request = MockRequest(cookies={"session": "session123"})

        extractor = SessionExtractor(
            session_store={"session123": {"user_id": "user123"}}
        )

        assert extractor.can_extract(request) is True

    def test_can_extract_without_session_cookie(self):
        """Test that extractor returns False without session cookie."""
        request = MockRequest(cookies={})

        extractor = SessionExtractor()

        assert extractor.can_extract(request) is False

    def test_extract_user_from_session(self):
        """Test extracting user from session store."""
        session_store = {
            "session123": {
                "user_id": "user123",
                "email": "user@example.com",
            }
        }
        request = MockRequest(cookies={"session": "session123"})

        extractor = SessionExtractor(session_store=session_store)
        result = extractor.extract(request)

        assert result is not None
        assert result["user_id"] == "user123"
        assert result["session_id"] == "session123"

    def test_extract_returns_none_for_invalid_session(self):
        """Test that extraction returns None for invalid sessions."""
        request = MockRequest(cookies={"session": "invalid_session"})

        extractor = SessionExtractor(session_store={})
        result = extractor.extract(request)

        assert result is None

    def test_custom_cookie_name(self):
        """Test using custom session cookie name."""
        session_store = {"my_session": {"user_id": "user123"}}
        request = MockRequest(cookies={"my_cookie": "my_session"})

        extractor = SessionExtractor(
            session_cookie_name="my_cookie",
            session_store=session_store,
        )

        result = extractor.extract(request)
        assert result["user_id"] == "user123"


class TestHeaderExtractor:
    """Tests for the HeaderExtractor."""

    def test_can_extract_always_true(self):
        """Test that header extractor can always extract some context."""
        request = MockRequest()

        extractor = HeaderExtractor()

        assert extractor.can_extract(request) is True

    def test_extract_request_id(self):
        """Test extracting request ID from X-Request-ID header."""
        request = MockRequest(headers={"X-Request-ID": "req-123"})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["request_id"] == "req-123"

    def test_extract_correlation_id(self):
        """Test extracting request ID from X-Correlation-ID header."""
        request = MockRequest(headers={"X-Correlation-ID": "corr-456"})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["request_id"] == "corr-456"

    def test_generate_request_id_if_missing(self):
        """Test that request ID is generated if not in headers."""
        request = MockRequest(headers={})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["request_id"] is not None
        assert len(result["request_id"]) == 36

    def test_extract_client_ip_from_forwarded_for(self):
        """Test extracting client IP from X-Forwarded-For header."""
        request = MockRequest(headers={"X-Forwarded-For": "10.0.0.1, 192.168.1.1"})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["client_id"] == "10.0.0.1"

    def test_extract_client_ip_from_real_ip(self):
        """Test extracting client IP from X-Real-IP header."""
        request = MockRequest(headers={"X-Real-IP": "10.0.0.2"})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["client_id"] == "10.0.0.2"

    def test_extract_client_ip_from_connection(self):
        """Test extracting client IP from connection info."""
        request = MockRequest(client_host="192.168.1.100")

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["client_id"] == "192.168.1.100"

    def test_extract_trace_id_from_traceparent(self):
        """Test extracting trace ID from W3C traceparent header."""
        traceparent = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"
        request = MockRequest(headers={"traceparent": traceparent})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["trace_id"] == "0af7651916cd43dd8448eb211c80319c"

    def test_extract_trace_id_from_custom_header(self):
        """Test extracting trace ID from X-Trace-ID header."""
        request = MockRequest(headers={"X-Trace-ID": "trace-789"})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["trace_id"] == "trace-789"

    def test_extract_user_agent(self):
        """Test extracting User-Agent header."""
        request = MockRequest(headers={"User-Agent": "Mozilla/5.0 (Test)"})

        extractor = HeaderExtractor()
        result = extractor.extract(request)

        assert result["user_agent"] == "Mozilla/5.0 (Test)"


class TestCompositeExtractor:
    """Tests for the CompositeExtractor."""

    def test_combines_multiple_extractors(self):
        """Test that composite extractor combines results from multiple extractors."""
        token = create_jwt_token({"sub": "user123"})
        request = MockRequest(
            headers={
                "Authorization": f"Bearer {token}",
                "X-Request-ID": "req-123",
                "User-Agent": "TestAgent/1.0",
            },
            client_host="192.168.1.1",
        )

        extractor = CompositeExtractor(
            [
                JWTExtractor(verify_signature=False),
                HeaderExtractor(),
            ]
        )

        result = extractor.extract(request)

        assert result["user_id"] == "user123"
        assert result["request_id"] == "req-123"
        assert result["client_id"] == "192.168.1.1"
        assert result["user_agent"] == "TestAgent/1.0"

    def test_build_context(self):
        """Test building a complete SecurityContext."""
        token = create_jwt_token({"sub": "user123"})
        request = MockRequest(
            headers={
                "Authorization": f"Bearer {token}",
                "X-Request-ID": "req-123",
                "X-Trace-ID": "trace-456",
            },
            client_host="192.168.1.1",
        )

        extractor = CompositeExtractor(
            [
                JWTExtractor(verify_signature=False),
                HeaderExtractor(),
            ]
        )

        context = extractor.build_context(request)

        assert context is not None
        assert context.user_id == "user123"
        assert context.client_id == "192.168.1.1"
        assert context.request_id == "req-123"
        assert context.trace_id == "trace-456"

    def test_build_context_returns_none_without_user(self):
        """Test that build_context returns None if no user can be extracted."""
        request = MockRequest(
            headers={"X-Request-ID": "req-123"},
            client_host="192.168.1.1",
        )

        extractor = CompositeExtractor([HeaderExtractor()])

        context = extractor.build_context(request)

        assert context is None

    def test_can_extract_any(self):
        """Test that can_extract returns True if any extractor can extract."""
        request = MockRequest(headers={"X-Request-ID": "req-123"})

        extractor = CompositeExtractor(
            [
                JWTExtractor(),
                HeaderExtractor(),
            ]
        )

        assert extractor.can_extract(request) is True
