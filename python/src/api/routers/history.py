"""
History router translated from COBOL program INQHIST.cbl.

Replaces:
  - INQHIST.cbl: Transaction history inquiry from DB2 POSHIST table
  - CICS pseudo-conversational history browsing

Provides transaction history endpoints with date range filters.
"""

import logging
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from src.api.schemas import TransactionListResponse, TransactionResponse
from src.api.security import require_inquiry_access
from src.db.repository import TransactionRepository
from src.db.session import get_session

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/{portfolio_id}", response_model=TransactionListResponse)
def get_transaction_history(
    portfolio_id: str,
    start_date: date | None = Query(default=None),
    end_date: date | None = Query(default=None),
    trn_type: str | None = Query(default=None),
    trn_status: str | None = Query(default=None, alias="status"),
    session: Session = Depends(get_session),
    user_id: str = Depends(require_inquiry_access),
) -> TransactionListResponse:
    """
    Get transaction history for a portfolio.

    Translates INQHIST.cbl:
      EXEC SQL
        SELECT * FROM TRANSACTION_HISTORY
        WHERE PORTFOLIO_ID = :WS-PORTFOLIO-ID
          AND TRN_DATE BETWEEN :WS-START-DATE AND :WS-END-DATE
        ORDER BY TRN_DATE DESC, TRN_TIME DESC
      END-EXEC
    """
    repo = TransactionRepository(session)
    transactions = repo.list_by_portfolio(
        portfolio_id=portfolio_id,
        start_date=start_date,
        end_date=end_date,
        trn_type=trn_type,
        status=trn_status,
    )

    items = [TransactionResponse.model_validate(t) for t in transactions]
    return TransactionListResponse(items=items, total=len(items))


@router.get("/detail/{transaction_id}", response_model=TransactionResponse)
def get_transaction(
    transaction_id: str,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_inquiry_access),
) -> TransactionResponse:
    """
    Get a specific transaction by ID.

    Translates INQHIST.cbl single record read:
      EXEC SQL
        SELECT * FROM TRANSACTION_HISTORY
        WHERE TRANSACTION_ID = :WS-TRANSACTION-ID
      END-EXEC
    """
    repo = TransactionRepository(session)
    transaction = repo.get_by_id(transaction_id)
    if transaction is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Transaction not found: {transaction_id}",
        )
    return TransactionResponse.model_validate(transaction)
