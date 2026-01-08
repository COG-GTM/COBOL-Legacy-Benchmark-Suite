"""
History Loader - Migrated from COBOL HISTLD00 program.

This module implements the history loading and audit trail functionality
from the original COBOL program, including DB2 operations for historical
data management.

Original COBOL Program: src/programs/batch/HISTLD00.cbl (referenced in architecture)
"""

import logging
import json
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Dict, Optional, Any
from enum import Enum

from sqlalchemy.orm import Session
from sqlalchemy import text

from ..models.history import HistoryRecord, History, HistoryRecordType, HistoryActionCode
from ..models.position import PositionRecord
from ..models.transaction import TransactionRecord
from ..database.connection import session_scope, get_session

logger = logging.getLogger(__name__)


@dataclass
class LoadResult:
    """Result of history load operation"""
    records_read: int = 0
    records_loaded: int = 0
    records_rejected: int = 0
    records_duplicate: int = 0
    return_code: int = 0  # 0=success, 4=warnings, 8=errors


class HistoryLoader:
    """
    History Loader - Migrated from COBOL HISTLD00.
    
    Loads audit history records into the database for tracking:
    - Position changes
    - Transaction processing
    - Portfolio modifications
    
    Original COBOL program flow:
    1. 0000-MAIN: Main control
    2. 1000-INITIALIZE: Open files, prepare SQL
    3. 2000-PROCESS: Read input, insert history records
    4. 3000-TERMINATE: Commit, close files, display statistics
    """
    
    def __init__(self, session: Session = None):
        """
        Initialize the history loader.
        
        Args:
            session: SQLAlchemy session (optional)
        """
        self._session = session
        self._owns_session = session is None
        
        # Sequence counter for generating unique keys
        self._sequence_counter: Dict[str, int] = {}
        
        # Statistics
        self.records_read = 0
        self.records_loaded = 0
        self.records_rejected = 0
        
        logger.info("HistoryLoader initialized")
    
    @property
    def session(self) -> Session:
        """Get or create database session"""
        if self._session is None:
            self._session = get_session()
        return self._session
    
    def load_position_history(
        self, 
        portfolio_id: str,
        before_position: Optional[PositionRecord],
        after_position: PositionRecord,
        action: HistoryActionCode,
        user: str = 'SYSTEM',
        reason_code: str = ''
    ) -> HistoryRecord:
        """
        Create history record for position change.
        
        Args:
            portfolio_id: Portfolio identifier
            before_position: Position before change (None for ADD)
            after_position: Position after change
            action: Action code (ADD, CHANGE, DELETE)
            user: User who made the change
            reason_code: Reason for change
            
        Returns:
            Created HistoryRecord
        """
        now = datetime.now()
        date_str = now.strftime('%Y%m%d')
        time_str = now.strftime('%H%M%S')
        
        # Generate sequence number
        seq_no = self._get_next_sequence(portfolio_id, date_str, time_str)
        
        # Create before/after images
        before_image = json.dumps(before_position.to_dict()) if before_position else ''
        after_image = json.dumps(after_position.to_dict())
        
        history_record = HistoryRecord(
            portfolio_id=portfolio_id,
            date=date_str,
            time=time_str,
            seq_no=f"{seq_no:04d}",
            record_type=HistoryRecordType.POSITION,
            action_code=action,
            before_image=before_image,
            after_image=after_image,
            reason_code=reason_code,
            process_date=now,
            process_user=user
        )
        
        self._save_history_record(history_record)
        return history_record
    
    def load_transaction_history(
        self,
        transaction: TransactionRecord,
        action: HistoryActionCode,
        user: str = 'SYSTEM',
        reason_code: str = ''
    ) -> HistoryRecord:
        """
        Create history record for transaction.
        
        Args:
            transaction: Transaction record
            action: Action code
            user: User who processed the transaction
            reason_code: Reason code
            
        Returns:
            Created HistoryRecord
        """
        now = datetime.now()
        date_str = now.strftime('%Y%m%d')
        time_str = now.strftime('%H%M%S')
        
        seq_no = self._get_next_sequence(transaction.portfolio_id, date_str, time_str)
        
        history_record = HistoryRecord(
            portfolio_id=transaction.portfolio_id,
            date=date_str,
            time=time_str,
            seq_no=f"{seq_no:04d}",
            record_type=HistoryRecordType.TRANSACTION,
            action_code=action,
            before_image='',
            after_image=json.dumps(transaction.to_dict()),
            reason_code=reason_code,
            process_date=now,
            process_user=user
        )
        
        self._save_history_record(history_record)
        return history_record
    
    def load_batch_history(
        self,
        records: List[Dict[str, Any]]
    ) -> LoadResult:
        """
        Load a batch of history records.
        Implements COBOL 2000-PROCESS paragraph.
        
        Args:
            records: List of history record dictionaries
            
        Returns:
            LoadResult with statistics
        """
        logger.info(f"Loading batch of {len(records)} history records")
        
        result = LoadResult()
        
        for record_data in records:
            self.records_read += 1
            result.records_read += 1
            
            try:
                # Validate record
                history_record = HistoryRecord.from_dict(record_data)
                is_valid, errors = history_record.validate()
                
                if not is_valid:
                    logger.warning(f"Invalid record: {errors}")
                    self.records_rejected += 1
                    result.records_rejected += 1
                    result.return_code = max(result.return_code, 4)
                    continue
                
                # Check for duplicate
                if self._is_duplicate(history_record):
                    logger.debug(f"Duplicate record: {history_record.key}")
                    result.records_duplicate += 1
                    continue
                
                # Save record
                self._save_history_record(history_record)
                self.records_loaded += 1
                result.records_loaded += 1
                
            except Exception as e:
                logger.error(f"Error loading record: {e}")
                self.records_rejected += 1
                result.records_rejected += 1
                result.return_code = max(result.return_code, 8)
        
        # Commit batch
        if self._owns_session:
            try:
                self.session.commit()
            except Exception as e:
                logger.error(f"Error committing batch: {e}")
                self.session.rollback()
                result.return_code = 12
        
        self._display_statistics(result)
        return result
    
    def get_history(
        self,
        portfolio_id: str,
        start_date: str = None,
        end_date: str = None,
        record_type: HistoryRecordType = None,
        limit: int = 100
    ) -> List[HistoryRecord]:
        """
        Query history records.
        
        Args:
            portfolio_id: Portfolio identifier
            start_date: Start date (YYYYMMDD)
            end_date: End date (YYYYMMDD)
            record_type: Filter by record type
            limit: Maximum records to return
            
        Returns:
            List of HistoryRecord objects
        """
        query = self.session.query(History).filter(
            History.portfolio_id == portfolio_id
        )
        
        if start_date:
            query = query.filter(History.date >= start_date)
        
        if end_date:
            query = query.filter(History.date <= end_date)
        
        if record_type:
            query = query.filter(History.record_type == record_type.value)
        
        query = query.order_by(History.date.desc(), History.time.desc())
        query = query.limit(limit)
        
        return [h.to_record() for h in query.all()]
    
    def get_audit_trail(
        self,
        portfolio_id: str,
        investment_id: str = None,
        start_date: str = None,
        end_date: str = None
    ) -> List[Dict[str, Any]]:
        """
        Get audit trail for a portfolio/investment.
        
        Args:
            portfolio_id: Portfolio identifier
            investment_id: Investment identifier (optional)
            start_date: Start date
            end_date: End date
            
        Returns:
            List of audit trail entries
        """
        history_records = self.get_history(
            portfolio_id=portfolio_id,
            start_date=start_date,
            end_date=end_date,
            limit=1000
        )
        
        audit_trail = []
        for record in history_records:
            entry = {
                'timestamp': f"{record.date} {record.time}",
                'record_type': record.record_type.value,
                'action': record.action_code.value,
                'user': record.process_user,
                'reason': record.reason_code
            }
            
            # Parse images if present
            if record.before_image:
                try:
                    entry['before'] = json.loads(record.before_image)
                except json.JSONDecodeError:
                    entry['before'] = record.before_image
            
            if record.after_image:
                try:
                    entry['after'] = json.loads(record.after_image)
                except json.JSONDecodeError:
                    entry['after'] = record.after_image
            
            # Filter by investment_id if specified
            if investment_id:
                after_data = entry.get('after', {})
                if isinstance(after_data, dict):
                    if after_data.get('investment_id') != investment_id:
                        continue
            
            audit_trail.append(entry)
        
        return audit_trail
    
    def _get_next_sequence(self, portfolio_id: str, date: str, time: str) -> int:
        """Generate next sequence number for a key"""
        key = f"{portfolio_id}|{date}|{time}"
        
        if key not in self._sequence_counter:
            # Query max sequence for this key
            max_seq = self.session.query(History).filter(
                History.portfolio_id == portfolio_id,
                History.date == date,
                History.time == time
            ).count()
            self._sequence_counter[key] = max_seq
        
        self._sequence_counter[key] += 1
        return self._sequence_counter[key]
    
    def _is_duplicate(self, record: HistoryRecord) -> bool:
        """Check if record already exists"""
        existing = self.session.query(History).filter(
            History.portfolio_id == record.portfolio_id,
            History.date == record.date,
            History.time == record.time,
            History.seq_no == record.seq_no
        ).first()
        return existing is not None
    
    def _save_history_record(self, record: HistoryRecord):
        """Save history record to database"""
        db_record = History.from_record(record)
        self.session.add(db_record)
    
    def _display_statistics(self, result: LoadResult):
        """Display processing statistics"""
        logger.info("=" * 60)
        logger.info("HISTLD00 Processing Statistics:")
        logger.info(f"  Records Read:      {result.records_read}")
        logger.info(f"  Records Loaded:    {result.records_loaded}")
        logger.info(f"  Records Rejected:  {result.records_rejected}")
        logger.info(f"  Records Duplicate: {result.records_duplicate}")
        logger.info(f"  Return Code:       {result.return_code}")
        logger.info("=" * 60)
    
    def close(self):
        """Close session if owned"""
        if self._owns_session and self._session:
            self._session.close()
            self._session = None
