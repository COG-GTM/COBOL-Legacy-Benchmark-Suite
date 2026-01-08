"""Recovery Handler - converted from RCVPRC00.cbl.

This module provides checkpoint/restart and recovery functionality
similar to the COBOL RCVPRC00 batch program.

COBOL Program Reference (RCVPRC00.cbl):
- Manages checkpoint/restart for batch jobs
- Handles recovery from failures
- Maintains recovery state in database
"""

from datetime import datetime
from typing import Optional

from sqlalchemy.orm import Session

from app.database.models import BatchControl
from app.models.error import ErrorCategory, ReturnCode
from app.utils.error_handler import ErrorHandler
from app.utils.logging import get_logger, log_checkpoint

logger = get_logger(__name__)


class CheckpointData:
    """Checkpoint data for recovery."""

    def __init__(
        self,
        job_name: str,
        process_date: str,
        checkpoint_id: str,
        records_processed: int = 0,
        last_key: str = "",
        custom_data: dict = None,
    ):
        self.job_name = job_name
        self.process_date = process_date
        self.checkpoint_id = checkpoint_id
        self.records_processed = records_processed
        self.last_key = last_key
        self.custom_data = custom_data or {}
        self.timestamp = datetime.now()

    def to_dict(self) -> dict:
        """Convert to dictionary."""
        return {
            "job_name": self.job_name,
            "process_date": self.process_date,
            "checkpoint_id": self.checkpoint_id,
            "records_processed": self.records_processed,
            "last_key": self.last_key,
            "custom_data": self.custom_data,
            "timestamp": self.timestamp.isoformat(),
        }


class RecoveryHandler:
    """Recovery Handler - replaces RCVPRC00 batch program.

    This class manages checkpoint/restart functionality for batch jobs,
    allowing recovery from failures without reprocessing all data.
    """

    PROGRAM_NAME = "RCVPRC00"

    def __init__(self, db: Session, max_restarts: int = 3):
        self.db = db
        self.max_restarts = max_restarts
        self.error_handler = ErrorHandler(db, self.PROGRAM_NAME)
        self.return_code = ReturnCode.SUCCESS
        self._checkpoints: dict[str, CheckpointData] = {}

    def save_checkpoint(
        self,
        job_name: str,
        process_date: str,
        checkpoint_id: str,
        records_processed: int,
        last_key: str = "",
        custom_data: dict = None,
    ) -> bool:
        """Save a checkpoint - similar to 2000-SAVE-CHECKPOINT.

        Args:
            job_name: Job name
            process_date: Process date
            checkpoint_id: Checkpoint identifier
            records_processed: Number of records processed
            last_key: Last processed key for restart
            custom_data: Additional checkpoint data

        Returns:
            True if checkpoint saved successfully
        """
        try:
            checkpoint = CheckpointData(
                job_name=job_name,
                process_date=process_date,
                checkpoint_id=checkpoint_id,
                records_processed=records_processed,
                last_key=last_key,
                custom_data=custom_data,
            )

            key = f"{job_name}:{process_date}"
            self._checkpoints[key] = checkpoint

            batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == job_name,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if batch_control:
                batch_control.records_read = records_processed
                batch_control.error_desc = f"CP:{checkpoint_id}:{last_key}"
                self.db.commit()

            log_checkpoint(logger, self.PROGRAM_NAME, checkpoint_id, records_processed)
            return True

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="RH01", category=ErrorCategory.SYSTEM
            )
            return False

    def get_checkpoint(
        self, job_name: str, process_date: str
    ) -> Optional[CheckpointData]:
        """Get last checkpoint - similar to 3000-GET-CHECKPOINT.

        Args:
            job_name: Job name
            process_date: Process date

        Returns:
            CheckpointData if found, None otherwise
        """
        key = f"{job_name}:{process_date}"
        if key in self._checkpoints:
            return self._checkpoints[key]

        batch_control = (
            self.db.query(BatchControl)
            .filter(
                BatchControl.job_name == job_name,
                BatchControl.process_date == process_date,
            )
            .first()
        )

        if batch_control and batch_control.error_desc:
            if batch_control.error_desc.startswith("CP:"):
                parts = batch_control.error_desc.split(":")
                if len(parts) >= 3:
                    return CheckpointData(
                        job_name=job_name,
                        process_date=process_date,
                        checkpoint_id=parts[1],
                        records_processed=batch_control.records_read,
                        last_key=parts[2] if len(parts) > 2 else "",
                    )

        return None

    def can_restart(self, job_name: str, process_date: str) -> tuple[bool, str]:
        """Check if job can be restarted.

        Args:
            job_name: Job name
            process_date: Process date

        Returns:
            Tuple of (can_restart, reason)
        """
        batch_control = (
            self.db.query(BatchControl)
            .filter(
                BatchControl.job_name == job_name,
                BatchControl.process_date == process_date,
            )
            .first()
        )

        if not batch_control:
            return False, "Job not found"

        if batch_control.status == "D":
            return False, "Job already completed successfully"

        if batch_control.status == "A":
            return False, "Job is currently active"

        if batch_control.restart_count >= self.max_restarts:
            return False, f"Maximum restarts ({self.max_restarts}) exceeded"

        return True, "Job can be restarted"

    def prepare_restart(
        self, job_name: str, process_date: str
    ) -> Optional[CheckpointData]:
        """Prepare job for restart - similar to 4000-PREPARE-RESTART.

        Args:
            job_name: Job name
            process_date: Process date

        Returns:
            CheckpointData for restart position, None if cannot restart
        """
        can_restart, reason = self.can_restart(job_name, process_date)
        if not can_restart:
            logger.warning(f"Cannot restart {job_name}: {reason}")
            return None

        try:
            batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == job_name,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            batch_control.status = "R"
            batch_control.restart_count += 1
            batch_control.attempt_ts = datetime.now()
            self.db.commit()

            checkpoint = self.get_checkpoint(job_name, process_date)

            logger.info(
                f"Job {job_name} prepared for restart (attempt {batch_control.restart_count})",
                extra={
                    "checkpoint": checkpoint.checkpoint_id if checkpoint else "NONE",
                    "records_processed": checkpoint.records_processed if checkpoint else 0,
                },
            )

            return checkpoint

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="RH02", category=ErrorCategory.SYSTEM
            )
            return None

    def clear_checkpoint(self, job_name: str, process_date: str) -> bool:
        """Clear checkpoint data after successful completion.

        Args:
            job_name: Job name
            process_date: Process date

        Returns:
            True if cleared successfully
        """
        key = f"{job_name}:{process_date}"
        if key in self._checkpoints:
            del self._checkpoints[key]

        try:
            batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == job_name,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if batch_control:
                batch_control.error_desc = None
                self.db.commit()

            return True

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="RH03", category=ErrorCategory.SYSTEM
            )
            return False

    def get_failed_jobs(self, process_date: str) -> list[dict]:
        """Get list of failed jobs that can be restarted.

        Args:
            process_date: Process date

        Returns:
            List of failed job information
        """
        failed_jobs = (
            self.db.query(BatchControl)
            .filter(
                BatchControl.process_date == process_date,
                BatchControl.status == "E",
            )
            .all()
        )

        result = []
        for job in failed_jobs:
            can_restart, reason = self.can_restart(job.job_name, process_date)
            checkpoint = self.get_checkpoint(job.job_name, process_date)

            result.append({
                "job_name": job.job_name,
                "return_code": job.return_code,
                "restart_count": job.restart_count,
                "can_restart": can_restart,
                "restart_reason": reason,
                "checkpoint_id": checkpoint.checkpoint_id if checkpoint else None,
                "records_processed": checkpoint.records_processed if checkpoint else 0,
            })

        return result
