"""Portfolio service — business logic translated from PORTMSTR.cbl."""

from sqlalchemy.orm import Session
from models.portfolio import Portfolio
from models.position import Position
from schemas.portfolio import PortfolioCreate, PortfolioUpdate
from datetime import datetime


def list_portfolios(db: Session, status: str | None = None, skip: int = 0, limit: int = 100):
    query = db.query(Portfolio)
    if status:
        query = query.filter(Portfolio.status == status)
    total = query.count()
    portfolios = query.offset(skip).limit(limit).all()
    return portfolios, total


def get_portfolio(db: Session, portfolio_id: str) -> Portfolio | None:
    return db.query(Portfolio).filter(Portfolio.portfolio_id == portfolio_id).first()


def get_portfolio_detail(db: Session, portfolio_id: str):
    portfolio = get_portfolio(db, portfolio_id)
    if not portfolio:
        return None

    positions = (
        db.query(Position)
        .filter(Position.portfolio_id == portfolio_id, Position.status == "A")
        .all()
    )

    total_market = sum(float(p.market_value or 0) for p in positions)
    total_cost = sum(float(p.cost_basis or 0) for p in positions)
    total_gl = total_market - total_cost
    total_gl_pct = (total_gl / total_cost * 100) if total_cost > 0 else 0

    return {
        "portfolio": portfolio,
        "positions": positions,
        "total_gain_loss": round(total_gl, 2),
        "total_gain_loss_percent": round(total_gl_pct, 2),
        "position_count": len(positions),
    }


def create_portfolio(db: Session, data: PortfolioCreate) -> Portfolio:
    portfolio = Portfolio(
        portfolio_id=data.portfolio_id,
        account_number=data.account_number,
        client_name=data.client_name,
        client_type=data.client_type,
        portfolio_name=data.portfolio_name,
        currency_code=data.currency_code,
        risk_level=data.risk_level,
    )
    db.add(portfolio)
    db.commit()
    db.refresh(portfolio)
    return portfolio


def update_portfolio(db: Session, portfolio_id: str, data: PortfolioUpdate) -> Portfolio | None:
    portfolio = get_portfolio(db, portfolio_id)
    if not portfolio:
        return None
    update_data = data.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(portfolio, key, value)
    portfolio.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(portfolio)
    return portfolio


def delete_portfolio(db: Session, portfolio_id: str) -> bool:
    portfolio = get_portfolio(db, portfolio_id)
    if not portfolio:
        return False
    portfolio.status = "C"
    portfolio.close_date = datetime.utcnow().date()
    portfolio.updated_at = datetime.utcnow()
    db.commit()
    return True
