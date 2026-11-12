# `cobol_fields` — COBOL data definitions for the Python migration

A field-definition layer, copybook parser and field-by-field diff harness that
let a Python port of this suite read and write the **same bytes** the COBOL
programs do. Everything is driven by the suite's own copybooks in
`src/copybook/` and verified against GnuCOBOL.

## First principles (what we tell the customer)

1. **A PIC clause is a storage contract, not a type hint.** `PIC S9(13)V99 COMP-3`
   says: 15 digits, 2 of them after an implied point, signed, packed two digits
   per byte with a sign nibble — 8 bytes exactly. Python has nothing like it, so
   the migration starts by modelling the contract (`CobolField`), not by guessing
   `int` vs `float`.
2. **COMP / BINARY / COMP-5 / COMP-X are all "binary" and all different.**
   Width comes from the digit count *and the compiler* (IBM: 2/4/8 bytes;
   GnuCOBOL default: 1/2/4/8). `BINARY`/`COMP` are truncated to the PIC digits
   on store (TRUNC(STD)); `COMP-5` keeps the full two's-complement range and is
   native-endian (big on z/OS, little on x86); `COMP-X` is unsigned and sized
   from the PIC. Getting any one of these wrong moves every later field in the record.
3. **COMP-3 is money. Money is `Decimal`, never `float`.** Packed decimal is
   exact fixed-point; `0.1 + 0.1 + ...` ten times is `1.00` in COBOL and
   `0.9999999999999999` in float, which COBOL's MOVE truncates to `0.99`.
   `CobolField.encode()` refuses a `float` outright.
4. **DISPLAY fields carry the code page; binary fields do not.** Zoned decimal
   overpunches the sign into the last digit (`-12345.67` is `F1F2F3F4F5F6D7` in
   EBCDIC, `123456w` in GnuCOBOL ASCII). COMP-3/COMP bytes are the same on both.
5. **COBOL truncates silently; Python does not.** High-order digits beyond the PIC
   and low-order digits beyond the scale are dropped by COBOL MOVE/COMPUTE.
   The codec makes this an explicit `truncate=True` choice and raises otherwise.
6. **Prove it, don't assert it.** Run the same data through COBOL and Python and
   diff the records field by field. `tests/test_cobol_parity.py` does exactly
   that with `cobc`: COBOL writes → Python decodes; Python writes → COBOL reads
   and DISPLAYs; and every `01` record in `src/copybook/` has its Python length
   checked against `LENGTH OF` from the compiler.

## Layout

```
python/
  cobol_fields/
    pic.py        PIC clause parser (9/S/V/P, X/A, edited)
    fields.py     CobolField + Dialect: encode/decode for DISPLAY, COMP/BINARY, COMP-3, COMP-5, COMP-X
    copybook.py   copybook -> RecordLayout (OCCURS, REDEFINES, SIGN, 88s, multi-01)
    diff.py       field-by-field record diff with mismatch classification
    cli.py        python -m cobol_fields layout | dump | diff
  tests/
    test_fields.py        known IBM byte patterns, truncation, sign, EBCDIC, float-vs-Decimal
    test_copybook.py      offsets for POSREC/TRNREC/PORTFLIO/RTNCODE, OCCURS/REDEFINES, diff classes
    test_cobol_parity.py  GnuCOBOL round trips (skipped if cobc is absent)
    cobol/                CODECREC.cpy (one field per USAGE), CODECWR/CODECRD/POSWRT fixtures
```

## Usage

```python
from decimal import Decimal
from cobol_fields import CobolField, IBM, GNUCOBOL, load_layout

qty = CobolField.from_clauses("POS-QUANTITY", "S9(11)V9(4)", "COMP-3")
qty.length                                   # 8
qty.encode(Decimal("1234567.8912")).hex()    # '000012345678912c'
qty.decode(bytes.fromhex("000012345678912c"))  # Decimal('1234567.8912')

pos = load_layout("src/copybook/common/POSREC.cpy", dialect=IBM)   # 138 bytes, EBCDIC
rec = pos.decode(raw_138_bytes)              # {'POS-PORTFOLIO-ID': 'PORT0001', 'POS-QUANTITY': Decimal(...), ...}
pos.encode(rec) == raw_138_bytes             # True
```

```
python -m cobol_fields layout src/copybook/common/POSREC.cpy
python -m cobol_fields dump   src/copybook/common/POSREC.cpy positions.dat --dialect gnucobol-ibm
python -m cobol_fields diff   src/copybook/common/POSREC.cpy cobol_out.dat python_out.dat   # exit 1 on mismatch
```

Dialects: `ibm` (z/OS: 2/4/8-byte binaries, cp037), `gnucobol` (`cobc` default:
1/2/4/8, ASCII, native-endian COMP-5), `gnucobol-ibm` (`cobc -std=ibm`: IBM widths, ASCII).

## Running the tests

```
cd python/tests
python3 -m pytest -q
```

GnuCOBOL (`apt install gnucobol`) enables the parity tests; without it they are skipped.
