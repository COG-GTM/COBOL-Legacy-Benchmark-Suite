"""COBOL data-item semantics expressed in Python.

Translated programs must not use Python's native string and number semantics
where COBOL's differ. The helpers below encode the COBOL rules that the
translations depend on. The reference implementation is GnuCOBOL 3.1.2, which
is the oracle used by the parity tests; where IBM Enterprise COBOL for z/OS is
known to differ, the difference is documented on the helper.
"""

from decimal import Decimal

DIGITS = "0123456789"


def pic_x(value: str, length: int) -> str:
    """MOVE an alphanumeric sender into a ``PIC X(length)`` receiver.

    The receiver is left justified, space filled on the right and truncated on
    the right when the sender is longer.
    """
    return value[:length].ljust(length)


def is_numeric(value: str) -> bool:
    """``IS NUMERIC`` class test for an alphanumeric (``PIC X``) item.

    True only when every character position holds a digit, so trailing spaces
    of a partially filled field make the test fail.
    """
    return len(value) > 0 and all(char in DIGITS for char in value)


def move_alphanumeric_to_numeric(
    value: str, integer_digits: int, decimal_digits: int
) -> Decimal:
    """MOVE an alphanumeric sender into a signed ``PIC S9(i)V9(d)`` receiver.

    GnuCOBOL scans the sender as a free form number: spaces and commas are
    ignored, an optional sign may precede the first digit, and at most one
    decimal point is honoured. Any other character, a second decimal point or a
    trailing sign makes the whole conversion yield zero. Excess fraction digits
    are truncated and high order digits beyond the receiver's capacity are
    dropped.

    IBM Enterprise COBOL treats the sender of such a MOVE as an unsigned
    integer instead, so a sender holding ``'-12.34'`` converts differently
    there. Programs that reach this helper are flagged in the divergence log of
    their translation pair.
    """
    sign = 1
    seen_sign = False
    seen_point = False
    integer_part: list[str] = []
    fraction_part: list[str] = []

    for char in value:
        if char in " ,":
            continue
        if char in "+-":
            if seen_sign or seen_point or integer_part:
                return Decimal(0)
            seen_sign = True
            sign = -1 if char == "-" else 1
            continue
        if char == ".":
            if seen_point:
                return Decimal(0)
            seen_point = True
            continue
        if char not in DIGITS:
            return Decimal(0)
        if seen_point:
            fraction_part.append(char)
        else:
            integer_part.append(char)

    digits = "".join(integer_part)[-integer_digits:] or "0"
    fraction = "".join(fraction_part)[:decimal_digits].ljust(decimal_digits, "0")
    return Decimal(f"{digits}.{fraction}") * sign
