"""FastAPI application - replaces CICS online transaction processing.

Main application entry point replacing the CICS PINQ transaction
defined in PORTDFN.csd. Maps INQONLN.cbl functions to REST endpoints.

COBOL INQONLN functions (EVALUATE WS-COMM-FUNCTION):
- MENU: Display main menu -> GET /
- INQP: Portfolio inquiry -> GET /api/portfolios/{id}
- INQH: History inquiry -> GET /api/portfolios/{id}/history
- EXIT: End session -> POST /api/auth/logout
"""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from python_app.online.routers import auth_router, inquiry_router, reports_router

logger = logging.getLogger("portfolio.online.app")


def create_app() -> FastAPI:
    """Create and configure the FastAPI application.

    Replaces CICS resource definitions from PORTDFN.csd:
    - PROGRAM(INQONLN) -> FastAPI main app
    - TRANSACTION(PINQ) -> REST API endpoints
    - MAPSET(INQSET) -> JSON response schemas
    """
    app = FastAPI(
        title="Investment Portfolio Management System",
        description=(
            "REST API for portfolio inquiry and management. "
            "Migrated from COBOL/CICS online transaction processing system."
        ),
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    # CORS middleware (replaces CICS terminal access control)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Register routers
    app.include_router(auth_router, prefix="/api/auth", tags=["Authentication"])
    app.include_router(inquiry_router, prefix="/api", tags=["Inquiry"])
    app.include_router(reports_router, prefix="/api/reports", tags=["Reports"])

    @app.get("/", tags=["Menu"])
    async def menu() -> dict[str, str]:
        """Main menu - replaces INQONLN MENU function.

        COBOL: Displays INQMAP1 (main menu screen).
        """
        return {
            "system": "Investment Portfolio Management System",
            "version": "1.0.0",
            "endpoints": {
                "portfolio_inquiry": "/api/portfolios/{portfolio_id}",
                "position_inquiry": "/api/portfolios/{portfolio_id}/positions",
                "history_inquiry": "/api/portfolios/{portfolio_id}/history",
                "reports": "/api/reports/",
                "auth": "/api/auth/login",
            },
        }

    @app.get("/health", tags=["System"])
    async def health_check() -> dict[str, str]:
        """Health check endpoint."""
        return {"status": "healthy"}

    return app


# Application instance
app = create_app()
