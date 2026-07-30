"""Translation of ``src/programs/portfolio/PORTVALD.cbl`` (subprogram PORTVALD).

The COBOL subprogram validates one data element per call and reports the
outcome through its linkage record. The translation is behaviour preserving:
observable results match the COBOL program for every input, including the
cases where the COBOL logic does not match its own comments. Those cases are
listed in ``translations/python/README.md`` and are locked down by the parity
tests against the GnuCOBOL oracle.
"""

from dataclasses import dataclass

from clbs.copybook.common.portval import (
    AMOUNT_DECIMAL_DIGITS,
    AMOUNT_INTEGER_DIGITS,
    MESSAGE_LENGTH,
    VAL_ERR_ACCT,
    VAL_ERR_AMT,
    VAL_ERR_ID,
    VAL_ERR_TYPE,
    VAL_ID_PREFIX,
    VAL_INVALID_ACCT,
    VAL_INVALID_AMT,
    VAL_INVALID_ID,
    VAL_INVALID_TYPE,
    VAL_MAX_AMOUNT,
    VAL_MIN_AMOUNT,
    VAL_SUCCESS,
)
from clbs.runtime.picture import is_numeric, move_alphanumeric_to_numeric, pic_x

INPUT_VALUE_LENGTH = 50

VALIDATE_ID = "I"
VALIDATE_ACCOUNT = "A"
VALIDATE_TYPE = "T"
VALIDATE_AMOUNT = "M"

NUMERIC_CHECK_LENGTH = 10
ID_PREFIX = slice(0, 4)
ID_SEQUENCE = slice(4, 8)
INVESTMENT_TYPES = ("STK", "BND", "MMF", "ETF")


@dataclass
class ValidationRequest:
    """``LS-VALIDATION-REQUEST`` linkage record.

    ``input_value`` is stored as the caller passes it; the program reads it as
    a ``PIC X(50)`` field. ``error_msg`` is space filled to 50 characters like
    its COBOL counterpart.
    """

    validate_type: str
    input_value: str
    return_code: int = 0
    error_msg: str = " " * MESSAGE_LENGTH


def portvald(request: ValidationRequest) -> None:
    """PROCEDURE DIVISION USING LS-VALIDATION-REQUEST."""
    value = pic_x(request.input_value, INPUT_VALUE_LENGTH)

    if request.validate_type == VALIDATE_ID:
        _validate_id(request, value)
    elif request.validate_type == VALIDATE_ACCOUNT:
        _validate_account(request, value)
    elif request.validate_type == VALIDATE_TYPE:
        _validate_type(request, value)
    elif request.validate_type == VALIDATE_AMOUNT:
        _validate_amount(request, value)
    else:
        _fail(request, VAL_INVALID_ID, "Invalid validation type")


def _validate_id(request: ValidationRequest, value: str) -> None:
    """1000-VALIDATE-ID: prefix ``PORT`` followed by four numeric digits.

    The digit check moves the four character sequence into a ``PIC X(10)``
    work field, so the field always carries six trailing spaces and the
    numeric class test always fails.
    """
    if value[ID_PREFIX] != VAL_ID_PREFIX:
        _fail(request, VAL_INVALID_ID, VAL_ERR_ID)
        return

    numeric_check = pic_x(value[ID_SEQUENCE], NUMERIC_CHECK_LENGTH)
    if not is_numeric(numeric_check):
        _fail(request, VAL_INVALID_ID, VAL_ERR_ID)
        return

    _succeed(request)


def _validate_account(request: ValidationRequest, value: str) -> None:
    """2000-VALIDATE-ACCOUNT: ten numeric digits.

    The class test is applied to the whole ``PIC X(50)`` field rather than to
    the ten significant characters, so any account number shorter than 50
    digits fails.
    """
    if not is_numeric(value) or value == "0" * INPUT_VALUE_LENGTH:
        _fail(request, VAL_INVALID_ACCT, VAL_ERR_ACCT)
        return

    _succeed(request)


def _validate_type(request: ValidationRequest, value: str) -> None:
    """3000-VALIDATE-TYPE: investment type must be a known code."""
    if value not in [pic_x(code, INPUT_VALUE_LENGTH) for code in INVESTMENT_TYPES]:
        _fail(request, VAL_INVALID_TYPE, VAL_ERR_TYPE)
        return

    _succeed(request)


def _validate_amount(request: ValidationRequest, value: str) -> None:
    """4000-VALIDATE-AMOUNT: amount must fall inside the permitted range.

    The range spans the full capacity of the ``PIC S9(13)V99`` work field, so
    the conversion cannot produce a value outside it and the check never
    rejects an input.
    """
    amount = move_alphanumeric_to_numeric(
        value, AMOUNT_INTEGER_DIGITS, AMOUNT_DECIMAL_DIGITS
    )

    if amount < VAL_MIN_AMOUNT or amount > VAL_MAX_AMOUNT:
        _fail(request, VAL_INVALID_AMT, VAL_ERR_AMT)
        return

    _succeed(request)


def _succeed(request: ValidationRequest) -> None:
    request.return_code = VAL_SUCCESS
    request.error_msg = " " * MESSAGE_LENGTH


def _fail(request: ValidationRequest, return_code: int, message: str) -> None:
    request.return_code = return_code
    request.error_msg = pic_x(message, MESSAGE_LENGTH)
