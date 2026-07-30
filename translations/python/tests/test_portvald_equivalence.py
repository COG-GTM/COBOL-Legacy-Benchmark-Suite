"""Differential test: the Python translation must agree with compiled COBOL.

Skipped when GnuCOBOL is not installed, so the suite still runs on machines that
only have Python.
"""

from __future__ import annotations

from pathlib import Path

import pytest
from conftest import load_vectors

from clbs.portfolio.portvald import ValidationRequest, portvald
from tools.cobol_bridge import (
    REPO_ROOT,
    CobolUnavailableError,
    call_program,
    compile_module,
)

PROGRAM = "PORTVALD"
SOURCE = REPO_ROOT / "src" / "programs" / "portfolio" / f"{PROGRAM}.cbl"

# 01 LS-VALIDATION-REQUEST: X(1) + X(50) + S9(4) COMP + X(50).
TYPE_SLICE = slice(0, 1)
INPUT_SLICE = slice(1, 51)
RETURN_CODE_SLICE = slice(51, 53)
ERROR_MSG_SLICE = slice(53, 103)
LINKAGE_LENGTH = 103

VECTORS = load_vectors(PROGRAM)


@pytest.fixture(scope="session")
def compiled_program(cobol_module_dir: Path) -> Path:
    try:
        return compile_module(SOURCE, cobol_module_dir)
    except CobolUnavailableError as error:
        pytest.skip(str(error))


def call_cobol(validate_type: str, input_value: str, module_dir: Path) -> tuple[int, str]:
    linkage = bytearray(b" " * LINKAGE_LENGTH)
    linkage[TYPE_SLICE] = validate_type.encode().ljust(1)[:1]
    linkage[INPUT_SLICE] = input_value.encode().ljust(50)[:50]
    linkage[RETURN_CODE_SLICE] = b"\x00\x00"

    call_program(PROGRAM, linkage, module_dir)

    return (
        int.from_bytes(linkage[RETURN_CODE_SLICE], "big", signed=True),
        linkage[ERROR_MSG_SLICE].decode(),
    )


@pytest.mark.parametrize("case", VECTORS, ids=[case["name"] for case in VECTORS])
def test_python_matches_cobol(
    case: dict[str, object], compiled_program: Path, cobol_module_dir: Path
) -> None:
    validate_type = str(case["validate_type"])
    input_value = str(case["input_value"])

    cobol_return_code, cobol_error_msg = call_cobol(
        validate_type, input_value, cobol_module_dir
    )
    request = ValidationRequest(validate_type=validate_type, input_value=input_value)
    portvald(request)

    assert (request.return_code, request.error_msg) == (
        cobol_return_code,
        cobol_error_msg,
    )
