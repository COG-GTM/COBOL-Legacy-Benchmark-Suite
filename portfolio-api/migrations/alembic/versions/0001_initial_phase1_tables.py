"""initial_phase1_tables

Revision ID: 0001
Revises:
Create Date: 2026-05-08 19:41:16.537034

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '0001'
down_revision: Union[str, Sequence[str], None] = None
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
        sa.Column(
            "last_maint_date", sa.DateTime, nullable=False, server_default=sa.func.now()
        ),
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
        sa.Column("quantity", sa.Numeric(18, 4), nullable=False),
        sa.Column("cost_basis", sa.Numeric(18, 2), nullable=False),
        sa.Column("market_value", sa.Numeric(18, 2), nullable=False),
        sa.Column("currency_code", sa.String(3), nullable=False),
        sa.Column("last_maint_date", sa.DateTime, nullable=False),
        sa.Column("last_maint_user", sa.String(8), nullable=False),
    )
    op.create_index(
        "idx_positions_date", "investment_positions", ["position_date", "portfolio_id"]
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
        sa.Column("quantity", sa.Numeric(18, 4), nullable=False),
        sa.Column("price", sa.Numeric(18, 4), nullable=False),
        sa.Column("amount", sa.Numeric(18, 2), nullable=False),
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

    # --- audit_log ---
    op.create_table(
        "audit_log",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column(
            "timestamp", sa.DateTime, nullable=False, server_default=sa.func.now()
        ),
        sa.Column("user_id", sa.String(8), nullable=True),
        sa.Column("terminal_id", sa.String(8), nullable=True),
        sa.Column("program_id", sa.String(8), nullable=True),
        sa.Column("action", sa.String(10), nullable=True),
        sa.Column("entity_type", sa.String(20), nullable=True),
        sa.Column("entity_id", sa.String(20), nullable=True),
        sa.Column("before_image", sa.Text, nullable=True),
        sa.Column("after_image", sa.Text, nullable=True),
        sa.Column("message", sa.String(256), nullable=True),
    )

    # --- error_log ---
    op.create_table(
        "error_log",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column(
            "timestamp", sa.DateTime, nullable=False, server_default=sa.func.now()
        ),
        sa.Column("program_id", sa.String(8), nullable=True),
        sa.Column("category", sa.String(2), nullable=True),
        sa.Column("error_code", sa.String(4), nullable=True),
        sa.Column("severity", sa.Integer, nullable=True),
        sa.Column("error_text", sa.String(80), nullable=True),
        sa.Column("error_details", sa.String(256), nullable=True),
        sa.Column("return_code", sa.Integer, nullable=True),
    )

    # --- batch_control ---
    op.create_table(
        "batch_control",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("job_name", sa.String(8), nullable=False),
        sa.Column("process_date", sa.String(8), nullable=False),
        sa.Column("sequence_no", sa.Integer, nullable=True),
        sa.Column("status", sa.String(1), nullable=True),
        sa.Column("records_read", sa.Integer, server_default="0"),
        sa.Column("records_written", sa.Integer, server_default="0"),
        sa.Column("start_time", sa.DateTime, nullable=True),
        sa.Column("end_time", sa.DateTime, nullable=True),
    )

    # --- views ---
    op.execute(
        """
        CREATE VIEW active_portfolios AS
        SELECT *
        FROM portfolio_master
        WHERE status = 'A'
          AND (close_date IS NULL OR close_date > CURRENT_DATE)
        """
    )

    op.execute(
        """
        CREATE VIEW current_positions AS
        SELECT p.*, pm.portfolio_name, pm.client_id
        FROM investment_positions p
        JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
        WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day'
        """
    )


def downgrade() -> None:
    op.execute("DROP VIEW IF EXISTS current_positions")
    op.execute("DROP VIEW IF EXISTS active_portfolios")
    op.drop_table("batch_control")
    op.drop_table("error_log")
    op.drop_table("audit_log")
    op.drop_table("transaction_history")
    op.drop_table("investment_positions")
    op.drop_table("portfolio_master")
