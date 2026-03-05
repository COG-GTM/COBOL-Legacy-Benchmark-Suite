"""
Process recovery handler translated from COBOL program RCVPRC00.cbl.

Recovery modes: PROCESS, SEQUENCE, ALL
Recovery actions: RESTART, BYPASS, TERMINATE
Translates:
- 2110-DETERMINE-ACTION: evaluate restartable/restart_count/max_restarts
- Recovery actions update batch control status
"""

import logging
from datetime import datetime

from sqlalchemy.orm import Session

from src.common.constants import (
    BatchStatus,
    RecoveryAction,
    RecoveryMode,
    ReturnCode,
)
from src.common.error_handler import BatchError
from src.db.repository import BatchControlRepository

logger = logging.getLogger(__name__)


class ProcessRecovery:
    """
    Handle recovery of failed batch processes.
    Translates RCVPRC00.cbl recovery logic.
    """

    def __init__(self, session: Session):
        self.session = session
        self.batch_repo = BatchControlRepository(session)
        self.recovered: int = 0
        self.bypassed: int = 0
        self.terminated: int = 0

    def recover(
        self, mode: str, process_date: str, job_name: str | None = None
    ) -> ReturnCode:
        """
        Main recovery dispatch.
        Translates RCVPRC00.cbl EVALUATE RCV-MODE.
        """
        match mode:
            case RecoveryMode.PROCESS:
                return self._recover_process(job_name or "", process_date)
            case RecoveryMode.SEQUENCE:
                return self._recover_sequence(process_date)
            case RecoveryMode.ALL:
                return self._recover_all(process_date)
            case _:
                raise BatchError(
                    f"Invalid recovery mode: {mode}",
                    error_code="RC01",
                    program="RCVPRC00",
                )

    def _recover_process(self, job_name: str, process_date: str) -> ReturnCode:
        """Recover a single process."""
        record = self.batch_repo.get(job_name, process_date)
        if record is None:
            raise BatchError(
                f"Batch record not found: {job_name}/{process_date}",
                job_name=job_name,
                error_code="RC02",
                program="RCVPRC00",
            )

        if record.status != BatchStatus.ERROR.value:
            logger.info("Job %s is not in error state: %s", job_name, record.status)
            return ReturnCode.WARNING

        action = self._determine_action(record)
        self._apply_action(record, action)
        return ReturnCode.SUCCESS

    def _recover_sequence(self, process_date: str) -> ReturnCode:
        """Recover all failed processes for a date."""
        records = self.batch_repo.list_by_date(process_date)
        failed = [r for r in records if r.status == BatchStatus.ERROR.value]

        if not failed:
            logger.info("No failed processes for date %s", process_date)
            return ReturnCode.SUCCESS

        for record in failed:
            action = self._determine_action(record)
            self._apply_action(record, action)

        return ReturnCode.SUCCESS

    def _recover_all(self, process_date: str) -> ReturnCode:
        """Recover all processes."""
        return self._recover_sequence(process_date)

    def _determine_action(self, record) -> str:
        """
        Translates RCVPRC00.cbl 2110-DETERMINE-ACTION.
        Evaluates PSR-RESTARTABLE, BCT-RESTART-COUNT, BCT-MAX-RESTARTS.
        """
        # Check if restartable
        if record.restart_count >= record.max_restarts:
            logger.warning(
                "Job %s exceeded max restarts (%d/%d) — terminating",
                record.job_name, record.restart_count, record.max_restarts,
            )
            return RecoveryAction.TERMINATE

        # Default: restart
        logger.info(
            "Job %s will be restarted (attempt %d/%d)",
            record.job_name, record.restart_count + 1, record.max_restarts,
        )
        return RecoveryAction.RESTART

    def _apply_action(self, record, action: str) -> None:
        """
        Translates RCVPRC00.cbl recovery action application.
        Updates BCT-STATUS based on determined action.
        """
        match action:
            case RecoveryAction.RESTART:
                record.status = BatchStatus.READY.value
                record.restart_count += 1
                record.attempt_ts = datetime.now()
                self.recovered += 1
                logger.info("Job %s set to READY for restart", record.job_name)

            case RecoveryAction.BYPASS:
                record.status = BatchStatus.DONE.value
                record.return_code = ReturnCode.WARNING.value
                self.bypassed += 1
                logger.info("Job %s bypassed", record.job_name)

            case RecoveryAction.TERMINATE:
                record.status = BatchStatus.SUSPENDED.value
                record.return_code = ReturnCode.SEVERE.value
                self.terminated += 1
                logger.warning("Job %s terminated", record.job_name)

        self.batch_repo.update(record)

    def get_summary(self) -> dict:
        return {
            "recovered": self.recovered,
            "bypassed": self.bypassed,
            "terminated": self.terminated,
        }
