"""
Application configuration settings.
Replaces JCL parameters and CICS system definitions.
"""

from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    app_name: str = "Portfolio Management System"
    app_version: str = "1.0.0"
    debug: bool = False

    database_url: str = "postgresql://postgres:postgres@localhost:5432/portfolio_db"
    database_pool_size: int = 10
    database_max_overflow: int = 20

    secret_key: str = "your-secret-key-change-in-production"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30

    batch_commit_frequency: int = 1000
    batch_max_errors: int = 100
    batch_max_restarts: int = 3

    log_level: str = "INFO"
    log_format: str = "json"

    cors_origins: list[str] = ["*"]

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
