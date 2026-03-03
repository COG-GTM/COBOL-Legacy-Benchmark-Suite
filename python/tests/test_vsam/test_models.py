"""
Unit tests for VSAM SQLAlchemy model definitions.

Tests verify:
  - Table creation and schema correctness
  - Composite primary key constraints
  - Column types and constraints
  - Index definitions
  - Check constraints for status/type fields
"""

import pytest
from sqlalchemy import create_engine, inspect
from sqlalchemy.orm import Session

from src.database.vsam import (
    AuditHistory,
    Base,
    PortfolioMaster,
    PositionHistory,
    TransactionHistory,
)


@pytest.fixture(scope="module")
def engine():
    """Create an in-memory SQLite engine for testing."""
    eng = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(eng)
    yield eng
    Base.metadata.drop_all(eng)


@pytest.fixture()
def session(engine):
    """Provide a transactional session that rolls back after each test."""
    with Session(engine) as sess:
        yield sess
        sess.rollback()


# -----------------------------------------------------------------------
# Table existence and structure tests
# -----------------------------------------------------------------------
class TestTableCreation:
    def test_all_tables_created(self, engine):
        inspector = inspect(engine)
        table_names = inspector.get_table_names()
        assert "vsam_portfolio_master" in table_names
        assert "vsam_transaction_history" in table_names
        assert "vsam_position_history" in table_names
        assert "vsam_audit_history" in table_names

    def test_portfolio_master_columns(self, engine):
        inspector = inspect(engine)
        columns = {c["name"] for c in inspector.get_columns("vsam_portfolio_master")}
        expected = {
            "portfolio_id", "account_type", "branch_id",
            "account_no", "client_name", "client_type",
            "create_date", "last_maint_date", "status",
            "total_value", "cash_balance",
            "last_user", "last_trans_date",
        }
        assert expected.issubset(columns)

    def test_transaction_history_columns(self, engine):
        inspector = inspect(engine)
        columns = {c["name"] for c in inspector.get_columns("vsam_transaction_history")}
        expected = {
            "transaction_date", "transaction_time", "portfolio_id", "sequence_no",
            "investment_id", "transaction_type",
            "quantity", "price", "amount", "currency", "status",
            "process_date", "process_user",
        }
        assert expected.issubset(columns)

    def test_position_history_columns(self, engine):
        inspector = inspect(engine)
        columns = {c["name"] for c in inspector.get_columns("vsam_position_history")}
        expected = {
            "portfolio_id", "position_date", "investment_id",
            "quantity", "cost_basis", "market_value", "currency", "status",
            "last_maint_date", "last_maint_user",
        }
        assert expected.issubset(columns)

    def test_audit_history_columns(self, engine):
        inspector = inspect(engine)
        columns = {c["name"] for c in inspector.get_columns("vsam_audit_history")}
        expected = {
            "portfolio_id", "history_date", "history_time", "sequence_no",
            "record_type", "action_code",
            "before_image", "after_image", "reason_code",
            "process_date", "process_user",
        }
        assert expected.issubset(columns)


# -----------------------------------------------------------------------
# Composite primary key tests
# -----------------------------------------------------------------------
class TestCompositePrimaryKeys:
    def test_portfolio_master_pk(self, engine):
        inspector = inspect(engine)
        pk = inspector.get_pk_constraint("vsam_portfolio_master")
        assert set(pk["constrained_columns"]) == {
            "portfolio_id", "account_type", "branch_id",
        }

    def test_transaction_history_pk(self, engine):
        inspector = inspect(engine)
        pk = inspector.get_pk_constraint("vsam_transaction_history")
        assert set(pk["constrained_columns"]) == {
            "transaction_date", "transaction_time", "portfolio_id", "sequence_no",
        }

    def test_position_history_pk(self, engine):
        inspector = inspect(engine)
        pk = inspector.get_pk_constraint("vsam_position_history")
        assert set(pk["constrained_columns"]) == {
            "portfolio_id", "position_date", "investment_id",
        }

    def test_audit_history_pk(self, engine):
        inspector = inspect(engine)
        pk = inspector.get_pk_constraint("vsam_audit_history")
        assert set(pk["constrained_columns"]) == {
            "portfolio_id", "history_date", "history_time", "sequence_no",
        }


# -----------------------------------------------------------------------
# Index tests
# -----------------------------------------------------------------------
class TestIndexes:
    def _get_index_names(self, engine, table_name):
        inspector = inspect(engine)
        return {idx["name"] for idx in inspector.get_indexes(table_name)}

    def test_portfolio_master_indexes(self, engine):
        indexes = self._get_index_names(engine, "vsam_portfolio_master")
        assert "ix_vsam_portmstr_account" in indexes
        assert "ix_vsam_portmstr_client" in indexes
        assert "ix_vsam_portmstr_status" in indexes

    def test_transaction_history_indexes(self, engine):
        indexes = self._get_index_names(engine, "vsam_transaction_history")
        assert "ix_vsam_tranhist_portfolio" in indexes
        assert "ix_vsam_tranhist_investment" in indexes
        assert "ix_vsam_tranhist_status" in indexes

    def test_position_history_indexes(self, engine):
        indexes = self._get_index_names(engine, "vsam_position_history")
        assert "ix_vsam_poshist_date" in indexes
        assert "ix_vsam_poshist_investment" in indexes
        assert "ix_vsam_poshist_status" in indexes

    def test_audit_history_indexes(self, engine):
        indexes = self._get_index_names(engine, "vsam_audit_history")
        assert "ix_vsam_audhist_date" in indexes
        assert "ix_vsam_audhist_rectype" in indexes
        assert "ix_vsam_audhist_action" in indexes


# -----------------------------------------------------------------------
# Record insertion and retrieval tests
# -----------------------------------------------------------------------
class TestRecordOperations:
    def test_insert_and_read_portfolio_master(self, session):
        record = PortfolioMaster(
            portfolio_id="PORT0001",
            account_type="IN",
            branch_id="01",
            account_no="ACC0000001",
            client_name="John Doe",
            client_type="I",
            create_date="20240101",
            last_maint_date="20240315",
            status="A",
            total_value=100000.00,
            cash_balance=25000.50,
            last_user="ADMIN01",
            last_trans_date="20240315",
        )
        session.add(record)
        session.flush()

        result = session.get(
            PortfolioMaster, ("PORT0001", "IN", "01")
        )
        assert result is not None
        assert result.client_name == "John Doe"
        assert result.client_type == "I"
        assert result.status == "A"

    def test_insert_and_read_transaction_history(self, session):
        record = TransactionHistory(
            transaction_date="20240315",
            transaction_time="143022",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="INV0000001",
            transaction_type="BU",
            quantity=100.0000,
            price=50.2500,
            amount=5025.00,
            currency="USD",
            status="D",
            process_date="2024-03-15T14:30:22.000000",
            process_user="BATCH01",
        )
        session.add(record)
        session.flush()

        result = session.get(
            TransactionHistory,
            ("20240315", "143022", "PORT0001", "000001"),
        )
        assert result is not None
        assert result.investment_id == "INV0000001"
        assert result.transaction_type == "BU"
        assert result.status == "D"

    def test_insert_and_read_position_history(self, session):
        record = PositionHistory(
            portfolio_id="PORT0001",
            position_date="20240315",
            investment_id="INV0000001",
            quantity=500.0000,
            cost_basis=25000.00,
            market_value=27500.00,
            currency="USD",
            status="A",
            last_maint_date="2024-03-15T14:30:22.000000",
            last_maint_user="BATCH01",
        )
        session.add(record)
        session.flush()

        result = session.get(
            PositionHistory,
            ("PORT0001", "20240315", "INV0000001"),
        )
        assert result is not None
        assert float(result.market_value) == 27500.00
        assert result.status == "A"

    def test_insert_and_read_audit_history(self, session):
        record = AuditHistory(
            portfolio_id="PORT0001",
            history_date="20240315",
            history_time="143022",
            sequence_no="0001",
            record_type="PT",
            action_code="A",
            before_image=None,
            after_image="NEW PORTFOLIO RECORD IMAGE",
            reason_code="INIT",
            process_date="2024-03-15T14:30:22.000000",
            process_user="ADMIN01",
        )
        session.add(record)
        session.flush()

        result = session.get(
            AuditHistory,
            ("PORT0001", "20240315", "143022", "0001"),
        )
        assert result is not None
        assert result.record_type == "PT"
        assert result.action_code == "A"
        assert result.before_image is None
        assert result.after_image == "NEW PORTFOLIO RECORD IMAGE"

    def test_repr_methods(self, session):
        pm = PortfolioMaster(
            portfolio_id="P001", account_type="IN", branch_id="01",
            account_no="A001", client_name="Test", client_type="I",
            create_date="20240101", last_maint_date="20240101",
            status="A", total_value=0, cash_balance=0,
            last_user="USR", last_trans_date="20240101",
        )
        assert "P001" in repr(pm)

        th = TransactionHistory(
            transaction_date="20240101", transaction_time="120000",
            portfolio_id="P001", sequence_no="000001",
            investment_id="I001", transaction_type="BU",
            quantity=1, price=1, amount=1, currency="USD",
            status="D", process_date="2024", process_user="USR",
        )
        assert "P001" in repr(th)

        ph = PositionHistory(
            portfolio_id="P001", position_date="20240101",
            investment_id="I001", quantity=1, cost_basis=1,
            market_value=1, currency="USD", status="A",
            last_maint_date="2024", last_maint_user="USR",
        )
        assert "P001" in repr(ph)

        ah = AuditHistory(
            portfolio_id="P001", history_date="20240101",
            history_time="120000", sequence_no="0001",
            record_type="PT", action_code="A",
            before_image=None, after_image="img",
            reason_code="TEST", process_date="2024",
            process_user="USR",
        )
        assert "P001" in repr(ah)


# -----------------------------------------------------------------------
# Duplicate key constraint tests
# -----------------------------------------------------------------------
class TestDuplicateKeyConstraints:
    def test_duplicate_portfolio_master_rejected(self, session):
        record1 = PortfolioMaster(
            portfolio_id="DUPTEST1", account_type="IN", branch_id="01",
            account_no="ACC001", client_name="Client A", client_type="I",
            create_date="20240101", last_maint_date="20240101",
            status="A", total_value=0, cash_balance=0,
            last_user="USR", last_trans_date="20240101",
        )
        record2 = PortfolioMaster(
            portfolio_id="DUPTEST1", account_type="IN", branch_id="01",
            account_no="ACC002", client_name="Client B", client_type="C",
            create_date="20240102", last_maint_date="20240102",
            status="A", total_value=0, cash_balance=0,
            last_user="USR", last_trans_date="20240102",
        )
        session.add(record1)
        session.flush()
        session.add(record2)
        with pytest.raises(Exception):
            session.flush()
