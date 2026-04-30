"""Seed the portfolio database with sample data matching the COBOL legacy system."""
from datetime import datetime, date, time
from decimal import Decimal
from sqlalchemy.orm import sessionmaker
from models.database import engine, Base, Portfolio, Position
from models.transactions import Transaction

Base.metadata.create_all(bind=engine)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def seed_portfolio_data():
    session = SessionLocal()
    try:
        existing = session.query(Portfolio).filter_by(port_id="PF-12345", account_no="1234567890").first()
        if existing:
            print("Portfolio data already exists. Skipping seeding.")
            return

        portfolio = Portfolio(
            port_id="PF-12345",
            account_no="1234567890",
            client_name="Sample Client",
            client_type="I",
            create_date=date(2024, 1, 15),
            last_maint=date.today(),
            status="A",
            total_value=Decimal("125498.50"),
            cash_balance=Decimal("252.00"),
            last_user="SYSTEM",
            last_trans="SEED001",
        )
        session.add(portfolio)
        session.flush()

        positions = [
            Position(
                portfolio_id="PF-12345", date=date.today(), investment_id="AAPL",
                quantity=Decimal("150.0000"), cost_basis=Decimal("25500.00"),
                market_value=Decimal("27787.50"), currency="USD", status="A",
                last_maint_date=datetime.now(), last_maint_user="SYSTEM",
            ),
            Position(
                portfolio_id="PF-12345", date=date.today(), investment_id="MSFT",
                quantity=Decimal("100.0000"), cost_basis=Decimal("34000.00"),
                market_value=Decimal("37885.00"), currency="USD", status="A",
                last_maint_date=datetime.now(), last_maint_user="SYSTEM",
            ),
            Position(
                portfolio_id="PF-12345", date=date.today(), investment_id="GOOGL",
                quantity=Decimal("75.0000"), cost_basis=Decimal("10000.00"),
                market_value=Decimal("10692.00"), currency="USD", status="A",
                last_maint_date=datetime.now(), last_maint_user="SYSTEM",
            ),
            Position(
                portfolio_id="PF-12345", date=date.today(), investment_id="TSLA",
                quantity=Decimal("200.0000"), cost_basis=Decimal("47748.00"),
                market_value=Decimal("49134.00"), currency="USD", status="A",
                last_maint_date=datetime.now(), last_maint_user="SYSTEM",
            ),
        ]
        for p in positions:
            session.add(p)

        transactions = [
            Transaction(
                date=date(2024, 1, 15), time=time(10, 30, 0),
                portfolio_id="PF-12345", sequence_no="000001",
                investment_id="AAPL", type="BU",
                quantity=Decimal("150.0000"), price=Decimal("170.0000"),
                amount=Decimal("25500.00"), currency="USD", status="D",
                process_date=datetime(2024, 1, 15, 10, 30), process_user="SYSTEM",
            ),
            Transaction(
                date=date(2024, 1, 20), time=time(14, 15, 0),
                portfolio_id="PF-12345", sequence_no="000002",
                investment_id="MSFT", type="BU",
                quantity=Decimal("100.0000"), price=Decimal("340.0000"),
                amount=Decimal("34000.00"), currency="USD", status="D",
                process_date=datetime(2024, 1, 20, 14, 15), process_user="SYSTEM",
            ),
            Transaction(
                date=date(2024, 2, 5), time=time(9, 0, 0),
                portfolio_id="PF-12345", sequence_no="000003",
                investment_id="GOOGL", type="BU",
                quantity=Decimal("75.0000"), price=Decimal("133.3333"),
                amount=Decimal("10000.00"), currency="USD", status="D",
                process_date=datetime(2024, 2, 5, 9, 0), process_user="SYSTEM",
            ),
            Transaction(
                date=date(2024, 2, 10), time=time(11, 45, 0),
                portfolio_id="PF-12345", sequence_no="000004",
                investment_id="TSLA", type="BU",
                quantity=Decimal("200.0000"), price=Decimal("238.7400"),
                amount=Decimal("47748.00"), currency="USD", status="D",
                process_date=datetime(2024, 2, 10, 11, 45), process_user="SYSTEM",
            ),
            Transaction(
                date=date(2024, 3, 1), time=time(10, 0, 0),
                portfolio_id="PF-12345", sequence_no="000005",
                investment_id="AAPL", type="FE",
                quantity=Decimal("0.0000"), price=Decimal("0.0000"),
                amount=Decimal("12.50"), currency="USD", status="D",
                process_date=datetime(2024, 3, 1, 10, 0), process_user="SYSTEM",
            ),
            Transaction(
                date=date(2024, 4, 15), time=time(15, 30, 0),
                portfolio_id="PF-12345", sequence_no="000006",
                investment_id="MSFT", type="SL",
                quantity=Decimal("25.0000"), price=Decimal("390.0000"),
                amount=Decimal("9750.00"), currency="USD", status="D",
                process_date=datetime(2024, 4, 15, 15, 30), process_user="SYSTEM",
            ),
        ]
        for t in transactions:
            session.add(t)

        session.commit()
        print(f"Seeded portfolio {portfolio.port_id} for account {portfolio.account_no}")
        print(f"Added {len(positions)} positions and {len(transactions)} transactions")

    except Exception as e:
        session.rollback()
        print(f"Error seeding database: {e}")
        raise
    finally:
        session.close()


if __name__ == "__main__":
    seed_portfolio_data()
