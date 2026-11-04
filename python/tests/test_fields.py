"""Byte-level codec tests against known IBM Enterprise COBOL storage patterns."""
from decimal import Decimal

import pytest

from cobol_fields import GNUCOBOL, IBM, CobolField, CobolFieldError, SignMode, Usage, parse_pic
from cobol_fields.pic import PicKind

F = CobolField.from_clauses


# --------------------------------------------------------------------- PIC
@pytest.mark.parametrize("pic,signed,ints,scale,length,kind", [
    ("9(4)", False, 4, 0, 4, PicKind.NUMERIC),
    ("S9(4)", True, 4, 0, 4, PicKind.NUMERIC),
    ("S9(11)V9(4)", True, 11, 4, 15, PicKind.NUMERIC),
    ("S9(13)V99", True, 13, 2, 15, PicKind.NUMERIC),
    ("V999", False, 0, 3, 3, PicKind.NUMERIC),
    ("X(08)", False, 0, 0, 8, PicKind.ALPHANUMERIC),
    ("XX", False, 0, 0, 2, PicKind.ALPHANUMERIC),
    ("A(3)X9", False, 0, 0, 5, PicKind.ALPHANUMERIC),
    ("-9(7).99", True, 7, 2, 11, PicKind.EDITED),
    ("ZZ,ZZ9.99", False, 5, 2, 9, PicKind.EDITED),
])
def test_parse_pic(pic, signed, ints, scale, length, kind):
    p = parse_pic(pic)
    assert (p.signed, p.int_digits, p.scale, p.length, p.kind) == (signed, ints, scale, length, kind)


def test_pic_rejects_garbage():
    with pytest.raises(ValueError):
        parse_pic("9(4)Q")


# ------------------------------------------------------------------ COMP-3
@pytest.mark.parametrize("pic,value,hexed", [
    ("S9(7)V99", "1234567.89", "123456789c"),
    ("S9(7)V99", "-1234567.89", "123456789d"),
    ("S9(7)V99", "0", "000000000c"),
    ("9(5)", "12345", "12345f"),
    ("S9(11)V9(4)", "1234567.8912", "000012345678912c"),  # POS-QUANTITY, 8 bytes
    ("S9(13)V99", "-98765432109.87", "009876543210987d"), # POS-COST-BASIS, 8 bytes
    ("S9(13)V99", "0.01", "000000000000001c"),
])
def test_comp3_known_bytes(pic, value, hexed):
    f = F("X", pic, "COMP-3")
    assert f.encode(Decimal(value)).hex() == hexed
    assert f.decode(bytes.fromhex(hexed)) == Decimal(value)


def test_comp3_length_is_digits_plus_sign_over_two():
    assert F("Q", "S9(11)V9(4)", "COMP-3").length == 8
    assert F("Q", "S9(13)V9(2)", "COMP-3").length == 8
    assert F("Q", "9(5)", "COMP-3").length == 3
    assert F("Q", "9(4)", "COMP-3").length == 3   # even digit count -> leading pad nibble


def test_comp3_accepts_unsigned_f_and_alt_positive_nibbles_when_decoding():
    f = F("X", "S9(3)", "COMP-3")
    assert f.decode(bytes.fromhex("123f")) == 123
    assert f.decode(bytes.fromhex("123a")) == 123
    assert f.decode(bytes.fromhex("123b")) == -123


def test_comp3_strict_rejects_bad_nibbles():
    f = F("X", "S9(3)", "COMP-3")
    with pytest.raises(CobolFieldError):
        f.decode(bytes.fromhex("12af"))
    with pytest.raises(CobolFieldError):
        f.decode(bytes.fromhex("1230"))
    assert f.decode(bytes.fromhex("1230"), strict=False) == 123


# ------------------------------------------------------------- COMP/BINARY
@pytest.mark.parametrize("pic,ibm_len,gnu_len", [
    ("9(2)", 2, 1), ("S9(4)", 2, 2), ("S9(5)", 4, 4), ("S9(9)", 4, 4), ("S9(10)", 8, 8), ("S9(18)", 8, 8),
])
def test_comp_width_depends_on_dialect(pic, ibm_len, gnu_len):
    assert F("B", pic, "COMP").length == ibm_len
    assert F("B", pic, "BINARY").length == ibm_len
    assert F("B", pic, "COMP", dialect=GNUCOBOL).length == gnu_len


def test_comp_known_bytes_big_endian_twos_complement():
    f = F("B", "S9(4)", "COMP")
    assert f.encode(1234).hex() == "04d2"
    assert f.encode(-1234).hex() == "fb2e"
    assert f.decode(bytes.fromhex("fb2e")) == -1234
    assert F("B", "S9(4)V99", "COMP").encode(Decimal("1234.56")).hex() == "0001e240"


def test_binary_is_truncated_to_pic_not_to_byte_width():
    f = F("B", "S9(4)", "BINARY")                  # 2 bytes can hold 32767, PIC allows 9999
    with pytest.raises(CobolFieldError):
        f.encode(12345)
    assert f.encode(12345, truncate=True) == f.encode(2345)   # TRUNC(STD): keep low-order 4 digits
    with pytest.raises(CobolFieldError):
        f.decode(bytes.fromhex("7fff"))            # 32767 > 9999: not valid under TRUNC(STD)
    assert f.decode(bytes.fromhex("7fff"), strict=False) == 32767


def test_comp5_uses_full_binary_range():
    f = F("B", "S9(4)", "COMP-5")
    assert f.encode(32767).hex() == "7fff"
    assert f.decode(bytes.fromhex("8000")) == -32768
    with pytest.raises(CobolFieldError):
        f.encode(32768)


def test_comp5_byteorder_is_native_on_gnucobol_big_on_ibm():
    assert F("B", "S9(4)", "COMP-5", dialect=IBM).encode(1234).hex() == "04d2"
    gnu = F("B", "S9(4)", "COMP-5", dialect=GNUCOBOL).encode(1234).hex()
    assert gnu in ("04d2", "d204")


def test_compx_is_unsigned_and_sized_from_pic():
    assert F("X", "9(4)", "COMP-X").length == 2
    assert F("X", "9(4)", "COMP-X").encode(65535).hex() == "ffff"   # full byte range, no PIC truncation
    assert F("X", "X(2)", "COMP-X").length == 2
    assert F("X", "X(2)", "COMP-X").decode(b"\x01\x00") == 256
    assert F("X", "9(9)", "COMP-X").length == 4
    with pytest.raises(CobolFieldError):
        F("X", "9(4)", "COMP-X").encode(-1)


# ----------------------------------------------------------------- DISPLAY
def test_zoned_decimal_ebcdic_overpunch():
    f = F("D", "S9(5)V99", "DISPLAY")               # IBM default: cp037
    assert f.encode(Decimal("12345.67")).hex() == "f1f2f3f4f5f6c7"
    assert f.encode(Decimal("-12345.67")).hex() == "f1f2f3f4f5f6d7"
    assert f.decode(bytes.fromhex("f1f2f3f4f5f6d7")) == Decimal("-12345.67")
    assert f.decode(bytes.fromhex("f1f2f3f4f5f6f7")) == Decimal("12345.67")   # unsigned zone accepted


def test_zoned_decimal_ascii_overpunch_matches_gnucobol():
    f = F("D", "S9(5)V99", "DISPLAY", dialect=GNUCOBOL)
    assert f.encode(Decimal("-12345.67")) == b"123456w"       # GnuCOBOL: 0x30|digit + 0x40
    assert f.encode(Decimal("-0.01")) == b"000000q"
    assert f.decode(b"123456w") == Decimal("-12345.67")


def test_sign_separate_leading():
    f = F("D", "S9(5)", "DISPLAY", sign=SignMode.LEADING_SEPARATE, dialect=GNUCOBOL)
    assert f.length == 6
    assert f.encode(-12345) == b"-12345"
    assert f.decode(b"+12345") == 12345


def test_text_ebcdic_padding_and_truncation():
    f = F("T", "X(5)")
    assert f.encode("AB") == "AB   ".encode("cp037")
    assert f.decode("AB   ".encode("cp037")) == "AB   "
    with pytest.raises(CobolFieldError):
        f.encode("ABCDEFG")
    assert f.encode("ABCDEFG", truncate=True) == "ABCDE".encode("cp037")
    assert f.encode("AB", encoding="ascii") == b"AB   "


# -------------------------------------------------------------- semantics
def test_float_is_refused():
    with pytest.raises(CobolFieldError, match="float"):
        F("M", "S9(13)V99", "COMP-3").encode(0.1 + 0.2)


def test_decimal_vs_float_reconciliation():
    """The #1 COBOL-migration bug: float arithmetic drifts, fixed-point does not."""
    f = F("M", "S9(13)V99", "COMP-3")
    cobol_total = f.encode(sum(Decimal("0.10") for _ in range(10)))   # ten ADD 0.10 in COBOL -> 1.00
    assert f.decode(cobol_total) == Decimal("1.00")
    float_total = 0.0
    for _ in range(10):                                                 # ten ADD 0.10 in float
        float_total += 0.10
    assert float_total == 0.9999999999999999                           # float drift
    drifted = f.encode(Decimal(str(float_total)), truncate=True)        # COBOL MOVE truncates -> 0.99
    assert f.decode(drifted) == Decimal("0.99")
    assert drifted != cobol_total                                       # a one-cent break the diff catches


def test_scale_truncates_not_rounds():
    f = F("M", "S9(5)V99", "COMP-3")
    with pytest.raises(CobolFieldError):
        f.encode(Decimal("1.999"))
    assert f.decode(f.encode(Decimal("1.999"), truncate=True)) == Decimal("1.99")   # COBOL MOVE truncates


def test_high_order_truncation_on_comp3():
    f = F("M", "9(3)", "COMP-3")
    assert f.decode(f.encode(12345, truncate=True)) == 345


def test_usage_aliases():
    assert Usage.parse("COMPUTATIONAL-3") is Usage.COMP_3
    assert Usage.parse("PACKED-DECIMAL") is Usage.COMP_3
    assert Usage.parse("BINARY") is Usage.COMP
    assert Usage.parse("COMP-4") is Usage.COMP
