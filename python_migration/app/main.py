"""
FastAPI Application Entry Point.
Replaces CICS transaction processing with REST API.

This is the main entry point for the Portfolio Management System API.
It replaces the CICS online transaction processing environment.
"""

from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.router import api_router
from app.config import get_settings
from app.services.database import get_database_service
from app.utils.exceptions import PortfolioError
from app.utils.logging import get_logger, setup_logging


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """
    Application lifespan manager.
    Handles startup and shutdown events.
    """
    setup_logging()
    logger = get_logger("main")
    logger.info("Starting Portfolio Management System API")

    db_service = get_database_service()
    db_service.create_tables()
    logger.info("Database tables created/verified")

    yield

    logger.info("Shutting down Portfolio Management System API")


def create_app() -> FastAPI:
    """
    Create and configure the FastAPI application.

    Returns:
        Configured FastAPI application instance
    """
    settings = get_settings()

    app = FastAPI(
        title=settings.app_name,
        version=settings.app_version,
        description="""
        Portfolio Management System API

        This API provides endpoints for managing investment portfolios,
        including portfolio inquiries, position management, and transaction
        history. It is a Python migration of the COBOL Legacy Benchmark Suite.

        ## Features

        * Portfolio Management - Create and query portfolios
        * Position Tracking - View and update portfolio positions
        * Transaction History - Query transaction history
        * Authentication - JWT-based authentication

        ## Migration Notes

        This API replaces the following CICS transactions:
        * PINQ - Portfolio Inquiry (INQONLN, INQPORT, INQHIST)
        * Security validation (SECMGR)

        Database: PostgreSQL (replacing DB2 for z/OS)
        """,
        lifespan=lifespan,
        docs_url="/api/docs",
        redoc_url="/api/redoc",
        openapi_url="/api/openapi.json",
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(api_router, prefix="/api/v1")

    @app.exception_handler(PortfolioError)
    async def portfolio_error_handler(
        request: Request,
        exc: PortfolioError,
    ) -> JSONResponse:
        """
        Handle PortfolioError exceptions.
        Returns structured error response matching COBOL error format.
        """
        return JSONResponse(
            status_code=_get_http_status(exc),
            content=exc.to_dict(),
        )

    @app.get("/")
    async def root() -> dict:
        """Root endpoint."""
        return {
            "name": settings.app_name,
            "version": settings.app_version,
            "status": "running",
            "docs": "/api/docs",
        }

    return app


def _get_http_status(exc: PortfolioError) -> int:
    """Map PortfolioError severity to HTTP status code."""
    from app.utils.exceptions import ErrorSeverity

    if exc.severity == ErrorSeverity.WARNING:
        return status.HTTP_404_NOT_FOUND
    elif exc.severity == ErrorSeverity.ERROR:
        return status.HTTP_400_BAD_REQUEST
    elif exc.severity >= ErrorSeverity.SEVERE:
        return status.HTTP_500_INTERNAL_SERVER_ERROR
    return status.HTTP_400_BAD_REQUEST


app = create_app()


if __name__ == "__main__":
    import uvicorn

    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.debug,
    )
