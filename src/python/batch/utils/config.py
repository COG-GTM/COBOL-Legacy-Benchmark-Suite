"""
Configuration Management

Provides configuration handling for batch processing programs.
Replaces JCL parameters and SYSIN configuration.
"""

import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, Optional

try:
    import yaml
    YAML_AVAILABLE = True
except ImportError:
    YAML_AVAILABLE = False

import json


@dataclass
class DatabaseConfig:
    """Database configuration."""
    host: str = "localhost"
    port: int = 5432
    name: str = "portfolio"
    user: str = "postgres"
    password: str = "postgres"
    pool_size: int = 5
    max_overflow: int = 10
    
    @property
    def url(self) -> str:
        """Get database connection URL."""
        return f"postgresql://{self.user}:{self.password}@{self.host}:{self.port}/{self.name}"
    
    @classmethod
    def from_env(cls) -> "DatabaseConfig":
        """Create configuration from environment variables."""
        return cls(
            host=os.environ.get("DB_HOST", "localhost"),
            port=int(os.environ.get("DB_PORT", "5432")),
            name=os.environ.get("DB_NAME", "portfolio"),
            user=os.environ.get("DB_USER", "postgres"),
            password=os.environ.get("DB_PASSWORD", "postgres"),
            pool_size=int(os.environ.get("DB_POOL_SIZE", "5")),
            max_overflow=int(os.environ.get("DB_MAX_OVERFLOW", "10")),
        )


@dataclass
class CheckpointConfig:
    """Checkpoint configuration."""
    commit_freq: int = 1000
    max_errors: int = 100
    max_restarts: int = 3
    storage_type: str = "file"
    storage_path: str = "/tmp/checkpoints"
    
    @classmethod
    def from_env(cls) -> "CheckpointConfig":
        """Create configuration from environment variables."""
        return cls(
            commit_freq=int(os.environ.get("CHECKPOINT_COMMIT_FREQ", "1000")),
            max_errors=int(os.environ.get("CHECKPOINT_MAX_ERRORS", "100")),
            max_restarts=int(os.environ.get("CHECKPOINT_MAX_RESTARTS", "3")),
            storage_type=os.environ.get("CHECKPOINT_STORAGE_TYPE", "file"),
            storage_path=os.environ.get("CHECKPOINT_STORAGE_PATH", "/tmp/checkpoints"),
        )


@dataclass
class ProcessingConfig:
    """Processing configuration."""
    process_date: str = ""
    restart_mode: bool = False
    dry_run: bool = False
    verbose: bool = False
    log_level: str = "INFO"
    log_file: Optional[str] = None
    
    def __post_init__(self) -> None:
        if not self.process_date:
            from datetime import datetime
            self.process_date = datetime.now().strftime("%Y%m%d")
    
    @classmethod
    def from_env(cls) -> "ProcessingConfig":
        """Create configuration from environment variables."""
        return cls(
            process_date=os.environ.get("PROCESS_DATE", ""),
            restart_mode=os.environ.get("RESTART_MODE", "false").lower() == "true",
            dry_run=os.environ.get("DRY_RUN", "false").lower() == "true",
            verbose=os.environ.get("VERBOSE", "false").lower() == "true",
            log_level=os.environ.get("LOG_LEVEL", "INFO"),
            log_file=os.environ.get("LOG_FILE"),
        )


@dataclass
class BatchConfig:
    """
    Complete batch processing configuration.
    
    Replaces JCL parameters and SYSIN configuration from COBOL.
    """
    database: DatabaseConfig = field(default_factory=DatabaseConfig)
    checkpoint: CheckpointConfig = field(default_factory=CheckpointConfig)
    processing: ProcessingConfig = field(default_factory=ProcessingConfig)
    custom: Dict[str, Any] = field(default_factory=dict)
    
    @classmethod
    def from_env(cls) -> "BatchConfig":
        """Create configuration from environment variables."""
        return cls(
            database=DatabaseConfig.from_env(),
            checkpoint=CheckpointConfig.from_env(),
            processing=ProcessingConfig.from_env(),
        )
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "BatchConfig":
        """Create configuration from dictionary."""
        return cls(
            database=DatabaseConfig(**data.get("database", {})),
            checkpoint=CheckpointConfig(**data.get("checkpoint", {})),
            processing=ProcessingConfig(**data.get("processing", {})),
            custom=data.get("custom", {}),
        )
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert configuration to dictionary."""
        return {
            "database": {
                "host": self.database.host,
                "port": self.database.port,
                "name": self.database.name,
                "user": self.database.user,
                "pool_size": self.database.pool_size,
                "max_overflow": self.database.max_overflow,
            },
            "checkpoint": {
                "commit_freq": self.checkpoint.commit_freq,
                "max_errors": self.checkpoint.max_errors,
                "max_restarts": self.checkpoint.max_restarts,
                "storage_type": self.checkpoint.storage_type,
                "storage_path": self.checkpoint.storage_path,
            },
            "processing": {
                "process_date": self.processing.process_date,
                "restart_mode": self.processing.restart_mode,
                "dry_run": self.processing.dry_run,
                "verbose": self.processing.verbose,
                "log_level": self.processing.log_level,
                "log_file": self.processing.log_file,
            },
            "custom": self.custom,
        }


def load_config(
    config_file: Optional[str] = None,
    use_env: bool = True,
) -> BatchConfig:
    """
    Load batch processing configuration.
    
    Priority (highest to lowest):
    1. Environment variables
    2. Configuration file
    3. Default values
    
    Args:
        config_file: Optional path to configuration file (JSON or YAML)
        use_env: Whether to use environment variables
        
    Returns:
        BatchConfig instance
    """
    config = BatchConfig()
    
    if config_file:
        config_path = Path(config_file)
        if config_path.exists():
            with open(config_path, "r") as f:
                if config_path.suffix in (".yaml", ".yml") and YAML_AVAILABLE:
                    data = yaml.safe_load(f)
                else:
                    data = json.load(f)
            config = BatchConfig.from_dict(data)
    
    if use_env:
        env_config = BatchConfig.from_env()
        if os.environ.get("DB_HOST"):
            config.database = env_config.database
        if os.environ.get("CHECKPOINT_COMMIT_FREQ"):
            config.checkpoint = env_config.checkpoint
        if os.environ.get("PROCESS_DATE") or os.environ.get("RESTART_MODE"):
            config.processing = env_config.processing
    
    return config


def save_config(config: BatchConfig, config_file: str) -> None:
    """
    Save batch processing configuration to file.
    
    Args:
        config: BatchConfig instance
        config_file: Path to configuration file
    """
    config_path = Path(config_file)
    data = config.to_dict()
    
    with open(config_path, "w") as f:
        if config_path.suffix in (".yaml", ".yml") and YAML_AVAILABLE:
            yaml.dump(data, f, default_flow_style=False)
        else:
            json.dump(data, f, indent=2)
