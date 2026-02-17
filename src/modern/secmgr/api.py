"""
REST API for the Security Manager service.

Provides HTTP endpoints that mirror the COBOL SECMGR COMMAREA interface.
Callers (like the modernized INQONLN) invoke these endpoints instead of
EXEC CICS LINK PROGRAM('SECMGR').

HTTP status code mapping:
  COBOL SEC-RESPONSE-CODE 0  -> 200 OK
  COBOL SEC-RESPONSE-CODE 8  -> 401 Unauthorized / 403 Forbidden
  COBOL SEC-RESPONSE-CODE 12 -> 500 Internal Server Error
"""

from __future__ import annotations

import logging
from typing import Optional

from fastapi import APIRouter, Depends, Header, HTTPException, Request
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from .context import SecurityContext, clear_security_context, set_security_context
from .database import get_db
from .exceptions import (
    AuditException,
    AuthorizationException,
    SecurityError,
    ValidationException,
)
from .responses import SecurityResponse
from .service import SecurityManager

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/security", tags=["security"])


class SecurityRequest(BaseModel):
    """Request body matching the COBOL SECURITY-REQUEST-AREA structure.

    Fields:
        request_type: 'V' (validate), 'A' (authorize), or 'L' (audit).
        user_id: The user to validate/authorize (SEC-USER-ID).
        resource_name: The resource being accessed (SEC-RESOURCE-NAME).
        access_type: The access type requested (SEC-ACCESS-TYPE).
    """

    request_type: str = Field(
        ...,
        pattern="^[VAL]$",
        description="V=Validate, A=Authorize, L=Audit",
    )
    user_id: str = Field(..., max_length=8)
    resource_name: str = Field(default="", max_length=8)
    access_type: str = Field(default="", max_length=8)


class SecurityChainRequest(BaseModel):
    """Request body for the full Validate -> Authorize -> Audit chain."""

    user_id: str = Field(..., max_length=8)
    resource_name: str = Field(..., max_length=8)
    access_type: str = Field(..., max_length=8)


class SecurityResponseModel(BaseModel):
    """Response model matching the COBOL response structure."""

    code: int = Field(description="0=success, 8=denied, 12=error")
    success: bool
    error_info: Optional[str] = None
    request_type: str = ""
    user_id: str = ""
    resource_name: str = ""
    access_type: str = ""


def _build_context(
    request: Request,
    user_id: str,
    x_terminal_id: str = Header(default="HTTP", alias="X-Terminal-ID"),
    x_transaction_id: str = Header(default="REST", alias="X-Transaction-ID"),
    x_program_name: str = Header(default="UNKNOWN", alias="X-Program-Name"),
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-ID"),
) -> SecurityContext:
    """Build a SecurityContext from HTTP headers.

    Replaces EXEC CICS ASSIGN by extracting context from HTTP request headers.
    In production, the user_id would come from a validated JWT token.
    """
    ctx = SecurityContext(
        user_id=user_id,
        terminal_id=x_terminal_id,
        transaction_id=x_transaction_id,
        program_name=x_program_name,
        trace_id=x_trace_id or request.headers.get("traceparent"),
    )
    set_security_context(ctx)
    return ctx


def _handle_security_error(exc: SecurityError) -> None:
    """Convert SecurityError exceptions to appropriate HTTP responses."""
    if isinstance(exc, ValidationException):
        raise HTTPException(status_code=401, detail=exc.message)
    elif isinstance(exc, AuthorizationException):
        raise HTTPException(status_code=403, detail=exc.message)
    elif isinstance(exc, AuditException):
        raise HTTPException(status_code=500, detail=exc.message)
    else:
        raise HTTPException(status_code=500, detail=exc.message)


def _to_response_model(resp: SecurityResponse) -> SecurityResponseModel:
    """Convert internal SecurityResponse to API response model."""
    return SecurityResponseModel(**resp.to_dict())


@router.post(
    "/process",
    response_model=SecurityResponseModel,
    summary="Process a single security request",
    description=(
        "Dispatches a security request by type, mirroring the COBOL "
        "EVALUATE block in SECMGR PROCEDURE DIVISION."
    ),
)
def process_request(
    body: SecurityRequest,
    request: Request,
    x_terminal_id: str = Header(default="HTTP", alias="X-Terminal-ID"),
    x_transaction_id: str = Header(default="REST", alias="X-Transaction-ID"),
    x_program_name: str = Header(default="UNKNOWN", alias="X-Program-Name"),
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-ID"),
    db: Session = Depends(get_db),
) -> SecurityResponseModel:
    ctx = _build_context(
        request,
        body.user_id,
        x_terminal_id,
        x_transaction_id,
        x_program_name,
        x_trace_id,
    )
    try:
        mgr = SecurityManager(db)
        resp = mgr.process_request(
            request_type=body.request_type,
            context=ctx,
            resource_name=body.resource_name,
            access_type=body.access_type,
        )
        return _to_response_model(resp)
    except SecurityError as exc:
        _handle_security_error(exc)
        raise
    finally:
        clear_security_context()


@router.post(
    "/validate",
    response_model=SecurityResponseModel,
    summary="Validate user credentials",
    description="Corresponds to COBOL P100-VALIDATE-USER.",
)
def validate_user(
    body: SecurityRequest,
    request: Request,
    x_terminal_id: str = Header(default="HTTP", alias="X-Terminal-ID"),
    x_transaction_id: str = Header(default="REST", alias="X-Transaction-ID"),
    x_program_name: str = Header(default="UNKNOWN", alias="X-Program-Name"),
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-ID"),
    db: Session = Depends(get_db),
) -> SecurityResponseModel:
    ctx = _build_context(
        request,
        body.user_id,
        x_terminal_id,
        x_transaction_id,
        x_program_name,
        x_trace_id,
    )
    try:
        mgr = SecurityManager(db)
        resp = mgr.validate_user(ctx)
        return _to_response_model(resp)
    except SecurityError as exc:
        _handle_security_error(exc)
        raise
    finally:
        clear_security_context()


@router.post(
    "/authorize",
    response_model=SecurityResponseModel,
    summary="Check user authorization",
    description="Corresponds to COBOL P200-CHECK-AUTH.",
)
def authorize_access(
    body: SecurityRequest,
    request: Request,
    x_terminal_id: str = Header(default="HTTP", alias="X-Terminal-ID"),
    x_transaction_id: str = Header(default="REST", alias="X-Transaction-ID"),
    x_program_name: str = Header(default="UNKNOWN", alias="X-Program-Name"),
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-ID"),
    db: Session = Depends(get_db),
) -> SecurityResponseModel:
    ctx = _build_context(
        request,
        body.user_id,
        x_terminal_id,
        x_transaction_id,
        x_program_name,
        x_trace_id,
    )
    try:
        mgr = SecurityManager(db)
        resp = mgr.authorize_access(ctx, body.resource_name, body.access_type)
        return _to_response_model(resp)
    except SecurityError as exc:
        _handle_security_error(exc)
        raise
    finally:
        clear_security_context()


@router.post(
    "/audit",
    response_model=SecurityResponseModel,
    summary="Log an access attempt",
    description="Corresponds to COBOL P300-LOG-ACCESS.",
)
def audit_access(
    body: SecurityRequest,
    request: Request,
    x_terminal_id: str = Header(default="HTTP", alias="X-Terminal-ID"),
    x_transaction_id: str = Header(default="REST", alias="X-Transaction-ID"),
    x_program_name: str = Header(default="UNKNOWN", alias="X-Program-Name"),
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-ID"),
    db: Session = Depends(get_db),
) -> SecurityResponseModel:
    ctx = _build_context(
        request,
        body.user_id,
        x_terminal_id,
        x_transaction_id,
        x_program_name,
        x_trace_id,
    )
    try:
        mgr = SecurityManager(db)
        resp = mgr.audit_access(ctx, body.resource_name, body.access_type)
        return _to_response_model(resp)
    except SecurityError as exc:
        _handle_security_error(exc)
        raise
    finally:
        clear_security_context()


@router.post(
    "/check",
    response_model=SecurityResponseModel,
    summary="Execute full security chain (Validate -> Authorize -> Audit)",
    description=(
        "Executes the complete three-phase security check, mirroring "
        "INQONLN.cbl P050-SECURITY-CHECK."
    ),
)
def security_check(
    body: SecurityChainRequest,
    request: Request,
    x_terminal_id: str = Header(default="HTTP", alias="X-Terminal-ID"),
    x_transaction_id: str = Header(default="REST", alias="X-Transaction-ID"),
    x_program_name: str = Header(default="UNKNOWN", alias="X-Program-Name"),
    x_trace_id: Optional[str] = Header(default=None, alias="X-Trace-ID"),
    db: Session = Depends(get_db),
) -> SecurityResponseModel:
    ctx = _build_context(
        request,
        body.user_id,
        x_terminal_id,
        x_transaction_id,
        x_program_name,
        x_trace_id,
    )
    try:
        mgr = SecurityManager(db)
        resp = mgr.execute_security_chain(
            context=ctx,
            resource_name=body.resource_name,
            access_type=body.access_type,
        )
        return _to_response_model(resp)
    except SecurityError as exc:
        _handle_security_error(exc)
        raise
    finally:
        clear_security_context()


@router.get(
    "/health",
    summary="Health check",
)
def health_check() -> dict[str, str]:
    return {"status": "healthy", "service": "secmgr"}
