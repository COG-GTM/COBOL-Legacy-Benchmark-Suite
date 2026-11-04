from decimal import Decimal

import pytest

from cobol_fields import GNUCOBOL, IBM, diff_bytes, load_layout, parse_copybook
from conftest import COPYBOOKS

COMMON = COPYBOOKS / "common"


def test_posrec_layout_offsets():
    lay = load_layout(COMMON / "POSREC.cpy")
    assert lay.name == "POSITION-RECORD" and lay.length == 138
    by = {f.short_name: f for f in lay.data_fields}
    assert (by["POS-QUANTITY"].offset, by["POS-QUANTITY"].length) == (26, 8)
    assert (by["POS-COST-BASIS"].offset, by["POS-MARKET-VALUE"].offset) == (34, 42)
    assert by["POS-STATUS"].offset == 53
    assert by["POS-FILLER"].length == 50


def test_trnrec_and_portflio_lengths():
    assert load_layout(COMMON / "TRNREC.cpy").length == 152
    assert load_layout(COMMON / "PORTFLIO.cpy").length == 148


def test_multi_record_copybook_requires_record_name():
    with pytest.raises(ValueError, match="RETURN-HANDLING"):
        load_layout(COMMON / "RETHND.cpy")
    assert load_layout(COMMON / "RETHND.cpy", record="STD-ERROR-CODES").name == "STD-ERROR-CODES"


def test_occurs_and_comp_in_rtncode_copybook():
    lay = load_layout(COMMON / "RTNCODE.cpy")
    assert lay.length == 165
    assert load_layout(COMMON / "RTNCODE.cpy", dialect=GNUCOBOL).length == 165  # S9(4) COMP is 2 bytes in both


def test_qualified_names_for_duplicate_fillers_and_groups():
    lay = load_layout(COMMON / "POSREC.cpy")
    assert "POS-KEY.POS-PORTFOLIO-ID" in {f.name for f in lay.data_fields}


def test_posrec_round_trip_ebcdic():
    lay = load_layout(COMMON / "POSREC.cpy", dialect=IBM)
    values = {
        "POS-PORTFOLIO-ID": "PORT0001", "POS-DATE": "20240315", "POS-INVESTMENT-ID": "INV0000001",
        "POS-QUANTITY": Decimal("1234567.8912"), "POS-COST-BASIS": Decimal("-98765432109.87"),
        "POS-MARKET-VALUE": Decimal("0.01"), "POS-CURRENCY": "USD", "POS-STATUS": "A",
    }
    raw = lay.encode(values)
    assert len(raw) == 138
    assert raw[:8] == "PORT0001".encode("cp037")
    assert raw[26:34].hex() == "000012345678912c"
    back = lay.decode(raw)
    for k, v in values.items():
        assert back[k] == v if not isinstance(v, str) else back[k].rstrip() == v


def test_redefines_and_occurs_offsets():
    text = """
       01  REC.
           05  A            PIC X(4).
           05  B REDEFINES A PIC 9(4).
           05  C OCCURS 3 TIMES.
               10  C-N      PIC S9(3) COMP-3.
               10  C-T      PIC X.
           05  D            PIC S9(4) COMP.
    """
    lay = parse_copybook(text)[0]
    by = {f.name: f for f in lay.fields}
    assert lay.length == 4 + 3 * 3 + 2
    assert by["B"].redefines and by["B"].offset == 0
    assert by["C(1).C-N"].offset == 4 and by["C(3).C-T"].offset == 12
    assert by["D"].offset == 13


def test_diff_classifies_mismatches():
    lay = load_layout(COMMON / "POSREC.cpy", dialect=GNUCOBOL)
    base = {"POS-PORTFOLIO-ID": "PORT0001", "POS-QUANTITY": Decimal("100.5"), "POS-COST-BASIS": Decimal("19.99"),
            "POS-MARKET-VALUE": Decimal("2009.99"), "POS-CURRENCY": "USD", "POS-STATUS": "A"}
    good = lay.encode(base)
    bad = lay.encode({**base, "POS-MARKET-VALUE": Decimal("2010.00"), "POS-COST-BASIS": Decimal("-19.99"),
                      "POS-CURRENCY": "usd"})
    rep = diff_bytes(lay, good + good, good + bad)
    assert not rep.ok
    assert rep.records_expected == rep.records_actual == 2
    reasons = {d.field.split(".")[-1]: d.reason for d in rep.diffs}
    assert reasons["POS-COST-BASIS"] == "sign flipped"
    assert "POS-MARKET-VALUE" in reasons and "POS-CURRENCY" in reasons
    assert all(d.record_no == 2 for d in rep.diffs)
    assert "MISMATCH" in rep.summary()
    assert diff_bytes(lay, good, good).ok


def test_oversized_redefines_reserves_longest_view():
    lay = parse_copybook("""
       01  REC.
           05  A            PIC X.
           05  B REDEFINES A PIC X(4).
           05  C            PIC X.
    """)[0]
    by = {f.name: f for f in lay.fields}
    assert lay.length == 5
    assert (by["A"].offset, by["B"].offset, by["C"].offset) == (0, 0, 4)


def test_encode_honours_redefines_view_and_rejects_conflicts():
    lay = parse_copybook("""
       01  REC.
           05  A            PIC X(4).
           05  B REDEFINES A PIC 9(4).
           05  C            PIC X.
    """, dialect=GNUCOBOL)[0]
    assert lay.encode({"B": 1234, "C": "Z"}) == b"1234Z"
    assert lay.encode({"A": "TEST"}) == b"TEST "
    with pytest.raises(ValueError, match="overlap"):
        lay.encode({"A": "TEST", "B": 1})


def test_group_sign_clause_inherited_by_children():
    lay = parse_copybook("""
       01  REC.
           05  G SIGN LEADING SEPARATE.
               10  N        PIC S9(2).
               10  M        PIC S9(2) SIGN TRAILING.
           05  T            PIC X.
    """, dialect=GNUCOBOL)[0]
    by = {f.name: f for f in lay.fields}
    assert (by["G.N"].length, by["G.M"].length, by["T"].offset) == (3, 2, 5)
    assert lay.encode({"N": -5, "M": -5}) == b"-05" + b"0u" + b" "


def test_repeated_filler_siblings_keep_their_own_offsets():
    lay = parse_copybook("""
       01  REC.
           05  FILLER       PIC X(2).
           05  N            PIC X.
           05  FILLER       PIC X(3).
           05  M            PIC X.
    """, dialect=GNUCOBOL)[0]
    assert lay.length == 7
    assert [(f.name, f.offset) for f in lay.fields] == [("FILLER@0", 0), ("N", 2), ("FILLER@3", 3), ("M", 6)]
    assert lay.encode({"N": "n", "M": "m"}) == b"  n   m"
    # the suite's DB2-ERROR-HANDLING.DB2-ERROR-MESSAGE has three FILLER siblings
    msg = load_layout(COPYBOOKS / "db2" / "DBPROC.cpy", record="DB2-ERROR-HANDLING")
    offs = [f.offset for f in msg.fields if f.name.startswith("DB2-ERROR-MESSAGE.")]
    assert len(offs) >= 6 and offs == sorted(offs) and len(set(offs)) == len(offs)
