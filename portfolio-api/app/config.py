from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql+asyncpg://portfolio:portfolio@localhost:5432/portfolio_db"
    SECRET_KEY: str = "change-me-in-production"
    DEBUG: bool = False
    APP_NAME: str = "Investment Portfolio Management API"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
