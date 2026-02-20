"""Checkpoint/Restart Handler - migrated from CKPRST.cbl.

Manages checkpoint processing for batch programs including initialization,
checkpoint taking, committing, and restart processing.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.checkpoint import (
    CheckpointControl,
    CheckpointRecord,
    CheckpointStatus,
    CheckpointPhase,
    RestartMode,
)
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "CKPRST"


class CheckpointRestartHandler:
    def __init__(self):
        self._checkpoint_records: dict[str, CheckpointRecord] = {}
        self._file_path: Optional[str] = None

    def initialize(self, control: CheckpointControl, file_path: Optional[str] = None) -> int:
        self._file_path = file_path
        control.status = CheckpointStatus.ACTIVE
        if control.restart_mode != RestartMode.RESTART:
            control.run_date = datetime.now().strftime("%Y%m%d")
            control.run_time = datetime.now().strftime("%H%M%S")
        control.records_read = 0
        control.records_processed = 0
        control.records_error = 0
        control.phase = CheckpointPhase.INIT

        if file_path is not None:
            self._load_checkpoints(file_path)

        if control.restart_mode == RestartMode.RESTART:
            return self._handle_restart(control)

        logger.info("Checkpoint handler initialized for %s", control.program_id)
        return ReturnCode.SUCCESS

    def take_checkpoint(self, control: CheckpointControl) -> int:
        control.last_time = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")

        record = CheckpointRecord(
            program_id=control.program_id,
            run_date=control.run_date,
            data=self._serialize_control(control),
        )
        self._checkpoint_records[record.checkpoint_key] = record

        logger.debug(
            "Checkpoint taken for %s - Read: %d, Processed: %d, Errors: %d",
            control.program_id,
            control.records_read,
            control.records_processed,
            control.records_error,
        )
        return ReturnCode.SUCCESS

    def commit_checkpoint(self, control: CheckpointControl) -> int:
        if self._file_path is not None:
            return self._save_checkpoints(self._file_path)

        return ReturnCode.SUCCESS

    def restart(self, control: CheckpointControl) -> int:
        return self._handle_restart(control)

    def _handle_restart(self, control: CheckpointControl) -> int:
        key = f"{control.program_id}{control.run_date}"
        record = self._checkpoint_records.get(key)

        if record is None:
            logger.warning("No checkpoint found for %s, starting fresh", key)
            control.restart_mode = RestartMode.NORMAL
            return ReturnCode.WARNING

        self._deserialize_control(record.data, control)
        control.status = CheckpointStatus.RESTARTED
        control.restart_count += 1

        if control.restart_count > control.max_restarts:
            logger.error(
                "Max restarts exceeded for %s (%d > %d)",
                control.program_id,
                control.restart_count,
                control.max_restarts,
            )
            return ReturnCode.ERROR

        logger.info(
            "Restart from checkpoint for %s (attempt %d, last key: %s)",
            control.program_id,
            control.restart_count,
            control.last_key,
        )
        return ReturnCode.SUCCESS

    def _serialize_control(self, control: CheckpointControl) -> str:
        return (
            f"{control.records_read}|{control.records_processed}|"
            f"{control.records_error}|{control.last_key}|"
            f"{control.phase}|{control.restart_count}"
        )

    def _deserialize_control(self, data: str, control: CheckpointControl) -> None:
        parts = data.split("|")
        if len(parts) >= 6:
            control.records_read = int(parts[0])
            control.records_processed = int(parts[1])
            control.records_error = int(parts[2])
            control.last_key = parts[3]
            control.phase = parts[4]
            control.restart_count = int(parts[5])

    def _load_checkpoints(self, file_path: str) -> None:
        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|", 2)
                    if len(parts) >= 3:
                        record = CheckpointRecord(
                            program_id=parts[0],
                            run_date=parts[1],
                            data=parts[2],
                        )
                        self._checkpoint_records[record.checkpoint_key] = record
        except FileNotFoundError:
            pass
        except Exception as e:
            logger.error("Error loading checkpoints: %s", e)

    def _save_checkpoints(self, file_path: str) -> int:
        try:
            with open(file_path, "w") as f:
                for record in self._checkpoint_records.values():
                    f.write(f"{record.program_id}|{record.run_date}|{record.data}\n")
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving checkpoints: %s", e)
            return ReturnCode.ERROR
