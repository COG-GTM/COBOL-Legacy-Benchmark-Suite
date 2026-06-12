#!/usr/bin/env python3
"""Parse fixed-width golden .dat fixtures into CSV and JSON.

Decodes records produced per the copybook layouts:
  - PORTFLIO.cpy : portfolio master record, 148 bytes
  - TRNREC.cpy   : transaction record, 152 bytes
COMP-3 (packed decimal) fields are decoded to scaled decimal strings so Java
tests can load them as java.math.BigDecimal.

Usage:
  python3 parse_fixtures.py portfolio  portfolio.dat  portfolio.csv  portfolio.json
  python3 parse_fixtures.py transaction transactions.dat transactions.csv transactions.json
"""

import csv
import json
import sys
from decimal import Decimal
from pathlib import Path


def unpack_comp3(data: bytes, scale: int) -> str:
    nibbles = []
    for b in data:
        nibbles.append(b >> 4)
        nibbles.append(b & 0x0F)
    sign_nibble = nibbles.pop()
    digits = "".join(str(n) for n in nibbles)
    value = Decimal(digits).scaleb(-scale)
    if sign_nibble in (0xD, 0xB):
        value = -value
    return f"{value:.{scale}f}" if scale else str(value)


# (field name, length in bytes, kind, scale) — kind: x=text, 9=zoned numeric,
# p=COMP-3 packed decimal
PORTFOLIO_LAYOUT = [
    ("PORT-ID", 8, "x", 0),
    ("PORT-ACCOUNT-NO", 10, "x", 0),
    ("PORT-CLIENT-NAME", 30, "x", 0),
    ("PORT-CLIENT-TYPE", 1, "x", 0),
    ("PORT-CREATE-DATE", 8, "9", 0),
    ("PORT-LAST-MAINT", 8, "9", 0),
    ("PORT-STATUS", 1, "x", 0),
    ("PORT-TOTAL-VALUE", 8, "p", 2),    # S9(13)V99 COMP-3
    ("PORT-CASH-BALANCE", 8, "p", 2),   # S9(13)V99 COMP-3
    ("PORT-LAST-USER", 8, "x", 0),
    ("PORT-LAST-TRANS", 8, "9", 0),
    ("PORT-FILLER", 50, "x", 0),
]

TRANSACTION_LAYOUT = [
    ("TRN-DATE", 8, "x", 0),
    ("TRN-TIME", 6, "x", 0),
    ("TRN-PORTFOLIO-ID", 8, "x", 0),
    ("TRN-SEQUENCE-NO", 6, "x", 0),
    ("TRN-INVESTMENT-ID", 10, "x", 0),
    ("TRN-TYPE", 2, "x", 0),
    ("TRN-QUANTITY", 8, "p", 4),        # S9(11)V9(4) COMP-3
    ("TRN-PRICE", 8, "p", 4),           # S9(11)V9(4) COMP-3
    ("TRN-AMOUNT", 8, "p", 2),          # S9(13)V9(2) COMP-3
    ("TRN-CURRENCY", 3, "x", 0),
    ("TRN-STATUS", 1, "x", 0),
    ("TRN-PROCESS-DATE", 26, "x", 0),
    ("TRN-PROCESS-USER", 8, "x", 0),
    ("TRN-FILLER", 50, "x", 0),
]

LAYOUTS = {
    "portfolio": (PORTFOLIO_LAYOUT, 148),
    "transaction": (TRANSACTION_LAYOUT, 152),
}


def parse_record(rec: bytes, layout) -> dict:
    out = {}
    pos = 0
    for name, length, kind, scale in layout:
        raw = rec[pos:pos + length]
        pos += length
        if name.endswith("FILLER"):
            continue
        if kind == "p":
            out[name] = unpack_comp3(raw, scale)
        else:
            out[name] = raw.decode("ascii").rstrip()
    return out


def main() -> None:
    if len(sys.argv) != 5 or sys.argv[1] not in LAYOUTS:
        raise SystemExit(__doc__)
    layout, lrecl = LAYOUTS[sys.argv[1]]
    data = Path(sys.argv[2]).read_bytes()
    if len(data) % lrecl:
        raise SystemExit(f"file size {len(data)} not a multiple of LRECL {lrecl}")

    records = [parse_record(data[i:i + lrecl], layout)
               for i in range(0, len(data), lrecl)]

    fields = [f[0] for f in layout if not f[0].endswith("FILLER")]
    with open(sys.argv[3], "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(records)
    Path(sys.argv[4]).write_text(json.dumps(records, indent=2) + "\n")
    print(f"parsed {len(records)} records -> {sys.argv[3]}, {sys.argv[4]}")


if __name__ == "__main__":
    main()
