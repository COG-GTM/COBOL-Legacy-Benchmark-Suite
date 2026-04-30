"""Seed database with realistic portfolio data.

Replaces TSTGEN00 (test data generator) from the legacy COBOL system.
Generates portfolios, positions, and transaction history that mirror
the data patterns of the original VSAM/DB2 stores.
"""

from datetime import date, datetime, timedelta
import random

from sqlalchemy.orm import Session

from .models import Portfolio, Position, Transaction

INVESTMENTS = [
    ("AAPL000001", "Apple Inc."),
    ("MSFT000001", "Microsoft Corp."),
    ("GOOGL00001", "Alphabet Inc."),
    ("AMZN000001", "Amazon.com Inc."),
    ("NVDA000001", "NVIDIA Corp."),
    ("META000001", "Meta Platforms"),
    ("TSLA000001", "Tesla Inc."),
    ("JPM0000001", "JPMorgan Chase"),
    ("V000000001", "Visa Inc."),
    ("JNJ0000001", "Johnson & Johnson"),
    ("UNH0000001", "UnitedHealth Grp"),
    ("PG00000001", "Procter & Gamble"),
    ("HD00000001", "Home Depot Inc."),
    ("BAC0000001", "Bank of America"),
    ("XOM0000001", "Exxon Mobil Corp"),
]

CLIENTS = [
    ("PORT0001", "1000000001", "Meridian Capital Partners", "C", "H"),
    ("PORT0002", "1000000002", "Sarah J. Mitchell", "I", "M"),
    ("PORT0003", "1000000003", "Westfield Family Trust", "T", "L"),
    ("PORT0004", "1000000004", "Apex Growth Fund LLC", "C", "H"),
    ("PORT0005", "1000000005", "Robert A. Chen", "I", "M"),
    ("PORT0006", "1000000006", "Horizon Retirement Trust", "T", "L"),
    ("PORT0007", "1000000007", "James D. Whitfield", "I", "A"),
    ("PORT0008", "1000000008", "Sterling Ventures Inc.", "C", "H"),
    ("PORT0009", "1000000009", "Elena K. Vasquez", "I", "M"),
    ("PORT0010", "1000000010", "Pacific Coast Trust", "T", "L"),
]


def seed_database(db: Session) -> None:
    if db.query(Portfolio).count() > 0:
        return

    random.seed(42)
    today = date.today()
    seq_counter = 0

    for port_id, acct, name, ctype, risk in CLIENTS:
        open_dt = today - timedelta(days=random.randint(365, 1800))
        is_active = random.random() > 0.1
        status = "A" if is_active else random.choice(["C", "S"])

        portfolio = Portfolio(
            portfolio_id=port_id,
            account_no=acct,
            client_name=name,
            client_type=ctype,
            currency_code="USD",
            risk_level=risk,
            status=status,
            total_value=0,
            cash_balance=round(random.uniform(5000, 500000), 2),
            open_date=open_dt,
            close_date=None if is_active else today - timedelta(days=random.randint(1, 90)),
            last_maint_date=datetime.utcnow(),
            last_maint_user="SEEDPROC",
        )
        db.add(portfolio)

        num_positions = random.randint(3, 8)
        selected = random.sample(INVESTMENTS, num_positions)
        total_mv = 0.0
        total_cb = 0.0

        for inv_id, inv_name in selected:
            qty = round(random.uniform(10, 5000), 4)
            cost_per = round(random.uniform(20, 800), 4)
            cost_basis = round(qty * cost_per, 2)
            gain_factor = random.uniform(0.7, 1.8)
            market_value = round(cost_basis * gain_factor, 2)

            pos = Position(
                portfolio_id=port_id,
                investment_id=inv_id,
                investment_name=inv_name,
                position_date=today,
                quantity=qty,
                cost_basis=cost_basis,
                market_value=market_value,
                currency_code="USD",
                status="A" if is_active else "C",
                last_maint_date=datetime.utcnow(),
                last_maint_user="SEEDPROC",
            )
            db.add(pos)
            total_mv += market_value
            total_cb += cost_basis

            num_txns = random.randint(2, 6)
            for t in range(num_txns):
                seq_counter += 1
                txn_date = open_dt + timedelta(days=random.randint(0, (today - open_dt).days))
                txn_type = random.choice(["BU", "BU", "BU", "SL", "FE"])
                txn_qty = round(random.uniform(1, qty / 2), 4) if txn_type != "FE" else 0
                txn_price = round(cost_per * random.uniform(0.8, 1.3), 4)
                txn_amount = round(txn_qty * txn_price, 2) if txn_type != "FE" else round(random.uniform(5, 200), 2)
                txn_status = random.choice(["D", "D", "D", "D", "P"])

                txn = Transaction(
                    transaction_id=txn_date.strftime("%Y%m%d") + f"{seq_counter:06d}" + f"{t:06d}",
                    portfolio_id=port_id,
                    investment_id=inv_id,
                    transaction_date=txn_date,
                    transaction_type=txn_type,
                    quantity=txn_qty,
                    price=txn_price,
                    amount=txn_amount,
                    currency_code="USD",
                    status=txn_status,
                    process_date=datetime.combine(txn_date, datetime.min.time()),
                    process_user="BCHCTL00",
                )
                db.add(txn)

        portfolio.total_value = round(total_mv + portfolio.cash_balance, 2)
        db.flush()

    db.commit()
