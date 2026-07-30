"""Parity tests for the PORTVALD translation pair.

The expectations are not hand written: ``oracle/generate_golden.sh`` compiles
``src/programs/portfolio/PORTVALD.cbl`` with GnuCOBOL and records the linkage
results of every case in ``tests/golden/portvald.txt``. A translation is
accepted only when it reproduces those results exactly.
"""

from decimal import Decimal
from pathlib import Path

import pytest

from clbs.programs.portfolio.portvald import ValidationRequest, portvald
from clbs.runtime.picture import is_numeric, move_alphanumeric_to_numeric, pic_x

PAIR_DIR = Path(__file__).resolve().parents[1]
CASE_FILE = PAIR_DIR / "oracle" / "portvald_cases.txt"
GOLDEN_FILE = PAIR_DIR / "tests" / "golden" / "portvald.txt"

CASE_ID_COLUMNS = slice(0, 4)
TYPE_COLUMN = slice(5, 6)
VALUE_COLUMNS = slice(7, 57)


def load_cases() -> dict[str, tuple[str, str]]:
    cases = {}
    for line in CASE_FILE.read_text().splitlines():
        if not line.strip() or line.startswith("*"):
            continue
        padded = line.ljust(57)
        cases[padded[CASE_ID_COLUMNS]] = (
            padded[TYPE_COLUMN],
            padded[VALUE_COLUMNS],
        )
    return cases


def load_golden() -> dict[str, tuple[int, str]]:
    golden = {}
    for line in GOLDEN_FILE.read_text().splitlines():
        case_id, return_code, error_msg = line.split("|")
        golden[case_id] = (int(return_code), error_msg.rstrip())
    return golden


CASES = load_cases()
GOLDEN = load_golden()


def test_every_case_has_an_oracle_result() -> None:
    assert CASES and CASES.keys() == GOLDEN.keys()


@pytest.mark.parametrize("case_id", sorted(CASES))
def test_matches_cobol_oracle(case_id: str) -> None:
    validate_type, input_value = CASES[case_id]
    request = ValidationRequest(validate_type=validate_type, input_value=input_value)

    portvald(request)

    assert (request.return_code, request.error_msg.rstrip()) == GOLDEN[case_id]


def test_error_message_keeps_cobol_padding() -> None:
    request = ValidationRequest(validate_type="T", input_value="XYZ")

    portvald(request)

    assert request.error_msg == pic_x("Invalid Investment Type", 50)


def test_numeric_class_test_rejects_padded_field() -> None:
    assert is_numeric("1234")
    assert not is_numeric("1234      ")
    assert not is_numeric("")


@pytest.mark.parametrize(
    ("sender", "expected"),
    [
        ("1000.50", Decimal("1000.50")),
        ("0000000000100050", Decimal("100050.00")),
        ("-5", Decimal("-5.00")),
        ("12.3456", Decimal("12.34")),
        ("99999999999999999", Decimal("9999999999999.00")),
        ("1,000", Decimal("1000.00")),
        ("12 34", Decimal("1234.00")),
        ("+7", Decimal("7.00")),
        ("ABC", Decimal("0.00")),
        ("1.2.3", Decimal("0.00")),
        ("5-", Decimal("0.00")),
        ("", Decimal("0.00")),
    ],
)
def test_alphanumeric_to_numeric_move(sender: str, expected: Decimal) -> None:
    """Conversions verified against GnuCOBOL 3.1.2 with a MOVE to S9(13)V99."""
    assert move_alphanumeric_to_numeric(sender, 13, 2) == expected
