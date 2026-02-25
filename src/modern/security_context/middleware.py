"""
FastAPI middleware for security context propagation.

This module provides middleware that automatically extracts security context
from incoming requests and makes it available throughout the request lifecycle
via dependency injection.

This replaces the implicit CICS context that was available to all programs
in the mainframe environment with explicit, typed context injection.
"""

from __future__ import annotations

import time
import uuid
from typing import Any, Callable, Optional

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from .context import (
    SecurityContext,
    SecurityContextBuilder,
    set_current_context,
    get_current_context,
    clear_current_context,
)
from .extractors import (
    CompositeExtractor,
    ContextExtractor,
    HeaderExtractor,
    JWTExtractor,
)


class SecurityContextMiddleware(BaseHTTPMiddleware):
    """
    Middleware that extracts and propagates security context for each request.

    This middleware:
    1. Extracts user identity from JWT/OAuth2/session
    2. Captures client identifier from IP/headers
    3. Generates or extracts request correlation ID
    4. Sets the context in thread-local/async-local storage
    5. Adds context headers to the response

    Usage:
        app = FastAPI()
        app.add_middleware(
            SecurityContextMiddleware,
            extractors=[JWTExtractor(secret_key="..."), HeaderExtractor()]
        )
    """

    def __init__(
        self,
        app: Any,
        extractors: Optional[list[ContextExtractor]] = None,
        require_auth: bool = False,
        anonymous_user_id: str = "anonymous",
        response_headers: bool = True,
    ):
        """
        Initialize the security context middleware.

        Args:
            app: The ASGI application.
            extractors: List of context extractors to use.
            require_auth: If True, reject requests without valid authentication.
            anonymous_user_id: User ID to use for unauthenticated requests.
            response_headers: If True, add context headers to responses.
        """
        super().__init__(app)
        self.extractors = extractors or [JWTExtractor(), HeaderExtractor()]
        self.composite_extractor = CompositeExtractor(self.extractors)
        self.require_auth = require_auth
        self.anonymous_user_id = anonymous_user_id
        self.response_headers = response_headers

    async def dispatch(
        self, request: Request, call_next: Callable[[Request], Response]
    ) -> Response:
        """Process the request and propagate security context."""
        start_time = time.time()

        context = self._build_context(request)

        if context is None and self.require_auth:
            from starlette.responses import JSONResponse

            return JSONResponse(
                status_code=401,
                content={"error": "Authentication required"},
                headers={"WWW-Authenticate": "Bearer"},
            )

        if context is None:
            context = self._build_anonymous_context(request)

        set_current_context(context)

        request.state.security_context = context

        try:
            response = await call_next(request)

            if self.response_headers and context:
                response.headers["X-Request-ID"] = context.request_id
                if context.trace_id:
                    response.headers["X-Trace-ID"] = context.trace_id

            duration_ms = int((time.time() - start_time) * 1000)
            response.headers["X-Response-Time"] = f"{duration_ms}ms"

            return response
        finally:
            clear_current_context()

    def _build_context(self, request: Request) -> Optional[SecurityContext]:
        """Build security context from request using extractors."""
        return self.composite_extractor.build_context(request)

    def _build_anonymous_context(self, request: Request) -> SecurityContext:
        """Build context for anonymous/unauthenticated requests."""
        header_extractor = HeaderExtractor()
        extracted = header_extractor.extract(request) or {}

        client_id = extracted.get("client_id")
        if not client_id and hasattr(request, "client") and request.client:
            client_id = request.client.host
        if not client_id:
            client_id = "unknown"

        builder = SecurityContextBuilder()
        builder.with_user_id(self.anonymous_user_id)
        builder.with_client_id(client_id)
        builder.with_request_id(extracted.get("request_id", str(uuid.uuid4())))

        if "trace_id" in extracted:
            builder.with_trace_id(extracted["trace_id"])
        if "user_agent" in extracted:
            builder.with_user_agent(extracted["user_agent"])

        return builder.build()


def get_security_context(request: Request) -> SecurityContext:
    """
    FastAPI dependency for injecting security context.

    This provides the dependency injection pattern for passing context
    explicitly to security methods, replacing the implicit CICS context.

    Usage:
        @app.get("/portfolio/{portfolio_id}")
        async def get_portfolio(
            portfolio_id: str,
            context: SecurityContext = Depends(get_security_context)
        ):
            # context is now available with user_id, client_id, request_id
            ...

    Original COBOL pattern (INQONLN.cbl):
        EXEC CICS ASSIGN
                  USERID(SEC-USER-ID)
        END-EXEC.

    Modern pattern:
        context: SecurityContext = Depends(get_security_context)
        user_id = context.user_id
    """
    context = getattr(request.state, "security_context", None)
    if context is None:
        context = get_current_context()
    if context is None:
        raise ValueError("Security context not available. Is middleware configured?")
    return context


def get_optional_security_context(request: Request) -> Optional[SecurityContext]:
    """
    FastAPI dependency for optionally injecting security context.

    Returns None if no context is available instead of raising an error.
    """
    context = getattr(request.state, "security_context", None)
    if context is None:
        context = get_current_context()
    return context


class SecurityContextDependency:
    """
    Configurable dependency for security context injection.

    Allows customization of context requirements per endpoint.
    """

    def __init__(
        self,
        require_auth: bool = True,
        required_roles: Optional[list[str]] = None,
        required_permissions: Optional[list[str]] = None,
    ):
        self.require_auth = require_auth
        self.required_roles = required_roles or []
        self.required_permissions = required_permissions or []

    def __call__(self, request: Request) -> SecurityContext:
        """Get and validate security context."""
        context = getattr(request.state, "security_context", None)
        if context is None:
            context = get_current_context()

        if context is None:
            if self.require_auth:
                from fastapi import HTTPException

                raise HTTPException(
                    status_code=401,
                    detail="Authentication required",
                    headers={"WWW-Authenticate": "Bearer"},
                )
            raise ValueError("Security context not available")

        if self.required_roles:
            user_roles = context.claims.get("roles", [])
            if not any(role in user_roles for role in self.required_roles):
                from fastapi import HTTPException

                raise HTTPException(
                    status_code=403,
                    detail=f"Required roles: {self.required_roles}",
                )

        return context
