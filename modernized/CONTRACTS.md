# Portfolio Modernization Contracts

Authoritative spec for the COBOL -> JavaScript conversion of the `src/programs/portfolio/`
programs. Everything in this document is either (a) copied from the copybooks, or (b)
measured by executing the COBOL under GnuCOBOL 3.1.2. Nothing here is guessed; items that
could not be executed are explicitly marked **DERIVED**.

Both workstreams build against this file. If a workstream finds a fact here to be wrong,
stop and report the discrepancy rather than silently coding around it.

- Target platform: **AWS Lambda-style handler** (single entry point, routes on an action field).
- Datastore: **in-memory / JSON document store** keyed on the 18-byte `PORT-KEY` (demo scope).
- Money and quantity: **decimal.js**. Native JS numbers are not permitted for any COMP-3 field.

---

## 1. Record geometry (measured)

Measured with `LENGTH OF` under GnuCOBOL against `src/copybook/common/*.cpy`. The copybooks
are the source of truth, **not** the simplified layout in
`documentation/operations/test-data-specs.md`.

### 1.1 `PORT-RECORD` (from `PORTFLIO.cpy`) — total **148 bytes**

| Field | PIC | Offset | Bytes |
|---|---|---|---|
| `PORT-ID` | `X(8)` | 0 | 8 |
| `PORT-ACCOUNT-NO` | `X(10)` | 8 | 10 |
| `PORT-CLIENT-NAME` | `X(30)` | 18 | 30 |
| `PORT-CLIENT-TYPE` | `X(1)` | 48 | 1 |
| `PORT-CREATE-DATE` | `9(8)` | 49 | 8 |
| `PORT-LAST-MAINT` | `9(8)` | 57 | 8 |
| `PORT-STATUS` | `X(1)` | 65 | 1 |
| `PORT-TOTAL-VALUE` | `S9(13)V99 COMP-3` | 66 | 8 |
| `PORT-CASH-BALANCE` | `S9(13)V99 COMP-3` | 74 | 8 |
| `PORT-LAST-USER` | `X(8)` | 82 | 8 |
| `PORT-LAST-TRANS` | `9(8)` | 90 | 8 |
| `PORT-FILLER` | `X(50)` | 98 | 50 |

`PORT-KEY` = offsets 0..17 (18 bytes) = `PORT-ID` + `PORT-ACCOUNT-NO`. This matches
`KEYS(18 0)` in `src/jcl/portfolio/PORTDEF.jcl`.

Condition names (level 88): `PORT-CLIENT-TYPE` in {`I` individual, `C` corporate, `T` trust};
`PORT-STATUS` in {`A` active, `C` closed, `S` suspended}.

> **Discrepancy on record length.** `PORTDEF.jcl` declares `RECORDSIZE(200 200)` but the
> copybook layout is 148 bytes. The copybook wins (per instruction). Treat bytes 148..199
> as unused slack on the legacy VSAM cluster; the JS store does not model them.

### 1.2 `TRANSACTION-RECORD` (from `TRNREC.cpy`) — total **152 bytes**

| Field | PIC | Offset | Bytes |
|---|---|---|---|
| `TRN-DATE` | `X(8)` | 0 | 8 |
| `TRN-TIME` | `X(6)` | 8 | 6 |
| `TRN-PORTFOLIO-ID` | `X(8)` | 14 | 8 |
| `TRN-SEQUENCE-NO` | `X(6)` | 22 | 6 |
| `TRN-INVESTMENT-ID` | `X(10)` | 28 | 10 |
| `TRN-TYPE` | `X(2)` | 38 | 2 |
| `TRN-QUANTITY` | `S9(11)V9(4) COMP-3` | 40 | 8 |
| `TRN-PRICE` | `S9(11)V9(4) COMP-3` | 48 | 8 |
| `TRN-AMOUNT` | `S9(13)V9(2) COMP-3` | 56 | 8 |
| `TRN-CURRENCY` | `X(3)` | 64 | 3 |
| `TRN-STATUS` | `X(1)` | 67 | 1 |
| `TRN-PROCESS-DATE` | `X(26)` | 68 | 26 |
| `TRN-PROCESS-USER` | `X(8)` | 94 | 8 |
| `TRN-FILLER` | `X(50)` | 102 | 50 |

`TRN-KEY` = offsets 0..27 (28 bytes). `TRN-TYPE` in {`BU`, `SL`, `TR`, `FE`};
`TRN-STATUS` in {`P`, `D`, `F`, `R`}.

### 1.3 `PORTREC` — **DERIVED, does not exist in the repository**

`PORTTRAN.cbl:40` contains `COPY PORTREC.` but **no `PORTREC` copybook exists anywhere in
this repository**, so `PORTTRAN` does not compile as-is (`error: PORTREC: No such file or
directory`). `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST`, which `PORTTRAN` reads and rewrites,
are absent from `PORTFLIO.cpy`.

The layout below is **derived from `PORTTRAN`'s own field usage**, chosen to preserve both
the 148-byte record length and the 18-byte key so it stays wire-compatible with `PORTFLIO`:
`PORT-FILLER X(50)` is replaced by the two position fields plus a shortened filler.

| Field | PIC | Offset | Bytes |
|---|---|---|---|
| ...identical to `PORT-RECORD` offsets 0..97... | | 0 | 98 |
| `PORT-TOTAL-UNITS` | `S9(11)V9(4) COMP-3` | 98 | 8 |
| `PORT-TOTAL-COST` | `S9(13)V99 COMP-3` | 106 | 8 |
| `PORT-FILLER` | `X(34)` | 114 | 34 |

Everything downstream that depends on `PORTREC` (i.e. every `PORTTRAN`/transaction
expectation) is tagged `derived: true` in the golden set and reported as **DERIVED** in the
parity report — it is reasoned from documented rules, not captured from execution.

> `PORTTRAN`'s `SELECT` declares `RECORD KEY IS PORT-ID` (8 bytes), unlike every other
> program, which uses the full 18-byte `PORT-KEY`. The JS transaction path therefore
> resolves a portfolio by `PORT-ID` alone and must fail explicitly if the ID is ambiguous
> across account numbers.

---

## 2. COMP-3 (packed decimal) codec

A `PIC S9(a)V9(b) COMP-3` field occupies `ceil((a+b+1)/2)` bytes. Digits are stored two per
byte, most significant first, with the low nibble of the final byte holding the sign:
`0xC` = positive, `0xD` = negative. Both fields used here (15 total digits) occupy 8 bytes.

The decoder must return a `Decimal`, never a JS `number`, and must preserve scale
(`b` fractional digits). The encoder must round-trip every vector below exactly.

### Reference vectors (captured from GnuCOBOL, not hand-computed)

Raw bytes are committed at `golden/vectors/comp3-vectors.bin` (12 records x 8 bytes) and
mirrored as JSON at `golden/vectors/comp3-vectors.json`. The codec test suite must assert
against these bytes in both directions. **`comp3-vectors.bin` is the authority** — it is raw
COBOL output; the table below is a transcription of it for readability.

| # | PIC | Value | Hex |
|---|---|---|---|
| 1 | `S9(13)V99` | `0.00` | `000000000000000C` |
| 2 | `S9(13)V99` | `1.00` | `000000000000100C` |
| 3 | `S9(13)V99` | `0.01` | `000000000000001C` |
| 4 | `S9(13)V99` | `12345678.90` | `000001234567890C` |
| 5 | `S9(13)V99` | `-12345678.90` | `000001234567890D` |
| 6 | `S9(13)V99` | `9999999999999.99` | `999999999999999C` |
| 7 | `S9(13)V99` | `-9999999999999.99` | `999999999999999D` |
| 8 | `S9(13)V99` | `-0.05` | `000000000000005D` |
| 9 | `S9(11)V9(4)` | `0.0000` | `000000000000000C` |
| 10 | `S9(11)V9(4)` | `100.5000` | `000000001005000C` |
| 11 | `S9(11)V9(4)` | `-100.5000` | `000000001005000D` |
| 12 | `S9(11)V9(4)` | `0.0001` | `000000000000001C` |

Note vectors 3 and 12 share identical bytes — the byte pattern alone is ambiguous, so the
decoder **must** be told the scale by the schema. Any codec that infers scale from bytes is
wrong.

---

## 3. Fixed-width / display-field codec

- `X(n)`: space-padded on the right. Decoding trims trailing spaces; a field that is all
  spaces decodes to `""`. Encoding pads to exactly `n`; longer input is an error, never a
  silent truncation.
- `9(n)` (display, unsigned): ASCII digits, zero-padded on the left. `PORT-CREATE-DATE`,
  `PORT-LAST-MAINT` and `PORT-LAST-TRANS` are `YYYYMMDD`; `00000000` means "unset" and
  normalizes to `null`.
- Normalization for comparison: trailing-space-insensitive for `X(n)`, `Decimal.eq` (not
  string or byte equality) for COMP-3, `null`-equivalent for all-zero dates.

---

## 4. Validation (`PORTVALD`) — measured, and it does not match the documentation

`PORTVALD` takes `LS-VALIDATE-TYPE X(1)`, `LS-INPUT-VALUE X(50)` and returns
`LS-RETURN-CODE S9(4) COMP` plus `LS-ERROR-MSG X(50)`. Return codes from `PORTVAL.cpy`:
`0` success, `1` invalid ID, `2` invalid account, `3` invalid type, `4` invalid amount.
`VAL-MIN-AMOUNT`/`VAL-MAX-AMOUNT` are `-9999999999999.99`/`+9999999999999.99` — i.e. the
full range of the field, so the range check can never reject a representable value.

### 4.1 Observed behaviour — EXECUTED, not derived

Captured by `golden/cobol/VALDRV.cbl`, which `CALL`s the real unmodified `PORTVALD`. The
authoritative output is committed at `golden/expected/portvald.txt`; reproduce with
`golden/cobol/run-baseline.sh`. **That file is the authority** — the table is a transcription.

| Case | Type | Input | RC | Message |
|---|---|---|---|---|
| VAL-01 | `I` | `PORT0001` | **1** | `Invalid Portfolio ID format` |
| VAL-02 | `I` | `PORT9999` | **1** | `Invalid Portfolio ID format` |
| VAL-03 | `I` | `XXXX0001` | 1 | `Invalid Portfolio ID format` |
| VAL-04 | `I` | `PORTABCD` | 1 | `Invalid Portfolio ID format` |
| VAL-05 | `I` | (spaces) | 1 | `Invalid Portfolio ID format` |
| VAL-06 | `A` | `0000000001` | **2** | `Invalid Account Number format` |
| VAL-07 | `A` | `1234567890` | **2** | `Invalid Account Number format` |
| VAL-08 | `A` | `0000000000` | 2 | `Invalid Account Number format` |
| VAL-09 | `A` | `12345ABCDE` | 2 | `Invalid Account Number format` |
| VAL-10 | `T` | `STK` | 0 | (spaces) |
| VAL-11 | `T` | `BND` | 0 | (spaces) |
| VAL-12 | `T` | `MMF` | 0 | (spaces) |
| VAL-13 | `T` | `ETF` | 0 | (spaces) |
| VAL-14 | `T` | `XYZ` | 3 | `Invalid Investment Type` |
| VAL-15 | `T` | `stk` | 3 | `Invalid Investment Type` |
| VAL-16 | `M` | `000000000100000` | 0 | (spaces) |
| VAL-17 | `M` | `999999999999999` | 0 | (spaces) |
| VAL-18 | `M` | `1000.00` | 0 | (spaces) |
| VAL-19 | `M` | `NOTANUM` | **0** | (spaces) |
| VAL-20 | `Z` | `PORT0001` | 1 | `Invalid validation type` |

### 4.2 Why, and what the JS must do

**Two of the four rules always reject; a third always accepts. Only type validation works.**

- **ID — always rejects.** `MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK` moves 4
  characters into a `PIC X(10)` field, which left-justifies and space-pads to `"0001      "`.
  `IS NUMERIC` on an alphanumeric item requires *every* character to be a digit, so the
  embedded spaces fail the test for **every** input. A well-formed `PORT0001` is rejected
  (VAL-01). Legacy `I` therefore returns `1` unconditionally.
- **Account — always rejects.** `IF LS-INPUT-VALUE IS NOT NUMERIC` tests all 50 bytes of the
  parameter, so a 10-digit account number followed by 40 spaces is never numeric.
  Legacy `A` returns `2` unconditionally.
- **Type — works, and accepts all four documented types.** Comparing `X(50)` against `'STK'`
  space-pads the literal, so the match succeeds. `STK`, `BND`, `MMF`, `ETF` all return `0`
  (VAL-10..13); the comparison is case-sensitive, so `stk` returns `3` (VAL-15). Legacy and
  modernized behaviour are **identical** here — there is no divergence for `T`.
- **Amount — always accepts, including non-numeric text.**
  `MOVE LS-INPUT-VALUE TO VAL-TEMP-NUM` is an alphanumeric-to-numeric move, and the bounds
  `VAL-MIN-AMOUNT`/`VAL-MAX-AMOUNT` are the field's own representable limits, so the range
  test can never fail. `NOTANUM` returns `0` (VAL-19). Legacy `M` returns `0`
  unconditionally, and `VAL-ERR-AMT` (`Amount outside valid range`) is dead text that the
  legacy program can never emit.

The JS validation module therefore implements **two modes**, and the golden set pins both:

- `mode: 'legacy'` — bug-for-bug reproduction of the table in 4.1. **This is the mode the
  parity harness asserts against**, because parity is defined against observed behaviour.
- `mode: 'modernized'` — the documented intent (ID = `PORT` + 4 numerics, account = 10
  numerics and not all zeros, type in {STK,BND,MMF,ETF}, amount numeric and within
  VAL-MIN/MAX).

The modes diverge on exactly three validation types — `I`, `A` and `M`. They agree on `T`
and on the unknown-type arm. Every divergence is enumerated in the parity report as a
**known, intentional divergence** with the reason. Divergences that are *not* on that list
fail the build.

Error message strings must be reproduced verbatim from `PORTVAL.cpy`:
`Invalid Portfolio ID format`, `Invalid Account Number format`, `Invalid Investment Type`,
`Amount outside valid range`, and the inline literal `Invalid validation type`.

---

## 5. Status and error mapping

`FILE STATUS` -> result, per instruction:

| FILE STATUS | Meaning | Result | HTTP |
|---|---|---|---|
| `00` | success | ok | 200 (201 on create) |
| `22` | duplicate key on write | conflict | 409 |
| `23` | record not found | notFound | 404 |
| `10` | end of file | endOfFile (control flow only) | n/a |
| other | I/O failure | ioError | 500 |

Validation failures (pre-I/O rejects, e.g. `PORTADD`'s blank-ID / blank-name / status-not-`A`
check) map to **400** and carry the legacy `LS-RETURN-CODE` and message verbatim.

`CALL 'ERRPROC'` -> structured `logger.error({ program, category, code, text, key })`.
`CALL 'AUDPROC'` / the `AUDIT-FILE` write in `PORTDEL` -> an audit record appended to the
audit store, retaining the legacy field set (`timestamp`, `action`, `key`, `reason`,
`status`) so the golden set can diff it.

---

## 6. Action routing (`PORTMSTR`)

`PORTMSTR` dispatches on its `LS-COMMAND-AREA`. The single Lambda handler mirrors it:

| Action | Legacy paragraph | Legacy program |
|---|---|---|
| `create` | `2000-CREATE-PORTFOLIO` | `PORTADD` |
| `read` | `3000-READ-PORTFOLIO` | `PORTREAD` |
| `list` | `2000-PROCESS` loop (`READ NEXT`) | `PORTREAD` |
| `update` | `4000-UPDATE-PORTFOLIO` | `PORTUPDT` |
| `delete` | `5000-DELETE-PORTFOLIO` | `PORTDEL` |
| `transaction` | `2200-UPDATE-POSITIONS` | `PORTTRAN` |
| (unknown) | `WHEN OTHER` -> `Invalid command` | `PORTMSTR` |

---

## 7. Legacy compilation status (measured)

Relevant because it determines which "before" numbers are executed vs derived.

| Program | `cobc` result | Notes |
|---|---|---|
| `PORTREAD` | compiles clean | baseline executable |
| `PORTVALD` | compiles clean (`-m`) | baseline module; behaviour in section 4 |
| `PORTUPDT` | compiles after staging fix | line 1 comment sits in column 8, not 7 |
| `PORTADD` | **does not compile** | `PORT-RECORD` ambiguous: both `FD`s `COPY PORTFLIO` |
| `PORTDEL` | **does not compile** | syntax error at line 169 (`ACCEPT ... FROM TIME STAMP`) |
| `PORTMSTR` | **does not compile** | `USING` on an executable; `LS-*`/`ERR-*` fields undefined |
| `PORTTRAN` | **does not compile** | missing `PORTREC` copybook (section 1.3) |
| `TSTGEN00` | **does not compile** | `COPY ... REPLACING ==:PREFIX:==` against a copybook with no such marker; ~8 paragraphs `PERFORM`ed but never defined |
| `TSTVAL00` | **does not compile** | `SELECT ... ASSIGN TO ACTUAL` uses a reserved word; ~7 paragraphs `PERFORM`ed but never defined |

---

## 8. Legacy defects that change observable behaviour

These are read off the source, not guessed. The JS port reproduces them in `legacy` mode and
each one is pinned by a golden case, so the modernized alternative can never drift silently.

1. **`PORTTRAN`'s position math is dead code.** `2200-UPDATE-POSITIONS` — and therefore
   `2210-PROCESS-BUY`, `2220-PROCESS-SELL`, `2230-PROCESS-TRANSFER`, `2240-PROCESS-FEE` and
   the whole audit trail — is never `PERFORM`ed. The only reachable path is
   `0000-MAIN` -> `2000-PROCESS-TRANSACTIONS` -> `2100-VALIDATE-TRANSACTION` ->
   `2110/2120/2130`. So the legacy program **validates transactions and counts them, but
   never updates any portfolio**. The BU/SL/TR/FE math the task describes exists in the
   source but cannot execute.
   The JS port implements the math (that is the point of the conversion) and the golden set
   asserts both: the *reachable* legacy result (validate + count only) and the *intended*
   position math, the latter labelled DERIVED.
2. **`PORTTRAN` `REWRITE PORTFOLIO-RECORD`** names a record that is never declared (the FD's
   record is `PORT-RECORD` via `COPY PORTREC`). Another reason the paragraph cannot compile.
3. **`PORTUPDT` silently succeeds on an unknown action.** `2200-APPLY-UPDATE`'s `EVALUATE`
   has no `WHEN OTHER`, so an action that is not `S`/`V`/`N` falls through, the unchanged
   record is `REWRITE`n, and the run is counted as a **successful update**. Not an error.
4. **`PORTUPDT` value update goes through an alphanumeric-to-numeric `MOVE`.**
   `MOVE UPDT-NEW-VALUE` (`X(50)`) `TO WS-NUMERIC-WORK` (`S9(13)V99`) then into
   `PORT-TOTAL-VALUE`. The result for input containing a `.` or trailing spaces is decided by
   the runtime, not by the program. `PORTUPDT` compiles, so this expectation is **captured by
   execution**, never hand-derived.
5. **`PORTADD` declares `COPY PORTFLIO` under two different `FD`s**, so `PORT-RECORD` and
   `PORT-KEY` are ambiguous and `WRITE PORT-RECORD` will not compile. The staged copy
   qualifies the input FD's record; the transform is recorded in the before/after artifact.
6. **`PORTDEL` uses `ACCEPT ... FROM TIME STAMP`**, which GnuCOBOL rejects. Staged to
   `FUNCTION CURRENT-DATE`. The audit timestamp is therefore normalized out of the diff (it
   is nondeterministic by nature); the remaining audit fields are compared exactly.
7. **Two different `AUDIT-RECORD` layouts** exist under the same `01` name: `PORTDEL`'s
   inline 80-byte record, and `AUDITLOG.cpy`'s 392-byte record used by `PORTTRAN`. Both are
   modelled; do not conflate them.
8. **`PORTADD` never validates the account number** and never calls `PORTVALD`. Its only
   check is blank ID / blank name / status not `A`. Portfolio IDs that `PORTVALD` would
   reject are accepted by the create path.

---

## 9. Staging rule

Staging rule: **do not edit files under `src/`**. Compile from a staged copy under
`build/stage/` produced by a documented, minimal transform, and record the transform per
program in the before/after artifact so the "before" is reproducible and auditable.

---

## 10. The executed COBOL baseline

`golden/cobol/stage.sh` compiles the staged legacy programs; `golden/cobol/run-baseline.sh`
runs them against the golden input deck generated by `golden/cobol/GOLDGEN.cbl` and captures
the results in `golden/expected/`. **Those files are the EXPECTED-RESULTS the JS side is
diffed against.** Five of the seven programs are genuinely executed.

| Program | Baseline | Artifacts |
|---|---|---|
| `PORTVALD` | **EXECUTED** | `portvald.txt` |
| `PORTREAD` | **EXECUTED** | `portread.stdout.txt`, `seed-state.txt` |
| `PORTADD` | **EXECUTED** (staged, transform 2) | `portadd.stdout.txt`, `portadd.state.txt` |
| `PORTUPDT` | **EXECUTED** (staged, transform 1) | `portupdt.stdout.txt`, `portupdt.state.txt` |
| `PORTDEL` | **EXECUTED** (staged, transform 3) | `portdel.stdout.txt`, `portdel.state.txt` |
| `PORTTRAN` | DERIVED — cannot compile, `PORTREC` absent | `golden/expected/DERIVED.md` |
| `PORTMSTR` | DERIVED — cannot compile, `LS-*`/`ERR-*` undefined | `golden/expected/DERIVED.md` |

### 10.1 Golden input deck

`GOLDGEN` seeds three portfolios (`PORT0001` corporate/active/1250000.00,
`PORT0002` individual/active/45000.50, `PORT0003` trust/**suspended**/**-780000.00**, the
negative value exercising the COMP-3 sign nibble) and writes four input decks: 6 add cases,
5 update cases, 3 delete cases, 9 transaction cases. Nothing is random — there is no seed to
fix, which is a stronger reproducibility guarantee than `TSTGEN00`'s `RANDOM-SEED` file.

### 10.2 Executed results the JS must reproduce

**`PORTADD`** — `added 1, duplicates 2, errors 3`. The single successful create is
`PORT0010`; the two duplicates are the seeded `PORT0001` and `PORT0010` re-offered later in
the same run; the three rejects are blank ID, blank name and status `S`.
Note `PORT-CREATE-DATE` and `PORT-LAST-MAINT` on a created record are set from
`ACCEPT ... FROM DATE YYYYMMDD`, i.e. **the run date**. The harness normalizes both to the
sentinel `@RUNDATE` — it is the one genuinely nondeterministic field in the create path.
`PORT-LAST-TRANS` is *not* touched and stays `00000000` -> `null`.

**`PORTUPDT`** — `updates 4, errors 1`. Three real updates land on `PORT0002`
(status -> `C`, name -> `JANE Q PUBLIC-DOE`, total value -> `99999.99`), the not-found
`PORT9998` is the single error, and the unknown action `X` on `PORT0001` is counted as the
**fourth successful update** while leaving the record byte-identical (defect 3, now
confirmed by execution). The `V` action's alphanumeric-to-numeric `MOVE` of `'99999.99'`
resolves to exactly `99999.99` — captured, not assumed.

**`PORTDEL`** — `deleted 1, not found 2, errors 0`, leaving 2 portfolios. One audit record is
written: `AUD-ACTION` `DELETE`, `AUD-KEY` `PORT00030000000003` (the full 18-byte key),
`AUD-REASON` `01`, and `AUD-STATUS` **`S`** — the deleted portfolio's own `PORT-STATUS`, not
a success/failure flag. `AUD-TIMESTAMP` is wall-clock and normalized out of the diff.

**`PORTREAD`** — lists all 3 seeded portfolios in `PORT-KEY` order and reports
`Total Records Read: 0000003`. Its `Total Value` display is a signed edited field
(`+0000001250000.00`, `-0000000780000.00`), so the sign is always present.
