#!/usr/bin/env python3
"""Golden test-data generator replicating src/programs/test/TSTGEN00.cbl.

TSTGEN00.cbl is a skeleton on this repo: the record-generation paragraphs it
PERFORMs (2210-GEN-PORT-DATA, 2220-WRITE-PORT-RECORD, 2310-GEN-TRAN-DATA,
2320-WRITE-TRAN-RECORD, 2410/2420/2510/2520) are referenced but never defined,
so the program cannot compile under GnuCOBOL (or any COBOL compiler).

This script faithfully replicates the program's defined behaviour:
  - reads a fixed-width config file (TSTCFG): CFG-TEST-TYPE PIC X(10),
    CFG-VOLUME PIC 9(6), CFG-PARAMETERS PIC X(64)
  - reads a 9-digit random seed (RANDSEED): SEED-RECORD PIC 9(9)
  - for each config record, generates CFG-VOLUME records of the requested
    type into PORTOUT / TRANOUT
and fills in the missing data-generation paragraphs with deterministic,
seed-driven logic that produces records conforming exactly to the copybook
layouts PORTFLIO.cpy (148 bytes) and TRNREC.cpy (152 bytes), with COMP-3
fields encoded as packed decimal.

Usage:
  python3 tstgen.py --config <TSTCFG file> --seed <RANDSEED file> \
      --portout portfolio.dat --tranout transactions.dat
"""

import argparse
from pathlib import Path

# ---------------------------------------------------------------------------
# Packed decimal (COMP-3) encoding
# ---------------------------------------------------------------------------


def comp3(value_scaled: int, digits: int) -> bytes:
    """Encode an already-scaled integer as packed decimal with `digits` digits.

    A PIC S9(13)V99 COMP-3 field has 15 digits + sign nibble = 8 bytes.
    Sign nibble: 0xC positive, 0xD negative.
    """
    sign = 0xC if value_scaled >= 0 else 0xD
    s = str(abs(value_scaled)).rjust(digits, "0")
    if len(s) > digits:
        raise ValueError(f"value {value_scaled} exceeds {digits} digits")
    nibbles = [int(c) for c in s] + [sign]
    if len(nibbles) % 2:
        nibbles.insert(0, 0)
    return bytes((nibbles[i] << 4) | nibbles[i + 1] for i in range(0, len(nibbles), 2))


def pic_x(text: str, n: int) -> bytes:
    return text[:n].ljust(n).encode("ascii")


def pic_9(value: int, n: int) -> bytes:
    return str(value).rjust(n, "0")[-n:].encode("ascii")


# ---------------------------------------------------------------------------
# Deterministic pseudo-random sequence (replaces WS-RANDOM-SEED handling).
# Linear congruential generator so output is reproducible from RANDSEED.
# ---------------------------------------------------------------------------


class Lcg:
    M = 2_147_483_647  # 2^31 - 1
    A = 16_807         # Lehmer / MINSTD

    def __init__(self, seed: int):
        self.state = (seed % self.M) or 1

    def next(self) -> int:
        self.state = (self.state * self.A) % self.M
        return self.state

    def randint(self, lo: int, hi: int) -> int:
        return lo + self.next() % (hi - lo + 1)

    def choice(self, seq):
        return seq[self.next() % len(seq)]


# ---------------------------------------------------------------------------
# Record builders (PORTFLIO.cpy and TRNREC.cpy layouts)
# ---------------------------------------------------------------------------

CLIENT_TYPES = ["I", "C", "T"]          # 88 PORT-INDIVIDUAL/CORPORATE/TRUST
PORT_STATUSES = ["A", "A", "A", "C", "S"]  # weighted toward ACTIVE
TRAN_TYPES = ["BU", "SL", "TR", "FE"]   # 88 TRN-TYPE-*
TRAN_STATUSES = ["P", "D", "D", "F", "R"]  # weighted toward DONE
CURRENCIES = ["USD", "USD", "USD", "EUR", "GBP"]
FIRST_NAMES = ["ALICE", "BOB", "CAROL", "DAVID", "EMMA",
               "FRANK", "GRACE", "HENRY", "IRENE", "JACK"]
LAST_NAMES = ["ANDERSON", "BAKER", "CARTER", "DIAZ", "EVANS",
              "FISHER", "GARCIA", "HUGHES", "IRWIN", "JONES"]
INVESTMENTS = ["IBM       ", "MSFT      ", "AAPL      ", "GOOG      ",
               "TBILL3M   ", "TBOND10Y  ", "SP500IDX  ", "CORPBND A "]

PORTFOLIO_LRECL = 148
TRANSACTION_LRECL = 152


def gen_portfolio_record(rng: Lcg, seq: int) -> bytes:
    """2210-GEN-PORT-DATA / 2220-WRITE-PORT-RECORD equivalent."""
    port_id = f"P{seq:07d}"                       # PORT-ID        X(8)
    account = f"{rng.randint(1000000000, 1999999999)}"  # PORT-ACCOUNT-NO X(10)
    name = f"{rng.choice(FIRST_NAMES)} {rng.choice(LAST_NAMES)}"
    client_type = rng.choice(CLIENT_TYPES)
    create_date = 20230000 + rng.randint(1, 12) * 100 + rng.randint(1, 28)
    last_maint = 20240000 + rng.randint(1, 12) * 100 + rng.randint(1, 28)
    status = rng.choice(PORT_STATUSES)
    total_value = rng.randint(0, 5_000_000_00)    # scaled by 100 (V99)
    cash_balance = rng.randint(-50_000_00, 500_000_00)
    last_user = f"TST{rng.randint(1, 999):05d}"
    last_trans = rng.randint(1, 99999999)

    rec = b"".join([
        pic_x(port_id, 8),
        pic_x(account, 10),
        pic_x(name, 30),
        pic_x(client_type, 1),
        pic_9(create_date, 8),
        pic_9(last_maint, 8),
        pic_x(status, 1),
        comp3(total_value, 15),     # PORT-TOTAL-VALUE  S9(13)V99 COMP-3
        comp3(cash_balance, 15),    # PORT-CASH-BALANCE S9(13)V99 COMP-3
        pic_x(last_user, 8),
        pic_9(last_trans, 8),
        pic_x("", 50),              # PORT-FILLER
    ])
    assert len(rec) == PORTFOLIO_LRECL
    return rec


def gen_transaction_record(rng: Lcg, seq: int) -> bytes:
    """2310-GEN-TRAN-DATA / 2320-WRITE-TRAN-RECORD equivalent."""
    trn_date = 20240000 + rng.randint(1, 12) * 100 + rng.randint(1, 28)
    trn_time = rng.randint(0, 23) * 10000 + rng.randint(0, 59) * 100 \
        + rng.randint(0, 59)
    portfolio_id = f"P{rng.randint(1, 25):07d}"
    investment = rng.choice(INVESTMENTS)
    trn_type = rng.choice(TRAN_TYPES)
    quantity = rng.randint(1_0000, 10_000_0000)      # scaled by 10^4 (V9(4))
    price = rng.randint(1_0000, 1_000_0000)          # scaled by 10^4
    amount = (quantity * price) // 10 ** 6           # scaled by 100 (V99)
    if trn_type == "SL":
        quantity, amount = -quantity, -amount
    currency = rng.choice(CURRENCIES)
    status = rng.choice(TRAN_STATUSES)
    process_date = (f"{trn_date // 10000:04d}-{trn_date // 100 % 100:02d}-"
                    f"{trn_date % 100:02d}-{trn_time // 10000:02d}."
                    f"{trn_time // 100 % 100:02d}.{trn_time % 100:02d}.000000")
    process_user = f"TST{rng.randint(1, 999):05d}"

    rec = b"".join([
        pic_9(trn_date, 8),         # TRN-DATE X(8) YYYYMMDD
        pic_9(trn_time, 6),         # TRN-TIME X(6) HHMMSS
        pic_x(portfolio_id, 8),     # TRN-PORTFOLIO-ID
        pic_9(seq, 6),              # TRN-SEQUENCE-NO
        pic_x(investment, 10),      # TRN-INVESTMENT-ID
        pic_x(trn_type, 2),
        comp3(quantity, 15),        # TRN-QUANTITY S9(11)V9(4) COMP-3
        comp3(price, 15),           # TRN-PRICE    S9(11)V9(4) COMP-3
        comp3(amount, 15),          # TRN-AMOUNT   S9(13)V9(2) COMP-3
        pic_x(currency, 3),
        pic_x(status, 1),
        pic_x(process_date, 26),    # TRN-PROCESS-DATE (DB2 timestamp style)
        pic_x(process_user, 8),
        pic_x("", 50),              # TRN-FILLER
    ])
    assert len(rec) == TRANSACTION_LRECL
    return rec


# ---------------------------------------------------------------------------
# Main: mirrors 0000-MAIN / 2000-PROCESS / 2100-GENERATE-TEST-DATA
# ---------------------------------------------------------------------------


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--config", required=True, help="TSTCFG config file")
    ap.add_argument("--seed", required=True, help="RANDSEED file (9 digits)")
    ap.add_argument("--portout", required=True, help="PORTOUT output file")
    ap.add_argument("--tranout", required=True, help="TRANOUT output file")
    args = ap.parse_args()

    seed = int(Path(args.seed).read_text().strip())
    rng = Lcg(seed)

    port_records = []
    tran_records = []

    # 2000-PROCESS: read each CONFIG-RECORD and dispatch on CFG-TEST-TYPE
    for line in Path(args.config).read_text().splitlines():
        if not line.strip():
            continue
        test_type = line[0:10].strip()
        volume = int(line[10:16])
        # CFG-PARAMETERS (line[16:80]) is read but unused by TSTGEN00

        if test_type == "PORTFOLIO":          # 2200-GEN-PORTFOLIO
            for i in range(1, volume + 1):
                port_records.append(gen_portfolio_record(rng, i))
        elif test_type == "TRANSACTN":        # 2300-GEN-TRANSACTION
            for i in range(1, volume + 1):
                tran_records.append(gen_transaction_record(rng, i))
        else:
            raise SystemExit(f"INVALID TEST TYPE: {test_type!r} "
                             "(ERROR/VOLUME paragraphs are undefined in "
                             "TSTGEN00.cbl and are not replicated)")

    Path(args.portout).write_bytes(b"".join(port_records))
    Path(args.tranout).write_bytes(b"".join(tran_records))
    print(f"wrote {len(port_records)} portfolio records -> {args.portout}")
    print(f"wrote {len(tran_records)} transaction records -> {args.tranout}")


if __name__ == "__main__":
    main()
