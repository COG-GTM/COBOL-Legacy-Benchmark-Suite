"""Test Data Generator.

Replaces: TSTGEN00 (src/programs/test/TSTGEN00.cbl)

Generates synthetic portfolio and transaction data for testing
and benchmarking using the faker library with seeded random
for reproducibility.

Original COBOL flow (TSTGEN00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE (set up random seed)
    2000-GENERATE-DATA
      2100-GENERATE-PORTFOLIOS
      2200-GENERATE-POSITIONS
      2300-GENERATE-TRANSACTIONS
      2400-GENERATE-HISTORY
    3000-WRITE-OUTPUT
    4000-FINALIZE
"""

from __future__ import annotations

import logging
import random
from datetime import date, datetime, time
from decimal import Decimal

from faker import Faker

from portfolio_manager.models.copybook_models import (
    CurrencyCode,
    TransactionRecord,
    TransactionStatus,
    TransactionType,
)
from portfolio_manager.models.database import (
    AuditLog,
    InvestmentPosition,
    PortfolioMaster,
    PositionHistory,
    TransactionHistory,
)

logger = logging.getLogger(__name__)

# Seeded faker for reproducibility (replaces TSTGEN00 random seed)
DEFAULT_SEED = 42


class TestDataGenerator:
    """Generate synthetic test data.

    Replaces TSTGEN00 (src/programs/test/TSTGEN00.cbl).
    Uses faker with seeded random for reproducibility.
    """

    PROGRAM_ID = "TSTGEN00"

    # Constants matching COBOL test data patterns
    ACCOUNT_TYPES = ["IN", "CO", "TR", "RT", "MF"]
    BRANCH_IDS = ["01", "02", "03", "04", "05"]
    RISK_LEVELS = ["L", "M", "H"]
    CURRENCIES = ["USD", "EUR", "GBP", "JPY", "CAD"]
    INVESTMENT_PREFIXES = [
        "AAPL", "GOOG", "MSFT", "AMZN", "META",
        "TSLA", "NVDA", "JPM", "BAC", "WFC",
    ]

    def __init__(self, seed: int = DEFAULT_SEED):
        self._faker = Faker()
        Faker.seed(seed)
        random.seed(seed)

    # -----------------------------------------------------------------------
    # 2100-GENERATE-PORTFOLIOS
    # -----------------------------------------------------------------------

    def generate_portfolios(self, count: int = 10) -> list[PortfolioMaster]:
        """Generate synthetic portfolio master records.

        Replaces TSTGEN00 paragraph 2100-GENERATE-PORTFOLIOS.

        Args:
            count: Number of portfolios to generate.

        Returns:
            List of PortfolioMaster ORM objects.
        """
        portfolios = []
        for i in range(count):
            portfolio_id = f"PORT{i + 1:04d}"
            open_date = self._faker.date_between(
                start_date=date(2020, 1, 1), end_date=date(2025, 12, 31)
            )
            status = random.choice(["A", "A", "A", "A", "C"])  # 80% active

            portfolio = PortfolioMaster(
                portfolio_id=portfolio_id,
                account_type=random.choice(self.ACCOUNT_TYPES),
                branch_id=random.choice(self.BRANCH_IDS),
                client_id=f"CLT{i + 1:06d}",
                portfolio_name=f"{self._faker.company()} Portfolio",
                currency_code=random.choice(self.CURRENCIES),
                risk_level=random.choice(self.RISK_LEVELS),
                status=status,
                open_date=open_date,
                close_date=(
                    self._faker.date_between(start_date=open_date, end_date=date(2026, 3, 1))
                    if status == "C"
                    else None
                ),
                last_maint_date=datetime.now(),
                last_maint_user="TSTGEN",
            )
            portfolios.append(portfolio)

        logger.info(
            "%s: Generated %d portfolio records", self.PROGRAM_ID, count
        )
        return portfolios

    # -----------------------------------------------------------------------
    # 2200-GENERATE-POSITIONS
    # -----------------------------------------------------------------------

    def generate_positions(
        self,
        portfolios: list[PortfolioMaster],
        positions_per_portfolio: int = 5,
    ) -> list[InvestmentPosition]:
        """Generate synthetic investment position records.

        Replaces TSTGEN00 paragraph 2200-GENERATE-POSITIONS.

        Args:
            portfolios: List of portfolio records to create positions for.
            positions_per_portfolio: Number of positions per portfolio.

        Returns:
            List of InvestmentPosition ORM objects.
        """
        positions = []
        for portfolio in portfolios:
            if portfolio.status == "C":
                continue

            investments = random.sample(
                self.INVESTMENT_PREFIXES,
                min(positions_per_portfolio, len(self.INVESTMENT_PREFIXES)),
            )

            for inv_prefix in investments:
                inv_id = f"{inv_prefix}{random.randint(100, 999):03d}"
                quantity = Decimal(str(random.randint(10, 10000)))
                price = Decimal(str(round(random.uniform(10.0, 500.0), 4)))
                cost_basis = quantity * price
                # Market value varies ±20% from cost
                market_factor = Decimal(str(round(random.uniform(0.8, 1.2), 4)))
                market_value = cost_basis * market_factor

                position = InvestmentPosition(
                    portfolio_id=portfolio.portfolio_id,
                    investment_id=inv_id,
                    position_date=date.today(),
                    quantity=quantity,
                    cost_basis=cost_basis.quantize(Decimal("0.01")),
                    market_value=market_value.quantize(Decimal("0.01")),
                    currency_code=portfolio.currency_code,
                    last_maint_date=datetime.now(),
                    last_maint_user="TSTGEN",
                )
                positions.append(position)

        logger.info(
            "%s: Generated %d position records", self.PROGRAM_ID, len(positions)
        )
        return positions

    # -----------------------------------------------------------------------
    # 2300-GENERATE-TRANSACTIONS
    # -----------------------------------------------------------------------

    def generate_transactions(
        self,
        portfolios: list[PortfolioMaster],
        txns_per_portfolio: int = 10,
    ) -> list[TransactionHistory]:
        """Generate synthetic transaction history records.

        Replaces TSTGEN00 paragraph 2300-GENERATE-TRANSACTIONS.

        Args:
            portfolios: Portfolio records to create transactions for.
            txns_per_portfolio: Number of transactions per portfolio.

        Returns:
            List of TransactionHistory ORM objects.
        """
        transactions = []
        txn_types = ["BU", "SL", "TR", "FE"]

        for portfolio in portfolios:
            for j in range(txns_per_portfolio):
                txn_date = self._faker.date_between(
                    start_date=portfolio.open_date,
                    end_date=date(2026, 3, 1),
                )
                txn_time = time(
                    random.randint(8, 17),
                    random.randint(0, 59),
                    random.randint(0, 59),
                )
                txn_type = random.choice(txn_types)
                quantity = Decimal(str(random.randint(1, 1000)))
                price = Decimal(str(round(random.uniform(10.0, 500.0), 4)))
                amount = (quantity * price).quantize(Decimal("0.01"))

                inv_id = f"{random.choice(self.INVESTMENT_PREFIXES)}{random.randint(100, 999):03d}"

                txn = TransactionHistory(
                    transaction_id=f"TXN{self._faker.unique.random_int(min=100000, max=999999)}",
                    portfolio_id=portfolio.portfolio_id,
                    transaction_date=txn_date,
                    transaction_time=txn_time,
                    investment_id=inv_id,
                    transaction_type=txn_type,
                    quantity=quantity,
                    price=price,
                    amount=amount,
                    currency_code=portfolio.currency_code,
                    status="P",
                    process_date=datetime.combine(txn_date, txn_time),
                    process_user="TSTGEN",
                )
                transactions.append(txn)

        logger.info(
            "%s: Generated %d transaction records",
            self.PROGRAM_ID,
            len(transactions),
        )
        return transactions

    # -----------------------------------------------------------------------
    # 2400-GENERATE-HISTORY (position history records for POSHIST)
    # -----------------------------------------------------------------------

    def generate_position_history(
        self,
        transactions: list[TransactionHistory],
    ) -> list[PositionHistory]:
        """Generate position history records from transactions.

        Replaces TSTGEN00 paragraph 2400-GENERATE-HISTORY.

        Args:
            transactions: Transaction records to derive history from.

        Returns:
            List of PositionHistory ORM objects.
        """
        history_records = []

        for txn in transactions:
            history = PositionHistory(
                account_no=txn.portfolio_id[:8],
                portfolio_id=txn.portfolio_id,
                trans_date=txn.transaction_date,
                trans_time=txn.transaction_time,
                trans_type=txn.transaction_type,
                security_id=txn.investment_id,
                quantity=txn.quantity,
                price=txn.price,
                amount=txn.amount,
                fees=Decimal("0"),
                total_amount=txn.amount,
                cost_basis=txn.amount,
                gain_loss=Decimal("0"),
                process_date=date.today(),
                process_time=time(0, 0, 0),
                program_id=self.PROGRAM_ID,
                user_id="TSTGEN",
            )
            history_records.append(history)

        logger.info(
            "%s: Generated %d history records",
            self.PROGRAM_ID,
            len(history_records),
        )
        return history_records

    # -----------------------------------------------------------------------
    # Generate TransactionRecord (Pydantic model for batch pipeline input)
    # -----------------------------------------------------------------------

    def generate_transaction_records(
        self,
        count: int = 100,
    ) -> list[TransactionRecord]:
        """Generate TransactionRecord Pydantic models for batch pipeline testing.

        These are used as input to the batch pipeline (TRNVAL00 -> POSUPD00 -> HISTLD00).

        Args:
            count: Number of records to generate.

        Returns:
            List of TransactionRecord Pydantic models.
        """
        records = []
        for _ in range(count):
            txn_date = self._faker.date_between(
                start_date=date(2024, 1, 1), end_date=date(2026, 3, 1)
            )
            quantity = Decimal(str(random.randint(1, 5000)))
            price = Decimal(str(round(random.uniform(10.0, 500.0), 4)))
            amount = (quantity * price).quantize(Decimal("0.01"))

            record = TransactionRecord(
                trn_date=txn_date.strftime("%Y%m%d"),
                trn_time=(
                    f"{random.randint(8, 17):02d}"
                    f"{random.randint(0, 59):02d}"
                    f"{random.randint(0, 59):02d}"
                ),
                portfolio_id=f"PORT{random.randint(1, 100):04d}",
                sequence_no=f"{random.randint(1, 999999):06d}",
                investment_id=(
                    f"{random.choice(self.INVESTMENT_PREFIXES)}"
                    f"{random.randint(100, 999):03d}"
                ),
                transaction_type=random.choice(list(TransactionType)),
                quantity=quantity,
                price=price,
                amount=amount,
                currency=random.choice(list(CurrencyCode)),
                status=TransactionStatus.PENDING,
            )
            records.append(record)

        logger.info(
            "%s: Generated %d transaction records for batch testing",
            self.PROGRAM_ID,
            count,
        )
        return records

    # -----------------------------------------------------------------------
    # Generate audit log entries
    # -----------------------------------------------------------------------

    def generate_audit_logs(self, count: int = 50) -> list[AuditLog]:
        """Generate synthetic audit log entries.

        Args:
            count: Number of audit entries to generate.

        Returns:
            List of AuditLog ORM objects.
        """
        actions = ["CREATE", "UPDATE", "DELETE", "INQUIRE", "LOGIN", "LOGOUT"]
        types = ["TRAN", "USER", "SYST"]
        statuses = ["SUCC", "SUCC", "SUCC", "SUCC", "FAIL", "WARN"]  # 67% success

        logs = []
        for _ in range(count):
            log = AuditLog(
                user_id=f"USR{random.randint(1, 20):04d}",
                program=random.choice(["INQONLN", "INQPORT", "INQHIST", "POSUPD00", "TRNVAL00"]),
                audit_type=random.choice(types),
                action=random.choice(actions),
                status=random.choice(statuses),
                portfolio_id=f"PORT{random.randint(1, 100):04d}",
                message=self._faker.sentence(nb_words=6),
            )
            logs.append(log)

        logger.info("%s: Generated %d audit log entries", self.PROGRAM_ID, count)
        return logs
