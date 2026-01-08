"""History API endpoints - converted from INQHIST.cbl.

This module provides REST API endpoints for transaction history inquiries,
replacing the CICS INQHIST program functionality.

COBOL Program Reference (INQHIST.cbl):
- Handles transaction history inquiries
- Reads from DB2 POSHIST table
- Returns history data to CICS screens
"""

from datetime import date, datetime, time
from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.database.models import AuditLog, PositionHistory, TransactionHistory
from app.utils.logging import get_logger

logger = get_logger(__name__)

router = APIRouter(prefix="/history", tags=["History"])


class TransactionHistoryResponse(BaseModel):
    """Transaction history response model - similar to INQHIST output."""

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

    class Config:
        from_attributes = True


class PositionHistoryResponse(BaseModel):
    """Position history response model - from POSHIST table."""

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
    process_date: date
    program_id: str

    class Config:
        from_attributes = True


class HistorySummary(BaseModel):
    """History summary statistics."""

    portfolio_id: str
    total_transactions: int
    total_buys: int
    total_sells: int
    total_buy_amount: Decimal
    total_sell_amount: Decimal
    net_amount: Decimal
    date_range_start: Optional[date]
    date_range_end: Optional[date]


class HistoryListResponse(BaseModel):
    """Paginated history list response."""

    transactions: list[TransactionHistoryResponse]
    total_count: int
    summary: Optional[HistorySummary]


@router.get(
    "/transactions/{portfolio_id}",
    response_model=HistoryListResponse,
    summary="Get transaction history",
    description="Retrieve transaction history for a portfolio - replaces INQHIST CICS program",
)
async def get_transaction_history(
    portfolio_id: str,
    start_date: Optional[date] = Query(None, description="Start date filter"),
    end_date: Optional[date] = Query(None, description="End date filter"),
    transaction_type: Optional[str] = Query(
        None, description="Transaction type filter (BU, SL, TR, FE)"
    ),
    include_summary: bool = Query(True, description="Include summary statistics"),
    skip: int = Query(0, ge=0, description="Number of records to skip"),
    limit: int = Query(100, ge=1, le=1000, description="Maximum records to return"),
    db: Session = Depends(get_db),
) -> HistoryListResponse:
    """Get transaction history for a portfolio.

    This endpoint replaces the CICS INQHIST program's history inquiry
    functionality, reading from the database instead of DB2 directly.
    """
    query = db.query(TransactionHistory).filter(
        TransactionHistory.portfolio_id == portfolio_id
    )

    if start_date:
        query = query.filter(TransactionHistory.transaction_date >= start_date)

    if end_date:
        query = query.filter(TransactionHistory.transaction_date <= end_date)

    if transaction_type:
        query = query.filter(TransactionHistory.transaction_type == transaction_type)

    total_count = query.count()

    transactions = (
        query.order_by(
            TransactionHistory.transaction_date.desc(),
            TransactionHistory.transaction_time.desc(),
        )
        .offset(skip)
        .limit(limit)
        .all()
    )

    transaction_responses = [
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
        for t in transactions
    ]

    summary = None
    if include_summary:
        summary = _calculate_summary(db, portfolio_id, start_date, end_date)

    _log_inquiry(db, portfolio_id, "HISTORY", "INQHIST")

    logger.info(
        f"History inquiry: {portfolio_id}, transactions: {len(transactions)}"
    )

    return HistoryListResponse(
        transactions=transaction_responses,
        total_count=total_count,
        summary=summary,
    )


@router.get(
    "/positions/{portfolio_id}",
    response_model=list[PositionHistoryResponse],
    summary="Get position history",
    description="Retrieve position history from POSHIST table",
)
async def get_position_history(
    portfolio_id: str,
    start_date: Optional[date] = Query(None, description="Start date filter"),
    end_date: Optional[date] = Query(None, description="End date filter"),
    security_id: Optional[str] = Query(None, description="Security ID filter"),
    skip: int = Query(0, ge=0, description="Number of records to skip"),
    limit: int = Query(100, ge=1, le=1000, description="Maximum records to return"),
    db: Session = Depends(get_db),
) -> list[PositionHistoryResponse]:
    """Get position history from POSHIST table."""
    query = db.query(PositionHistory).filter(
        PositionHistory.portfolio_id == portfolio_id
    )

    if start_date:
        query = query.filter(PositionHistory.trans_date >= start_date)

    if end_date:
        query = query.filter(PositionHistory.trans_date <= end_date)

    if security_id:
        query = query.filter(PositionHistory.security_id == security_id)

    history = (
        query.order_by(
            PositionHistory.trans_date.desc(),
            PositionHistory.trans_time.desc(),
        )
        .offset(skip)
        .limit(limit)
        .all()
    )

    return [
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
            process_date=h.process_date,
            program_id=h.program_id,
        )
        for h in history
    ]


@router.get(
    "/summary/{portfolio_id}",
    response_model=HistorySummary,
    summary="Get history summary",
    description="Get summary statistics for portfolio transaction history",
)
async def get_history_summary(
    portfolio_id: str,
    start_date: Optional[date] = Query(None, description="Start date filter"),
    end_date: Optional[date] = Query(None, description="End date filter"),
    db: Session = Depends(get_db),
) -> HistorySummary:
    """Get summary statistics for portfolio history."""
    summary = _calculate_summary(db, portfolio_id, start_date, end_date)

    if summary.total_transactions == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"No transaction history found for portfolio {portfolio_id}",
        )

    return summary


def _calculate_summary(
    db: Session,
    portfolio_id: str,
    start_date: Optional[date],
    end_date: Optional[date],
) -> HistorySummary:
    """Calculate summary statistics for portfolio history."""
    query = db.query(TransactionHistory).filter(
        TransactionHistory.portfolio_id == portfolio_id
    )

    if start_date:
        query = query.filter(TransactionHistory.transaction_date >= start_date)

    if end_date:
        query = query.filter(TransactionHistory.transaction_date <= end_date)

    transactions = query.all()

    total_buys = sum(1 for t in transactions if t.transaction_type == "BU")
    total_sells = sum(1 for t in transactions if t.transaction_type == "SL")

    total_buy_amount = sum(
        t.amount for t in transactions if t.transaction_type == "BU"
    ) or Decimal("0")

    total_sell_amount = sum(
        t.amount for t in transactions if t.transaction_type == "SL"
    ) or Decimal("0")

    date_range = (
        db.query(
            func.min(TransactionHistory.transaction_date),
            func.max(TransactionHistory.transaction_date),
        )
        .filter(TransactionHistory.portfolio_id == portfolio_id)
        .first()
    )

    return HistorySummary(
        portfolio_id=portfolio_id,
        total_transactions=len(transactions),
        total_buys=total_buys,
        total_sells=total_sells,
        total_buy_amount=total_buy_amount,
        total_sell_amount=total_sell_amount,
        net_amount=total_sell_amount - total_buy_amount,
        date_range_start=date_range[0] if date_range else None,
        date_range_end=date_range[1] if date_range else None,
    )


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
