"""COBOL field definitions and byte-level codecs.

One ``CobolField`` = one elementary item: its PIC, its USAGE, and the rules
for turning a Python value into the exact bytes COBOL would store (and back).

Mapping summary
---------------
======================  ==========================  ======================
USAGE                   Storage                     Python value
======================  ==========================  ======================
DISPLAY  PIC X/A        1 char per byte (EBCDIC or  ``str``
                        ASCII)
DISPLAY  PIC 9/S9       zoned decimal, sign         ``Decimal``
                        overpunched in last byte
COMP / BINARY / COMP-4  big-endian two's complement ``Decimal`` (``int`` for
                        2/4/8 bytes, truncated to   scale 0 is accepted on
                        PIC digits (TRUNC(STD))     encode)
COMP-5                  native binary, full range   ``Decimal``
                        of the storage (TRUNC(BIN))
COMP-X                  big-endian unsigned, byte   ``Decimal``
                        count from PIC, full range
COMP-3 / PACKED-DECIMAL BCD, 2 digits/byte, sign    ``Decimal``
                        nibble C/D/F
======================  ==========================  ======================

Numeric values are always ``decimal.Decimal`` on the Python side. COBOL
arithmetic is fixed-point and deterministic; ``float`` is never used.
"""
from __future__ import annotations

from dataclasses import dataclass, field
import sys
from decimal import Decimal, ROUND_DOWN
from enum import Enum
from typing import Union

from .pic import Pic, PicKind, parse_pic

Number = Union[int, Decimal, str]


class CobolFieldError(ValueError):
    """Raised when a value cannot be represented in a field without loss."""


class Usage(Enum):
    DISPLAY = "DISPLAY"
    COMP = "COMP"        # also BINARY, COMP-4, COMPUTATIONAL(-4)
    COMP_3 = "COMP-3"    # also PACKED-DECIMAL
    COMP_5 = "COMP-5"
    COMP_X = "COMP-X"
    COMP_1 = "COMP-1"    # single float  (4 bytes)  - not used by this suite
    COMP_2 = "COMP-2"    # double float  (8 bytes)  - not used by this suite

    @classmethod
    def parse(cls, token: str) -> "Usage":
        t = token.upper().replace("COMPUTATIONAL", "COMP")
        aliases = {
            "DISPLAY": cls.DISPLAY,
            "COMP": cls.COMP, "COMP-4": cls.COMP, "BINARY": cls.COMP,
            "COMP-3": cls.COMP_3, "PACKED-DECIMAL": cls.COMP_3,
            "COMP-5": cls.COMP_5,
            "COMP-X": cls.COMP_X,
            "COMP-1": cls.COMP_1,
            "COMP-2": cls.COMP_2,
        }
        try:
            return aliases[t]
        except KeyError:
            raise ValueError(f"unknown USAGE {token!r}") from None


class SignMode(Enum):
    TRAILING = "trailing"                    # default: overpunch last byte
    LEADING = "leading"                      # overpunch first byte
    TRAILING_SEPARATE = "trailing-separate"  # '+'/'-' as an extra last byte
    LEADING_SEPARATE = "leading-separate"    # '+'/'-' as an extra first byte

    @property
    def separate(self) -> bool:
        return self in (SignMode.TRAILING_SEPARATE, SignMode.LEADING_SEPARATE)


@dataclass(frozen=True)
class Dialect:
    """Compiler / platform options that change the byte layout."""
    name: str = "ibm"
    #: COMP/BINARY/COMP-5 storage for 1-4 / 5-9 / 10-18 digits.
    #: IBM Enterprise COBOL: 2-4-8. GnuCOBOL default (binary-size 1-2-4-8):
    #: 1 byte for 1-2 digits, 2 for 3-4, 4 for 5-9, 8 for 10-18.
    binary_size: str = "2-4-8"
    #: Character set of DISPLAY data on disk.
    encoding: str = "cp037"
    #: TRUNC(STD) -> True: COMP/BINARY values are truncated to the PIC digit
    #: count on store. TRUNC(BIN) -> False: full binary range (COMP-5 semantics).
    binary_truncate: bool = True
    #: COMP-5 is "native" binary: big-endian on z/OS, little-endian on x86
    #: (GnuCOBOL). COMP/BINARY/COMP-X stay big-endian everywhere.
    comp5_byteorder: str = "big"

    def comp_bytes(self, digits: int) -> int:
        if digits < 1 or digits > 18:
            raise ValueError(f"binary field must have 1-18 digits, got {digits}")
        if self.binary_size == "1-2-4-8":
            if digits <= 2:
                return 1
            if digits <= 4:
                return 2
        elif digits <= 4:
            return 2
        if digits <= 9:
            return 4
        return 8


IBM = Dialect()
GNUCOBOL = Dialect(name="gnucobol", binary_size="1-2-4-8", encoding="ascii", comp5_byteorder=sys.byteorder)
#: GnuCOBOL compiled with -std=ibm: IBM binary sizes, but data is still ASCII
#: and native-endian because the program runs on x86, not z/OS.
GNUCOBOL_IBM = Dialect(name="gnucobol-ibm", binary_size="2-4-8", encoding="ascii", comp5_byteorder=sys.byteorder)
DIALECTS = {d.name: d for d in (IBM, GNUCOBOL, GNUCOBOL_IBM)}

# Zoned-decimal sign zones (high nibble of the overpunched byte).
_EBCDIC_POS, _EBCDIC_NEG, _EBCDIC_UNSIGNED = 0xC, 0xD, 0xF
_ASCII_POS, _ASCII_NEG = 0x3, 0x7  # GnuCOBOL / Micro Focus ASCII: '0'-'9' / 'p'-'y'
# Overpunch characters as they appear after an EBCDIC->ASCII text conversion.
_OVERPUNCH_POS = {"{": 0, "A": 1, "B": 2, "C": 3, "D": 4, "E": 5, "F": 6, "G": 7, "H": 8, "I": 9}
_OVERPUNCH_NEG = {"}": 0, "J": 1, "K": 2, "L": 3, "M": 4, "N": 5, "O": 6, "P": 7, "Q": 8, "R": 9}


def _to_decimal(value: Number) -> Decimal:
    if isinstance(value, Decimal):
        return value
    if isinstance(value, bool):
        raise CobolFieldError("bool is not a COBOL numeric value")
    if isinstance(value, int):
        return Decimal(value)
    if isinstance(value, str):
        return Decimal(value.strip())
    if isinstance(value, float):
        raise CobolFieldError(
            "refusing to encode a float: COBOL arithmetic is fixed-point; "
            "pass a Decimal (e.g. Decimal('123.45'))")
    raise CobolFieldError(f"unsupported numeric type {type(value).__name__}")


@dataclass(frozen=True)
class CobolField:
    """An elementary COBOL data item."""
    name: str
    pic: Pic
    usage: Usage = Usage.DISPLAY
    sign: SignMode = SignMode.TRAILING
    dialect: Dialect = field(default=IBM, compare=False)

    # -- construction -----------------------------------------------------
    @classmethod
    def from_clauses(cls, name: str, pic: str, usage: str = "DISPLAY",
                     sign: SignMode = SignMode.TRAILING,
                     dialect: Dialect = IBM) -> "CobolField":
        return cls(name, parse_pic(pic), Usage.parse(usage), sign, dialect)

    def __post_init__(self) -> None:
        if self.usage in (Usage.COMP, Usage.COMP_3, Usage.COMP_5) and self.pic.kind is not PicKind.NUMERIC:
            raise ValueError(f"{self.name}: {self.usage.value} requires a numeric PICTURE, got {self.pic.raw!r}")
        if self.usage is Usage.COMP_X and self.pic.kind is PicKind.EDITED:
            raise ValueError(f"{self.name}: COMP-X requires PIC 9(n) or PIC X(n)")
        if self.usage in (Usage.COMP_1, Usage.COMP_2):
            raise NotImplementedError(f"{self.name}: floating-point usages are out of scope for this suite")

    # -- derived properties ----------------------------------------------
    @property
    def is_numeric(self) -> bool:
        return self.pic.kind is PicKind.NUMERIC or self.usage is Usage.COMP_X

    @property
    def scale(self) -> int:
        return self.pic.scale

    @property
    def signed(self) -> bool:
        return self.pic.signed

    @property
    def length(self) -> int:
        """Storage length in bytes."""
        p, u = self.pic, self.usage
        if u is Usage.DISPLAY:
            if p.kind is PicKind.NUMERIC:
                return p.length + (1 if self.sign.separate and p.signed else 0)
            return p.length
        if u in (Usage.COMP, Usage.COMP_5):
            return self.dialect.comp_bytes(p.digits)
        if u is Usage.COMP_3:
            return (p.digits + 2) // 2  # digits + sign nibble, rounded up
        if u is Usage.COMP_X:
            if p.kind is PicKind.ALPHANUMERIC:
                return p.length
            n = 1
            while 256 ** n - 1 < p.max_value:
                n += 1
            return n
        raise AssertionError(u)

    @property
    def max_value(self) -> Decimal:
        """Largest value the field accepts in strict mode."""
        if self.usage is Usage.COMP_X:
            return Decimal(256 ** self.length - 1)
        if self.usage is Usage.COMP_5 or (self.usage is Usage.COMP and not self.dialect.binary_truncate):
            bits = self.length * 8
            return Decimal(2 ** (bits - 1) - 1 if self.signed else 2 ** bits - 1)
        return Decimal(self.pic.max_value).scaleb(-self.scale)

    @property
    def min_value(self) -> Decimal:
        if not self.signed or self.usage is Usage.COMP_X:
            return Decimal(0)
        if self.usage is Usage.COMP_5 or (self.usage is Usage.COMP and not self.dialect.binary_truncate):
            return Decimal(-(2 ** (self.length * 8 - 1)))
        return -self.max_value

    # -- value normalisation ---------------------------------------------
    def _unscaled(self, value: Number, truncate: bool) -> int:
        """Apply COBOL MOVE semantics and return the stored integer.

        * extra decimal places are dropped (COBOL truncates unless ROUNDED)
        * unsigned fields store the absolute value
        * high-order digits beyond the PIC are dropped when ``truncate`` is
          True (what COBOL silently does), otherwise ``CobolFieldError``.
        """
        d = _to_decimal(value)
        if not d.is_finite():
            raise CobolFieldError(f"{self.name}: non-finite value {d}")
        if self.pic.scale_shift:
            d = d.scaleb(-self.pic.scale_shift)
        q = d.quantize(Decimal(1).scaleb(-self.scale), rounding=ROUND_DOWN)
        if q != d and not truncate:
            raise CobolFieldError(f"{self.name}: {d} has more than {self.scale} decimal places")
        n = int(q.scaleb(self.scale))
        if n < 0 and not self.signed:
            if not truncate:
                raise CobolFieldError(f"{self.name}: negative value {d} into unsigned PIC {self.pic.raw}")
            n = -n
        if self.usage is Usage.COMP_X:
            limit = 256 ** self.length
            if n >= limit or n < 0:
                if not truncate:
                    raise CobolFieldError(f"{self.name}: {d} exceeds COMP-X range 0..{limit - 1}")
                n %= limit
            return n
        if self.usage is Usage.COMP_5 or (self.usage is Usage.COMP and not self.dialect.binary_truncate):
            bits = self.length * 8
            lo, hi = (-(2 ** (bits - 1)), 2 ** (bits - 1) - 1) if self.signed else (0, 2 ** bits - 1)
            if n < lo or n > hi:
                if not truncate:
                    raise CobolFieldError(f"{self.name}: {d} exceeds binary range {lo}..{hi}")
                n %= 2 ** bits
                if self.signed and n > hi:
                    n -= 2 ** bits
            return n
        limit = 10 ** self.pic.digits
        if abs(n) >= limit:
            if not truncate:
                raise CobolFieldError(
                    f"{self.name}: {d} does not fit PIC {self.pic.raw} (max {self.max_value})")
            sign = -1 if n < 0 else 1
            n = sign * (abs(n) % limit)
        return n

    def _scaled(self, n: int) -> Decimal:
        d = Decimal(n).scaleb(-self.scale)
        if self.pic.scale_shift:
            d = d.scaleb(self.pic.scale_shift)
        return d

    # -- encode -----------------------------------------------------------
    def encode(self, value, *, truncate: bool = False, encoding: str | None = None) -> bytes:
        """Return exactly ``self.length`` bytes for ``value``.

        ``truncate=True`` reproduces COBOL's silent MOVE truncation
        (high-order digits, extra decimals, dropped sign, text cut on the
        right). The default is strict and raises ``CobolFieldError`` instead,
        which is what you want while validating a migration.
        """
        enc = encoding or self.dialect.encoding
        u = self.usage
        if u is Usage.DISPLAY and self.pic.kind is not PicKind.NUMERIC:
            return self._encode_text(value, truncate, enc)
        if u is Usage.COMP_X and self.pic.kind is PicKind.ALPHANUMERIC:
            n = self._unscaled(value, truncate)
            return n.to_bytes(self.length, "big", signed=False)
        n = self._unscaled(value, truncate)
        if u is Usage.DISPLAY:
            return self._encode_zoned(n, enc)
        if u is Usage.COMP_3:
            return self._encode_packed(n)
        if u in (Usage.COMP, Usage.COMP_5):
            return n.to_bytes(self.length, self._byteorder, signed=self.signed)
        if u is Usage.COMP_X:
            return n.to_bytes(self.length, "big", signed=False)
        raise AssertionError(u)

    @property
    def _byteorder(self) -> str:
        return self.dialect.comp5_byteorder if self.usage is Usage.COMP_5 else "big"

    def _encode_text(self, value, truncate: bool, enc: str) -> bytes:
        if value is None:
            value = ""
        if isinstance(value, bytes):
            raw = value
        else:
            raw = str(value).encode(enc)
        if len(raw) > self.length:
            if not truncate:
                raise CobolFieldError(f"{self.name}: {value!r} is longer than PIC {self.pic.raw}")
            raw = raw[: self.length]
        return raw.ljust(self.length, " ".encode(enc))

    def _encode_zoned(self, n: int, enc: str) -> bytes:
        digits = f"{abs(n):0{self.pic.length}d}"
        ebcdic = _is_ebcdic(enc)
        if self.sign.separate and self.signed:
            s = "-" if n < 0 else "+"
            text = s + digits if self.sign is SignMode.LEADING_SEPARATE else digits + s
            return text.encode(enc)
        out = bytearray(digits.encode(enc))
        if self.signed:
            idx = 0 if self.sign is SignMode.LEADING else -1
            if ebcdic:
                zone = _EBCDIC_NEG if n < 0 else _EBCDIC_POS
            else:
                zone = _ASCII_NEG if n < 0 else _ASCII_POS
            out[idx] = (zone << 4) | (out[idx] & 0x0F)
        return bytes(out)

    def _encode_packed(self, n: int) -> bytes:
        digits = f"{abs(n):0{self.pic.digits}d}"
        if len(digits) % 2 == 0:  # digits + sign nibble must fill whole bytes
            digits = "0" + digits
        if self.signed:
            sign = 0xD if n < 0 else 0xC
        else:
            sign = 0xF
        nibbles = [int(c) for c in digits] + [sign]
        return bytes((nibbles[i] << 4) | nibbles[i + 1] for i in range(0, len(nibbles), 2))

    # -- decode -----------------------------------------------------------
    def decode(self, raw: bytes, *, encoding: str | None = None, strict: bool = True):
        """Turn ``raw`` (exactly ``self.length`` bytes) into a Python value.

        Text fields return ``str`` (trailing spaces kept - callers decide
        whether padding is significant). Numeric fields return ``Decimal``.
        With ``strict`` the decoder rejects invalid sign nibbles / zones and
        non-digit nibbles, which is how corrupted or mis-aligned data shows up.
        """
        if len(raw) != self.length:
            raise CobolFieldError(f"{self.name}: expected {self.length} bytes, got {len(raw)}")
        enc = encoding or self.dialect.encoding
        u = self.usage
        if u is Usage.DISPLAY and self.pic.kind is not PicKind.NUMERIC:
            return raw.decode(enc)
        if u is Usage.COMP_X:
            return self._scaled(int.from_bytes(raw, "big", signed=False))
        if u in (Usage.COMP, Usage.COMP_5):
            n = int.from_bytes(raw, self._byteorder, signed=self.signed)
            if strict and u is Usage.COMP and self.dialect.binary_truncate and abs(n) > self.pic.max_value:
                raise CobolFieldError(
                    f"{self.name}: binary value {n} exceeds PIC {self.pic.raw} (TRUNC(BIN)/COMP-5 data?)")
            return self._scaled(n)
        if u is Usage.COMP_3:
            return self._scaled(self._decode_packed(raw, strict))
        return self._scaled(self._decode_zoned(raw, enc, strict))

    def _decode_packed(self, raw: bytes, strict: bool) -> int:
        nibbles = []
        for b in raw:
            nibbles.append(b >> 4)
            nibbles.append(b & 0x0F)
        sign = nibbles.pop()
        if sign in (0xD, 0xB):
            neg = True
        elif sign in (0xC, 0xF, 0xA, 0xE):
            neg = False
        elif strict:
            raise CobolFieldError(f"{self.name}: invalid packed sign nibble 0x{sign:X} in {raw.hex()}")
        else:
            neg = False
        if len(nibbles) > self.pic.digits:          # even digit count: leading pad nibble
            pad, nibbles = nibbles[0], nibbles[1:]
            if strict and pad != 0:
                raise CobolFieldError(f"{self.name}: non-zero pad nibble 0x{pad:X} in {raw.hex()}")
        if strict and any(d > 9 for d in nibbles):
            raise CobolFieldError(f"{self.name}: invalid packed digit in {raw.hex()}")
        n = 0
        for d in nibbles:
            n = n * 10 + (d if d <= 9 else 0)
        return -n if neg else n

    def _decode_zoned(self, raw: bytes, enc: str, strict: bool) -> int:
        ebcdic = _is_ebcdic(enc)
        neg = False
        if self.sign.separate and self.signed:
            s_idx = 0 if self.sign is SignMode.LEADING_SEPARATE else -1
            s = raw[s_idx:s_idx + 1].decode(enc) if s_idx == 0 else raw[-1:].decode(enc)
            if s not in "+-":
                raise CobolFieldError(f"{self.name}: invalid separate sign {s!r}")
            neg = s == "-"
            body = raw[1:] if s_idx == 0 else raw[:-1]
            text = body.decode(enc)
        else:
            idx = 0 if self.sign is SignMode.LEADING else len(raw) - 1
            buf = bytearray(raw)
            b = buf[idx]
            zone, digit = b >> 4, b & 0x0F
            if ebcdic:
                if zone == _EBCDIC_NEG:
                    neg = True
                elif zone in (_EBCDIC_POS, _EBCDIC_UNSIGNED):
                    pass
                elif strict:
                    raise CobolFieldError(f"{self.name}: invalid EBCDIC sign zone 0x{zone:X}")
                buf[idx] = 0xF0 | digit
                text = bytes(buf).decode(enc)
            else:
                ch = chr(b)
                if zone == _ASCII_NEG:
                    neg = True
                    buf[idx] = 0x30 | digit
                elif ch in _OVERPUNCH_POS:
                    buf[idx] = 0x30 | _OVERPUNCH_POS[ch]
                elif ch in _OVERPUNCH_NEG:
                    neg = True
                    buf[idx] = 0x30 | _OVERPUNCH_NEG[ch]
                elif zone != _ASCII_POS and strict:
                    raise CobolFieldError(f"{self.name}: invalid ASCII sign byte 0x{b:02X}")
                text = bytes(buf).decode(enc)
        if strict and not text.isdigit():
            raise CobolFieldError(f"{self.name}: non-numeric DISPLAY data {text!r}")
        n = int(text) if text.strip() else 0
        return -n if neg else n

    # -- misc -------------------------------------------------------------
    def describe(self) -> str:
        bits = [f"PIC {self.pic.raw}"]
        if self.usage is not Usage.DISPLAY:
            bits.append(self.usage.value)
        return " ".join(bits)


def _is_ebcdic(encoding: str) -> bool:
    """True when the codec is an EBCDIC code page (digit zero is 0xF0)."""
    return "0".encode(encoding) == b"\xf0"
