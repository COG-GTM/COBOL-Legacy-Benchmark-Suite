"""HISTLD00 — Position History DB2 Loader (Python migration).

This module is the Python equivalent of
``src/programs/batch/HISTLD00.cbl``. It reads transaction history records
from a sequential / VSAM-like input, transforms each record into a
``POSHIST`` row using the field mapping from the COBOL ``2200-LOAD-TO-DB2``
paragraph, and bulk-inserts them with commit checkpointing.

The high-level structure mirrors the COBOL program:

    0000-MAIN          -> :py:meth:`HistoryLoader.run`
    1000-INITIALIZE    -> :py:meth:`HistoryLoader.initialize`
        1100-OPEN-FILES         -> :py:meth:`_open_files`
        1200-CONNECT-DB2        -> :py:meth:`_connect_db`
        1300-INIT-CHECKPOINTS   -> :py:meth:`_init_batch_control`
    2000-PROCESS       -> :py:meth:`HistoryLoader.process_records`
        2100-READ-HISTORY       -> :py:meth:`_read_history`
        2200-LOAD-TO-DB2        -> :py:meth:`load_to_db`
        2300-CHECK-COMMIT       -> :py:meth:`check_commit`
        2310-UPDATE-CHECKPOINT  -> :py:meth:`_update_checkpoint`
    3000-TERMINATE     -> :py:meth:`HistoryLoader.finalize`
        3100-FINAL-COMMIT       -> :py:meth:`_final_commit`
        3200-CLOSE-FILES        -> :py:meth:`_close_files`
        3300-DISCONNECT-DB2     -> :py:meth:`_disconnect_db`
        3400-DISPLAY-STATS      -> :py:meth:`_display_stats`
    9000-ERROR-ROUTINE -> :py:meth:`HistoryLoader.error_routine`
"""

from __future__ import annotations

import argparse
import json
import logging
import sys
from dataclasses import asdict, dataclass, field, is_dataclass
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from typing import Any, Iterable, Iterator, List, Optional

from sqlalchemy import insert, select
from sqlalchemy.exc import IntegrityError, SQLAlchemyError

from python.common.db_connection import DatabaseConnection
from python.common.error_handler import ErrorHandler
from python.common.vsam_file import OpenMode, VsamFile, VsamStatus
from python.config import HistoryLoaderConfig, configure_logging
from python.models.batch_constants import ProcessStatus, ReturnCode
from python.models.batch_control import BatchControlRecord
from python.models.error_message import ErrorCategory, ErrorSeverity
from python.models.history_record import TransactionHistoryRecord
from python.models.poshist_record import PosHistRecord
from python.models.poshist_table import PosHist


LOGGER = logging.getLogger("HISTLD00")


@dataclass
class LoaderStats:
    """Mirror of the WS-COUNTERS group in HISTLD00.cbl."""

    records_read: int = 0
    records_written: int = 0
    error_count: int = 0
    commit_count: int = 0
    duplicates_skipped: int = 0
    commits_issued: int = 0


class HistoryLoader:
    """Python port of HISTLD00.cbl.

    Args:
        config: :class:`HistoryLoaderConfig` controlling DB URL, paths, and
            commit threshold.
        history_records: Optional iterable of pre-loaded
            :class:`TransactionHistoryRecord` instances. When provided, the
            loader skips opening the VSAM-like ``TRANSACTION-HISTORY`` file
            and reads from this iterable instead. This is the recommended
            entry point for tests and library usage.
        db: Optional pre-built :class:`DatabaseConnection`. Useful for tests
            that want to share an in-memory SQLite engine.
        error_handler: Optional pre-built :class:`ErrorHandler`.
        batch_control: Optional initial :class:`BatchControlRecord`. When
            ``None`` the loader will read it from the BCT VSAM file (or
            create a fresh one with ``status=READY``).
    """

    def __init__(
        self,
        config: Optional[HistoryLoaderConfig] = None,
        *,
        history_records: Optional[Iterable[TransactionHistoryRecord]] = None,
        db: Optional[DatabaseConnection] = None,
        error_handler: Optional[ErrorHandler] = None,
        batch_control: Optional[BatchControlRecord] = None,
    ) -> None:
        self.config = config or HistoryLoaderConfig()
        self.stats = LoaderStats()
        self._history_iterable = history_records
        self._db = db or DatabaseConnection(
            self.config.db_url, create_schema=True
        )
        self._owns_db = db is None
        self._error_handler = error_handler or ErrorHandler(
            log_path=self.config.errlog_path,
            program_id=self.config.program_id,
        )
        self._owns_error_handler = error_handler is None
        self._batch_control = batch_control
        self._tranhist_file: Optional[VsamFile] = None
        self._bchctl_file: Optional[VsamFile] = None
        self._return_code: int = ReturnCode.SUCCESS

    # ------------------------------------------------------------------
    # Public lifecycle: 0000-MAIN
    # ------------------------------------------------------------------
    def run(self) -> int:
        """Execute the full ETL pipeline. Returns the COBOL RETURN-CODE."""
        try:
            self.initialize()
            self.process_records()
        except Exception as exc:  # noqa: BLE001 - mirrors COBOL global error
            self.error_routine(f"Unhandled error: {exc}")
            self._return_code = max(self._return_code, ReturnCode.SEVERE)
        finally:
            try:
                self.finalize()
            except Exception:  # noqa: BLE001
                LOGGER.exception("Error during finalize()")
                self._return_code = max(self._return_code, ReturnCode.SEVERE)

        # COBOL: ``MOVE WS-ERROR-COUNT TO RETURN-CODE``. We surface the max of
        # the worst severity hit and the raw error count to preserve fidelity
        # while still allowing harness scripts to detect "errors > 0".
        return max(self._return_code, self.stats.error_count)

    # ------------------------------------------------------------------
    # 1000-INITIALIZE
    # ------------------------------------------------------------------
    def initialize(self) -> None:
        """Open files, connect to the database, and mark BCT ACTIVE."""
        self._error_handler.open()
        self._open_files()
        self._connect_db()
        self._init_batch_control()

    def _open_files(self) -> None:
        """1100-OPEN-FILES."""
        if self._history_iterable is None:
            self._tranhist_file = VsamFile(self.config.tranhist_path)
            status = self._tranhist_file.open(OpenMode.INPUT)
            if status != VsamStatus.SUCCESS.value:
                self._raise_error(
                    "Error opening history file",
                    category=ErrorCategory.VSAM,
                    code="OPN1",
                )

        self._bchctl_file = VsamFile(self.config.bchctl_path)
        status = self._bchctl_file.open(OpenMode.IO)
        if status != VsamStatus.SUCCESS.value:
            self._raise_error(
                "Error opening control file",
                category=ErrorCategory.VSAM,
                code="OPN2",
            )

    def _connect_db(self) -> None:
        """1200-CONNECT-DB2."""
        try:
            self._db.connect()
        except SQLAlchemyError as exc:
            self._raise_error(
                f"Connection failed: {exc}",
                category=ErrorCategory.SYSTEM,
                code="DB01",
                severity=ErrorSeverity.SEVERE,
            )

    def _init_batch_control(self) -> None:
        """1300-INIT-CHECKPOINTS: read BCT, set status to ACTIVE, REWRITE."""
        if self._batch_control is None:
            self._batch_control = self._read_batch_control()
        self._batch_control.mark_active()
        self._rewrite_batch_control()

    def _read_batch_control(self) -> BatchControlRecord:
        """Locate the batch-control record for this job."""
        assert self._bchctl_file is not None
        candidate_key = f"{self.config.job_name:<8}"
        # First try a "starts-with" lookup (job_name only). This mirrors
        # ``MOVE SPACES TO BCT-KEY`` followed by ``READ`` in the COBOL.
        status = self._bchctl_file.start(candidate_key)
        if status != VsamStatus.SUCCESS.value:
            # No record yet — create one with status READY.
            today = datetime.now(timezone.utc).strftime("%Y%m%d")
            return BatchControlRecord(
                job_name=self.config.job_name,
                process_date=today,
                sequence_no=1,
                status=ProcessStatus.READY.value,
                program_name=self.config.program_id,
            )
        read_status, payload = self._bchctl_file.read()
        if read_status != VsamStatus.SUCCESS.value or payload is None:
            self._raise_error(
                "Control record not found",
                category=ErrorCategory.VSAM,
                code="BCT1",
            )
        return _bct_from_dict(payload)  # type: ignore[arg-type]

    def _rewrite_batch_control(self) -> None:
        """REWRITE BATCH-CONTROL-RECORD."""
        if self._bchctl_file is None or self._batch_control is None:
            return
        payload = _bct_to_dict(self._batch_control)
        status = self._bchctl_file.rewrite(self._batch_control.key, payload)
        if status != VsamStatus.SUCCESS.value:
            # If the record didn't exist, write a new one.
            self._bchctl_file.write(self._batch_control.key, payload)

    # ------------------------------------------------------------------
    # 2000-PROCESS
    # ------------------------------------------------------------------
    def process_records(self) -> None:
        """Main read/load/checkpoint loop (PERFORM 2000-PROCESS UNTIL EOF)."""
        for record in self._history_source():
            if self.stats.error_count > self.config.max_errors:
                LOGGER.warning(
                    "Error count %d exceeded threshold %d; stopping",
                    self.stats.error_count,
                    self.config.max_errors,
                )
                break

            self.stats.records_read += 1
            self.load_to_db(record)
            self.check_commit()

    def _history_source(self) -> Iterator[TransactionHistoryRecord]:
        """2100-READ-HISTORY: iterate over the configured input source."""
        if self._history_iterable is not None:
            for item in self._history_iterable:
                yield _coerce_history_record(item)
            return

        assert self._tranhist_file is not None
        for raw in self._tranhist_file:
            yield _coerce_history_record(raw)

    # ------------------------------------------------------------------
    # 2200-LOAD-TO-DB2
    # ------------------------------------------------------------------
    def load_to_db(self, history: TransactionHistoryRecord) -> Optional[PosHistRecord]:
        """Map TH-* fields to PH-* fields and INSERT INTO POSHIST.

        Returns the inserted :class:`PosHistRecord` on success, ``None``
        when the row was skipped (duplicate key) or rejected (DB error).
        """
        record = PosHistRecord.from_transaction_history(
            history,
            program_id=self.config.program_id,
            user_id=self.config.user_id,
        )
        try:
            self._db.connection.execute(
                insert(PosHist), [record.to_dict()]
            )
        except IntegrityError:
            # SQLCODE -803 equivalent: duplicate row, skip silently.
            self.stats.duplicates_skipped += 1
            self._db.rollback()
            return None
        except SQLAlchemyError as exc:
            self.stats.error_count += 1
            self._db.rollback()
            self._error_handler.log_error(
                program=self.config.program_id,
                category=ErrorCategory.SYSTEM.value,
                code="DB02",
                severity=ErrorSeverity.ERROR,
                text=f"INSERT failed: {exc.__class__.__name__}",
                details=str(exc)[:256],
            )
            return None
        self.stats.records_written += 1
        return record

    # ------------------------------------------------------------------
    # 2300-CHECK-COMMIT / 2310-UPDATE-CHECKPOINT
    # ------------------------------------------------------------------
    def check_commit(self) -> bool:
        """Increment commit counter and commit at the threshold.

        Returns ``True`` if a commit was issued.
        """
        self.stats.commit_count += 1
        if self.stats.commit_count >= self.config.commit_threshold:
            self._db.commit()
            self.stats.commit_count = 0
            self.stats.commits_issued += 1
            self._update_checkpoint()
            return True
        return False

    def _update_checkpoint(self) -> None:
        """2310-UPDATE-CHECKPOINT: persist read/written counts to BCT."""
        if self._batch_control is None:
            return
        self._batch_control.records_read = self.stats.records_read
        self._batch_control.records_written = self.stats.records_written
        self._rewrite_batch_control()

    # ------------------------------------------------------------------
    # 3000-TERMINATE
    # ------------------------------------------------------------------
    def finalize(self) -> None:
        """3000-TERMINATE: final commit, status DONE, close files, stats."""
        try:
            self._final_commit()
            self._mark_batch_control_done()
        finally:
            self._close_files()
            self._disconnect_db()
            self._display_stats()
            if self._owns_error_handler:
                self._error_handler.close()

    def _final_commit(self) -> None:
        """3100-FINAL-COMMIT."""
        try:
            self._db.commit()
        except SQLAlchemyError as exc:
            LOGGER.error("Final commit failed: %s", exc)
            self._return_code = max(self._return_code, ReturnCode.ERROR)
        self._update_checkpoint()
        self.stats.commits_issued += 1

    def _mark_batch_control_done(self) -> None:
        if self._batch_control is None:
            return
        if self.stats.error_count > 0:
            self._batch_control.mark_error(
                error_desc=f"{self.stats.error_count} record errors",
                return_code=ReturnCode.ERROR,
            )
            self._return_code = max(self._return_code, ReturnCode.ERROR)
        else:
            self._batch_control.mark_done(return_code=self._return_code)
        self._rewrite_batch_control()

    def _close_files(self) -> None:
        """3200-CLOSE-FILES."""
        if self._tranhist_file is not None:
            self._tranhist_file.close()
        if self._bchctl_file is not None:
            self._bchctl_file.close()

    def _disconnect_db(self) -> None:
        """3300-DISCONNECT-DB2."""
        if self._owns_db:
            self._db.disconnect()

    def _display_stats(self) -> None:
        """3400-DISPLAY-STATS."""
        LOGGER.info("HISTLD00 Processing Statistics:")
        LOGGER.info("  Records Read:    %d", self.stats.records_read)
        LOGGER.info("  Records Written: %d", self.stats.records_written)
        LOGGER.info("  Duplicates:      %d", self.stats.duplicates_skipped)
        LOGGER.info("  Errors:          %d", self.stats.error_count)
        LOGGER.info("  Commits Issued:  %d", self.stats.commits_issued)

    # ------------------------------------------------------------------
    # 9000-ERROR-ROUTINE
    # ------------------------------------------------------------------
    def error_routine(
        self,
        error_text: str,
        *,
        category: ErrorCategory = ErrorCategory.PROCESSING,
        code: str = "9000",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        details: str = "",
    ) -> None:
        """Log the error and roll back the current transaction."""
        self._error_handler.log_error(
            program=self.config.program_id,
            category=category.value,
            code=code,
            severity=int(severity),
            text=error_text,
            details=details,
        )
        try:
            self._db.rollback()
        except SQLAlchemyError:
            LOGGER.exception("Rollback failed in error_routine")

    # ------------------------------------------------------------------
    # Query helpers
    # ------------------------------------------------------------------
    def fetch_all_poshist(self) -> List[PosHistRecord]:
        """Return all POSHIST rows as :class:`PosHistRecord` objects."""
        if self._db.connection.closed:
            self._db.connect()
        rows = self._db.connection.execute(
            select(PosHist).order_by(
                PosHist.account_no,
                PosHist.portfolio_id,
                PosHist.trans_date,
                PosHist.trans_time,
                PosHist.security_id,
            )
        ).all()
        return [
            PosHistRecord(
                account_no=row.account_no,
                portfolio_id=row.portfolio_id,
                trans_date=row.trans_date,
                trans_time=row.trans_time,
                trans_type=row.trans_type,
                security_id=row.security_id,
                quantity=row.quantity,
                price=row.price,
                amount=row.amount,
                fees=row.fees,
                total_amount=row.total_amount,
                cost_basis=row.cost_basis,
                gain_loss=row.gain_loss,
                process_date=row.process_date,
                process_time=row.process_time,
                program_id=row.program_id,
                user_id=row.user_id,
                audit_timestamp=row.audit_timestamp,
            )
            for row in rows
        ]

    # ------------------------------------------------------------------
    # Internal: error helpers
    # ------------------------------------------------------------------
    def _raise_error(
        self,
        text: str,
        *,
        category: ErrorCategory,
        code: str,
        severity: ErrorSeverity = ErrorSeverity.SEVERE,
    ) -> None:
        self.stats.error_count += 1
        self._error_handler.log_error(
            program=self.config.program_id,
            category=category.value,
            code=code,
            severity=int(severity),
            text=text,
        )
        self._return_code = max(self._return_code, int(severity))
        raise RuntimeError(text)


# ----------------------------------------------------------------------
# Helpers for serializing batch-control records to/from VSAM payloads
# ----------------------------------------------------------------------
def _bct_to_dict(record: BatchControlRecord) -> dict:
    payload = asdict(record)
    payload["prereqs"] = [asdict(p) for p in record.prereqs]
    return payload


def _bct_from_dict(payload: dict) -> BatchControlRecord:
    prereqs = payload.pop("prereqs", []) or []
    record = BatchControlRecord(**payload)
    if prereqs:
        from python.models.batch_control import PrerequisiteJob

        record.prereqs = [PrerequisiteJob(**p) for p in prereqs]
    return record


def _coerce_history_record(item: Any) -> TransactionHistoryRecord:
    """Accept dataclass instances or dicts and return a TransactionHistoryRecord."""
    if isinstance(item, TransactionHistoryRecord):
        return item
    if is_dataclass(item):
        return TransactionHistoryRecord(**asdict(item))
    if isinstance(item, dict):
        # Decimals come back as strings when round-tripped through JSON; let
        # the dataclass __post_init__ coerce them back.
        return TransactionHistoryRecord(**item)
    if isinstance(item, str):
        return TransactionHistoryRecord(**json.loads(item))
    raise TypeError(f"Unsupported history record type: {type(item).__name__}")


# ----------------------------------------------------------------------
# CLI entry point
# ----------------------------------------------------------------------
def _build_argparser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="histld00",
        description=(
            "Python migration of HISTLD00 — Position History DB2 Loader. "
            "Reads TRANHIST records and bulk-inserts them into the POSHIST table "
            "with commit checkpointing."
        ),
    )
    p.add_argument("--db-url", default="sqlite:///poshist.db",
                   help="SQLAlchemy URL for the POSHIST database.")
    p.add_argument("--tranhist", default="tranhist.db",
                   help="Path to the transaction-history input file.")
    p.add_argument("--bchctl", default="bchctl.db",
                   help="Path to the batch-control file.")
    p.add_argument("--errlog", default="errlog.txt",
                   help="Path to the sequential error log.")
    p.add_argument("--commit-threshold", type=int, default=1000,
                   help="Commit every N records (default: 1000, mirrors COBOL).")
    p.add_argument("--max-errors", type=int, default=100,
                   help="Stop processing once error count exceeds N.")
    p.add_argument("--program-id", default="HISTLD00",
                   help="Program ID stored in PH-PROGRAM-ID / ERR-PROGRAM.")
    p.add_argument("--user-id", default="BATCH",
                   help="User ID stored in PH-USER-ID.")
    p.add_argument("--job-name", default="HISTLD00",
                   help="BCT-JOB-NAME used to look up the batch-control record.")
    p.add_argument("--log-level", default="INFO",
                   choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"])
    return p


def main(argv: Optional[List[str]] = None) -> int:
    """CLI entry point: returns the COBOL-style RETURN-CODE."""
    args = _build_argparser().parse_args(argv)
    config = HistoryLoaderConfig.from_cli(args)
    configure_logging(config.log_level)
    loader = HistoryLoader(config=config)
    return loader.run()


if __name__ == "__main__":  # pragma: no cover
    sys.exit(main())
