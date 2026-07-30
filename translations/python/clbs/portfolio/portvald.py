"""Translation of ``src/programs/portfolio/PORTVALD.cbl``.

Portfolio validation subroutine. ``PROCEDURE DIVISION USING
LS-VALIDATION-REQUEST`` becomes :func:`portvald`, which mutates the request in
place the way a COBOL CALL BY REFERENCE does; each paragraph becomes a private
function of the same name.

The translation is behaviour preserving, including the quirks of the original
(documented in ``documentation/technical/python-migration-plan.md``): validation
types ``I`` and ``A`` can never succeed because the class test runs against a
space padded item, and type ``M`` can never fail because the bounds are the
extremes of the receiving picture.
"""

from __future__ import annotations

from dataclasses import dataclass

from clbs.copybooks import portval
from clbs.runtime.picture import alphanumeric, is_numeric, move_to_numeric

VALIDATE_TYPE_LENGTH = 1
INPUT_VALUE_LENGTH = 50

VAL_ID = "I"
VAL_ACCT = "A"
VAL_TYPE = "T"
VAL_AMT = "M"

VALID_INVESTMENT_TYPES = ("STK", "BND", "MMF", "ETF")

SPACES = alphanumeric("", portval.ERROR_MESSAGE_LENGTH)

_PADDED_INVESTMENT_TYPES = tuple(
    alphanumeric(investment_type, INPUT_VALUE_LENGTH)
    for investment_type in VALID_INVESTMENT_TYPES
)


@dataclass
class ValidationRequest:
    """``01 LS-VALIDATION-REQUEST`` in the LINKAGE SECTION.

    Fields are held at their picture length so that comparisons and returned
    messages match the COBOL record byte for byte.
    """

    validate_type: str
    input_value: str
    return_code: int = portval.VAL_SUCCESS
    error_msg: str = SPACES

    def __post_init__(self) -> None:
        self.validate_type = alphanumeric(self.validate_type, VALIDATE_TYPE_LENGTH)
        self.input_value = alphanumeric(self.input_value, INPUT_VALUE_LENGTH)
        self.error_msg = alphanumeric(self.error_msg, portval.ERROR_MESSAGE_LENGTH)


def portvald(request: ValidationRequest) -> None:
    """0000-MAIN."""
    if request.validate_type == VAL_ID:
        _validate_id(request)
    elif request.validate_type == VAL_ACCT:
        _validate_account(request)
    elif request.validate_type == VAL_TYPE:
        _validate_type(request)
    elif request.validate_type == VAL_AMT:
        _validate_amount(request)
    else:
        request.return_code = portval.VAL_INVALID_ID
        request.error_msg = alphanumeric(
            "Invalid validation type", portval.ERROR_MESSAGE_LENGTH
        )


def _validate_id(request: ValidationRequest) -> None:
    """1000-VALIDATE-ID: portfolio ID must start with 'PORT' and have 4 numeric digits."""
    if request.input_value[:4] != portval.VAL_ID_PREFIX:
        _reject(request, portval.VAL_INVALID_ID, portval.VAL_ERR_ID)
        return

    numeric_check = alphanumeric(
        request.input_value[4:8], portval.VAL_NUMERIC_CHECK_LENGTH
    )
    if not is_numeric(numeric_check):
        _reject(request, portval.VAL_INVALID_ID, portval.VAL_ERR_ID)
        return

    _accept(request)


def _validate_account(request: ValidationRequest) -> None:
    """2000-VALIDATE-ACCOUNT: account number must be 10 numeric digits."""
    all_zeros = request.input_value == "0" * INPUT_VALUE_LENGTH
    if not is_numeric(request.input_value) or all_zeros:
        _reject(request, portval.VAL_INVALID_ACCT, portval.VAL_ERR_ACCT)
        return

    _accept(request)


def _validate_type(request: ValidationRequest) -> None:
    """3000-VALIDATE-TYPE: investment type must be a known value."""
    if request.input_value not in _PADDED_INVESTMENT_TYPES:
        _reject(request, portval.VAL_INVALID_TYPE, portval.VAL_ERR_TYPE)
        return

    _accept(request)


def _validate_amount(request: ValidationRequest) -> None:
    """4000-VALIDATE-AMOUNT: amount must be within the valid range."""
    amount = move_to_numeric(
        request.input_value, portval.VAL_AMOUNT_DIGITS, portval.VAL_AMOUNT_SCALE
    )
    if amount < portval.VAL_MIN_AMOUNT or amount > portval.VAL_MAX_AMOUNT:
        _reject(request, portval.VAL_INVALID_AMT, portval.VAL_ERR_AMT)
        return

    _accept(request)


def _accept(request: ValidationRequest) -> None:
    request.return_code = portval.VAL_SUCCESS
    request.error_msg = SPACES


def _reject(request: ValidationRequest, return_code: int, error_msg: str) -> None:
    request.return_code = return_code
    request.error_msg = error_msg
