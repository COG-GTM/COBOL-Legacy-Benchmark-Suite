# Golden Test Fixtures (Ticket 0.7)

Golden datasets derived from the COBOL test data generator
(`src/programs/test/TSTGEN00.cbl`) for use by the Java migration test suites.

## Why a Python fallback generator was used

`TSTGEN00.cbl` **cannot be compiled** with GnuCOBOL (or any COBOL compiler):
it is a skeleton program. Its `PROCEDURE DIVISION` PERFORMs the paragraphs
`2210-GEN-PORT-DATA`, `2220-WRITE-PORT-RECORD`, `2310-GEN-TRAN-DATA`,
`2320-WRITE-TRAN-RECORD`, `2410-GEN-DATA-ERRORS`, `2420-GEN-PROCESS-ERRORS`,
`2510-GEN-LARGE-PORTFOLIO`, and `2520-GEN-LARGE-TRANSACTION`, but none of
these paragraphs are defined in the source. It also has unrelated compile
errors (FD records missing PICTURE, ambiguous `WS-PORT-STATUS` /
`WS-TRAN-STATUS`, undefined `WS-ERROR-MESSAGE`).

Per the ticket's fallback instruction, `tools/tstgen.py` faithfully replicates
all *defined* behaviour of TSTGEN00 (config-driven dispatch, seed file,
file structure, EVALUATE on `CFG-TEST-TYPE`) and fills in the missing
data-generation paragraphs with deterministic, seed-driven logic that emits
records conforming exactly to the copybook layouts. The `ERROR` and `VOLUME`
test types are rejected because their generation logic does not exist in the
COBOL source to replicate.

## Record layouts

### Portfolio master record — `PORTFLIO.cpy`, 148 bytes/record

| Offset | Len | Field | PIC | Notes |
|---|---|---|---|---|
| 0 | 8 | PORT-ID | X(8) | |
| 8 | 10 | PORT-ACCOUNT-NO | X(10) | |
| 18 | 30 | PORT-CLIENT-NAME | X(30) | |
| 48 | 1 | PORT-CLIENT-TYPE | X(1) | I/C/T |
| 49 | 8 | PORT-CREATE-DATE | 9(8) | YYYYMMDD |
| 57 | 8 | PORT-LAST-MAINT | 9(8) | YYYYMMDD |
| 65 | 1 | PORT-STATUS | X(1) | A/C/S |
| 66 | 8 | PORT-TOTAL-VALUE | S9(13)V99 COMP-3 | packed decimal |
| 74 | 8 | PORT-CASH-BALANCE | S9(13)V99 COMP-3 | packed decimal |
| 82 | 8 | PORT-LAST-USER | X(8) | |
| 90 | 8 | PORT-LAST-TRANS | 9(8) | |
| 98 | 50 | PORT-FILLER | X(50) | spaces |

### Transaction record — `TRNREC.cpy`, 152 bytes/record

| Offset | Len | Field | PIC | Notes |
|---|---|---|---|---|
| 0 | 8 | TRN-DATE | X(8) | YYYYMMDD |
| 8 | 6 | TRN-TIME | X(6) | HHMMSS |
| 14 | 8 | TRN-PORTFOLIO-ID | X(8) | |
| 22 | 6 | TRN-SEQUENCE-NO | X(6) | |
| 28 | 10 | TRN-INVESTMENT-ID | X(10) | |
| 38 | 2 | TRN-TYPE | X(2) | BU/SL/TR/FE |
| 40 | 8 | TRN-QUANTITY | S9(11)V9(4) COMP-3 | packed decimal |
| 48 | 8 | TRN-PRICE | S9(11)V9(4) COMP-3 | packed decimal |
| 56 | 8 | TRN-AMOUNT | S9(13)V9(2) COMP-3 | packed decimal |
| 64 | 3 | TRN-CURRENCY | X(3) | ISO code |
| 67 | 1 | TRN-STATUS | X(1) | P/D/F/R |
| 68 | 26 | TRN-PROCESS-DATE | X(26) | DB2 timestamp format |
| 94 | 8 | TRN-PROCESS-USER | X(8) | |
| 102 | 50 | TRN-FILLER | X(50) | spaces |

Note: the JCL (`src/jcl/test/TSTGEN.jcl`) declares `LRECL=100` for both
output datasets, which is inconsistent with the copybook layouts (148/152
bytes). The copybooks are authoritative; the fixtures use the copybook
lengths.

COMP-3 fields use standard packed-decimal encoding: two digits per byte, sign
in the final nibble (0xC positive, 0xD negative). Records are ASCII text plus
packed-decimal binary; there is no record delimiter (fixed-width, like
mainframe RECFM=FB datasets).

## Files

- `config/tstcfg.txt` — TSTCFG config (TEST-TYPE X(10), VOLUME 9(6), PARAMETERS X(64)): 25 portfolio + 50 transaction records
- `config/randseed.txt` — RANDSEED file (9-digit seed `123456789`)
- `data/portfolio.dat` / `data/transactions.dat` — raw fixed-width records
- `data/portfolio.csv` / `.json`, `data/transactions.csv` / `.json` — parsed equivalents; COMP-3 fields rendered as scaled decimal strings suitable for `new BigDecimal(...)`
- `tools/tstgen.py` — deterministic generator (TSTGEN00 replica)
- `tools/parse_fixtures.py` — `.dat` → `.csv`/`.json` converter

## How to regenerate

```bash
cd java/test-fixtures
python3 tools/tstgen.py --config config/tstcfg.txt --seed config/randseed.txt \
    --portout data/portfolio.dat --tranout data/transactions.dat
python3 tools/parse_fixtures.py portfolio   data/portfolio.dat    data/portfolio.csv    data/portfolio.json
python3 tools/parse_fixtures.py transaction data/transactions.dat data/transactions.csv data/transactions.json
```

The generator is fully deterministic (Lehmer/MINSTD LCG seeded from
`config/randseed.txt`); regenerating with the same config and seed produces
byte-identical `.dat` files.
