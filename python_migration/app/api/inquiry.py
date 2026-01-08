"""Inquiry API endpoints - converted from INQONLN.cbl.

This module provides the main inquiry controller REST API endpoints,
replacing the CICS INQONLN program functionality.

COBOL Program Reference (INQONLN.cbl):
- Main CICS controller for online inquiries
- Manages screen flow and program dispatch
- Handles user session and navigation
"""

from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.database.models import AuditLog, PortfolioMaster
from app.utils.logging import get_logger

logger = get_logger(__name__)

router = APIRouter(prefix="/inquiry", tags=["Inquiry"])


class MenuOption(BaseModel):
    """Menu option for inquiry system."""

    code: str
    name: str
    description: str
    endpoint: str


class MenuResponse(BaseModel):
    """Menu response - replaces BMS menu screen."""

    title: str
    options: list[MenuOption]
    message: Optional[str] = None


class SearchRequest(BaseModel):
    """Search request model."""

    search_type: str = Field(..., description="Type of search (portfolio, client, investment)")
    search_value: str = Field(..., description="Search value")
    exact_match: bool = Field(False, description="Require exact match")


class SearchResult(BaseModel):
    """Search result item."""

    portfolio_id: str
    portfolio_name: str
    client_id: str
    status: str
    match_type: str


class SearchResponse(BaseModel):
    """Search response model."""

    results: list[SearchResult]
    total_count: int
    search_type: str
    search_value: str


@router.get(
    "/menu",
    response_model=MenuResponse,
    summary="Get inquiry menu",
    description="Get main inquiry menu - replaces INQONLN menu screen",
)
async def get_menu() -> MenuResponse:
    """Get main inquiry menu.

    This endpoint replaces the CICS INQONLN program's main menu
    screen (INQMENU in BMS).
    """
    options = [
        MenuOption(
            code="1",
            name="Portfolio Inquiry",
            description="View portfolio details and positions",
            endpoint="/api/v1/portfolio/{portfolio_id}",
        ),
        MenuOption(
            code="2",
            name="Transaction History",
            description="View transaction history for a portfolio",
            endpoint="/api/v1/history/transactions/{portfolio_id}",
        ),
        MenuOption(
            code="3",
            name="Position History",
            description="View position history from POSHIST",
            endpoint="/api/v1/history/positions/{portfolio_id}",
        ),
        MenuOption(
            code="4",
            name="Search",
            description="Search for portfolios",
            endpoint="/api/v1/inquiry/search",
        ),
    ]

    return MenuResponse(
        title="Investment Portfolio Inquiry System",
        options=options,
        message="Select an option to continue",
    )


@router.post(
    "/search",
    response_model=SearchResponse,
    summary="Search portfolios",
    description="Search for portfolios by various criteria",
)
async def search_portfolios(
    request: SearchRequest,
    db: Session = Depends(get_db),
) -> SearchResponse:
    """Search for portfolios.

    This endpoint provides search functionality similar to the
    CICS INQONLN program's search capabilities.
    """
    results = []

    if request.search_type == "portfolio":
        if request.exact_match:
            portfolios = (
                db.query(PortfolioMaster)
                .filter(PortfolioMaster.portfolio_id == request.search_value)
                .all()
            )
        else:
            portfolios = (
                db.query(PortfolioMaster)
                .filter(
                    PortfolioMaster.portfolio_id.like(f"%{request.search_value}%")
                )
                .limit(100)
                .all()
            )

        results = [
            SearchResult(
                portfolio_id=p.portfolio_id,
                portfolio_name=p.portfolio_name,
                client_id=p.client_id,
                status=p.status,
                match_type="portfolio_id",
            )
            for p in portfolios
        ]

    elif request.search_type == "client":
        if request.exact_match:
            portfolios = (
                db.query(PortfolioMaster)
                .filter(PortfolioMaster.client_id == request.search_value)
                .all()
            )
        else:
            portfolios = (
                db.query(PortfolioMaster)
                .filter(PortfolioMaster.client_id.like(f"%{request.search_value}%"))
                .limit(100)
                .all()
            )

        results = [
            SearchResult(
                portfolio_id=p.portfolio_id,
                portfolio_name=p.portfolio_name,
                client_id=p.client_id,
                status=p.status,
                match_type="client_id",
            )
            for p in portfolios
        ]

    elif request.search_type == "name":
        portfolios = (
            db.query(PortfolioMaster)
            .filter(PortfolioMaster.portfolio_name.ilike(f"%{request.search_value}%"))
            .limit(100)
            .all()
        )

        results = [
            SearchResult(
                portfolio_id=p.portfolio_id,
                portfolio_name=p.portfolio_name,
                client_id=p.client_id,
                status=p.status,
                match_type="portfolio_name",
            )
            for p in portfolios
        ]

    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid search type: {request.search_type}. Valid types: portfolio, client, name",
        )

    _log_search(db, request.search_type, request.search_value, len(results))

    return SearchResponse(
        results=results,
        total_count=len(results),
        search_type=request.search_type,
        search_value=request.search_value,
    )


@router.get(
    "/validate/{portfolio_id}",
    summary="Validate portfolio exists",
    description="Check if a portfolio exists - similar to INQONLN validation",
)
async def validate_portfolio(
    portfolio_id: str,
    db: Session = Depends(get_db),
) -> dict:
    """Validate that a portfolio exists.

    This endpoint provides validation similar to the CICS INQONLN
    program's portfolio validation before displaying details.
    """
    portfolio = (
        db.query(PortfolioMaster)
        .filter(PortfolioMaster.portfolio_id == portfolio_id)
        .first()
    )

    if not portfolio:
        return {
            "valid": False,
            "portfolio_id": portfolio_id,
            "message": "Portfolio not found",
        }

    return {
        "valid": True,
        "portfolio_id": portfolio_id,
        "portfolio_name": portfolio.portfolio_name,
        "status": portfolio.status,
        "message": "Portfolio found",
    }


@router.get(
    "/status",
    summary="Get inquiry system status",
    description="Get status of the inquiry system",
)
async def get_status(db: Session = Depends(get_db)) -> dict:
    """Get inquiry system status.

    This endpoint provides system status information similar to
    the CICS system status displays.
    """
    try:
        portfolio_count = db.query(PortfolioMaster).count()
        active_count = (
            db.query(PortfolioMaster)
            .filter(PortfolioMaster.status == "A")
            .count()
        )

        return {
            "status": "online",
            "system": "PINQ",
            "program": "INQONLN",
            "timestamp": datetime.now().isoformat(),
            "statistics": {
                "total_portfolios": portfolio_count,
                "active_portfolios": active_count,
            },
        }
    except Exception as e:
        logger.error(f"Status check failed: {e}")
        return {
            "status": "error",
            "system": "PINQ",
            "program": "INQONLN",
            "timestamp": datetime.now().isoformat(),
            "error": str(e),
        }


def _log_search(
    db: Session, search_type: str, search_value: str, result_count: int
) -> None:
    """Log search to audit log."""
    try:
        audit = AuditLog(
            timestamp=datetime.now(),
            user_id="API",
            program="INQONLN",
            access_type="SEARCH",
            action_status="SUCC",
            message=f"Search: {search_type}={search_value}, results={result_count}",
        )
        db.add(audit)
        db.commit()
    except Exception as e:
        logger.warning(f"Failed to log audit: {e}")
        db.rollback()
