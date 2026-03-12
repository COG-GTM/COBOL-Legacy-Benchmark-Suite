"""
Portfolio router translated from COBOL online programs:
  - INQPORT.cbl: Portfolio inquiry
  - PORTMSTR.cbl: Portfolio master file operations

Replaces CICS transaction PINQ for portfolio CRUD operations.
"""

import logging

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from src.api.schemas import (
    PortfolioCreateRequest,
    PortfolioListResponse,
    PortfolioResponse,
    PortfolioUpdateRequest,
    TransactionCreateRequest,
    TransactionResponse,
)
from src.api.security import require_portfolio_access
from src.common.error_handler import (
    DuplicateError,
    NotFoundError,
    ValidationError,
)
from src.db.session import get_session
from src.models.portfolio import PortfolioRecord
from src.models.transaction import TransactionRecord
from src.portfolio.service import PortfolioService
from src.portfolio.transaction_service import TransactionService

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/{portfolio_id}", response_model=PortfolioResponse)
def get_portfolio(
    portfolio_id: str,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_portfolio_access),
) -> PortfolioResponse:
    """
    Get portfolio by ID.

    Translates INQPORT.cbl portfolio inquiry.
    """
    try:
        service = PortfolioService(session)
        portfolio = service.get_by_id(portfolio_id)
        return PortfolioResponse.model_validate(portfolio)
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.post("", response_model=PortfolioResponse, status_code=status.HTTP_201_CREATED)
def create_portfolio(
    request: PortfolioCreateRequest,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_portfolio_access),
) -> PortfolioResponse:
    """
    Create a new portfolio.

    Translates PORTADD.cbl create portfolio flow.
    """
    try:
        record = PortfolioRecord(
            portfolio_id=request.portfolio_id,
            account_no=request.account_no,
            account_type=request.account_type,
            branch_id=request.branch_id,
            client_id=request.client_id,
            portfolio_name=request.portfolio_name,
            currency_code=request.currency_code,
            risk_level=request.risk_level,
            client_name=request.client_name,
            client_type=request.client_type,
            open_date=request.open_date,
            cash_balance=request.cash_balance,
        )
        service = PortfolioService(session)
        portfolio = service.create(record, user_id=user_id)
        return PortfolioResponse.model_validate(portfolio)
    except DuplicateError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc)) from exc
    except ValidationError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc


@router.put("/{portfolio_id}", response_model=PortfolioResponse)
def update_portfolio(
    portfolio_id: str,
    request: PortfolioUpdateRequest,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_portfolio_access),
) -> PortfolioResponse:
    """
    Update an existing portfolio.

    Translates PORTUPDT.cbl update flow.
    """
    try:
        updates = request.model_dump(exclude_none=True)
        service = PortfolioService(session)
        portfolio = service.update(portfolio_id, updates, user_id=user_id)
        return PortfolioResponse.model_validate(portfolio)
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValidationError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc


@router.delete("/{portfolio_id}", response_model=PortfolioResponse)
def delete_portfolio(
    portfolio_id: str,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_portfolio_access),
) -> PortfolioResponse:
    """
    Close/delete a portfolio.

    Translates PORTDEL.cbl closure flow.
    """
    try:
        service = PortfolioService(session)
        portfolio = service.delete(portfolio_id, user_id=user_id)
        return PortfolioResponse.model_validate(portfolio)
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValidationError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc


@router.get("", response_model=PortfolioListResponse)
def list_portfolios(
    offset: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=1000),
    status_filter: str | None = Query(default=None, alias="status"),
    branch_id: str | None = Query(default=None),
    client_id: str | None = Query(default=None),
    session: Session = Depends(get_session),
    user_id: str = Depends(require_portfolio_access),
) -> PortfolioListResponse:
    """List portfolios with optional filters."""
    service = PortfolioService(session)
    portfolios = service.list_all(
        offset=offset, limit=limit, status=status_filter,
        branch_id=branch_id, client_id=client_id,
    )
    items = [PortfolioResponse.model_validate(p) for p in portfolios]
    return PortfolioListResponse(
        items=items,
        total=len(items),
        offset=offset,
        limit=limit,
    )


@router.post("/{portfolio_id}/transactions", response_model=TransactionResponse, status_code=status.HTTP_201_CREATED)
def create_transaction(
    portfolio_id: str,
    request: TransactionCreateRequest,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_portfolio_access),
) -> TransactionResponse:
    """
    Create and process a transaction for a portfolio.

    Translates PORTTRAN.cbl transaction processing.
    """
    try:
        record = TransactionRecord(
            portfolio_id=portfolio_id,
            investment_id=request.investment_id,
            trn_type=request.trn_type,
            quantity=request.quantity,
            price=request.price,
            amount=request.amount,
        )
        service = TransactionService(session)
        transaction = service.process(record, user_id=user_id)
        return TransactionResponse.model_validate(transaction)
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValidationError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc
