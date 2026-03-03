"""Batch Pipeline Runner - replaces JCL job sequencing and BCHCTL00/PRCSEQ00.

Implements the sequential batch pipeline with RC <= 4 gating:
TRNVAL00 -> POSUPD00 -> HISTLD00 -> RPTPOS00 -> End of Day

Enforces return code gating between steps:
- RC 0: Success, continue
- RC 1-4: Warning, continue
- RC 5+: Error, stop pipeline
"""

import logging
import time
from datetime import datetime
from enum import StrEnum
from typing import Any, Callable

from python_app.models.return_code import RC_MAX_CONTINUE, RC_SUCCESS, classify_return_code

logger = logging.getLogger("portfolio.batch.pipeline")


class PipelineStatus(StrEnum):
    """Pipeline execution status."""

    PENDING = "PENDING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    STOPPED = "STOPPED"


class StepResult:
    """Result of a single pipeline step."""

    def __init__(self, step_name: str, return_code: int, elapsed: float) -> None:
        self.step_name = step_name
        self.return_code = return_code
        self.elapsed = elapsed
        self.status = classify_return_code(return_code)
        self.timestamp = datetime.now().isoformat()

    def can_continue(self) -> bool:
        """Check if pipeline can continue after this step (RC <= 4)."""
        return self.return_code <= RC_MAX_CONTINUE


class PipelineStep:
    """Definition of a pipeline step."""

    def __init__(
        self,
        name: str,
        func: Callable[..., int],
        args: tuple[Any, ...] = (),
        kwargs: dict[str, Any] | None = None,
    ) -> None:
        self.name = name
        self.func = func
        self.args = args
        self.kwargs = kwargs or {}


class BatchPipeline:
    """Batch pipeline runner replacing BCHCTL00/PRCSEQ00 job sequencing.

    Executes steps sequentially with RC gating. If any step returns
    RC > 4, the pipeline stops (matching COBOL IF WS-RETURN-CODE > 4).
    """

    def __init__(self, pipeline_name: str = "EOD") -> None:
        self.pipeline_name = pipeline_name
        self.steps: list[PipelineStep] = []
        self.results: list[StepResult] = []
        self.status = PipelineStatus.PENDING
        self.start_time: float | None = None
        self.end_time: float | None = None
        self.highest_rc = RC_SUCCESS

    def add_step(
        self,
        name: str,
        func: Callable[..., int],
        args: tuple[Any, ...] = (),
        kwargs: dict[str, Any] | None = None,
    ) -> None:
        """Add a step to the pipeline."""
        self.steps.append(PipelineStep(name, func, args, kwargs))

    def execute(self) -> int:
        """Execute the pipeline - replaces PRCSEQ00 P200-PROCESS-SEQUENCE.

        Runs each step sequentially. If a step returns RC > 4,
        the pipeline stops (matching COBOL gating logic).

        Returns the highest return code from all executed steps.
        """
        self.status = PipelineStatus.RUNNING
        self.start_time = time.time()
        self.results.clear()
        self.highest_rc = RC_SUCCESS

        logger.info(
            "Pipeline '%s' started at %s with %d steps",
            self.pipeline_name, datetime.now().isoformat(), len(self.steps),
        )

        for i, step in enumerate(self.steps, 1):
            logger.info(
                "Step %d/%d: %s - STARTING",
                i, len(self.steps), step.name,
            )

            step_start = time.time()
            try:
                rc = step.func(*step.args, **step.kwargs)
            except Exception as exc:
                logger.error("Step %s failed with exception: %s", step.name, exc)
                rc = 12  # SEVERE

            elapsed = time.time() - step_start
            result = StepResult(step.name, rc, elapsed)
            self.results.append(result)

            if rc > self.highest_rc:
                self.highest_rc = rc

            logger.info(
                "Step %d/%d: %s - RC=%d (%s) in %.2fs",
                i, len(self.steps), step.name, rc, result.status, elapsed,
            )

            # RC gating: stop if RC > 4
            if not result.can_continue():
                logger.warning(
                    "Pipeline '%s' STOPPED at step '%s' - RC=%d exceeds threshold %d",
                    self.pipeline_name, step.name, rc, RC_MAX_CONTINUE,
                )
                self.status = PipelineStatus.STOPPED
                self.end_time = time.time()
                return self.highest_rc

        self.status = PipelineStatus.COMPLETED
        self.end_time = time.time()
        total_elapsed = self.end_time - self.start_time

        logger.info(
            "Pipeline '%s' COMPLETED - highest RC=%d, total time=%.2fs",
            self.pipeline_name, self.highest_rc, total_elapsed,
        )
        return self.highest_rc

    def get_summary(self) -> dict[str, Any]:
        """Get pipeline execution summary."""
        return {
            "pipeline_name": self.pipeline_name,
            "status": self.status,
            "highest_rc": self.highest_rc,
            "total_steps": len(self.steps),
            "executed_steps": len(self.results),
            "elapsed_seconds": round(
                (self.end_time or time.time()) - (self.start_time or time.time()), 2
            ),
            "steps": [
                {
                    "name": r.step_name,
                    "return_code": r.return_code,
                    "status": r.status,
                    "elapsed": round(r.elapsed, 2),
                    "timestamp": r.timestamp,
                }
                for r in self.results
            ],
        }
