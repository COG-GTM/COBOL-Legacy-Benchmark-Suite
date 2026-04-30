from fastapi import APIRouter
from models.database import SessionLocal, Portfolio
from models.transactions import Transaction
from models.portfolio import TransactionItem
from routers.portfolio import _build_portfolio_summary, _generate_mock_portfolio
from datetime import datetime

router = APIRouter(prefix="/api", tags=["dashboard"])

COLORS = ["#06402B", "#0D7B51", "#15B878", "#4CD4A1", "#A3E8CF", "#1E3A5F", "#2D6A9F", "#6CA6CD"]


@router.get("/dashboard/{account_number}")
async def get_dashboard(account_number: str):
    """Get dashboard data: portfolio, transactions, allocation, and performance."""
    session = SessionLocal()
    try:
        portfolio_obj = (
            session.query(Portfolio)
            .filter(Portfolio.account_no == account_number)
            .first()
        )

        if portfolio_obj:
            summary = _build_portfolio_summary(portfolio_obj)
            txns = (
                session.query(Transaction)
                .filter(Transaction.portfolio_id == portfolio_obj.port_id)
                .order_by(Transaction.date.desc(), Transaction.time.desc())
                .all()
            )
            tx_list = [TransactionItem(**t.to_dict()) for t in txns]
        else:
            summary = _generate_mock_portfolio(account_number)
            tx_list = []

        allocation_data = []
        for i, h in enumerate(summary.holdings):
            allocation_data.append({
                "name": h.symbol,
                "value": h.marketValue,
                "color": COLORS[i % len(COLORS)],
            })

        total_val = summary.totalValue
        performance_data = _generate_performance_series(total_val)

        return {
            "accountNumber": account_number,
            "portfolio": summary,
            "transactions": tx_list,
            "allocationData": allocation_data,
            "performanceData": performance_data,
        }
    finally:
        session.close()


def _generate_performance_series(current_value: float) -> list[dict]:
    months = ["Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun"]
    factors = [0.82, 0.85, 0.88, 0.90, 0.87, 0.91, 0.93, 0.95, 0.92, 0.96, 0.98, 1.00]
    return [{"month": m, "value": round(current_value * f, 2)} for m, f in zip(months, factors)]
