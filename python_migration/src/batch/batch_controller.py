"""
Batch Controller - Migrated from COBOL batch control framework.

This module implements the batch job control and checkpoint/restart
functionality from the original COBOL framework.

Original COBOL Programs: BCHCTL, CHKPNT (referenced in architecture)
"""

import logging
import json
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Dict, Optional, Callable, Any
from enum import Enum

from sqlalchemy.orm import Session

from ..models.batch_control import BatchControlRecord, BatchControl, BatchStatus
from ..database.connection import session_scope, get_session

logger = logging.getLogger(__name__)


@dataclass
class CheckpointData:
    """Checkpoint data for restart capability"""
    job_name: str
    step_name: str
    records_processed: int
    last_key: str
    custom_data: Dict[str, Any] = field(default_factory=dict)
    timestamp: datetime = field(default_factory=datetime.now)


@dataclass
class JobResult:
    """Result of batch job execution"""
    job_name: str
    return_code: int
    records_read: int = 0
    records_written: int = 0
    start_time: datetime = None
    end_time: datetime = None
    error_message: str = ""
    checkpoints: List[CheckpointData] = field(default_factory=list)


class BatchController:
    """
    Batch Controller - Manages batch job execution.
    
    Provides:
    - Job scheduling and dependency management
    - Checkpoint/restart capability
    - Job status tracking
    - Error handling and recovery
    
    Original COBOL framework features:
    - BCHCTL: Batch control record management
    - CHKPNT: Checkpoint/restart processing
    - ERRPROC: Error handling procedures
    """
    
    def __init__(self, session: Session = None):
        """
        Initialize the batch controller.
        
        Args:
            session: SQLAlchemy session (optional)
        """
        self._session = session
        self._owns_session = session is None
        
        # Registered jobs
        self._jobs: Dict[str, Callable] = {}
        
        # Current job context
        self._current_job: Optional[BatchControlRecord] = None
        self._checkpoint_interval: int = 1000
        self._records_since_checkpoint: int = 0
        
        logger.info("BatchController initialized")
    
    @property
    def session(self) -> Session:
        """Get or create database session"""
        if self._session is None:
            self._session = get_session()
        return self._session
    
    def register_job(self, job_name: str, job_function: Callable):
        """
        Register a batch job.
        
        Args:
            job_name: Unique job identifier
            job_function: Function to execute for the job
        """
        self._jobs[job_name] = job_function
        logger.info(f"Registered job: {job_name}")
    
    def submit_job(
        self,
        job_name: str,
        process_date: str = None,
        prerequisites: List[str] = None
    ) -> BatchControlRecord:
        """
        Submit a job for execution.
        
        Args:
            job_name: Job identifier
            process_date: Processing date (defaults to today)
            prerequisites: List of prerequisite job names
            
        Returns:
            BatchControlRecord for the submitted job
        """
        if process_date is None:
            process_date = datetime.now().strftime('%Y%m%d')
        
        # Get next sequence number
        sequence_no = self._get_next_sequence(job_name, process_date)
        
        # Create batch control record
        batch_record = BatchControlRecord(
            job_name=job_name,
            process_date=process_date,
            sequence_no=sequence_no,
            status=BatchStatus.READY,
            prereq_count=len(prerequisites) if prerequisites else 0
        )
        
        # Save to database
        db_record = BatchControl.from_record(batch_record)
        self.session.add(db_record)
        self.session.commit()
        
        logger.info(f"Submitted job: {job_name} for {process_date}")
        return batch_record
    
    def execute_job(
        self,
        job_name: str,
        process_date: str = None,
        **kwargs
    ) -> JobResult:
        """
        Execute a batch job.
        
        Args:
            job_name: Job identifier
            process_date: Processing date
            **kwargs: Additional arguments for the job function
            
        Returns:
            JobResult with execution results
        """
        if job_name not in self._jobs:
            raise ValueError(f"Unknown job: {job_name}")
        
        if process_date is None:
            process_date = datetime.now().strftime('%Y%m%d')
        
        # Get or create batch control record
        batch_record = self._get_or_create_batch_record(job_name, process_date)
        self._current_job = batch_record
        
        # Check prerequisites
        if not batch_record.can_start:
            logger.warning(f"Job {job_name} cannot start - prerequisites not met")
            return JobResult(
                job_name=job_name,
                return_code=12,
                error_message="Prerequisites not met"
            )
        
        # Start job
        batch_record.start()
        self._update_batch_record(batch_record)
        
        result = JobResult(
            job_name=job_name,
            return_code=0,
            start_time=datetime.now()
        )
        
        try:
            # Execute job function
            job_function = self._jobs[job_name]
            job_result = job_function(**kwargs)
            
            # Process job result
            if isinstance(job_result, dict):
                result.records_read = job_result.get('records_read', 0)
                result.records_written = job_result.get('records_written', 0)
                result.return_code = job_result.get('return_code', 0)
            elif isinstance(job_result, int):
                result.return_code = job_result
            
            # Complete job
            batch_record.complete(result.return_code)
            batch_record.records_read = result.records_read
            batch_record.records_written = result.records_written
            
        except Exception as e:
            logger.error(f"Job {job_name} failed: {e}")
            result.return_code = 12
            result.error_message = str(e)
            batch_record.complete(12, str(e))
        
        result.end_time = datetime.now()
        self._update_batch_record(batch_record)
        self._current_job = None
        
        self._display_job_result(result)
        return result
    
    def checkpoint(
        self,
        step_name: str,
        records_processed: int,
        last_key: str,
        custom_data: Dict[str, Any] = None
    ):
        """
        Create a checkpoint for restart capability.
        Implements COBOL CHKPNT functionality.
        
        Args:
            step_name: Current step name
            records_processed: Number of records processed
            last_key: Last processed key
            custom_data: Additional checkpoint data
        """
        if self._current_job is None:
            logger.warning("No current job for checkpoint")
            return
        
        checkpoint = CheckpointData(
            job_name=self._current_job.job_name,
            step_name=step_name,
            records_processed=records_processed,
            last_key=last_key,
            custom_data=custom_data or {}
        )
        
        # Save checkpoint to database (could use separate table)
        self._current_job.step_name = step_name
        self._update_batch_record(self._current_job)
        
        # Commit transaction
        self.session.commit()
        
        logger.debug(f"Checkpoint: {step_name}, records={records_processed}, key={last_key}")
        self._records_since_checkpoint = 0
    
    def should_checkpoint(self) -> bool:
        """Check if checkpoint should be taken"""
        self._records_since_checkpoint += 1
        return self._records_since_checkpoint >= self._checkpoint_interval
    
    def get_last_checkpoint(
        self,
        job_name: str,
        process_date: str
    ) -> Optional[CheckpointData]:
        """
        Get last checkpoint for restart.
        
        Args:
            job_name: Job identifier
            process_date: Processing date
            
        Returns:
            CheckpointData if found, None otherwise
        """
        batch_record = self.session.query(BatchControl).filter(
            BatchControl.job_name == job_name,
            BatchControl.process_date == process_date,
            BatchControl.status == BatchStatus.ACTIVE.value
        ).first()
        
        if batch_record:
            return CheckpointData(
                job_name=batch_record.job_name,
                step_name=batch_record.step_name or '',
                records_processed=batch_record.records_read,
                last_key='',  # Would need separate checkpoint table for full data
                timestamp=batch_record.attempt_timestamp
            )
        
        return None
    
    def get_job_status(
        self,
        job_name: str,
        process_date: str = None
    ) -> Optional[BatchControlRecord]:
        """
        Get job status.
        
        Args:
            job_name: Job identifier
            process_date: Processing date (defaults to today)
            
        Returns:
            BatchControlRecord if found
        """
        if process_date is None:
            process_date = datetime.now().strftime('%Y%m%d')
        
        db_record = self.session.query(BatchControl).filter(
            BatchControl.job_name == job_name,
            BatchControl.process_date == process_date
        ).order_by(BatchControl.sequence_no.desc()).first()
        
        if db_record:
            return db_record.to_record()
        
        return None
    
    def get_pending_jobs(self, process_date: str = None) -> List[BatchControlRecord]:
        """
        Get all pending jobs for a date.
        
        Args:
            process_date: Processing date
            
        Returns:
            List of pending BatchControlRecord objects
        """
        if process_date is None:
            process_date = datetime.now().strftime('%Y%m%d')
        
        db_records = self.session.query(BatchControl).filter(
            BatchControl.process_date == process_date,
            BatchControl.status == BatchStatus.READY.value
        ).all()
        
        return [r.to_record() for r in db_records]
    
    def get_completed_jobs(self, process_date: str = None) -> Dict[str, int]:
        """
        Get completed jobs and their return codes.
        
        Args:
            process_date: Processing date
            
        Returns:
            Dictionary mapping job_name to return_code
        """
        if process_date is None:
            process_date = datetime.now().strftime('%Y%m%d')
        
        db_records = self.session.query(BatchControl).filter(
            BatchControl.process_date == process_date,
            BatchControl.status == BatchStatus.DONE.value
        ).all()
        
        return {r.job_name: r.return_code for r in db_records}
    
    def _get_next_sequence(self, job_name: str, process_date: str) -> int:
        """Get next sequence number for job"""
        max_seq = self.session.query(BatchControl).filter(
            BatchControl.job_name == job_name,
            BatchControl.process_date == process_date
        ).count()
        return max_seq + 1
    
    def _get_or_create_batch_record(
        self,
        job_name: str,
        process_date: str
    ) -> BatchControlRecord:
        """Get existing or create new batch record"""
        db_record = self.session.query(BatchControl).filter(
            BatchControl.job_name == job_name,
            BatchControl.process_date == process_date,
            BatchControl.status.in_([BatchStatus.READY.value, BatchStatus.ACTIVE.value])
        ).first()
        
        if db_record:
            return db_record.to_record()
        
        return self.submit_job(job_name, process_date)
    
    def _update_batch_record(self, record: BatchControlRecord):
        """Update batch record in database"""
        db_record = self.session.query(BatchControl).filter(
            BatchControl.job_name == record.job_name,
            BatchControl.process_date == record.process_date,
            BatchControl.sequence_no == record.sequence_no
        ).first()
        
        if db_record:
            db_record.status = record.status.value
            db_record.step_name = record.step_name
            db_record.program_name = record.program_name
            db_record.start_time = record.start_time
            db_record.end_time = record.end_time
            db_record.return_code = record.return_code
            db_record.error_desc = record.error_desc
            db_record.restart_count = record.restart_count
            db_record.attempt_timestamp = record.attempt_timestamp
            db_record.complete_timestamp = record.complete_timestamp
            db_record.records_read = record.records_read
            db_record.records_written = record.records_written
            self.session.commit()
    
    def _display_job_result(self, result: JobResult):
        """Display job execution result"""
        logger.info("=" * 60)
        logger.info(f"Job Execution Result: {result.job_name}")
        logger.info(f"  Return Code:     {result.return_code}")
        logger.info(f"  Records Read:    {result.records_read}")
        logger.info(f"  Records Written: {result.records_written}")
        logger.info(f"  Start Time:      {result.start_time}")
        logger.info(f"  End Time:        {result.end_time}")
        if result.error_message:
            logger.info(f"  Error:           {result.error_message}")
        logger.info("=" * 60)
    
    def close(self):
        """Close session if owned"""
        if self._owns_session and self._session:
            self._session.close()
            self._session = None
