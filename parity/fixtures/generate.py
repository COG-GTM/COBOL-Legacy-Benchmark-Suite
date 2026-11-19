#!/usr/bin/env python3
"""Generates the fixed-width COBOL input files used by the parity harness.

Layouts follow src/copybook/common/PORTFLIO.cpy (148 bytes, COMP-3 money),
and the UPDATE-RECORD / DELETE-RECORD FDs in PORTUPDT.cbl / PORTDEL.cbl.
"""
from decimal import Decimal
from pathlib import Path

HERE = Path(__file__).parent


def comp3(value: str, digits: int = 15, scale: int = 2) -> bytes:
    d = Decimal(value)
    sign = 0xD if d < 0 else 0xC
    n = int(abs(d).scaleb(scale))
    s = str(n).rjust(digits, "0")
    nibbles = [int(c) for c in s] + [sign]
    if len(nibbles) % 2:
        nibbles = [0] + nibbles
    return bytes((nibbles[i] << 4) | nibbles[i + 1] for i in range(0, len(nibbles), 2))


def x(value: str, n: int) -> bytes:
    return value.ljust(n)[:n].encode("ascii")


def port(pid, acct, name, ctype, status, total, cash, user="LOADER", last_trans="00000000"):
    rec = (x(pid, 8) + x(acct, 10) + x(name, 30) + x(ctype, 1)
           + b"00000000" + b"00000000" + x(status, 1)
           + comp3(total) + comp3(cash) + x(user, 8) + x(last_trans, 8) + x("", 50))
    assert len(rec) == 148, len(rec)
    return rec


PORTFOLIOS = [
    port("PORT0001", "1234567890", "GROWTH FUND CLIENT", "I", "A", "12345.67", "1000.00"),
    port("PORT0002", "2345678901", "INCOME TRUST", "T", "A", "98765.43", "250.50"),
    port("PORT0001", "1234567890", "DUPLICATE OF PORT0001", "I", "A", "1.00", "0.00"),
    port("PORT0003", "3333333333", "", "I", "A", "10.00", "0.00"),
    port("PORT0004", "4444444444", "CLOSED ON ARRIVAL", "C", "C", "10.00", "0.00"),
    port("", "5555555555", "NO ID", "I", "A", "10.00", "0.00"),
    port("PORT0005", "3456789012", "ACME CORPORATE", "C", "A", "-42.10", "99999.99"),
]

UPDATES = [
    ("PORT0001", "1234567890", "N", "GROWTH FUND RENAMED"),
    ("PORT0002", "2345678901", "S", "SUSPENDED"),
    ("PORT0009", "0000000009", "N", "MISSING"),
    ("PORT0005", "3456789012", "V", "250000"),
    ("PORT0001", "1234567890", "V", "1050.75"),
    ("PORT0001", "1234567890", "X", "UNKNOWN ACTION"),
]

DELETES = [
    ("PORT0002", "2345678901", "01"),
    ("PORT0009", "0000000009", "03"),
]

VALIDATIONS = [
    ("I", "PORT1234"), ("I", "PORX1234"), ("I", "PORT12A4"), ("I", ""),
    ("A", "1234567890"), ("A", "0" * 50), ("A", "1" * 50),
    ("T", "STK"), ("T", "BND"), ("T", "MMF"), ("T", "ETF"), ("T", "XYZ"),
    ("M", "100"), ("X", "ANY"),
]


def main():
    (HERE / "portadd.dat").write_bytes(b"".join(PORTFOLIOS))
    (HERE / "portupdt.dat").write_bytes(
        b"".join(x(i, 8) + x(a, 10) + x(c, 1) + x(v, 50) for i, a, c, v in UPDATES))
    (HERE / "portdel.dat").write_bytes(
        b"".join(x(i, 8) + x(a, 10) + x(r, 2) + x("", 60) for i, a, r in DELETES))
    (HERE / "portvald.txt").write_text(
        "".join(f"{t}{v.ljust(50)}\n" for t, v in VALIDATIONS))


if __name__ == "__main__":
    main()
