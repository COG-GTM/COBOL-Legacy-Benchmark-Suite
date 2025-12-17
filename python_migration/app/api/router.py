"""
API Router - Main router that combines all API endpoints.
Replaces INQONLN.cbl main controller functionality.
"""

from fastapi import APIRouter

from app.api.endpoints import auth, health, portfolios, transactions

api_router = APIRouter()

api_router.include_router(health.router, prefix="/health", tags=["health"])
api_router.include_router(auth.router, prefix="/auth", tags=["authentication"])
api_router.include_router(portfolios.router, prefix="/portfolios", tags=["portfolios"])
api_router.include_router(transactions.router, prefix="/transactions", tags=["transactions"])
