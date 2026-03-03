"""Recovery Process module - replaces RCVPRC00.cbl.

Handles recovery processing for failed batch jobs with three modes
and three recovery actions.

COBOL program flow:
- Modes (LS-RCV-MODE): P=Process, S=Step, A=All
- Actions (LS-RCV-ACTION): R=Restart, B=Bypass, T=Terminate
- Functions: INIT, RCVR, STAT, TERM
"""

import logging
from datetime import datetime
from enum import StrEnum
from typing import Any

from python_app.batch.prcseq00 import ProcessSequenceManager, ProcessStatus

logger = logging.getLogger("portfolio.batch.rcvprc00")


class RecoveryMode(StrEnum):
    """Recovery mode codes matching COBOL LS-RCV-MODE."""

    PROCESS = "P"  # Recover single process
    STEP = "S"  # Recover from specific step
    ALL = "A"  # Recover all failed processes


class RecoveryAction(StrEnum):
    """Recovery action codes matching COBOL LS-RCV-ACTION."""

    RESTART = "R"  # Restart the failed process
    BYPASS = "B"  # Bypass and continue with next
    TERMINATE = "T"  # Terminate the pipeline


class RecoveryResult:
    """Result of a recovery operation."""

    def __init__(
        self,
        process_name: str,
        action: RecoveryAction,
        success: bool,
        message: str = "",
    ) -> None:
        self.process_name = process_name
        self.action = action
        self.success = success
        self.message = message
        self.timestamp = datetime.now().isoformat()


class RecoveryProcessor:
    """Recovery processor replacing RCVPRC00.cbl.

    Handles restart, bypass, and terminate actions for failed
    batch processes. Works with ProcessSequenceManager to update
    process states after recovery.
    """

    MAX_RESTARTS = 3  # WS-MAX-RESTART-ATTEMPTS from RCVPRC00.cbl

    def __init__(self, sequence_manager: ProcessSequenceManager) -> None:
        self.sequence_manager = sequence_manager
        self.recovery_log: list[RecoveryResult] = []
        self.restart_counts: dict[str, int] = {}

    def initialize(self) -> None:
        """Initialize recovery processor - replaces P100-INITIALIZE."""
        self.recovery_log.clear()
        self.restart_counts.clear()
        logger.info("RCVPRC00 INIT: Recovery processor initialized")

    def recover(
        self,
        process_name: str,
        mode: RecoveryMode,
        action: RecoveryAction,
    ) -> list[RecoveryResult]:
        """Execute recovery - replaces P200-RECOVER.

        COBOL EVALUATE LS-RCV-MODE:
        - P: Recover single process (P210-RECOVER-PROCESS)
        - S: Recover from step (P220-RECOVER-STEP)
        - A: Recover all failed (P230-RECOVER-ALL)
        """
        results: list[RecoveryResult] = []

        if mode == RecoveryMode.PROCESS:
            result = self._recover_process(process_name, action)
            results.append(result)

        elif mode == RecoveryMode.STEP:
            # Recover from the specified step onwards
            found = False
            for name in self.sequence_manager.sequence_order:
                if name == process_name:
                    found = True
                if found:
                    entry = self.sequence_manager.processes.get(name)
                    if entry and entry.status in (ProcessStatus.ERROR, ProcessStatus.PENDING):
                        result = self._recover_process(name, action)
                        results.append(result)

        elif mode == RecoveryMode.ALL:
            # Recover all failed processes
            for name in self.sequence_manager.sequence_order:
                entry = self.sequence_manager.processes.get(name)
                if entry and entry.status == ProcessStatus.ERROR:
                    result = self._recover_process(name, action)
                    results.append(result)

        self.recovery_log.extend(results)
        return results

    def _recover_process(
        self,
        process_name: str,
        action: RecoveryAction,
    ) -> RecoveryResult:
        """Recover a single process - replaces P210-RECOVER-PROCESS.

        Applies the specified action:
        - RESTART: Reset to PENDING if under max restart count
        - BYPASS: Set to BYPASSED, allow pipeline to continue
        - TERMINATE: Set to ERROR, stop pipeline
        """
        if action == RecoveryAction.RESTART:
            restart_count = self.restart_counts.get(process_name, 0)
            if restart_count >= self.MAX_RESTARTS:
                logger.warning(
                    "RCVPRC00: Max restarts (%d) reached for %s",
                    self.MAX_RESTARTS, process_name,
                )
                return RecoveryResult(
                    process_name, action, False,
                    f"Max restarts ({self.MAX_RESTARTS}) exceeded",
                )

            self.restart_counts[process_name] = restart_count + 1
            self.sequence_manager.update_status(
                process_name, ProcessStatus.PENDING, return_code=0,
            )
            logger.info(
                "RCVPRC00: Process %s reset for restart (attempt %d/%d)",
                process_name, restart_count + 1, self.MAX_RESTARTS,
            )
            return RecoveryResult(
                process_name, action, True,
                f"Reset for restart (attempt {restart_count + 1}/{self.MAX_RESTARTS})",
            )

        elif action == RecoveryAction.BYPASS:
            self.sequence_manager.update_status(
                process_name, ProcessStatus.BYPASSED, return_code=0,
            )
            logger.info("RCVPRC00: Process %s bypassed", process_name)
            return RecoveryResult(process_name, action, True, "Process bypassed")

        elif action == RecoveryAction.TERMINATE:
            self.sequence_manager.update_status(
                process_name, ProcessStatus.ERROR, return_code=12,
                error_desc="Terminated by recovery process",
            )
            logger.info("RCVPRC00: Process %s terminated", process_name)
            return RecoveryResult(process_name, action, True, "Process terminated")

        return RecoveryResult(process_name, action, False, f"Unknown action: {action}")

    def get_status(self) -> dict[str, Any]:
        """Get recovery status - replaces P300-GET-STATUS."""
        return {
            "total_recoveries": len(self.recovery_log),
            "restart_counts": dict(self.restart_counts),
            "recent": [
                {
                    "process": r.process_name,
                    "action": r.action,
                    "success": r.success,
                    "message": r.message,
                    "timestamp": r.timestamp,
                }
                for r in self.recovery_log[-10:]
            ],
        }

    def terminate(self) -> dict[str, Any]:
        """Terminate recovery processor - replaces P400-TERMINATE."""
        status = self.get_status()
        logger.info(
            "RCVPRC00 TERM: %d total recoveries processed",
            len(self.recovery_log),
        )
        return status
