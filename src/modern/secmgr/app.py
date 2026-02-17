"""
FastAPI application entry point for the Security Manager service.

This module wires together the database, API routes, and middleware.
Run with: uvicorn secmgr.app:app --reload
"""

from __future__ import annotations

import logging

from fastapi import FastAPI

from .api import router as security_router
from .database import init_db

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)

app = FastAPI(
    title="SECMGR - Security Manager Service",
    description=(
        "Modernized Python implementation of the COBOL SECMGR program. "
        "Provides user validation, authorization, and audit logging "
        "via a REST API."
    ),
    version="1.0.0",
)

app.include_router(security_router)


@app.on_event("startup")
def on_startup() -> None:
    init_db()
