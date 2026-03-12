"""
Inquiry router translated from COBOL program INQONLN.cbl.

Replaces the CICS pseudo-conversational inquiry flow:
  EVALUATE WS-FUNCTION-CODE
    WHEN 'INQP'  -> position inquiry
    WHEN 'MENU'  -> navigation
  END-EVALUATE

Position inquiry endpoints replacing INQPORT.cbl VSAM reads.
"""

import logging
from datetime import date
from decimal import Decimal

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from src.api.schemas import PortfolioPositionsResponse, PositionResponse
from src.api.security import require_inquiry_access
from src.db.repository import PortfolioRepository, PositionRepository
from src.db.session import get_session

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/{portfolio_id}", response_model=PortfolioPositionsResponse)
def get_positions(
    portfolio_id: str,
    position_date: date | None = Query(default=None),
    session: Session = Depends(get_session),
    user_id: str = Depends(require_inquiry_access),
) -> PortfolioPositionsResponse:
    """
    Get all positions for a portfolio.

    Translates INQPORT.cbl position inquiry:
      READ POSFILE KEY IS WS-PORTFOLIO-ID
      PERFORM UNTIL END-OF-POSITIONS
        READ POSFILE NEXT
      END-PERFORM
    """
    # Verify portfolio exists
    portfolio_repo = PortfolioRepository(session)
    portfolio = portfolio_repo.get_by_id(portfolio_id)
    if portfolio is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Portfolio not found: {portfolio_id}",
        )

    position_repo = PositionRepository(session)
    positions = position_repo.list_by_portfolio(portfolio_id, position_date)

    total_market_value = Decimal("0.00")
    total_cost_basis = Decimal("0.00")
    items: list[PositionResponse] = []

    for pos in positions:
        gain_loss = pos.market_value - pos.cost_basis
        total_market_value += pos.market_value
        total_cost_basis += pos.cost_basis
        items.append(
            PositionResponse(
                portfolio_id=pos.portfolio_id,
                investment_id=pos.investment_id,
                position_date=pos.position_date,
                quantity=pos.quantity,
                cost_basis=pos.cost_basis,
                market_value=pos.market_value,
                gain_loss=gain_loss,
                currency=pos.currency,
                status=pos.status,
            )
        )

    return PortfolioPositionsResponse(
        portfolio_id=portfolio_id,
        positions=items,
        total_market_value=total_market_value,
        total_cost_basis=total_cost_basis,
        total_gain_loss=total_market_value - total_cost_basis,
    )


@router.get("/{portfolio_id}/{investment_id}", response_model=PositionResponse)
def get_position(
    portfolio_id: str,
    investment_id: str,
    position_date: date | None = Query(default=None),
    session: Session = Depends(get_session),
    user_id: str = Depends(require_inquiry_access),
) -> PositionResponse:
    """
    Get a specific position for a portfolio/investment pair.

    Translates INQPORT.cbl specific position read:
      READ POSFILE KEY IS WS-POS-KEY
    """
    position_repo = PositionRepository(session)

    if position_date:
        position = position_repo.get(portfolio_id, investment_id, position_date)
    else:
        position = position_repo.get_latest_position(portfolio_id, investment_id)

    if position is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Position not found: {portfolio_id}/{investment_id}",
        )

    return PositionResponse(
        portfolio_id=position.portfolio_id,
        investment_id=position.investment_id,
        position_date=position.position_date,
        quantity=position.quantity,
        cost_basis=position.cost_basis,
        market_value=position.market_value,
        gain_loss=position.market_value - position.cost_basis,
        currency=position.currency,
        status=position.status,
    )
