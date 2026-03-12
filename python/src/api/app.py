"""
FastAPI application replacing CICS transaction server.

Main application setup translating the CICS environment:
  - CICS transaction PINQ -> REST API endpoints
  - BMS maps -> Pydantic request/response schemas
  - CICS pseudo-conversational -> Stateless REST
"""

import logging
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from src.api.routers import admin, history, inquiry, portfolio
from src.common.logging_config import configure_logging
from src.db.engine import dispose_engine, get_engine
from src.db.tables import Base

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Application lifespan: startup and shutdown."""
    configure_logging()
    logger.info("Starting Investment Portfolio Management System")

    # Initialize database (use singleton engine so dispose_engine() cleans it up)
    engine = get_engine()
    Base.metadata.create_all(engine)
    logger.info("Database initialized")

    yield

    # Shutdown
    dispose_engine()
    logger.info("Application shutdown complete")


app = FastAPI(
    title="Investment Portfolio Management System",
    description=(
        "REST API for managing investment portfolios, transactions, and positions. "
        "Migrated from COBOL/CICS/DB2/VSAM mainframe system."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

# Register routers (replacing CICS transaction routing)
app.include_router(portfolio.router, prefix="/portfolios", tags=["Portfolios"])
app.include_router(inquiry.router, prefix="/positions", tags=["Positions"])
app.include_router(history.router, prefix="/transactions", tags=["Transactions"])
app.include_router(admin.router, prefix="/admin", tags=["Admin"])
