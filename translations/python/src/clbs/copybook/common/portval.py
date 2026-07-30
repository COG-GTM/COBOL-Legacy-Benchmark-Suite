"""Translation of ``src/copybook/common/PORTVAL.cpy``.

Portfolio validation return codes, error messages and constants. Message
constants keep the ``PIC X(50)`` padding of the copybook so that receiving
fields match the mainframe byte for byte.
"""

from decimal import Decimal

from clbs.runtime.picture import pic_x

MESSAGE_LENGTH = 50

VAL_SUCCESS = 0
VAL_INVALID_ID = 1
VAL_INVALID_ACCT = 2
VAL_INVALID_TYPE = 3
VAL_INVALID_AMT = 4

VAL_ERR_ID = pic_x("Invalid Portfolio ID format", MESSAGE_LENGTH)
VAL_ERR_ACCT = pic_x("Invalid Account Number format", MESSAGE_LENGTH)
VAL_ERR_TYPE = pic_x("Invalid Investment Type", MESSAGE_LENGTH)
VAL_ERR_AMT = pic_x("Amount outside valid range", MESSAGE_LENGTH)

VAL_MIN_AMOUNT = Decimal("-9999999999999.99")
VAL_MAX_AMOUNT = Decimal("9999999999999.99")
VAL_ID_PREFIX = "PORT"

AMOUNT_INTEGER_DIGITS = 13
AMOUNT_DECIMAL_DIGITS = 2
