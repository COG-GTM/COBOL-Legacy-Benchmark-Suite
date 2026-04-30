import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from models.database import engine, Base
from routers import portfolios, transactions, reports

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Investment Portfolio Management API",
    description="Modernized from COBOL Legacy Benchmark Suite — full investment portfolio management with transaction processing, position tracking, and reporting.",
    version="2.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(portfolios.router)
app.include_router(transactions.router)
app.include_router(reports.router)


@app.get("/healthz")
async def healthz():
    return {"status": "ok", "version": "2.0.0"}
