from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql+asyncpg://portfolio:portfolio@localhost:5432/portfolio_db"
    SECRET_KEY: str = "change-me-in-production"
    DEBUG: bool = False
    APP_NAME: str = "Investment Portfolio Management API"

    # JWT configuration
    JWT_SECRET: str = "jwt-secret-change-in-production"
    JWT_REFRESH_SECRET: str = "jwt-refresh-secret-change-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRY_MINUTES: int = 30
    JWT_REFRESH_EXPIRY_DAYS: int = 7

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
