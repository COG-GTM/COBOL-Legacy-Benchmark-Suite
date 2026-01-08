"""History Loader - converted from HISTLD00.cbl.

This module provides ETL functionality for loading transaction history
to the database, similar to the COBOL HISTLD00 batch program.

COBOL Program Reference (HISTLD00.cbl):
- Reads from TRANSACTION-HISTORY VSAM file
- Loads data to DB2 POSHIST table
- Implements checkpoint/restart with commit threshold
- Handles duplicate key errors (SQLCODE -803)
"""

from datetime import datetime
from decimal import Decimal
from typing import Generator, Optional

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.database.models import BatchControl, PositionHistory
from app.models.error import ErrorCategory, ReturnCode
from app.utils.error_handler import ErrorHandler
from app.utils.logging import get_logger, log_batch_end, log_batch_start, log_checkpoint

logger = get_logger(__name__)


class HistoryLoader:
    """History Loader - replaces HISTLD00 batch program.

    This class loads transaction history data from source files
    to the POSHIST database table, implementing checkpoint/restart
    functionality similar to the COBOL program.
    """

    PROGRAM_NAME = "HISTLD00"
    DEFAULT_COMMIT_THRESHOLD = 1000

    def __init__(
        self,
        db: Session,
        commit_threshold: int = DEFAULT_COMMIT_THRESHOLD,
        max_errors: int = 100,
    ):
        self.db = db
        self.commit_threshold = commit_threshold
        self.max_errors = max_errors
        self.error_handler = ErrorHandler(db, self.PROGRAM_NAME)

        self.records_read = 0
        self.records_written = 0
        self.error_count = 0
        self.commit_count = 0
        self.return_code = ReturnCode.SUCCESS

        self.batch_control: Optional[BatchControl] = None

    def initialize(self, job_name: str, process_date: str) -> bool:
        """Initialize the history loader - similar to 1000-INITIALIZE.

        Args:
            job_name: Batch job name
            process_date: Process date (YYYYMMDD)

        Returns:
            True if initialization successful
        """
        log_batch_start(logger, self.PROGRAM_NAME, job_name, process_date)

        try:
            self.batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == job_name,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if not self.batch_control:
                self.batch_control = BatchControl(
                    job_name=job_name,
                    process_date=process_date,
                    sequence_no=1,
                    status="A",
                    program_name=self.PROGRAM_NAME,
                    start_time=datetime.now().strftime("%H:%M:%S"),
                    attempt_ts=datetime.now(),
                )
                self.db.add(self.batch_control)
            else:
                self.batch_control.status = "A"
                self.batch_control.attempt_ts = datetime.now()
                self.batch_control.start_time = datetime.now().strftime("%H:%M:%S")

            self.db.commit()
            return True

        except Exception as e:
            self.error_handler.handle_error(
                e, code="HL01", category=ErrorCategory.SYSTEM
            )
            self.return_code = ReturnCode.SEVERE
            return False

    def load_record(self, record: dict) -> bool:
        """Load a single history record - similar to 2200-LOAD-TO-DB2.

        Args:
            record: Dictionary with history record data

        Returns:
            True if record loaded successfully
        """
        self.records_read += 1

        try:
            history_record = PositionHistory(
                account_no=record.get("account_no", "")[:8],
                portfolio_id=record.get("portfolio_id", "")[:10],
                trans_date=self._parse_date(record.get("trans_date")),
                trans_time=self._parse_time(record.get("trans_time")),
                trans_type=record.get("trans_type", "")[:2],
                security_id=record.get("security_id", "")[:12],
                quantity=Decimal(str(record.get("quantity", 0))),
                price=Decimal(str(record.get("price", 0))),
                amount=Decimal(str(record.get("amount", 0))),
                fees=Decimal(str(record.get("fees", 0))),
                total_amount=Decimal(str(record.get("total_amount", 0))),
                cost_basis=Decimal(str(record.get("cost_basis", 0))),
                gain_loss=Decimal(str(record.get("gain_loss", 0))),
                process_date=datetime.now().date(),
                process_time=datetime.now().time(),
                program_id=self.PROGRAM_NAME,
                user_id=record.get("user_id", "BATCH")[:8],
                audit_timestamp=datetime.now(),
            )

            self.db.add(history_record)
            self.records_written += 1
            self.commit_count += 1

            if self.commit_count >= self.commit_threshold:
                self._checkpoint()

            return True

        except IntegrityError:
            self.db.rollback()
            logger.debug(f"Duplicate record skipped: {record.get('account_no')}")
            return True

        except Exception as e:
            self.db.rollback()
            self.error_count += 1
            self.error_handler.handle_error(
                e,
                code="HL02",
                category=ErrorCategory.PROCESSING,
                details=f"Record: {record.get('account_no')}",
            )

            if self.error_count > self.max_errors:
                self.return_code = ReturnCode.SEVERE
                raise

            return False

    def load_batch(self, records: list[dict]) -> dict:
        """Load a batch of history records.

        Args:
            records: List of record dictionaries

        Returns:
            Processing statistics
        """
        for record in records:
            if self.error_count > self.max_errors:
                logger.error(f"Max errors exceeded: {self.error_count}")
                break
            self.load_record(record)

        self._final_commit()
        return self.get_statistics()

    def load_from_generator(self, records: Generator[dict, None, None]) -> dict:
        """Load records from a generator (for large files).

        Args:
            records: Generator yielding record dictionaries

        Returns:
            Processing statistics
        """
        for record in records:
            if self.error_count > self.max_errors:
                logger.error(f"Max errors exceeded: {self.error_count}")
                break
            self.load_record(record)

        self._final_commit()
        return self.get_statistics()

    def _checkpoint(self) -> None:
        """Save checkpoint - similar to 2310-UPDATE-CHECKPOINT."""
        try:
            self.db.commit()
            self.commit_count = 0

            if self.batch_control:
                self.batch_control.records_read = self.records_read
                self.batch_control.records_written = self.records_written
                self.db.commit()

            log_checkpoint(
                logger,
                self.PROGRAM_NAME,
                f"CP-{self.records_read}",
                self.records_read,
            )

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="HL03", category=ErrorCategory.SYSTEM
            )

    def _final_commit(self) -> None:
        """Final commit - similar to 3100-FINAL-COMMIT."""
        try:
            self.db.commit()

            if self.batch_control:
                self.batch_control.status = "D" if self.return_code <= ReturnCode.WARNING else "E"
                self.batch_control.end_time = datetime.now().strftime("%H:%M:%S")
                self.batch_control.complete_ts = datetime.now()
                self.batch_control.records_read = self.records_read
                self.batch_control.records_written = self.records_written
                self.batch_control.return_code = self.return_code
                self.db.commit()

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="HL04", category=ErrorCategory.SYSTEM
            )

    def terminate(self) -> None:
        """Terminate processing - similar to 3000-TERMINATE."""
        self._final_commit()

        log_batch_end(
            logger,
            self.PROGRAM_NAME,
            self.batch_control.job_name if self.batch_control else "UNKNOWN",
            self.return_code,
            self.records_read,
            self.records_written,
            self.error_count,
        )

    def _parse_date(self, date_value) -> datetime.date:
        """Parse date from various formats."""
        if isinstance(date_value, datetime):
            return date_value.date()
        if isinstance(date_value, str):
            if len(date_value) == 8:
                return datetime.strptime(date_value, "%Y%m%d").date()
            elif len(date_value) == 10:
                return datetime.strptime(date_value, "%Y-%m-%d").date()
        return datetime.now().date()

    def _parse_time(self, time_value) -> datetime.time:
        """Parse time from various formats."""
        if isinstance(time_value, datetime):
            return time_value.time()
        if isinstance(time_value, str):
            if len(time_value) == 6:
                return datetime.strptime(time_value, "%H%M%S").time()
            elif len(time_value) == 8:
                return datetime.strptime(time_value, "%H:%M:%S").time()
        return datetime.now().time()

    def get_statistics(self) -> dict:
        """Get processing statistics - similar to 3400-DISPLAY-STATS."""
        return {
            "program": self.PROGRAM_NAME,
            "records_read": self.records_read,
            "records_written": self.records_written,
            "error_count": self.error_count,
            "return_code": self.return_code,
        }
