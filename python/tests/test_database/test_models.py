"""Unit tests for DB2-migrated SQLAlchemy model definitions.

These tests verify that model metadata (columns, types, indexes, keys,
relationships) correctly reflects the original DB2 DDL without requiring
a live database connection.
"""

import unittest
from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy import Date, DateTime, Integer, Numeric, String, Time, create_engine
from sqlalchemy.orm import Session

from database.db2 import (
    Base,
    ErrLog,
    InvestmentPositions,
    PortfolioMaster,
    PosHist,
    RtnCodes,
    TransactionHistory,
)


class _SchemaTestBase(unittest.TestCase):
    """Helpers shared by every model-test class."""

    @classmethod
    def setUpClass(cls) -> None:
        """Create an in-memory SQLite database and all tables."""
        cls.engine = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(cls.engine)

    # ---- helpers ----

    @staticmethod
    def _col(model, name):
        """Return the Column object for *name* on *model*."""
        return model.__table__.columns[name]

    @staticmethod
    def _pk_names(model):
        return [c.name for c in model.__table__.primary_key.columns]

    @staticmethod
    def _index_names(model):
        return {idx.name for idx in model.__table__.indexes}

    @staticmethod
    def _index_columns(model, idx_name):
        for idx in model.__table__.indexes:
            if idx.name == idx_name:
                return [c.name for c in idx.columns]
        return None


# ====================================================================
# PortfolioMaster
# ====================================================================
class TestPortfolioMaster(_SchemaTestBase):
    model = PortfolioMaster

    def test_tablename(self):
        self.assertEqual(self.model.__tablename__, "portfolio_master")

    def test_primary_key(self):
        self.assertEqual(self._pk_names(self.model), ["portfolio_id"])

    def test_column_types(self):
        col = self._col
        m = self.model
        self.assertIsInstance(col(m, "portfolio_id").type, String)
        self.assertEqual(col(m, "portfolio_id").type.length, 8)
        self.assertIsInstance(col(m, "account_type").type, String)
        self.assertEqual(col(m, "account_type").type.length, 2)
        self.assertIsInstance(col(m, "client_id").type, String)
        self.assertEqual(col(m, "client_id").type.length, 10)
        self.assertIsInstance(col(m, "portfolio_name").type, String)
        self.assertEqual(col(m, "portfolio_name").type.length, 50)
        self.assertIsInstance(col(m, "currency_code").type, String)
        self.assertEqual(col(m, "currency_code").type.length, 3)
        self.assertIsInstance(col(m, "risk_level").type, String)
        self.assertEqual(col(m, "risk_level").type.length, 1)
        self.assertIsInstance(col(m, "open_date").type, Date)
        self.assertIsInstance(col(m, "last_maint_date").type, DateTime)

    def test_close_date_nullable(self):
        self.assertTrue(self._col(self.model, "close_date").nullable)

    def test_non_nullable_columns(self):
        for name in (
            "portfolio_id",
            "account_type",
            "branch_id",
            "client_id",
            "portfolio_name",
            "currency_code",
            "risk_level",
            "status",
            "open_date",
            "last_maint_date",
            "last_maint_user",
        ):
            self.assertFalse(
                self._col(self.model, name).nullable,
                f"{name} should be NOT NULL",
            )

    def test_index_client_status(self):
        self.assertIn("idx_port_master_client", self._index_names(self.model))
        self.assertEqual(
            self._index_columns(self.model, "idx_port_master_client"),
            ["client_id", "status"],
        )

    def test_column_count(self):
        self.assertEqual(len(self.model.__table__.columns), 12)


# ====================================================================
# InvestmentPositions
# ====================================================================
class TestInvestmentPositions(_SchemaTestBase):
    model = InvestmentPositions

    def test_tablename(self):
        self.assertEqual(self.model.__tablename__, "investment_positions")

    def test_composite_primary_key(self):
        self.assertEqual(
            self._pk_names(self.model),
            ["portfolio_id", "investment_id", "position_date"],
        )

    def test_foreign_key(self):
        fks = self._col(self.model, "portfolio_id").foreign_keys
        fk_targets = {fk.target_fullname for fk in fks}
        self.assertIn("portfolio_master.portfolio_id", fk_targets)

    def test_numeric_columns(self):
        for name in ("quantity", "cost_basis", "market_value"):
            self.assertIsInstance(
                self._col(self.model, name).type, Numeric
            )

    def test_quantity_precision(self):
        col = self._col(self.model, "quantity")
        self.assertEqual(col.type.precision, 18)
        self.assertEqual(col.type.scale, 4)

    def test_index_positions_date(self):
        self.assertIn("idx_positions_date", self._index_names(self.model))
        self.assertEqual(
            self._index_columns(self.model, "idx_positions_date"),
            ["position_date", "portfolio_id"],
        )

    def test_column_count(self):
        self.assertEqual(len(self.model.__table__.columns), 9)


# ====================================================================
# TransactionHistory
# ====================================================================
class TestTransactionHistory(_SchemaTestBase):
    model = TransactionHistory

    def test_tablename(self):
        self.assertEqual(self.model.__tablename__, "transaction_history")

    def test_primary_key(self):
        self.assertEqual(self._pk_names(self.model), ["transaction_id"])

    def test_foreign_key(self):
        fks = self._col(self.model, "portfolio_id").foreign_keys
        fk_targets = {fk.target_fullname for fk in fks}
        self.assertIn("portfolio_master.portfolio_id", fk_targets)

    def test_indexes(self):
        names = self._index_names(self.model)
        self.assertIn("idx_trans_hist_port", names)
        self.assertIn("idx_trans_hist_date", names)

    def test_index_columns(self):
        self.assertEqual(
            self._index_columns(self.model, "idx_trans_hist_port"),
            ["portfolio_id", "transaction_date"],
        )
        self.assertEqual(
            self._index_columns(self.model, "idx_trans_hist_date"),
            ["transaction_date", "portfolio_id"],
        )

    def test_time_column(self):
        self.assertIsInstance(
            self._col(self.model, "transaction_time").type, Time
        )

    def test_column_count(self):
        self.assertEqual(len(self.model.__table__.columns), 13)


# ====================================================================
# PosHist
# ====================================================================
class TestPosHist(_SchemaTestBase):
    model = PosHist

    def test_tablename(self):
        self.assertEqual(self.model.__tablename__, "poshist")

    def test_composite_primary_key(self):
        self.assertEqual(
            self._pk_names(self.model),
            ["account_no", "portfolio_id", "trans_date", "trans_time"],
        )

    def test_fees_server_default(self):
        col = self._col(self.model, "fees")
        self.assertIsNotNone(col.server_default)

    def test_audit_timestamp_server_default(self):
        col = self._col(self.model, "audit_timestamp")
        self.assertIsNotNone(col.server_default)

    def test_indexes(self):
        names = self._index_names(self.model)
        self.assertIn("poshist_ix1", names)
        self.assertIn("poshist_ix2", names)

    def test_index_columns(self):
        self.assertEqual(
            self._index_columns(self.model, "poshist_ix1"),
            ["security_id", "trans_date"],
        )
        self.assertEqual(
            self._index_columns(self.model, "poshist_ix2"),
            ["process_date", "program_id"],
        )

    def test_numeric_columns(self):
        for name in (
            "quantity",
            "price",
            "amount",
            "fees",
            "total_amount",
            "cost_basis",
            "gain_loss",
        ):
            self.assertIsInstance(
                self._col(self.model, name).type, Numeric
            )

    def test_column_count(self):
        self.assertEqual(len(self.model.__table__.columns), 18)


# ====================================================================
# ErrLog
# ====================================================================
class TestErrLog(_SchemaTestBase):
    model = ErrLog

    def test_tablename(self):
        self.assertEqual(self.model.__tablename__, "errlog")

    def test_composite_primary_key(self):
        self.assertEqual(
            self._pk_names(self.model), ["error_timestamp", "program_id"]
        )

    def test_additional_info_nullable(self):
        self.assertTrue(self._col(self.model, "additional_info").nullable)

    def test_error_severity_integer(self):
        self.assertIsInstance(
            self._col(self.model, "error_severity").type, Integer
        )

    def test_index(self):
        self.assertIn("errlog_ix1", self._index_names(self.model))
        # error_severity uses desc() so it's an expression, not a plain column
        for idx in self.model.__table__.indexes:
            if idx.name == "errlog_ix1":
                exprs = list(idx.expressions)
                self.assertEqual(len(exprs), 2)
                # First element is process_date column
                self.assertEqual(exprs[0].name, "process_date")
                # Second element is desc(error_severity) expression
                self.assertIn("DESC", str(exprs[1]).upper())

    def test_column_count(self):
        self.assertEqual(len(self.model.__table__.columns), 10)


# ====================================================================
# RtnCodes
# ====================================================================
class TestRtnCodes(_SchemaTestBase):
    model = RtnCodes

    def test_tablename(self):
        self.assertEqual(self.model.__tablename__, "rtncodes")

    def test_composite_primary_key(self):
        self.assertEqual(
            self._pk_names(self.model), ["timestamp", "program_id"]
        )

    def test_message_text_nullable(self):
        self.assertTrue(self._col(self.model, "message_text").nullable)

    def test_indexes(self):
        names = self._index_names(self.model)
        self.assertIn("rtncodes_prg_idx", names)
        self.assertIn("rtncodes_sts_idx", names)

    def test_index_columns(self):
        self.assertEqual(
            self._index_columns(self.model, "rtncodes_prg_idx"),
            ["program_id", "timestamp"],
        )
        self.assertEqual(
            self._index_columns(self.model, "rtncodes_sts_idx"),
            ["status_code", "timestamp"],
        )

    def test_column_count(self):
        self.assertEqual(len(self.model.__table__.columns), 6)


# ====================================================================
# Connection / Pooling Configuration
# ====================================================================
class TestConnectionConfig(unittest.TestCase):
    def test_pool_settings(self):
        from database.connection import create_db_engine

        # Use a postgresql-style URL with NullPool won't work for sqlite,
        # so we verify the function signature accepts the settings by
        # inspecting the engine creation with a postgresql URL prefix.
        # Instead, test with a StaticPool to avoid sqlite pool limitations.
        from sqlalchemy import create_engine, pool as sa_pool

        engine = create_engine(
            "sqlite:///:memory:",
            poolclass=sa_pool.StaticPool,
        )
        # Verify create_db_engine passes the right kwargs by checking
        # the function itself rather than sqlite (which ignores pool_size).
        import inspect
        from database.connection import create_db_engine as cde

        src = inspect.getsource(cde)
        self.assertIn("pool_size=20", src)
        self.assertIn("max_overflow=80", src)

    def test_default_url(self):
        import os
        from database.connection import get_database_url

        os.environ.setdefault("DB_HOST", "localhost")
        os.environ.setdefault("DB_PORT", "5432")
        os.environ.setdefault("DB_NAME", "portfolio")
        os.environ.setdefault("DB_USER", "postgres")
        os.environ.setdefault("DB_PASSWORD", "")
        url = get_database_url()
        self.assertIn("postgresql+psycopg2://", url)
        self.assertIn("localhost", url)
        self.assertIn("5432", url)
        self.assertIn("portfolio", url)


# ====================================================================
# Cross-Model Metadata
# ====================================================================
class TestMetadata(_SchemaTestBase):
    def test_all_tables_registered(self):
        table_names = set(Base.metadata.tables.keys())
        expected = {
            "portfolio_master",
            "investment_positions",
            "transaction_history",
            "poshist",
            "errlog",
            "rtncodes",
        }
        self.assertTrue(expected.issubset(table_names))

    def test_create_all_succeeds(self):
        """Tables can be created on an in-memory DB without errors."""
        engine = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(engine)
        inspector_tables = set(
            engine.dialect.get_table_names(engine.connect())
        )
        self.assertIn("portfolio_master", inspector_tables)


# ====================================================================
# CRUD Smoke Tests (in-memory SQLite)
# ====================================================================
class TestCRUDSmoke(_SchemaTestBase):
    def test_insert_portfolio_master(self):
        with Session(self.engine) as session:
            pm = PortfolioMaster(
                portfolio_id="PORT0001",
                account_type="IN",
                branch_id="01",
                client_id="CLIENT0001",
                portfolio_name="Test Portfolio",
                currency_code="USD",
                risk_level="M",
                status="A",
                open_date=date(2024, 1, 1),
                last_maint_date=datetime(2024, 1, 1, 12, 0, 0),
                last_maint_user="ADMIN",
            )
            session.add(pm)
            session.commit()
            result = session.get(PortfolioMaster, "PORT0001")
            self.assertIsNotNone(result)
            self.assertEqual(result.portfolio_name, "Test Portfolio")

    def test_insert_transaction_history(self):
        with Session(self.engine) as session:
            # Ensure parent exists
            pm = session.get(PortfolioMaster, "PORT0001")
            if pm is None:
                pm = PortfolioMaster(
                    portfolio_id="PORT0001",
                    account_type="IN",
                    branch_id="01",
                    client_id="CLIENT0001",
                    portfolio_name="Test Portfolio",
                    currency_code="USD",
                    risk_level="M",
                    status="A",
                    open_date=date(2024, 1, 1),
                    last_maint_date=datetime(2024, 1, 1, 12, 0, 0),
                    last_maint_user="ADMIN",
                )
                session.add(pm)
                session.flush()

            th = TransactionHistory(
                transaction_id="20240101120000000001",
                portfolio_id="PORT0001",
                transaction_date=date(2024, 1, 1),
                transaction_time=time(12, 0, 0),
                investment_id="INV0000001",
                transaction_type="BU",
                quantity=Decimal("100.0000"),
                price=Decimal("50.2500"),
                amount=Decimal("5025.00"),
                currency_code="USD",
                status="P",
                process_date=datetime(2024, 1, 1, 12, 0, 0),
                process_user="ADMIN",
            )
            session.add(th)
            session.commit()
            result = session.get(TransactionHistory, "20240101120000000001")
            self.assertIsNotNone(result)

    def test_insert_errlog(self):
        with Session(self.engine) as session:
            ts = datetime(2024, 6, 15, 10, 30, 0)
            err = ErrLog(
                error_timestamp=ts,
                program_id="TRNVAL00",
                error_type="A",
                error_severity=3,
                error_code="ERR00010",
                error_message="Validation failed",
                process_date=date(2024, 6, 15),
                process_time=time(10, 30, 0),
                user_id="BATCH",
            )
            session.add(err)
            session.commit()
            result = session.get(ErrLog, (ts, "TRNVAL00"))
            self.assertIsNotNone(result)

    def test_insert_rtncodes(self):
        with Session(self.engine) as session:
            ts = datetime(2024, 6, 15, 11, 0, 0)
            rc = RtnCodes(
                timestamp=ts,
                program_id="POSUPD00",
                return_code=0,
                highest_code=0,
                status_code="S",
                message_text="Success",
            )
            session.add(rc)
            session.commit()
            result = session.get(RtnCodes, (ts, "POSUPD00"))
            self.assertIsNotNone(result)


if __name__ == "__main__":
    unittest.main()
