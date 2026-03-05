"""
Process sequence manager translated from COBOL program PRCSEQ00.cbl.

EVALUATE TRUE with 4 functions (INIT/NEXT/STAT/TERM).
Manages process sequencing table with dependency checking.
"""

import logging
from datetime import datetime

from sqlalchemy.orm import Session

from src.common.constants import BatchStatus, ReturnCode, SequencerFunction
from src.common.error_handler import BatchError
from src.db.repository import BatchControlRepository

logger = logging.getLogger(__name__)


class ProcessStep:
    """Represents a single step in the batch sequence."""

    def __init__(
        self,
        step_id: str,
        job_name: str,
        description: str = "",
        dependencies: list[str] | None = None,
        dep_hard_flags: list[bool] | None = None,
        dep_max_rc: list[int] | None = None,
        restartable: bool = True,
    ):
        self.step_id = step_id
        self.job_name = job_name
        self.description = description
        self.dependencies = dependencies or []
        self.dep_hard_flags = dep_hard_flags or []
        self.dep_max_rc = dep_max_rc or []
        self.restartable = restartable
        self.status: str = BatchStatus.READY.value
        self.return_code: int = ReturnCode.SUCCESS.value
        self.start_time: datetime | None = None
        self.end_time: datetime | None = None


class BatchSequencer:
    """
    Orchestrate batch processing steps in correct sequence.
    Translates PRCSEQ00.cbl process sequencing logic.
    """

    def __init__(self, session: Session):
        self.session = session
        self.batch_repo = BatchControlRepository(session)
        self.steps: list[ProcessStep] = []
        self._current_index: int = 0

    def dispatch(self, function: str) -> ReturnCode:
        """
        Main dispatch. Translates PRCSEQ00.cbl EVALUATE TRUE.
        """
        match function:
            case SequencerFunction.INIT:
                return self._initialize()
            case SequencerFunction.NEXT:
                return self._get_next()
            case SequencerFunction.STATUS:
                return self._check_status()
            case SequencerFunction.TERMINATE:
                return self._terminate()
            case _:
                raise BatchError(
                    f"Invalid sequencer function: {function}",
                    error_code="PS01",
                    program="PRCSEQ00",
                )

    def add_step(self, step: ProcessStep) -> None:
        """Add a step to the sequence."""
        self.steps.append(step)

    def _initialize(self) -> ReturnCode:
        """Translates 1000-PROCESS-INITIALIZE."""
        logger.info("Initializing batch sequence with %d steps", len(self.steps))
        self._current_index = 0
        for step in self.steps:
            step.status = BatchStatus.READY.value
        return ReturnCode.SUCCESS

    def _get_next(self) -> ReturnCode:
        """
        Translates 2000-GET-NEXT-PROCESS.
        Find next ready step whose dependencies are satisfied.
        """
        for i in range(self._current_index, len(self.steps)):
            step = self.steps[i]
            if step.status == BatchStatus.READY.value:
                if self._check_dependencies(step):
                    self._current_index = i
                    step.status = BatchStatus.ACTIVE.value
                    step.start_time = datetime.now()
                    logger.info("Next step: %s (%s)", step.step_id, step.description)
                    return ReturnCode.SUCCESS
                else:
                    logger.debug("Step %s: dependencies not met", step.step_id)

        # No more steps
        logger.info("All batch steps complete or blocked")
        return ReturnCode.WARNING

    def _check_dependencies(self, step: ProcessStep) -> bool:
        """
        Translates 2200-CHECK-DEPENDENCIES.
        Evaluates PSR-DEP-HARD and PSR-DEP-RC for dependency status.
        """
        for idx, dep_id in enumerate(step.dependencies):
            dep_step = self._find_step(dep_id)
            if dep_step is None:
                continue

            is_hard = step.dep_hard_flags[idx] if idx < len(step.dep_hard_flags) else True
            max_rc = step.dep_max_rc[idx] if idx < len(step.dep_max_rc) else 0

            if dep_step.status != BatchStatus.DONE.value:
                if is_hard:
                    return False
                # Soft dependency — check if it errored
                if dep_step.status == BatchStatus.ERROR.value:
                    return False

            # Check return code threshold
            if dep_step.return_code > max_rc:
                if is_hard:
                    return False

        return True

    def _check_status(self) -> ReturnCode:
        """
        Translates 3000-CHECK-STATUS with 3300-CHECK-COMPLETION.
        Count active/error/done processes.
        """
        active = sum(1 for s in self.steps if s.status == BatchStatus.ACTIVE.value)
        errors = sum(1 for s in self.steps if s.status == BatchStatus.ERROR.value)
        done = sum(1 for s in self.steps if s.status == BatchStatus.DONE.value)

        logger.info(
            "Sequence status: %d active, %d done, %d errors, %d total",
            active, done, errors, len(self.steps),
        )

        if errors > 0:
            return ReturnCode.ERROR
        if done == len(self.steps):
            return ReturnCode.SUCCESS
        return ReturnCode.WARNING

    def _terminate(self) -> ReturnCode:
        """Translates 4000-PROCESS-TERMINATE."""
        logger.info("Terminating batch sequence")
        return self._check_status()

    def mark_step_complete(
        self, step_id: str, return_code: ReturnCode = ReturnCode.SUCCESS
    ) -> None:
        """Mark a step as completed."""
        step = self._find_step(step_id)
        if step:
            step.end_time = datetime.now()
            step.return_code = return_code.value
            if return_code.value <= ReturnCode.WARNING.value:
                step.status = BatchStatus.DONE.value
            else:
                step.status = BatchStatus.ERROR.value

    def mark_step_error(self, step_id: str, return_code: ReturnCode = ReturnCode.ERROR) -> None:
        """Mark a step as errored."""
        step = self._find_step(step_id)
        if step:
            step.end_time = datetime.now()
            step.return_code = return_code.value
            step.status = BatchStatus.ERROR.value

    def get_current_step(self) -> ProcessStep | None:
        """Get the currently active step."""
        if 0 <= self._current_index < len(self.steps):
            return self.steps[self._current_index]
        return None

    def _find_step(self, step_id: str) -> ProcessStep | None:
        for step in self.steps:
            if step.step_id == step_id:
                return step
        return None
