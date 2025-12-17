"""
Transaction endpoints - migrated from INQHIST.cbl.
Provides REST API for transaction history inquiries.

Original COBOL Program: INQHIST.cbl
- P100-INIT-PROGRAM: Initialize program
- P200-GET-HISTORY: Read history from DB2
- P300-FORMAT-MAP: Format display map
- P400-SCROLL-FORWARD: Handle scrolling
- P500-SCROLL-BACKWARD: Handle scrolling
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
from app.models.domain import TransactionStatus, TransactionType
from app.services.database import get_db
from app.services.portfolio import PortfolioService
from app.services.transaction import TransactionService
from app.utils.exceptions import (
    PortfolioNotFoundError,
    TransactionNotFoundError,
)

router = APIRouter()


class TransactionResponse(BaseModel):
    """Transaction response model - replaces BMS map fields."""
    transaction_id: str
    portfolio_id: str
    transaction_date: date
    transaction_time: str
    investment_id: str
    transaction_type: str
    quantity: float
    price: float
    amount: float
    fees: float
    total_amount: float
    currency_code: str
    status: str
    process_date: str

    class Config:
        from_attributes = True


class TransactionCreateRequest(BaseModel):
    """Transaction creation request model."""
    portfolio_id: str = Field(..., min_length=1, max_length=8)
    investment_id: str = Field(..., min_length=1, max_length=10)
    transaction_type: TransactionType
    quantity: Decimal = Field(..., gt=0)
    price: Decimal = Field(..., gt=0)
    fees: Decimal = Field(default=Decimal("0"), ge=0)


class TransactionStatusUpdateRequest(BaseModel):
    """Transaction status update request model."""
    status: TransactionStatus


class TransactionSummaryResponse(BaseModel):
    """Transaction summary response model."""
    portfolio_id: str
    transaction_count: int
    buy_count: int
    sell_count: int
    transfer_count: int
    fee_count: int
    total_buy_amount: float
    total_sell_amount: float
    total_fees: float
    net_amount: float


class PositionHistoryResponse(BaseModel):
    """Position history response model - from POSHIST DB2 table."""
    account_no: str
    portfolio_id: str
    trans_date: date
    trans_time: str
    trans_type: str
    security_id: str
    quantity: float
    price: float
    amount: float
    fees: float
    total_amount: float
    cost_basis: float
    gain_loss: float

    class Config:
        from_attributes = True


@router.get("/portfolio/{portfolio_id}", response_model=list[TransactionResponse])
async def get_transaction_history(
    portfolio_id: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
    start_date: date | None = Query(None, description="Start date filter"),
    end_date: date | None = Query(None, description="End date filter"),
    transaction_type: str | None = Query(None, description="Transaction type filter (BU/SL/TR/FE)"),
    limit: int = Query(100, ge=1, le=1000, description="Maximum records to return"),
    offset: int = Query(0, ge=0, description="Number of records to skip"),
) -> list[TransactionResponse]:
    """
    Get transaction history for a portfolio.
    Replaces P200-GET-HISTORY in INQHIST.cbl.

    Original COBOL query:
    SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT
    FROM POSHIST
    WHERE ACCOUNT_NO = :WS-ACCOUNT-NO
    ORDER BY TRANS_DATE DESC
    """
    portfolio_service = PortfolioService(db)
    transaction_service = TransactionService(db)

    try:
        portfolio_service.get_portfolio(portfolio_id)

        transactions = transaction_service.get_transaction_history(
            portfolio_id=portfolio_id,
            start_date=start_date,
            end_date=end_date,
            transaction_type=transaction_type,
            limit=limit,
            offset=offset,
        )

        return [
            TransactionResponse(
                transaction_id=t.transaction_id,
                portfolio_id=t.portfolio_id,
                transaction_date=t.transaction_date,
                transaction_time=t.transaction_time.isoformat() if t.transaction_time else "",
                investment_id=t.investment_id,
                transaction_type=t.transaction_type,
                quantity=float(t.quantity or 0),
                price=float(t.price or 0),
                amount=float(t.amount or 0),
                fees=float(t.fees or 0),
                total_amount=float(t.total_amount or 0),
                currency_code=t.currency_code,
                status=t.status,
                process_date=t.process_date.isoformat() if t.process_date else "",
            )
            for t in transactions
        ]
    except PortfolioNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.get("/portfolio/{portfolio_id}/summary", response_model=TransactionSummaryResponse)
async def get_transaction_summary(
    portfolio_id: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
    start_date: date | None = Query(None, description="Start date filter"),
    end_date: date | None = Query(None, description="End date filter"),
) -> TransactionSummaryResponse:
    """
    Get transaction summary for a portfolio.
    Provides aggregated transaction statistics.
    """
    portfolio_service = PortfolioService(db)
    transaction_service = TransactionService(db)

    try:
        portfolio_service.get_portfolio(portfolio_id)

        summary = transaction_service.get_transaction_summary(
            portfolio_id=portfolio_id,
            start_date=start_date,
            end_date=end_date,
        )

        return TransactionSummaryResponse(**summary)
    except PortfolioNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.get("/history/{account_no}", response_model=list[PositionHistoryResponse])
async def get_position_history(
    account_no: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
    start_date: date | None = Query(None, description="Start date filter"),
    end_date: date | None = Query(None, description="End date filter"),
    limit: int = Query(100, ge=1, le=1000, description="Maximum records to return"),
) -> list[PositionHistoryResponse]:
    """
    Get position history from POSHIST table.
    Replaces DB2 query in INQHIST.cbl.
    """
    transaction_service = TransactionService(db)

    history = transaction_service.get_position_history(
        account_no=account_no,
        start_date=start_date,
        end_date=end_date,
        limit=limit,
    )

    return [
        PositionHistoryResponse(
            account_no=h.account_no,
            portfolio_id=h.portfolio_id,
            trans_date=h.trans_date,
            trans_time=h.trans_time.isoformat() if h.trans_time else "",
            trans_type=h.trans_type,
            security_id=h.security_id,
            quantity=float(h.quantity or 0),
            price=float(h.price or 0),
            amount=float(h.amount or 0),
            fees=float(h.fees or 0),
            total_amount=float(h.total_amount or 0),
            cost_basis=float(h.cost_basis or 0),
            gain_loss=float(h.gain_loss or 0),
        )
        for h in history
    ]


@router.get("/{transaction_id}", response_model=TransactionResponse)
async def get_transaction(
    transaction_id: str,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> TransactionResponse:
    """Get a specific transaction by ID."""
    transaction_service = TransactionService(db)

    try:
        t = transaction_service.get_transaction(transaction_id)

        return TransactionResponse(
            transaction_id=t.transaction_id,
            portfolio_id=t.portfolio_id,
            transaction_date=t.transaction_date,
            transaction_time=t.transaction_time.isoformat() if t.transaction_time else "",
            investment_id=t.investment_id,
            transaction_type=t.transaction_type,
            quantity=float(t.quantity or 0),
            price=float(t.price or 0),
            amount=float(t.amount or 0),
            fees=float(t.fees or 0),
            total_amount=float(t.total_amount or 0),
            currency_code=t.currency_code,
            status=t.status,
            process_date=t.process_date.isoformat() if t.process_date else "",
        )
    except TransactionNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.post("", response_model=TransactionResponse, status_code=status.HTTP_201_CREATED)
async def create_transaction(
    request: TransactionCreateRequest,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> TransactionResponse:
    """Create a new transaction."""
    transaction_service = TransactionService(db)

    try:
        t = transaction_service.create_transaction(
            portfolio_id=request.portfolio_id,
            investment_id=request.investment_id,
            transaction_type=request.transaction_type,
            quantity=request.quantity,
            price=request.price,
            user_id=current_user.user_id,
            fees=request.fees,
        )
        db.commit()

        return TransactionResponse(
            transaction_id=t.transaction_id,
            portfolio_id=t.portfolio_id,
            transaction_date=t.transaction_date,
            transaction_time=t.transaction_time.isoformat() if t.transaction_time else "",
            investment_id=t.investment_id,
            transaction_type=t.transaction_type,
            quantity=float(t.quantity or 0),
            price=float(t.price or 0),
            amount=float(t.amount or 0),
            fees=float(t.fees or 0),
            total_amount=float(t.total_amount or 0),
            currency_code=t.currency_code,
            status=t.status,
            process_date=t.process_date.isoformat() if t.process_date else "",
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


@router.patch("/{transaction_id}/status", response_model=TransactionResponse)
async def update_transaction_status(
    transaction_id: str,
    request: TransactionStatusUpdateRequest,
    current_user: Annotated[UserResponse, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> TransactionResponse:
    """Update transaction status."""
    transaction_service = TransactionService(db)

    try:
        t = transaction_service.update_transaction_status(
            transaction_id=transaction_id,
            status=request.status,
            user_id=current_user.user_id,
        )
        db.commit()

        return TransactionResponse(
            transaction_id=t.transaction_id,
            portfolio_id=t.portfolio_id,
            transaction_date=t.transaction_date,
            transaction_time=t.transaction_time.isoformat() if t.transaction_time else "",
            investment_id=t.investment_id,
            transaction_type=t.transaction_type,
            quantity=float(t.quantity or 0),
            price=float(t.price or 0),
            amount=float(t.amount or 0),
            fees=float(t.fees or 0),
            total_amount=float(t.total_amount or 0),
            currency_code=t.currency_code,
            status=t.status,
            process_date=t.process_date.isoformat() if t.process_date else "",
        )
    except TransactionNotFoundError as e:
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
