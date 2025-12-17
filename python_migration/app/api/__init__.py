"""
API endpoints for the Portfolio Management System.
These endpoints replace CICS transactions with REST API.
"""

from app.api.router import api_router

__all__ = ["api_router"]
