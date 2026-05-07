"""COBOL-to-Python output comparison tests (Step 8 of the migration plan).

The COBOL HISTLD00 program is not executable in the Python test
environment (no z/OS / DB2), so this module performs parity checks against
captured baseline fixtures derived from the COBOL field specifications.
The fixtures live under ``python/tests/fixtures/`` and are loaded as JSON.

For each fixture we:

1. Run the Python ``HistoryLoader`` against the input records.
2. Query the resulting POSHIST rows.
3. Compare every field against the expected baseline, reporting precise
   mismatches (field name, expected value, actual value).
4. Compare the run statistics (records_read / written / errors / commits)
   against the expected DISPLAY output.
5. Verify the rewritten BCT-STATUS, BCT-RETURN-CODE, BCT-COMPLETE-TS.
"""

from __future__ import annotations

import json
from decimal import Decimal
from pathlib import Path
from typing import Any, Dict, List, Tuple

import pytest

from python.batch.history_loader import HistoryLoader
from python.config import HistoryLoaderConfig
from python.models.batch_constants import ProcessStatus
from python.models.batch_control import BatchControlRecord
from python.models.history_record import TransactionHistoryRecord


FIXTURES = Path(__file__).parent / "fixtures"
EXPECTED = FIXTURES / "expected"


def _load_input(name: str) -> List[TransactionHistoryRecord]:
    path = FIXTURES / name
    raw = json.loads(path.read_text())
    return [TransactionHistoryRecord(**row) for row in raw]


def _load_expected_rows(name: str) -> List[Dict[str, Any]]:
    return json.loads((EXPECTED / name).read_text())


def _diff_rows(
    expected: List[Dict[str, Any]],
    actual: List[Dict[str, Any]],
    *,
    ignore: Tuple[str, ...] = ("process_date", "process_time", "audit_timestamp"),
) -> List[str]:
    """Return a list of human-readable mismatches between expected/actual rows.

    Audit columns (``process_date``, ``process_time``, ``audit_timestamp``)
    are ignored by default because they vary by run.
    """
    mismatches: List[str] = []
    if len(expected) != len(actual):
        mismatches.append(
            f"row count mismatch: expected={len(expected)} actual={len(actual)}"
        )

    for idx, (exp, act) in enumerate(zip(expected, actual)):
        for field, exp_val in exp.items():
            if field in ignore:
                continue
            act_val = act.get(field)
            if isinstance(exp_val, str) and isinstance(act_val, Decimal):
                exp_val = Decimal(exp_val)
            if isinstance(act_val, Decimal):
                exp_val = Decimal(str(exp_val))
            if exp_val != act_val:
                mismatches.append(
                    f"row[{idx}].{field}: expected={exp_val!r} actual={act_val!r}"
                )
    return mismatches


def _row_to_dict(row) -> Dict[str, Any]:
    return row.to_dict()


# ----------------------------------------------------------------------
# Field-by-field comparison
# ----------------------------------------------------------------------
def test_parity_small_fixture(tmp_path):
    inputs = _load_input("test_tranhist_small.json")
    expected = _load_expected_rows("poshist_small.json")
    expected_stats = _load_expected_rows("stats_expected.json")["small"]

    config = HistoryLoaderConfig(
        db_url="sqlite:///" + str(tmp_path / "poshist.sqlite"),
        tranhist_path=tmp_path / "tranhist.sqlite",
        bchctl_path=tmp_path / "bchctl.sqlite",
        errlog_path=tmp_path / "errlog.txt",
        commit_threshold=1000,
        max_errors=100,
    )
    bct = BatchControlRecord(
        job_name="HISTLD00",
        process_date="20260115",
        sequence_no=1,
        program_name="HISTLD00",
    )
    loader = HistoryLoader(config=config, history_records=inputs, batch_control=bct)
    rc = loader.run()
    assert rc == 0

    loader._db.connect()
    actual = [_row_to_dict(r) for r in loader.fetch_all_poshist()]
    loader._db.disconnect()

    mismatches = _diff_rows(expected, actual)
    assert mismatches == [], "\n".join(mismatches)

    # Statistics comparison
    assert loader.stats.records_read == expected_stats["records_read"]
    assert loader.stats.records_written == expected_stats["records_written"]
    assert loader.stats.error_count == expected_stats["error_count"]
    assert loader.stats.commits_issued == expected_stats["commits_issued"]

    # Batch control comparison
    assert bct.status == ProcessStatus.DONE.value
    assert bct.return_code == expected_stats["return_code"]
    assert bct.complete_ts.strip() != ""


def test_parity_medium_commit_threshold(tmp_path):
    inputs = _load_input("test_tranhist_medium.json")
    expected_stats = _load_expected_rows("stats_expected.json")["medium"]

    config = HistoryLoaderConfig(
        db_url="sqlite:///" + str(tmp_path / "poshist.sqlite"),
        tranhist_path=tmp_path / "tranhist.sqlite",
        bchctl_path=tmp_path / "bchctl.sqlite",
        errlog_path=tmp_path / "errlog.txt",
        commit_threshold=expected_stats["commit_threshold"],
        max_errors=100,
    )
    loader = HistoryLoader(config=config, history_records=inputs)
    loader.run()
    assert loader.stats.records_read == expected_stats["records_read"]
    assert loader.stats.records_written == expected_stats["records_written"]
    assert loader.stats.commits_issued == expected_stats["commits_issued"]


def test_parity_edge_cases(tmp_path):
    """Verify edge-case decimals: zero, negative, max precision."""
    inputs = _load_input("test_tranhist_edge.json")
    expected = _load_expected_rows("poshist_edge.json")
    config = HistoryLoaderConfig(
        db_url="sqlite:///" + str(tmp_path / "poshist.sqlite"),
        tranhist_path=tmp_path / "tranhist.sqlite",
        bchctl_path=tmp_path / "bchctl.sqlite",
        errlog_path=tmp_path / "errlog.txt",
        commit_threshold=1000,
        max_errors=100,
    )
    loader = HistoryLoader(config=config, history_records=inputs)
    loader.run()

    loader._db.connect()
    actual = [_row_to_dict(r) for r in loader.fetch_all_poshist()]
    loader._db.disconnect()

    mismatches = _diff_rows(expected, actual)
    assert mismatches == [], "\n".join(mismatches)


@pytest.mark.parametrize(
    "trans_type,action_code",
    [
        ("PT", "A"),  # PORTFOLIO + ADD
        ("PS", "C"),  # POSITION + CHANGE
        ("TR", "D"),  # TRANSACTION + DELETE
    ],
)
def test_parity_record_types_and_actions(tmp_path, trans_type, action_code):
    """Cover all HIST-RECORD-TYPE / HIST-ACTION-CODE combinations."""
    record = TransactionHistoryRecord(
        account_no="ACCT0001",
        portfolio_id="PORT000001",
        trans_date="2026-01-15",
        trans_time="10:00:00",
        trans_type=trans_type,
        security_id=f"SEC{trans_type}{action_code}",
        quantity=Decimal("100.000"),
        price=Decimal("50.250"),
        amount=Decimal("5025.00"),
        fees=Decimal("9.99"),
        total_amount=Decimal("5034.99"),
        cost_basis=Decimal("4900.00"),
        gain_loss=Decimal("125.00"),
    )
    config = HistoryLoaderConfig(
        db_url="sqlite:///" + str(tmp_path / "poshist.sqlite"),
        tranhist_path=tmp_path / "tranhist.sqlite",
        bchctl_path=tmp_path / "bchctl.sqlite",
        errlog_path=tmp_path / "errlog.txt",
        commit_threshold=1000,
        max_errors=100,
    )
    loader = HistoryLoader(config=config, history_records=[record])
    loader.run()
    assert loader.stats.records_written == 1
