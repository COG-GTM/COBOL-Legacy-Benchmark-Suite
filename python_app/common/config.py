"""Application configuration module.

Provides centralized configuration using pydantic-settings,
replacing COBOL compile-time constants and JCL DD parameters.
"""

from pydantic_settings import BaseSettings


class DatabaseConfig(BaseSettings):
    """Database configuration replacing DB2 connection parameters."""

    url: str = "postgresql://postgres:postgres@localhost:5432/portfolio_mgmt"
    pool_size: int = 20
    max_overflow: int = 80
    pool_recycle: int = 3600
    echo: bool = False

    model_config = {"env_prefix": "DB_"}


class BatchConfig(BaseSettings):
    """Batch processing configuration replacing JCL parameters."""

    commit_interval: int = 1000  # HISTLD00 WS-COMMIT-INTERVAL
    max_errors: int = 100  # Maximum errors before abort
    checkpoint_interval: int = 1000  # Checkpoint frequency
    max_restarts: int = 3  # BCHCON WS-MAX-RESTARTS
    rc_max_continue: int = 4  # RC <= 4 allows pipeline to continue

    model_config = {"env_prefix": "BATCH_"}


class SecurityConfig(BaseSettings):
    """Security configuration replacing RACF/CICS security parameters."""

    jwt_secret_key: str = "portfolio-management-secret-key"
    jwt_algorithm: str = "HS256"
    jwt_expiration_minutes: int = 30
    max_login_attempts: int = 3  # SECMGR WS-MAX-ATTEMPTS

    model_config = {"env_prefix": "SECURITY_"}


class AppConfig(BaseSettings):
    """Top-level application configuration."""

    app_name: str = "Investment Portfolio Management System"
    debug: bool = False
    log_level: str = "INFO"

    database: DatabaseConfig = DatabaseConfig()
    batch: BatchConfig = BatchConfig()
    security: SecurityConfig = SecurityConfig()

    model_config = {"env_prefix": "APP_"}


def get_config() -> AppConfig:
    """Get application configuration singleton."""
    return AppConfig()
