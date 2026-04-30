from fastapi import APIRouter
from models.portfolio import PortfolioSummary, PortfolioHolding, TransactionResponse, TransactionItem
from models.database import SessionLocal, Portfolio, Position
from models.transactions import Transaction
from datetime import datetime

router = APIRouter(prefix="/api", tags=["portfolio"])

INVESTMENT_NAMES = {
    "AAPL": "Apple Inc.",
    "MSFT": "Microsoft Corporation",
    "GOOGL": "Alphabet Inc.",
    "TSLA": "Tesla Inc.",
    "AMZN": "Amazon.com Inc.",
    "NVDA": "NVIDIA Corporation",
    "JPM": "JPMorgan Chase & Co.",
    "V": "Visa Inc.",
}


def _build_portfolio_summary(portfolio: Portfolio) -> PortfolioSummary:
    holdings = []
    total_market = 0.0
    total_cost = 0.0

    for pos in portfolio.positions:
        if pos.status != "A":
            continue
        gl = pos.calculate_gain_loss()
        mv = float(pos.market_value or 0)
        cb = float(pos.cost_basis or 0)
        total_market += mv
        total_cost += cb

        current_price = mv / float(pos.quantity) if pos.quantity else 0.0

        holdings.append(
            PortfolioHolding(
                symbol=pos.investment_id,
                name=INVESTMENT_NAMES.get(pos.investment_id, pos.investment_id),
                shares=int(pos.quantity or 0),
                currentPrice=round(current_price, 2),
                marketValue=mv,
                gainLoss=float(gl["gain_loss"]),
                gainLossPercent=round(float(gl["gain_loss_percent"]), 2),
            )
        )

    total_gl = total_market - total_cost
    total_gl_pct = (total_gl / total_cost * 100) if total_cost else 0.0

    return PortfolioSummary(
        accountNumber=portfolio.account_no,
        totalValue=round(total_market, 2),
        totalGainLoss=round(total_gl, 2),
        totalGainLossPercent=round(total_gl_pct, 2),
        holdings=holdings,
        lastUpdated=datetime.now().strftime("%B %d, %Y, %I:%M %p"),
    )


@router.get("/portfolio/{account_number}", response_model=PortfolioSummary)
async def get_portfolio(account_number: str):
    """Get portfolio summary and holdings for an account."""
    session = SessionLocal()
    try:
        portfolio = (
            session.query(Portfolio)
            .filter(Portfolio.account_no == account_number)
            .first()
        )
        if portfolio:
            return _build_portfolio_summary(portfolio)

        # Fall back to mock data if not in DB
        return _generate_mock_portfolio(account_number)
    finally:
        session.close()


@router.get("/transactions/{account_number}", response_model=TransactionResponse)
async def get_transactions(account_number: str):
    """Get transaction history for an account."""
    session = SessionLocal()
    try:
        portfolio = (
            session.query(Portfolio)
            .filter(Portfolio.account_no == account_number)
            .first()
        )
        if portfolio:
            txns = (
                session.query(Transaction)
                .filter(Transaction.portfolio_id == portfolio.port_id)
                .order_by(Transaction.date.desc(), Transaction.time.desc())
                .all()
            )
            return TransactionResponse(
                accountNumber=account_number,
                transactions=[TransactionItem(**t.to_dict()) for t in txns],
                message=f"Found {len(txns)} transactions",
            )

        return TransactionResponse(
            accountNumber=account_number,
            transactions=[],
            message="No portfolio found for this account",
        )
    finally:
        session.close()


def _generate_mock_portfolio(account_number: str) -> PortfolioSummary:
    holdings = [
        PortfolioHolding(symbol="AAPL", name="Apple Inc.", shares=150, currentPrice=185.25, marketValue=27787.50, gainLoss=2287.50, gainLossPercent=8.97),
        PortfolioHolding(symbol="MSFT", name="Microsoft Corporation", shares=100, currentPrice=378.85, marketValue=37885.00, gainLoss=3885.00, gainLossPercent=11.42),
        PortfolioHolding(symbol="GOOGL", name="Alphabet Inc.", shares=75, currentPrice=142.56, marketValue=10692.00, gainLoss=692.00, gainLossPercent=6.92),
        PortfolioHolding(symbol="TSLA", name="Tesla Inc.", shares=200, currentPrice=245.67, marketValue=49134.00, gainLoss=1386.00, gainLossPercent=2.90),
    ]
    return PortfolioSummary(
        accountNumber=account_number,
        totalValue=125498.50,
        totalGainLoss=8250.50,
        totalGainLossPercent=7.03,
        holdings=holdings,
        lastUpdated=datetime.now().strftime("%B %d, %Y, %I:%M %p"),
    )
