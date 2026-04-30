from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from models.database import get_db
from schemas.transaction import (
    TransactionCreate, TransactionResponse, TransactionListResponse,
)
from services import transaction_service

router = APIRouter(prefix="/api/transactions", tags=["transactions"])


@router.get("", response_model=TransactionListResponse)
def list_transactions(
    portfolio_id: str | None = None,
    transaction_type: str | None = Query(None, pattern=r"^(BU|SL|TR|FE)$"),
    status: str | None = Query(None, pattern=r"^[PDFR]$"),
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
):
    transactions, total = transaction_service.list_transactions(
        db, portfolio_id, transaction_type, status, skip, limit
    )
    return TransactionListResponse(
        transactions=[TransactionResponse.model_validate(t) for t in transactions],
        total=total,
    )


@router.get("/{transaction_id}", response_model=TransactionResponse)
def get_transaction(transaction_id: str, db: Session = Depends(get_db)):
    txn = transaction_service.get_transaction(db, transaction_id)
    if not txn:
        raise HTTPException(status_code=404, detail="Transaction not found")
    return TransactionResponse.model_validate(txn)


@router.post("", response_model=TransactionResponse, status_code=201)
def create_transaction(data: TransactionCreate, db: Session = Depends(get_db)):
    txn, result = transaction_service.submit_transaction(db, data)
    if not txn:
        raise HTTPException(
            status_code=422,
            detail={
                "return_code": result.return_code,
                "errors": result.errors,
                "warnings": result.warnings,
            },
        )
    return TransactionResponse.model_validate(txn)
