"""Batch Controller - converted from BCHCTL00.cbl.

This module provides batch job control functionality similar to
the COBOL BCHCTL00 batch program.

COBOL Program Reference (BCHCTL00.cbl):
- Manages batch job execution and dependencies
- Implements checkpoint/restart functionality
- Tracks job status and statistics
"""

from datetime import datetime
from typing import Optional

from sqlalchemy.orm import Session

from app.database.models import BatchControl, ProcessDependency
from app.models.error import ErrorCategory, ReturnCode
from app.utils.error_handler import ErrorHandler
from app.utils.logging import get_logger

logger = get_logger(__name__)


class BatchController:
    """Batch Controller - replaces BCHCTL00 batch program.

    This class manages batch job execution, dependencies, and
    checkpoint/restart functionality.
    """

    PROGRAM_NAME = "BCHCTL00"

    def __init__(self, db: Session):
        self.db = db
        self.error_handler = ErrorHandler(db, self.PROGRAM_NAME)
        self.return_code = ReturnCode.SUCCESS

    def initialize_job(
        self,
        job_name: str,
        process_date: str,
        program_name: str,
        step_name: str = "",
    ) -> Optional[BatchControl]:
        """Initialize a batch job - similar to 1000-INITIALIZE.

        Args:
            job_name: Job name (8 chars max)
            process_date: Process date (YYYYMMDD)
            program_name: Program to execute
            step_name: Step name within job

        Returns:
            BatchControl record if successful
        """
        try:
            existing = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == job_name,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if existing:
                if existing.status in ("D", "E"):
                    existing.status = "R"
                    existing.restart_count += 1
                    existing.return_code = 0
                    existing.error_desc = None
                    self.db.commit()
                    logger.info(f"Job {job_name} reset for restart (attempt {existing.restart_count})")
                    return existing
                elif existing.status == "A":
                    logger.warning(f"Job {job_name} is already active")
                    return None
                else:
                    return existing

            batch_control = BatchControl(
                job_name=job_name,
                process_date=process_date,
                sequence_no=self._get_next_sequence(job_name, process_date),
                status="R",
                step_name=step_name,
                program_name=program_name,
                prereq_count=0,
                return_code=0,
                restart_count=0,
            )

            self.db.add(batch_control)
            self.db.commit()

            logger.info(f"Job {job_name} initialized for {process_date}")
            return batch_control

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="BC01", category=ErrorCategory.SYSTEM
            )
            return None

    def start_job(self, job_name: str, process_date: str) -> bool:
        """Start a batch job - similar to 2000-START-JOB.

        Args:
            job_name: Job name
            process_date: Process date

        Returns:
            True if job started successfully
        """
        try:
            batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == job_name,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if not batch_control:
                logger.error(f"Job {job_name} not found")
                return False

            if batch_control.status != "R":
                logger.error(f"Job {job_name} is not ready (status: {batch_control.status})")
                return False

            if not self._check_prerequisites(job_name, process_date):
                batch_control.status = "W"
                self.db.commit()
                logger.info(f"Job {job_name} waiting for prerequisites")
                return False

            batch_control.status = "A"
            batch_control.start_time = datetime.now().strftime("%H:%M:%S")
            batch_control.attempt_ts = datetime.now()
            self.db.commit()

            logger.info(f"Job {job_name} started")
            return True

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="BC02", category=ErrorCategory.SYSTEM
            )
            return False

    def complete_job(
        self,
        job_name: str,
        process_date: str,
        return_code: int,
        records_read: int = 0,
        records_written: int = 0,
        error_desc: str = "",
    ) -> bool:
        """Complete a batch job - similar to 3000-COMPLETE-JOB.

        Args:
            job_name: Job name
            process_date: Process date
            return_code: Job return code
            records_read: Records read count
            records_written: Records written count
            error_desc: Error description if failed

        Returns:
            True if job completed successfully
        """
        try:
            batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == job_name,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if not batch_control:
                logger.error(f"Job {job_name} not found")
                return False

            batch_control.status = "D" if return_code <= 4 else "E"
            batch_control.end_time = datetime.now().strftime("%H:%M:%S")
            batch_control.complete_ts = datetime.now()
            batch_control.return_code = return_code
            batch_control.records_read = records_read
            batch_control.records_written = records_written
            batch_control.error_desc = error_desc[:80] if error_desc else None

            self.db.commit()

            logger.info(
                f"Job {job_name} completed with RC={return_code}",
                extra={
                    "records_read": records_read,
                    "records_written": records_written,
                },
            )
            return True

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="BC03", category=ErrorCategory.SYSTEM
            )
            return False

    def abort_job(
        self,
        job_name: str,
        process_date: str,
        error_desc: str,
    ) -> bool:
        """Abort a batch job - similar to 4000-ABORT-JOB.

        Args:
            job_name: Job name
            process_date: Process date
            error_desc: Error description

        Returns:
            True if job aborted successfully
        """
        return self.complete_job(
            job_name,
            process_date,
            return_code=16,
            error_desc=error_desc,
        )

    def _check_prerequisites(self, job_name: str, process_date: str) -> bool:
        """Check if all prerequisites are met.

        Args:
            job_name: Job name
            process_date: Process date

        Returns:
            True if all prerequisites are met
        """
        dependencies = (
            self.db.query(ProcessDependency)
            .filter(
                ProcessDependency.process_id == job_name,
                ProcessDependency.process_date == process_date,
            )
            .all()
        )

        for dep in dependencies:
            prereq = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == dep.dep_process_id,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if not prereq:
                logger.debug(f"Prerequisite {dep.dep_process_id} not found")
                return False

            if prereq.status != "D":
                logger.debug(f"Prerequisite {dep.dep_process_id} not complete")
                return False

            if prereq.return_code > dep.dep_max_rc:
                logger.debug(
                    f"Prerequisite {dep.dep_process_id} RC={prereq.return_code} > max {dep.dep_max_rc}"
                )
                return False

        return True

    def _get_next_sequence(self, job_name: str, process_date: str) -> int:
        """Get next sequence number for job."""
        max_seq = (
            self.db.query(BatchControl.sequence_no)
            .filter(
                BatchControl.job_name == job_name,
                BatchControl.process_date == process_date,
            )
            .order_by(BatchControl.sequence_no.desc())
            .first()
        )
        return (max_seq[0] + 1) if max_seq else 1

    def get_job_status(self, job_name: str, process_date: str) -> Optional[dict]:
        """Get job status.

        Args:
            job_name: Job name
            process_date: Process date

        Returns:
            Job status dictionary
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
            return None

        return {
            "job_name": batch_control.job_name,
            "process_date": batch_control.process_date,
            "status": batch_control.status,
            "program_name": batch_control.program_name,
            "start_time": batch_control.start_time,
            "end_time": batch_control.end_time,
            "return_code": batch_control.return_code,
            "records_read": batch_control.records_read,
            "records_written": batch_control.records_written,
            "restart_count": batch_control.restart_count,
            "error_desc": batch_control.error_desc,
        }

    def list_jobs(self, process_date: str, status: str = None) -> list[dict]:
        """List jobs for a process date.

        Args:
            process_date: Process date
            status: Optional status filter

        Returns:
            List of job status dictionaries
        """
        query = self.db.query(BatchControl).filter(
            BatchControl.process_date == process_date
        )

        if status:
            query = query.filter(BatchControl.status == status)

        jobs = query.order_by(BatchControl.sequence_no).all()

        return [
            {
                "job_name": job.job_name,
                "status": job.status,
                "program_name": job.program_name,
                "return_code": job.return_code,
            }
            for job in jobs
        ]
