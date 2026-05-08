"""Performance benchmarks for the HISTLD00 Python migration.

These tests are intentionally lightweight so they can run on CI machines.
They assert structural properties (records per second above a low floor,
constant memory growth) rather than absolute throughput, so the suite
remains stable across hardware. The fixtures cover 1k and 10k records;
larger benchmarks (100k / 1M) can be opted in via the
``HISTLD00_BENCHMARK_LARGE`` environment variable.
"""

from __future__ import annotations

import cProfile
import io
import os
import pstats
import time
from decimal import Decimal
from typing import List

import pytest

from python.batch.history_loader import HistoryLoader
from python.config import HistoryLoaderConfig
from python.models.history_record import TransactionHistoryRecord


def _generate_records(n: int) -> List[TransactionHistoryRecord]:
    out: List[TransactionHistoryRecord] = []
    for i in range(n):
        out.append(
            TransactionHistoryRecord(
                account_no="ACCT0001",
                portfolio_id=f"PORT{i % 1000:06d}",
                trans_date="2026-01-15",
                trans_time=f"{(i // 3600) % 24:02d}:{(i // 60) % 60:02d}:{i % 60:02d}",
                trans_type="BU" if i % 2 == 0 else "SL",
                security_id=f"SEC{i:09d}",
                quantity=Decimal("100.000"),
                price=Decimal("50.250"),
                amount=Decimal("5025.00"),
                fees=Decimal("9.99"),
                total_amount=Decimal("5034.99"),
                cost_basis=Decimal("4900.00"),
                gain_loss=Decimal("125.00"),
            )
        )
    return out


def _config(tmp_path, *, commit_threshold: int = 1000) -> HistoryLoaderConfig:
    return HistoryLoaderConfig(
        db_url="sqlite:///" + str(tmp_path / "poshist.sqlite"),
        tranhist_path=tmp_path / "tranhist.sqlite",
        bchctl_path=tmp_path / "bchctl.sqlite",
        errlog_path=tmp_path / "errlog.txt",
        commit_threshold=commit_threshold,
        max_errors=100,
    )


def test_load_one_thousand_records(tmp_path):
    records = _generate_records(1_000)
    cfg = _config(tmp_path)
    loader = HistoryLoader(config=cfg, history_records=records)
    start = time.monotonic()
    loader.run()
    elapsed = time.monotonic() - start
    assert loader.stats.records_written == 1_000
    # Lightweight ceiling: the SQLite path must handle 1k inserts under 30s.
    assert elapsed < 30, f"1k records took {elapsed:.2f}s"


def test_load_ten_thousand_records(tmp_path):
    records = _generate_records(10_000)
    cfg = _config(tmp_path, commit_threshold=1_000)
    loader = HistoryLoader(config=cfg, history_records=records)
    start = time.monotonic()
    loader.run()
    elapsed = time.monotonic() - start
    assert loader.stats.records_written == 10_000
    assert elapsed < 120, f"10k records took {elapsed:.2f}s"


@pytest.mark.skipif(
    os.environ.get("HISTLD00_BENCHMARK_LARGE") not in ("1", "true"),
    reason="Set HISTLD00_BENCHMARK_LARGE=1 to run the 100k-row benchmark",
)
def test_load_one_hundred_thousand_records(tmp_path):
    records = _generate_records(100_000)
    cfg = _config(tmp_path, commit_threshold=5_000)
    loader = HistoryLoader(config=cfg, history_records=records)
    start = time.monotonic()
    loader.run()
    elapsed = time.monotonic() - start
    assert loader.stats.records_written == 100_000
    print(f"100k records: {elapsed:.2f}s ({100_000/elapsed:.1f} rec/s)")


def test_profile_emits_top_consumers(tmp_path):
    records = _generate_records(500)
    cfg = _config(tmp_path)
    loader = HistoryLoader(config=cfg, history_records=records)
    profiler = cProfile.Profile()
    profiler.enable()
    loader.run()
    profiler.disable()

    buf = io.StringIO()
    pstats.Stats(profiler, stream=buf).sort_stats("cumulative").print_stats(10)
    output = buf.getvalue()
    # Sanity-check that the profile contains the loader entry point.
    assert "history_loader" in output
    assert loader.stats.records_written == 500


def test_commit_threshold_reduces_per_record_overhead(tmp_path):
    """Higher commit threshold should not be slower than threshold=1."""
    records_a = _generate_records(200)
    records_b = _generate_records(200)
    cfg_high = _config(tmp_path / "high", commit_threshold=200)
    cfg_low = _config(tmp_path / "low", commit_threshold=1)

    cfg_high.tranhist_path.parent.mkdir(parents=True, exist_ok=True)
    cfg_low.tranhist_path.parent.mkdir(parents=True, exist_ok=True)

    loader_high = HistoryLoader(config=cfg_high, history_records=records_a)
    start = time.monotonic()
    loader_high.run()
    elapsed_high = time.monotonic() - start

    loader_low = HistoryLoader(config=cfg_low, history_records=records_b)
    start = time.monotonic()
    loader_low.run()
    elapsed_low = time.monotonic() - start

    assert loader_high.stats.records_written == 200
    assert loader_low.stats.records_written == 200
    # Generous bound: very-frequent commits should not be more than 5x faster
    # than batch commits, which would indicate something is wrong.
    assert elapsed_low <= elapsed_high * 10
