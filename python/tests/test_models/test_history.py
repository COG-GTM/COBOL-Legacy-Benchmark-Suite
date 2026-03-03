"""Tests for HistoryRecord models (HISTREC copybook)."""

import pytest
from pydantic import ValidationError

from src.models.history import (
    HistoryAudit,
    HistoryData,
    HistoryKey,
    HistoryRecord,
)


class TestHistoryData:
    def test_valid_data(self):
        data = HistoryData(
            hist_record_type="PT",
            hist_action_code="A",
            hist_before_image="",
            hist_after_image="some after image",
            hist_reason_code="NEW",
        )
        assert data.hist_record_type == "PT"
        assert data.hist_action_code == "A"

    def test_all_record_types(self):
        for rt in ("PT", "PS", "TR"):
            data = HistoryData(
                hist_record_type=rt,
                hist_action_code="A",
                hist_before_image="",
                hist_after_image="",
                hist_reason_code="TST",
            )
            assert data.hist_record_type == rt

    def test_all_action_codes(self):
        for ac in ("A", "C", "D"):
            data = HistoryData(
                hist_record_type="PT",
                hist_action_code=ac,
                hist_before_image="",
                hist_after_image="",
                hist_reason_code="TST",
            )
            assert data.hist_action_code == ac

    def test_invalid_record_type(self):
        with pytest.raises(ValidationError, match="hist_record_type must be one of"):
            HistoryData(
                hist_record_type="XX",
                hist_action_code="A",
                hist_before_image="",
                hist_after_image="",
                hist_reason_code="TST",
            )

    def test_invalid_action_code(self):
        with pytest.raises(ValidationError, match="hist_action_code must be one of"):
            HistoryData(
                hist_record_type="PT",
                hist_action_code="Z",
                hist_before_image="",
                hist_after_image="",
                hist_reason_code="TST",
            )

    def test_before_image_max_length(self):
        with pytest.raises(ValidationError):
            HistoryData(
                hist_record_type="PT",
                hist_action_code="A",
                hist_before_image="x" * 401,
                hist_after_image="",
                hist_reason_code="TST",
            )


class TestHistoryRecord:
    def test_full_record(self):
        record = HistoryRecord(
            hist_key=HistoryKey(
                hist_portfolio_id="PORT0001",
                hist_date="20240315",
                hist_time="143022",
                hist_seq_no="0001",
            ),
            hist_data=HistoryData(
                hist_record_type="TR",
                hist_action_code="A",
                hist_before_image="before",
                hist_after_image="after",
                hist_reason_code="BUY",
            ),
            hist_audit=HistoryAudit(
                hist_process_date="2024-03-15T14:30:22.000000",
                hist_process_user="ADMIN01",
            ),
        )
        assert record.hist_key.hist_portfolio_id == "PORT0001"
        assert record.hist_data.hist_record_type == "TR"
        assert record.hist_filler == ""
