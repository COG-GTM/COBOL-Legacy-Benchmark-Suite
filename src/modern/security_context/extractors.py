"""
Context extractors for various authentication mechanisms.

This module provides extractors that can pull user identity and context
information from different sources:
- JWT tokens (Authorization: Bearer header)
- OAuth2 access tokens
- Session cookies
- HTTP headers (X-Request-ID, X-Forwarded-For, etc.)

These replace the implicit CICS ASSIGN commands that captured context
from the mainframe environment.
"""

from __future__ import annotations

import base64
import json
import re
from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Any, Optional, Protocol
import uuid

from .context import SecurityContext, SecurityContextBuilder
from .models import UserIdentity


class Request(Protocol):
    """Protocol for HTTP request objects (compatible with FastAPI, Flask, etc.)."""

    @property
    def headers(self) -> dict[str, str]:
        ...

    @property
    def cookies(self) -> dict[str, str]:
        ...

    @property
    def client(self) -> Any:
        ...


class ContextExtractor(ABC):
    """
    Abstract base class for context extractors.

    Each extractor is responsible for extracting specific pieces of
    security context from an HTTP request.
    """

    @abstractmethod
    def extract(self, request: Request) -> Optional[dict[str, Any]]:
        """
        Extract context information from the request.

        Returns:
            Dictionary with extracted context fields, or None if extraction fails.
        """
        pass

    @abstractmethod
    def can_extract(self, request: Request) -> bool:
        """
        Check if this extractor can handle the given request.

        Returns:
            True if the extractor can extract context from this request.
        """
        pass


class JWTExtractor(ContextExtractor):
    """
    Extracts user identity from JWT tokens.

    Supports tokens in:
    - Authorization: Bearer <token> header
    - Custom header (configurable)
    - Cookie (configurable)

    This replaces the CICS ASSIGN USERID command by extracting the
    authenticated user from a cryptographically signed token.
    """

    def __init__(
        self,
        secret_key: Optional[str] = None,
        public_key: Optional[str] = None,
        algorithms: list[str] = None,
        verify_signature: bool = True,
        header_name: str = "Authorization",
        header_prefix: str = "Bearer",
        cookie_name: Optional[str] = None,
        audience: Optional[str] = None,
        issuer: Optional[str] = None,
    ):
        self.secret_key = secret_key
        self.public_key = public_key
        self.algorithms = algorithms or ["HS256", "RS256"]
        self.verify_signature = verify_signature
        self.header_name = header_name
        self.header_prefix = header_prefix
        self.cookie_name = cookie_name
        self.audience = audience
        self.issuer = issuer

    def can_extract(self, request: Request) -> bool:
        """Check if request contains a JWT token."""
        if self._get_token_from_header(request):
            return True
        if self.cookie_name and self._get_token_from_cookie(request):
            return True
        return False

    def extract(self, request: Request) -> Optional[dict[str, Any]]:
        """Extract user identity from JWT token."""
        token = self._get_token_from_header(request)
        if not token and self.cookie_name:
            token = self._get_token_from_cookie(request)

        if not token:
            return None

        try:
            payload = self._decode_token(token)
            if payload is None:
                return None

            user_id = payload.get("sub") or payload.get("user_id") or payload.get("email")
            if not user_id:
                return None

            identity = UserIdentity(
                user_id=str(user_id),
                email=payload.get("email"),
                display_name=payload.get("name") or payload.get("display_name"),
                roles=payload.get("roles", []),
                groups=payload.get("groups", []),
                issuer=payload.get("iss"),
                subject=payload.get("sub"),
                audience=payload.get("aud"),
                issued_at=self._parse_timestamp(payload.get("iat")),
                expires_at=self._parse_timestamp(payload.get("exp")),
                token_type="JWT",
            )

            return {
                "user_id": identity.user_id,
                "identity": identity,
                "claims": payload,
            }
        except Exception:
            return None

    def _get_token_from_header(self, request: Request) -> Optional[str]:
        """Extract token from Authorization header."""
        auth_header = request.headers.get(self.header_name, "")
        if auth_header.startswith(f"{self.header_prefix} "):
            return auth_header[len(self.header_prefix) + 1 :]
        return None

    def _get_token_from_cookie(self, request: Request) -> Optional[str]:
        """Extract token from cookie."""
        if self.cookie_name:
            return request.cookies.get(self.cookie_name)
        return None

    def _decode_token(self, token: str) -> Optional[dict[str, Any]]:
        """
        Decode JWT token.

        For production use, this should use a proper JWT library like PyJWT.
        This implementation provides basic decoding for demonstration.
        """
        try:
            if self.verify_signature:
                try:
                    import jwt

                    key = self.public_key or self.secret_key
                    if not key:
                        return self._decode_without_verification(token)

                    return jwt.decode(
                        token,
                        key,
                        algorithms=self.algorithms,
                        audience=self.audience,
                        issuer=self.issuer,
                    )
                except ImportError:
                    return self._decode_without_verification(token)
            else:
                return self._decode_without_verification(token)
        except Exception:
            return None

    def _decode_without_verification(self, token: str) -> Optional[dict[str, Any]]:
        """Decode JWT without signature verification (for testing/development)."""
        try:
            parts = token.split(".")
            if len(parts) != 3:
                return None

            payload_b64 = parts[1]
            padding = 4 - len(payload_b64) % 4
            if padding != 4:
                payload_b64 += "=" * padding

            payload_json = base64.urlsafe_b64decode(payload_b64)
            return json.loads(payload_json)
        except Exception:
            return None

    def _parse_timestamp(self, ts: Any) -> Optional[datetime]:
        """Parse Unix timestamp to datetime."""
        if ts is None:
            return None
        try:
            return datetime.fromtimestamp(int(ts), tz=timezone.utc)
        except (ValueError, TypeError):
            return None


class OAuth2Extractor(ContextExtractor):
    """
    Extracts user identity from OAuth2 access tokens.

    Supports token introspection and userinfo endpoints for
    validating opaque access tokens.
    """

    def __init__(
        self,
        introspection_url: Optional[str] = None,
        userinfo_url: Optional[str] = None,
        client_id: Optional[str] = None,
        client_secret: Optional[str] = None,
        header_name: str = "Authorization",
        header_prefix: str = "Bearer",
    ):
        self.introspection_url = introspection_url
        self.userinfo_url = userinfo_url
        self.client_id = client_id
        self.client_secret = client_secret
        self.header_name = header_name
        self.header_prefix = header_prefix

    def can_extract(self, request: Request) -> bool:
        """Check if request contains an OAuth2 token."""
        auth_header = request.headers.get(self.header_name, "")
        return auth_header.startswith(f"{self.header_prefix} ")

    def extract(self, request: Request) -> Optional[dict[str, Any]]:
        """
        Extract user identity from OAuth2 token.

        In production, this would call the introspection or userinfo endpoint.
        """
        auth_header = request.headers.get(self.header_name, "")
        if not auth_header.startswith(f"{self.header_prefix} "):
            return None

        token = auth_header[len(self.header_prefix) + 1 :]

        if self.introspection_url:
            return self._introspect_token(token)
        elif self.userinfo_url:
            return self._get_userinfo(token)

        return None

    def _introspect_token(self, token: str) -> Optional[dict[str, Any]]:
        """
        Introspect OAuth2 token.

        In production, this would make an HTTP request to the introspection endpoint.
        """
        try:
            import httpx

            response = httpx.post(
                self.introspection_url,
                data={"token": token},
                auth=(self.client_id, self.client_secret) if self.client_id else None,
            )
            if response.status_code == 200:
                data = response.json()
                if data.get("active"):
                    return {
                        "user_id": data.get("sub") or data.get("username"),
                        "claims": data,
                    }
        except Exception:
            pass
        return None

    def _get_userinfo(self, token: str) -> Optional[dict[str, Any]]:
        """
        Get user info from OAuth2 userinfo endpoint.

        In production, this would make an HTTP request to the userinfo endpoint.
        """
        try:
            import httpx

            response = httpx.get(
                self.userinfo_url,
                headers={"Authorization": f"Bearer {token}"},
            )
            if response.status_code == 200:
                data = response.json()
                return {
                    "user_id": data.get("sub") or data.get("email"),
                    "claims": data,
                }
        except Exception:
            pass
        return None


class SessionExtractor(ContextExtractor):
    """
    Extracts user identity from session cookies.

    This provides compatibility with traditional session-based authentication
    while supporting the modern context propagation model.
    """

    def __init__(
        self,
        session_cookie_name: str = "session",
        session_store: Optional[dict[str, dict[str, Any]]] = None,
    ):
        self.session_cookie_name = session_cookie_name
        self.session_store = session_store or {}

    def can_extract(self, request: Request) -> bool:
        """Check if request contains a session cookie."""
        return self.session_cookie_name in request.cookies

    def extract(self, request: Request) -> Optional[dict[str, Any]]:
        """Extract user identity from session."""
        session_id = request.cookies.get(self.session_cookie_name)
        if not session_id:
            return None

        session_data = self.session_store.get(session_id)
        if not session_data:
            return None

        user_id = session_data.get("user_id")
        if not user_id:
            return None

        return {
            "user_id": user_id,
            "session_id": session_id,
            "claims": session_data,
        }


class HeaderExtractor(ContextExtractor):
    """
    Extracts context information from HTTP headers.

    Extracts:
    - Request ID from X-Request-ID or X-Correlation-ID
    - Client IP from X-Forwarded-For or X-Real-IP
    - Trace ID from traceparent (W3C) or X-Trace-ID
    - User Agent from User-Agent header

    This replaces the CICS ASSIGN TERMID and TRANSID commands.
    """

    REQUEST_ID_HEADERS = ["X-Request-ID", "X-Correlation-ID", "X-Request-Id"]
    CLIENT_IP_HEADERS = ["X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"]
    TRACE_ID_HEADERS = ["traceparent", "X-Trace-ID", "X-B3-TraceId"]

    def can_extract(self, request: Request) -> bool:
        """Header extractor can always extract some context."""
        return True

    def extract(self, request: Request) -> Optional[dict[str, Any]]:
        """Extract context from HTTP headers."""
        result: dict[str, Any] = {}

        request_id = self._extract_request_id(request)
        if request_id:
            result["request_id"] = request_id
        else:
            result["request_id"] = str(uuid.uuid4())

        client_id = self._extract_client_id(request)
        if client_id:
            result["client_id"] = client_id

        trace_id = self._extract_trace_id(request)
        if trace_id:
            result["trace_id"] = trace_id

        user_agent = request.headers.get("User-Agent")
        if user_agent:
            result["user_agent"] = user_agent

        return result

    def _extract_request_id(self, request: Request) -> Optional[str]:
        """Extract request ID from headers."""
        for header in self.REQUEST_ID_HEADERS:
            value = request.headers.get(header)
            if value:
                return value
        return None

    def _extract_client_id(self, request: Request) -> Optional[str]:
        """Extract client identifier from headers or connection info."""
        for header in self.CLIENT_IP_HEADERS:
            value = request.headers.get(header)
            if value:
                if "," in value:
                    return value.split(",")[0].strip()
                return value

        if hasattr(request, "client") and request.client:
            if hasattr(request.client, "host"):
                return request.client.host

        return None

    def _extract_trace_id(self, request: Request) -> Optional[str]:
        """Extract distributed trace ID from headers."""
        traceparent = request.headers.get("traceparent")
        if traceparent:
            match = re.match(r"^\d{2}-([a-f0-9]{32})-", traceparent)
            if match:
                return match.group(1)

        for header in self.TRACE_ID_HEADERS[1:]:
            value = request.headers.get(header)
            if value:
                return value

        return None


class CompositeExtractor(ContextExtractor):
    """
    Combines multiple extractors to build a complete security context.

    Tries each extractor in order and merges the results.
    """

    def __init__(self, extractors: list[ContextExtractor]):
        self.extractors = extractors

    def can_extract(self, request: Request) -> bool:
        """Check if any extractor can handle the request."""
        return any(e.can_extract(request) for e in self.extractors)

    def extract(self, request: Request) -> Optional[dict[str, Any]]:
        """Extract and merge context from all applicable extractors."""
        result: dict[str, Any] = {}

        for extractor in self.extractors:
            if extractor.can_extract(request):
                extracted = extractor.extract(request)
                if extracted:
                    result.update(extracted)

        return result if result else None

    def build_context(self, request: Request) -> Optional[SecurityContext]:
        """Build a complete SecurityContext from the request."""
        extracted = self.extract(request)
        if not extracted:
            return None

        user_id = extracted.get("user_id")
        client_id = extracted.get("client_id", "unknown")
        request_id = extracted.get("request_id", str(uuid.uuid4()))

        if not user_id:
            return None

        builder = SecurityContextBuilder()
        builder.with_user_id(user_id)
        builder.with_client_id(client_id)
        builder.with_request_id(request_id)

        if "trace_id" in extracted:
            builder.with_trace_id(extracted["trace_id"])
        if "session_id" in extracted:
            builder.with_session_id(extracted["session_id"])
        if "user_agent" in extracted:
            builder.with_user_agent(extracted["user_agent"])
        if "claims" in extracted:
            builder.with_claims(extracted["claims"])

        return builder.build()
