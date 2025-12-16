"""
Checkpoint Manager for COBOL Legacy Migration
Manages checkpoint/restart functionality for long-running migrations.
"""

import os
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Optional
import structlog

logger = structlog.get_logger(__name__)


class CheckpointManager:
    """Manages checkpoints for migration restart capability."""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.checkpoint_dir = Path(os.environ.get('CHECKPOINT_PATH', '/tmp/migration_checkpoints'))
        self.checkpoint_dir.mkdir(parents=True, exist_ok=True)
        
    def get_checkpoint(self, source_name: str) -> int:
        """Get the last checkpoint position for a source."""
        
        checkpoint_file = self.checkpoint_dir / f"{source_name}.checkpoint"
        
        if checkpoint_file.exists():
            try:
                with open(checkpoint_file, 'r') as f:
                    data = json.load(f)
                    position = data.get('position', 0)
                    logger.info(f"Resuming from checkpoint", source=source_name, position=position)
                    return position
            except Exception as e:
                logger.warning(f"Error reading checkpoint", source=source_name, error=str(e))
                
        return 0
    
    def save_checkpoint(self, source_name: str, position: int, metadata: Optional[Dict[str, Any]] = None) -> None:
        """Save checkpoint for a source."""
        
        checkpoint_file = self.checkpoint_dir / f"{source_name}.checkpoint"
        
        data = {
            'source': source_name,
            'position': position,
            'timestamp': datetime.now().isoformat(),
            'metadata': metadata or {}
        }
        
        try:
            with open(checkpoint_file, 'w') as f:
                json.dump(data, f, indent=2)
            logger.debug(f"Checkpoint saved", source=source_name, position=position)
        except Exception as e:
            logger.error(f"Error saving checkpoint", source=source_name, error=str(e))
    
    def clear_checkpoint(self, source_name: str) -> None:
        """Clear checkpoint for a source."""
        
        checkpoint_file = self.checkpoint_dir / f"{source_name}.checkpoint"
        
        if checkpoint_file.exists():
            checkpoint_file.unlink()
            logger.info(f"Checkpoint cleared", source=source_name)
    
    def clear_all_checkpoints(self) -> None:
        """Clear all checkpoints."""
        
        for checkpoint_file in self.checkpoint_dir.glob("*.checkpoint"):
            checkpoint_file.unlink()
            
        logger.info("All checkpoints cleared")
    
    def list_checkpoints(self) -> Dict[str, Dict[str, Any]]:
        """List all checkpoints."""
        
        checkpoints = {}
        
        for checkpoint_file in self.checkpoint_dir.glob("*.checkpoint"):
            try:
                with open(checkpoint_file, 'r') as f:
                    data = json.load(f)
                    source_name = checkpoint_file.stem
                    checkpoints[source_name] = data
            except Exception as e:
                logger.warning(f"Error reading checkpoint file", file=str(checkpoint_file), error=str(e))
                
        return checkpoints
    
    def get_migration_status(self) -> Dict[str, Any]:
        """Get overall migration status."""
        
        checkpoints = self.list_checkpoints()
        
        status = {
            'total_sources': len(checkpoints),
            'sources': {},
            'last_updated': None
        }
        
        for source_name, data in checkpoints.items():
            status['sources'][source_name] = {
                'position': data.get('position', 0),
                'timestamp': data.get('timestamp'),
            }
            
            timestamp = data.get('timestamp')
            if timestamp:
                if status['last_updated'] is None or timestamp > status['last_updated']:
                    status['last_updated'] = timestamp
                    
        return status
