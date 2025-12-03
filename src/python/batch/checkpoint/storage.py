"""
Checkpoint Storage

Provides storage backends for checkpoint data.
Replaces VSAM checkpoint file access patterns.
"""

import json
from abc import ABC, abstractmethod
from datetime import datetime
from pathlib import Path
from typing import Optional

from sqlalchemy.orm import Session

from ..database.models import Checkpoint as CheckpointModel
from ..models.checkpoint import CheckpointControl, CheckpointHeader, CheckpointStatus


class CheckpointStorage(ABC):
    """Abstract base class for checkpoint storage."""
    
    @abstractmethod
    def save(self, checkpoint: CheckpointControl) -> None:
        """Save checkpoint data."""
        pass
    
    @abstractmethod
    def load(self, program_id: str, run_date: str) -> Optional[CheckpointControl]:
        """Load checkpoint data for a program and date."""
        pass
    
    @abstractmethod
    def get_latest(self, program_id: str) -> Optional[CheckpointControl]:
        """Get the latest checkpoint for a program."""
        pass
    
    @abstractmethod
    def delete(self, program_id: str, run_date: str) -> bool:
        """Delete checkpoint data."""
        pass


class FileCheckpointStorage(CheckpointStorage):
    """
    File-based checkpoint storage.
    
    Stores checkpoint data as JSON files, similar to VSAM KSDS access.
    """
    
    def __init__(self, base_path: str = "/tmp/checkpoints"):
        """
        Initialize file-based checkpoint storage.
        
        Args:
            base_path: Base directory for checkpoint files
        """
        self.base_path = Path(base_path)
        self.base_path.mkdir(parents=True, exist_ok=True)
    
    def _get_file_path(self, program_id: str, run_date: str) -> Path:
        """Get the file path for a checkpoint."""
        return self.base_path / f"{program_id.strip()}_{run_date.strip()}.json"
    
    def save(self, checkpoint: CheckpointControl) -> None:
        """Save checkpoint data to file."""
        file_path = self._get_file_path(
            checkpoint.program_id, checkpoint.run_date
        )
        with open(file_path, "w") as f:
            json.dump(checkpoint.to_dict(), f, indent=2)
    
    def load(self, program_id: str, run_date: str) -> Optional[CheckpointControl]:
        """Load checkpoint data from file."""
        file_path = self._get_file_path(program_id, run_date)
        if not file_path.exists():
            return None
        
        with open(file_path, "r") as f:
            data = json.load(f)
        return CheckpointControl.from_dict(data)
    
    def get_latest(self, program_id: str) -> Optional[CheckpointControl]:
        """Get the latest checkpoint for a program."""
        pattern = f"{program_id.strip()}_*.json"
        files = sorted(self.base_path.glob(pattern), reverse=True)
        
        if not files:
            return None
        
        with open(files[0], "r") as f:
            data = json.load(f)
        return CheckpointControl.from_dict(data)
    
    def delete(self, program_id: str, run_date: str) -> bool:
        """Delete checkpoint file."""
        file_path = self._get_file_path(program_id, run_date)
        if file_path.exists():
            file_path.unlink()
            return True
        return False


class DatabaseCheckpointStorage(CheckpointStorage):
    """
    Database-based checkpoint storage.
    
    Stores checkpoint data in PostgreSQL, replacing VSAM file access.
    """
    
    def __init__(self, session_factory):
        """
        Initialize database checkpoint storage.
        
        Args:
            session_factory: SQLAlchemy session factory
        """
        self.session_factory = session_factory
    
    def _to_db_model(self, checkpoint: CheckpointControl) -> CheckpointModel:
        """Convert CheckpointControl to database model."""
        return CheckpointModel(
            program_id=checkpoint.program_id.strip(),
            run_date=checkpoint.run_date.strip(),
            run_time=checkpoint.run_time.strip(),
            status=checkpoint.status.value,
            phase=checkpoint.phase.value,
            records_read=checkpoint.records_read,
            records_processed=checkpoint.records_processed,
            records_error=checkpoint.records_error,
            restart_count=checkpoint.counters.restart_count,
            last_key=checkpoint.last_key.strip() if checkpoint.last_key else None,
            last_checkpoint_time=datetime.now(),
            commit_freq=checkpoint.commit_freq,
            max_errors=checkpoint.max_errors,
            max_restarts=checkpoint.max_restarts,
            restart_mode=checkpoint.restart_mode.value,
        )
    
    def _from_db_model(self, model: CheckpointModel) -> CheckpointControl:
        """Convert database model to CheckpointControl."""
        from ..models.checkpoint import (
            CheckpointCounters,
            CheckpointControlInfo,
            CheckpointPhase,
            CheckpointPosition,
            CheckpointResources,
            RestartMode,
        )
        
        return CheckpointControl(
            header=CheckpointHeader(
                program_id=model.program_id,
                run_date=model.run_date,
                run_time=model.run_time,
                status=CheckpointStatus(model.status),
            ),
            counters=CheckpointCounters(
                records_read=model.records_read,
                records_processed=model.records_processed,
                records_error=model.records_error,
                restart_count=model.restart_count,
            ),
            position=CheckpointPosition(
                last_key=model.last_key or "",
                last_time=model.last_checkpoint_time.isoformat() if model.last_checkpoint_time else "",
                phase=CheckpointPhase(model.phase),
            ),
            resources=CheckpointResources(),
            control_info=CheckpointControlInfo(
                commit_freq=model.commit_freq,
                max_errors=model.max_errors,
                max_restarts=model.max_restarts,
                restart_mode=RestartMode(model.restart_mode),
            ),
        )
    
    def save(self, checkpoint: CheckpointControl) -> None:
        """Save checkpoint data to database."""
        session: Session = self.session_factory()
        try:
            existing = session.query(CheckpointModel).filter(
                CheckpointModel.program_id == checkpoint.program_id.strip(),
                CheckpointModel.run_date == checkpoint.run_date.strip(),
                CheckpointModel.run_time == checkpoint.run_time.strip(),
            ).first()
            
            if existing:
                existing.status = checkpoint.status.value
                existing.phase = checkpoint.phase.value
                existing.records_read = checkpoint.records_read
                existing.records_processed = checkpoint.records_processed
                existing.records_error = checkpoint.records_error
                existing.restart_count = checkpoint.counters.restart_count
                existing.last_key = checkpoint.last_key.strip() if checkpoint.last_key else None
                existing.last_checkpoint_time = datetime.now()
                existing.restart_mode = checkpoint.restart_mode.value
            else:
                db_model = self._to_db_model(checkpoint)
                session.add(db_model)
            
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()
    
    def load(self, program_id: str, run_date: str) -> Optional[CheckpointControl]:
        """Load checkpoint data from database."""
        session: Session = self.session_factory()
        try:
            model = session.query(CheckpointModel).filter(
                CheckpointModel.program_id == program_id.strip(),
                CheckpointModel.run_date == run_date.strip(),
            ).order_by(CheckpointModel.run_time.desc()).first()
            
            if model:
                return self._from_db_model(model)
            return None
        finally:
            session.close()
    
    def get_latest(self, program_id: str) -> Optional[CheckpointControl]:
        """Get the latest checkpoint for a program."""
        session: Session = self.session_factory()
        try:
            model = session.query(CheckpointModel).filter(
                CheckpointModel.program_id == program_id.strip(),
            ).order_by(
                CheckpointModel.run_date.desc(),
                CheckpointModel.run_time.desc(),
            ).first()
            
            if model:
                return self._from_db_model(model)
            return None
        finally:
            session.close()
    
    def delete(self, program_id: str, run_date: str) -> bool:
        """Delete checkpoint data from database."""
        session: Session = self.session_factory()
        try:
            deleted = session.query(CheckpointModel).filter(
                CheckpointModel.program_id == program_id.strip(),
                CheckpointModel.run_date == run_date.strip(),
            ).delete()
            session.commit()
            return deleted > 0
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()
