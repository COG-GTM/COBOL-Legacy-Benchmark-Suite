"""
Inquiry router translated from COBOL program INQONLN.cbl.

Replaces CICS pseudo-conversational inquiry flow with REST endpoints.
Translates INQONLN EVALUATE WS-COMMAREA-FUNCTION dispatcher.
"""

import logging
from decimal import Decimal

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from src.api.schemas import PositionListResponse, PositionResponse
from src.api.security import validate_api_key
from src.db.repository import PositionRepository
from src.db.session import get_session_dependency

logger = logging.getLogger(__name__)
router = APIRouter()


def _position_to_response(pos) -> PositionResponse:
    return PositionResponse(
        portfolio_id=pos.portfolio_id,
        investment_id=pos.investment_id,
        position_date=str(pos.position_date),
        quantity=str(Decimal(str(pos.quantity))),
        cost_basis=str(Decimal(str(pos.cost_basis))),
        market_value=str(Decimal(str(pos.market_value))),
        currency=pos.currency_code,
        status=pos.status,
    )


@router.get("/{portfolio_id}", response_model=PositionListResponse)
def get_positions(
    portfolio_id: str,
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """
    GET /positions/{portfolio_id} — position inquiry.
    Translates INQPORT.cbl P200-GET-POSITION.
    """
    repo = PositionRepository(session)
    positions = repo.list_by_portfolio(portfolio_id)
    return PositionListResponse(
        portfolio_id=portfolio_id,
        positions=[_position_to_response(p) for p in positions],
        total=len(positions),
    )


@router.get("/{portfolio_id}/{investment_id}", response_model=PositionResponse)
def get_specific_position(
    portfolio_id: str,
    investment_id: str,
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """
    GET /positions/{portfolio_id}/{investment_id} — specific position.
    Translates INQPORT.cbl EXEC CICS READ FILE('POSFILE') RIDFLD.
    """
    repo = PositionRepository(session)
    position = repo.get_latest(portfolio_id, investment_id)
    if position is None:
        raise HTTPException(
            status_code=404,
            detail=f"Position not found: {portfolio_id}/{investment_id}",
        )
    return _position_to_response(position)
