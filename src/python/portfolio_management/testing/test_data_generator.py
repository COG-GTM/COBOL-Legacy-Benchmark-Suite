"""Test Data Generator - migrated from TSTGEN00.cbl.

Generates comprehensive test data for all system components including
portfolio records, position records, transaction records, and history
records for testing and validation.
"""

import logging
import random
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Optional

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.position import PositionRecord
from portfolio_management.models.transaction import TransactionRecord
from portfolio_management.models.history import HistoryRecord
from portfolio_management.models.common import ReturnCode, TransactionType

logger = logging.getLogger(__name__)

PROGRAM_ID = "TSTGEN00"


class TestDataGenerator:
    def __init__(self, seed: Optional[int] = None):
        self._seed = seed
        if seed is not None:
            random.seed(seed)
        self._portfolios: list[PortfolioRecord] = []
        self._positions: list[PositionRecord] = []
        self._transactions: list[TransactionRecord] = []
        self._history: list[HistoryRecord] = []

    def generate_all(
        self,
        num_portfolios: int = 10,
        positions_per_portfolio: int = 5,
        transactions_per_portfolio: int = 20,
    ) -> int:
        self._portfolios = self._generate_portfolios(num_portfolios)
        self._positions = self._generate_positions(
            self._portfolios, positions_per_portfolio
        )
        self._transactions = self._generate_transactions(
            self._portfolios, transactions_per_portfolio
        )
        self._history = self._generate_history(self._transactions)

        logger.info(
            "Test data generated:\n"
            "  Portfolios:    %d\n"
            "  Positions:     %d\n"
            "  Transactions:  %d\n"
            "  History:       %d",
            len(self._portfolios),
            len(self._positions),
            len(self._transactions),
            len(self._history),
        )
        return ReturnCode.SUCCESS

    def _generate_portfolios(self, count: int) -> list[PortfolioRecord]:
        records = []
        current_date = datetime.now().strftime("%Y%m%d")
        client_types = ["I", "C", "T"]

        for i in range(count):
            total_value = Decimal(str(round(random.uniform(10000, 5000000), 2)))
            cash_pct = random.uniform(0.05, 0.20)
            cash_balance = Decimal(str(round(float(total_value) * cash_pct, 2)))

            record = PortfolioRecord(
                port_id=f"PORT{i:04d}",
                account_no=f"{1000000000 + i}",
                client_name=f"TEST CLIENT {i:05d}",
                client_type=random.choice(client_types),
                create_date=current_date,
                last_maint=current_date,
                status="A",
                total_value=total_value,
                cash_balance=cash_balance,
                audit_timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
                audit_user="TSTGEN",
            )
            records.append(record)

        return records

    def _generate_positions(
        self, portfolios: list[PortfolioRecord], per_portfolio: int
    ) -> list[PositionRecord]:
        records = []
        current_date = datetime.now().strftime("%Y%m%d")
        security_prefixes = ["STK", "BND", "MUT", "ETF", "OPT"]

        for portfolio in portfolios:
            for j in range(per_portfolio):
                prefix = random.choice(security_prefixes)
                quantity = Decimal(str(round(random.uniform(10, 10000), 2)))
                price = Decimal(str(round(random.uniform(5, 500), 4)))
                market_value = Decimal(str(round(float(quantity) * float(price), 2)))
                cost_basis = Decimal(
                    str(round(float(market_value) * random.uniform(0.8, 1.2), 2))
                )

                record = PositionRecord(
                    portfolio_id=portfolio.port_id,
                    position_date=current_date,
                    investment_id=f"{prefix}{j:05d}",
                    quantity=quantity,
                    cost_basis=cost_basis,
                    market_value=market_value,
                    audit_timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
                    audit_user="TSTGEN",
                )
                records.append(record)

        return records

    def _generate_transactions(
        self, portfolios: list[PortfolioRecord], per_portfolio: int
    ) -> list[TransactionRecord]:
        records = []
        trans_types = [
            TransactionType.BUY,
            TransactionType.SELL,
            TransactionType.FEE,
        ]

        for portfolio in portfolios:
            base_date = datetime.now() - timedelta(days=30)

            for j in range(per_portfolio):
                trans_date = base_date + timedelta(days=random.randint(0, 30))
                trans_type = random.choice(trans_types)
                quantity = Decimal(str(round(random.uniform(1, 1000), 2)))
                price = Decimal(str(round(random.uniform(5, 500), 4)))
                amount = Decimal(str(round(float(quantity) * float(price), 2)))

                record = TransactionRecord(
                    trans_date=trans_date.strftime("%Y%m%d"),
                    trans_time=trans_date.strftime("%H%M%S"),
                    portfolio_id=portfolio.port_id,
                    sequence_no=f"{j:06d}",
                    investment_id=f"STK{j:05d}",
                    trans_type=trans_type,
                    quantity=quantity,
                    price=price,
                    amount=amount,
                    audit_timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
                    audit_user="TSTGEN",
                )
                records.append(record)

        return records

    def _generate_history(
        self, transactions: list[TransactionRecord]
    ) -> list[HistoryRecord]:
        records = []
        for transaction in transactions:
            record = HistoryRecord(
                account_no="",
                portfolio_id=transaction.portfolio_id,
                trans_date=transaction.trans_date,
                trans_time=transaction.trans_time,
                trans_type=transaction.trans_type,
                security_id=transaction.investment_id,
                quantity=transaction.quantity,
                price=transaction.price,
                amount=transaction.amount,
            )
            records.append(record)
        return records

    def save_to_files(self, output_dir: str) -> int:
        import os

        os.makedirs(output_dir, exist_ok=True)

        try:
            with open(os.path.join(output_dir, "portfolios.dat"), "w") as f:
                for r in self._portfolios:
                    f.write(
                        f"{r.port_id}|{r.account_no}|{r.client_name}|"
                        f"{r.client_type}|{r.create_date}|{r.last_maint}|"
                        f"{r.status}|{r.total_value}|{r.cash_balance}\n"
                    )

            with open(os.path.join(output_dir, "positions.dat"), "w") as f:
                for r in self._positions:
                    f.write(
                        f"{r.portfolio_id}|{r.position_date}|{r.investment_id}|"
                        f"{r.quantity}|{r.cost_basis}|{r.market_value}\n"
                    )

            with open(os.path.join(output_dir, "transactions.dat"), "w") as f:
                for r in self._transactions:
                    f.write(
                        f"{r.trans_date}|{r.trans_time}|{r.portfolio_id}|"
                        f"{r.sequence_no}|{r.investment_id}|{r.trans_type}|"
                        f"{r.quantity}|{r.price}|{r.amount}\n"
                    )

            logger.info("Test data files saved to: %s", output_dir)
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving test data: %s", e)
            return ReturnCode.ERROR

    @property
    def portfolios(self) -> list[PortfolioRecord]:
        return list(self._portfolios)

    @property
    def positions(self) -> list[PositionRecord]:
        return list(self._positions)

    @property
    def transactions(self) -> list[TransactionRecord]:
        return list(self._transactions)

    @property
    def history(self) -> list[HistoryRecord]:
        return list(self._history)
