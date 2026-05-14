"""FastAPI application – Investment Portfolio Management System.

Modernized replacement for the COBOL Legacy Benchmark Suite.
Replaces:
  - INQONLN  (online inquiry controller)
  - INQPORT  (portfolio position inquiry)
  - INQHIST  (transaction history inquiry)
  - PORTMSTR (portfolio master CRUD)
  - TRNVAL00 (transaction validation)
  - RPTPOS00 / RPTAUD00 / RPTSTA00 (reporting)
"""

import uuid
from datetime import date, datetime
from pathlib import Path

from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from .database import Base, engine, get_db
from .models import Portfolio, Position, Transaction
from .schemas import (
    DashboardStats,
    PortfolioCreate,
    PortfolioDetail,
    PortfolioSummary,
    PortfolioUpdate,
    PositionOut,
    TransactionCreate,
    TransactionOut,
)
from .seed import seed_database

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Investment Portfolio Manager",
    description="Modernized from COBOL Legacy Benchmark Suite",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup() -> None:
    db = next(get_db())
    try:
        seed_database(db)
    finally:
        db.close()


# ---------------------------------------------------------------------------
# Dashboard – replaces RPTSTA00 (system statistics report)
# ---------------------------------------------------------------------------

@app.get("/api/dashboard", response_model=DashboardStats)
def get_dashboard(db: Session = Depends(get_db)) -> DashboardStats:
    portfolios = db.query(Portfolio).all()
    positions = db.query(Position).filter(Position.status == "A").all()
    transactions = db.query(Transaction).order_by(Transaction.transaction_date.desc()).limit(10).all()

    total_mv = sum(float(p.market_value) for p in positions)
    total_cb = sum(float(p.cost_basis) for p in positions)
    total_gl = total_mv - total_cb
    total_gl_pct = (total_gl / total_cb * 100) if total_cb else 0

    status_counts: dict[str, int] = {}
    for p in portfolios:
        label = {"A": "Active", "C": "Closed", "S": "Suspended"}.get(p.status, p.status)
        status_counts[label] = status_counts.get(label, 0) + 1

    type_counts: dict[str, int] = {}
    for p in portfolios:
        label = {"I": "Individual", "C": "Corporate", "T": "Trust"}.get(p.client_type, p.client_type)
        type_counts[label] = type_counts.get(label, 0) + 1

    top_performers = []
    for pos in sorted(positions, key=lambda p: float(p.market_value) - float(p.cost_basis), reverse=True)[:5]:
        gl = float(pos.market_value) - float(pos.cost_basis)
        gl_pct = (gl / float(pos.cost_basis) * 100) if float(pos.cost_basis) else 0
        top_performers.append({
            "investment_id": pos.investment_id,
            "investment_name": pos.investment_name,
            "market_value": float(pos.market_value),
            "gain_loss": round(gl, 2),
            "gain_loss_pct": round(gl_pct, 2),
        })

    return DashboardStats(
        total_portfolios=len(portfolios),
        active_portfolios=sum(1 for p in portfolios if p.status == "A"),
        total_market_value=round(total_mv, 2),
        total_cost_basis=round(total_cb, 2),
        total_gain_loss=round(total_gl, 2),
        total_gain_loss_pct=round(total_gl_pct, 2),
        total_positions=len(positions),
        total_transactions=db.query(Transaction).count(),
        recent_transactions=[_txn_out(t) for t in transactions],
        portfolio_breakdown=[{"name": k, "value": v} for k, v in type_counts.items()],
        status_breakdown=[{"name": k, "value": v} for k, v in status_counts.items()],
        top_performers=top_performers,
    )


# ---------------------------------------------------------------------------
# Portfolios – replaces PORTMSTR (CRUD) and INQPORT (inquiry)
# ---------------------------------------------------------------------------

@app.get("/api/portfolios", response_model=list[PortfolioSummary])
def list_portfolios(
    status: str | None = None,
    client_type: str | None = None,
    search: str | None = None,
    db: Session = Depends(get_db),
) -> list[PortfolioSummary]:
    q = db.query(Portfolio)
    if status:
        q = q.filter(Portfolio.status == status)
    if client_type:
        q = q.filter(Portfolio.client_type == client_type)
    if search:
        q = q.filter(
            (Portfolio.client_name.ilike(f"%{search}%"))
            | (Portfolio.portfolio_id.ilike(f"%{search}%"))
            | (Portfolio.account_no.ilike(f"%{search}%"))
        )
    portfolios = q.order_by(Portfolio.portfolio_id).all()
    return [_portfolio_summary(p, db) for p in portfolios]


@app.get("/api/portfolios/{portfolio_id}", response_model=PortfolioDetail)
def get_portfolio(portfolio_id: str, db: Session = Depends(get_db)) -> PortfolioDetail:
    portfolio = db.query(Portfolio).filter(Portfolio.portfolio_id == portfolio_id).first()
    if not portfolio:
        raise HTTPException(status_code=404, detail="Portfolio not found")

    positions = db.query(Position).filter(
        Position.portfolio_id == portfolio_id,
        Position.status == "A",
    ).all()

    transactions = db.query(Transaction).filter(
        Transaction.portfolio_id == portfolio_id,
    ).order_by(Transaction.transaction_date.desc()).limit(20).all()

    summary = _portfolio_summary(portfolio, db)
    return PortfolioDetail(
        **summary.model_dump(),
        positions=[_pos_out(p) for p in positions],
        recent_transactions=[_txn_out(t) for t in transactions],
    )


@app.post("/api/portfolios", response_model=PortfolioSummary, status_code=201)
def create_portfolio(data: PortfolioCreate, db: Session = Depends(get_db)) -> PortfolioSummary:
    existing = db.query(Portfolio).filter(Portfolio.portfolio_id == data.portfolio_id).first()
    if existing:
        raise HTTPException(status_code=409, detail="Portfolio ID already exists")

    portfolio = Portfolio(
        portfolio_id=data.portfolio_id,
        account_no=data.account_no,
        client_name=data.client_name,
        client_type=data.client_type,
        currency_code=data.currency_code,
        risk_level=data.risk_level,
        status="A",
        total_value=data.cash_balance,
        cash_balance=data.cash_balance,
        open_date=date.today(),
        last_maint_date=datetime.utcnow(),
        last_maint_user="WEBUSER",
    )
    db.add(portfolio)
    db.commit()
    db.refresh(portfolio)
    return _portfolio_summary(portfolio, db)


@app.put("/api/portfolios/{portfolio_id}", response_model=PortfolioSummary)
def update_portfolio(
    portfolio_id: str, data: PortfolioUpdate, db: Session = Depends(get_db)
) -> PortfolioSummary:
    portfolio = db.query(Portfolio).filter(Portfolio.portfolio_id == portfolio_id).first()
    if not portfolio:
        raise HTTPException(status_code=404, detail="Portfolio not found")

    if data.client_name is not None:
        portfolio.client_name = data.client_name
    if data.status is not None:
        portfolio.status = data.status
    if data.risk_level is not None:
        portfolio.risk_level = data.risk_level
    if data.cash_balance is not None:
        portfolio.cash_balance = data.cash_balance

    portfolio.last_maint_date = datetime.utcnow()
    portfolio.last_maint_user = "WEBUSER"
    db.commit()
    db.refresh(portfolio)
    return _portfolio_summary(portfolio, db)


@app.delete("/api/portfolios/{portfolio_id}", status_code=204)
def delete_portfolio(portfolio_id: str, db: Session = Depends(get_db)) -> None:
    portfolio = db.query(Portfolio).filter(Portfolio.portfolio_id == portfolio_id).first()
    if not portfolio:
        raise HTTPException(status_code=404, detail="Portfolio not found")
    portfolio.status = "C"
    portfolio.close_date = date.today()
    portfolio.last_maint_date = datetime.utcnow()
    db.commit()


# ---------------------------------------------------------------------------
# Positions – replaces INQPORT position inquiry
# ---------------------------------------------------------------------------

@app.get("/api/portfolios/{portfolio_id}/positions", response_model=list[PositionOut])
def list_positions(portfolio_id: str, db: Session = Depends(get_db)) -> list[PositionOut]:
    positions = db.query(Position).filter(Position.portfolio_id == portfolio_id).all()
    return [_pos_out(p) for p in positions]


# ---------------------------------------------------------------------------
# Transactions – replaces INQHIST (history inquiry) and TRNVAL00 (validation)
# ---------------------------------------------------------------------------

@app.get("/api/transactions", response_model=list[TransactionOut])
def list_transactions(
    portfolio_id: str | None = None,
    transaction_type: str | None = None,
    limit: int = Query(default=50, le=200),
    db: Session = Depends(get_db),
) -> list[TransactionOut]:
    q = db.query(Transaction)
    if portfolio_id:
        q = q.filter(Transaction.portfolio_id == portfolio_id)
    if transaction_type:
        q = q.filter(Transaction.transaction_type == transaction_type)
    txns = q.order_by(Transaction.transaction_date.desc()).limit(limit).all()
    return [_txn_out(t) for t in txns]


@app.post("/api/transactions", response_model=TransactionOut, status_code=201)
def create_transaction(data: TransactionCreate, db: Session = Depends(get_db)) -> TransactionOut:
    portfolio = db.query(Portfolio).filter(Portfolio.portfolio_id == data.portfolio_id).first()
    if not portfolio:
        raise HTTPException(status_code=404, detail="Portfolio not found")
    if portfolio.status != "A":
        raise HTTPException(status_code=400, detail="Portfolio is not active")

    txn_id = uuid.uuid4().hex
    if data.amount is not None:
        amount = round(data.amount, 2)
    else:
        amount = round(data.quantity * data.price, 2)

    txn = Transaction(
        transaction_id=txn_id,
        portfolio_id=data.portfolio_id,
        investment_id=data.investment_id,
        transaction_date=date.today(),
        transaction_type=data.transaction_type,
        quantity=data.quantity,
        price=data.price,
        amount=amount,
        currency_code="USD",
        status="D",
        process_date=datetime.utcnow(),
        process_user="WEBUSER",
    )
    db.add(txn)
    db.commit()
    db.refresh(txn)
    return _txn_out(txn)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _pos_out(p: Position) -> PositionOut:
    gl = float(p.market_value) - float(p.cost_basis)
    gl_pct = (gl / float(p.cost_basis) * 100) if float(p.cost_basis) else 0
    return PositionOut(
        id=p.id,
        portfolio_id=p.portfolio_id,
        investment_id=p.investment_id,
        investment_name=p.investment_name,
        position_date=p.position_date,
        quantity=float(p.quantity),
        cost_basis=float(p.cost_basis),
        market_value=float(p.market_value),
        gain_loss=round(gl, 2),
        gain_loss_pct=round(gl_pct, 2),
        currency_code=p.currency_code,
        status=p.status,
        last_maint_date=p.last_maint_date,
    )


def _txn_out(t: Transaction) -> TransactionOut:
    return TransactionOut(
        id=t.id,
        transaction_id=t.transaction_id,
        portfolio_id=t.portfolio_id,
        investment_id=t.investment_id,
        transaction_date=t.transaction_date,
        transaction_type=t.transaction_type,
        quantity=float(t.quantity),
        price=float(t.price),
        amount=float(t.amount),
        currency_code=t.currency_code,
        status=t.status,
        process_date=t.process_date,
    )


def _portfolio_summary(p: Portfolio, db: Session) -> PortfolioSummary:
    positions = db.query(Position).filter(
        Position.portfolio_id == p.portfolio_id,
        Position.status == "A",
    ).all()

    total_mv = sum(float(pos.market_value) for pos in positions)
    total_cb = sum(float(pos.cost_basis) for pos in positions)
    total_gl = total_mv - total_cb
    total_gl_pct = (total_gl / total_cb * 100) if total_cb else 0

    return PortfolioSummary(
        id=p.id,
        portfolio_id=p.portfolio_id,
        account_no=p.account_no,
        client_name=p.client_name,
        client_type=p.client_type,
        currency_code=p.currency_code,
        risk_level=p.risk_level,
        status=p.status,
        total_value=float(p.total_value),
        cash_balance=float(p.cash_balance),
        open_date=p.open_date,
        close_date=p.close_date,
        last_maint_date=p.last_maint_date,
        position_count=len(positions),
        total_market_value=round(total_mv, 2),
        total_cost_basis=round(total_cb, 2),
        total_gain_loss=round(total_gl, 2),
        total_gain_loss_pct=round(total_gl_pct, 2),
    )


# ---------------------------------------------------------------------------
# Static file serving for the React frontend (production)
# ---------------------------------------------------------------------------

FRONTEND_DIR = Path(__file__).resolve().parent.parent.parent / "frontend" / "dist"

if FRONTEND_DIR.exists():
    app.mount("/assets", StaticFiles(directory=str(FRONTEND_DIR / "assets")), name="static-assets")

    @app.get("/{full_path:path}")
    def serve_spa(full_path: str) -> FileResponse:
        file_path = (FRONTEND_DIR / full_path).resolve()
        if file_path.is_relative_to(FRONTEND_DIR) and file_path.is_file():
            return FileResponse(str(file_path))
        return FileResponse(str(FRONTEND_DIR / "index.html"))
