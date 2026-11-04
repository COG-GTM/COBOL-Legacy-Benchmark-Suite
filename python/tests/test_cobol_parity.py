"""Parity with a real COBOL compiler (GnuCOBOL).

COBOL writes -> Python decodes; Python writes -> COBOL reads and DISPLAYs.
Two compiler configs: -std=default (binary-size 1-2-4-8) and -std=ibm (2-4-8).
"""
import re
from decimal import Decimal

import pytest

from cobol_fields import GNUCOBOL, GNUCOBOL_IBM, diff_bytes, load_layout, parse_copybook
from cobol_fields.diff import iter_records
from conftest import COBOL_FIXTURES, COPYBOOKS, compile_cobol, requires_cobc, run_cobol

pytestmark = requires_cobc

STD = {"default": GNUCOBOL, "ibm": GNUCOBOL_IBM}

EXPECTED = [
    {"C-S4-COMP": 1234, "C-U2-COMP": 42, "C-S8-COMP": 12345678, "C-S18-COMP": 123456789012345678,
     "C-S4V2-COMP": Decimal("1234.56"), "C-S9-BIN": 123456789, "C-S4-COMP5": 1234, "C-U4-COMPX": 9999,
     "C-X2-COMPX": 65535, "C-S7V2-COMP3": Decimal("1234567.89"), "C-U5-COMP3": 12345,
     "C-S5V2-DISP": Decimal("12345.67"), "C-U6-DISP": 123456, "C-SEP-DISP": 12345, "C-TEXT": "POSITIVE"},
    {"C-S4-COMP": -1234, "C-U2-COMP": 7, "C-S8-COMP": -12345678, "C-S18-COMP": -123456789012345678,
     "C-S4V2-COMP": Decimal("-0.01"), "C-S9-BIN": -123456789, "C-S4-COMP5": -32768, "C-U4-COMPX": 0,
     "C-X2-COMPX": 256, "C-S7V2-COMP3": Decimal("-1234567.89"), "C-U5-COMP3": 0,
     "C-S5V2-DISP": Decimal("-12345.67"), "C-U6-DISP": 0, "C-SEP-DISP": -12345, "C-TEXT": "NEGATIVE"},
    {"C-S4-COMP": 9999, "C-U2-COMP": 99, "C-S8-COMP": 99999999, "C-S18-COMP": 999999999999999999,
     "C-S4V2-COMP": Decimal("-9999.99"), "C-S9-BIN": 999999999, "C-S4-COMP5": 32767, "C-U4-COMPX": 65535,
     "C-X2-COMPX": 1, "C-S7V2-COMP3": Decimal("9999999.99"), "C-U5-COMP3": 99999,
     "C-S5V2-DISP": Decimal("-0.01"), "C-U6-DISP": 999999, "C-SEP-DISP": 0, "C-TEXT": "EXTREMES"},
]


def _norm(v):
    return v.rstrip() if isinstance(v, str) else v


@pytest.mark.parametrize("std", ["default", "ibm"])
def test_cobol_writes_python_decodes_every_usage(tmp_path, std):
    exe = compile_cobol(COBOL_FIXTURES / "CODECWR.cbl", tmp_path, std)
    run_cobol(exe, tmp_path)
    raw = (tmp_path / "codec.dat").read_bytes()
    lay = load_layout(COBOL_FIXTURES / "CODECREC.cpy", dialect=STD[std])
    assert lay.length == (66 if std == "default" else 67)   # 9(2) COMP: 1 byte vs 2 bytes
    recs = list(iter_records(raw, lay.length))
    assert len(recs) == 3
    for rec, want in zip(recs, EXPECTED):
        got = {k.split(".")[-1]: _norm(v) for k, v in lay.decode(rec).items()}
        assert got == want
    # Python re-encodes the decoded values to the identical bytes COBOL wrote.
    for rec in recs:
        assert lay.encode(lay.decode(rec)) == rec


def _parse_display(out: str) -> list[dict]:
    recs, cur = [], None
    for line in out.splitlines():
        if line.strip() == "RECORD":
            cur = {}
            recs.append(cur)
        elif "=" in line and cur is not None:
            k, v = line.split("=", 1)
            v = v.strip()
            cur[k] = v if k == "C-TEXT" else Decimal(v.replace(" ", ""))
    return recs


@pytest.mark.parametrize("std", ["default", "ibm"])
def test_python_writes_cobol_reads(tmp_path, std):
    lay = load_layout(COBOL_FIXTURES / "CODECREC.cpy", dialect=STD[std])
    (tmp_path / "codec.dat").write_bytes(b"".join(lay.encode(r) for r in EXPECTED))
    exe = compile_cobol(COBOL_FIXTURES / "CODECRD.cbl", tmp_path, std)
    seen = _parse_display(run_cobol(exe, tmp_path))
    assert len(seen) == 3
    for got, want in zip(seen, EXPECTED):
        assert {k: _norm(v) for k, v in got.items()} == {k: Decimal(v) if not isinstance(v, str) else v
                                                          for k, v in want.items()}


def test_suite_copybooks_poswrt_parity(tmp_path):
    """The repo's own POSREC/TRNREC (COMP-3 money fields) written by COBOL, read by Python."""
    exe = compile_cobol(COBOL_FIXTURES / "POSWRT.cbl", tmp_path, "ibm")
    run_cobol(exe, tmp_path)
    pos = load_layout(COPYBOOKS / "common" / "POSREC.cpy", dialect=GNUCOBOL_IBM)
    trn = load_layout(COPYBOOKS / "common" / "TRNREC.cpy", dialect=GNUCOBOL_IBM)
    p = [pos.decode(r) for r in iter_records((tmp_path / "positions.dat").read_bytes(), pos.length)]
    t = [trn.decode(r) for r in iter_records((tmp_path / "transactions.dat").read_bytes(), trn.length)]
    assert [r["POS-QUANTITY"] for r in p] == [Decimal("1234567.8912"), Decimal("99999999999.9999")]
    assert p[0]["POS-COST-BASIS"] == Decimal("-98765432109.87")
    assert p[0]["POS-MARKET-VALUE"] == Decimal("0.01")
    assert p[1]["POS-MARKET-VALUE"] == Decimal("-9999999999999.99")
    assert (p[0]["POS-STATUS"], p[1]["POS-STATUS"]) == ("A", "C")
    # COBOL COMPUTE without ROUNDED truncates 100.5 * 19.99 = 2008.995 to the V99 scale.
    assert Decimal("100.5") * Decimal("19.99") == Decimal("2008.995")
    assert t[0]["TRN-AMOUNT"] == Decimal("2008.99")
    assert t[1]["TRN-AMOUNT"] == Decimal("-12.50")
    assert t[1]["TRN-TYPE"] == "FE" and t[1]["TRN-STATUS"] == "R"
    # Python port writes the same records -> byte-identical -> diff harness reports MATCH.
    again = b"".join(pos.encode(r) for r in p)
    assert again == (tmp_path / "positions.dat").read_bytes()
    assert diff_bytes(pos, (tmp_path / "positions.dat").read_bytes(), again).ok


SKIP = {"CKPRST", "RETHND", "DBPROC", "SQLCA"}  # not standalone-compilable (program fragments / COPY SQLCA)


def test_every_suite_copybook_length_matches_cobc(tmp_path):
    """LENGTH OF every 01 record in src/copybook == Python layout length (IBM binary sizes)."""
    checked = 0
    for cpy in sorted(COPYBOOKS.rglob("*.cpy")):
        if cpy.stem in SKIP:
            continue
        layouts = parse_copybook(cpy.read_text(), dialect=GNUCOBOL_IBM)
        shows = "\n".join(f'           DISPLAY "{lay.name}="\n               LENGTH OF {lay.name}.' for lay in layouts)
        src = tmp_path / f"L{cpy.stem}.cbl"
        src.write_text("       IDENTIFICATION DIVISION.\n       PROGRAM-ID. LENCHK.\n       DATA DIVISION.\n"
                       f"       WORKING-STORAGE SECTION.\n       COPY {cpy.stem}.\n       PROCEDURE DIVISION.\n"
                       f"{shows}\n           STOP RUN.\n")
        out = run_cobol(compile_cobol(src, tmp_path, "ibm"), tmp_path)
        got = {k: int(v) for k, v in re.findall(r"^(\S+)=(\d+)$", out, re.M)}
        assert got == {lay.name: lay.length for lay in layouts}, cpy.name
        checked += len(got)
    assert checked >= 25
