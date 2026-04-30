"""Initial schema with portfolios, positions, and transactions.

Revision ID: 001
Revises:
Create Date: 2024-01-01 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "portfolios",
        sa.Column("port_id", sa.String(8), primary_key=True),
        sa.Column("account_no", sa.String(10), primary_key=True),
        sa.Column("client_name", sa.String(30)),
        sa.Column("client_type", sa.String(1)),
        sa.Column("create_date", sa.Date),
        sa.Column("last_maint", sa.Date),
        sa.Column("status", sa.String(1)),
        sa.Column("total_value", sa.Numeric(15, 2)),
        sa.Column("cash_balance", sa.Numeric(15, 2)),
        sa.Column("last_user", sa.String(8)),
        sa.Column("last_trans", sa.String(8)),
        sa.CheckConstraint("client_type IN ('I', 'C', 'T')"),
        sa.CheckConstraint("status IN ('A', 'C', 'S')"),
    )
    op.create_index("idx_portfolio_status", "portfolios", ["status"])
    op.create_index("idx_portfolio_client_type", "portfolios", ["client_type"])

    op.create_table(
        "positions",
        sa.Column("portfolio_id", sa.String(8), primary_key=True),
        sa.Column("date", sa.Date, primary_key=True),
        sa.Column("investment_id", sa.String(10), primary_key=True),
        sa.Column("quantity", sa.Numeric(15, 4)),
        sa.Column("cost_basis", sa.Numeric(15, 2)),
        sa.Column("market_value", sa.Numeric(15, 2)),
        sa.Column("currency", sa.String(3)),
        sa.Column("status", sa.String(1)),
        sa.Column("last_maint_date", sa.DateTime),
        sa.Column("last_maint_user", sa.String(8)),
        sa.ForeignKeyConstraint(["portfolio_id"], ["portfolios.port_id"]),
        sa.CheckConstraint("status IN ('A', 'C', 'P')"),
    )
    op.create_index("idx_position_portfolio_id", "positions", ["portfolio_id"])
    op.create_index("idx_position_date", "positions", ["date"])
    op.create_index("idx_position_investment_id", "positions", ["investment_id"])
    op.create_index("idx_position_status", "positions", ["status"])

    op.create_table(
        "transactions",
        sa.Column("date", sa.Date, primary_key=True),
        sa.Column("time", sa.Time, primary_key=True),
        sa.Column("portfolio_id", sa.String(8), primary_key=True),
        sa.Column("sequence_no", sa.String(6), primary_key=True),
        sa.Column("investment_id", sa.String(10)),
        sa.Column("type", sa.String(2)),
        sa.Column("quantity", sa.Numeric(15, 4)),
        sa.Column("price", sa.Numeric(15, 4)),
        sa.Column("amount", sa.Numeric(15, 2)),
        sa.Column("currency", sa.String(3)),
        sa.Column("status", sa.String(1)),
        sa.Column("process_date", sa.DateTime),
        sa.Column("process_user", sa.String(8)),
        sa.ForeignKeyConstraint(["portfolio_id"], ["portfolios.port_id"]),
        sa.CheckConstraint("type IN ('BU', 'SL', 'TR', 'FE')"),
        sa.CheckConstraint("status IN ('P', 'D', 'F', 'R')"),
    )
    op.create_index("idx_transaction_portfolio_id", "transactions", ["portfolio_id"])
    op.create_index("idx_transaction_date", "transactions", ["date"])
    op.create_index("idx_transaction_investment_id", "transactions", ["investment_id"])
    op.create_index("idx_transaction_type", "transactions", ["type"])
    op.create_index("idx_transaction_status", "transactions", ["status"])


def downgrade() -> None:
    op.drop_table("transactions")
    op.drop_table("positions")
    op.drop_table("portfolios")
