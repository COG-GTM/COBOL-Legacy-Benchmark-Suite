"""
Portfolio endpoints - migrated from INQPORT.cbl.
Provides REST API for portfolio inquiries and management.

Original COBOL Program: INQPORT.cbl
- P100-INIT-PROGRAM: Initialize program
- P200-GET-POSITION: Read position from VSAM
- P300-FORMAT-MAP: Format display map
- P900-NOT-FOUND: Handle not found
- P999-RETURN: Return to CICS
"""

from datetime import date
from decimal import Decimal
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.api.endpoints.auth import UserResponse, get_current_user
from app.services.database import get_db
from app.services.portfolio import PortfolioService
from app.utils.exceptions import (
    PortfolioNotFoundError,
    PositionNotFoundError,
    ValidationError,
)

router = APIRouter()


class PositionResponse(BaseModel):
    """Position response model - replaces BMS map fields."""
    portfolio_id: str
    investment_id: str
    position_date: date
    quantity: float
    cost_basis: float
    market_value: float
    currency_code: str
    status: str
    gain_loss: float
    gain_loss_pct: float

    class Config:
        from_attributes = True


class PortfolioResponse(BaseModel):
    """Portfolio response model."""
    portfolio_id: str
    portfolio_name: str
    client_id: str
    client_name: str | None
    status: str
    currency_code: str
    total_value: float
    cash_balance: float
    open_date: date
    last_maint_date: str | None

    class Config:
        from_attributes = True


class PortfolioSummaryResponse(BaseModel):
    """Portfolio summary response model."""
    portfolio_id: str
    portfolio_name: str
    client_id: str
    status: str
    position_count: int
    total_cost_basis: float
    total_market_value: float
    total_gain_loss: float
    cash_balance: float
    last_maint_date: str | None


class PortfolioCreateRequest(BaseModel):
    """Portfolio creation request model."""
    portfolio_id: str = Field(..., min_length=1, max_length=8)
    client_id: str = Field(..., min_length=1, max_length=10)
    portfolio_name: str = Field(..., min_length=1, max_length=50)
    account_type: str = Field(default="IN", max_length=2)
    branch_id: str = Field(default="01", max_length=2)
    currency_code: str = Field(default="USD", max_length=3)


class PositionUpdateRequest(BaseModel):
    """Position update request model."""
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    position_date: date | None = None


@router.get("/{portfolio_id}", response_model=PortfolioResponse)
async def get_portfolio(
    portfolio_id: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> PortfolioResponse:
    """
    Get portfolio by ID.
    Replaces INQPORT P200-GET-POSITION (portfolio lookup).
    """
    service = PortfolioService(db)

    try:
        portfolio = service.get_portfolio(portfolio_id)
        return PortfolioResponse(
            portfolio_id=portfolio.portfolio_id,
            portfolio_name=portfolio.portfolio_name,
            client_id=portfolio.client_id,
            client_name=portfolio.client_name,
            status=portfolio.status,
            currency_code=portfolio.currency_code,
            total_value=float(portfolio.total_value or 0),
            cash_balance=float(portfolio.cash_balance or 0),
            open_date=portfolio.open_date,
            last_maint_date=portfolio.last_maint_date.isoformat() if portfolio.last_maint_date else None,
        )
    except PortfolioNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.get("/{portfolio_id}/summary", response_model=PortfolioSummaryResponse)
async def get_portfolio_summary(
    portfolio_id: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> PortfolioSummaryResponse:
    """
    Get portfolio summary with calculated values.
    Replaces P300-FORMAT-MAP in INQPORT.cbl.
    """
    service = PortfolioService(db)

    try:
        summary = service.get_portfolio_summary(portfolio_id)
        return PortfolioSummaryResponse(**summary)
    except PortfolioNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.get("/{portfolio_id}/positions", response_model=list[PositionResponse])
async def get_positions(
    portfolio_id: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
    position_date: date | None = Query(None, description="Filter by position date"),
    status_filter: str | None = Query(None, alias="status", description="Filter by status (A/C/P)"),
) -> list[PositionResponse]:
    """
    Get positions for a portfolio.
    Replaces VSAM file read in INQPORT.cbl.
    """
    service = PortfolioService(db)

    try:
        service.get_portfolio(portfolio_id)
        positions = service.get_positions(
            portfolio_id=portfolio_id,
            position_date=position_date,
            status=status_filter,
        )

        result = []
        for pos in positions:
            cost_basis = float(pos.cost_basis or 0)
            market_value = float(pos.market_value or 0)
            gain_loss = market_value - cost_basis
            gain_loss_pct = (gain_loss / cost_basis * 100) if cost_basis != 0 else 0

            result.append(PositionResponse(
                portfolio_id=pos.portfolio_id,
                investment_id=pos.investment_id,
                position_date=pos.position_date,
                quantity=float(pos.quantity or 0),
                cost_basis=cost_basis,
                market_value=market_value,
                currency_code=pos.currency_code,
                status=pos.status,
                gain_loss=gain_loss,
                gain_loss_pct=gain_loss_pct,
            ))

        return result
    except PortfolioNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.get("/{portfolio_id}/positions/{investment_id}", response_model=PositionResponse)
async def get_position(
    portfolio_id: str,
    investment_id: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
    position_date: date | None = Query(None, description="Position date"),
) -> PositionResponse:
    """
    Get a specific position.
    Replaces P200-GET-POSITION in INQPORT.cbl.
    """
    service = PortfolioService(db)

    try:
        pos = service.get_position(portfolio_id, investment_id, position_date)

        cost_basis = float(pos.cost_basis or 0)
        market_value = float(pos.market_value or 0)
        gain_loss = market_value - cost_basis
        gain_loss_pct = (gain_loss / cost_basis * 100) if cost_basis != 0 else 0

        return PositionResponse(
            portfolio_id=pos.portfolio_id,
            investment_id=pos.investment_id,
            position_date=pos.position_date,
            quantity=float(pos.quantity or 0),
            cost_basis=cost_basis,
            market_value=market_value,
            currency_code=pos.currency_code,
            status=pos.status,
            gain_loss=gain_loss,
            gain_loss_pct=gain_loss_pct,
        )
    except PositionNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.post("", response_model=PortfolioResponse, status_code=status.HTTP_201_CREATED)
async def create_portfolio(
    request: PortfolioCreateRequest,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> PortfolioResponse:
    """Create a new portfolio."""
    service = PortfolioService(db)

    try:
        portfolio = service.create_portfolio(
            portfolio_id=request.portfolio_id,
            client_id=request.client_id,
            portfolio_name=request.portfolio_name,
            user_id=current_user.user_id,
            account_type=request.account_type,
            branch_id=request.branch_id,
            currency_code=request.currency_code,
        )
        db.commit()

        return PortfolioResponse(
            portfolio_id=portfolio.portfolio_id,
            portfolio_name=portfolio.portfolio_name,
            client_id=portfolio.client_id,
            client_name=portfolio.client_name,
            status=portfolio.status,
            currency_code=portfolio.currency_code,
            total_value=float(portfolio.total_value or 0),
            cash_balance=float(portfolio.cash_balance or 0),
            open_date=portfolio.open_date,
            last_maint_date=portfolio.last_maint_date.isoformat() if portfolio.last_maint_date else None,
        )
    except ValidationError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e),
        )


@router.put("/{portfolio_id}/positions/{investment_id}", response_model=PositionResponse)
async def update_position(
    portfolio_id: str,
    investment_id: str,
    request: PositionUpdateRequest,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> PositionResponse:
    """
    Update or create a position.
    Replaces position update logic from POSUPD00.cbl.
    """
    service = PortfolioService(db)

    try:
        pos = service.update_position(
            portfolio_id=portfolio_id,
            investment_id=investment_id,
            quantity=request.quantity,
            cost_basis=request.cost_basis,
            market_value=request.market_value,
            user_id=current_user.user_id,
            position_date=request.position_date,
        )
        db.commit()

        cost_basis = float(pos.cost_basis or 0)
        market_value = float(pos.market_value or 0)
        gain_loss = market_value - cost_basis
        gain_loss_pct = (gain_loss / cost_basis * 100) if cost_basis != 0 else 0

        return PositionResponse(
            portfolio_id=pos.portfolio_id,
            investment_id=pos.investment_id,
            position_date=pos.position_date,
            quantity=float(pos.quantity or 0),
            cost_basis=cost_basis,
            market_value=market_value,
            currency_code=pos.currency_code,
            status=pos.status,
            gain_loss=gain_loss,
            gain_loss_pct=gain_loss_pct,
        )
    except PortfolioNotFoundError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e),
        )
