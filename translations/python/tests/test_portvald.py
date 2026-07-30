"""PORTVALD translation against the recorded behaviour of the COBOL original."""

from __future__ import annotations

import pytest
from conftest import load_vectors

from clbs.portfolio.portvald import ValidationRequest, portvald

VECTORS = load_vectors("PORTVALD")


@pytest.mark.parametrize("case", VECTORS, ids=[case["name"] for case in VECTORS])
def test_matches_recorded_cobol_behaviour(case: dict[str, object]) -> None:
    request = ValidationRequest(
        validate_type=str(case["validate_type"]), input_value=str(case["input_value"])
    )

    portvald(request)

    assert request.return_code == case["return_code"]
    assert request.error_msg.rstrip() == case["error_msg"]


def test_request_fields_keep_their_picture_length() -> None:
    request = ValidationRequest(validate_type="T", input_value="STK")

    portvald(request)

    assert len(request.input_value) == 50
    assert len(request.error_msg) == 50
