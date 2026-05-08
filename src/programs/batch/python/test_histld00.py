"""Smoke tests for histld00.py.

These exercise the main flow end-to-end against an in-memory SQLite
database so the COBOL → Python port can be validated without a real DB2
instance.
"""

from __future__ import annotations

import dataclasses
import sqlite3
import tempfile
import unittest
from decimal import Decimal
from pathlib import Path

from . import histld00


def _sample_record(
    *,
    account_no: str = "A0000001",
    portfolio_id: str = "PF00000001",
    trans_date: str = "2024-01-02",
    trans_time: str = "09:30:00",
    trans_type: str = "BU",
    security_id: str = "SEC000000001",
    quantity: str = "100.000",
    price: str = "12.345",
    amount: str = "1234.50",
    fees: str = "1.00",
    total_amount: str = "1235.50",
    cost_basis: str = "1234.50",
    gain_loss: str = "0.00",
) -> histld00.TransactionHistoryRecord:
    return histld00.TransactionHistoryRecord(
        th_account_no=account_no,
        th_portfolio_id=portfolio_id,
        th_trans_date=trans_date,
        th_trans_time=trans_time,
        th_trans_type=trans_type,
        th_security_id=security_id,
        th_quantity=Decimal(quantity),
        th_price=Decimal(price),
        th_amount=Decimal(amount),
        th_fees=Decimal(fees),
        th_total_amount=Decimal(total_amount),
        th_cost_basis=Decimal(cost_basis),
        th_gain_loss=Decimal(gain_loss),
    )


def _write_records(
    path: Path, records: list[histld00.TransactionHistoryRecord]
) -> None:
    with path.open("wb") as handle:
        for rec in records:
            handle.write(histld00.encode_record(rec))


class RecordCodecTests(unittest.TestCase):
    def test_round_trip(self) -> None:
        rec = _sample_record(
            quantity="-1.500", price="0.001", gain_loss="-9999999999.99"
        )
        encoded = histld00.encode_record(rec)
        self.assertEqual(len(encoded), histld00.INPUT_RECORD_LENGTH)
        decoded = histld00.parse_record(encoded)
        self.assertEqual(decoded.th_quantity, Decimal("-1.500"))
        self.assertEqual(decoded.th_price, Decimal("0.001"))
        self.assertEqual(decoded.th_gain_loss, Decimal("-9999999999.99"))
        self.assertEqual(decoded.th_account_no, rec.th_account_no)


class JobSmokeTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.tmpdir = Path(self.tmp.name)
        self.input_path = self.tmpdir / "tranhist.dat"
        self.checkpoint_path = self.tmpdir / "histld00.ckpt"
        self.db_path = self.tmpdir / "posmvp.sqlite"

    def _make_loader(self) -> tuple[histld00.HistoryLoader, sqlite3.Connection]:
        conn = sqlite3.connect(self.db_path)
        conn.executescript(histld00.SQLITE_POSHIST_DDL)
        conn.commit()
        loader = histld00.HistoryLoader(conn, paramstyle="qmark")
        return loader, conn

    def _make_job(self, *, commit_every: int = 2, max_errors: int = 100) -> tuple[
        histld00.HISTLD00Job, sqlite3.Connection
    ]:
        loader, conn = self._make_loader()
        config = histld00.JobConfig(
            input_file=self.input_path,
            checkpoint_file=self.checkpoint_path,
            db_driver="sqlite",
            db_dsn=str(self.db_path),
            user_id="testuser",
            commit_threshold=commit_every,
            max_error_count=max_errors,
            min_checkpoint_interval_seconds=0,
        )
        return histld00.HISTLD00Job(config, loader=loader), conn

    def test_loads_unique_records(self) -> None:
        records = [
            _sample_record(account_no=f"A{i:07d}", portfolio_id=f"PF{i:08d}")
            for i in range(5)
        ]
        _write_records(self.input_path, records)
        job, conn = self._make_job(commit_every=2)
        rc = job.run()
        self.assertEqual(rc, 0)
        self.assertEqual(job.stats.records_read, 5)
        self.assertEqual(job.stats.records_written, 5)
        self.assertEqual(job.stats.duplicate_count, 0)
        self.assertEqual(job.stats.error_count, 0)
        with sqlite3.connect(self.db_path) as verify:
            (count,) = verify.execute("SELECT COUNT(*) FROM POSHIST").fetchone()
        self.assertEqual(count, 5)

    def test_skips_duplicate_keys(self) -> None:
        # Two records with identical primary key (account_no, portfolio_id,
        # trans_date, trans_time) — the second must be skipped silently like
        # SQLCODE -803 in COBOL, not counted as an error.
        rec = _sample_record()
        records = [rec, dataclasses.replace(rec, th_amount=Decimal("9999.99"))]
        _write_records(self.input_path, records)
        job, _ = self._make_job(commit_every=10)
        rc = job.run()
        self.assertEqual(rc, 0)
        self.assertEqual(job.stats.records_read, 2)
        self.assertEqual(job.stats.records_written, 1)
        self.assertEqual(job.stats.duplicate_count, 1)
        self.assertEqual(job.stats.error_count, 0)

    def test_checkpoint_restart_skips_processed_records(self) -> None:
        records = [
            _sample_record(account_no=f"A{i:07d}", portfolio_id=f"PF{i:08d}")
            for i in range(4)
        ]
        _write_records(self.input_path, records)
        # Pre-seed checkpoint as if 2 records were already processed.
        self.checkpoint_path.write_text(
            '{"job_name": "HISTLD00", "records_read": 2,'
            ' "records_written": 2, "error_count": 0}'
        )
        job, _ = self._make_job(commit_every=10)
        rc = job.run()
        self.assertEqual(rc, 0)
        # records_read counter should be the cumulative total.
        self.assertEqual(job.stats.records_read, 4)
        # Only the last two were actually inserted this run.
        self.assertEqual(job.stats.records_written, 4)  # 2 prior + 2 new

    def test_return_code_is_error_count(self) -> None:
        # Force an error by closing the cursor mid-flight via a bad row:
        # write a record with an unparseable date and verify it lands in
        # error_count rather than aborting the run.
        good = _sample_record()
        bad = dataclasses.replace(good, th_trans_date="not-a-date")
        records = [good, bad, dataclasses.replace(good, th_account_no="A9999999")]
        _write_records(self.input_path, records)
        job, _ = self._make_job(commit_every=10)
        rc = job.run()
        self.assertEqual(rc, job.stats.error_count)
        self.assertEqual(job.stats.records_read, 3)
        self.assertEqual(job.stats.records_written, 2)
        self.assertEqual(job.stats.error_count, 1)


class DuplicateKeyDetectionTests(unittest.TestCase):
    def test_sqlite_unique_constraint_message(self) -> None:
        try:
            raise sqlite3.IntegrityError("UNIQUE constraint failed: POSHIST.X")
        except sqlite3.IntegrityError as exc:
            self.assertTrue(histld00._is_duplicate_key(exc))

    def test_db2_sqlcode_attribute(self) -> None:
        exc = Exception("insert failed")
        exc.sqlcode = -803  # type: ignore[attr-defined]
        self.assertTrue(histld00._is_duplicate_key(exc))

    def test_postgres_sqlstate(self) -> None:
        exc = Exception("duplicate")
        exc.sqlstate = "23505"  # type: ignore[attr-defined]
        self.assertTrue(histld00._is_duplicate_key(exc))

    def test_unrelated_error_not_duplicate(self) -> None:
        exc = Exception("connection timed out")
        self.assertFalse(histld00._is_duplicate_key(exc))


if __name__ == "__main__":  # pragma: no cover
    unittest.main()
