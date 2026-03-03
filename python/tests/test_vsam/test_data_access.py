"""
Unit tests for the VSAM-like data access layer.

Tests verify VSAM-equivalent operations:
  - read_by_key()   → Keyed random access
  - read_next()     → Sequential forward browse
  - write()         → Insert new record
  - rewrite()       → Update existing record
  - delete_by_key() → Remove record by key
  - Error handling  → VSAM status codes
"""

import pytest
from sqlalchemy import create_engine

from src.database.vsam import (
    AuditHistory,
    Base,
    PortfolioMaster,
    PositionHistory,
    TransactionHistory,
    VSAMDataAccess,
    VSAMError,
    VSAMStatus,
    create_audit_history_dao,
    create_portfolio_master_dao,
    create_position_history_dao,
    create_transaction_history_dao,
)


@pytest.fixture(scope="module")
def engine():
    """Create an in-memory SQLite engine for testing."""
    eng = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(eng)
    yield eng
    Base.metadata.drop_all(eng)


@pytest.fixture(autouse=True)
def clean_tables(engine):
    """Clean all tables before each test."""
    from sqlalchemy.orm import Session

    with Session(engine) as session:
        session.query(PortfolioMaster).delete()
        session.query(TransactionHistory).delete()
        session.query(PositionHistory).delete()
        session.query(AuditHistory).delete()
        session.commit()
    yield


# -----------------------------------------------------------------------
# Factory function tests
# -----------------------------------------------------------------------
class TestFactoryFunctions:
    def test_create_portfolio_master_dao(self, engine):
        dao = create_portfolio_master_dao(engine)
        assert isinstance(dao, VSAMDataAccess)
        assert dao.model is PortfolioMaster

    def test_create_transaction_history_dao(self, engine):
        dao = create_transaction_history_dao(engine)
        assert isinstance(dao, VSAMDataAccess)
        assert dao.model is TransactionHistory

    def test_create_position_history_dao(self, engine):
        dao = create_position_history_dao(engine)
        assert isinstance(dao, VSAMDataAccess)
        assert dao.model is PositionHistory

    def test_create_audit_history_dao(self, engine):
        dao = create_audit_history_dao(engine)
        assert isinstance(dao, VSAMDataAccess)
        assert dao.model is AuditHistory


# -----------------------------------------------------------------------
# Helper to create sample records
# -----------------------------------------------------------------------
def _make_portfolio(
    pid: str = "PORT0001",
    atype: str = "IN",
    branch: str = "01",
    name: str = "Test Client",
) -> PortfolioMaster:
    return PortfolioMaster(
        portfolio_id=pid,
        account_type=atype,
        branch_id=branch,
        account_no="ACC0000001",
        client_name=name,
        client_type="I",
        create_date="20240101",
        last_maint_date="20240315",
        status="A",
        total_value=100000.00,
        cash_balance=25000.50,
        last_user="ADMIN01",
        last_trans_date="20240315",
    )


def _make_transaction(
    date: str = "20240315",
    time: str = "143022",
    pid: str = "PORT0001",
    seq: str = "000001",
) -> TransactionHistory:
    return TransactionHistory(
        transaction_date=date,
        transaction_time=time,
        portfolio_id=pid,
        sequence_no=seq,
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


def _make_position(
    pid: str = "PORT0001",
    date: str = "20240315",
    inv: str = "INV0000001",
) -> PositionHistory:
    return PositionHistory(
        portfolio_id=pid,
        position_date=date,
        investment_id=inv,
        quantity=500.0000,
        cost_basis=25000.00,
        market_value=27500.00,
        currency="USD",
        status="A",
        last_maint_date="2024-03-15T14:30:22.000000",
        last_maint_user="BATCH01",
    )


def _make_audit(
    pid: str = "PORT0001",
    date: str = "20240315",
    time: str = "143022",
    seq: str = "0001",
) -> AuditHistory:
    return AuditHistory(
        portfolio_id=pid,
        history_date=date,
        history_time=time,
        sequence_no=seq,
        record_type="PT",
        action_code="A",
        before_image=None,
        after_image="AFTER IMAGE DATA",
        reason_code="INIT",
        process_date="2024-03-15T14:30:22.000000",
        process_user="ADMIN01",
    )


# -----------------------------------------------------------------------
# WRITE operation tests
# -----------------------------------------------------------------------
class TestWrite:
    def test_write_portfolio_master(self, engine):
        dao = create_portfolio_master_dao(engine)
        status = dao.write(_make_portfolio())
        assert status == VSAMStatus.SUCCESS

    def test_write_transaction_history(self, engine):
        dao = create_transaction_history_dao(engine)
        status = dao.write(_make_transaction())
        assert status == VSAMStatus.SUCCESS

    def test_write_position_history(self, engine):
        dao = create_position_history_dao(engine)
        status = dao.write(_make_position())
        assert status == VSAMStatus.SUCCESS

    def test_write_audit_history(self, engine):
        dao = create_audit_history_dao(engine)
        status = dao.write(_make_audit())
        assert status == VSAMStatus.SUCCESS

    def test_write_duplicate_raises_error(self, engine):
        dao = create_portfolio_master_dao(engine)
        dao.write(_make_portfolio(pid="DUP00001"))
        with pytest.raises(VSAMError) as exc_info:
            dao.write(_make_portfolio(pid="DUP00001"))
        assert exc_info.value.status == VSAMStatus.DUPLICATE_KEY


# -----------------------------------------------------------------------
# READ BY KEY operation tests
# -----------------------------------------------------------------------
class TestReadByKey:
    def test_read_existing_portfolio(self, engine):
        dao = create_portfolio_master_dao(engine)
        dao.write(_make_portfolio(pid="READ0001"))
        record = dao.read_by_key(
            portfolio_id="READ0001", account_type="IN", branch_id="01"
        )
        assert record.portfolio_id == "READ0001"
        assert record.client_name == "Test Client"

    def test_read_existing_transaction(self, engine):
        dao = create_transaction_history_dao(engine)
        dao.write(_make_transaction(date="20240401", seq="000099"))
        record = dao.read_by_key(
            transaction_date="20240401",
            transaction_time="143022",
            portfolio_id="PORT0001",
            sequence_no="000099",
        )
        assert record.investment_id == "INV0000001"

    def test_read_nonexistent_raises_error(self, engine):
        dao = create_portfolio_master_dao(engine)
        with pytest.raises(VSAMError) as exc_info:
            dao.read_by_key(
                portfolio_id="NOEXIST1", account_type="XX", branch_id="99"
            )
        assert exc_info.value.status == VSAMStatus.RECORD_NOT_FOUND

    def test_read_incomplete_key_raises_error(self, engine):
        dao = create_portfolio_master_dao(engine)
        with pytest.raises(VSAMError) as exc_info:
            dao.read_by_key(portfolio_id="PORT0001")
        assert exc_info.value.status == VSAMStatus.LOGIC_ERROR


# -----------------------------------------------------------------------
# READ NEXT (sequential browse) tests
# -----------------------------------------------------------------------
class TestReadNext:
    def test_sequential_browse_all(self, engine):
        dao = create_portfolio_master_dao(engine)
        dao.write(_make_portfolio(pid="SEQ00001"))
        dao.write(_make_portfolio(pid="SEQ00002"))
        dao.write(_make_portfolio(pid="SEQ00003"))

        records = dao.read_all()
        assert len(records) >= 3
        # Verify key-sequenced order
        ids = [r.portfolio_id for r in records]
        assert ids == sorted(ids)

    def test_sequential_browse_with_start_key(self, engine):
        dao = create_portfolio_master_dao(engine)
        dao.write(_make_portfolio(pid="BRW00001"))
        dao.write(_make_portfolio(pid="BRW00002"))
        dao.write(_make_portfolio(pid="BRW00003"))

        records = dao.read_all(start_key={"portfolio_id": "BRW00002"})
        ids = [r.portfolio_id for r in records]
        assert "BRW00001" not in ids
        assert "BRW00002" in ids
        assert "BRW00003" in ids

    def test_cursor_read_next_returns_none_at_eof(self, engine):
        dao = create_portfolio_master_dao(engine)
        dao.write(_make_portfolio(pid="EOF00001"))

        cursor = dao.open_cursor(start_key={"portfolio_id": "EOF00001"})
        record = dao.read_next(cursor)
        assert record is not None

        # Continue reading until EOF
        while dao.read_next(cursor) is not None:
            pass

        # After EOF, subsequent reads return None
        assert dao.read_next(cursor) is None

    def test_transaction_sequential_browse(self, engine):
        dao = create_transaction_history_dao(engine)
        dao.write(_make_transaction(date="20240101", seq="000001"))
        dao.write(_make_transaction(date="20240102", seq="000001"))
        dao.write(_make_transaction(date="20240103", seq="000001"))

        records = dao.read_all()
        dates = [r.transaction_date for r in records]
        assert dates == sorted(dates)

    def test_multi_column_start_key_composite_comparison(self, engine):
        """Verify composite tuple comparison for multi-column start_key.

        Regression test: per-column >= filters would incorrectly exclude
        records like ("20240316", "080000") when start_key is
        {"transaction_date": "20240315", "transaction_time": "120000"}
        because "080000" < "120000" even though the composite key is greater.
        """
        dao = create_transaction_history_dao(engine)
        # Record BEFORE the start key (should be excluded)
        dao.write(_make_transaction(date="20240314", time="180000", seq="000010"))
        # Record AT the start key (should be included)
        dao.write(_make_transaction(date="20240315", time="120000", seq="000011"))
        # Record with later date but earlier time (should be included
        # with correct composite comparison, was excluded by the old bug)
        dao.write(_make_transaction(date="20240316", time="080000", seq="000012"))
        # Record clearly after (should be included)
        dao.write(_make_transaction(date="20240317", time="140000", seq="000013"))

        records = dao.read_all(
            start_key={
                "transaction_date": "20240315",
                "transaction_time": "120000",
            }
        )
        dates_times = [
            (r.transaction_date, r.transaction_time) for r in records
        ]
        # The record before start_key must be excluded
        assert ("20240314", "180000") not in dates_times
        # All three records at or after the composite start key must appear
        assert ("20240315", "120000") in dates_times
        assert ("20240316", "080000") in dates_times  # This was the bug
        assert ("20240317", "140000") in dates_times


# -----------------------------------------------------------------------
# REWRITE operation tests
# -----------------------------------------------------------------------
class TestRewrite:
    def test_rewrite_updates_data_fields(self, engine):
        dao = create_portfolio_master_dao(engine)
        dao.write(_make_portfolio(pid="RWR00001", name="Original Name"))

        updated = _make_portfolio(pid="RWR00001", name="Updated Name")
        status = dao.rewrite(updated)
        assert status == VSAMStatus.SUCCESS

        record = dao.read_by_key(
            portfolio_id="RWR00001", account_type="IN", branch_id="01"
        )
        assert record.client_name == "Updated Name"

    def test_rewrite_nonexistent_raises_error(self, engine):
        dao = create_portfolio_master_dao(engine)
        record = _make_portfolio(pid="NOREC001")
        with pytest.raises(VSAMError) as exc_info:
            dao.rewrite(record)
        assert exc_info.value.status == VSAMStatus.RECORD_NOT_FOUND

    def test_rewrite_position_history(self, engine):
        dao = create_position_history_dao(engine)
        dao.write(_make_position(pid="RWRP0001", date="20240401"))

        updated = _make_position(pid="RWRP0001", date="20240401")
        updated.market_value = 99999.99
        updated.status = "C"
        dao.rewrite(updated)

        record = dao.read_by_key(
            portfolio_id="RWRP0001",
            position_date="20240401",
            investment_id="INV0000001",
        )
        assert float(record.market_value) == 99999.99
        assert record.status == "C"


# -----------------------------------------------------------------------
# DELETE BY KEY operation tests
# -----------------------------------------------------------------------
class TestDeleteByKey:
    def test_delete_existing_record(self, engine):
        dao = create_portfolio_master_dao(engine)
        dao.write(_make_portfolio(pid="DEL00001"))
        status = dao.delete_by_key(
            portfolio_id="DEL00001", account_type="IN", branch_id="01"
        )
        assert status == VSAMStatus.SUCCESS

        with pytest.raises(VSAMError) as exc_info:
            dao.read_by_key(
                portfolio_id="DEL00001", account_type="IN", branch_id="01"
            )
        assert exc_info.value.status == VSAMStatus.RECORD_NOT_FOUND

    def test_delete_nonexistent_raises_error(self, engine):
        dao = create_portfolio_master_dao(engine)
        with pytest.raises(VSAMError) as exc_info:
            dao.delete_by_key(
                portfolio_id="NODEL001", account_type="XX", branch_id="99"
            )
        assert exc_info.value.status == VSAMStatus.RECORD_NOT_FOUND

    def test_delete_incomplete_key_raises_error(self, engine):
        dao = create_portfolio_master_dao(engine)
        with pytest.raises(VSAMError) as exc_info:
            dao.delete_by_key(portfolio_id="DEL00001")
        assert exc_info.value.status == VSAMStatus.LOGIC_ERROR

    def test_delete_transaction(self, engine):
        dao = create_transaction_history_dao(engine)
        dao.write(_make_transaction(date="20240501", seq="999999"))
        status = dao.delete_by_key(
            transaction_date="20240501",
            transaction_time="143022",
            portfolio_id="PORT0001",
            sequence_no="999999",
        )
        assert status == VSAMStatus.SUCCESS


# -----------------------------------------------------------------------
# VSAM status code tests
# -----------------------------------------------------------------------
class TestVSAMStatus:
    def test_status_codes(self):
        assert VSAMStatus.SUCCESS == "00"
        assert VSAMStatus.DUPLICATE_KEY == "22"
        assert VSAMStatus.RECORD_NOT_FOUND == "23"
        assert VSAMStatus.END_OF_FILE == "10"
        assert VSAMStatus.SEQUENCE_ERROR == "21"
        assert VSAMStatus.LOGIC_ERROR == "92"

    def test_vsam_error_attributes(self):
        err = VSAMError("23", "Record not found")
        assert err.status == "23"
        assert err.message == "Record not found"
        assert "23" in str(err)
        assert "Record not found" in str(err)
