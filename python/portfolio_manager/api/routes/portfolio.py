"""Portfolio inquiry API routes.

Replaces:
  - INQPORT (src/programs/online/INQPORT.cbl) — portfolio position inquiry
  - INQONLN routing to INQPORT via EXEC CICS LINK
  - POSMAP BMS screen map -> JSON responses

Original COBOL flow (INQPORT.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE (check COMMAREA, set up error handling)
    2000-PROCESS-REQUEST
      2100-RECEIVE-INPUT (EXEC CICS RECEIVE MAP)
      2200-VALIDATE-INPUT
      2300-READ-PORTFOLIO (EXEC CICS READ FILE('POSFILE'))
      2400-FORMAT-OUTPUT (move data to POSMAP)
    3000-SEND-RESPONSE (EXEC CICS SEND MAP)
    P900-ERROR-ROUTINE (EXEC CICS HANDLE CONDITION)
"""

from __future__ import annotations

import logging
from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from portfolio_manager.models.database import InvestmentPosition, PortfolioMaster
from portfolio_manager.services.database import get_session
from portfolio_manager.services.error_handler import log_audit_event

logger = logging.getLogger(__name__)

router = APIRouter()


# ---------------------------------------------------------------------------
# Response models (replace BMS POSMAP screen map)
# ---------------------------------------------------------------------------


class PortfolioResponse(BaseModel):
    """Portfolio summary response.

    Replaces POSMAP output fields from INQPORT.
    """

    portfolio_id: str
    portfolio_name: str
    account_type: str
    branch_id: str
    client_id: str
    currency_code: str
    risk_level: str
    status: str
    open_date: date
    close_date: date | None = None
    last_maint_date: datetime
    last_maint_user: str


class PositionResponse(BaseModel):
    """Investment position response."""

    portfolio_id: str
    investment_id: str
    position_date: date
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    currency_code: str
    unrealized_gain_loss: Decimal = Decimal("0")


class PortfolioDetailResponse(BaseModel):
    """Full portfolio detail with positions.

    Replaces the complete POSMAP screen display.
    """

    portfolio: PortfolioResponse
    positions: list[PositionResponse]
    total_market_value: Decimal = Decimal("0")
    total_cost_basis: Decimal = Decimal("0")
    total_gain_loss: Decimal = Decimal("0")
    position_count: int = 0


class PortfolioListResponse(BaseModel):
    """Paginated list of portfolios."""

    portfolios: list[PortfolioResponse]
    total: int
    page: int
    page_size: int


class PortfolioCreateRequest(BaseModel):
    """Request to create a new portfolio."""

    portfolio_id: str = Field(max_length=8)
    portfolio_name: str = Field(max_length=50)
    account_type: str = Field(max_length=2)
    branch_id: str = Field(max_length=2)
    client_id: str = Field(max_length=10)
    currency_code: str = Field(default="USD", max_length=3)
    risk_level: str = Field(default="M", max_length=1)


# ---------------------------------------------------------------------------
# Dependency: database session
# ---------------------------------------------------------------------------


def get_db_session():
    """Provide a database session for request handling."""
    with get_session() as session:
        yield session


# ---------------------------------------------------------------------------
# Endpoints (replace INQPORT CICS processing)
# ---------------------------------------------------------------------------


@router.get("/portfolios", response_model=PortfolioListResponse)
def list_portfolios(
    status: Optional[str] = Query(None, max_length=1, description="Filter by status (A/C/S)"),
    client_id: Optional[str] = Query(None, max_length=10),
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    session: Session = Depends(get_db_session),
) -> PortfolioListResponse:
    """List portfolios with optional filters.

    Replaces INQPORT menu/list mode.
    """
    query = select(PortfolioMaster)

    if status:
        query = query.where(PortfolioMaster.status == status)
    if client_id:
        query = query.where(PortfolioMaster.client_id == client_id)

    # Count total
    from sqlalchemy import func

    count_query = select(func.count()).select_from(query.subquery())
    total = session.execute(count_query).scalar() or 0

    # Paginate
    query = query.offset((page - 1) * page_size).limit(page_size)
    results = session.execute(query).scalars().all()

    portfolios = [_portfolio_to_response(p) for p in results]

    return PortfolioListResponse(
        portfolios=portfolios,
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get("/portfolios/{portfolio_id}", response_model=PortfolioDetailResponse)
def get_portfolio(
    portfolio_id: str,
    position_date: Optional[date] = Query(None, description="Position date filter"),
    session: Session = Depends(get_db_session),
) -> PortfolioDetailResponse:
    """Get portfolio details with positions.

    Replaces INQPORT paragraph 2300-READ-PORTFOLIO which does:
      EXEC CICS READ FILE('POSFILE') INTO(PORTFOLIO-RECORD)
                 RIDFLD(WS-PORTFOLIO-KEY) KEYLENGTH(12)

    Then 2400-FORMAT-OUTPUT maps data to POSMAP screen.
    In Python, we return JSON instead.
    """
    # Read portfolio master
    portfolio = session.execute(
        select(PortfolioMaster).where(
            PortfolioMaster.portfolio_id == portfolio_id
        )
    ).scalar_one_or_none()

    if portfolio is None:
        # Replaces EXEC CICS HANDLE CONDITION NOTFND
        raise HTTPException(
            status_code=404,
            detail=f"Portfolio {portfolio_id} not found",
        )

    # Read positions
    pos_query = select(InvestmentPosition).where(
        InvestmentPosition.portfolio_id == portfolio_id
    )
    if position_date:
        pos_query = pos_query.where(
            InvestmentPosition.position_date == position_date
        )
    pos_query = pos_query.order_by(
        InvestmentPosition.investment_id,
        InvestmentPosition.position_date.desc(),
    )

    positions = session.execute(pos_query).scalars().all()

    # Format response (replaces 2400-FORMAT-OUTPUT)
    pos_responses = []
    total_mv = Decimal("0")
    total_cb = Decimal("0")

    for pos in positions:
        gain_loss = pos.market_value - pos.cost_basis
        pos_responses.append(
            PositionResponse(
                portfolio_id=pos.portfolio_id,
                investment_id=pos.investment_id,
                position_date=pos.position_date,
                quantity=pos.quantity,
                cost_basis=pos.cost_basis,
                market_value=pos.market_value,
                currency_code=pos.currency_code,
                unrealized_gain_loss=gain_loss,
            )
        )
        total_mv += pos.market_value
        total_cb += pos.cost_basis

    return PortfolioDetailResponse(
        portfolio=_portfolio_to_response(portfolio),
        positions=pos_responses,
        total_market_value=total_mv,
        total_cost_basis=total_cb,
        total_gain_loss=total_mv - total_cb,
        position_count=len(pos_responses),
    )


@router.post("/portfolios", response_model=PortfolioResponse, status_code=201)
def create_portfolio(
    request: PortfolioCreateRequest,
    session: Session = Depends(get_db_session),
) -> PortfolioResponse:
    """Create a new portfolio."""
    # Check for duplicate
    existing = session.execute(
        select(PortfolioMaster).where(
            PortfolioMaster.portfolio_id == request.portfolio_id
        )
    ).scalar_one_or_none()

    if existing is not None:
        raise HTTPException(
            status_code=409,
            detail=f"Portfolio {request.portfolio_id} already exists",
        )

    portfolio = PortfolioMaster(
        portfolio_id=request.portfolio_id,
        portfolio_name=request.portfolio_name,
        account_type=request.account_type,
        branch_id=request.branch_id,
        client_id=request.client_id,
        currency_code=request.currency_code,
        risk_level=request.risk_level,
        status="A",
        open_date=date.today(),
        last_maint_date=datetime.now(),
        last_maint_user="API",
    )
    session.add(portfolio)
    session.flush()

    log_audit_event(
        session=session,
        user_id="API",
        program="INQPORT",
        audit_type="TRAN",
        action="CREATE",
        portfolio_id=request.portfolio_id,
        message=f"Portfolio created: {request.portfolio_name}",
    )

    return _portfolio_to_response(portfolio)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _portfolio_to_response(p: PortfolioMaster) -> PortfolioResponse:
    """Convert ORM model to response model."""
    return PortfolioResponse(
        portfolio_id=p.portfolio_id,
        portfolio_name=p.portfolio_name,
        account_type=p.account_type,
        branch_id=p.branch_id,
        client_id=p.client_id,
        currency_code=p.currency_code,
        risk_level=p.risk_level,
        status=p.status,
        open_date=p.open_date,
        close_date=p.close_date,
        last_maint_date=p.last_maint_date,
        last_maint_user=p.last_maint_user,
    )
