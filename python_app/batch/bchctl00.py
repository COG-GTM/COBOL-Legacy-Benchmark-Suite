"""Batch Control module - replaces BCHCTL00.cbl.

Manages batch job control with functions for initialization,
status checking, updating, and termination.

COBOL program flow (EVALUATE LS-BCT-FUNCTION):
- INIT: Initialize batch control record
- CHEK: Check prerequisites and job status
- UPDT: Update job status and return code
- TERM: Terminate and finalize batch control
"""

import logging
from datetime import datetime
from typing import Any

from python_app.models.batch_control import BatchControlRecord, BatchStatus

logger = logging.getLogger("portfolio.batch.bchctl00")


class BatchController:
    """Batch control processor replacing BCHCTL00.cbl.

    Manages job lifecycle: INIT -> CHEK -> UPDT -> TERM.
    """

    def __init__(self) -> None:
        self.jobs: dict[str, BatchControlRecord] = {}

    def init_job(
        self,
        job_name: str,
        process_date: str,
        program_name: str = "",
        sequence_no: int = 0,
    ) -> BatchControlRecord:
        """Initialize a batch control record - replaces P100-INITIALIZE.

        COBOL: Sets BCT-STATUS to 'A' (Active), records start time.
        """
        record = BatchControlRecord(
            job_name=job_name,
            process_date=process_date,
            sequence_no=sequence_no,
            status=BatchStatus.ACTIVE,
            program_name=program_name,
            start_time=datetime.now().strftime("%H%M%S%f")[:8],
        )
        key = record.composite_key
        self.jobs[key] = record
        logger.info("BCHCTL00 INIT: job=%s, date=%s, seq=%d", job_name, process_date, sequence_no)
        return record

    def check_job(self, job_name: str, process_date: str, sequence_no: int = 0) -> dict[str, Any]:
        """Check job prerequisites and status - replaces P200-CHECK-STATUS.

        COBOL: Reads BCT record, checks BCT-PREREQ-JOBS,
        verifies all prerequisites are DONE with RC <= 4.
        """
        key = f"{job_name}{process_date}{sequence_no:04d}"
        record = self.jobs.get(key)

        if record is None:
            return {"status": "NOT_FOUND", "can_run": False, "message": f"Job {job_name} not found"}

        # Check prerequisites
        prereqs_met = True
        failed_prereqs: list[str] = []
        for prereq in record.prereq_jobs:
            prereq_key = f"{prereq.name}{process_date}{prereq.sequence:04d}"
            prereq_record = self.jobs.get(prereq_key)
            if prereq_record is None or prereq_record.status != BatchStatus.DONE:
                prereqs_met = False
                failed_prereqs.append(prereq.name)
            elif prereq_record.return_code > 4:
                prereqs_met = False
                failed_prereqs.append(f"{prereq.name}(RC={prereq_record.return_code})")

        return {
            "status": record.status,
            "can_run": prereqs_met and record.status in (BatchStatus.READY, BatchStatus.ACTIVE),
            "failed_prereqs": failed_prereqs,
            "return_code": record.return_code,
        }

    def update_job(
        self,
        job_name: str,
        process_date: str,
        *,
        status: BatchStatus | None = None,
        return_code: int | None = None,
        step_name: str = "",
        sequence_no: int = 0,
    ) -> BatchControlRecord | None:
        """Update job status - replaces P300-UPDATE-STATUS.

        COBOL: Updates BCT-STATUS, BCT-RETURN-CODE, BCT-STEP-NAME.
        """
        key = f"{job_name}{process_date}{sequence_no:04d}"
        record = self.jobs.get(key)

        if record is None:
            logger.warning("BCHCTL00 UPDT: job %s not found", job_name)
            return None

        if status is not None:
            record.status = status
        if return_code is not None:
            record.return_code = return_code
        if step_name:
            record.step_name = step_name

        record.attempt_ts = datetime.now().isoformat()
        logger.info(
            "BCHCTL00 UPDT: job=%s, status=%s, rc=%d",
            job_name, record.status, record.return_code,
        )
        return record

    def terminate_job(
        self,
        job_name: str,
        process_date: str,
        return_code: int = 0,
        sequence_no: int = 0,
    ) -> BatchControlRecord | None:
        """Terminate a batch job - replaces P400-TERMINATE.

        COBOL: Sets BCT-STATUS to DONE or ERROR, records end time.
        """
        key = f"{job_name}{process_date}{sequence_no:04d}"
        record = self.jobs.get(key)

        if record is None:
            logger.warning("BCHCTL00 TERM: job %s not found", job_name)
            return None

        record.return_code = return_code
        record.end_time = datetime.now().strftime("%H%M%S%f")[:8]

        if return_code <= 4:
            record.status = BatchStatus.DONE
        else:
            record.status = BatchStatus.ERROR

        logger.info(
            "BCHCTL00 TERM: job=%s, status=%s, rc=%d",
            job_name, record.status, record.return_code,
        )
        return record

    def get_all_jobs(self) -> list[BatchControlRecord]:
        """Get all batch control records."""
        return list(self.jobs.values())
