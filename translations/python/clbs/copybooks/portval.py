"""Translation of ``src/copybook/common/PORTVAL.cpy``.

Portfolio validation return codes, error messages and constants. Group items in
the copybook become module level constants; the ``VAL-WORK-AREAS`` group holds
scratch fields only and therefore has no counterpart here.
"""

from __future__ import annotations

from decimal import Decimal

from clbs.runtime.picture import alphanumeric

VAL_SUCCESS = 0
VAL_INVALID_ID = 1
VAL_INVALID_ACCT = 2
VAL_INVALID_TYPE = 3
VAL_INVALID_AMT = 4

ERROR_MESSAGE_LENGTH = 50

VAL_ERR_ID = alphanumeric("Invalid Portfolio ID format", ERROR_MESSAGE_LENGTH)
VAL_ERR_ACCT = alphanumeric("Invalid Account Number format", ERROR_MESSAGE_LENGTH)
VAL_ERR_TYPE = alphanumeric("Invalid Investment Type", ERROR_MESSAGE_LENGTH)
VAL_ERR_AMT = alphanumeric("Amount outside valid range", ERROR_MESSAGE_LENGTH)

VAL_MIN_AMOUNT = Decimal("-9999999999999.99")
VAL_MAX_AMOUNT = Decimal("9999999999999.99")
VAL_ID_PREFIX = "PORT"

VAL_AMOUNT_DIGITS = 13
VAL_AMOUNT_SCALE = 2
VAL_NUMERIC_CHECK_LENGTH = 10
