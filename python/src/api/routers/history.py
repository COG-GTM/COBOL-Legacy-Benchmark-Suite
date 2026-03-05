"""
History router translated from COBOL program INQHIST.cbl.

Translates:
- P200-GET-HISTORY: DB2 SELECT with cursor management
- P250-FETCH-HISTORY: array fetch for paginated results
"""

import logging
from datetime import date
from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from src.api.schemas import TransactionListResponse, TransactionResponse
from src.api.security import validate_api_key
from src.db.repository import TransactionRepository
from src.db.session import get_session_dependency

logger = logging.getLogger(__name__)
router = APIRouter()


def _trn_to_response(trn) -> TransactionResponse:
    return TransactionResponse(
        transaction_id=trn.transaction_id,
        portfolio_id=trn.portfolio_id,
        investment_id=trn.investment_id,
        trn_date=str(trn.trn_date),
        trn_time=trn.trn_time,
        trn_type=trn.trn_type,
        quantity=str(Decimal(str(trn.quantity))),
        price=str(Decimal(str(trn.price))),
        amount=str(Decimal(str(trn.amount))),
        currency=trn.currency_code,
        status=trn.status,
        process_date=str(trn.process_date) if trn.process_date else None,
        fees=str(Decimal(str(trn.fees))),
        gain_loss=str(Decimal(str(trn.gain_loss))),
    )


@router.get("/{portfolio_id}", response_model=TransactionListResponse)
def get_transaction_history(
    portfolio_id: str,
    start_date: Optional[str] = Query(default=None, description="YYYY-MM-DD"),
    end_date: Optional[str] = Query(default=None, description="YYYY-MM-DD"),
    limit: int = Query(default=100, ge=1, le=1000),
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """
    GET /transactions/{portfolio_id} — transaction history with date range.
    Translates INQHIST.cbl P200-GET-HISTORY DB2 query.
    """
    repo = TransactionRepository(session)

    sd = date.fromisoformat(start_date) if start_date else None
    ed = date.fromisoformat(end_date) if end_date else None

    transactions = repo.list_by_portfolio(
        portfolio_id, start_date=sd, end_date=ed, limit=limit
    )
    return TransactionListResponse(
        portfolio_id=portfolio_id,
        transactions=[_trn_to_response(t) for t in transactions],
        total=len(transactions),
    )


@router.get("/detail/{transaction_id}", response_model=TransactionResponse)
def get_transaction_detail(
    transaction_id: int,
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """GET /transactions/{transaction_id} — specific transaction details."""
    repo = TransactionRepository(session)
    trn = repo.get_by_id(transaction_id)
    if trn is None:
        raise HTTPException(status_code=404, detail=f"Transaction {transaction_id} not found")
    return _trn_to_response(trn)
