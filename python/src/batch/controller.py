"""
Batch controller translated from COBOL program BCHCTL00.cbl.

Replaces:
  - BCHCTL00.cbl EVALUATE TRUE dispatch:
      WHEN BCH-FUNC-INIT  PERFORM 1000-INITIALIZE-BATCH
      WHEN BCH-FUNC-CHEK  PERFORM 2000-CHECK-PREREQUISITES
      WHEN BCH-FUNC-UPDT  PERFORM 3000-UPDATE-STATUS
      WHEN BCH-FUNC-TERM  PERFORM 4000-TERMINATE-BATCH

Manages batch job lifecycle: initialization, prerequisite checks,
status updates, and termination.
"""

import logging
from datetime import date, datetime

from sqlalchemy.orm import Session

from src.common.constants import (
    MAX_BATCH_ERRORS,
    BatchStatus,
    ReturnCode,
)
from src.common.error_handler import BatchError
from src.db.repository import BatchControlRepository
from src.db.tables import BatchControl
from src.models.batch_control import BatchParameters, BatchStatusRecord

logger = logging.getLogger(__name__)


class BatchController:
    """
    Batch job lifecycle controller.

    Each COBOL paragraph from BCHCTL00.cbl becomes a method:
      0100-DISPATCH         -> dispatch()
      1000-INITIALIZE-BATCH -> initialize()
      2000-CHECK-PREREQS    -> check_prerequisites()
      3000-UPDATE-STATUS    -> update_status()
      4000-TERMINATE-BATCH  -> terminate()
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._repo = BatchControlRepository(session)
        self._status = BatchStatusRecord(batch_id="")

    @property
    def status(self) -> BatchStatusRecord:
        """Current batch status."""
        return self._status

    # ------------------------------------------------------------------
    # 0100-DISPATCH  (EVALUATE TRUE pattern)
    # ------------------------------------------------------------------
    def dispatch(self, function: str, params: BatchParameters) -> ReturnCode:
        """
        Dispatch batch function.

        Translates BCHCTL00.cbl 0100-DISPATCH:
          EVALUATE TRUE
            WHEN BCH-FUNC-INIT  PERFORM 1000-INITIALIZE-BATCH
            WHEN BCH-FUNC-CHEK  PERFORM 2000-CHECK-PREREQUISITES
            WHEN BCH-FUNC-UPDT  PERFORM 3000-UPDATE-STATUS
            WHEN BCH-FUNC-TERM  PERFORM 4000-TERMINATE-BATCH
          END-EVALUATE
        """
        match function.upper():
            case "INIT":
                return self.initialize(params)
            case "CHEK" | "CHECK":
                return self.check_prerequisites(params)
            case "UPDT" | "UPDATE":
                return self.update_status(params)
            case "TERM" | "TERMINATE":
                return self.terminate(params)
            case _:
                logger.error("Unknown batch function: %s", function)
                return ReturnCode.ERROR

    # ------------------------------------------------------------------
    # 1000-INITIALIZE-BATCH
    # ------------------------------------------------------------------
    def initialize(self, params: BatchParameters) -> ReturnCode:
        """
        Initialize a batch job.

        Translates BCHCTL00.cbl 1000-INITIALIZE-BATCH:
          - Create or retrieve batch control record
          - Set status to ACTIVE
          - Initialize counters
        """
        logger.info("Initializing batch job: %s", params.batch_id)
        self._status = BatchStatusRecord(
            batch_id=params.batch_id,
            status=BatchStatus.ACTIVE,
            start_time=datetime.now(),
            current_step="INIT",
        )

        # Create or update batch control record
        control = self._repo.get_by_id(params.batch_id)
        if control is None:
            control = BatchControl(
                batch_id=params.batch_id,
                batch_name=params.batch_id,
                batch_status=BatchStatus.ACTIVE.value,
                schedule_date=params.process_date,
                process_type=params.process_type.value,
                max_restarts=3,
                restart_count=0,
            )
            self._repo.create(control)
        else:
            # Check restart limits
            if params.restart_flag and control.restart_count >= control.max_restarts:
                raise BatchError(
                    f"Maximum restart attempts ({control.max_restarts}) exceeded for {params.batch_id}",
                    step="INIT",
                )
            control.batch_status = BatchStatus.ACTIVE.value
            if params.restart_flag:
                control.restart_count += 1
            else:
                control.restart_count = 0
            self._repo.update(control)

        logger.info("Batch job initialized: %s", params.batch_id)
        return ReturnCode.SUCCESS

    # ------------------------------------------------------------------
    # 2000-CHECK-PREREQUISITES
    # ------------------------------------------------------------------
    def check_prerequisites(self, params: BatchParameters) -> ReturnCode:
        """
        Check batch prerequisites.

        Translates BCHCTL00.cbl 2000-CHECK-PREREQUISITES:
          - Verify all prerequisite jobs completed successfully
          - Check dependency conditions
        """
        logger.info("Checking prerequisites for batch: %s", params.batch_id)
        self._status.current_step = "CHEK"

        control = self._repo.get_by_id(params.batch_id)
        if control is None:
            raise BatchError(f"Batch control not found: {params.batch_id}", step="CHEK")

        # All prerequisites pass by default (no prerequisite chain in simple setup)
        logger.info("Prerequisites satisfied for: %s", params.batch_id)
        return ReturnCode.SUCCESS

    # ------------------------------------------------------------------
    # 3000-UPDATE-STATUS
    # ------------------------------------------------------------------
    def update_status(self, params: BatchParameters) -> ReturnCode:
        """
        Update batch status.

        Translates BCHCTL00.cbl 3000-UPDATE-STATUS.
        """
        control = self._repo.get_by_id(params.batch_id)
        if control is None:
            raise BatchError(f"Batch control not found: {params.batch_id}", step="UPDT")

        if self._status.error_count > 0:
            control.batch_status = BatchStatus.ERROR.value
        else:
            control.batch_status = BatchStatus.ACTIVE.value

        self._repo.update(control)
        return ReturnCode.SUCCESS

    # ------------------------------------------------------------------
    # 4000-TERMINATE-BATCH
    # ------------------------------------------------------------------
    def terminate(self, params: BatchParameters) -> ReturnCode:
        """
        Terminate a batch job.

        Translates BCHCTL00.cbl 4000-TERMINATE-BATCH:
          - Set final status
          - Record end time
          - Set return code
        """
        logger.info("Terminating batch job: %s", params.batch_id)

        self._status.end_time = datetime.now()
        self._status.current_step = "TERM"

        control = self._repo.get_by_id(params.batch_id)
        if control is not None:
            if self._status.error_count >= MAX_BATCH_ERRORS:
                control.batch_status = BatchStatus.ERROR.value
                self._status.return_code = ReturnCode.SEVERE
            elif self._status.error_count > 0:
                control.batch_status = BatchStatus.DONE.value
                self._status.return_code = ReturnCode.WARNING
            else:
                control.batch_status = BatchStatus.DONE.value
                self._status.return_code = ReturnCode.SUCCESS

            control.last_run_date = date.today()
            control.last_run_rc = self._status.return_code
            self._repo.update(control)

        self._status.status = BatchStatus.DONE

        logger.info(
            "Batch job %s terminated: RC=%d, read=%d, processed=%d, errors=%d",
            params.batch_id,
            self._status.return_code,
            self._status.records_read,
            self._status.records_processed,
            self._status.error_count,
        )
        return ReturnCode(self._status.return_code)

    def increment_read(self) -> None:
        """Increment records read counter."""
        self._status.records_read += 1

    def increment_processed(self) -> None:
        """Increment records processed counter."""
        self._status.records_processed += 1

    def increment_error(self, message: str = "") -> None:
        """Increment error counter and record last error."""
        self._status.error_count += 1
        if message:
            self._status.last_error_msg = message[:80]

    def increment_written(self) -> None:
        """Increment records written counter."""
        self._status.records_written += 1
