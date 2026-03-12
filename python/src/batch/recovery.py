"""
Recovery processor translated from COBOL program RCVPRC00.cbl.

Replaces:
  - RCVPRC00.cbl 1000-INITIALIZE-RECOVERY: Load checkpoint data
  - RCVPRC00.cbl 2000-DETERMINE-RESTART: Find restart point
  - RCVPRC00.cbl 3000-EXECUTE-RECOVERY: Resume processing
  - RCVPRC00.cbl 4000-SAVE-CHECKPOINT: Save progress

Implements checkpoint/restart as database transactions with
idempotent processing to enable safe re-runs.
"""

import logging
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.constants import (
    COMMIT_THRESHOLD,
    CheckpointPhase,
    CheckpointStatus,
)
from src.db.repository import CheckpointRepository
from src.db.tables import Checkpoint

logger = logging.getLogger(__name__)


class RecoveryManager:
    """
    Checkpoint/restart recovery manager.

    Translates RCVPRC00.cbl paragraph structure.
    Enables batch jobs to resume from the last successful checkpoint
    after a failure, ensuring idempotent processing.
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._repo = CheckpointRepository(session)
        self._current_checkpoint: Checkpoint | None = None

    def initialize_recovery(self, batch_id: str) -> tuple[bool, str]:
        """
        Initialize recovery for a batch job.

        Translates RCVPRC00.cbl 1000-INITIALIZE-RECOVERY.

        Returns:
            Tuple of (needs_restart, restart_step_name).
            If needs_restart is True, restart_step_name contains the
            program/step name to restart from (stored in restart_data).
        """
        logger.info("Initializing recovery for batch: %s", batch_id)

        checkpoint = self._repo.get_latest_for_batch(batch_id)
        if checkpoint is None:
            logger.info("No checkpoint found, starting from beginning")
            return False, ""

        if checkpoint.status == CheckpointStatus.COMPLETE.value:
            logger.info("Last checkpoint was complete, starting fresh")
            return False, ""

        if checkpoint.status in (
            CheckpointStatus.ACTIVE.value,
            CheckpointStatus.FAILED.value,
        ):
            # restart_data stores the step/program name for sequencer restart
            restart_step = checkpoint.restart_data if checkpoint.restart_data else ""
            logger.info(
                "Found restart point: step=%s, key=%s, records=%d, phase=%s",
                restart_step,
                checkpoint.last_key,
                checkpoint.records_at_checkpoint,
                checkpoint.phase,
            )
            self._current_checkpoint = checkpoint
            return True, restart_step

        return False, ""

    def determine_restart_point(self, batch_id: str) -> str:
        """
        Determine the restart point for a failed batch.

        Translates RCVPRC00.cbl 2000-DETERMINE-RESTART.

        Returns:
            The last processed key, or empty string if no restart needed.
        """
        checkpoint = self._repo.get_latest_for_batch(batch_id)
        if checkpoint is None:
            return ""

        if checkpoint.status in (
            CheckpointStatus.ACTIVE.value,
            CheckpointStatus.FAILED.value,
        ):
            # Mark as restarted
            checkpoint.status = CheckpointStatus.RESTARTED.value
            self._repo.update(checkpoint)
            return checkpoint.last_key

        return ""

    def save_checkpoint(
        self,
        batch_id: str,
        last_key: str,
        records_processed: int,
        phase: CheckpointPhase = CheckpointPhase.PROCESS,
        total_amount: Decimal = Decimal("0.00"),
        current_step: str = "",
    ) -> Checkpoint:
        """
        Save a checkpoint during batch processing.

        Translates RCVPRC00.cbl 4000-SAVE-CHECKPOINT.
        Called periodically (every COMMIT_THRESHOLD records) to
        enable restart from the last checkpoint.
        """
        now = datetime.now()
        checkpoint_id = f"{batch_id}_{now.strftime('%H%M%S')}"

        checkpoint = Checkpoint(
            checkpoint_id=checkpoint_id,
            batch_id=batch_id,
            status=CheckpointStatus.ACTIVE.value,
            phase=phase.value,
            save_date=date.today(),
            save_time=now,
            last_key=last_key[:50],
            records_at_checkpoint=records_processed,
            commit_count=records_processed // COMMIT_THRESHOLD,
            restart_data=current_step[:200],
            total_amount=total_amount,
        )

        # Upsert: update existing or create new
        existing = self._repo.get_by_id(checkpoint_id)
        if existing is not None:
            existing.batch_id = checkpoint.batch_id
            existing.status = checkpoint.status
            existing.phase = checkpoint.phase
            existing.save_date = checkpoint.save_date
            existing.save_time = now
            existing.last_key = checkpoint.last_key
            existing.records_at_checkpoint = checkpoint.records_at_checkpoint
            existing.commit_count = checkpoint.commit_count
            existing.restart_data = checkpoint.restart_data
            existing.total_amount = checkpoint.total_amount
            self._repo.update(existing)
            result = existing
        else:
            result = self._repo.create(checkpoint)

        self._session.commit()
        logger.debug(
            "Checkpoint saved: id=%s, key=%s, records=%d",
            checkpoint_id,
            last_key,
            records_processed,
        )
        return result

    def complete_checkpoint(self, batch_id: str) -> None:
        """
        Mark the latest checkpoint as complete.

        Called when batch processing finishes successfully.
        """
        checkpoint = self._repo.get_latest_for_batch(batch_id)
        if checkpoint is not None:
            checkpoint.status = CheckpointStatus.COMPLETE.value
            checkpoint.phase = CheckpointPhase.TERMINATE.value
            self._repo.update(checkpoint)
            self._session.commit()
            logger.info("Checkpoint completed for batch: %s", batch_id)

    def fail_checkpoint(self, batch_id: str, error_msg: str = "") -> None:
        """
        Mark the latest checkpoint as failed.

        Called when batch processing encounters a fatal error.
        Preserves restart_data (step name) so that initialize_recovery
        can still determine which step to restart from.
        """
        checkpoint = self._repo.get_latest_for_batch(batch_id)
        if checkpoint is not None:
            checkpoint.status = CheckpointStatus.FAILED.value
            # Preserve both restart_data (step name) and last_key (record key)
            # so initialize_recovery can determine the correct restart point.
            # Error details are logged only — not stored in checkpoint fields.
            self._repo.update(checkpoint)
            self._session.commit()
            logger.error("Checkpoint failed for batch: %s - %s", batch_id, error_msg)
