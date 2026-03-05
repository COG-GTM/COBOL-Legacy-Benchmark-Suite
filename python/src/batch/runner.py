"""
Batch runner replacing JCL job definitions from src/jcl/.

Orchestrates the full batch cycle with CLI interface.
Can be scheduled via cron, APScheduler, or Celery.

Usage:
    python -m src.batch.runner --full-cycle --process-date 20240101
    python -m src.batch.runner --step validate --process-date 20240101
"""

import argparse
import logging
import sys
from datetime import date

from src.batch.controller import BatchController
from src.batch.history_loader import HistoryLoader
from src.batch.position_updater import PositionUpdater
from src.batch.recovery import ProcessRecovery
from src.batch.reporting import BatchReporting
from src.batch.sequencer import BatchSequencer, ProcessStep
from src.batch.validator import TransactionValidator
from src.common.constants import BatchFunction, ReturnCode, SequencerFunction
from src.common.error_handler import BatchError
from src.common.logging_config import configure_logging
from src.db.engine import init_db
from src.db.session import get_db_session

logger = logging.getLogger(__name__)


def run_full_cycle(process_date: str) -> ReturnCode:
    """
    Run the complete batch processing cycle.
    Replaces JCL job PORTBCH1 (main batch job).
    """
    logger.info("Starting full batch cycle for date %s", process_date)

    with get_db_session() as session:
        controller = BatchController(session)

        # Step 1: Initialize
        rc = controller.dispatch(BatchFunction.INIT, "FULLCYCL", process_date)
        if rc != ReturnCode.SUCCESS:
            return rc

        # Step 2: Check prerequisites
        rc = controller.dispatch(BatchFunction.CHECK, "FULLCYCL", process_date)
        if rc.value > ReturnCode.WARNING.value:
            return rc

        # Step 3: Update status to active
        controller.dispatch(BatchFunction.UPDATE, "FULLCYCL", process_date)

        # Step 4: Set up sequence
        sequencer = BatchSequencer(session)
        sequencer.add_step(ProcessStep("VALIDATE", "TRNVAL00", "Validate transactions"))
        sequencer.add_step(ProcessStep(
            "POSUPDATE", "POSUPD00", "Update positions",
            dependencies=["VALIDATE"], dep_hard_flags=[True], dep_max_rc=[4],
        ))
        sequencer.add_step(ProcessStep(
            "HISTLOAD", "HISTLD00", "Load history",
            dependencies=["POSUPDATE"], dep_hard_flags=[True], dep_max_rc=[4],
        ))
        sequencer.add_step(ProcessStep(
            "REPORTS", "RPTPOS00", "Generate reports",
            dependencies=["HISTLOAD"], dep_hard_flags=[False], dep_max_rc=[8],
        ))

        sequencer.dispatch(SequencerFunction.INIT)

        max_rc = ReturnCode.SUCCESS

        # Process each step
        while True:
            rc = sequencer.dispatch(SequencerFunction.NEXT)
            if rc != ReturnCode.SUCCESS:
                break

            step = sequencer.get_current_step()
            if step is None:
                break

            try:
                step_rc = _run_step(session, step.step_id, process_date, controller)
                sequencer.mark_step_complete(step.step_id, step_rc)
                if step_rc.value > max_rc.value:
                    max_rc = step_rc
            except Exception as e:
                logger.error("Step %s failed: %s", step.step_id, e)
                sequencer.mark_step_error(step.step_id)
                max_rc = ReturnCode.ERROR

        # Step 5: Terminate
        controller.dispatch(BatchFunction.TERMINATE, "FULLCYCL", process_date)

    logger.info("Full batch cycle complete. Return code: %s", max_rc.value)
    return max_rc


def _run_step(
    session, step_id: str, process_date: str, controller: BatchController | None = None,
) -> ReturnCode:
    """Run a single batch step and propagate counts to the controller."""
    logger.info("Running step: %s", step_id)

    match step_id:
        case "VALIDATE":
            rc, summary = run_validate(session)
            if controller is not None:
                controller.update_counts(
                    "FULLCYCL", process_date,
                    records_read=summary.get("records_validated", 0),
                    error_count=summary.get("records_rejected", 0),
                )
            return rc
        case "POSUPDATE":
            rc, summary = run_position_update(session, process_date)
            if controller is not None:
                controller.update_counts(
                    "FULLCYCL", process_date,
                    records_read=summary.get("records_processed", 0),
                    records_written=summary.get("records_updated", 0),
                    error_count=summary.get("error_count", 0),
                )
            return rc
        case "HISTLOAD":
            rc, summary = run_history_load(session, process_date)
            if controller is not None:
                controller.update_counts(
                    "FULLCYCL", process_date,
                    records_read=summary.get("records_read", 0),
                    records_written=summary.get("records_written", 0),
                    error_count=summary.get("error_count", 0),
                )
            return rc
        case "REPORTS":
            return run_reports(session, process_date)
        case _:
            raise BatchError(f"Unknown step: {step_id}", error_code="RN01")


def run_validate(session) -> tuple[ReturnCode, dict]:
    """Run transaction validation step."""
    from src.db.repository import TransactionRepository

    trn_repo = TransactionRepository(session)
    pending = trn_repo.list_pending()
    validator = TransactionValidator(session)
    rc = validator.validate_batch(pending)
    summary = validator.get_summary()
    logger.info("Validation: %s", summary)
    return rc, summary


def run_position_update(session, process_date: str) -> tuple[ReturnCode, dict]:
    """Run position update step."""
    from datetime import date as date_type

    pd = date_type(int(process_date[:4]), int(process_date[4:6]), int(process_date[6:8]))
    updater = PositionUpdater(session)
    rc = updater.process_pending_transactions(pd)
    summary = updater.get_summary()
    logger.info("Position update: %s", summary)
    return rc, summary


def run_history_load(session, process_date: str) -> tuple[ReturnCode, dict]:
    """Run history load step."""
    loader = HistoryLoader(session)
    rc = loader.load_transactions(process_date)
    summary = loader.get_summary()
    logger.info("History load: %s", summary)
    return rc, summary


def run_reports(session, process_date: str) -> ReturnCode:
    """Run report generation step."""
    reporting = BatchReporting(session)
    reporting.generate_position_report()
    reporting.generate_stats_report(process_date)
    return ReturnCode.SUCCESS


def run_recovery(process_date: str, mode: str = "A", job_name: str | None = None) -> ReturnCode:
    """Run recovery process."""
    with get_db_session() as session:
        recovery = ProcessRecovery(session)
        rc = recovery.recover(mode, process_date, job_name)
        summary = recovery.get_summary()
        logger.info("Recovery: %s", summary)
        return rc


def main() -> int:
    """CLI entry point replacing JCL job submission."""
    parser = argparse.ArgumentParser(description="Batch Processing Runner")
    parser.add_argument(
        "--process-date",
        default=date.today().strftime("%Y%m%d"),
        help="Processing date in YYYYMMDD format",
    )
    parser.add_argument(
        "--full-cycle",
        action="store_true",
        help="Run full batch cycle",
    )
    parser.add_argument(
        "--step",
        choices=["validate", "position-update", "history-load", "reports"],
        help="Run specific step",
    )
    parser.add_argument(
        "--recover",
        action="store_true",
        help="Run recovery for failed jobs",
    )
    parser.add_argument(
        "--recovery-mode",
        choices=["P", "S", "A"],
        default="A",
    )
    parser.add_argument("--job-name", help="Job name for recovery")
    parser.add_argument("--log-level", default="INFO")
    parser.add_argument("--init-db", action="store_true", help="Initialize database tables")

    args = parser.parse_args()
    configure_logging(args.log_level)

    if args.init_db:
        init_db()
        logger.info("Database initialized")
        return 0

    if args.full_cycle:
        rc = run_full_cycle(args.process_date)
        return rc.value

    if args.step:
        with get_db_session() as session:
            match args.step:
                case "validate":
                    rc, _ = run_validate(session)
                case "position-update":
                    rc, _ = run_position_update(session, args.process_date)
                case "history-load":
                    rc, _ = run_history_load(session, args.process_date)
                case "reports":
                    rc = run_reports(session, args.process_date)
                case _:
                    logger.error("Unknown step: %s", args.step)
                    return ReturnCode.ERROR.value
            return rc.value

    if args.recover:
        rc = run_recovery(args.process_date, args.recovery_mode, args.job_name)
        return rc.value

    parser.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
