"""
Batch controller translated from COBOL program BCHCTL00.cbl.

EVALUATE TRUE dispatch pattern with 4 functions:
- INIT (1000-PROCESS-INITIALIZE)
- CHEK (2000-CHECK-PREREQUISITES)
- UPDT (3000-UPDATE-STATUS)
- TERM (4000-PROCESS-TERMINATE)
"""

import logging
from datetime import datetime

from sqlalchemy.orm import Session

from src.common.constants import BatchFunction, BatchStatus, ReturnCode
from src.common.error_handler import BatchError
from src.db.repository import BatchControlRepository
from src.db.tables import BatchControl

logger = logging.getLogger(__name__)


class BatchController:
    """
    Batch job lifecycle controller.
    Translates BCHCTL00.cbl EVALUATE TRUE dispatcher.
    """

    def __init__(self, session: Session):
        self.session = session
        self.repo = BatchControlRepository(session)

    def dispatch(self, function: str, job_name: str, process_date: str) -> ReturnCode:
        """
        Main dispatch. Translates BCHCTL00.cbl 0000-MAIN EVALUATE TRUE.
        """
        match function:
            case BatchFunction.INIT:
                return self._initialize(job_name, process_date)
            case BatchFunction.CHECK:
                return self._check_prerequisites(job_name, process_date)
            case BatchFunction.UPDATE:
                return self._update_status(job_name, process_date)
            case BatchFunction.TERMINATE:
                return self._terminate(job_name, process_date)
            case _:
                raise BatchError(
                    f"Invalid batch function: {function}",
                    job_name=job_name,
                    error_code="BC01",
                    program="BCHCTL00",
                )

    def _initialize(self, job_name: str, process_date: str) -> ReturnCode:
        """
        Translates 1000-PROCESS-INITIALIZE.
        Create or reset batch control record.
        """
        logger.info("Initializing batch job: %s for date %s", job_name, process_date)

        record = self.repo.get(job_name, process_date)
        if record is not None:
            # Reset existing record
            record.status = BatchStatus.READY.value
            record.return_code = ReturnCode.SUCCESS.value
            record.start_time = None
            record.end_time = None
            record.records_read = 0
            record.records_written = 0
            record.error_count = 0
            record.error_desc = ""
            self.repo.update(record)
        else:
            record = BatchControl(
                job_name=job_name,
                process_date=process_date,
                sequence_no=0,
                status=BatchStatus.READY.value,
                return_code=ReturnCode.SUCCESS.value,
            )
            self.repo.create(record)

        logger.info("Batch job initialized: %s", job_name)
        return ReturnCode.SUCCESS

    def _check_prerequisites(self, job_name: str, process_date: str) -> ReturnCode:
        """
        Translates 2000-CHECK-PREREQUISITES.
        Verify job is ready to run.
        """
        record = self.repo.get(job_name, process_date)
        if record is None:
            raise BatchError(
                f"Batch control record not found: {job_name}/{process_date}",
                job_name=job_name,
                error_code="BC02",
                program="BCHCTL00",
            )

        if record.status not in (BatchStatus.READY.value, BatchStatus.ERROR.value):
            logger.warning("Job %s not in ready/error state: %s", job_name, record.status)
            return ReturnCode.WARNING

        # Check restart count
        if record.status == BatchStatus.ERROR.value:
            if record.restart_count >= record.max_restarts:
                logger.error("Job %s exceeded max restarts (%d)", job_name, record.max_restarts)
                return ReturnCode.ERROR
            record.restart_count += 1
            self.repo.update(record)

        logger.info("Prerequisites check passed for job: %s", job_name)
        return ReturnCode.SUCCESS

    def _update_status(self, job_name: str, process_date: str) -> ReturnCode:
        """
        Translates 3000-UPDATE-STATUS.
        Mark job as active with start time.
        """
        record = self.repo.get(job_name, process_date)
        if record is None:
            raise BatchError(
                f"Batch control record not found: {job_name}/{process_date}",
                job_name=job_name,
                error_code="BC02",
                program="BCHCTL00",
            )

        record.status = BatchStatus.ACTIVE.value
        record.start_time = datetime.now()
        record.attempt_ts = datetime.now()
        self.repo.update(record)

        logger.info("Batch job started: %s", job_name)
        return ReturnCode.SUCCESS

    def _terminate(self, job_name: str, process_date: str) -> ReturnCode:
        """
        Translates 4000-PROCESS-TERMINATE.
        Mark job as done or error with end time.
        """
        record = self.repo.get(job_name, process_date)
        if record is None:
            raise BatchError(
                f"Batch control record not found: {job_name}/{process_date}",
                job_name=job_name,
                error_code="BC02",
                program="BCHCTL00",
            )

        record.end_time = datetime.now()
        if record.error_count > 0:
            record.status = BatchStatus.ERROR.value
            record.return_code = ReturnCode.ERROR.value
            self.repo.update(record)
            logger.warning("Batch job completed with errors: %s (%d errors)", job_name, record.error_count)
            return ReturnCode.ERROR
        else:
            record.status = BatchStatus.DONE.value
            record.return_code = ReturnCode.SUCCESS.value
            self.repo.update(record)
            logger.info("Batch job completed successfully: %s", job_name)
            return ReturnCode.SUCCESS

    def update_counts(
        self,
        job_name: str,
        process_date: str,
        records_read: int = 0,
        records_written: int = 0,
        error_count: int = 0,
        error_desc: str = "",
    ) -> None:
        """Update processing counters."""
        record = self.repo.get(job_name, process_date)
        if record is not None:
            record.records_read += records_read
            record.records_written += records_written
            record.error_count += error_count
            if error_desc:
                record.error_desc = error_desc[:80]
            self.repo.update(record)
