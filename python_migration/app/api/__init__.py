"""FastAPI REST API endpoints converted from CICS online programs.

This module provides REST API endpoints that replace the CICS
online transaction processing programs.

Programs Converted:
- INQONLN -> inquiry.py
- INQPORT -> portfolio.py
- INQHIST -> history.py
- SECMGR -> auth.py
"""

from app.api.auth import router as auth_router
from app.api.history import router as history_router
from app.api.inquiry import router as inquiry_router
from app.api.portfolio import router as portfolio_router

__all__ = [
    "auth_router",
    "history_router",
    "inquiry_router",
    "portfolio_router",
]
