from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from models.database import get_db
from schemas.portfolio import (
    PortfolioCreate, PortfolioUpdate, PortfolioResponse,
    PortfolioListResponse, PortfolioDetailResponse, PositionSummary,
)
from services import portfolio_service

router = APIRouter(prefix="/api/portfolios", tags=["portfolios"])


@router.get("", response_model=PortfolioListResponse)
def list_portfolios(
    status: str | None = Query(None, pattern=r"^[ACS]$"),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=500),
    db: Session = Depends(get_db),
):
    portfolios, total = portfolio_service.list_portfolios(db, status, skip, limit)
    return PortfolioListResponse(
        portfolios=[PortfolioResponse.model_validate(p) for p in portfolios],
        total=total,
    )


@router.get("/{portfolio_id}", response_model=PortfolioDetailResponse)
def get_portfolio(portfolio_id: str, db: Session = Depends(get_db)):
    detail = portfolio_service.get_portfolio_detail(db, portfolio_id)
    if not detail:
        raise HTTPException(status_code=404, detail="Portfolio not found")

    portfolio = detail["portfolio"]
    positions = detail["positions"]

    return PortfolioDetailResponse(
        **PortfolioResponse.model_validate(portfolio).model_dump(),
        positions=[
            PositionSummary(
                investment_id=p.investment_id,
                symbol=p.symbol,
                name=p.name,
                quantity=float(p.quantity or 0),
                cost_basis=float(p.cost_basis or 0),
                current_price=float(p.current_price or 0),
                market_value=float(p.market_value or 0),
                gain_loss=p.gain_loss,
                gain_loss_percent=p.gain_loss_percent,
                status=p.status,
            )
            for p in positions
        ],
        total_gain_loss=detail["total_gain_loss"],
        total_gain_loss_percent=detail["total_gain_loss_percent"],
        position_count=detail["position_count"],
    )


@router.post("", response_model=PortfolioResponse, status_code=201)
def create_portfolio(data: PortfolioCreate, db: Session = Depends(get_db)):
    existing = portfolio_service.get_portfolio(db, data.portfolio_id)
    if existing:
        raise HTTPException(status_code=409, detail="Portfolio ID already exists")
    portfolio = portfolio_service.create_portfolio(db, data)
    return PortfolioResponse.model_validate(portfolio)


@router.put("/{portfolio_id}", response_model=PortfolioResponse)
def update_portfolio(portfolio_id: str, data: PortfolioUpdate, db: Session = Depends(get_db)):
    portfolio = portfolio_service.update_portfolio(db, portfolio_id, data)
    if not portfolio:
        raise HTTPException(status_code=404, detail="Portfolio not found")
    return PortfolioResponse.model_validate(portfolio)


@router.delete("/{portfolio_id}", status_code=204)
def delete_portfolio(portfolio_id: str, db: Session = Depends(get_db)):
    success = portfolio_service.delete_portfolio(db, portfolio_id)
    if not success:
        raise HTTPException(status_code=404, detail="Portfolio not found")
