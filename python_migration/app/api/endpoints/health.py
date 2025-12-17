"""
Health check endpoints.
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("")
async def health_check() -> dict:
    """Health check endpoint."""
    return {"status": "healthy", "service": "portfolio-management-system"}


@router.get("/ready")
async def readiness_check() -> dict:
    """Readiness check endpoint."""
    return {"status": "ready"}
