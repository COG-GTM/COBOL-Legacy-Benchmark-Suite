"""COBOL data-item semantics required by the translated programs.

Only the semantics exercised by translated programs are implemented here. Every
helper mirrors an Enterprise COBOL construct so that a translated program can be
read side by side with its COBOL original.
"""

from __future__ import annotations

import re
from decimal import Decimal

_NUMERIC_LITERAL = re.compile(r"^([+-]?)(\d*)(?:\.(\d*))?$")


def alphanumeric(value: str, length: int) -> str:
    """MOVE into a ``PIC X(length)`` item: left justified, space padded, truncated."""
    return value[:length].ljust(length)


def is_numeric(value: str) -> bool:
    """``IS NUMERIC`` class test on an alphanumeric item.

    Only unsigned digits satisfy the test; embedded or trailing spaces do not.
    """
    return len(value) > 0 and all("0" <= char <= "9" for char in value)


def move_to_numeric(value: str, digits: int, scale: int) -> Decimal:
    """MOVE an alphanumeric item into a ``PIC S9(digits)V9(scale)`` item.

    Enterprise COBOL leaves this move undefined for non-numeric content; the
    reference behaviour pinned here is GnuCOBOL's: the text is parsed as a
    signed decimal literal (surrounding spaces and group separators ignored),
    unparsable content yields zero, excess decimal places are truncated toward
    zero and high order digits beyond ``digits`` are dropped.
    """
    match = _NUMERIC_LITERAL.match(value.strip().replace(",", ""))
    if match is None:
        return Decimal(0)

    sign, integer, fraction = match.groups()
    if not integer and not fraction:
        return Decimal(0)

    integer = integer[-digits:] if digits else ""
    fraction = (fraction or "")[:scale].ljust(scale, "0")
    return Decimal(f"{sign}{integer or '0'}.{fraction or '0'}")
