"""Portfolio API endpoints - converted from INQPORT.cbl.

This module provides REST API endpoints for portfolio inquiries,
replacing the CICS INQPORT program functionality.

COBOL Program Reference (INQPORT.cbl):
- Handles portfolio position inquiries
- Reads from VSAM POSFILE
- Returns position data to CICS screens
"""

from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.database.models import AuditLog, InvestmentPosition, PortfolioMaster
from app.utils.logging import get_logger

logger = get_logger(__name__)

router = APIRouter(prefix="/portfolio", tags=["Portfolio"])


class PositionResponse(BaseModel):
    """Position response model - similar to INQPORT output."""

    portfolio_id: str
    investment_id: str
    position_date: date
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    currency_code: str
    unrealized_gain_loss: Decimal

    class Config:
        from_attributes = True


class PortfolioSummary(BaseModel):
    """Portfolio summary response."""

    portfolio_id: str
    portfolio_name: str
    client_id: str
    account_type: str
    currency_code: str
    status: str
    open_date: date
    total_positions: int
    total_market_value: Decimal
    total_cost_basis: Decimal
    total_unrealized_gain_loss: Decimal


class PortfolioDetailResponse(BaseModel):
    """Detailed portfolio response with positions."""

    summary: PortfolioSummary
    positions: list[PositionResponse]


class PortfolioListResponse(BaseModel):
    """List of portfolios response."""

    portfolios: list[PortfolioSummary]
    total_count: int


@router.get(
    "/{portfolio_id}",
    response_model=PortfolioDetailResponse,
    summary="Get portfolio details",
    description="Retrieve portfolio details with all positions - replaces INQPORT CICS program",
)
async def get_portfolio(
    portfolio_id: str,
    position_date: Optional[date] = Query(None, description="Position date filter"),
    db: Session = Depends(get_db),
) -> PortfolioDetailResponse:
    """Get portfolio details with positions.

    This endpoint replaces the CICS INQPORT program's portfolio inquiry
    functionality, reading from the database instead of VSAM files.
    """
    portfolio = (
        db.query(PortfolioMaster)
        .filter(PortfolioMaster.portfolio_id == portfolio_id)
        .first()
    )

    if not portfolio:
        logger.warning(f"Portfolio not found: {portfolio_id}")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Portfolio {portfolio_id} not found",
        )

    query = db.query(InvestmentPosition).filter(
        InvestmentPosition.portfolio_id == portfolio_id
    )

    if position_date:
        query = query.filter(InvestmentPosition.position_date == position_date)

    positions = query.order_by(InvestmentPosition.investment_id).all()

    total_market_value = sum(p.market_value or Decimal("0") for p in positions)
    total_cost_basis = sum(p.cost_basis or Decimal("0") for p in positions)

    position_responses = [
        PositionResponse(
            portfolio_id=p.portfolio_id,
            investment_id=p.investment_id,
            position_date=p.position_date,
            quantity=p.quantity,
            cost_basis=p.cost_basis,
            market_value=p.market_value,
            currency_code=p.currency_code,
            unrealized_gain_loss=(p.market_value or Decimal("0"))
            - (p.cost_basis or Decimal("0")),
        )
        for p in positions
    ]

    summary = PortfolioSummary(
        portfolio_id=portfolio.portfolio_id,
        portfolio_name=portfolio.portfolio_name,
        client_id=portfolio.client_id,
        account_type=portfolio.account_type,
        currency_code=portfolio.currency_code,
        status=portfolio.status,
        open_date=portfolio.open_date,
        total_positions=len(positions),
        total_market_value=total_market_value,
        total_cost_basis=total_cost_basis,
        total_unrealized_gain_loss=total_market_value - total_cost_basis,
    )

    _log_inquiry(db, portfolio_id, "PORTFOLIO", "INQPORT")

    logger.info(f"Portfolio inquiry: {portfolio_id}, positions: {len(positions)}")

    return PortfolioDetailResponse(summary=summary, positions=position_responses)


@router.get(
    "/{portfolio_id}/positions",
    response_model=list[PositionResponse],
    summary="Get portfolio positions",
    description="Retrieve all positions for a portfolio",
)
async def get_positions(
    portfolio_id: str,
    investment_id: Optional[str] = Query(None, description="Filter by investment ID"),
    db: Session = Depends(get_db),
) -> list[PositionResponse]:
    """Get positions for a portfolio."""
    portfolio = (
        db.query(PortfolioMaster)
        .filter(PortfolioMaster.portfolio_id == portfolio_id)
        .first()
    )

    if not portfolio:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Portfolio {portfolio_id} not found",
        )

    query = db.query(InvestmentPosition).filter(
        InvestmentPosition.portfolio_id == portfolio_id
    )

    if investment_id:
        query = query.filter(InvestmentPosition.investment_id == investment_id)

    positions = query.order_by(
        InvestmentPosition.investment_id, InvestmentPosition.position_date.desc()
    ).all()

    return [
        PositionResponse(
            portfolio_id=p.portfolio_id,
            investment_id=p.investment_id,
            position_date=p.position_date,
            quantity=p.quantity,
            cost_basis=p.cost_basis,
            market_value=p.market_value,
            currency_code=p.currency_code,
            unrealized_gain_loss=(p.market_value or Decimal("0"))
            - (p.cost_basis or Decimal("0")),
        )
        for p in positions
    ]


@router.get(
    "/",
    response_model=PortfolioListResponse,
    summary="List portfolios",
    description="List all portfolios with optional filters",
)
async def list_portfolios(
    client_id: Optional[str] = Query(None, description="Filter by client ID"),
    status_filter: Optional[str] = Query(
        None, alias="status", description="Filter by status (A=Active, C=Closed)"
    ),
    skip: int = Query(0, ge=0, description="Number of records to skip"),
    limit: int = Query(100, ge=1, le=1000, description="Maximum records to return"),
    db: Session = Depends(get_db),
) -> PortfolioListResponse:
    """List portfolios with optional filters."""
    query = db.query(PortfolioMaster)

    if client_id:
        query = query.filter(PortfolioMaster.client_id == client_id)

    if status_filter:
        query = query.filter(PortfolioMaster.status == status_filter)

    total_count = query.count()
    portfolios = (
        query.order_by(PortfolioMaster.portfolio_id).offset(skip).limit(limit).all()
    )

    summaries = []
    for portfolio in portfolios:
        positions = (
            db.query(InvestmentPosition)
            .filter(InvestmentPosition.portfolio_id == portfolio.portfolio_id)
            .all()
        )

        total_market_value = sum(p.market_value or Decimal("0") for p in positions)
        total_cost_basis = sum(p.cost_basis or Decimal("0") for p in positions)

        summaries.append(
            PortfolioSummary(
                portfolio_id=portfolio.portfolio_id,
                portfolio_name=portfolio.portfolio_name,
                client_id=portfolio.client_id,
                account_type=portfolio.account_type,
                currency_code=portfolio.currency_code,
                status=portfolio.status,
                open_date=portfolio.open_date,
                total_positions=len(positions),
                total_market_value=total_market_value,
                total_cost_basis=total_cost_basis,
                total_unrealized_gain_loss=total_market_value - total_cost_basis,
            )
        )

    return PortfolioListResponse(portfolios=summaries, total_count=total_count)


def _log_inquiry(
    db: Session, portfolio_id: str, inquiry_type: str, program: str
) -> None:
    """Log inquiry to audit log - similar to CICS audit logging."""
    try:
        audit = AuditLog(
            timestamp=datetime.now(),
            user_id="API",
            program=program,
            access_type="INQUIRE",
            portfolio_id=portfolio_id,
            action_status="SUCC",
            message=f"{inquiry_type} inquiry",
        )
        db.add(audit)
        db.commit()
    except Exception as e:
        logger.warning(f"Failed to log audit: {e}")
        db.rollback()
