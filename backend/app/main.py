from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import portfolio, accounts, dashboard

app = FastAPI(
    title="CLBS Portfolio Management API",
    description="FastAPI backend for the modernized COBOL Legacy Benchmark Suite",
    version="2.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(portfolio.router)
app.include_router(accounts.router)
app.include_router(dashboard.router)


@app.get("/healthz")
async def healthz():
    return {"status": "ok"}
