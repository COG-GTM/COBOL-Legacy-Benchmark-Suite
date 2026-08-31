# Position Valuation and Update — Behavioural Specification

Derived from the COBOL source in this repository, not from the documentation. Where the
documentation and the code disagree, this document follows the code and records the disagreement
as an open question.

Every rule cites the program and paragraph it was read from. Rules marked **[golden]** are also
pinned by a vector captured from the compiled COBOL (see [Parity evidence](#parity-evidence));
rules marked **[read]** were derived from the source text alone.

---

## 1. Scope

### 1.1 Programs in the slice

| Program | Role in the slice | Why it is in scope |
| --- | --- | --- |
| `src/programs/portfolio/PORTTRAN.cbl` | Transaction validation, position update, audit | The whole of the update path lives here |
| `src/programs/portfolio/PORTVALD.cbl` | Field-level validation subroutine | Named in the task; the validation authority for portfolio IDs, accounts, types and amounts |
| `src/programs/portfolio/PORTUPDT.cbl` | Field-level amendment of the portfolio master | The only other program that writes `PORT-TOTAL-VALUE` |
| `src/programs/batch/RPTPOS00.cbl` (¶`2110-FORMAT-POSITION` only) | Position valuation arithmetic | The only place in the system where a position is *valued* rather than accumulated |
| `src/programs/common/ERRPROC.cbl` | Error logging subroutine | Called by `PORTTRAN 9000-ERROR-ROUTINE` on every failure |
| `src/programs/common/AUDPROC.cbl` | Audit logging subroutine | Called by `PORTTRAN 2310-WRITE-AUDIT-RECORD` on every update |

Copybooks: `TRNREC`, `POSREC`, `PORTFLIO`, `PORTVAL`, `AUDITLOG`, `ERRHAND`.

### 1.2 Programs deliberately excluded

`PORTMSTR`, `PORTINQ`, `PORTRPT`, the CICS online programs, the DB2 access layer, `HISTLD00`,
`BCHCTL00` and the remaining 30-odd programs. None of them is on the read/compute/write path for a
position; pulling them in would have made the slice unreviewable.

### 1.3 The starting point named in the task does not exist

`src/programs/batch/POSUPDT.cbl` **is a zero-byte file.** It is not a stub and not a
copybook-driven shell: there is no source in it at all.

```
$ wc -l src/programs/batch/POSUPDT.cbl
0 src/programs/batch/POSUPDT.cbl
```

`documentation/technical/system-architecture.md` (lines 95-97, 502) describes `POSUPDT` as
"Updates position records", reading the Transaction File and writing the Position Master, and
`documentation/technical/data-dictionary.md` (line 275) schedules `POSUPD00` after `TRNVAL00`.
The behaviour the documentation attributes to `POSUPDT` is implemented, paragraph for paragraph,
in `PORTTRAN 2200-UPDATE-POSITIONS` and its subordinates. This specification therefore treats
`PORTTRAN` as the position update step and says so explicitly rather than inventing a program
(open question **OQ-2**).

---

## 2. Record layouts the slice depends on

### 2.1 `TRANSACTION-RECORD` — `src/copybook/common/TRNREC.cpy`

| Field | PIC | Notes |
| --- | --- | --- |
| `TRN-DATE` | `X(08)` | `YYYYMMDD` |
| `TRN-TIME` | `X(06)` | `HHMMSS` |
| `TRN-PORTFOLIO-ID` | `X(08)` | Key into the portfolio master |
| `TRN-SEQUENCE-NO` | `X(06)` | |
| `TRN-INVESTMENT-ID` | `X(10)` | Not used by the update path |
| `TRN-TYPE` | `X(02)` | `BU`, `SL`, `TR`, `FE` |
| `TRN-QUANTITY` | `S9(11)V9(4) COMP-3` | 4 decimal places |
| `TRN-PRICE` | `S9(11)V9(4) COMP-3` | Validated, never used in arithmetic |
| `TRN-AMOUNT` | `S9(13)V9(2) COMP-3` | 2 decimal places; **independent input, not derived** |
| `TRN-CURRENCY` | `X(03)` | Never checked |
| `TRN-STATUS` | `X(01)` | Never checked |
| `TRN-PROCESS-DATE` / `TRN-PROCESS-USER` | `X(26)` / `X(08)` | Audit stamp, never written by this slice |
| `TRN-FILLER` | `X(50)` | Padding; not modelled |

### 2.2 `POSITION-RECORD` — `src/copybook/common/POSREC.cpy`

Key `POS-PORTFOLIO-ID X(08)` + `POS-DATE X(08)` + `POS-INVESTMENT-ID X(10)`; data
`POS-QUANTITY S9(11)V9(4) COMP-3`, `POS-COST-BASIS S9(13)V9(2) COMP-3`,
`POS-MARKET-VALUE S9(13)V9(2) COMP-3`, `POS-CURRENCY X(03)`, `POS-STATUS X(01)` (`A`/`C`/`P`).

Nothing in the slice computes `POS-MARKET-VALUE`. There is no price field on the position record
and no previous-day value, so the position master cannot be revalued from its own contents
(**OQ-5**).

### 2.3 `PORT-RECORD` — `src/copybook/common/PORTFLIO.cpy`

Key `PORT-ID X(8)` + `PORT-ACCOUNT-NO X(10)`; `PORT-CLIENT-NAME X(30)`, `PORT-STATUS X(1)`
(`A`/`C`/`S`), `PORT-TOTAL-VALUE S9(13)V99 COMP-3`, `PORT-CASH-BALANCE S9(13)V99 COMP-3`.

### 2.4 The copybook `PORTTRAN` actually uses does not exist

`PORTTRAN` line 40 is `COPY PORTREC.`, and there is no `PORTREC` copybook anywhere in the
repository — `PORTFLIO` is a different copybook with a different layout, and it has neither
`PORT-TOTAL-UNITS` nor `PORT-TOTAL-COST`, which are the two fields `2210`/`2220`/`2240` update.
**The program as committed does not compile.** The PIC clauses of the two fields that carry all
of the money in this slice are therefore not recoverable from source. This specification assumes
they match their POSREC counterparts, i.e. `PORT-TOTAL-UNITS PIC S9(11)V9(4) COMP-3` and
`PORT-TOTAL-COST PIC S9(13)V9(2) COMP-3`, which are also the widths of the operands added to them
(**OQ-1** — the single assumption that a mainframe owner must confirm, because it fixes where
money truncates).

---

## 3. Numeric semantics (R-5)

These apply to every arithmetic statement in the slice and are the reason the Java port states a
scale and a `RoundingMode` at every store.

| Rule | Statement | Source |
| --- | --- | --- |
| **R-5.1** **[golden]** | Storing into a numeric field keeps exactly the field's decimal places and **truncates toward zero**. No statement in the slice carries `ROUNDED`, so nothing anywhere rounds half-up. `0.999` stored into `S9(13)V9(2)` is `0.99`; `-0.999` is `-0.99`. | `PORTTRAN 2210`–`2240`, `RPTPOS00 2110` (absence of `ROUNDED`) |
| **R-5.2** **[golden]** | Storing a value too large for the field's integer digits **silently discards the high-order digits and keeps the sign**. No statement in the slice carries `ON SIZE ERROR`. `99999999999.9999 + 0.0001` in `S9(11)V9(4)` is `0.0000`, not an error. | `PORTTRAN 2210`–`2240` (absence of `ON SIZE ERROR`) |
| **R-5.3** **[read]** | `COMP-3` versus display storage changes the byte layout, not the result: both are fixed-point decimal. Binary floating point is never equivalent, which is why the port uses `BigDecimal` throughout. | `TRNREC`, `POSREC`, `PORTFLIO` |
| **R-5.4** **[read]** | An alphanumeric field is always its full declared width; a shorter `MOVE` space-fills the remainder. Spaces are not numeric, which is what makes rules R-1.3 and R-2.2 below fail. | ISO COBOL `MOVE`; `PORTVALD 1000`, `2000` |

---

## 4. `PORTVALD` — field validation subroutine

Linkage: `LS-VALIDATE-TYPE X(1)`, `LS-INPUT-VALUE X(50)`, `LS-RETURN-CODE S9(4) COMP`,
`LS-ERROR-MSG X(50)`. Return codes from `PORTVAL.cpy`: `+0` success, `+1` invalid ID, `+2` invalid
account, `+3` invalid type, `+4` invalid amount.

| Rule | Statement | Source |
| --- | --- | --- |
| **R-0.1** **[read]** | The request type selects one of four validations: `I` ID, `A` account, `T` investment type, `M` amount. | `0000-MAIN` |
| **R-0.2** **[golden]** | An unrecognised request type returns **`+1` (`VAL-INVALID-ID`)** with the message `Invalid validation type`. The ID return code is reused for a condition unrelated to IDs, so a caller that switches on the return code alone cannot distinguish a bad ID from a bad request. | `0000-MAIN` `WHEN OTHER` |
| **R-1.1** **[golden]** | An ID whose first four characters are not `PORT` is rejected with `+1` and `Invalid Portfolio ID format`. The comparison is case sensitive: `port0001` fails. | `1000-VALIDATE-ID` |
| **R-1.2** **[read]** | Characters 5-8 are then required to be numeric. | `1000-VALIDATE-ID` |
| **R-1.3** **[golden]** | **The numeric test can never pass.** `MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK` moves 4 characters into a `PIC X(10)` field, space-filling positions 5-10, and `IF VAL-NUMERIC-CHECK IS NOT NUMERIC` is therefore always true (R-5.4). **Every portfolio ID is rejected, including a well-formed `PORT0001`.** `VAL-SUCCESS` is unreachable for request type `I`. | `1000-VALIDATE-ID`, `PORTVAL.cpy` line 43 (**OQ-3**) |
| **R-2.1** **[read]** | The account number must be numeric and must not be all zeros. | `2000-VALIDATE-ACCOUNT` |
| **R-2.2** **[golden]** | The class test is applied to the whole `PIC X(50)` linkage field rather than to the 10 account digits the comment describes, so a 10-digit account number followed by 40 spaces **fails**. Only a 50-digit, non-zero input succeeds. | `2000-VALIDATE-ACCOUNT` (**OQ-4**) |
| **R-3.1** **[golden]** | The investment type must be `STK`, `BND`, `MMF` or `ETF`. The comparison is between `PIC X(50)` and a 3-character literal, which COBOL pads with spaces: trailing spaces are accepted, any other trailing content is rejected, and matching is case sensitive. | `3000-VALIDATE-TYPE` |
| **R-4.1** **[golden]** | Amount validation is **vacuous**. The bounds `VAL-MIN-AMOUNT`/`VAL-MAX-AMOUNT` (±9999999999999.99) are exactly the representable range of the receiving field `VAL-TEMP-NUM PIC S9(13)V99`, so no value that can be moved into it can fall outside them. The paragraph returns `+0` for every input, numeric or not, and `VAL-INVALID-AMT` (+4) is unreachable. | `4000-VALIDATE-AMOUNT`, `PORTVAL.cpy` lines 35-36, 44 |

**Consequence.** Two of the four validations `PORTVALD` offers cannot return success, and a third
cannot return failure. Any caller relying on `PORTVALD` to accept a valid portfolio ID is
currently getting a rejection. `PORTTRAN` does not call `PORTVALD` — it validates inline (§6) —
which is why the defect has survived.

---

## 5. `RPTPOS00 2110-FORMAT-POSITION` — position valuation (R-6)

```cobol
COMPUTE WS-POS-CHANGE-PCT =
    (POS-CURRENT-VALUE - POS-PREVIOUS-VALUE) / POS-PREVIOUS-VALUE * 100
```

Receiving field `WS-POS-CHANGE-PCT PIC +ZZ9.99`: sign always printed, three integer digits, two
decimals, seven characters wide.

| Rule | Statement | Source |
| --- | --- | --- |
| **R-6.1** **[golden]** | Both operands are position values at two decimal places, so a current value of `100.005` is indistinguishable from `100.00`; the quotient is computed at intermediate precision and truncated toward zero to two decimals at the store (no `ROUNDED`). `3/7` reports as `- 57.14`, `7/3` as `+133.33`. | `2110-FORMAT-POSITION` |
| **R-6.2** **[golden]** | The field holds three integer digits and the statement has no `ON SIZE ERROR`, so a change of 1000% or more **loses its high-order digits**: a value going from 100.00 to 12345.67 (+12245.67%) prints as `+245.67`. | `2110-FORMAT-POSITION`, `WS-POSITION-DETAIL` line 74 |
| **R-6.3** **[golden]** | A zero previous value is a **division by zero with no `ON SIZE ERROR`**, whose result the COBOL standard leaves undefined. Under GnuCOBOL the receiving field is left holding its previous content, and because `WS-POS-CHANGE-PCT` is never re-initialised between positions, the report silently repeats the preceding position's percentage. A z/OS run may instead abend S0C7/S0CB. | `2110-FORMAT-POSITION` (**OQ-9**) |
| **R-6.4** **[read]** | `RPTPOS00` references `POS-CURRENT-VALUE`, `POS-PREVIOUS-VALUE` and `POS-DESCRIPTION`, **none of which exist in `POSREC`**, and `TRAN-KEY`, which does not exist in `TRNREC`. Like `PORTTRAN`, the program as committed does not compile. The port assumes both value fields have the width of `POS-MARKET-VALUE`, `S9(13)V9(2)`. | `RPTPOS00` lines 20, 134-140 vs `POSREC.cpy` (**OQ-5**) |

---

## 6. `PORTTRAN 2100` — transaction validation (R-7)

Checks run in sequence and **short-circuit**: `2100` runs the next check only while `ERR-TEXT` is
still spaces, so at most one error is reported per transaction and the first failure hides any
later one (**R-7.6** **[read]**, `2100-VALIDATE-TRANSACTION`).

| Rule | Statement | Source |
| --- | --- | --- |
| **R-7.1** **[read]** | A blank `TRN-PORTFOLIO-ID` fails with `Portfolio ID is required`, before any file access. | `2110-CHECK-PORTFOLIO` |
| **R-7.2** **[read]** | The portfolio must exist on `PORTFILE` (keyed read, `INVALID KEY`). The message is built with `STRING … DELIMITED BY SIZE`, so the ID is appended at its full `PIC X(8)` width, trailing spaces included. | `2110-CHECK-PORTFOLIO` |
| **R-7.3** **[read]** | `TRN-TYPE` must be `BU`, `SL`, `TR` or `FE`; the test is case sensitive and the message embeds the raw 2-character type. | `2120-CHECK-TRANSACTION-TYPE` |
| **R-7.4** **[read]** | `TRN-QUANTITY`, `TRN-PRICE` and `TRN-AMOUNT` must each be **strictly greater than zero**. Comparisons are against zero, so negatives fail the same test. | `2130-CHECK-AMOUNTS` |
| **R-7.5** **[read]** | Transfers are exempt from the price and amount checks but **not** from the quantity check: `IF TRN-QUANTITY <= ZERO` has no `AND TRN-TYPE NOT = 'TR'` clause, while the other two do. A transfer with zero price and zero amount is accepted; a transfer with zero quantity is rejected. | `2130-CHECK-AMOUNTS` lines 152-164 (**OQ-7**) |
| **R-7.7** **[read]** | Because `TRN-QUANTITY` holds 4 decimals (R-5.1), a quantity below `0.0001` truncates to zero on input and is rejected by R-7.4. | `TRNREC`, `2130-CHECK-AMOUNTS` |
| **R-7.8** **[read]** | Nothing checks that `TRN-AMOUNT` equals `TRN-QUANTITY × TRN-PRICE`. Price is validated and then never used in any computation. | `2130`, `2210`–`2240` (**OQ-6**) |

---

## 7. `PORTTRAN 2200` — position update (R-8)

| Rule | Statement | Source |
| --- | --- | --- |
| **R-8.1** **[read]** | **Buy**: re-read the portfolio by key, `ADD TRN-QUANTITY TO PORT-TOTAL-UNITS`, `ADD TRN-AMOUNT TO PORT-TOTAL-COST`, `REWRITE`. | `2210-PROCESS-BUY` |
| **R-8.2** **[read]** | **Sell**: `SUBTRACT TRN-QUANTITY FROM PORT-TOTAL-UNITS` and `SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST`. The cost basis is reduced by the **sale proceeds**, not by the cost of the units sold, so a profitable sale reduces cost faster than units. No realised gain or loss is computed anywhere. | `2220-PROCESS-SELL` (**OQ-6**) |
| **R-8.3** **[read]** | A sell is refused with `Insufficient units for sale` when `PORT-TOTAL-UNITS < TRN-QUANTITY`; nothing is written. | `2220-PROCESS-SELL` |
| **R-8.4** **[read]** | Nothing guards the cost basis: a large enough sale or fee drives `PORT-TOTAL-COST` negative, and that record is rewritten. | `2220`, `2240` |
| **R-8.5** **[read]** | **Fee**: `SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST` only; units untouched. | `2240-PROCESS-FEE` |
| **R-8.6** **[read]** | **Transfer**: always fails with `Transfer processing not implemented`. Transfers pass validation (R-7.5) and then always fail here — every `TR` record in the file becomes an error. | `2230-PROCESS-TRANSFER` |
| **R-8.7** **[read]** | A portfolio missing at update time fails with `Portfolio not found for update` (buy, sell) or `Portfolio not found for fee` (fee). | `2210`, `2220`, `2240` |
| **R-8.8** **[read]** | No `ADD`/`SUBTRACT` carries `ON SIZE ERROR`, so a total that outgrows its field wraps silently (R-5.2) rather than failing the transaction. | `2210`–`2240` |
| **R-8.9** **[read]** | A transaction type outside the four codes falls through the `EVALUATE` with no `WHEN OTHER`: nothing is updated and no error is raised. | `2200-UPDATE-POSITIONS` |

---

## 8. `PORTTRAN 2300` — audit trail (R-9)

| Rule | Statement | Source |
| --- | --- | --- |
| **R-9.1** **[read]** | An audit record is written **after every update attempt**, successful or not, with `AUD-TYPE = 'TRAN'` and `AUD-PROGRAM = 'PORTTRAN'`. The action mapping is not the obvious one: buy → `CREATE`, sell → `DELETE`, transfer and fee → `UPDATE`. | `2300-UPDATE-AUDIT-TRAIL` |
| **R-9.2** **[read]** | `AUD-STATUS` is derived from `WS-PORT-STATUS`, the **file** status, not from `ERR-TEXT`. A failed transfer is therefore audited as `SUCC`, because the last portfolio file operation succeeded. | `2300` lines 268-272 |
| **R-9.3** **[read]** | For an unknown transaction type the action `EVALUATE` also has no `WHEN OTHER`, so `AUD-ACTION` keeps its `INITIALIZE` value (spaces) and an audit record is still written. | `2300` lines 257-266 |
| **R-9.4** **[read]** | `AUD-BEFORE-IMAGE` is taken from `PORT-RECORD` **after** the update has been rewritten, so the "before" image is in fact the after image. The audit trail cannot reconstruct a prior state. | `2300` line 278 (**OQ-8**) |
| **R-9.5** **[read]** | `2310` calls `AUDPROC` passing `AUDIT-RECORD` (the `AUDITLOG` layout) where `AUDPROC` declares `LS-AUDIT-REQUEST` (a different layout), then tests the special register `RETURN-CODE`, which `AUDPROC` never sets — it sets `LS-RETURN-CODE`. The audit-failure path is unreachable and the record written is misaligned. | `2310-WRITE-AUDIT-RECORD`, `AUDPROC 2000-PROCESS-AUDIT` (**OQ-8**) |

---

## 9. `PORTTRAN` driver and error handling (R-10)

| Rule | Statement | Source |
| --- | --- | --- |
| **R-10.1** **[read]** | **`PORTTRAN` never performs `2200-UPDATE-POSITIONS`.** `2000-PROCESS-TRANSACTIONS` reads a record and performs `2100-VALIDATE-TRANSACTION`; no `PERFORM 2200-UPDATE-POSITIONS` statement exists anywhere in the program. As committed, the program validates a transaction file, counts results, and **no portfolio balance ever changes** — the buy, sell, transfer, fee and audit paragraphs are all dead code. | `PORTTRAN` lines 92-118 vs 167-180 (**OQ-2**) |
| **R-10.2** **[read]** | `WS-PROCESS-COUNT` counts transactions that passed validation, not transactions applied. | `2100`, `3000-TERMINATE` |
| **R-10.3** **[read]** | Every failure increments `WS-ERROR-COUNT` and calls `ERRPROC` with category `PR` and program `PORTTRAN`. `9000-ERROR-ROUTINE` sets neither `ERR-CODE` nor `ERR-SEVERITY`, so both are logged at their initial values. | `9000-ERROR-ROUTINE`, `ERRHAND.cpy` |
| **R-10.4** **[read]** | The driver loop runs `UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100`. The test is evaluated before each read, so the run stops **after the 101st error**, leaving the rest of the transaction file unread and unprocessed. This is a silent partial run: the job does not abend and the return code is not set. | `0000-MAIN` lines 64-66 (**OQ-12**) |
| **R-10.5** **[read]** | Exactly 100 errors do not stop the run. | `0000-MAIN` |
| **R-10.6** **[read]** | If the transaction file fails to open, the program logs an error, skips the loop entirely and terminates normally. A failed open of the portfolio file does not stop processing at all. | `1000-INITIALIZE`, `0000-MAIN` line 63 |

---

## 10. `PORTUPDT` — field amendment (R-11)

Reads a sequential amendment file (`UPDT-ID X(8)` + `UPDT-ACCT-NO X(10)`, `UPDT-ACTION X(1)`,
`UPDT-NEW-VALUE X(50)`) and applies one field per record to the portfolio master.

| Rule | Statement | Source |
| --- | --- | --- |
| **R-11.1** **[read]** | Actions: `S` → `PORT-STATUS`, `N` → `PORT-CLIENT-NAME`, `V` → `PORT-TOTAL-VALUE`. | `2200-APPLY-UPDATE` |
| **R-11.2** **[read]** | `S` moves a `PIC X(50)` field into `PORT-STATUS PIC X(1)`, keeping **only the first character**, with no validation against the `88`-levels `A`/`C`/`S`. `CLOSED` sets status `C` by accident; `SUSPEND` sets `S`; `XYZ` sets `X`, a value no other program recognises. | `2200-APPLY-UPDATE` line 134, `PORTFLIO.cpy` (**OQ-10**) |
| **R-11.3** **[golden]** | `V` performs `MOVE UPDT-NEW-VALUE TO WS-NUMERIC-WORK` (`PIC X(50)` → `PIC S9(13)V99`) and then stores that into `PORT-TOTAL-VALUE`. **Alphanumeric-to-numeric `MOVE` is the one construct in this slice whose result is not portable.** Measured under GnuCOBOL, leading/trailing spaces are ignored, an embedded decimal point is honoured, and the value is truncated to the receiving field: `"12500.00"` → 12,500.00, `"999999999999999"` → 9,999,999,999,999.00. IBM Enterprise COBOL specifies the sending item be treated as an unsigned integer aligned on the rightmost digit, with no decimal-point handling, which gives a **different amount of money** for `"12500.00"`. | `2200-APPLY-UPDATE` lines 138-139 (**OQ-11**) |
| **R-11.4** **[read]** | A non-numeric amendment value has no defined result under either compiler. The port refuses it rather than inventing one. | `2200-APPLY-UPDATE` (**OQ-11**) |
| **R-11.5** **[read]** | An action outside `S`/`V`/`N` falls through the `EVALUATE` with no `WHEN OTHER`: the record is rewritten **unchanged** and counted as a successful update. | `2200-APPLY-UPDATE` |
| **R-11.6** **[read]** | A missing portfolio record increments the error count and is displayed, not logged through `ERRPROC`. Unlike `PORTTRAN` there is no error ceiling: the run continues to end of file however many records fail. | `2100-PROCESS-UPDATE` |
| **R-11.7** **[read]** | `PORTUPDT` writes `PORT-TOTAL-VALUE`, while `PORTTRAN` writes `PORT-TOTAL-UNITS`/`PORT-TOTAL-COST`. These are different fields on the same record and nothing reconciles them. | `PORTUPDT 2200`, `PORTTRAN 2210` |

---

## 11. Rounding and precision decisions in the port

| Decision | Justification |
| --- | --- |
| `BigDecimal` everywhere, never `double` | `COMP-3` is fixed-point decimal; binary floating point cannot represent `0.01` and would diverge on the first fee. |
| Scale 4 for quantities, scale 2 for money | `TRN-QUANTITY`/`POS-QUANTITY` are `V9(4)`; `TRN-AMOUNT`/`POS-COST-BASIS`/`PORT-TOTAL-VALUE` are `V9(2)`. |
| `RoundingMode.DOWN` at every store | R-5.1: no statement in the slice carries `ROUNDED`, and COBOL truncation is toward zero. |
| Wrap modulo 10^intDigits on overflow, keeping the sign | R-5.2: no statement carries `ON SIZE ERROR`; packed-decimal high-order truncation leaves the sign nibble untouched. |
| Division at `MathContext.DECIMAL128`, truncated once at the store | COBOL holds `COMPUTE` intermediates at implementation-defined high precision (IBM: ≥30 digits) and truncates only into the receiving field. Every division here is followed by a store at 2 decimals, so any intermediate width beyond ~20 digits gives identical results. |
| Operands truncated to the receiving field's scale **before** the operation | The operands are themselves fields of that width; `100.005` cannot exist in an `S9(13)V9(2)` item. |

---

## 12. Parity evidence

The expected values in the parity tests are not hand-computed. `modernization/position-valuation/parity/`
contains four COBOL harnesses compiled with GnuCOBOL against the unmodified programs and the
production `PIC` clauses:

| Harness | Exercises | Golden file |
| --- | --- | --- |
| `PVDRIVE.cbl` | Calls the unmodified `PORTVALD` with 34 inputs | `portvald-golden.csv` |
| `PARITHM.cbl` | The `ADD`/`SUBTRACT`/`COMPUTE` statements of `PORTTRAN 2210`–`2240` and `RPTPOS00 2110` | `arithmetic-golden.csv` |
| `PDIVZER.cbl` | The zero-divisor case of `RPTPOS00 2110` | `arithmetic-golden.csv` |
| `PUPDMOV.cbl` | The two `MOVE`s of the `V` branch of `PORTUPDT 2200` | `portupdt-move-golden.csv` |

`parity/generate-golden-vectors.sh` regenerates all three files. The Java tests read them from
`src/test/resources/parity/` and assert row by row, so a divergence between the port and the
COBOL fails the build.

---

## 13. Open questions

These need a human answer before anything is cut over. They are not gaps in the analysis; they are
places where the COBOL does not determine the answer.

| # | Question | Why it matters | Current port behaviour |
| --- | --- | --- | --- |
| **OQ-1** | What are the real PIC clauses of `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST`? The `PORTREC` copybook `PORTTRAN` copies is not in the repository. | Fixes where every buy/sell/fee truncates and where it wraps. | Assumes `S9(11)V9(4) COMP-3` and `S9(13)V9(2) COMP-3`, matching POSREC and the operands. |
| **OQ-2** | Is `PORTTRAN`'s missing `PERFORM 2200-UPDATE-POSITIONS` a bug, or does the real `POSUPDT` (empty here) do the updating? | Decides whether the modern service should apply updates at all. | Both: `clbs.porttran.apply-updates=false` (default) reproduces the COBOL exactly; `true` runs the dead paragraphs. |
| **OQ-3** | Should `1000-VALIDATE-ID` accept a well-formed ID (R-1.3)? | Today the validator rejects every portfolio ID ever passed to it. | Defect reproduced, pinned by a golden vector. |
| **OQ-4** | Should `2000-VALIDATE-ACCOUNT` test 10 digits rather than 50 (R-2.2)? | Today only a 50-digit account passes. | Defect reproduced. |
| **OQ-5** | Where do `POS-CURRENT-VALUE` and `POS-PREVIOUS-VALUE` come from? They are not in `POSREC`, and nothing computes `POS-MARKET-VALUE`. | Without them there is no valuation input at all; the report cannot run. | Assumes both have the width of `POS-MARKET-VALUE`. |
| **OQ-6** | Should a sell reduce cost basis by proceeds (current) or by the cost of the units sold, and where is realised P&L computed? | Cost basis and every downstream P&L number are wrong if the current behaviour is unintended. | Current behaviour reproduced. |
| **OQ-7** | Is the transfer exemption in `2130` meant to cover quantity as well as price and amount (R-7.5)? | Determines whether zero-quantity transfers should be accepted. | Asymmetry reproduced. |
| **OQ-8** | Should the audit record carry a real before-image and a real timestamp (R-9.4, R-9.5)? | The audit trail is currently unusable for reconstruction and its failure path is unreachable. | Timestamp and images are written correctly in the port; the two defects are documented, not reproduced, because reproducing them corrupts the record. |
| **OQ-9** | On z/OS, does the zero-divisor `COMPUTE` in `RPTPOS00 2110` abend (S0C7/S0CB) or leave the field unchanged (R-6.3)? | One prints a stale percentage for the position; the other kills the report job. | Flagged as a size error, no value produced; the caller decides. |
| **OQ-10** | Should `PORTUPDT` validate `PORT-STATUS` against `A`/`C`/`S` (R-11.2)? | It can currently write a status no other program recognises. | Behaviour reproduced. |
| **OQ-11** | Do production amendment files contain a decimal point in `UPDT-NEW-VALUE` (R-11.3)? | GnuCOBOL and IBM Enterprise COBOL give **different amounts** for `"12500.00"`. | GnuCOBOL semantics, pinned by a golden vector; non-numeric input is refused rather than guessed. |
| **OQ-12** | On hitting 101 errors, should the job abend rather than terminate normally with the file half-read (R-10.4)? | A silent partial run is indistinguishable from a clean one in the return code. | Reproduced; the result object exposes `haltedOnErrorLimit` so a caller can fail the job. |
