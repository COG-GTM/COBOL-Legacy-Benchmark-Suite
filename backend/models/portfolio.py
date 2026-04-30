from pydantic import BaseModel
from typing import List


class PortfolioHolding(BaseModel):
    symbol: str
    name: str
    shares: int
    currentPrice: float
    marketValue: float
    gainLoss: float
    gainLossPercent: float


class PortfolioSummary(BaseModel):
    accountNumber: str
    totalValue: float
    totalGainLoss: float
    totalGainLossPercent: float
    holdings: List[PortfolioHolding]
    lastUpdated: str


class AccountValidationResponse(BaseModel):
    valid: bool
    message: str


class TransactionItem(BaseModel):
    date: str
    time: str
    portfolioId: str
    sequenceNo: str
    investmentId: str
    type: str
    typeName: str
    quantity: float
    price: float
    amount: float
    currency: str
    status: str
    statusName: str
    processDate: str | None
    processUser: str


class TransactionResponse(BaseModel):
    accountNumber: str
    transactions: List[TransactionItem]
    message: str
