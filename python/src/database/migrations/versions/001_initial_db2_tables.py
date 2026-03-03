"""Initial DB2 table migration to PostgreSQL.

Revision ID: 001
Revises: None
Create Date: 2024-01-01 00:00:00.000000

Migrates all DB2 table definitions:
  - portfolio_master
  - investment_positions
  - transaction_history
  - poshist
  - errlog
  - rtncodes
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # --- portfolio_master ---
    op.create_table(
        "portfolio_master",
        sa.Column("portfolio_id", sa.String(8), primary_key=True),
        sa.Column("account_type", sa.String(2), nullable=False),
        sa.Column("branch_id", sa.String(2), nullable=False),
        sa.Column("client_id", sa.String(10), nullable=False),
        sa.Column("portfolio_name", sa.String(50), nullable=False),
        sa.Column("currency_code", sa.String(3), nullable=False),
        sa.Column("risk_level", sa.String(1), nullable=False),
        sa.Column("status", sa.String(1), nullable=False),
        sa.Column("open_date", sa.Date, nullable=False),
        sa.Column("close_date", sa.Date, nullable=True),
        sa.Column("last_maint_date", sa.DateTime, nullable=False),
        sa.Column("last_maint_user", sa.String(8), nullable=False),
    )
    op.create_index(
        "idx_port_master_client", "portfolio_master", ["client_id", "status"]
    )

    # --- investment_positions ---
    op.create_table(
        "investment_positions",
        sa.Column(
            "portfolio_id",
            sa.String(8),
            sa.ForeignKey("portfolio_master.portfolio_id"),
            primary_key=True,
        ),
        sa.Column("investment_id", sa.String(10), primary_key=True),
        sa.Column("position_date", sa.Date, primary_key=True),
        sa.Column(
            "quantity", sa.Numeric(precision=18, scale=4), nullable=False
        ),
        sa.Column(
            "cost_basis", sa.Numeric(precision=18, scale=2), nullable=False
        ),
        sa.Column(
            "market_value", sa.Numeric(precision=18, scale=2), nullable=False
        ),
        sa.Column("currency_code", sa.String(3), nullable=False),
        sa.Column("last_maint_date", sa.DateTime, nullable=False),
        sa.Column("last_maint_user", sa.String(8), nullable=False),
    )
    op.create_index(
        "idx_positions_date",
        "investment_positions",
        ["position_date", "portfolio_id"],
    )

    # --- transaction_history ---
    op.create_table(
        "transaction_history",
        sa.Column("transaction_id", sa.String(20), primary_key=True),
        sa.Column(
            "portfolio_id",
            sa.String(8),
            sa.ForeignKey("portfolio_master.portfolio_id"),
            nullable=False,
        ),
        sa.Column("transaction_date", sa.Date, nullable=False),
        sa.Column("transaction_time", sa.Time, nullable=False),
        sa.Column("investment_id", sa.String(10), nullable=False),
        sa.Column("transaction_type", sa.String(2), nullable=False),
        sa.Column(
            "quantity", sa.Numeric(precision=18, scale=4), nullable=False
        ),
        sa.Column(
            "price", sa.Numeric(precision=18, scale=4), nullable=False
        ),
        sa.Column(
            "amount", sa.Numeric(precision=18, scale=2), nullable=False
        ),
        sa.Column("currency_code", sa.String(3), nullable=False),
        sa.Column("status", sa.String(1), nullable=False),
        sa.Column("process_date", sa.DateTime, nullable=False),
        sa.Column("process_user", sa.String(8), nullable=False),
    )
    op.create_index(
        "idx_trans_hist_port",
        "transaction_history",
        ["portfolio_id", "transaction_date"],
    )
    op.create_index(
        "idx_trans_hist_date",
        "transaction_history",
        ["transaction_date", "portfolio_id"],
    )

    # --- poshist ---
    op.create_table(
        "poshist",
        sa.Column("account_no", sa.String(8), primary_key=True),
        sa.Column("portfolio_id", sa.String(10), primary_key=True),
        sa.Column("trans_date", sa.Date, primary_key=True),
        sa.Column("trans_time", sa.Time, primary_key=True),
        sa.Column("trans_type", sa.String(2), nullable=False),
        sa.Column("security_id", sa.String(12), nullable=False),
        sa.Column(
            "quantity", sa.Numeric(precision=15, scale=3), nullable=False
        ),
        sa.Column(
            "price", sa.Numeric(precision=15, scale=3), nullable=False
        ),
        sa.Column(
            "amount", sa.Numeric(precision=15, scale=2), nullable=False
        ),
        sa.Column(
            "fees",
            sa.Numeric(precision=15, scale=2),
            nullable=False,
            server_default="0",
        ),
        sa.Column(
            "total_amount", sa.Numeric(precision=15, scale=2), nullable=False
        ),
        sa.Column(
            "cost_basis", sa.Numeric(precision=15, scale=2), nullable=False
        ),
        sa.Column(
            "gain_loss", sa.Numeric(precision=15, scale=2), nullable=False
        ),
        sa.Column("process_date", sa.Date, nullable=False),
        sa.Column("process_time", sa.Time, nullable=False),
        sa.Column("program_id", sa.String(8), nullable=False),
        sa.Column("user_id", sa.String(8), nullable=False),
        sa.Column(
            "audit_timestamp",
            sa.DateTime,
            nullable=False,
            server_default=sa.text("now()"),
        ),
    )
    op.create_index("poshist_ix1", "poshist", ["security_id", "trans_date"])
    op.create_index("poshist_ix2", "poshist", ["process_date", "program_id"])

    # --- errlog ---
    op.create_table(
        "errlog",
        sa.Column("error_timestamp", sa.DateTime, primary_key=True),
        sa.Column("program_id", sa.String(8), primary_key=True),
        sa.Column("error_type", sa.String(1), nullable=False),
        sa.Column("error_severity", sa.Integer, nullable=False),
        sa.Column("error_code", sa.String(8), nullable=False),
        sa.Column("error_message", sa.String(200), nullable=False),
        sa.Column("process_date", sa.Date, nullable=False),
        sa.Column("process_time", sa.Time, nullable=False),
        sa.Column("user_id", sa.String(8), nullable=False),
        sa.Column("additional_info", sa.String(500), nullable=True),
    )
    op.create_index(
        "errlog_ix1", "errlog", ["process_date", "error_severity"]
    )

    # --- rtncodes ---
    op.create_table(
        "rtncodes",
        sa.Column("timestamp", sa.DateTime, primary_key=True),
        sa.Column("program_id", sa.String(8), primary_key=True),
        sa.Column("return_code", sa.Integer, nullable=False),
        sa.Column("highest_code", sa.Integer, nullable=False),
        sa.Column("status_code", sa.String(1), nullable=False),
        sa.Column("message_text", sa.String(80), nullable=True),
    )
    op.create_index(
        "rtncodes_prg_idx", "rtncodes", ["program_id", "timestamp"]
    )
    op.create_index(
        "rtncodes_sts_idx", "rtncodes", ["status_code", "timestamp"]
    )


def downgrade() -> None:
    op.drop_table("rtncodes")
    op.drop_table("errlog")
    op.drop_table("poshist")
    op.drop_table("transaction_history")
    op.drop_table("investment_positions")
    op.drop_table("portfolio_master")
