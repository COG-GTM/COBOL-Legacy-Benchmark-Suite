"""
Checkpoint Manager

Provides checkpoint/restart functionality for batch programs.
Corresponds to COBOL program CKPRST.cbl and copybook CKPRST.cpy.
"""

import logging
from datetime import datetime
from typing import Callable, Optional, TypeVar

from ..models.checkpoint import (
    CheckpointControl,
    CheckpointPhase,
    CheckpointStatus,
    RestartMode,
)
from .storage import CheckpointStorage, FileCheckpointStorage

logger = logging.getLogger(__name__)

T = TypeVar("T")


class CheckpointManager:
    """
    Checkpoint manager for batch processing.
    
    Implements the checkpoint/restart pattern from COBOL CKPRST program:
    - PROC-INIT: Initialize checkpoint processing
    - PROC-TAKE-CHECKPOINT: Take a checkpoint
    - PROC-COMMIT-CHECKPOINT: Commit checkpoint
    - PROC-RESTART: Handle restart processing
    """
    
    def __init__(
        self,
        program_id: str,
        storage: Optional[CheckpointStorage] = None,
        commit_freq: int = 1000,
        max_errors: int = 100,
        max_restarts: int = 3,
    ):
        """
        Initialize checkpoint manager.
        
        Args:
            program_id: Program identifier (8 characters max)
            storage: Checkpoint storage backend
            commit_freq: Checkpoint frequency (records between checkpoints)
            max_errors: Maximum allowed errors before abort
            max_restarts: Maximum restart attempts
        """
        self.program_id = program_id[:8].ljust(8)
        self.storage = storage or FileCheckpointStorage()
        self.commit_freq = commit_freq
        self.max_errors = max_errors
        self.max_restarts = max_restarts
        self.checkpoint: Optional[CheckpointControl] = None
        self._commit_callback: Optional[Callable[[], None]] = None
        self._rollback_callback: Optional[Callable[[], None]] = None
    
    def set_commit_callback(self, callback: Callable[[], None]) -> None:
        """Set callback to be called on checkpoint commit."""
        self._commit_callback = callback
    
    def set_rollback_callback(self, callback: Callable[[], None]) -> None:
        """Set callback to be called on checkpoint rollback."""
        self._rollback_callback = callback
    
    def initialize(self, restart: bool = False) -> CheckpointControl:
        """
        Initialize checkpoint processing.
        
        Corresponds to PROC-INIT in CKPRST.cbl.
        
        Args:
            restart: If True, attempt to restart from last checkpoint
            
        Returns:
            CheckpointControl structure
        """
        logger.info(f"Initializing checkpoint for program {self.program_id}")
        
        if restart:
            existing = self.storage.get_latest(self.program_id)
            if existing and not existing.is_complete():
                logger.info(f"Found existing checkpoint, preparing restart")
                if not existing.can_restart():
                    raise RuntimeError(
                        f"Maximum restarts ({self.max_restarts}) exceeded"
                    )
                existing.prepare_restart()
                existing.increment_restart()
                self.checkpoint = existing
                self.storage.save(self.checkpoint)
                return self.checkpoint
        
        self.checkpoint = CheckpointControl.create_new(
            program_id=self.program_id,
            commit_freq=self.commit_freq,
            max_errors=self.max_errors,
            max_restarts=self.max_restarts,
        )
        self.storage.save(self.checkpoint)
        logger.info(f"Created new checkpoint: {self.checkpoint.run_date} {self.checkpoint.run_time}")
        return self.checkpoint
    
    def start_processing(self) -> None:
        """
        Mark checkpoint as active and start processing.
        
        Should be called after initialization and before processing records.
        """
        if self.checkpoint is None:
            raise RuntimeError("Checkpoint not initialized")
        
        self.checkpoint.start_processing()
        self.storage.save(self.checkpoint)
        logger.info("Checkpoint processing started")
    
    def take_checkpoint(self, last_key: str) -> None:
        """
        Take a checkpoint.
        
        Corresponds to PROC-TAKE-CHECKPOINT in CKPRST.cbl.
        
        Args:
            last_key: Last processed record key
        """
        if self.checkpoint is None:
            raise RuntimeError("Checkpoint not initialized")
        
        self.checkpoint.take_checkpoint(last_key)
        self.storage.save(self.checkpoint)
        logger.debug(
            f"Checkpoint taken: records_processed={self.checkpoint.records_processed}, "
            f"last_key={last_key.strip()}"
        )
    
    def commit_checkpoint(self) -> None:
        """
        Commit checkpoint.
        
        Corresponds to PROC-COMMIT-CHECKPOINT in CKPRST.cbl.
        Calls the commit callback if set.
        """
        if self.checkpoint is None:
            raise RuntimeError("Checkpoint not initialized")
        
        if self._commit_callback:
            self._commit_callback()
        
        self.storage.save(self.checkpoint)
        logger.info(
            f"Checkpoint committed: records_processed={self.checkpoint.records_processed}"
        )
    
    def rollback_checkpoint(self) -> None:
        """
        Rollback to last checkpoint.
        
        Calls the rollback callback if set.
        """
        if self.checkpoint is None:
            raise RuntimeError("Checkpoint not initialized")
        
        if self._rollback_callback:
            self._rollback_callback()
        
        logger.warning("Checkpoint rolled back")
    
    def complete_processing(self) -> None:
        """
        Mark processing as complete.
        
        Should be called after all records have been processed successfully.
        """
        if self.checkpoint is None:
            raise RuntimeError("Checkpoint not initialized")
        
        self.checkpoint.complete_processing()
        self.storage.save(self.checkpoint)
        logger.info(
            f"Processing complete: "
            f"records_read={self.checkpoint.records_read}, "
            f"records_processed={self.checkpoint.records_processed}, "
            f"records_error={self.checkpoint.records_error}"
        )
    
    def fail_processing(self, error_message: str = "") -> None:
        """
        Mark processing as failed.
        
        Args:
            error_message: Error description
        """
        if self.checkpoint is None:
            raise RuntimeError("Checkpoint not initialized")
        
        self.checkpoint.fail_processing()
        self.storage.save(self.checkpoint)
        logger.error(f"Processing failed: {error_message}")
    
    def record_read(self) -> None:
        """Increment records read counter."""
        if self.checkpoint:
            self.checkpoint.increment_read()
    
    def record_processed(self, last_key: str = "") -> bool:
        """
        Increment records processed counter and check if checkpoint needed.
        
        Args:
            last_key: Last processed record key
            
        Returns:
            True if checkpoint was taken, False otherwise
        """
        if self.checkpoint is None:
            return False
        
        self.checkpoint.increment_processed()
        
        if self.checkpoint.should_checkpoint():
            self.take_checkpoint(last_key)
            self.commit_checkpoint()
            return True
        
        return False
    
    def record_error(self) -> bool:
        """
        Increment error counter and check if max errors exceeded.
        
        Returns:
            True if processing should continue, False if max errors exceeded
        """
        if self.checkpoint is None:
            return True
        
        self.checkpoint.increment_error()
        
        if self.checkpoint.has_exceeded_errors():
            logger.error(
                f"Maximum errors ({self.max_errors}) exceeded, "
                f"current errors: {self.checkpoint.records_error}"
            )
            return False
        
        return True
    
    def get_restart_key(self) -> Optional[str]:
        """
        Get the last key from restart checkpoint.
        
        Returns:
            Last processed key if in restart mode, None otherwise
        """
        if self.checkpoint and self.checkpoint.is_restart_mode():
            return self.checkpoint.last_key.strip() or None
        return None
    
    def is_restart_mode(self) -> bool:
        """Check if in restart mode."""
        return self.checkpoint is not None and self.checkpoint.is_restart_mode()
    
    def get_statistics(self) -> dict:
        """
        Get processing statistics.
        
        Returns:
            Dictionary with processing statistics
        """
        if self.checkpoint is None:
            return {}
        
        return {
            "program_id": self.checkpoint.program_id.strip(),
            "run_date": self.checkpoint.run_date.strip(),
            "run_time": self.checkpoint.run_time.strip(),
            "status": self.checkpoint.status.value,
            "phase": self.checkpoint.phase.value,
            "records_read": self.checkpoint.records_read,
            "records_processed": self.checkpoint.records_processed,
            "records_error": self.checkpoint.records_error,
            "restart_count": self.checkpoint.counters.restart_count,
            "last_key": self.checkpoint.last_key.strip(),
        }
    
    def display_statistics(self) -> None:
        """Display processing statistics (corresponds to DISPLAY statements in COBOL)."""
        stats = self.get_statistics()
        if not stats:
            return
        
        print(f"{stats['program_id']} Processing Statistics:")
        print(f"  Run Date:          {stats['run_date']}")
        print(f"  Run Time:          {stats['run_time']}")
        print(f"  Status:            {stats['status']}")
        print(f"  Records Read:      {stats['records_read']}")
        print(f"  Records Processed: {stats['records_processed']}")
        print(f"  Records Error:     {stats['records_error']}")
        print(f"  Restart Count:     {stats['restart_count']}")
