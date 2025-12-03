"""
Base Batch Processor

Provides common functionality for all batch processing programs.
Implements the standard COBOL batch processing pattern:
- 0000-MAIN
- 1000-INITIALIZE
- 2000-PROCESS
- 3000-TERMINATE
"""

import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Any, Optional

from ..checkpoint.manager import CheckpointManager
from ..checkpoint.storage import CheckpointStorage
from ..models.batch_control import ReturnCode

logger = logging.getLogger(__name__)


class ProcessingPhase(Enum):
    """Processing phases matching COBOL paragraph structure."""
    INITIALIZE = "INIT"
    PROCESS = "PROC"
    TERMINATE = "TERM"


@dataclass
class ProcessingResult:
    """
    Result of batch processing.
    
    Corresponds to return codes and statistics from COBOL programs.
    """
    return_code: int = 0
    records_read: int = 0
    records_processed: int = 0
    records_written: int = 0
    records_error: int = 0
    error_messages: list = field(default_factory=list)
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    
    @property
    def is_success(self) -> bool:
        return self.return_code == ReturnCode.SUCCESS.value
    
    @property
    def is_warning(self) -> bool:
        return self.return_code == ReturnCode.WARNING.value
    
    @property
    def is_error(self) -> bool:
        return self.return_code >= ReturnCode.ERROR.value
    
    @property
    def duration_seconds(self) -> float:
        if self.start_time and self.end_time:
            return (self.end_time - self.start_time).total_seconds()
        return 0.0
    
    def add_error(self, message: str) -> None:
        self.error_messages.append(message)
        self.records_error += 1


class BatchProcessor(ABC):
    """
    Abstract base class for batch processors.
    
    Implements the standard COBOL batch processing pattern with:
    - Checkpoint/restart support
    - Error handling
    - Statistics tracking
    - Logging
    """
    
    PROGRAM_ID: str = "BATCH00"
    COMMIT_FREQ: int = 1000
    MAX_ERRORS: int = 100
    MAX_RESTARTS: int = 3
    
    def __init__(
        self,
        checkpoint_storage: Optional[CheckpointStorage] = None,
        restart: bool = False,
    ):
        """
        Initialize batch processor.
        
        Args:
            checkpoint_storage: Storage backend for checkpoints
            restart: If True, attempt to restart from last checkpoint
        """
        self.checkpoint_manager = CheckpointManager(
            program_id=self.PROGRAM_ID,
            storage=checkpoint_storage,
            commit_freq=self.COMMIT_FREQ,
            max_errors=self.MAX_ERRORS,
            max_restarts=self.MAX_RESTARTS,
        )
        self.restart = restart
        self.result = ProcessingResult()
        self.phase = ProcessingPhase.INITIALIZE
        self._initialized = False
        self._terminated = False
    
    def run(self) -> ProcessingResult:
        """
        Main entry point for batch processing.
        
        Corresponds to 0000-MAIN in COBOL programs.
        
        Returns:
            ProcessingResult with statistics and return code
        """
        logger.info(f"Starting {self.PROGRAM_ID}")
        self.result.start_time = datetime.now()
        
        try:
            self._initialize()
            self._process()
            self._terminate()
        except Exception as e:
            logger.exception(f"Fatal error in {self.PROGRAM_ID}")
            self.result.return_code = ReturnCode.CRITICAL.value
            self.result.add_error(str(e))
            self.checkpoint_manager.fail_processing(str(e))
        finally:
            self.result.end_time = datetime.now()
            self._display_statistics()
        
        return self.result
    
    def _initialize(self) -> None:
        """
        Initialize processing.
        
        Corresponds to 1000-INITIALIZE in COBOL programs.
        """
        logger.info(f"Initializing {self.PROGRAM_ID}")
        self.phase = ProcessingPhase.INITIALIZE
        
        self.checkpoint_manager.initialize(restart=self.restart)
        self.checkpoint_manager.start_processing()
        
        self.initialize()
        self._initialized = True
    
    def _process(self) -> None:
        """
        Main processing loop.
        
        Corresponds to 2000-PROCESS in COBOL programs.
        """
        logger.info(f"Processing {self.PROGRAM_ID}")
        self.phase = ProcessingPhase.PROCESS
        
        restart_key = self.checkpoint_manager.get_restart_key()
        if restart_key:
            logger.info(f"Restarting from key: {restart_key}")
            self.skip_to_key(restart_key)
        
        while True:
            record = self.read_next_record()
            if record is None:
                break
            
            self.checkpoint_manager.record_read()
            self.result.records_read += 1
            
            try:
                processed = self.process_record(record)
                if processed:
                    self.result.records_processed += 1
                    record_key = self.get_record_key(record)
                    self.checkpoint_manager.record_processed(record_key)
            except Exception as e:
                logger.warning(f"Error processing record: {e}")
                self.result.add_error(str(e))
                if not self.checkpoint_manager.record_error():
                    logger.error("Maximum errors exceeded, aborting")
                    self.result.return_code = ReturnCode.ERROR.value
                    break
    
    def _terminate(self) -> None:
        """
        Terminate processing.
        
        Corresponds to 3000-TERMINATE in COBOL programs.
        """
        logger.info(f"Terminating {self.PROGRAM_ID}")
        self.phase = ProcessingPhase.TERMINATE
        
        self.terminate()
        
        self.checkpoint_manager.commit_checkpoint()
        self.checkpoint_manager.complete_processing()
        
        if self.result.records_error > 0:
            self.result.return_code = max(
                self.result.return_code, ReturnCode.WARNING.value
            )
        
        self._terminated = True
    
    def _display_statistics(self) -> None:
        """
        Display processing statistics.
        
        Corresponds to DISPLAY statements in COBOL programs.
        """
        print(f"\n{self.PROGRAM_ID} Processing Statistics:")
        print(f"  Records Read:      {self.result.records_read}")
        print(f"  Records Processed: {self.result.records_processed}")
        print(f"  Records Written:   {self.result.records_written}")
        print(f"  Records Error:     {self.result.records_error}")
        print(f"  Return Code:       {self.result.return_code}")
        print(f"  Duration:          {self.result.duration_seconds:.2f} seconds")
        
        if self.result.error_messages:
            print(f"\n  Errors:")
            for i, msg in enumerate(self.result.error_messages[:10], 1):
                print(f"    {i}. {msg}")
            if len(self.result.error_messages) > 10:
                print(f"    ... and {len(self.result.error_messages) - 10} more")
    
    @abstractmethod
    def initialize(self) -> None:
        """
        Program-specific initialization.
        
        Override to implement file opening, database connections, etc.
        """
        pass
    
    @abstractmethod
    def read_next_record(self) -> Optional[Any]:
        """
        Read the next record for processing.
        
        Returns:
            Next record or None if end of input
        """
        pass
    
    @abstractmethod
    def process_record(self, record: Any) -> bool:
        """
        Process a single record.
        
        Args:
            record: Record to process
            
        Returns:
            True if record was processed successfully
        """
        pass
    
    @abstractmethod
    def get_record_key(self, record: Any) -> str:
        """
        Get the key for a record (for checkpoint tracking).
        
        Args:
            record: Record to get key from
            
        Returns:
            String representation of record key
        """
        pass
    
    @abstractmethod
    def terminate(self) -> None:
        """
        Program-specific termination.
        
        Override to implement file closing, final commits, etc.
        """
        pass
    
    def skip_to_key(self, key: str) -> None:
        """
        Skip to a specific key for restart processing.
        
        Override if restart requires special handling.
        
        Args:
            key: Key to skip to
        """
        pass
