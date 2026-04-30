"""Report service — translated from RPTPOS00, RPTAUD00, RPTSTA00."""

from datetime import datetime, date
from sqlalchemy.orm import Session
from sqlalchemy import func
from models.portfolio import Portfolio
from models.position import Position
from models.transaction import Transaction
from models.error_log import ErrorLog


def generate_position_report(db: Session):
    """Generate daily position report (RPTPOS00 logic)."""
    positions = (
        db.query(Position, Portfolio)
        .join(Portfolio, Position.portfolio_id == Portfolio.portfolio_id)
        .filter(Position.status == "A")
        .all()
    )

    items = []
    total_market = 0.0
    total_cost = 0.0

    for pos, port in positions:
        mv = float(pos.market_value or 0)
        cb = float(pos.cost_basis or 0)
        gl = mv - cb
        gl_pct = (gl / cb * 100) if cb > 0 else 0
        total_market += mv
        total_cost += cb
        items.append({
            "portfolio_id": pos.portfolio_id,
            "portfolio_name": port.portfolio_name,
            "investment_id": pos.investment_id,
            "symbol": pos.symbol,
            "name": pos.name,
            "quantity": float(pos.quantity or 0),
            "cost_basis": round(cb, 2),
            "market_value": round(mv, 2),
            "gain_loss": round(gl, 2),
            "gain_loss_percent": round(gl_pct, 2),
        })

    portfolio_ids = set(i["portfolio_id"] for i in items)
    return {
        "report_date": datetime.utcnow(),
        "report_type": "DAILY_POSITION",
        "total_portfolios": len(portfolio_ids),
        "total_positions": len(items),
        "total_market_value": round(total_market, 2),
        "total_cost_basis": round(total_cost, 2),
        "total_gain_loss": round(total_market - total_cost, 2),
        "items": items,
    }


def generate_audit_report(db: Session, limit: int = 100):
    """Generate audit report (RPTAUD00 logic)."""
    entries = db.query(ErrorLog).order_by(ErrorLog.timestamp.desc()).limit(limit).all()

    error_count = sum(1 for e in entries if e.severity == "ERROR")
    warning_count = sum(1 for e in entries if e.severity == "WARNING")

    return {
        "report_date": datetime.utcnow(),
        "report_type": "AUDIT",
        "total_entries": len(entries),
        "error_count": error_count,
        "warning_count": warning_count,
        "entries": [
            {
                "timestamp": e.timestamp,
                "program_id": e.program_id,
                "error_code": e.error_code,
                "account_number": e.account_number,
                "portfolio_id": e.portfolio_id,
                "description": e.error_description,
                "severity": e.severity,
            }
            for e in entries
        ],
    }


def generate_statistics(db: Session):
    """Generate system statistics (RPTSTA00 logic)."""
    total_portfolios = db.query(Portfolio).count()
    active_portfolios = db.query(Portfolio).filter(Portfolio.status == "A").count()
    total_positions = db.query(Position).filter(Position.status == "A").count()
    total_transactions = db.query(Transaction).count()

    today = date.today()
    transactions_today = (
        db.query(Transaction)
        .filter(Transaction.transaction_date == today)
        .count()
    )

    total_market = db.query(func.sum(Position.market_value)).filter(Position.status == "A").scalar() or 0
    total_cost = db.query(func.sum(Position.cost_basis)).filter(Position.status == "A").scalar() or 0
    avg_value = float(total_market) / active_portfolios if active_portfolios > 0 else 0

    return {
        "report_date": datetime.utcnow(),
        "total_portfolios": total_portfolios,
        "active_portfolios": active_portfolios,
        "total_positions": total_positions,
        "total_transactions": total_transactions,
        "transactions_today": transactions_today,
        "total_market_value": round(float(total_market), 2),
        "total_gain_loss": round(float(total_market) - float(total_cost), 2),
        "avg_portfolio_value": round(avg_value, 2),
        "system_health": "HEALTHY",
    }
