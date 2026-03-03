"""Prefect batch orchestration flow.

Replaces:
  - z/OS JCL scheduler
  - BCHCTL00 (src/programs/batch/BCHCTL00.cbl) — batch control program
  - CKPRST checkpoint/restart logic

The batch pipeline runs sequentially with return-code gating:
  TRNVAL00 (RC ≤ 4) → POSUPD00 (RC ≤ 4) → HISTLD00 → Reporting

Uses Prefect for:
  - Flow orchestration (replaces JCL)
  - Task state management (replaces BCHCTL checkpoint/restart)
  - Error handling and retry (replaces CKPRST)
  - Logging and observability
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import date, datetime

from prefect import flow, task

from portfolio_manager.batch.history_loader import HistoryLoader, LoadResult
from portfolio_manager.batch.position_updater import PositionUpdater, UpdateResult
from portfolio_manager.batch.transaction_validator import (
    BatchResult,
    TransactionValidator,
)
from portfolio_manager.models.copybook_models import TransactionRecord
from portfolio_manager.services.database import get_session

logger = logging.getLogger(__name__)

# Return code gate: RC ≤ 4 means continue, RC > 4 means stop
RC_GATE_THRESHOLD = 4


@dataclass
class PipelineResult:
    """Result of the entire batch pipeline run.

    Replaces the batch control record (BCHCTL) statistics.
    """

    start_time: datetime = field(default_factory=datetime.now)
    end_time: datetime | None = None
    validation_result: BatchResult | None = None
    update_result: UpdateResult | None = None
    load_result: LoadResult | None = None
    overall_rc: int = 0
    status: str = "PENDING"
    error_messages: list[str] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Prefect Tasks (replace individual JCL steps)
# ---------------------------------------------------------------------------


@task(name="TRNVAL00-Transaction-Validation", retries=0)
def task_validate_transactions(
    transactions: list[TransactionRecord],
) -> tuple[BatchResult, list[TransactionRecord], list[TransactionRecord]]:
    """Validate transactions (Step 1).

    Replaces JCL step STEP01 / TRNVAL00.
    """
    with get_session() as session:
        validator = TransactionValidator(session)
        return validator.run(transactions)


@task(name="POSUPD00-Position-Update", retries=1)
def task_update_positions(
    transactions: list[TransactionRecord],
    process_date: date | None = None,
) -> UpdateResult:
    """Update positions (Step 2).

    Replaces JCL step STEP02 / POSUPD00.
    """
    with get_session() as session:
        updater = PositionUpdater(session)
        return updater.run(transactions, process_date)


@task(name="HISTLD00-History-Load", retries=1)
def task_load_history(
    transactions: list[TransactionRecord],
) -> LoadResult:
    """Load history records (Step 3).

    Replaces JCL step STEP03 / HISTLD00.
    """
    with get_session() as session:
        loader = HistoryLoader(session)
        return loader.run(transactions)


# ---------------------------------------------------------------------------
# Prefect Flow (replaces the JCL job / BCHCTL00 orchestration)
# ---------------------------------------------------------------------------


@flow(name="Portfolio-Batch-Pipeline", log_prints=True)
def batch_pipeline(
    transactions: list[TransactionRecord],
    process_date: date | None = None,
) -> PipelineResult:
    """Run the complete batch processing pipeline.

    Replaces the z/OS batch scheduler + BCHCTL00 control program.

    Pipeline:
      1. TRNVAL00 — Validate transactions
         Gate: RC ≤ 4 to continue
      2. POSUPD00 — Update positions
         Gate: RC ≤ 4 to continue
      3. HISTLD00 — Load history

    Args:
        transactions: Input transaction records.
        process_date: Processing date (defaults to today).

    Returns:
        PipelineResult with overall pipeline statistics.
    """
    result = PipelineResult()
    result.status = "ACTIVE"

    logger.info("Batch pipeline starting with %d transactions", len(transactions))

    # Step 1: TRNVAL00 — Transaction Validation
    try:
        validation_result, valid_records, invalid_records = (
            task_validate_transactions(transactions)
        )
        result.validation_result = validation_result

        logger.info(
            "TRNVAL00 complete: RC=%d (valid=%d, invalid=%d)",
            validation_result.return_code,
            validation_result.records_valid,
            validation_result.records_invalid,
        )

        # Return-code gating (RC ≤ 4 = continue)
        if validation_result.return_code > RC_GATE_THRESHOLD:
            result.overall_rc = validation_result.return_code
            result.status = "FAILED"
            result.error_messages.append(
                f"TRNVAL00 failed with RC={validation_result.return_code}, pipeline stopped"
            )
            result.end_time = datetime.now()
            return result

    except Exception as exc:
        result.overall_rc = 12
        result.status = "FAILED"
        result.error_messages.append(f"TRNVAL00 exception: {exc}")
        result.end_time = datetime.now()
        return result

    # Step 2: POSUPD00 — Position Update
    try:
        update_result = task_update_positions(valid_records, process_date)
        result.update_result = update_result

        logger.info(
            "POSUPD00 complete: RC=%d (updated=%d, created=%d)",
            update_result.return_code,
            update_result.positions_updated,
            update_result.positions_created,
        )

        if update_result.return_code > RC_GATE_THRESHOLD:
            result.overall_rc = update_result.return_code
            result.status = "FAILED"
            result.error_messages.append(
                f"POSUPD00 failed with RC={update_result.return_code}, pipeline stopped"
            )
            result.end_time = datetime.now()
            return result

    except Exception as exc:
        result.overall_rc = 12
        result.status = "FAILED"
        result.error_messages.append(f"POSUPD00 exception: {exc}")
        result.end_time = datetime.now()
        return result

    # Step 3: HISTLD00 — History Load
    try:
        load_result = task_load_history(valid_records)
        result.load_result = load_result

        logger.info(
            "HISTLD00 complete: RC=%d (inserted=%d, dups=%d)",
            load_result.return_code,
            load_result.records_inserted,
            load_result.records_duplicate,
        )

        result.overall_rc = max(
            validation_result.return_code,
            update_result.return_code,
            load_result.return_code,
        )

    except Exception as exc:
        result.overall_rc = 12
        result.status = "FAILED"
        result.error_messages.append(f"HISTLD00 exception: {exc}")
        result.end_time = datetime.now()
        return result

    # Pipeline complete
    result.status = "COMPLETE" if result.overall_rc <= RC_GATE_THRESHOLD else "FAILED"
    result.end_time = datetime.now()

    logger.info(
        "Batch pipeline complete: status=%s overall_rc=%d",
        result.status,
        result.overall_rc,
    )

    return result
