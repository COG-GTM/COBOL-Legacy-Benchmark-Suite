"""
Batch sequencer translated from COBOL program PRCSEQ00.cbl.

Replaces:
  - PRCSEQ00.cbl: Orchestrate batch processing steps in correct sequence
  - Handle step dependencies and skip conditions
  - Manage process frequency (daily, weekly, monthly)
"""

import logging
from collections.abc import Callable
from datetime import date

from src.common.constants import (
    DependencyType,
    ProcessFrequency,
    ReturnCode,
)
from src.common.error_handler import BatchError
from src.models.batch_control import ProcessSequenceRecord

logger = logging.getLogger(__name__)


class StepResult:
    """Result of executing a single batch step."""

    def __init__(
        self,
        step_number: int,
        program_name: str,
        return_code: ReturnCode,
        message: str = "",
    ) -> None:
        self.step_number = step_number
        self.program_name = program_name
        self.return_code = return_code
        self.message = message

    @property
    def success(self) -> bool:
        return self.return_code <= ReturnCode.WARNING


class BatchSequencer:
    """
    Batch step sequencer.

    Translates PRCSEQ00.cbl:
      1000-LOAD-SEQUENCE      -> load_sequence()
      2000-VALIDATE-SEQUENCE  -> validate_sequence()
      3000-EXECUTE-SEQUENCE   -> execute()
      3100-CHECK-DEPENDENCY   -> _check_dependency()
      3200-CHECK-FREQUENCY    -> _check_frequency()
      3300-EXECUTE-STEP       -> _execute_step()
    """

    def __init__(self) -> None:
        self._steps: list[ProcessSequenceRecord] = []
        self._results: list[StepResult] = []
        self._step_handlers: dict[str, Callable[[], ReturnCode]] = {}

    def register_step(self, program_name: str, handler: Callable[[], ReturnCode]) -> None:
        """Register a callable handler for a step program name."""
        self._step_handlers[program_name] = handler

    def load_sequence(self, steps: list[ProcessSequenceRecord]) -> None:
        """
        Load processing sequence.

        Translates PRCSEQ00.cbl 1000-LOAD-SEQUENCE.
        Steps are sorted by step_number.
        """
        self._steps = sorted(steps, key=lambda s: s.step_number)
        self._results = []
        logger.info("Loaded %d batch steps", len(self._steps))

    def validate_sequence(self) -> ReturnCode:
        """
        Validate the loaded sequence.

        Translates PRCSEQ00.cbl 2000-VALIDATE-SEQUENCE.
        Checks for missing handlers and duplicate step numbers.
        """
        step_numbers = [s.step_number for s in self._steps]
        if len(step_numbers) != len(set(step_numbers)):
            logger.error("Duplicate step numbers found in sequence")
            return ReturnCode.ERROR

        for step in self._steps:
            if step.program_name not in self._step_handlers and not step.skip_flag:
                logger.warning(
                    "No handler registered for step %d: %s",
                    step.step_number,
                    step.program_name,
                )

        return ReturnCode.SUCCESS

    def execute(self, process_date: date, restart_step: str = "") -> ReturnCode:
        """
        Execute the loaded sequence.

        Translates PRCSEQ00.cbl 3000-EXECUTE-SEQUENCE.
        Processes steps in order, respecting dependencies and frequency filters.

        Args:
            process_date: Business date for frequency checking.
            restart_step: If provided, skip steps before this one.

        Returns:
            Overall return code (highest from all steps).
        """
        logger.info("Starting batch sequence execution for date %s", process_date)
        max_rc = ReturnCode.SUCCESS
        skip_until_restart = bool(restart_step)

        for step in self._steps:
            # Handle restart: skip steps until we reach the restart point
            if skip_until_restart:
                if step.program_name == restart_step:
                    skip_until_restart = False
                    logger.info("Restart point reached: %s", restart_step)
                else:
                    logger.info("Skipping step %d (before restart point)", step.step_number)
                    continue

            # Check skip flag
            if step.skip_flag:
                logger.info("Skipping step %d: %s (skip flag set)", step.step_number, step.program_name)
                continue

            # 3200-CHECK-FREQUENCY
            if not self._check_frequency(step, process_date):
                logger.info(
                    "Skipping step %d: %s (frequency filter)",
                    step.step_number,
                    step.program_name,
                )
                continue

            # 3100-CHECK-DEPENDENCY
            if not self._check_dependency(step):
                if step.dependency_type == DependencyType.REQUIRED:
                    logger.error(
                        "Required dependency failed for step %d: %s",
                        step.step_number,
                        step.program_name,
                    )
                    result = StepResult(
                        step.step_number, step.program_name, ReturnCode.ERROR,
                        "Dependency check failed",
                    )
                    self._results.append(result)
                    max_rc = max(max_rc, ReturnCode.ERROR)
                    break
                logger.warning(
                    "Optional dependency not met for step %d, continuing",
                    step.step_number,
                )

            # 3300-EXECUTE-STEP
            result = self._execute_step(step)
            self._results.append(result)
            max_rc = max(max_rc, result.return_code)

            # Abort on severe error
            if result.return_code >= ReturnCode.SEVERE:
                logger.error(
                    "Severe error at step %d: %s, aborting sequence",
                    step.step_number,
                    step.program_name,
                )
                break

        logger.info("Batch sequence completed with RC=%d", max_rc)
        return max_rc

    @property
    def results(self) -> list[StepResult]:
        """Get step execution results."""
        return self._results

    def _check_dependency(self, step: ProcessSequenceRecord) -> bool:
        """
        Check if step dependencies are satisfied.

        Translates PRCSEQ00.cbl 3100-CHECK-DEPENDENCY.
        A step's dependency is satisfied if all prior steps completed successfully.
        """
        if step.dependency_type in (DependencyType.OPTIONAL, DependencyType.SOFT):
            return True

        # Check all prior results succeeded
        for result in self._results:
            if result.return_code >= ReturnCode.ERROR:
                return False
        return True

    def _check_frequency(self, step: ProcessSequenceRecord, process_date: date) -> bool:
        """
        Check if step should run based on frequency.

        Translates PRCSEQ00.cbl 3200-CHECK-FREQUENCY.
        """
        match step.frequency:
            case ProcessFrequency.DAILY:
                return True
            case ProcessFrequency.WEEKLY:
                return process_date.weekday() == 0  # Monday
            case ProcessFrequency.MONTHLY:
                return process_date.day == 1  # First of month
            case _:
                return True

    def _execute_step(self, step: ProcessSequenceRecord) -> StepResult:
        """
        Execute a single batch step.

        Translates PRCSEQ00.cbl 3300-EXECUTE-STEP.
        """
        logger.info(
            "Executing step %d: %s (%s)",
            step.step_number,
            step.program_name,
            step.description,
        )
        handler = self._step_handlers.get(step.program_name)
        if handler is None:
            logger.warning("No handler for step %s, skipping", step.program_name)
            return StepResult(
                step.step_number, step.program_name, ReturnCode.WARNING,
                "No handler registered",
            )

        try:
            rc = handler()
            logger.info("Step %d completed with RC=%d", step.step_number, rc)
            return StepResult(step.step_number, step.program_name, rc)
        except BatchError as exc:
            logger.error("Step %d failed: %s", step.step_number, exc)
            return StepResult(
                step.step_number, step.program_name, exc.severity, str(exc),
            )
        except Exception as exc:
            logger.error("Step %d unexpected error: %s", step.step_number, exc)
            return StepResult(
                step.step_number, step.program_name, ReturnCode.SEVERE, str(exc),
            )
