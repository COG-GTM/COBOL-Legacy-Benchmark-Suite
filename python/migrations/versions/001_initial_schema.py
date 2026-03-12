"""Initial schema — all tables from COBOL DB2/VSAM definitions.

Creates tables:
  - portfolio_master   (from db2-definitions.sql PORTFOLIO_MASTER + VSAM PORTMSTR)
  - investment_positions (from db2-definitions.sql INVESTMENT_POSITIONS + VSAM POSHIST)
  - transaction_history  (from db2-definitions.sql TRANSACTION_HISTORY + VSAM TRANHIST)
  - poshist              (from POSHIST.sql position history)
  - errlog               (from ERRLOG.sql error log)
  - rtncodes             (from RTNCODES.sql return codes)

Revision ID: 001
Revises: None
Create Date: 2024-01-01 00:00:00.000000
"""
from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # ------------------------------------------------------------------
    # PORTFOLIO_MASTER
    # Source: db2-definitions.sql lines 10-24
    # VSAM: PORTMSTR (KSDS, key = portfolio_id + account_type + branch_id)
    # ------------------------------------------------------------------
    op.create_table(
        "portfolio_master",
        sa.Column("portfolio_id", sa.String(8), nullable=False, comment="Portfolio identifier"),
        sa.Column("account_type", sa.String(2), nullable=False, comment="Account type code"),
        sa.Column("branch_id", sa.String(2), nullable=False, comment="Branch identifier"),
        sa.Column("client_id", sa.String(10), nullable=False, comment="Client identifier"),
        sa.Column("portfolio_name", sa.String(50), nullable=False, comment="Portfolio display name"),
        sa.Column("currency_code", sa.String(3), nullable=False, comment="ISO currency code"),
        sa.Column("risk_level", sa.String(1), nullable=False, comment="Risk level code"),
        sa.Column(
            "status", sa.String(1), nullable=False,
            comment="Status: A=Active, C=Closed, S=Suspended",
        ),
        sa.Column("open_date", sa.Date(), nullable=False, comment="Portfolio open date"),
        sa.Column("close_date", sa.Date(), nullable=True, comment="Portfolio close date"),
        sa.Column("last_maint_date", sa.DateTime(), nullable=False, comment="Last maintenance timestamp"),
        sa.Column("last_maint_user", sa.String(8), nullable=False, comment="Last maintenance user ID"),
        sa.PrimaryKeyConstraint("portfolio_id"),
    )
    # db2-definitions.sql: IDX_PORT_MASTER_CLIENT ON (CLIENT_ID, STATUS)
    op.create_index("ix_portfolio_master_client", "portfolio_master", ["client_id", "status"])
    # VSAM PORTMSTR KSDS key
    op.create_index(
        "ix_portfolio_master_vsam_key", "portfolio_master",
        ["portfolio_id", "account_type", "branch_id"], unique=True,
    )
    # Branch lookup index
    op.create_index("ix_portfolio_master_branch", "portfolio_master", ["branch_id", "status"])

    # ------------------------------------------------------------------
    # INVESTMENT_POSITIONS
    # Source: db2-definitions.sql lines 29-41
    # VSAM: POSHIST (KSDS, key = portfolio_id + position_date + investment_id)
    # ------------------------------------------------------------------
    op.create_table(
        "investment_positions",
        sa.Column(
            "portfolio_id", sa.String(8), sa.ForeignKey("portfolio_master.portfolio_id"),
            nullable=False, comment="Portfolio identifier (FK to portfolio_master)",
        ),
        sa.Column("investment_id", sa.String(10), nullable=False, comment="Investment/security identifier"),
        sa.Column("position_date", sa.Date(), nullable=False, comment="Position date (YYYYMMDD)"),
        sa.Column("quantity", sa.Numeric(18, 4), nullable=False, comment="Position quantity"),
        sa.Column("cost_basis", sa.Numeric(18, 2), nullable=False, comment="Cost basis amount"),
        sa.Column("market_value", sa.Numeric(18, 2), nullable=False, comment="Current market value"),
        sa.Column("currency_code", sa.String(3), nullable=False, comment="ISO currency code"),
        sa.Column("last_maint_date", sa.DateTime(), nullable=False, comment="Last maintenance timestamp"),
        sa.Column("last_maint_user", sa.String(8), nullable=False, comment="Last maintenance user ID"),
        sa.PrimaryKeyConstraint("portfolio_id", "investment_id", "position_date"),
    )
    # db2-definitions.sql: IDX_POSITIONS_DATE
    op.create_index("ix_positions_date", "investment_positions", ["position_date", "portfolio_id"])
    # VSAM POSHIST KSDS key
    op.create_index(
        "ix_positions_vsam_key", "investment_positions",
        ["portfolio_id", "position_date", "investment_id"], unique=True,
    )

    # ------------------------------------------------------------------
    # TRANSACTION_HISTORY
    # Source: db2-definitions.sql lines 46-62
    # VSAM: TRANHIST (KSDS, key = date + time + portfolio_id + seq)
    # ------------------------------------------------------------------
    op.create_table(
        "transaction_history",
        sa.Column(
            "transaction_id", sa.String(20), nullable=False,
            comment="Transaction ID (YYYYMMDDHHMMSS + 6-digit seq)",
        ),
        sa.Column(
            "portfolio_id", sa.String(8), sa.ForeignKey("portfolio_master.portfolio_id"),
            nullable=False, comment="Portfolio identifier (FK to portfolio_master)",
        ),
        sa.Column("transaction_date", sa.Date(), nullable=False, comment="Transaction date"),
        sa.Column("transaction_time", sa.Time(), nullable=False, comment="Transaction time"),
        sa.Column("investment_id", sa.String(10), nullable=False, comment="Investment/security identifier"),
        sa.Column(
            "transaction_type", sa.String(2), nullable=False,
            comment="Type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee",
        ),
        sa.Column("quantity", sa.Numeric(18, 4), nullable=False, comment="Transaction quantity"),
        sa.Column("price", sa.Numeric(18, 4), nullable=False, comment="Transaction price"),
        sa.Column("amount", sa.Numeric(18, 2), nullable=False, comment="Transaction amount"),
        sa.Column("currency_code", sa.String(3), nullable=False, comment="ISO currency code"),
        sa.Column(
            "status", sa.String(1), nullable=False,
            comment="Status: P=Processed, F=Failed, R=Reversed",
        ),
        sa.Column("process_date", sa.DateTime(), nullable=False, comment="Processing timestamp"),
        sa.Column("process_user", sa.String(8), nullable=False, comment="Processing user ID"),
        sa.PrimaryKeyConstraint("transaction_id"),
    )
    # db2-definitions.sql: IDX_TRANS_HIST_PORT
    op.create_index("ix_trans_hist_port", "transaction_history", ["portfolio_id", "transaction_date"])
    # db2-definitions.sql: IDX_TRANS_HIST_DATE
    op.create_index("ix_trans_hist_date", "transaction_history", ["transaction_date", "portfolio_id"])
    # VSAM TRANHIST KSDS key access pattern
    op.create_index(
        "ix_trans_hist_vsam_key", "transaction_history",
        ["transaction_date", "transaction_time", "portfolio_id"],
    )

    # ------------------------------------------------------------------
    # POSHIST (Position History)
    # Source: POSHIST.sql
    # ------------------------------------------------------------------
    op.create_table(
        "poshist",
        sa.Column("account_no", sa.String(8), nullable=False, comment="Account number"),
        sa.Column("portfolio_id", sa.String(10), nullable=False, comment="Portfolio identifier"),
        sa.Column("trans_date", sa.Date(), nullable=False, comment="Transaction date"),
        sa.Column("trans_time", sa.Time(), nullable=False, comment="Transaction time"),
        sa.Column(
            "trans_type", sa.String(2), nullable=False,
            comment="Transaction type: BU=Buy, SL=Sell, TR=Transfer",
        ),
        sa.Column("security_id", sa.String(12), nullable=False, comment="Security identifier"),
        sa.Column("quantity", sa.Numeric(15, 3), nullable=False, comment="Transaction quantity"),
        sa.Column("price", sa.Numeric(15, 3), nullable=False, comment="Transaction price"),
        sa.Column("amount", sa.Numeric(15, 2), nullable=False, comment="Transaction amount"),
        sa.Column(
            "fees", sa.Numeric(15, 2), nullable=False,
            server_default="0", comment="Transaction fees",
        ),
        sa.Column("total_amount", sa.Numeric(15, 2), nullable=False, comment="Total amount including fees"),
        sa.Column("cost_basis", sa.Numeric(15, 2), nullable=False, comment="Cost basis amount"),
        sa.Column("gain_loss", sa.Numeric(15, 2), nullable=False, comment="Realized gain/loss amount"),
        sa.Column("process_date", sa.Date(), nullable=False, comment="Processing date"),
        sa.Column("process_time", sa.Time(), nullable=False, comment="Processing time"),
        sa.Column("program_id", sa.String(8), nullable=False, comment="Processing program ID"),
        sa.Column("user_id", sa.String(8), nullable=False, comment="Processing user ID"),
        sa.Column("audit_timestamp", sa.DateTime(), nullable=False, comment="Audit trail timestamp"),
        sa.PrimaryKeyConstraint("account_no", "portfolio_id", "trans_date", "trans_time"),
    )
    # POSHIST_IX1: (SECURITY_ID, TRANS_DATE)
    op.create_index("ix_poshist_security", "poshist", ["security_id", "trans_date"])
    # POSHIST_IX2: (PROCESS_DATE, PROGRAM_ID)
    op.create_index("ix_poshist_process", "poshist", ["process_date", "program_id"])

    # ------------------------------------------------------------------
    # ERRLOG (Error Log)
    # Source: ERRLOG.sql
    # ------------------------------------------------------------------
    op.create_table(
        "errlog",
        sa.Column("error_timestamp", sa.DateTime(), nullable=False, comment="Error occurrence timestamp"),
        sa.Column("program_id", sa.String(8), nullable=False, comment="Program that raised the error"),
        sa.Column(
            "error_type", sa.String(1), nullable=False,
            comment="Error type: S=System, A=Application, D=Data",
        ),
        sa.Column(
            "error_severity", sa.Integer(), nullable=False,
            comment="Severity: 1=Info, 2=Warning, 3=Error, 4=Severe",
        ),
        sa.Column("error_code", sa.String(8), nullable=False, comment="Error code identifier"),
        sa.Column("error_message", sa.String(200), nullable=False, comment="Error description message"),
        sa.Column("process_date", sa.Date(), nullable=False, comment="Processing date"),
        sa.Column("process_time", sa.Time(), nullable=False, comment="Processing time"),
        sa.Column("user_id", sa.String(8), nullable=False, comment="User ID at time of error"),
        sa.Column("additional_info", sa.String(500), nullable=True, comment="Additional diagnostic information"),
        sa.PrimaryKeyConstraint("error_timestamp", "program_id"),
    )
    # ERRLOG_IX1: (PROCESS_DATE, ERROR_SEVERITY DESC)
    op.create_index(
        "ix_errlog_process_date", "errlog",
        [sa.text("process_date"), sa.text("error_severity DESC")],
    )

    # ------------------------------------------------------------------
    # RTNCODES (Return Codes)
    # Source: RTNCODES.sql
    # ------------------------------------------------------------------
    op.create_table(
        "rtncodes",
        sa.Column("timestamp", sa.DateTime(), nullable=False, comment="Return code timestamp"),
        sa.Column("program_id", sa.String(8), nullable=False, comment="Program identifier"),
        sa.Column("return_code", sa.Integer(), nullable=False, comment="Program return code"),
        sa.Column("highest_code", sa.Integer(), nullable=False, comment="Highest return code in run"),
        sa.Column("status_code", sa.String(1), nullable=False, comment="Status code"),
        sa.Column("message_text", sa.String(80), nullable=True, comment="Descriptive message"),
        sa.PrimaryKeyConstraint("timestamp", "program_id"),
    )
    # RTNCODES_PRG_IDX: (PROGRAM_ID, TIMESTAMP)
    op.create_index("ix_rtncodes_program", "rtncodes", ["program_id", "timestamp"])
    # RTNCODES_STS_IDX: (STATUS_CODE, TIMESTAMP)
    op.create_index("ix_rtncodes_status", "rtncodes", ["status_code", "timestamp"])


def downgrade() -> None:
    op.drop_table("rtncodes")
    op.drop_table("errlog")
    op.drop_table("poshist")
    op.drop_table("transaction_history")
    op.drop_table("investment_positions")
    op.drop_table("portfolio_master")
