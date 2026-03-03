"""Transaction history inquiry API routes.

Replaces:
  - INQHIST (src/programs/online/INQHIST.cbl) — history inquiry handler
  - INQONLN routing to INQHIST via EXEC CICS LINK
  - HISMAP BMS screen map -> JSON responses
  - HISTORY_CURSOR DB2 cursor -> SQLAlchemy paginated queries

Original COBOL flow (INQHIST.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE
      EXEC CICS HANDLE CONDITION ERROR(P999-ERROR-ROUTINE)
      1100-CONNECT-DB2 (LINK to DB2ONLN)
    2000-PROCESS-REQUEST
      2100-OPEN-CURSOR (EXEC SQL OPEN HISTORY_CURSOR)
      2200-FETCH-DATA (EXEC SQL FETCH ... INTO :WS-HIST-*)
      2300-FORMAT-SCREEN (move to HISMAP fields)
    3000-SEND-RESPONSE (EXEC CICS SEND MAP('HISMAP'))
    P999-ERROR-ROUTINE (error handler)

Key COBOL patterns replaced:
  - EXEC CICS HANDLE CONDITION ERROR -> try/except
  - HISTORY_CURSOR fetching 3000 bytes -> SQLAlchemy pagination
  - 10 records per screen -> page_size=10
  - EXEC CICS RETURN -> HTTP response return
"""

from __future__ import annotations

import logging
from datetime import date, datetime, time
from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from portfolio_manager.models.database import PositionHistory, TransactionHistory
from portfolio_manager.services.database import get_session

logger = logging.getLogger(__name__)

router = APIRouter()


# ---------------------------------------------------------------------------
# Response models (replace BMS HISMAP screen map)
# ---------------------------------------------------------------------------


class TransactionHistoryResponse(BaseModel):
    """Transaction history entry response.

    Replaces HISMAP output fields.
    """

    transaction_id: str
    portfolio_id: str
    transaction_date: date
    transaction_time: time
    investment_id: str
    transaction_type: str
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency_code: str
    status: str
    process_date: datetime


class PositionHistoryResponse(BaseModel):
    """Position history entry response.

    Replaces POSHIST DB2 cursor fetch results.
    """

    account_no: str
    portfolio_id: str
    trans_date: date
    trans_time: time
    trans_type: str
    security_id: str
    quantity: Decimal
    price: Decimal
    amount: Decimal
    fees: Decimal
    total_amount: Decimal
    cost_basis: Decimal
    gain_loss: Decimal
    program_id: str
    audit_timestamp: datetime


class HistoryListResponse(BaseModel):
    """Paginated history list.

    Replaces INQHIST cursor-based array fetch.
    The original fetched 10 records per screen (3000 bytes max).
    """

    items: list[TransactionHistoryResponse] | list[PositionHistoryResponse]
    total: int
    page: int
    page_size: int
    has_next: bool
    has_prev: bool


# ---------------------------------------------------------------------------
# Dependency
# ---------------------------------------------------------------------------


def get_db_session():
    """Provide a database session."""
    with get_session() as session:
        yield session


# ---------------------------------------------------------------------------
# Transaction History endpoints (replace INQHIST for TRANSACTION_HISTORY)
# ---------------------------------------------------------------------------


@router.get("/transactions", response_model=HistoryListResponse)
def list_transactions(
    portfolio_id: Optional[str] = Query(None, max_length=8),
    start_date: Optional[date] = Query(None, description="Start date filter"),
    end_date: Optional[date] = Query(None, description="End date filter"),
    transaction_type: Optional[str] = Query(None, max_length=2),
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    session: Session = Depends(get_db_session),
) -> HistoryListResponse:
    """List transaction history with filters.

    Replaces INQHIST paragraph 2100-OPEN-CURSOR / 2200-FETCH-DATA.

    The original COBOL used HISTORY_CURSOR:
      EXEC SQL DECLARE HISTORY_CURSOR CURSOR FOR
        SELECT ... FROM TRANSACTION_HISTORY
        WHERE PORTFOLIO_ID = :WS-PORTFOLIO-ID
          AND TRANSACTION_DATE BETWEEN :WS-START-DATE AND :WS-END-DATE
        ORDER BY TRANSACTION_DATE DESC, TRANSACTION_TIME DESC
    """
    query = select(TransactionHistory)

    if portfolio_id:
        query = query.where(TransactionHistory.portfolio_id == portfolio_id)
    if start_date:
        query = query.where(TransactionHistory.transaction_date >= start_date)
    if end_date:
        query = query.where(TransactionHistory.transaction_date <= end_date)
    if transaction_type:
        query = query.where(TransactionHistory.transaction_type == transaction_type)

    query = query.order_by(
        TransactionHistory.transaction_date.desc(),
        TransactionHistory.transaction_time.desc(),
    )

    # Count total
    from sqlalchemy import func

    count_query = select(func.count()).select_from(query.subquery())
    total = session.execute(count_query).scalar() or 0

    # Paginate (replaces cursor FETCH with LIMIT/OFFSET)
    offset = (page - 1) * page_size
    results = (
        session.execute(query.offset(offset).limit(page_size)).scalars().all()
    )

    items = [
        TransactionHistoryResponse(
            transaction_id=t.transaction_id,
            portfolio_id=t.portfolio_id,
            transaction_date=t.transaction_date,
            transaction_time=t.transaction_time,
            investment_id=t.investment_id,
            transaction_type=t.transaction_type,
            quantity=t.quantity,
            price=t.price,
            amount=t.amount,
            currency_code=t.currency_code,
            status=t.status,
            process_date=t.process_date,
        )
        for t in results
    ]

    return HistoryListResponse(
        items=items,
        total=total,
        page=page,
        page_size=page_size,
        has_next=(offset + page_size) < total,
        has_prev=page > 1,
    )


@router.get("/transactions/{transaction_id}", response_model=TransactionHistoryResponse)
def get_transaction(
    transaction_id: str,
    session: Session = Depends(get_db_session),
) -> TransactionHistoryResponse:
    """Get a single transaction by ID.

    Replaces direct EXEC SQL SELECT from TRANSACTION_HISTORY.
    """
    txn = session.execute(
        select(TransactionHistory).where(
            TransactionHistory.transaction_id == transaction_id
        )
    ).scalar_one_or_none()

    if txn is None:
        raise HTTPException(status_code=404, detail="Transaction not found")

    return TransactionHistoryResponse(
        transaction_id=txn.transaction_id,
        portfolio_id=txn.portfolio_id,
        transaction_date=txn.transaction_date,
        transaction_time=txn.transaction_time,
        investment_id=txn.investment_id,
        transaction_type=txn.transaction_type,
        quantity=txn.quantity,
        price=txn.price,
        amount=txn.amount,
        currency_code=txn.currency_code,
        status=txn.status,
        process_date=txn.process_date,
    )


# ---------------------------------------------------------------------------
# Position History endpoints (replace INQHIST for POSHIST table)
# ---------------------------------------------------------------------------


@router.get("/positions/history", response_model=HistoryListResponse)
def list_position_history(
    portfolio_id: Optional[str] = Query(None, max_length=10),
    security_id: Optional[str] = Query(None, max_length=12),
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    session: Session = Depends(get_db_session),
) -> HistoryListResponse:
    """List position history.

    Replaces INQHIST DB2 cursor queries against POSHIST table.
    """
    query = select(PositionHistory)

    if portfolio_id:
        query = query.where(PositionHistory.portfolio_id == portfolio_id)
    if security_id:
        query = query.where(PositionHistory.security_id == security_id)
    if start_date:
        query = query.where(PositionHistory.trans_date >= start_date)
    if end_date:
        query = query.where(PositionHistory.trans_date <= end_date)

    query = query.order_by(
        PositionHistory.trans_date.desc(),
        PositionHistory.trans_time.desc(),
    )

    from sqlalchemy import func

    count_query = select(func.count()).select_from(query.subquery())
    total = session.execute(count_query).scalar() or 0

    offset = (page - 1) * page_size
    results = (
        session.execute(query.offset(offset).limit(page_size)).scalars().all()
    )

    items = [
        PositionHistoryResponse(
            account_no=h.account_no,
            portfolio_id=h.portfolio_id,
            trans_date=h.trans_date,
            trans_time=h.trans_time,
            trans_type=h.trans_type,
            security_id=h.security_id,
            quantity=h.quantity,
            price=h.price,
            amount=h.amount,
            fees=h.fees,
            total_amount=h.total_amount,
            cost_basis=h.cost_basis,
            gain_loss=h.gain_loss,
            program_id=h.program_id,
            audit_timestamp=h.audit_timestamp,
        )
        for h in results
    ]

    return HistoryListResponse(
        items=items,
        total=total,
        page=page,
        page_size=page_size,
        has_next=(offset + page_size) < total,
        has_prev=page > 1,
    )
