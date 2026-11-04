"""PICTURE clause parsing.

A PIC clause defines both the *precision* of a field (how many digits, where
the implied decimal point sits, whether a sign is kept) and, together with the
USAGE clause, its *byte representation*. Everything else in this package is
derived from the ``Pic`` produced here.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from enum import Enum

_REPEAT = re.compile(r"([A-Za-z90/,.+\-*$])\((\d+)\)")


class PicKind(Enum):
    ALPHANUMERIC = "alphanumeric"  # X / A
    NUMERIC = "numeric"            # 9 / S / V / P
    EDITED = "edited"              # Z, *, comma, +, -, CR, DB, $, / ...


@dataclass(frozen=True)
class Pic:
    raw: str
    kind: PicKind
    signed: bool = False
    int_digits: int = 0
    scale: int = 0
    scale_shift: int = 0  # P positions: 9(3)P(2) stores 3 digits but means x100
    length: int = 0       # display length in characters

    @property
    def digits(self) -> int:
        return self.int_digits + self.scale

    @property
    def max_value(self) -> int:
        """Largest unscaled magnitude the PIC can hold (10**digits - 1)."""
        return 10 ** self.digits - 1

    def __str__(self) -> str:  # pragma: no cover - cosmetic
        return self.raw


def expand(pic: str) -> str:
    """Expand repetition factors: ``S9(5)V99`` -> ``S99999V99``."""
    return _REPEAT.sub(lambda m: m.group(1) * int(m.group(2)), pic)


def parse_pic(pic: str) -> Pic:
    raw = pic.strip()
    text = expand(raw).upper()
    if not text:
        raise ValueError("empty PICTURE clause")

    if set(text) <= set("XA"):
        return Pic(raw=raw, kind=PicKind.ALPHANUMERIC, length=len(text))
    if set(text) <= set("XA9") and (set(text) & set("XA")):
        return Pic(raw=raw, kind=PicKind.ALPHANUMERIC, length=len(text))
    bad = set(text) - set("9SVPXAZ*+-.,/B0$CRD")
    if bad:
        raise ValueError(f"invalid PICTURE character(s) {sorted(bad)} in {raw!r}")

    if set(text) <= set("9SVP"):
        signed = text.startswith("S") or text.endswith("S")
        body = text.strip("S")
        if "S" in body:
            raise ValueError(f"misplaced S in PICTURE {raw!r}")
        if body.count("V") > 1:
            raise ValueError(f"more than one V in PICTURE {raw!r}")
        int_part, _, frac_part = body.partition("V")
        # P = assumed scaling positions; they take no storage.
        lead_p = len(int_part) - len(int_part.lstrip("P"))
        trail_p = len(frac_part) - len(frac_part.rstrip("P")) if frac_part else 0
        if lead_p == 0 and int_part.endswith("P"):
            # 9(3)P(2): P after the integer digits, no V
            trail_p_int = len(int_part) - len(int_part.rstrip("P"))
            int_digits = int_part.count("9")
            return Pic(raw, PicKind.NUMERIC, signed, int_digits, 0,
                       scale_shift=trail_p_int, length=int_digits)
        int_digits = int_part.count("9")
        scale = frac_part.count("9")
        shift = -lead_p if lead_p else trail_p
        return Pic(raw, PicKind.NUMERIC, signed, int_digits, scale,
                   scale_shift=shift, length=int_digits + scale)

    # Numeric-edited / alphanumeric-edited: edited fields are always DISPLAY
    # and are decoded as text; digits/scale are recorded for documentation.
    length = 0
    i = 0
    signed = "CR" in text or "DB" in text or "+" in text or "-" in text
    int_digits = scale = 0
    seen_point = False
    floating = {"+": 0, "-": 0, "$": 0}
    while i < len(text):
        two = text[i:i + 2]
        if two in ("CR", "DB"):
            length += 2
            i += 2
            continue
        c = text[i]
        length += 1
        i += 1
        if c in ".V":
            seen_point = True
        elif c in "9Z*":
            if seen_point:
                scale += 1
            else:
                int_digits += 1
        elif c in floating:
            floating[c] += 1
            if floating[c] > 1:           # 2nd+ floating symbol is a digit position
                if seen_point:
                    scale += 1
                else:
                    int_digits += 1
    return Pic(raw=raw, kind=PicKind.EDITED, signed=signed, int_digits=int_digits,
               scale=scale, length=length)
