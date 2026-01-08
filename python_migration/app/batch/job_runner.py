"""Job Runner - replaces JCL scripts in src/jcl/batch/.

This module provides Python-based job execution that replaces
the JCL batch job scripts.

JCL Scripts Replaced:
- DAILYJOB.jcl - Daily batch processing
- TRNVALJB.jcl - Transaction validation job
- POSUPDJ.jcl - Position update job
- HISTLDJB.jcl - History load job
"""

import sys
from datetime import datetime
from typing import Callable, Optional

from sqlalchemy.orm import Session

from app.batch.batch_controller import BatchController
from app.batch.history_loader import HistoryLoader
from app.batch.position_updater import PositionUpdater
from app.batch.process_sequencer import ProcessSequencer
from app.batch.recovery_handler import RecoveryHandler
from app.batch.transaction_validator import TransactionValidator
from app.database.connection import get_db_context
from app.models.error import ReturnCode
from app.utils.logging import get_logger, log_batch_end, log_batch_start

logger = get_logger(__name__)


class JobStep:
    """Represents a single step in a batch job."""

    def __init__(
        self,
        name: str,
        program: str,
        execute: Callable[[Session, str, str], int],
        condition: Optional[Callable[[int], bool]] = None,
    ):
        self.name = name
        self.program = program
        self.execute = execute
        self.condition = condition or (lambda rc: rc <= 4)


class JobRunner:
    """Job Runner - replaces JCL job execution.

    This class provides Python-based batch job execution that replaces
    the JCL scripts used in the COBOL system.
    """

    def __init__(self, db: Session):
        self.db = db
        self.batch_controller = BatchController(db)
        self.recovery_handler = RecoveryHandler(db)
        self.process_sequencer = ProcessSequencer(db)
        self.return_code = ReturnCode.SUCCESS
        self.step_results: list[dict] = []

    def run_job(
        self,
        job_name: str,
        process_date: str,
        steps: list[JobStep],
        restart_from: str = None,
    ) -> int:
        """Run a batch job with multiple steps.

        Args:
            job_name: Job name
            process_date: Process date (YYYYMMDD)
            steps: List of job steps to execute
            restart_from: Step name to restart from (optional)

        Returns:
            Final return code
        """
        log_batch_start(logger, "JOBRUN", job_name, process_date)

        batch_control = self.batch_controller.initialize_job(
            job_name, process_date, steps[0].program if steps else "UNKNOWN"
        )

        if not batch_control:
            logger.error(f"Failed to initialize job {job_name}")
            return ReturnCode.SEVERE

        if not self.batch_controller.start_job(job_name, process_date):
            logger.error(f"Failed to start job {job_name}")
            return ReturnCode.SEVERE

        skip_until_restart = restart_from is not None
        max_rc = ReturnCode.SUCCESS

        for step in steps:
            if skip_until_restart:
                if step.name == restart_from:
                    skip_until_restart = False
                else:
                    logger.info(f"Skipping step {step.name} (restart mode)")
                    continue

            if not step.condition(max_rc):
                logger.info(f"Skipping step {step.name} (condition not met, RC={max_rc})")
                self.step_results.append({
                    "step": step.name,
                    "program": step.program,
                    "status": "SKIPPED",
                    "return_code": 0,
                })
                continue

            logger.info(f"Executing step {step.name} ({step.program})")

            try:
                step_rc = step.execute(self.db, job_name, process_date)
                max_rc = max(max_rc, step_rc)

                self.step_results.append({
                    "step": step.name,
                    "program": step.program,
                    "status": "COMPLETED",
                    "return_code": step_rc,
                })

                logger.info(f"Step {step.name} completed with RC={step_rc}")

                if step_rc >= ReturnCode.SEVERE:
                    logger.error(f"Step {step.name} failed with RC={step_rc}")
                    break

            except Exception as e:
                logger.error(f"Step {step.name} aborted: {e}")
                max_rc = ReturnCode.TERMINAL
                self.step_results.append({
                    "step": step.name,
                    "program": step.program,
                    "status": "ABORTED",
                    "return_code": ReturnCode.TERMINAL,
                    "error": str(e),
                })
                break

        self.return_code = max_rc
        self.batch_controller.complete_job(
            job_name,
            process_date,
            max_rc,
            error_desc="" if max_rc <= 4 else f"Job ended with RC={max_rc}",
        )

        log_batch_end(logger, "JOBRUN", job_name, max_rc)
        return max_rc

    def get_results(self) -> dict:
        """Get job execution results."""
        return {
            "return_code": self.return_code,
            "steps": self.step_results,
        }


def run_transaction_validation(db: Session, job_name: str, process_date: str) -> int:
    """Execute transaction validation - replaces TRNVALJB.jcl."""
    validator = TransactionValidator(db)
    stats = validator.get_statistics()
    return stats.get("return_code", ReturnCode.SUCCESS)


def run_position_update(db: Session, job_name: str, process_date: str) -> int:
    """Execute position update - replaces POSUPDJ.jcl."""
    updater = PositionUpdater(db)
    stats = updater.get_statistics()
    return stats.get("return_code", ReturnCode.SUCCESS)


def run_history_load(db: Session, job_name: str, process_date: str) -> int:
    """Execute history load - replaces HISTLDJB.jcl."""
    loader = HistoryLoader(db)
    loader.initialize(job_name, process_date)
    loader.terminate()
    stats = loader.get_statistics()
    return stats.get("return_code", ReturnCode.SUCCESS)


def create_daily_job_steps() -> list[JobStep]:
    """Create steps for daily batch job - replaces DAILYJOB.jcl."""
    return [
        JobStep(
            name="STEP010",
            program="TRNVAL00",
            execute=run_transaction_validation,
        ),
        JobStep(
            name="STEP020",
            program="POSUPD00",
            execute=run_position_update,
            condition=lambda rc: rc <= 4,
        ),
        JobStep(
            name="STEP030",
            program="HISTLD00",
            execute=run_history_load,
            condition=lambda rc: rc <= 4,
        ),
    ]


def run_daily_batch(process_date: str = None) -> int:
    """Run daily batch processing - main entry point.

    Args:
        process_date: Process date (YYYYMMDD), defaults to today

    Returns:
        Final return code
    """
    if process_date is None:
        process_date = datetime.now().strftime("%Y%m%d")

    with get_db_context() as db:
        runner = JobRunner(db)
        steps = create_daily_job_steps()
        return runner.run_job("DAILYJOB", process_date, steps)


def main():
    """Command-line entry point for batch jobs."""
    import argparse

    parser = argparse.ArgumentParser(description="Run batch jobs")
    parser.add_argument(
        "job",
        choices=["daily", "trnval", "posupd", "histld"],
        help="Job to run",
    )
    parser.add_argument(
        "--date",
        default=datetime.now().strftime("%Y%m%d"),
        help="Process date (YYYYMMDD)",
    )
    parser.add_argument(
        "--restart-from",
        help="Step name to restart from",
    )

    args = parser.parse_args()

    with get_db_context() as db:
        runner = JobRunner(db)

        if args.job == "daily":
            steps = create_daily_job_steps()
            rc = runner.run_job("DAILYJOB", args.date, steps, args.restart_from)
        elif args.job == "trnval":
            rc = run_transaction_validation(db, "TRNVALJB", args.date)
        elif args.job == "posupd":
            rc = run_position_update(db, "POSUPDJ", args.date)
        elif args.job == "histld":
            rc = run_history_load(db, "HISTLDJB", args.date)
        else:
            rc = ReturnCode.ERROR

        results = runner.get_results()
        print(f"\nJob Results: RC={rc}")
        for step in results.get("steps", []):
            print(f"  {step['step']}: {step['status']} (RC={step['return_code']})")

        sys.exit(rc)


if __name__ == "__main__":
    main()
