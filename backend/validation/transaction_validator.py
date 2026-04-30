"""Transaction validation — translated from TRNVAL00.cbl business rules."""

from decimal import Decimal
from datetime import date
from typing import Optional
from sqlalchemy.orm import Session
from models.portfolio import Portfolio
from models.position import Position
from models.transaction import Transaction


class ValidationResult:
    def __init__(self):
        self.errors: list[str] = []
        self.warnings: list[str] = []

    @property
    def is_valid(self) -> bool:
        return len(self.errors) == 0

    @property
    def return_code(self) -> str:
        if not self.errors and not self.warnings:
            return "0000"
        if not self.errors and self.warnings:
            return "0004"
        return "0008"


def validate_transaction(
    db: Session,
    portfolio_id: str,
    investment_id: str,
    transaction_type: str,
    quantity: float,
    price: float,
    transaction_date: Optional[date] = None,
) -> ValidationResult:
    """Validate a transaction following TRNVAL00 rules."""
    result = ValidationResult()

    if not portfolio_id or len(portfolio_id) != 8:
        result.errors.append("E001: Portfolio ID must be exactly 8 characters")

    if not investment_id or len(investment_id) < 1 or len(investment_id) > 10:
        result.errors.append("E002: Investment ID must be 1-10 characters")

    if transaction_type not in ("BU", "SL", "TR", "FE"):
        result.errors.append("E003: Invalid transaction type. Must be BU, SL, TR, or FE")

    portfolio = db.query(Portfolio).filter(Portfolio.portfolio_id == portfolio_id).first()
    if not portfolio:
        result.errors.append("E001: Portfolio not found")
        return result

    if portfolio.status != "A":
        result.errors.append("E001: Portfolio is not active")

    if transaction_type in ("BU", "SL"):
        if quantity <= 0:
            result.errors.append("E004: Quantity must be positive for buy/sell transactions")
        if price <= 0:
            result.errors.append("E004: Price must be positive for buy/sell transactions")

    if transaction_type == "FE":
        amount = Decimal(str(quantity)) * Decimal(str(price))
        if amount == 0:
            result.warnings.append("W001: Zero dollar fee transaction")

    if transaction_type == "SL":
        position = (
            db.query(Position)
            .filter(
                Position.portfolio_id == portfolio_id,
                Position.investment_id == investment_id,
                Position.status == "A",
            )
            .first()
        )
        if not position:
            result.errors.append("E004: No active position found for this investment")
        elif float(position.quantity) < quantity:
            result.errors.append("E004: Insufficient position balance for sell")

    if transaction_date and transaction_date > date.today():
        result.errors.append("E003: Transaction date cannot be in the future")

    amount = Decimal(str(quantity)) * Decimal(str(price))
    if amount == 0 and transaction_type in ("BU", "SL"):
        result.warnings.append("W001: Zero dollar transaction")

    return result
