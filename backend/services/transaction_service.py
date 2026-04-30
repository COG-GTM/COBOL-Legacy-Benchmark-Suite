"""Transaction service — business logic translated from TRNVAL00.cbl and POSUPD00."""

import uuid
from decimal import Decimal
from datetime import datetime, date, time
from sqlalchemy.orm import Session
from models.transaction import Transaction
from models.position import Position
from models.history import PositionHistory
from validation.transaction_validator import validate_transaction, ValidationResult
from schemas.transaction import TransactionCreate


def _generate_transaction_id() -> str:
    now = datetime.utcnow()
    return now.strftime("%Y%m%d%H%M%S") + str(uuid.uuid4().hex[:6]).upper()


def _generate_sequence_no(db: Session, portfolio_id: str, txn_date: date) -> str:
    count = (
        db.query(Transaction)
        .filter(
            Transaction.portfolio_id == portfolio_id,
            Transaction.transaction_date == txn_date,
        )
        .count()
    )
    return str(count + 1).zfill(6)


def submit_transaction(db: Session, data: TransactionCreate) -> tuple[Transaction | None, ValidationResult]:
    """Validate and process a transaction (TRNVAL00 + POSUPD00 combined)."""
    result = validate_transaction(
        db,
        data.portfolio_id,
        data.investment_id,
        data.transaction_type,
        data.quantity,
        data.price,
    )

    if not result.is_valid:
        return None, result

    now = datetime.utcnow()
    txn_date = now.date()
    txn_time = now.time()
    amount = Decimal(str(data.quantity)) * Decimal(str(data.price))

    txn = Transaction(
        transaction_id=_generate_transaction_id(),
        portfolio_id=data.portfolio_id,
        investment_id=data.investment_id,
        transaction_date=txn_date,
        transaction_time=txn_time,
        sequence_no=_generate_sequence_no(db, data.portfolio_id, txn_date),
        transaction_type=data.transaction_type,
        quantity=data.quantity,
        price=data.price,
        amount=float(amount),
        currency=data.currency,
        status="D",
        process_date=now,
        process_user="SYSTEM",
    )
    db.add(txn)

    _update_position(db, data, amount, now)

    db.commit()
    db.refresh(txn)
    return txn, result


def _update_position(db: Session, data: TransactionCreate, amount: Decimal, now: datetime):
    """Update position records (POSUPD00 logic)."""
    position = (
        db.query(Position)
        .filter(
            Position.portfolio_id == data.portfolio_id,
            Position.investment_id == data.investment_id,
            Position.status == "A",
        )
        .first()
    )

    if data.transaction_type == "BU":
        if position:
            old_qty = Decimal(str(position.quantity))
            new_qty = old_qty + Decimal(str(data.quantity))
            old_cost = Decimal(str(position.cost_basis))
            new_cost = old_cost + amount
            position.quantity = float(new_qty)
            position.cost_basis = float(new_cost)
            new_price = Decimal(str(data.price))
            position.market_value = float(new_qty * new_price)
            position.current_price = float(new_price)
            position.updated_at = now
        else:
            position = Position(
                id=str(uuid.uuid4()),
                portfolio_id=data.portfolio_id,
                investment_id=data.investment_id,
                symbol=data.investment_id[:6].strip(),
                name=data.investment_id,
                position_date=now.date(),
                quantity=data.quantity,
                cost_basis=float(amount),
                market_value=float(amount),
                current_price=data.price,
                currency=data.currency,
                status="A",
            )
            db.add(position)

    elif data.transaction_type == "SL":
        if position:
            old_qty = Decimal(str(position.quantity))
            sell_qty = Decimal(str(data.quantity))
            new_qty = old_qty - sell_qty
            old_cost = Decimal(str(position.cost_basis))
            cost_reduction = (sell_qty / old_qty) * old_cost if old_qty > 0 else Decimal("0")
            position.quantity = float(new_qty)
            position.cost_basis = float(old_cost - cost_reduction)
            new_price = Decimal(str(data.price))
            position.market_value = float(new_qty * new_price)
            position.current_price = float(new_price)
            position.updated_at = now
            if new_qty <= 0:
                position.status = "C"

    if position and position.id:
        _record_history(db, position, now)


def _record_history(db: Session, position: Position, now: datetime):
    """Record position history snapshot (HISTLD00 logic)."""
    qty = Decimal(str(position.quantity))
    cost = Decimal(str(position.cost_basis))
    avg = cost / qty if qty > 0 else Decimal("0")

    history = PositionHistory(
        id=str(uuid.uuid4()),
        portfolio_id=position.portfolio_id,
        investment_id=position.investment_id,
        record_date=now.date(),
        share_balance=float(qty),
        cost_basis=float(cost),
        market_value=float(position.market_value or 0),
        avg_cost=float(avg),
        event_type="TRANSACTION",
    )
    db.add(history)


def list_transactions(
    db: Session,
    portfolio_id: str | None = None,
    transaction_type: str | None = None,
    status: str | None = None,
    skip: int = 0,
    limit: int = 50,
):
    query = db.query(Transaction)
    if portfolio_id:
        query = query.filter(Transaction.portfolio_id == portfolio_id)
    if transaction_type:
        query = query.filter(Transaction.transaction_type == transaction_type)
    if status:
        query = query.filter(Transaction.status == status)
    total = query.count()
    transactions = query.order_by(Transaction.created_at.desc()).offset(skip).limit(limit).all()
    return transactions, total


def get_transaction(db: Session, transaction_id: str) -> Transaction | None:
    return db.query(Transaction).filter(Transaction.transaction_id == transaction_id).first()
