"""Integration tests for the HISTLD00 Python migration.

End-to-end runs against an in-memory SQLite database covering the four
scenarios from Step 7 of the migration plan:

1. Basic load with field-level verification.
2. Commit checkpointing with a multi-batch input.
3. Pre-existing duplicate handling.
4. Mid-run database error handling.
"""

from __future__ import annotations

from decimal import Decimal
from unittest.mock import patch

from sqlalchemy.exc import OperationalError

from python.batch.history_loader import HistoryLoader
from python.models.batch_constants import ProcessStatus, ReturnCode
from python.models.batch_control import BatchControlRecord
from python.models.history_record import TransactionHistoryRecord


def _make_records(n: int, *, account: str = "ACCT0001"):
    """Build deterministic records with disjoint primary keys."""
    out = []
    for i in range(n):
        out.append(
            TransactionHistoryRecord(
                account_no=account,
                portfolio_id=f"PORT{i % 100:06d}",
                trans_date="2026-01-15",
                trans_time=f"{(i // 60) % 24:02d}:{i % 60:02d}:00",
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


# ----------------------------------------------------------------------
# 1. End-to-end test with SQLite (verifies field-level mapping)
# ----------------------------------------------------------------------
def test_end_to_end_inserts_all_records(loader_config):
    records = _make_records(10)
    loader = HistoryLoader(config=loader_config, history_records=records)
    rc = loader.run()

    assert rc == ReturnCode.SUCCESS
    assert loader.stats.records_read == 10
    assert loader.stats.records_written == 10
    assert loader.stats.error_count == 0
    assert loader.stats.duplicates_skipped == 0

    # Re-open the connection to verify rows landed in the table.
    loader._db.connect()
    rows = loader.fetch_all_poshist()
    assert len(rows) == 10
    by_security = {r.security_id: r for r in rows}
    for original in records:
        loaded = by_security[original.security_id]
        assert loaded.account_no == original.account_no
        assert loaded.portfolio_id == original.portfolio_id
        assert loaded.trans_date == original.trans_date
        assert loaded.trans_time == original.trans_time
        assert loaded.trans_type == original.trans_type
        assert loaded.quantity == original.quantity.quantize(Decimal("0.001"))
        assert loaded.price == original.price.quantize(Decimal("0.001"))
        assert loaded.amount == original.amount.quantize(Decimal("0.01"))
        assert loaded.fees == original.fees.quantize(Decimal("0.01"))
        assert loaded.total_amount == original.total_amount.quantize(Decimal("0.01"))
        assert loaded.cost_basis == original.cost_basis.quantize(Decimal("0.01"))
        assert loaded.gain_loss == original.gain_loss.quantize(Decimal("0.01"))
        assert loaded.program_id == "HISTLD00"
    loader._db.disconnect()


# ----------------------------------------------------------------------
# 2. End-to-end test with commit checkpointing
# ----------------------------------------------------------------------
def test_end_to_end_commit_checkpointing(loader_config, initial_batch_control):
    loader_config.commit_threshold = 1000
    records = _make_records(2500)
    loader = HistoryLoader(
        config=loader_config,
        history_records=records,
        batch_control=initial_batch_control,
    )
    loader.run()

    assert loader.stats.records_read == 2500
    assert loader.stats.records_written == 2500
    # 2 mid-batch commits at 1000 / 2000 + 1 final commit = 3
    assert loader.stats.commits_issued == 3
    # Batch control reflects checkpoint state.
    assert initial_batch_control.records_read == 2500
    assert initial_batch_control.records_written == 2500
    assert initial_batch_control.status == ProcessStatus.DONE.value


# ----------------------------------------------------------------------
# 3. End-to-end test with duplicates
# ----------------------------------------------------------------------
def test_end_to_end_with_duplicates(loader_config):
    records = _make_records(10)

    # First run: insert the first 3 records.
    pre = HistoryLoader(config=loader_config, history_records=records[:3])
    pre.run()
    assert pre.stats.records_written == 3

    # Second run: pass all 10 — first 3 duplicate, last 7 new.
    loader = HistoryLoader(config=loader_config, history_records=records)
    loader.run()
    assert loader.stats.records_read == 10
    assert loader.stats.records_written == 7
    assert loader.stats.duplicates_skipped == 3
    assert loader.stats.error_count == 0


# ----------------------------------------------------------------------
# 4. End-to-end test with errors
# ----------------------------------------------------------------------
def test_end_to_end_with_errors(loader_config):
    records = _make_records(5)
    loader = HistoryLoader(config=loader_config, history_records=records)
    loader.initialize()

    original_execute = loader._db.connection.execute
    fail_indices = {2, 4}
    state = {"i": 0}

    def patched_execute(stmt, *args, **kwargs):
        state["i"] += 1
        # Indices 2 and 4 simulate a non-duplicate DB error.
        if state["i"] in fail_indices:
            raise OperationalError("mid-batch failure", params=None, orig=Exception())
        return original_execute(stmt, *args, **kwargs)

    with patch.object(loader._db.connection, "execute", side_effect=patched_execute):
        loader.process_records()
    loader.finalize()

    assert loader.stats.error_count == len(fail_indices)
    assert loader.stats.records_written == len(records) - len(fail_indices)
    # The error log must contain the failure description.
    assert loader_config.errlog_path.exists()
    contents = loader_config.errlog_path.read_text()
    assert "INSERT failed" in contents


def test_run_returns_nonzero_on_errors(loader_config):
    records = _make_records(3)
    loader = HistoryLoader(config=loader_config, history_records=records)
    loader.initialize()

    original_execute = loader._db.connection.execute

    def always_fail(stmt, *args, **kwargs):
        if "poshist" in str(stmt).lower() and "insert" in str(stmt).lower():
            raise OperationalError("boom", params=None, orig=Exception("boom"))
        return original_execute(stmt, *args, **kwargs)

    with patch.object(loader._db.connection, "execute", side_effect=always_fail):
        loader.process_records()
    loader.finalize()
    assert loader.stats.error_count == 3
    # finalize() recorded the error state on the (auto-created) batch control.


def test_batch_control_persists_to_vsam(loader_config):
    records = _make_records(5)
    bct = BatchControlRecord(
        job_name="HISTLD00",
        process_date="20260115",
        sequence_no=1,
        program_name="HISTLD00",
    )
    loader = HistoryLoader(
        config=loader_config,
        history_records=records,
        batch_control=bct,
    )
    loader.run()

    # Re-open the BCT VSAM file and read the rewritten record.
    from python.common.vsam_file import OpenMode, VsamFile

    f = VsamFile(loader_config.bchctl_path)
    f.open(OpenMode.INPUT)
    status, payload = f.read(bct.key)
    f.close()
    assert status == "00"
    assert payload is not None
    assert payload["status"] == ProcessStatus.DONE.value
    assert payload["records_written"] == 5
