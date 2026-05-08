"""Shared pytest fixtures for the HISTLD00 Python migration."""

from __future__ import annotations

import sys
from decimal import Decimal
from pathlib import Path
from typing import List

import pytest

# Make ``import python.*`` work regardless of where pytest is invoked from.
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from python.common.db_connection import DatabaseConnection  # noqa: E402
from python.config import HistoryLoaderConfig  # noqa: E402
from python.models.batch_control import BatchControlRecord  # noqa: E402
from python.models.history_record import TransactionHistoryRecord  # noqa: E402


@pytest.fixture
def in_memory_db() -> DatabaseConnection:
    """Return an in-memory SQLite SQLAlchemy connection with the schema."""
    db = DatabaseConnection(url="sqlite:///:memory:", create_schema=True)
    db.connect()
    yield db
    db.disconnect()


@pytest.fixture
def loader_config(tmp_path) -> HistoryLoaderConfig:
    """Return a HistoryLoaderConfig pointed at temp paths and SQLite."""
    return HistoryLoaderConfig(
        db_url="sqlite:///" + str(tmp_path / "poshist.sqlite"),
        tranhist_path=tmp_path / "tranhist.sqlite",
        bchctl_path=tmp_path / "bchctl.sqlite",
        errlog_path=tmp_path / "errlog.txt",
        commit_threshold=1000,
        max_errors=100,
    )


@pytest.fixture
def fast_commit_config(tmp_path) -> HistoryLoaderConfig:
    """Same as ``loader_config`` but with a tiny commit threshold."""
    return HistoryLoaderConfig(
        db_url="sqlite:///" + str(tmp_path / "poshist.sqlite"),
        tranhist_path=tmp_path / "tranhist.sqlite",
        bchctl_path=tmp_path / "bchctl.sqlite",
        errlog_path=tmp_path / "errlog.txt",
        commit_threshold=10,
        max_errors=100,
    )


@pytest.fixture
def initial_batch_control() -> BatchControlRecord:
    return BatchControlRecord(
        job_name="HISTLD00",
        process_date="20260101",
        sequence_no=1,
    )


def make_history_records(count: int, *, account: str = "ACCT0001") -> List[TransactionHistoryRecord]:
    """Build ``count`` deterministic transaction-history records."""
    out: List[TransactionHistoryRecord] = []
    for i in range(count):
        out.append(
            TransactionHistoryRecord(
                account_no=account,
                portfolio_id=f"PORT{i % 100:06d}",
                trans_date="2026-01-15",
                trans_time=f"{(i % 24):02d}:00:00",
                trans_type="BU" if i % 2 == 0 else "SL",
                security_id=f"SEC{i:09d}",
                quantity=Decimal("100.000") + Decimal(i),
                price=Decimal("50.250"),
                amount=Decimal("5025.00") + Decimal(i),
                fees=Decimal("9.99"),
                total_amount=Decimal("5034.99"),
                cost_basis=Decimal("4900.00"),
                gain_loss=Decimal("125.00"),
            )
        )
    return out


@pytest.fixture
def history_records_factory():
    return make_history_records
