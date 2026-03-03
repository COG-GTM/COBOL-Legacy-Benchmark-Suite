"""FastAPI routers - replaces INQONLN, INQPORT, INQHIST CICS programs.

Maps CICS BMS screen interactions to REST API endpoints with JSON responses.

COBOL program mapping:
- INQONLN (MENU/INQP/INQH/EXIT) -> auth_router + inquiry_router
- INQPORT (portfolio/position inquiry) -> GET /portfolios/{id}, /positions
- INQHIST (history inquiry with cursor) -> GET /portfolios/{id}/history
"""

import logging
from typing import Any

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field

logger = logging.getLogger("portfolio.online.routers")


# ---------------------------------------------------------------------------
# Request/Response Models (replace BMS maps INQMAP1/INQMAP2/INQMAP3)
# ---------------------------------------------------------------------------


class LoginRequest(BaseModel):
    """Login request - replaces INQMAP1 user/password fields."""

    user_id: str = Field(max_length=8)
    password: str


class LoginResponse(BaseModel):
    """Login response with JWT token."""

    user_id: str
    token: str
    role: str


class PortfolioResponse(BaseModel):
    """Portfolio inquiry response - replaces INQMAP2 portfolio display."""

    portfolio_id: str
    account_no: str = ""
    client_name: str = ""
    client_type: str = ""
    status: str = ""
    total_value: float = 0.0
    cash_balance: float = 0.0
    create_date: str = ""


class PositionResponse(BaseModel):
    """Position detail - replaces INQMAP2 position lines."""

    portfolio_id: str
    investment_id: str
    quantity: float
    cost_basis: float
    market_value: float
    gain_loss: float
    currency: str
    status: str


class HistoryResponse(BaseModel):
    """History inquiry response - replaces INQMAP3 history display."""

    portfolio_id: str
    records: list[dict[str, Any]]
    total_records: int
    page: int
    page_size: int
    has_more: bool


class ReportResponse(BaseModel):
    """Report generation response."""

    report_type: str
    generated_at: str
    content: str | None = None
    data: dict[str, Any] | None = None


# ---------------------------------------------------------------------------
# Auth Router (replaces SECMGR validate/authorize)
# ---------------------------------------------------------------------------

auth_router = APIRouter()


@auth_router.post("/login", response_model=LoginResponse)
async def login(request: LoginRequest) -> LoginResponse:
    """User login - replaces INQONLN MENU function + SECMGR P100-VALIDATE-USER.

    COBOL flow:
    1. User enters ID/password on INQMAP1
    2. INQONLN calls SECMGR with function 'V'
    3. SECMGR validates against security file
    """
    # In production, this would use SecurityManager.validate_user()
    # For now, return a placeholder response
    return LoginResponse(
        user_id=request.user_id,
        token="placeholder-jwt-token",
        role="USER",
    )


@auth_router.post("/logout")
async def logout() -> dict[str, str]:
    """User logout - replaces INQONLN EXIT function.

    COBOL: EXEC CICS RETURN END-EXEC
    """
    return {"status": "logged_out", "message": "Session ended"}


# ---------------------------------------------------------------------------
# Inquiry Router (replaces INQPORT + INQHIST)
# ---------------------------------------------------------------------------

inquiry_router = APIRouter()


@inquiry_router.get("/portfolios/{portfolio_id}", response_model=PortfolioResponse)
async def get_portfolio(portfolio_id: str) -> PortfolioResponse:
    """Portfolio inquiry - replaces INQPORT.cbl.

    COBOL flow:
    1. INQONLN receives INQP function
    2. Calls INQPORT via EXEC CICS LINK
    3. INQPORT reads POSFILE VSAM (EXEC CICS READ)
    4. Returns data in DFHCOMMAREA
    """
    # In production, this would query PostgreSQL via SQLAlchemy
    # For now, return a placeholder
    return PortfolioResponse(
        portfolio_id=portfolio_id,
        account_no="",
        client_name="",
        status="ACTIVE",
    )


@inquiry_router.get("/portfolios/{portfolio_id}/positions", response_model=list[PositionResponse])
async def get_positions(portfolio_id: str) -> list[PositionResponse]:
    """Position inquiry - replaces INQPORT position reading.

    COBOL: EXEC CICS READ FILE('POSFILE')
           INTO(WS-POS-RECORD) RIDFLD(WS-POS-KEY) END-EXEC
    """
    # In production, query positions from PostgreSQL
    return []


@inquiry_router.get("/portfolios/{portfolio_id}/history", response_model=HistoryResponse)
async def get_history(
    portfolio_id: str,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=10, ge=1, le=100),
) -> HistoryResponse:
    """History inquiry - replaces INQHIST.cbl with cursor-based pagination.

    COBOL flow:
    1. INQONLN receives INQH function
    2. Calls INQHIST via EXEC CICS LINK
    3. INQHIST calls CURSMGR to declare/open cursor
    4. Fetches array of 10 rows (WS-FETCH-SIZE)
    5. Returns data in DFHCOMMAREA

    Python: Uses LIMIT/OFFSET pagination replacing CURSMGR cursor pattern.
    The COBOL cursor.fetchmany(n) pattern maps to SQL LIMIT/OFFSET.
    """
    # In production, query history from PostgreSQL with pagination
    return HistoryResponse(
        portfolio_id=portfolio_id,
        records=[],
        total_records=0,
        page=page,
        page_size=page_size,
        has_more=False,
    )


# ---------------------------------------------------------------------------
# Reports Router
# ---------------------------------------------------------------------------

reports_router = APIRouter()


@reports_router.get("/position", response_model=ReportResponse)
async def position_report(report_date: str = "") -> ReportResponse:
    """Position report endpoint - provides RPTPOS00 output via API."""
    return ReportResponse(
        report_type="POSITION",
        generated_at="",
        data={"message": "Use batch pipeline to generate position reports"},
    )


@reports_router.get("/audit", response_model=ReportResponse)
async def audit_report(report_date: str = "") -> ReportResponse:
    """Audit report endpoint - provides RPTAUD00 output via API."""
    return ReportResponse(
        report_type="AUDIT",
        generated_at="",
        data={"message": "Use batch pipeline to generate audit reports"},
    )


@reports_router.get("/statistics", response_model=ReportResponse)
async def statistics_report(report_date: str = "") -> ReportResponse:
    """Statistics report endpoint - provides RPTSTA00 output via API."""
    return ReportResponse(
        report_type="STATISTICS",
        generated_at="",
        data={"message": "Use batch pipeline to generate statistics reports"},
    )
