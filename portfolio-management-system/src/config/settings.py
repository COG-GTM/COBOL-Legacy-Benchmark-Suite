"""
Application configuration settings.
Replaces COBOL JCL PARM values and system configuration.
"""

import os
from typing import Optional

from pydantic_settings import BaseSettings


class DatabaseSettings(BaseSettings):
    """Database configuration settings."""
    
    host: str = "localhost"
    port: int = 5432
    name: str = "portfolio_db"
    user: str = "portfolio_user"
    password: str = ""
    pool_size: int = 10
    max_overflow: int = 20
    
    @property
    def url(self) -> str:
        """Generate database URL."""
        return f"postgresql://{self.user}:{self.password}@{self.host}:{self.port}/{self.name}"
    
    class Config:
        env_prefix = "DB_"


class AppSettings(BaseSettings):
    """Application settings."""
    
    debug: bool = False
    secret_key: str = "change-me-in-production"
    log_level: str = "INFO"
    
    # Batch processing settings
    batch_commit_frequency: int = 1000
    batch_max_errors: int = 100
    
    # Session settings
    session_timeout_minutes: int = 30
    
    class Config:
        env_prefix = "APP_"


class AirflowSettings(BaseSettings):
    """Airflow configuration settings."""
    
    home: str = "/opt/airflow"
    dags_folder: str = "/opt/airflow/dags"
    executor: str = "LocalExecutor"
    
    class Config:
        env_prefix = "AIRFLOW_"


class Settings(BaseSettings):
    """Main settings class combining all configuration."""
    
    database: DatabaseSettings = DatabaseSettings()
    app: AppSettings = AppSettings()
    airflow: AirflowSettings = AirflowSettings()
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


# Global settings instance
settings = Settings()
