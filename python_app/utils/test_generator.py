"""Test Data Generator module - replaces TSTGEN00.cbl.

Generates synthetic test data for portfolio, transaction, error,
and volume testing scenarios.

COBOL program flow (EVALUATE LS-TST-TYPE):
- PORTFOLIO: Generate portfolio test data (P100-GEN-PORTFOLIO)
- TRANSACTION: Generate transaction test data (P200-GEN-TRANSACTION)
- ERROR: Generate error condition data (P300-GEN-ERROR)
- VOLUME: Generate high-volume test data (P400-GEN-VOLUME)
"""

import logging
import random
import string
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Any

from python_app.models.portfolio import ClientType, PortfolioRecord, PortfolioStatus
from python_app.models.position import PositionRecord, PositionStatus
from python_app.models.transaction import (
    TransactionRecord,
    TransactionStatus,
    TransactionType,
)

logger = logging.getLogger("portfolio.utils.test_generator")

# Test data constants matching COBOL TSTGEN00
CURRENCIES = ["USD", "EUR", "GBP", "JPY", "CHF"]
INVESTMENT_PREFIXES = ["STK", "BND", "FND", "ETF", "OPT"]


class TestDataGenerator:
    """Test data generator replacing TSTGEN00.cbl.

    Generates realistic synthetic data for testing the
    portfolio management system.
    """

    def __init__(self, seed: int | None = None) -> None:
        self.rng = random.Random(seed)
        self.generated_counts: dict[str, int] = {
            "portfolios": 0,
            "positions": 0,
            "transactions": 0,
        }

    def gen_portfolio(self, count: int = 1) -> list[PortfolioRecord]:
        """Generate portfolio test data - replaces P100-GEN-PORTFOLIO."""
        portfolios: list[PortfolioRecord] = []
        for i in range(count):
            port_id = f"P{i + 1:07d}"
            acct_no = f"A{self.rng.randint(100000, 999999):010d}"
            portfolio = PortfolioRecord(
                id=port_id[:8],
                account_no=acct_no[:10],
                client_name=f"Client {i + 1}",
                client_type=self.rng.choice(list(ClientType)),
                create_date=(
                    datetime.now() - timedelta(days=self.rng.randint(30, 3650))
                ).strftime("%Y%m%d"),
                status=PortfolioStatus.ACTIVE,
                total_value=Decimal(str(self.rng.uniform(10000, 10000000))).quantize(
                    Decimal("0.01")
                ),
                cash_balance=Decimal(str(self.rng.uniform(1000, 500000))).quantize(
                    Decimal("0.01")
                ),
            )
            portfolios.append(portfolio)
            self.generated_counts["portfolios"] += 1

        logger.info("TSTGEN00 PORTFOLIO: generated %d portfolios", count)
        return portfolios

    def gen_transaction(
        self,
        count: int = 1,
        portfolio_ids: list[str] | None = None,
    ) -> list[TransactionRecord]:
        """Generate transaction test data - replaces P200-GEN-TRANSACTION."""
        transactions: list[TransactionRecord] = []

        if not portfolio_ids:
            portfolio_ids = [f"P{i + 1:07d}"[:8] for i in range(5)]

        for i in range(count):
            txn_date = (
                datetime.now() - timedelta(days=self.rng.randint(0, 365))
            ).strftime("%Y%m%d")
            txn_time = f"{self.rng.randint(0, 23):02d}{self.rng.randint(0, 59):02d}{self.rng.randint(0, 59):02d}"
            port_id = self.rng.choice(portfolio_ids)
            inv_prefix = self.rng.choice(INVESTMENT_PREFIXES)
            inv_id = f"{inv_prefix}{self.rng.randint(10000, 99999)}"

            quantity = Decimal(str(self.rng.uniform(1, 10000))).quantize(Decimal("0.0001"))
            price = Decimal(str(self.rng.uniform(1, 5000))).quantize(Decimal("0.0001"))
            amount = (quantity * price).quantize(Decimal("0.01"))

            txn = TransactionRecord(
                date=txn_date,
                time=txn_time,
                portfolio_id=port_id,
                sequence_no=f"{i + 1:06d}",
                investment_id=inv_id[:10],
                type=self.rng.choice(list(TransactionType)),
                quantity=quantity,
                price=price,
                amount=amount,
                currency=self.rng.choice(CURRENCIES),
                status=TransactionStatus.PENDING,
            )
            transactions.append(txn)
            self.generated_counts["transactions"] += 1

        logger.info("TSTGEN00 TRANSACTION: generated %d transactions", count)
        return transactions

    def gen_positions(
        self,
        count: int = 1,
        portfolio_ids: list[str] | None = None,
    ) -> list[PositionRecord]:
        """Generate position test data."""
        positions: list[PositionRecord] = []

        if not portfolio_ids:
            portfolio_ids = [f"P{i + 1:07d}"[:8] for i in range(5)]

        for i in range(count):
            port_id = self.rng.choice(portfolio_ids)
            inv_prefix = self.rng.choice(INVESTMENT_PREFIXES)
            inv_id = f"{inv_prefix}{self.rng.randint(10000, 99999)}"

            quantity = Decimal(str(self.rng.uniform(10, 10000))).quantize(Decimal("0.0001"))
            cost_basis = Decimal(str(self.rng.uniform(1000, 1000000))).quantize(Decimal("0.01"))
            market_value = cost_basis * Decimal(str(self.rng.uniform(0.5, 2.0)))
            market_value = market_value.quantize(Decimal("0.01"))

            position = PositionRecord(
                portfolio_id=port_id,
                date=datetime.now().strftime("%Y%m%d"),
                investment_id=inv_id[:10],
                quantity=quantity,
                cost_basis=cost_basis,
                market_value=market_value,
                currency=self.rng.choice(CURRENCIES),
                status=PositionStatus.ACTIVE,
            )
            positions.append(position)
            self.generated_counts["positions"] += 1

        logger.info("TSTGEN00 POSITION: generated %d positions", count)
        return positions

    def gen_error_data(self, count: int = 1) -> list[TransactionRecord]:
        """Generate error condition data - replaces P300-GEN-ERROR.

        Creates deliberately invalid records for error handling testing.
        """
        error_records: list[TransactionRecord] = []
        error_types = ["bad_date", "bad_currency", "zero_amount", "negative_qty", "empty_portfolio"]

        for i in range(count):
            error_type = self.rng.choice(error_types)
            txn = TransactionRecord(
                date="99999999" if error_type == "bad_date" else datetime.now().strftime("%Y%m%d"),
                time="120000",
                portfolio_id="" if error_type == "empty_portfolio" else f"P{i + 1:07d}"[:8],
                sequence_no=f"{i + 1:06d}",
                investment_id=f"ERR{i + 1:07d}"[:10],
                type=TransactionType.BUY,
                quantity=Decimal("-100.0000") if error_type == "negative_qty" else Decimal("100.0000"),
                price=Decimal("50.0000"),
                amount=Decimal("0") if error_type == "zero_amount" else Decimal("5000.00"),
                currency="XX" if error_type == "bad_currency" else "USD",
                status=TransactionStatus.PENDING,
            )
            error_records.append(txn)

        logger.info("TSTGEN00 ERROR: generated %d error records", count)
        return error_records

    def gen_volume(
        self,
        portfolio_count: int = 100,
        transactions_per_portfolio: int = 100,
    ) -> dict[str, Any]:
        """Generate high-volume test data - replaces P400-GEN-VOLUME."""
        portfolios = self.gen_portfolio(portfolio_count)
        port_ids = [p.id for p in portfolios]
        total_txns = portfolio_count * transactions_per_portfolio
        transactions = self.gen_transaction(total_txns, port_ids)
        positions = self.gen_positions(portfolio_count * 10, port_ids)

        logger.info(
            "TSTGEN00 VOLUME: %d portfolios, %d transactions, %d positions",
            len(portfolios), len(transactions), len(positions),
        )

        return {
            "portfolios": portfolios,
            "transactions": transactions,
            "positions": positions,
        }

    def get_stats(self) -> dict[str, int]:
        """Get generation statistics."""
        return dict(self.generated_counts)
