"""Unit tests for the HISTLD00 Python migration.

Covers data models, field mapping, commit threshold, duplicate handling,
error handling, and batch-control transitions per Step 6 of the migration
plan.
"""

from __future__ import annotations

from decimal import Decimal
from unittest.mock import patch

import pytest
from sqlalchemy.exc import IntegrityError, OperationalError

from python.batch.history_loader import HistoryLoader, LoaderStats
from python.common.db_connection import DatabaseConnection
from python.config import HistoryLoaderConfig
from python.models.batch_constants import ProcessStatus, ReturnCode
from python.models.batch_control import BatchControlRecord
from python.models.error_message import (
    ErrorCategory,
    ErrorMessage,
    ErrorSeverity,
)
from python.models.history_record import (
    HistoryActionCode,
    HistoryKey,
    HistoryRecord,
    HistoryRecordType,
    TransactionHistoryRecord,
)
from python.models.poshist_record import PosHistRecord


# ----------------------------------------------------------------------
# 1. Data model creation and validation
# ----------------------------------------------------------------------
class TestDataModelCreation:
    def test_history_key_truncates_and_pads_fields(self):
        key = HistoryKey(
            portfolio_id="P1",
            date="20260115",
            time="103045",
            seq_no="0001",
        )
        assert len(key.portfolio_id) == 8
        assert len(key.date) == 8
        assert len(key.time) == 6
        assert len(key.seq_no) == 4
        assert key.as_string() == "P1      " + "20260115" + "103045" + "0001"

    def test_history_record_truncates_oversized_fields(self):
        rec = HistoryRecord(
            key=HistoryKey(),
            record_type="PORTFOLIO",  # too long
            action_code="ADD",  # too long
            before_image="x" * 500,
            after_image="y" * 500,
            reason_code="REASONCODE",
            process_user="LONGUSERNAME",
        )
        assert rec.record_type == "PO"
        assert rec.action_code == "A"
        assert len(rec.before_image) == 400
        assert len(rec.after_image) == 400
        assert rec.reason_code == "REAS"
        assert rec.process_user == "LONGUSER"

    def test_history_record_type_enum_values(self):
        # Mirrors HISTREC.cpy 88-levels: 'PT' / 'PS' / 'TR'.
        assert HistoryRecordType.PORTFOLIO.value == "PT"
        assert HistoryRecordType.POSITION.value == "PS"
        assert HistoryRecordType.TRANSACTION.value == "TR"
        assert HistoryActionCode.ADD.value == "A"
        assert HistoryActionCode.CHANGE.value == "C"
        assert HistoryActionCode.DELETE.value == "D"

    def test_transaction_history_record_decimal_coercion(self):
        rec = TransactionHistoryRecord(
            account_no="ACCT0001",
            portfolio_id="PORT000001",
            trans_date="2026-01-15",
            trans_time="10:30:45",
            trans_type="BU",
            security_id="SEC000001",
            quantity="100.500",
            price="25.250",
            amount="2525.00",
            fees="9.99",
            total_amount="2534.99",
            cost_basis="2400.00",
            gain_loss="125.00",
        )
        assert isinstance(rec.quantity, Decimal)
        assert rec.quantity == Decimal("100.500")
        assert rec.price == Decimal("25.250")
        assert rec.amount == Decimal("2525.00")

    def test_poshist_record_quantizes_decimals(self):
        rec = PosHistRecord(
            quantity=Decimal("1.23456789"),  # quantize to 3 dp
            price=Decimal("50.12345"),
            amount=Decimal("100.987"),  # quantize to 2 dp
        )
        assert rec.quantity == Decimal("1.235")
        assert rec.price == Decimal("50.123")
        assert rec.amount == Decimal("100.99")

    def test_poshist_record_field_length_constraints(self):
        # Truncates per DBTBLS.cpy widths.
        rec = PosHistRecord(
            account_no="ACCOUNT_TOO_LONG",
            portfolio_id="PORTFOLIO_TOO_LONG_VALUE",
            trans_date="2026-01-15-EXTRA",
            trans_type="BUYY",
            security_id="SEC_VERY_LONG_ID_VALUE",
            program_id="PROGRAMID",
        )
        assert rec.account_no == "ACCOUNT_"
        assert rec.portfolio_id == "PORTFOLIO_"
        assert rec.trans_date == "2026-01-15"
        assert rec.trans_type == "BU"
        assert rec.security_id == "SEC_VERY_LON"
        assert rec.program_id == "PROGRAMI"

    def test_batch_control_record_default_status_is_ready(self):
        bct = BatchControlRecord(
            job_name="HISTLD00",
            process_date="20260101",
            sequence_no=1,
        )
        assert bct.status == ProcessStatus.READY.value
        assert bct.return_code == ReturnCode.SUCCESS

    def test_batch_control_status_transitions(self):
        bct = BatchControlRecord(job_name="HISTLD00", process_date="20260101", sequence_no=1)
        bct.mark_active()
        assert bct.status == ProcessStatus.ACTIVE.value
        assert bct.attempt_ts.strip() != ""

        bct.mark_done(return_code=ReturnCode.SUCCESS)
        assert bct.status == ProcessStatus.DONE.value
        assert bct.complete_ts.strip() != ""
        assert bct.return_code == ReturnCode.SUCCESS

    def test_batch_control_mark_error(self):
        bct = BatchControlRecord(job_name="HISTLD00", process_date="20260101", sequence_no=1)
        bct.mark_error("3 errors during load")
        assert bct.status == ProcessStatus.ERROR.value
        assert bct.error_desc == "3 errors during load"
        assert bct.return_code == ReturnCode.ERROR

    def test_error_message_truncation(self):
        msg = ErrorMessage(
            program="HISTLD00_LONG",
            category="VSAM",
            code="ABCDE",
            severity=ErrorSeverity.ERROR,
            text="x" * 200,
            details="y" * 500,
        )
        assert msg.program == "HISTLD00"
        assert msg.category == "VS"
        assert msg.code == "ABCD"
        assert len(msg.text) == 80
        assert len(msg.details) == 256

    def test_error_message_categories(self):
        # Mirrors ERR-CATEGORIES values in ERRHAND.cpy.
        assert ErrorCategory.VSAM.value == "VS"
        assert ErrorCategory.VALIDATION.value == "VL"
        assert ErrorCategory.PROCESSING.value == "PR"
        assert ErrorCategory.SYSTEM.value == "SY"


# ----------------------------------------------------------------------
# 2. Field mapping (2200-LOAD-TO-DB2 equivalent)
# ----------------------------------------------------------------------
class TestFieldMapping:
    def test_th_to_ph_mapping_preserves_all_fields(self):
        history = TransactionHistoryRecord(
            account_no="ACCT0001",
            portfolio_id="PORT000001",
            trans_date="2026-01-15",
            trans_time="10:30:45",
            trans_type="BU",
            security_id="SEC000001",
            quantity=Decimal("100.500"),
            price=Decimal("50.250"),
            amount=Decimal("5025.00"),
            fees=Decimal("9.99"),
            total_amount=Decimal("5034.99"),
            cost_basis=Decimal("4900.00"),
            gain_loss=Decimal("125.00"),
        )
        rec = PosHistRecord.from_transaction_history(history, program_id="HISTLD00", user_id="BATCH")

        # All 13 mapped fields are exactly preserved.
        assert rec.account_no == "ACCT0001"
        assert rec.portfolio_id == "PORT000001"
        assert rec.trans_date == "2026-01-15"
        assert rec.trans_time == "10:30:45"
        assert rec.trans_type == "BU"
        assert rec.security_id == "SEC000001"
        assert rec.quantity == Decimal("100.500")
        assert rec.price == Decimal("50.250")
        assert rec.amount == Decimal("5025.00")
        assert rec.fees == Decimal("9.99")
        assert rec.total_amount == Decimal("5034.99")
        assert rec.cost_basis == Decimal("4900.00")
        assert rec.gain_loss == Decimal("125.00")
        # Audit columns populated
        assert rec.program_id == "HISTLD00"
        assert rec.user_id == "BATCH"
        assert rec.audit_timestamp.strip() != ""
        assert rec.process_date.strip() != ""

    def test_decimal_precision_quantity_three_decimal_places(self):
        # PIC S9(12)V9(3) COMP-3 — three decimal places preserved
        history = TransactionHistoryRecord(
            quantity=Decimal("1.0"),
            price=Decimal("2.500"),
            amount=Decimal("2.50"),
        )
        rec = PosHistRecord.from_transaction_history(history)
        assert rec.quantity == Decimal("1.000")
        assert str(rec.quantity) == "1.000"
        assert str(rec.price) == "2.500"

    def test_decimal_precision_amount_two_decimal_places(self):
        # PIC S9(13)V9(2) COMP-3 — two decimal places preserved
        history = TransactionHistoryRecord(
            amount=Decimal("100.5"),
            fees=Decimal("0.5"),
        )
        rec = PosHistRecord.from_transaction_history(history)
        assert str(rec.amount) == "100.50"
        assert str(rec.fees) == "0.50"

    def test_load_to_db_inserts_record(self, loader_config, history_records_factory):
        records = history_records_factory(3)
        loader = HistoryLoader(config=loader_config, history_records=records)
        try:
            loader.initialize()
            for r in records:
                loader.load_to_db(r)
        finally:
            loader.finalize()

        assert loader.stats.records_written == 3
        assert loader.stats.error_count == 0


# ----------------------------------------------------------------------
# 3. Commit threshold logic (2300-CHECK-COMMIT)
# ----------------------------------------------------------------------
class TestCommitThreshold:
    def test_no_commit_below_threshold(self, loader_config, history_records_factory):
        loader_config.commit_threshold = 1000
        loader = HistoryLoader(
            config=loader_config,
            history_records=history_records_factory(999),
        )
        loader.run()
        # Only the final commit at 3100-FINAL-COMMIT counts.
        assert loader.stats.commits_issued == 1

    def test_commit_at_threshold(self, loader_config, history_records_factory):
        loader_config.commit_threshold = 100
        loader = HistoryLoader(
            config=loader_config,
            history_records=history_records_factory(100),
        )
        loader.run()
        # 1 mid-batch commit + 1 final commit.
        assert loader.stats.commits_issued == 2

    def test_commits_at_each_threshold_boundary(self, fast_commit_config, history_records_factory):
        fast_commit_config.commit_threshold = 10
        loader = HistoryLoader(
            config=fast_commit_config,
            history_records=history_records_factory(25),
        )
        loader.run()
        # 25 records / 10 threshold = 2 mid-batch + 1 final = 3.
        assert loader.stats.commits_issued == 3
        assert loader.stats.records_written == 25
        # commit_count is reset after each mid-batch commit and ends at 5.
        assert loader.stats.commit_count == 5

    def test_commit_count_resets_after_threshold(
        self, fast_commit_config, history_records_factory
    ):
        fast_commit_config.commit_threshold = 5
        loader = HistoryLoader(
            config=fast_commit_config,
            history_records=history_records_factory(5),
        )
        loader.run()
        # commit_count is reset to 0 after the mid-batch commit; the final
        # commit does not increment it.
        assert loader.stats.commit_count == 0
        assert loader.stats.commits_issued == 2  # 1 mid + 1 final


# ----------------------------------------------------------------------
# 4. Duplicate handling (SQLCODE -803 → IntegrityError)
# ----------------------------------------------------------------------
class TestDuplicateHandling:
    def test_duplicate_record_is_skipped(self, loader_config, history_records_factory):
        records = history_records_factory(5)
        # Force two records to share the same primary key tuple.
        records[2] = records[1]
        loader = HistoryLoader(config=loader_config, history_records=records)
        loader.run()
        assert loader.stats.records_written == 4
        assert loader.stats.duplicates_skipped == 1
        assert loader.stats.error_count == 0

    def test_duplicate_does_not_increment_error_count(
        self, loader_config, history_records_factory
    ):
        records = history_records_factory(3)
        # Pre-insert one record so the loader hits a duplicate on the first row.
        loader = HistoryLoader(config=loader_config, history_records=records[:1])
        loader.run()

        # Re-run with the same records: every insert is now a duplicate.
        loader2 = HistoryLoader(config=loader_config, history_records=records)
        loader2.run()
        assert loader2.stats.duplicates_skipped == 1
        assert loader2.stats.records_written == 2
        assert loader2.stats.error_count == 0

    def test_db_connection_helper_recognises_integrity_error(self):
        from sqlalchemy.exc import IntegrityError as IE

        err = IE("dup", params=None, orig=Exception("dup"))
        assert DatabaseConnection.is_duplicate_key_error(err) is True
        assert DatabaseConnection.is_duplicate_key_error(ValueError("nope")) is False


# ----------------------------------------------------------------------
# 5. Error handling (non-duplicate DB errors)
# ----------------------------------------------------------------------
class TestErrorHandling:
    def test_non_duplicate_error_increments_error_count(
        self, loader_config, history_records_factory
    ):
        records = history_records_factory(3)
        loader = HistoryLoader(config=loader_config, history_records=records)

        # Patch ``execute`` on a real connection to simulate a non-duplicate
        # SQL error on the second record. Note the mock is applied after
        # initialize() runs successfully.
        loader.initialize()
        original_execute = loader._db.connection.execute
        call_state = {"count": 0}

        def fake_execute(stmt, *args, **kwargs):
            call_state["count"] += 1
            if call_state["count"] == 2:
                raise OperationalError("simulated", params=None, orig=Exception("boom"))
            return original_execute(stmt, *args, **kwargs)

        with patch.object(loader._db.connection, "execute", side_effect=fake_execute):
            for r in records:
                loader.load_to_db(r)
                loader.check_commit()
        loader.finalize()

        assert loader.stats.error_count == 1
        assert loader.stats.records_written == 2

    def test_error_routine_logs_and_rolls_back(self, loader_config, history_records_factory):
        loader = HistoryLoader(config=loader_config, history_records=history_records_factory(0))
        loader.initialize()
        try:
            with patch.object(loader._db, "rollback") as mock_rollback:
                loader.error_routine("test failure", code="9001")
            mock_rollback.assert_called()
        finally:
            loader.finalize()

        assert loader_config.errlog_path.exists()
        contents = loader_config.errlog_path.read_text()
        assert "test failure" in contents

    def test_final_commit_failure_does_not_overcount_or_checkpoint(
        self, loader_config, history_records_factory, initial_batch_control
    ):
        """3100-FINAL-COMMIT must not bump commits_issued or rewrite the
        checkpoint when the underlying DB commit raises. The run should be
        recorded as ERROR in the batch-control file."""
        loader_config.commit_threshold = 1000  # avoid mid-batch commits
        records = history_records_factory(3)
        loader = HistoryLoader(
            config=loader_config,
            history_records=records,
            batch_control=initial_batch_control,
        )
        loader.initialize()
        loader.process_records()

        commits_before = loader.stats.commits_issued
        # Snapshot the BCT counts written by the last successful checkpoint
        # (none yet — we haven't crossed the threshold).
        bct_records_written_before = initial_batch_control.records_written

        # The first commit (3100-FINAL-COMMIT) must raise; the second
        # call (from DatabaseConnection.disconnect()) succeeds so the
        # rest of finalize() can run.
        with patch.object(
            loader._db,
            "commit",
            side_effect=[
                OperationalError("boom", params=None, orig=Exception("boom")),
                None,
            ],
        ):
            loader.finalize()

        # commits_issued must NOT have been incremented for the failed commit.
        assert loader.stats.commits_issued == commits_before
        # error_count is bumped so _mark_batch_control_done flags ERROR.
        assert loader.stats.error_count >= 1
        assert initial_batch_control.status == ProcessStatus.ERROR.value
        # Checkpoint must not have been advanced past the last good state.
        assert initial_batch_control.records_written == bct_records_written_before

    def test_loader_stops_after_max_errors(self, loader_config, history_records_factory):
        loader_config.max_errors = 3
        records = history_records_factory(20)

        loader = HistoryLoader(config=loader_config, history_records=records)
        loader.initialize()
        original_execute = loader._db.connection.execute

        def always_fail(stmt, *args, **kwargs):
            raise OperationalError("boom", params=None, orig=Exception("boom"))

        with patch.object(loader._db.connection, "execute", side_effect=always_fail):
            loader.process_records()
        loader.finalize()
        # Loop exits when error_count > max_errors.
        assert loader.stats.error_count == loader_config.max_errors + 1
        # We read all errored records up to the threshold but wrote none.
        assert loader.stats.records_written == 0


# ----------------------------------------------------------------------
# 6. Batch control updates
# ----------------------------------------------------------------------
class TestBatchControlUpdates:
    def test_status_transitions_ready_active_done(
        self, loader_config, history_records_factory, initial_batch_control
    ):
        loader = HistoryLoader(
            config=loader_config,
            history_records=history_records_factory(3),
            batch_control=initial_batch_control,
        )
        assert initial_batch_control.status == ProcessStatus.READY.value

        loader.initialize()
        assert initial_batch_control.status == ProcessStatus.ACTIVE.value

        loader.process_records()
        loader.finalize()
        assert initial_batch_control.status == ProcessStatus.DONE.value
        assert initial_batch_control.records_read == 3
        assert initial_batch_control.records_written == 3

    def test_status_transitions_to_error_on_failure(
        self, loader_config, history_records_factory, initial_batch_control
    ):
        loader = HistoryLoader(
            config=loader_config,
            history_records=history_records_factory(3),
            batch_control=initial_batch_control,
        )
        loader.initialize()
        # Inject errors deterministically.
        loader.stats.error_count = 5
        loader.finalize()
        assert initial_batch_control.status == ProcessStatus.ERROR.value
        assert "5 record errors" in initial_batch_control.error_desc

    def test_attempt_and_complete_timestamps(
        self, loader_config, history_records_factory, initial_batch_control
    ):
        loader = HistoryLoader(
            config=loader_config,
            history_records=history_records_factory(2),
            batch_control=initial_batch_control,
        )
        loader.run()
        assert initial_batch_control.attempt_ts.strip() != ""
        assert initial_batch_control.complete_ts.strip() != ""

    def test_restart_count_starts_at_zero(self, initial_batch_control):
        assert initial_batch_control.restart_count == 0

    def test_loader_stats_initial_values(self):
        stats = LoaderStats()
        assert stats.records_read == 0
        assert stats.records_written == 0
        assert stats.error_count == 0
        assert stats.commit_count == 0
        assert stats.duplicates_skipped == 0
        assert stats.commits_issued == 0


# ----------------------------------------------------------------------
# Bonus: small smoke test for the CLI entry point.
# ----------------------------------------------------------------------
def test_cli_entry_point_help(capsys):
    from python.batch.history_loader import _build_argparser

    parser = _build_argparser()
    with pytest.raises(SystemExit):
        parser.parse_args(["--help"])
    captured = capsys.readouterr()
    assert "Position History" in captured.out
