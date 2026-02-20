"""Batch Control Processor - migrated from BCHCTL00.cbl.

Manages process initialization, prerequisite checking, status updates,
and termination using indexed control file.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.batch_control import (
    BatchControlRecord,
    BatchControlConstants,
    BatchStatus,
)
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "BCHCTL00"


class BatchControlProcessor:
    def __init__(self):
        self._control_records: dict[str, BatchControlRecord] = {}
        self._return_code = ReturnCode.SUCCESS

    def process_request(self, function_code: str, control_request: BatchControlRecord) -> int:
        if function_code == "INIT":
            return self._process_initialize(control_request)
        elif function_code == "CHEK":
            return self._check_prerequisites(control_request)
        elif function_code == "UPDT":
            return self._update_status(control_request)
        elif function_code == "TERM":
            return self._process_terminate(control_request)
        else:
            logger.error("Invalid function code: %s", function_code)
            return ReturnCode.ERROR

    def _process_initialize(self, control_request: BatchControlRecord) -> int:
        key = control_request.batch_key

        if key in self._control_records:
            existing = self._control_records[key]
            if existing.status == BatchStatus.ERROR:
                if existing.restart_count >= BatchControlConstants.MAX_RESTARTS:
                    logger.error("Max restarts exceeded for %s", key)
                    return ReturnCode.ERROR
                existing.restart_count += 1
                existing.status = BatchStatus.ACTIVE
                existing.attempt_ts = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
                logger.info("Restarting process %s (attempt %d)", key, existing.restart_count)
            else:
                logger.error("Process %s already exists with status %s", key, existing.status)
                return ReturnCode.ERROR
        else:
            control_request.status = BatchStatus.ACTIVE
            control_request.attempt_ts = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
            self._control_records[key] = control_request
            logger.info("Process %s initialized", key)

        return ReturnCode.SUCCESS

    def _check_prerequisites(self, control_request: BatchControlRecord) -> int:
        for i in range(control_request.prereq_count):
            prereq = control_request.prereq_jobs[i]
            prereq_key = f"{prereq.name}{control_request.process_date}"

            found = False
            for stored_key, stored_rec in self._control_records.items():
                if stored_key.startswith(prereq_key):
                    found = True
                    if stored_rec.status != BatchStatus.DONE:
                        logger.warning(
                            "Prerequisite %s not complete (status: %s)",
                            prereq.name,
                            stored_rec.status,
                        )
                        return ReturnCode.WARNING
                    if stored_rec.return_code > prereq.return_code:
                        logger.warning(
                            "Prerequisite %s exceeded max RC (%d > %d)",
                            prereq.name,
                            stored_rec.return_code,
                            prereq.return_code,
                        )
                        return ReturnCode.WARNING
                    break

            if not found:
                logger.warning("Prerequisite %s not found", prereq.name)
                return ReturnCode.WARNING

        logger.info("All prerequisites satisfied for %s", control_request.batch_key)
        return ReturnCode.SUCCESS

    def _update_status(self, control_request: BatchControlRecord) -> int:
        key = control_request.batch_key
        if key not in self._control_records:
            logger.error("Process %s not found for status update", key)
            return ReturnCode.ERROR

        record = self._control_records[key]
        record.status = control_request.status
        record.return_code = control_request.return_code
        record.error_desc = control_request.error_desc

        logger.info("Process %s status updated to %s (RC=%d)", key, record.status, record.return_code)
        return ReturnCode.SUCCESS

    def _process_terminate(self, control_request: BatchControlRecord) -> int:
        key = control_request.batch_key
        if key not in self._control_records:
            logger.error("Process %s not found for termination", key)
            return ReturnCode.ERROR

        record = self._control_records[key]
        record.status = BatchStatus.DONE
        record.complete_ts = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
        record.return_code = control_request.return_code

        logger.info("Process %s terminated (RC=%d)", key, record.return_code)
        return ReturnCode.SUCCESS

    def load_from_file(self, file_path: str) -> int:
        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|")
                    if len(parts) >= 5:
                        record = BatchControlRecord(
                            job_name=parts[0].strip(),
                            process_date=parts[1].strip(),
                            sequence_no=int(parts[2].strip()),
                            status=parts[3].strip(),
                            program_name=parts[4].strip(),
                            return_code=int(parts[5].strip()) if len(parts) > 5 else 0,
                            error_desc=parts[6].strip() if len(parts) > 6 else "",
                        )
                        self._control_records[record.batch_key] = record
            return ReturnCode.SUCCESS
        except FileNotFoundError:
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error loading control file: %s", e)
            return ReturnCode.ERROR

    def save_to_file(self, file_path: str) -> int:
        try:
            with open(file_path, "w") as f:
                for record in self._control_records.values():
                    f.write(
                        f"{record.job_name}|{record.process_date}|"
                        f"{record.sequence_no}|{record.status}|"
                        f"{record.program_name}|{record.return_code}|"
                        f"{record.error_desc}\n"
                    )
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving control file: %s", e)
            return ReturnCode.ERROR

    def get_record(self, key: str) -> Optional[BatchControlRecord]:
        return self._control_records.get(key)
