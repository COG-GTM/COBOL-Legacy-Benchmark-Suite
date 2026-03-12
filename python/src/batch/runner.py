"""
Batch runner replacing JCL job definitions from src/jcl/.

Provides a command-line interface to orchestrate the full batch cycle
or run specific batch steps. Can be scheduled via cron or APScheduler.

Usage:
    python -m src.batch.runner --full-cycle --date 2024-01-15
    python -m src.batch.runner --step validate --date 2024-01-15
    python -m src.batch.runner --step position-update --date 2024-01-15
"""

import argparse
import logging
import sys
from datetime import date, datetime

from src.batch.controller import BatchController
from src.batch.history_loader import HistoryLoader
from src.batch.position_updater import PositionUpdater
from src.batch.recovery import RecoveryManager
from src.batch.reporting import BatchReporting
from src.batch.sequencer import BatchSequencer
from src.batch.validator import TransactionValidator
from src.common.constants import (
    BatchProcessType,
    DependencyType,
    ProcessFrequency,
    ReturnCode,
    TransactionStatus,
)
from src.common.logging_config import configure_logging
from src.db.engine import create_db_engine
from src.db.session import session_scope
from src.db.tables import Base
from src.models.batch_control import BatchParameters, ProcessSequenceRecord

logger = logging.getLogger(__name__)


def run_full_cycle(process_date: date, restart: bool = False) -> ReturnCode:
    """
    Run the full batch processing cycle.

    Translates the JCL batch job sequence:
      Step 1: Validate transactions (TRNVAL00)
      Step 2: Update positions (POSUPD00)
      Step 3: Load history (HISTLD00)
      Step 4: Generate reports (RPTGEN)
    """
    batch_id = f"BCH{process_date.strftime('%m%d')}"
    params = BatchParameters(
        batch_id=batch_id,
        process_date=process_date,
        process_type=BatchProcessType.INITIAL,
        restart_flag=restart,
    )

    with session_scope() as session:
        controller = BatchController(session)
        recovery = RecoveryManager(session)

        # Initialize
        controller.initialize(params)

        # Check for restart (returns step/program name, not record key)
        needs_restart, restart_step_name = recovery.initialize_recovery(batch_id)

        # Build sequence
        sequencer = BatchSequencer()
        steps = [
            ProcessSequenceRecord(
                sequence_id="DAILY",
                step_number=1,
                program_name="TRNVAL00",
                description="Validate transactions",
                dependency_type=DependencyType.REQUIRED,
                frequency=ProcessFrequency.DAILY,
            ),
            ProcessSequenceRecord(
                sequence_id="DAILY",
                step_number=2,
                program_name="POSUPD00",
                description="Update positions",
                dependency_type=DependencyType.REQUIRED,
                frequency=ProcessFrequency.DAILY,
            ),
            ProcessSequenceRecord(
                sequence_id="DAILY",
                step_number=3,
                program_name="HISTLD00",
                description="Load history",
                dependency_type=DependencyType.REQUIRED,
                frequency=ProcessFrequency.DAILY,
            ),
            ProcessSequenceRecord(
                sequence_id="DAILY",
                step_number=4,
                program_name="RPTGEN",
                description="Generate reports",
                dependency_type=DependencyType.OPTIONAL,
                frequency=ProcessFrequency.DAILY,
            ),
        ]
        sequencer.load_sequence(steps)

        # Register step handlers
        def validate_step() -> ReturnCode:
            validator = TransactionValidator(session)
            # Validate all pending transactions
            from src.db.repository import TransactionRepository
            txn_repo = TransactionRepository(session)
            pending = txn_repo.list_pending()
            for txn in pending:
                from src.models.transaction import TransactionRecord
                try:
                    record = TransactionRecord(
                        transaction_id=txn.transaction_id,
                        portfolio_id=txn.portfolio_id,
                        trn_type=txn.trn_type,
                        quantity=txn.quantity,
                        price=txn.price,
                        amount=txn.amount,
                        investment_id=txn.investment_id,
                    )
                except Exception as exc:
                    txn.status = TransactionStatus.FAILED.value
                    txn_repo.update(txn)
                    controller.increment_read()
                    controller.increment_error(str(exc))
                    continue
                result = validator.validate(record)
                controller.increment_read()
                if result.is_valid:
                    controller.increment_processed()
                else:
                    txn.status = TransactionStatus.FAILED.value
                    txn_repo.update(txn)
                    controller.increment_error("; ".join(result.errors))
            logger.info(
                "Validation: %d passed, %d failed",
                validator.total_passed,
                validator.total_failed,
            )
            if validator.total_failed > 0:
                return ReturnCode.WARNING
            return ReturnCode.SUCCESS

        def position_update_step() -> ReturnCode:
            updater = PositionUpdater(session)
            return updater.process(process_date)

        def history_load_step() -> ReturnCode:
            loader = HistoryLoader(session)
            return loader.process(process_date)

        def report_step() -> ReturnCode:
            reporting = BatchReporting(session)
            reporting.generate_position_report(process_date)
            reporting.generate_stats_report()
            return ReturnCode.SUCCESS

        sequencer.register_step("TRNVAL00", validate_step)
        sequencer.register_step("POSUPD00", position_update_step)
        sequencer.register_step("HISTLD00", history_load_step)
        sequencer.register_step("RPTGEN", report_step)

        # Execute sequence
        restart_from = restart_step_name if needs_restart else ""
        rc = sequencer.execute(process_date, restart_step=restart_from)

        # Save final checkpoint
        if rc <= ReturnCode.WARNING:
            recovery.complete_checkpoint(batch_id)
        else:
            recovery.fail_checkpoint(batch_id, f"Batch failed with RC={rc}")

        # Terminate
        controller.terminate(params)

    return rc


def run_single_step(step_name: str, process_date: date) -> ReturnCode:
    """Run a single batch step."""
    with session_scope() as session:
        match step_name.lower():
            case "validate":
                validator = TransactionValidator(session)
                from src.db.repository import TransactionRepository
                txn_repo = TransactionRepository(session)
                pending = txn_repo.list_pending()
                for txn in pending:
                    from src.models.transaction import TransactionRecord
                    try:
                        record = TransactionRecord(
                            transaction_id=txn.transaction_id,
                            portfolio_id=txn.portfolio_id,
                            trn_type=txn.trn_type,
                            quantity=txn.quantity,
                            price=txn.price,
                            amount=txn.amount,
                            investment_id=txn.investment_id,
                        )
                    except Exception:
                        txn.status = TransactionStatus.FAILED.value
                        txn_repo.update(txn)
                        validator.total_failed += 1
                        continue
                    result = validator.validate(record)
                    if not result.is_valid:
                        txn.status = TransactionStatus.FAILED.value
                        txn_repo.update(txn)
                return ReturnCode.SUCCESS if validator.total_failed == 0 else ReturnCode.WARNING

            case "position-update":
                updater = PositionUpdater(session)
                return updater.process(process_date)

            case "history-load":
                loader = HistoryLoader(session)
                return loader.process(process_date)

            case "reports":
                reporting = BatchReporting(session)
                reporting.generate_position_report(process_date)
                reporting.generate_audit_report(
                    datetime.combine(process_date, datetime.min.time()),
                    datetime.combine(process_date, datetime.max.time()),
                )
                reporting.generate_stats_report()
                return ReturnCode.SUCCESS

            case _:
                logger.error("Unknown step: %s", step_name)
                return ReturnCode.ERROR


def main() -> None:
    """CLI entry point for batch runner."""
    parser = argparse.ArgumentParser(
        description="Investment Portfolio Batch Runner"
    )
    parser.add_argument(
        "--full-cycle",
        action="store_true",
        help="Run the full batch processing cycle",
    )
    parser.add_argument(
        "--step",
        choices=["validate", "position-update", "history-load", "reports"],
        help="Run a specific batch step",
    )
    parser.add_argument(
        "--date",
        type=lambda s: date.fromisoformat(s),
        default=date.today(),
        help="Processing date (YYYY-MM-DD, default: today)",
    )
    parser.add_argument(
        "--restart",
        action="store_true",
        help="Restart from last checkpoint",
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="Enable debug logging",
    )

    args = parser.parse_args()

    configure_logging(level="DEBUG" if args.debug else "INFO")

    if not args.full_cycle and not args.step:
        parser.error("Either --full-cycle or --step is required")

    # Ensure database tables exist
    engine = create_db_engine()
    Base.metadata.create_all(engine)

    if args.full_cycle:
        rc = run_full_cycle(args.date, restart=args.restart)
    else:
        rc = run_single_step(args.step, args.date)

    logger.info("Batch completed with return code: %d", rc)
    sys.exit(rc)


if __name__ == "__main__":
    main()
