"""Main FastAPI application - replaces CICS PORTDFN.csd resource definitions.

This module provides the main FastAPI application that replaces
the CICS online transaction processing system.

CICS Resource Definitions Replaced (PORTDFN.csd):
- PINQ transaction -> /api/v1/inquiry endpoints
- INQONLN program -> InquiryController
- INQPORT program -> PortfolioAPI
- INQHIST program -> HistoryAPI
- SECMGR program -> AuthAPI
"""

import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.templating import Jinja2Templates

from app.api.auth import router as auth_router
from app.api.history import router as history_router
from app.api.inquiry import router as inquiry_router
from app.api.portfolio import router as portfolio_router
from app.database.connection import init_db
from app.utils.logging import get_logger, setup_logging

logger = get_logger(__name__)

TEMPLATES_DIR = os.path.join(os.path.dirname(__file__), "templates")
templates = Jinja2Templates(directory=TEMPLATES_DIR)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    setup_logging(
        level=os.getenv("LOG_LEVEL", "INFO"),
        json_format=os.getenv("LOG_FORMAT", "text") == "json",
    )
    logger.info("Starting Investment Portfolio Management System")

    init_db()
    logger.info("Database initialized")

    yield

    logger.info("Shutting down Investment Portfolio Management System")


app = FastAPI(
    title="Investment Portfolio Management System",
    description="""
    Python migration of the COBOL Legacy Benchmark Suite.

    This API replaces the CICS online transaction processing system
    for portfolio inquiries, transaction history, and user authentication.

    ## Features

    * **Portfolio Inquiry** - View portfolio details and positions (replaces INQPORT)
    * **Transaction History** - View transaction and position history (replaces INQHIST)
    * **Search** - Search for portfolios by various criteria (replaces INQONLN)
    * **Authentication** - JWT-based authentication (replaces SECMGR/RACF)

    ## COBOL Programs Replaced

    | COBOL Program | Python Module |
    |---------------|---------------|
    | INQONLN | app.api.inquiry |
    | INQPORT | app.api.portfolio |
    | INQHIST | app.api.history |
    | SECMGR | app.auth.security |

    ## Batch Programs Replaced

    | COBOL Program | Python Module |
    |---------------|---------------|
    | TRNVAL00 | app.batch.transaction_validator |
    | POSUPD00 | app.batch.position_updater |
    | HISTLD00 | app.batch.history_loader |
    | BCHCTL00 | app.batch.batch_controller |
    | PRCSEQ00 | app.batch.process_sequencer |
    | RCVPRC00 | app.batch.recovery_handler |
    """,
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router, prefix="/api/v1")
app.include_router(portfolio_router, prefix="/api/v1")
app.include_router(history_router, prefix="/api/v1")
app.include_router(inquiry_router, prefix="/api/v1")


@app.get("/", response_class=HTMLResponse, include_in_schema=False)
async def root(request: Request):
    """Root endpoint - serves main menu page."""
    return templates.TemplateResponse(
        "index.html",
        {
            "request": request,
            "title": "Investment Portfolio Management System",
            "system_id": "PORTMGMT",
        },
    )


@app.get("/health", tags=["System"])
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "system": "PORTMGMT",
        "version": "1.0.0",
    }


@app.get("/api/v1/system/info", tags=["System"])
async def system_info():
    """System information endpoint - similar to CICS system status."""
    return {
        "system_id": "PORTMGMT",
        "version": "1.0.0",
        "description": "Investment Portfolio Management System",
        "migration_source": "COBOL Legacy Benchmark Suite",
        "api_version": "v1",
        "endpoints": {
            "portfolio": "/api/v1/portfolio",
            "history": "/api/v1/history",
            "inquiry": "/api/v1/inquiry",
            "auth": "/api/v1/auth",
        },
        "documentation": {
            "swagger": "/docs",
            "redoc": "/redoc",
        },
    }


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler - similar to CICS ERRHNDL."""
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": True,
            "message": "Internal server error",
            "detail": str(exc) if os.getenv("DEBUG", "false").lower() == "true" else None,
        },
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", "8000")),
        reload=os.getenv("DEBUG", "false").lower() == "true",
    )
