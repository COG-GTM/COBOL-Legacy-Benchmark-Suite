"""
Portfolio router translated from COBOL online programs:
- INQPORT.cbl (Portfolio Position Inquiry Handler)
- PORTADD.cbl, PORTUPDT.cbl, PORTDEL.cbl via PortfolioService

Replaces CICS SEND MAP / RECEIVE MAP with REST endpoints.
"""

import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from src.api.schemas import (
    PortfolioCreateRequest,
    PortfolioListResponse,
    PortfolioResponse,
    PortfolioUpdateRequest,
)
from src.api.security import validate_api_key
from src.common.error_handler import ApplicationError, ValidationError
from src.db.session import get_session_dependency
from src.portfolio.service import PortfolioService

logger = logging.getLogger(__name__)
router = APIRouter()


def _portfolio_to_response(p) -> PortfolioResponse:
    return PortfolioResponse(
        portfolio_id=p.portfolio_id,
        client_id=p.client_id,
        client_name=p.client_name,
        client_type=p.client_type,
        portfolio_name=p.portfolio_name,
        account_type=p.account_type,
        branch_id=p.branch_id,
        currency_code=p.currency_code,
        risk_level=p.risk_level,
        status=p.status,
        total_value=str(p.total_value),
        cash_balance=str(p.cash_balance),
        open_date=str(p.open_date) if p.open_date else None,
        close_date=str(p.close_date) if p.close_date else None,
        last_maint_date=str(p.last_maint_date) if p.last_maint_date else None,
        last_maint_user=p.last_maint_user,
    )


@router.get("/{portfolio_id}", response_model=PortfolioResponse)
def get_portfolio(
    portfolio_id: str,
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """GET /portfolios/{id} — portfolio inquiry. Translates INQPORT.cbl."""
    svc = PortfolioService(session)
    portfolio = svc.get_by_id(portfolio_id)
    if portfolio is None:
        raise HTTPException(status_code=404, detail=f"Portfolio {portfolio_id} not found")
    return _portfolio_to_response(portfolio)


@router.post("", response_model=PortfolioResponse, status_code=status.HTTP_201_CREATED)
def create_portfolio(
    req: PortfolioCreateRequest,
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """POST /portfolios — create portfolio. Translates PORTADD.cbl."""
    svc = PortfolioService(session)
    try:
        portfolio = svc.create(
            portfolio_id=req.portfolio_id,
            client_id=req.client_id,
            client_name=req.client_name,
            portfolio_name=req.portfolio_name,
            account_type=req.account_type,
            branch_id=req.branch_id,
            currency_code=req.currency_code,
            risk_level=req.risk_level,
            client_type=req.client_type,
            user=user,
        )
        return _portfolio_to_response(portfolio)
    except ValidationError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except ApplicationError as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/{portfolio_id}", response_model=PortfolioResponse)
def update_portfolio(
    portfolio_id: str,
    req: PortfolioUpdateRequest,
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """PUT /portfolios/{id} — update portfolio. Translates PORTUPDT.cbl."""
    svc = PortfolioService(session)
    try:
        updates = req.model_dump(exclude_none=True)
        portfolio = svc.update(portfolio_id, user=user, **updates)
        return _portfolio_to_response(portfolio)
    except ValidationError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except ApplicationError as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/{portfolio_id}", response_model=PortfolioResponse)
def delete_portfolio(
    portfolio_id: str,
    reason: str = Query(default="", description="Reason for closing"),
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """DELETE /portfolios/{id} — close/delete portfolio. Translates PORTDEL.cbl."""
    svc = PortfolioService(session)
    try:
        portfolio = svc.delete(portfolio_id, user=user, reason=reason)
        return _portfolio_to_response(portfolio)
    except ValidationError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except ApplicationError as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("", response_model=PortfolioListResponse)
def list_portfolios(
    client_id: Optional[str] = Query(default=None),
    branch_id: Optional[str] = Query(default=None),
    status_filter: Optional[str] = Query(default=None, alias="status"),
    limit: int = Query(default=100, ge=1, le=1000),
    offset: int = Query(default=0, ge=0),
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """GET /portfolios — list portfolios with filters."""
    svc = PortfolioService(session)
    portfolios = svc.list_all(
        client_id=client_id,
        branch_id=branch_id,
        status=status_filter,
        limit=limit,
        offset=offset,
    )
    return PortfolioListResponse(
        portfolios=[_portfolio_to_response(p) for p in portfolios],
        total=len(portfolios),
    )
