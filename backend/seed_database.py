"""Seed database with sample data — translated from test-data-specs.md."""

import sys
import os
import uuid
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from datetime import date, time, datetime
from models.database import engine, SessionLocal, Base
from models.portfolio import Portfolio
from models.position import Position
from models.transaction import Transaction
from models.history import PositionHistory
from models.error_log import ErrorLog

Base.metadata.create_all(bind=engine)
db = SessionLocal()

PORTFOLIOS = [
    ("PORT0001", "1000000001", "John Smith", "I", "Growth Portfolio", "USD", "H"),
    ("PORT0002", "1000000002", "Jane Doe", "I", "Income Portfolio", "USD", "M"),
    ("PORT0003", "1000000003", "Acme Corporation", "C", "Corporate Treasury", "USD", "L"),
    ("PORT0004", "1000000004", "Smith Family Trust", "T", "Trust Fund", "USD", "M"),
    ("PORT0005", "1000000005", "Robert Johnson", "I", "Retirement Fund", "USD", "M"),
]

POSITIONS = [
    ("PORT0001", "AAPL", "AAPL", "Apple Inc.", 150, 25500.00, 27787.50, 185.25),
    ("PORT0001", "MSFT", "MSFT", "Microsoft Corporation", 100, 34000.00, 37885.00, 378.85),
    ("PORT0001", "GOOGL", "GOOGL", "Alphabet Inc.", 75, 10000.00, 10692.00, 142.56),
    ("PORT0002", "JNJ", "JNJ", "Johnson & Johnson", 200, 30000.00, 31500.00, 157.50),
    ("PORT0002", "PG", "PG", "Procter & Gamble", 150, 22000.00, 23475.00, 156.50),
    ("PORT0002", "KO", "KO", "Coca-Cola Company", 300, 17000.00, 17850.00, 59.50),
    ("PORT0003", "BND", "BND", "Vanguard Bond ETF", 500, 37500.00, 37250.00, 74.50),
    ("PORT0003", "GOVT", "GOVT", "US Treasury Bond ETF", 400, 9600.00, 9560.00, 23.90),
    ("PORT0004", "VTI", "VTI", "Vanguard Total Market ETF", 250, 52000.00, 56250.00, 225.00),
    ("PORT0004", "VXUS", "VXUS", "Vanguard Intl Stock ETF", 300, 16200.00, 16500.00, 55.00),
    ("PORT0005", "SPY", "SPY", "SPDR S&P 500 ETF", 100, 42000.00, 45300.00, 453.00),
    ("PORT0005", "QQQ", "QQQ", "Invesco QQQ Trust", 80, 28000.00, 31200.00, 390.00),
    ("PORT0005", "TSLA", "TSLA", "Tesla Inc.", 50, 10000.00, 12283.50, 245.67),
]

TRANSACTIONS = [
    ("PORT0001", "AAPL", "BU", 50, 170.00, "2024-01-15", "09:30:00"),
    ("PORT0001", "AAPL", "BU", 100, 175.00, "2024-03-20", "10:15:00"),
    ("PORT0001", "MSFT", "BU", 100, 340.00, "2024-02-10", "11:00:00"),
    ("PORT0001", "GOOGL", "BU", 75, 133.33, "2024-04-05", "14:30:00"),
    ("PORT0002", "JNJ", "BU", 200, 150.00, "2024-01-20", "09:45:00"),
    ("PORT0002", "PG", "BU", 150, 146.67, "2024-02-15", "10:30:00"),
    ("PORT0002", "KO", "BU", 300, 56.67, "2024-03-01", "11:15:00"),
    ("PORT0003", "BND", "BU", 500, 75.00, "2024-01-10", "09:00:00"),
    ("PORT0003", "GOVT", "BU", 400, 24.00, "2024-01-10", "09:05:00"),
    ("PORT0004", "VTI", "BU", 250, 208.00, "2024-02-01", "10:00:00"),
    ("PORT0004", "VXUS", "BU", 300, 54.00, "2024-02-01", "10:05:00"),
    ("PORT0005", "SPY", "BU", 100, 420.00, "2024-01-05", "09:30:00"),
    ("PORT0005", "QQQ", "BU", 80, 350.00, "2024-01-05", "09:35:00"),
    ("PORT0005", "TSLA", "BU", 50, 200.00, "2024-03-15", "13:00:00"),
    ("PORT0001", "AAPL", "SL", 20, 182.00, "2024-06-10", "15:00:00"),
    ("PORT0002", "KO", "SL", 50, 58.50, "2024-05-20", "14:00:00"),
]


def seed():
    existing = db.query(Portfolio).first()
    if existing:
        print("Database already seeded, skipping.")
        return

    for pid, acc, name, ctype, pname, curr, risk in PORTFOLIOS:
        db.add(Portfolio(
            portfolio_id=pid,
            account_number=acc,
            client_name=name,
            client_type=ctype,
            portfolio_name=pname,
            currency_code=curr,
            risk_level=risk,
            status="A",
            total_value=0,
            cash_balance=10000.00,
            open_date=date(2024, 1, 1),
        ))

    for pid, inv_id, sym, name, qty, cost, mv, price in POSITIONS:
        db.add(Position(
            id=str(uuid.uuid4()),
            portfolio_id=pid,
            investment_id=inv_id,
            symbol=sym,
            name=name,
            position_date=date.today(),
            quantity=qty,
            cost_basis=cost,
            market_value=mv,
            current_price=price,
            currency="USD",
            status="A",
        ))

    seq_counters: dict[str, int] = {}
    for pid, inv_id, ttype, qty, price, d, t in TRANSACTIONS:
        txn_date = date.fromisoformat(d)
        txn_time = time.fromisoformat(t)
        key = f"{pid}-{d}"
        seq_counters[key] = seq_counters.get(key, 0) + 1
        seq = str(seq_counters[key]).zfill(6)
        txn_id = txn_date.strftime("%Y%m%d") + txn_time.strftime("%H%M%S") + seq

        db.add(Transaction(
            transaction_id=txn_id,
            portfolio_id=pid,
            investment_id=inv_id,
            transaction_date=txn_date,
            transaction_time=txn_time,
            sequence_no=seq,
            transaction_type=ttype,
            quantity=qty,
            price=price,
            amount=round(qty * price, 2),
            currency="USD",
            status="D",
            process_date=datetime.combine(txn_date, txn_time),
            process_user="SYSTEM",
        ))

    for pid, inv_id, sym, name, qty, cost, mv, price in POSITIONS:
        avg = cost / qty if qty > 0 else 0
        db.add(PositionHistory(
            id=str(uuid.uuid4()),
            portfolio_id=pid,
            investment_id=inv_id,
            record_date=date.today(),
            share_balance=qty,
            cost_basis=cost,
            market_value=mv,
            avg_cost=round(avg, 4),
            event_type="SEED",
        ))

    db.flush()
    for pid, _, _, _, _, _, _ in PORTFOLIOS:
        port_positions = [p for p in POSITIONS if p[0] == pid]
        total = sum(p[6] for p in port_positions)
        db.query(Portfolio).filter(Portfolio.portfolio_id == pid).update(
            {"total_value": total}, synchronize_session="fetch"
        )

    db.commit()
    print(f"Seeded {len(PORTFOLIOS)} portfolios, {len(POSITIONS)} positions, {len(TRANSACTIONS)} transactions.")


if __name__ == "__main__":
    seed()
