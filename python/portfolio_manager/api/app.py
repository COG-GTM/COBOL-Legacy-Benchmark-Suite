"""FastAPI application setup.

Replaces the CICS transaction definition (PORTDFN.csd) and
INQONLN main controller program. Configures the FastAPI app
with all routers, middleware, and error handlers.
"""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from portfolio_manager.api.routes import history, portfolio
from portfolio_manager.services.error_handler import PortfolioError

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """Create and configure the FastAPI application.

    Replaces CICS PORTDFN.csd resource definitions and
    INQONLN main controller initialization.
    """
    app = FastAPI(
        title="Portfolio Management System",
        description=(
            "Investment Portfolio Management API. "
            "Migrated from COBOL/CICS (PINQ transaction)."
        ),
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    # CORS middleware
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Register routers (replaces CICS LINK routing in INQONLN)
    app.include_router(portfolio.router, prefix="/api/v1", tags=["Portfolio"])
    app.include_router(history.router, prefix="/api/v1", tags=["History"])

    # Global error handler (replaces EXEC CICS HANDLE CONDITION)
    @app.exception_handler(PortfolioError)
    async def portfolio_error_handler(
        request: Request, exc: PortfolioError
    ) -> JSONResponse:
        """Handle portfolio application errors.

        Replaces INQONLN P900-ERROR-ROUTINE and
        EXEC CICS HANDLE CONDITION ERROR patterns.
        """
        logger.error(
            "Portfolio error: code=%s program=%s message=%s",
            exc.error_code,
            exc.program_id,
            str(exc),
        )
        status_code = 400 if exc.severity <= 2 else 500
        return JSONResponse(
            status_code=status_code,
            content={
                "error": True,
                "error_code": exc.error_code,
                "message": str(exc),
                "program": exc.program_id,
            },
        )

    @app.exception_handler(Exception)
    async def general_error_handler(
        request: Request, exc: Exception
    ) -> JSONResponse:
        """Handle unexpected errors.

        Replaces CICS ABEND handling.
        """
        logger.exception("Unexpected error: %s", exc)
        return JSONResponse(
            status_code=500,
            content={
                "error": True,
                "error_code": "E999",
                "message": "Internal server error",
            },
        )

    @app.get("/health")
    async def health_check() -> dict[str, str]:
        """Health check endpoint."""
        return {"status": "healthy", "system": "CLBS-Python"}

    return app
