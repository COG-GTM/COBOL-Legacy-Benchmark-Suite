"""Initial tables - all COBOL data structures

Revision ID: 001
Revises: None
Create Date: 2024-01-01 00:00:00.000000

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
    # PORTFOLIO_MASTER
    op.create_table(
        "portfolio_master",
        sa.Column("portfolio_id", sa.String(8), primary_key=True),
        sa.Column("account_type", sa.String(2), nullable=False, server_default="IN"),
        sa.Column("branch_id", sa.String(2), nullable=False, server_default="00"),
        sa.Column("client_id", sa.String(10), nullable=False),
        sa.Column("portfolio_name", sa.String(50), nullable=False),
        sa.Column("currency_code", sa.String(3), nullable=False, server_default="USD"),
        sa.Column("risk_level", sa.String(1), nullable=False, server_default="M"),
        sa.Column("status", sa.String(1), nullable=False, server_default="A"),
        sa.Column("client_name", sa.String(30), nullable=False, server_default=""),
        sa.Column("client_type", sa.String(1), nullable=False, server_default="I"),
        sa.Column("total_value", sa.Numeric(15, 2), nullable=False, server_default="0"),
        sa.Column("cash_balance", sa.Numeric(15, 2), nullable=False, server_default="0"),
        sa.Column("account_number", sa.String(10), nullable=False, server_default=""),
        sa.Column("open_date", sa.Date, nullable=False, server_default=sa.text("CURRENT_DATE")),
        sa.Column("close_date", sa.Date, nullable=True),
        sa.Column("last_maint_date", sa.DateTime, nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("last_maint_user", sa.String(8), nullable=False, server_default="SYSTEM"),
    )
    op.create_index("idx_portfolio_client", "portfolio_master", ["client_id"])
    op.create_index("idx_portfolio_branch", "portfolio_master", ["branch_id"])
    op.create_index("idx_portfolio_status", "portfolio_master", ["status"])
    op.create_index("idx_portfolio_vsam_key", "portfolio_master", ["portfolio_id", "account_type", "branch_id"], unique=True)

    # INVESTMENT_POSITIONS
    op.create_table(
        "investment_positions",
        sa.Column("portfolio_id", sa.String(8), sa.ForeignKey("portfolio_master.portfolio_id"), primary_key=True),
        sa.Column("investment_id", sa.String(10), primary_key=True),
        sa.Column("position_date", sa.Date, primary_key=True),
        sa.Column("quantity", sa.Numeric(18, 4), nullable=False, server_default="0"),
        sa.Column("cost_basis", sa.Numeric(18, 2), nullable=False, server_default="0"),
        sa.Column("market_value", sa.Numeric(18, 2), nullable=False, server_default="0"),
        sa.Column("currency_code", sa.String(3), nullable=False, server_default="USD"),
        sa.Column("status", sa.String(1), nullable=False, server_default="A"),
        sa.Column("last_maint_date", sa.DateTime, nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("last_maint_user", sa.String(8), nullable=False, server_default="SYSTEM"),
    )
    op.create_index("idx_position_portfolio", "investment_positions", ["portfolio_id"])
    op.create_index("idx_position_date", "investment_positions", ["position_date"])
    op.create_index("idx_position_investment", "investment_positions", ["investment_id"])

    # TRANSACTION_HISTORY
    op.create_table(
        "transaction_history",
        sa.Column("transaction_id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("portfolio_id", sa.String(8), sa.ForeignKey("portfolio_master.portfolio_id"), nullable=False),
        sa.Column("investment_id", sa.String(10), nullable=False),
        sa.Column("trn_date", sa.Date, nullable=False),
        sa.Column("trn_time", sa.String(6), nullable=False, server_default="000000"),
        sa.Column("sequence_no", sa.String(6), nullable=False, server_default="000001"),
        sa.Column("trn_type", sa.String(2), nullable=False),
        sa.Column("quantity", sa.Numeric(18, 4), nullable=False, server_default="0"),
        sa.Column("price", sa.Numeric(18, 4), nullable=False, server_default="0"),
        sa.Column("amount", sa.Numeric(18, 2), nullable=False, server_default="0"),
        sa.Column("fees", sa.Numeric(18, 2), nullable=False, server_default="0"),
        sa.Column("total_amount", sa.Numeric(18, 2), nullable=False, server_default="0"),
        sa.Column("cost_basis", sa.Numeric(18, 2), nullable=False, server_default="0"),
        sa.Column("gain_loss", sa.Numeric(18, 2), nullable=False, server_default="0"),
        sa.Column("currency_code", sa.String(3), nullable=False, server_default="USD"),
        sa.Column("status", sa.String(1), nullable=False, server_default="P"),
        sa.Column("process_date", sa.Date, nullable=True),
        sa.Column("process_user", sa.String(8), nullable=False, server_default=""),
        sa.Column("created_at", sa.DateTime, nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
    )
    op.create_index("idx_trn_portfolio", "transaction_history", ["portfolio_id"])
    op.create_index("idx_trn_date", "transaction_history", ["trn_date"])
    op.create_index("idx_trn_type", "transaction_history", ["trn_type"])
    op.create_index("idx_trn_vsam_key", "transaction_history", ["trn_date", "trn_time", "portfolio_id", "sequence_no"])

    # AUDIT_LOG
    op.create_table(
        "audit_log",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("timestamp", sa.DateTime, nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("system_id", sa.String(8), nullable=False, server_default="SYSTEM"),
        sa.Column("user_id", sa.String(8), nullable=False, server_default=""),
        sa.Column("program", sa.String(8), nullable=False, server_default=""),
        sa.Column("terminal", sa.String(8), nullable=False, server_default=""),
        sa.Column("audit_type", sa.String(4), nullable=False),
        sa.Column("action", sa.String(8), nullable=False),
        sa.Column("status", sa.String(4), nullable=False, server_default="SUCC"),
        sa.Column("key_info", sa.String(50), nullable=False, server_default=""),
        sa.Column("before_image", sa.Text, nullable=False, server_default=""),
        sa.Column("after_image", sa.Text, nullable=False, server_default=""),
        sa.Column("message", sa.Text, nullable=False, server_default=""),
    )
    op.create_index("idx_audit_timestamp", "audit_log", ["timestamp"])
    op.create_index("idx_audit_user", "audit_log", ["user_id"])
    op.create_index("idx_audit_type", "audit_log", ["audit_type"])

    # ERROR_LOG
    op.create_table(
        "error_log",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("timestamp", sa.DateTime, nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("program", sa.String(8), nullable=False),
        sa.Column("category", sa.String(2), nullable=False),
        sa.Column("error_code", sa.String(4), nullable=False),
        sa.Column("severity", sa.Integer, nullable=False),
        sa.Column("error_text", sa.String(80), nullable=False),
        sa.Column("error_details", sa.Text, nullable=False, server_default=""),
    )
    op.create_index("idx_error_timestamp", "error_log", ["timestamp"])
    op.create_index("idx_error_severity", "error_log", ["severity"])

    # BATCH_CONTROL
    op.create_table(
        "batch_control",
        sa.Column("job_name", sa.String(8), primary_key=True),
        sa.Column("process_date", sa.String(8), primary_key=True),
        sa.Column("sequence_no", sa.Integer, nullable=False, server_default="0"),
        sa.Column("status", sa.String(1), nullable=False, server_default="R"),
        sa.Column("return_code", sa.Integer, nullable=False, server_default="0"),
        sa.Column("start_time", sa.DateTime, nullable=True),
        sa.Column("end_time", sa.DateTime, nullable=True),
        sa.Column("records_read", sa.Integer, nullable=False, server_default="0"),
        sa.Column("records_written", sa.Integer, nullable=False, server_default="0"),
        sa.Column("error_count", sa.Integer, nullable=False, server_default="0"),
        sa.Column("restart_count", sa.Integer, nullable=False, server_default="0"),
        sa.Column("max_restarts", sa.Integer, nullable=False, server_default="3"),
        sa.Column("error_desc", sa.String(80), nullable=False, server_default=""),
        sa.Column("attempt_ts", sa.DateTime, nullable=True),
    )

    # PROCESS_SEQUENCE
    op.create_table(
        "process_sequence",
        sa.Column("process_id", sa.String(8), primary_key=True),
        sa.Column("sequence_type", sa.String(3), primary_key=True),
        sa.Column("sequence_no", sa.Integer, nullable=False, server_default="0"),
        sa.Column("description", sa.String(40), nullable=False, server_default=""),
        sa.Column("restartable", sa.Boolean, nullable=False, server_default="1"),
    )

    # USER_AUTH
    op.create_table(
        "user_auth",
        sa.Column("user_id", sa.String(8), primary_key=True),
        sa.Column("resource", sa.String(8), primary_key=True),
        sa.Column("access_type", sa.String(8), primary_key=True),
        sa.Column("granted", sa.Boolean, nullable=False, server_default="1"),
    )


def downgrade() -> None:
    op.drop_table("user_auth")
    op.drop_table("process_sequence")
    op.drop_table("batch_control")
    op.drop_table("error_log")
    op.drop_table("audit_log")
    op.drop_table("transaction_history")
    op.drop_table("investment_positions")
    op.drop_table("portfolio_master")
