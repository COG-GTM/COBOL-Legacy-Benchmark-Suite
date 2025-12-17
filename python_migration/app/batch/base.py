"""
Base batch processor class.
Provides common functionality for all batch programs including:
- Checkpoint/restart capability
- Error handling and logging
- Commit frequency control
- Statistics tracking

Replaces common patterns from COBOL batch programs:
- 0000-MAIN-PROCESS
- 9000-ERROR-ROUTINE
- 9100-CHECKPOINT-ROUTINE
"""

from abc import ABC, abstractmethod
from datetime import date, datetime

from sqlalchemy.orm import Session

from app.config import get_settings
from app.models.database import BatchControl
from app.utils.exceptions import BatchProcessingError
from app.utils.logging import ErrorLogger, get_logger


class BatchProcessor(ABC):
    """
    Abstract base class for batch processors.
    Implements checkpoint/restart and error handling patterns from COBOL.

    Replaces CKPRST.cpy checkpoint/restart control structure.
    """

    def __init__(
        self,
        db: Session,
        job_name: str,
        program_name: str,
        process_date: date | None = None,
    ):
        self.db = db
        self.job_name = job_name
        self.program_name = program_name
        self.process_date = process_date or date.today()
        self.settings = get_settings()
        self.logger = get_logger(program_name)
        self.error_logger = ErrorLogger(program_name)

        self.records_read = 0
        self.records_processed = 0
        self.records_written = 0
        self.records_error = 0
        self.return_code = 0

        self.commit_frequency = self.settings.batch_commit_frequency
        self.max_errors = self.settings.batch_max_errors
        self.max_restarts = self.settings.batch_max_restarts

        self.last_key: str | None = None
        self.restart_key: str | None = None
        self.batch_control: BatchControl | None = None

    def run(self) -> int:
        """
        Main entry point for batch processing.
        Implements 0000-MAIN-PROCESS pattern from COBOL.

        Returns:
            Return code (0=success, 4=warning, 8+=error)
        """
        try:
            self._initialize()
            self._process()
            self._terminate()
        except BatchProcessingError as e:
            self.return_code = max(self.return_code, 12)
            self.error_logger.log_error(
                message=str(e),
                error_code="E007",
                severity=12,
                category="PR",
                job_name=self.job_name,
            )
            self._handle_error(e)
        except Exception as e:
            self.return_code = 16
            self.error_logger.log_error(
                message=f"Unexpected error: {e}",
                error_code="E007",
                severity=16,
                category="SY",
                job_name=self.job_name,
            )
            self._handle_error(e)
        finally:
            self._finalize()

        return self.return_code

    def _initialize(self) -> None:
        """
        Initialize batch processing.
        Implements P100-INIT pattern from COBOL batch programs.
        """
        self.error_logger.log_batch_start(
            job_name=self.job_name,
            step_name=self.program_name,
        )

        self.batch_control = self._get_or_create_batch_control()

        if self.batch_control.status == "E":
            if self.batch_control.restart_count >= self.max_restarts:
                raise BatchProcessingError(
                    f"Max restarts ({self.max_restarts}) exceeded",
                    job_name=self.job_name,
                    program=self.program_name,
                )
            self.restart_key = self.batch_control.last_key
            self.batch_control.restart_count += 1
            self.logger.info(
                "Restarting from checkpoint",
                restart_key=self.restart_key,
                restart_count=self.batch_control.restart_count,
            )

        self.batch_control.status = "A"
        self.batch_control.start_time = datetime.utcnow().time()
        self.batch_control.attempt_timestamp = datetime.utcnow()
        self.db.commit()

        self.initialize()

    def _process(self) -> None:
        """
        Main processing loop.
        Implements P200-PROCESS pattern from COBOL batch programs.
        """
        self.process()

    def _terminate(self) -> None:
        """
        Terminate batch processing.
        Implements P900-TERMINATE pattern from COBOL batch programs.
        """
        self.terminate()

        if self.batch_control:
            self.batch_control.status = "D"
            self.batch_control.end_time = datetime.utcnow().time()
            self.batch_control.complete_timestamp = datetime.utcnow()
            self.batch_control.return_code = self.return_code
            self.batch_control.records_read = self.records_read
            self.batch_control.records_written = self.records_written
            self.batch_control.records_error = self.records_error
            self.db.commit()

    def _finalize(self) -> None:
        """Final cleanup and logging."""
        self.error_logger.log_batch_end(
            job_name=self.job_name,
            return_code=self.return_code,
            records_read=self.records_read,
            records_written=self.records_written,
            records_error=self.records_error,
        )

    def _handle_error(self, error: Exception) -> None:
        """
        Handle batch processing error.
        Implements 9000-ERROR-ROUTINE pattern from COBOL.
        """
        if self.batch_control:
            self.batch_control.status = "E"
            self.batch_control.error_desc = str(error)[:80]
            self.batch_control.last_key = self.last_key
            self.db.commit()

        self.db.rollback()

    def checkpoint(self, key: str) -> None:
        """
        Take a checkpoint.
        Implements 9100-CHECKPOINT-ROUTINE pattern from COBOL.

        Args:
            key: Current processing key for restart
        """
        self.last_key = key

        if self.batch_control:
            self.batch_control.last_key = key
            self.batch_control.records_read = self.records_read
            self.batch_control.records_written = self.records_written
            self.batch_control.records_error = self.records_error

        self.db.commit()

        self.error_logger.log_checkpoint(
            checkpoint_key=key,
            records_processed=self.records_processed,
            phase="PROCESS",
        )

    def should_checkpoint(self) -> bool:
        """Check if checkpoint should be taken based on commit frequency."""
        return self.records_processed > 0 and self.records_processed % self.commit_frequency == 0

    def increment_error_count(self) -> bool:
        """
        Increment error count and check if max errors exceeded.

        Returns:
            True if processing should continue, False if max errors exceeded
        """
        self.records_error += 1
        if self.records_error >= self.max_errors:
            self.return_code = max(self.return_code, 8)
            return False
        return True

    def _get_or_create_batch_control(self) -> BatchControl:
        """Get existing batch control record or create new one."""
        sequence_no = self.db.query(BatchControl).filter(
            BatchControl.job_name == self.job_name,
            BatchControl.process_date == self.process_date,
        ).count() + 1

        existing = self.db.query(BatchControl).filter(
            BatchControl.job_name == self.job_name,
            BatchControl.process_date == self.process_date,
            BatchControl.status.in_(["R", "A", "E"]),
        ).first()

        if existing:
            return existing

        batch_control = BatchControl(
            job_name=self.job_name,
            process_date=self.process_date,
            sequence_no=sequence_no,
            status="R",
            program_name=self.program_name,
        )
        self.db.add(batch_control)
        self.db.flush()

        return batch_control

    @abstractmethod
    def initialize(self) -> None:
        """Initialize processing (to be implemented by subclasses)."""
        pass

    @abstractmethod
    def process(self) -> None:
        """Main processing logic (to be implemented by subclasses)."""
        pass

    @abstractmethod
    def terminate(self) -> None:
        """Terminate processing (to be implemented by subclasses)."""
        pass
