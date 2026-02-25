"""
Configuration Module

Contains application configuration management.
Replaces JCL parameter passing and system configuration.
"""

import os
from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Database configuration
    database_url: str = "postgresql://localhost:5432/portfolio"
    db_pool_size: int = 5
    db_max_overflow: int = 10
    
    # Application configuration
    app_name: str = "Portfolio Management System"
    app_version: str = "1.0.0"
    debug: bool = False
    
    # Security configuration
    secret_key: str = "change-me-in-production"
    jwt_algorithm: str = "HS256"
    jwt_expiration_hours: int = 24
    
    # Logging configuration
    log_level: str = "INFO"
    log_format: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    
    # Batch processing configuration
    batch_size: int = 1000
    checkpoint_interval: int = 100
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
