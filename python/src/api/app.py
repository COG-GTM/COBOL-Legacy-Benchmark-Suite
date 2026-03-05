"""
FastAPI application replacing CICS transaction server.
Main entry point for the REST API.
"""

import logging

from fastapi import FastAPI

from src.api.routers import admin, history, inquiry, portfolio
from src.common.logging_config import configure_logging
from src.db.engine import init_db

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Investment Portfolio Management System",
    description="REST API replacing CICS online programs — migrated from COBOL",
    version="1.0.0",
)

app.include_router(portfolio.router, prefix="/portfolios", tags=["Portfolios"])
app.include_router(inquiry.router, prefix="/positions", tags=["Positions"])
app.include_router(history.router, prefix="/transactions", tags=["Transactions"])
app.include_router(admin.router, tags=["Admin"])


@app.on_event("startup")
def startup_event():
    configure_logging()
    init_db()
    logger.info("Portfolio Management System API started")
