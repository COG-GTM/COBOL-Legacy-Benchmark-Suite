# Translation Notes - COBOL to Java

Living record of every mapping decision and every source discrepancy found while translating the
COBOL Legacy Benchmark Suite to Java. Each translated slice appends to this file; nothing here is
rewritten silently.

| Slice                                        | Status | Covers                                                              |
| -------------------------------------------- | ------ | ------------------------------------------------------------------- |
| Phase 0 - shared foundation                   | Landed | `TRNREC`, `POSREC`, `PORTFLIO`, `ERRHAND`, `AUDITLOG`, `AUDPROC`/`ERRPROC` contracts, test harness |
| Child 1 - `PORTTRAN.cbl`                      | Landed | `PortfolioTransactionProcessor`, `PortfolioRepository`, `TransactionSource` |
| Child 2 - portfolio CRUD                      | Open   | `PORTMSTR`, `PORTADD`, `PORTUPDT`, `PORTDEL`, `PORTREAD`             |
| Child 3 - batch pipeline                      | Open   | `HISTLD00`, transaction validation                                   |
| Child 4 - reporting                           | Open   | `RPTPOS00`, `RPTAUD00`, `RPTSTA00`                                   |

## 0. Why this reads oddly in places

The COBOL in this repository was generated as a translation benchmark and **was never compiled or
run on z/OS**. It contains copybooks that do not exist, paragraphs no one performs, and layouts that
disagree with the documentation describing them. The translation reproduces the behaviour that the
source specifies, including its defects, and records each defect here rather than quietly repairing
it. Where the source cannot be reproduced at all (a copybook that does not exist), the substitution
is spelled out below and marked in the Javadoc of the affected class.

Because the original cannot be executed, there is no reference output to diff against. The oracle is
the documented behaviour in `documentation/technical/data-dictionary.md` and
`documentation/operations/test-data-specs.md`, encoded as tests in
`src/test/java/com/clbs/portfolio/harness/DocumentedRulesTest.java`.

## 1. Layout and build

```
modernized/java/
  pom.xml                    Maven build; Java 11; JUnit 5
  model/                     translated copybooks   (package com.clbs.portfolio.model)
  service/                   translated programs and subroutine contracts
                                                    (package com.clbs.portfolio.service)
  src/test/java/             test harness and tests
  TRANSLATION-NOTES.md       this file
```

`mvn -f modernized/java/pom.xml test` builds and runs everything.

The two source roots are declared explicitly in `pom.xml` (`sourceDirectory` plus
`build-helper-maven-plugin`) because the directory names come from the translation plan while the
package names follow Java convention; the compiler is handed the roots rather than inferring
packages from directory depth.

## 2. Global mapping conventions

| COBOL                                | Java                                                        | Notes |
| ------------------------------------ | ----------------------------------------------------------- | ----- |
| `PIC S9(11)V9(4) COMP-3`             | `BigDecimal`, scale 4                                        | `CobolDecimal.quantity` |
| `PIC S9(13)V9(2) COMP-3`, `S9(13)V99`| `BigDecimal`, scale 2                                        | `CobolDecimal.amount` |
| `PIC 9(8)` (display)                 | `int`, sign dropped, high-order truncated                    | `CobolText.pic9` |
| `PIC S9(4) COMP`                     | `int`                                                        | severity fields |
| `PIC X(n)`                           | `String` stored space-padded to `n`, truncated beyond it     | `CobolText.picX` |
| level-88 condition set                | `enum` with `code()` / `fromCode()`                          | see below |
| group item (`TRN-KEY`)               | derived getter returning the concatenated children           | `getTrnKey()` |
| record area (`01` under `FD`)        | mutable class with a copy constructor                        | reads reuse one buffer |
| `INITIALIZE`                         | `initialize()` where a program performs it                   | `AuditRecord` |
| `CALL 'SUBPROG' USING ...`           | interface in `service/`, return code as the method result    | section 5 |

Three conventions are load-bearing and worth stating outright:

**No `double` or `float` anywhere.** Every numeric field that has decimals is `BigDecimal` with the
scale fixed by its picture clause, enforced in the setter, so a value can never be stored at the
wrong precision.

**Truncation, not rounding.** No statement in the translated slice says `ROUNDED` and none has an
`ON SIZE ERROR` clause, so `CobolDecimal.store` truncates excess decimals toward zero and drops
integer digits that do not fit - silently, as the mainframe would. `CobolDecimalTest` pins both.

**Coded fields keep their raw bytes.** `TRN-TYPE` is a two-byte buffer, not an enumeration: the whole
point of `2120-CHECK-TRANSACTION-TYPE` is that it can hold something else, and the error message
echoes whatever it holds. So `TransactionRecord.getTrnType()` returns the raw characters and
`getTransactionType()` returns the enum interpretation, `null` when no level-88 matches. The same
split applies to `TRN-STATUS`, `POS-STATUS`, `PORT-STATUS`, `PORT-CLIENT-TYPE`, `AUD-TYPE`,
`AUD-ACTION`, `AUD-STATUS` and `ERR-CATEGORY`.

## 3. Copybook mappings

### 3.1 `TRNREC.cpy` -> `TransactionRecord`

| COBOL field | Picture | Java |
| ----------- | ------- | ---- |
| `TRN-DATE` / `TRN-TIME` / `TRN-PORTFOLIO-ID` / `TRN-SEQUENCE-NO` | `X(8)` / `X(6)` / `X(8)` / `X(6)` | `String`, padded; `getTrnKey()` returns the 28-byte key |
| `TRN-INVESTMENT-ID` | `X(10)` | `String` |
| `TRN-TYPE` + 4 level-88s | `X(2)` | raw `String` + `TransactionType` |
| `TRN-QUANTITY` | `S9(11)V9(4) COMP-3` | `BigDecimal` scale 4 |
| `TRN-PRICE` | `S9(11)V9(4) COMP-3` | `BigDecimal` scale 4 |
| `TRN-AMOUNT` | `S9(13)V9(2) COMP-3` | `BigDecimal` scale 2 |
| `TRN-CURRENCY` | `X(3)` | `String` |
| `TRN-STATUS` + 4 level-88s | `X(1)` | raw `String` + `TransactionStatus` |
| `TRN-PROCESS-DATE` / `TRN-PROCESS-USER` / `TRN-FILLER` | `X(26)` / `X(8)` / `X(50)` | `String` |

### 3.2 `POSREC.cpy` -> `PositionRecord`

`POS-QUANTITY` scale 4; `POS-COST-BASIS` and `POS-MARKET-VALUE` scale 2; `POS-STATUS` maps to
`PositionStatus` (`A`/`C`/`P`); `getPosKey()` returns the 26-byte composite key.

### 3.3 `PORTFLIO.cpy` -> `PortfolioRecord`

`PORT-ID` (`X(8)`) is the VSAM record key. `PORT-TOTAL-VALUE` and `PORT-CASH-BALANCE` are scale 2.
`PORT-CREATE-DATE`, `PORT-LAST-MAINT` and `PORT-LAST-TRANS` are unsigned `PIC 9(8)` display fields
mapped to `int`. `toRecordImage()` renders the `01` buffer as characters for group moves such as
`MOVE PORT-RECORD TO AUD-BEFORE-IMAGE`; packed fields have no text form, so they are rendered as a
sign plus their unscaled digits and the image is an approximation by construction.

The class also carries two fields that **no copybook defines** - see G1.

### 3.4 `ERRHAND.cpy` -> `ErrorMessage`, `ErrorCategory`, `ErrorSeverity`, `VsamStatus`

The copybook is a set of constant tables plus the `ERR-MESSAGE` area. Constants become enums
(`ERR-CATEGORIES` -> `ErrorCategory`, `ERR-RETURN-CODES` -> `ErrorSeverity`, `ERR-VSAM-STATUSES` +
`ERR-VSAM-MSGS` -> `VsamStatus`), and the area becomes `ErrorMessage`.

`ERR-TEXT` is the program's error flag, not just a message: `PORTTRAN` decides what to do next with
`IF ERR-TEXT = SPACES`. It is therefore stored padded to its full 80 characters and read through
`isErrTextSpaces()`, with `clearErrText()` for `MOVE SPACES TO ERR-TEXT`. `getErrTextTrimmed()`
exists for assertions and logging only.

### 3.5 `AUDITLOG.cpy` -> `AuditRecord`, `AuditType`, `AuditAction`, `AuditStatus`

Straight field-for-field mapping. `AUD-ACTION` values are spelled to eight bytes in the copybook
(`VALUE 'CREATE  '`); `AuditAction.code()` returns the padded form so buffer comparisons match.

### 3.6 `documentation/technical/data-dictionary.md` section 6 -> `ErrorCode`

The documented error catalogue (`E001`-`E004`, `W001`-`W002`) is modelled so the harness can assert
against it. No translated program populates `ERR-CODE` - see G6.

## 4. Gotchas in the source

### G1 - `PORTTRAN` copies a copybook that does not exist

`PORTTRAN.cbl:40` has `COPY PORTREC` for the `PORTFOLIO-FILE` record area, and
`2210-PROCESS-BUY` / `2220-PROCESS-SELL` / `2240-PROCESS-FEE` update `PORT-TOTAL-UNITS` and
`PORT-TOTAL-COST`. There is no `PORTREC.cpy` anywhere in the repository. The real portfolio layout is
`src/copybook/common/PORTFLIO.cpy`, whose `PORT-FINANCIAL-INFO` group holds `PORT-TOTAL-VALUE` and
`PORT-CASH-BALANCE` - neither a unit count nor a cost basis. The program also references `PORT-ID`
and `PORT-ACCOUNT-NO`, which *are* `PORTFLIO` fields, so `PORTREC` was clearly meant to be a superset
of `PORTFLIO` rather than a different record.

**Decision.** `PortfolioRecord` translates `PORTFLIO.cpy` faithfully and adds two synthetic fields
for the two the program needs, typed from their closest documented equivalents in `POSREC.cpy`:

| Missing COBOL field | Java | Type taken from | Rationale |
| ------------------- | ---- | --------------- | --------- |
| `PORT-TOTAL-UNITS`  | `portTotalUnits` | `POS-QUANTITY` `PIC S9(11)V9(4) COMP-3` | It is compared against and updated by `TRN-QUANTITY`, which is `S9(11)V9(4)`; a holdings count is a quantity |
| `PORT-TOTAL-COST`   | `portTotalCost`  | `POS-COST-BASIS` `PIC S9(13)V9(2) COMP-3` | It is updated by `TRN-AMOUNT`, which is `S9(13)V9(2)`; "cost" against a portfolio is a cost basis |

Rejected alternative: reusing `PORT-TOTAL-VALUE` for `PORT-TOTAL-COST`. Market value and cost basis
are different quantities, and a fee subtracting from market value would be a different bug from the
one the source has.

The synthetic fields are excluded from `toRecordImage()` so that the audit before-image stays a
faithful picture of the `PORTFLIO` layout, and they are marked as synthetic in the class Javadoc.

### G2 - the position-update logic is dead code

`0000-MAIN` performs `2000-PROCESS-TRANSACTIONS` until end of file, and that paragraph reads a
record and performs `2100-VALIDATE-TRANSACTION` - nothing else. `2200-UPDATE-POSITIONS` and its
children `2210-PROCESS-BUY`, `2220-PROCESS-SELL`, `2230-PROCESS-TRANSFER`, `2240-PROCESS-FEE`, and
therefore `2300-UPDATE-AUDIT-TRAIL` and `2310-WRITE-AUDIT-RECORD`, are **never performed**. As
written, `PORTTRAN` validates and counts transactions, writes no portfolio updates and writes no
audit records.

**Decision.** The translated main flow must reproduce that: processing a transaction validates it and
nothing more. The update paragraphs are still translated, as methods that are not called from the
main loop, so the logic is captured and testable. Child 1 owns this and states it in the class
Javadoc of `PortfolioTransactionProcessor`.

### G3 - transfers are unimplemented

`2230-PROCESS-TRANSFER` moves `'Transfer processing not implemented'` to `ERR-TEXT` and calls the
error routine. That is the behaviour, and the translation reproduces it verbatim, error text
included. Note the interaction with `2130-CHECK-AMOUNTS`, which exempts `TR` from the price and
amount checks: a transfer is the only type that can validate with a zero price and a zero amount, and
then fails at update time - if update time were ever reached (G2).

### G4 - the subroutine linkage areas do not match what the callers pass

`PORTTRAN` calls `AUDPROC` with `AUDIT-RECORD` and `ERRPROC` with `ERR-MESSAGE`, but neither
subroutine declares that layout:

- `AUDPROC`'s `LS-AUDIT-REQUEST` begins with `LS-SYSTEM-INFO` and ends with `LS-RETURN-CODE`, while
  `AUDIT-RECORD` begins with a 26-byte `AUD-TIMESTAMP` and has no return-code field. Byte for byte,
  `AUDPROC` would read the caller's timestamp as system id, user id, program and terminal, and would
  write its return code past the end of the caller's record.
- `ERRPROC`'s `LS-ERROR-REQUEST` begins with `LS-PROGRAM-ID`, while `ERR-MESSAGE` begins with an
  18-byte `ERR-TIMESTAMP`; the two are offset by that timestamp.

**Decision.** The `service/` interfaces pass the typed record and return the status as the method
result, which reproduces the *intent* of both subroutines without reproducing a storage overlay that
would corrupt data. The mismatch is recorded in the Javadoc of `AuditProcessor` and `ErrorProcessor`.

Related: `2310-WRITE-AUDIT-RECORD` tests the `RETURN-CODE` special register after the call, but
`AUDPROC` only sets its linkage field and never `RETURN-CODE`. On z/OS the test would read whatever
the register happened to hold. The translation checks the value the subroutine reports.

### G5 - the documentation describes a different system

`documentation/technical/data-dictionary.md` documents an account-and-fund system
(`TR-ACCOUNT-NO PIC 9(09)`, `TR-FUND-ID`, transaction types `BY`/`SL`/`FE`, share quantities with
three decimals) while the copybooks the programs actually copy are portfolio-and-investment based
(`TRN-PORTFOLIO-ID X(08)`, `TRN-INVESTMENT-ID X(10)`, types `BU`/`SL`/`TR`/`FE`, quantities with
four decimals). `documentation/operations/test-data-specs.md` disagrees with both in three more
specific ways:

| Documented | Copybook | Handling |
| ---------- | -------- | -------- |
| `PORT-ID` is `PORT` plus five digits (9 chars) | `PORT-ID PIC X(8)` | The 9-character id truncates to `PORT0000`; the harness uses 8-character ids and `DocumentedRulesTest` pins the truncation |
| transaction types `B` / `S` | `TRN-TYPE X(2)` with `BU`/`SL`/`TR`/`FE` | Copybook codes are authoritative; `B` and `S` are invalid |
| portfolio status `A` / `I` / `C` | level-88s `A` (active), `C` (closed), `S` (suspended) | Copybook is authoritative; `I` maps to no condition |

**Decision.** Copybooks win for anything a program reads or writes; documentation wins for validation
rules, value ranges and the error catalogue, which no copybook states. Every disagreement above has a
test so it stays visible.

### G6 - errors are logged without a code or a severity

`9000-ERROR-ROUTINE` sets `ERR-CATEGORY` to `PR` and `ERR-PROGRAM` to `PORTTRAN`, and the caller has
already set `ERR-TEXT`. `ERR-CODE` and `ERR-SEVERITY` are never assigned, so every error reaches
`ERRPROC` with a blank code and severity zero - and `ERRPROC` copies that zero into its return code,
reporting "successful completion" for an error. Faithfully reproduced; the documented catalogue is
carried separately as `ErrorCode` for the harness.

### G7 - the audit status is decided by a stale file status

`2300-UPDATE-AUDIT-TRAIL` writes `SUCC` or `FAIL` based on `WS-PORT-STATUS`, the status of the last
`PORTFOLIO-FILE` operation. On the buy and sell paths the last operation is the `REWRITE`, so the
check is meaningful; on the transfer path (G3) no file operation happens at all, so the status is
whatever the previous transaction left behind. Reproduced as written.

### G8 - `STRING` over packed fields

`2300-UPDATE-AUDIT-TRAIL` builds `AUD-MESSAGE` with
`STRING 'Transaction: ' TRN-TYPE ' Amount: ' TRN-AMOUNT ' Units: ' TRN-QUANTITY`, where `TRN-AMOUNT`
and `TRN-QUANTITY` are `COMP-3`. IBM Enterprise COBOL rejects a packed sender in `STRING`
(the sender must be alphanumeric or an integer display item), so this statement would not compile as
written - one more sign the source was never built. The translation renders the packed fields the way
`CobolDecimal.image` does, sign plus unscaled digits, which is the closest readable equivalent of the
bytes the statement was reaching for. Child 1 records the exact rendering it uses.

### G9 - the portfolio record area has two different names

`PORTTRAN` writes the record area back with `REWRITE PORTFOLIO-RECORD` (`2210`, `2220`, `2240`) but
copies it with `MOVE PORT-RECORD TO AUD-BEFORE-IMAGE` (`2300`). `PORTFLIO.cpy` declares
`01 PORT-RECORD` and nothing anywhere declares `PORTFOLIO-RECORD`; both names must have come from
the `PORTREC` copybook that does not exist (G1), and only one `01` can sit under the `FD`.

**Decision.** One record area, `PortfolioTransactionProcessor.getPortfolioRecord()`. Both statements
address it, so the rewrite writes back every field the read delivered - not only the two the
paragraph changed. Pinned by `Updates.rewriteWritesBackTheWholeRecordArea`.

### G10 - `FUNCTION USER-ID` is not an IBM intrinsic function

`2300-UPDATE-AUDIT-TRAIL` has `MOVE FUNCTION USER-ID TO AUD-USER-ID`. `USER-ID` is not in the
intrinsic-function set of IBM Enterprise COBOL for z/OS (nor in the ISO standard); it is a GnuCOBOL
extension. A z/OS program obtains the user id from the environment - `EXEC CICS ASSIGN USERID` or
the security manager - not from a function call, so this statement is a third thing that would not
have compiled.

**Decision.** The value is supplied to the constructor rather than invented, defaulting to the
`user.name` system property, and is stored in the eight bytes of `AUD-USER-ID` like any other
`MOVE`. `FUNCTION CURRENT-DATE` on the line above is a real intrinsic and is rendered as its 21
characters `YYYYMMDDhhmmssnn±hhmm` from an injectable `Clock`, which `MOVE` pads to the 26 bytes of
`AUD-TIMESTAMP`. Pinned by `ControlFlow.userIdIsSupplied` and
`AuditTrail.successfulUpdateIsAuditedSucc`.

### G11 - an unrecognised transaction type is audited as if something had happened

The `EVALUATE TRN-TYPE` in `2200-UPDATE-POSITIONS` has no `WHEN OTHER`, and neither does the one in
`2300-UPDATE-AUDIT-TRAIL` that sets `AUD-ACTION`. A type that matches none of the four codes
therefore updates nothing, falls straight through to the audit trail and writes a record whose
action is still the spaces left by `INITIALIZE AUDIT-RECORD` - and whose status is `SUCC` whenever
the previous file operation happened to succeed (G7). Only reachable by calling the paragraph, since
nothing performs it (G2). Reproduced as written; pinned by `Updates.unrecognisedTypeStillAudits`.

### G12 - the "before image" is captured after the update

`2300-UPDATE-AUDIT-TRAIL` is performed at the end of `2200-UPDATE-POSITIONS`, by which point
`2210`/`2220`/`2240` have already changed the record area and rewritten it. The comment above the
statement says `Store original portfolio state`, but `AUD-BEFORE-IMAGE` receives the post-update
record; there is no saved copy of the original anywhere in the program, and `AUD-AFTER-IMAGE` is
never populated at all.

**Decision.** Reproduced: the image is taken from the record area at the point the statement runs.
It has a quiet interaction with G1 - the only fields the update paragraphs change are the two
synthetic ones, which `toRecordImage()` excludes, so in the translation the image is byte-identical
before and after a buy. Pinned by `AuditTrail.beforeImageIsTakenAfterTheUpdate`.

### G13 - a portfolio file that will not open does not stop the run

`1000-INITIALIZE` opens both files and logs an error for each open that fails, but `0000-MAIN` only
tests `WS-TRAN-STATUS`. A failed `OPEN I-O PORTFOLIO-FILE` is therefore logged and then ignored: the
loop runs, and every `READ PORTFOLIO-FILE` against an unopened file would fail with status `47`, so
`2110-CHECK-PORTFOLIO` would reject every transaction as an invalid portfolio id. `3000-TERMINATE`
then closes both files unconditionally, including one that never opened.

**Decision.** Reproduced: the translated `initialize()` logs and continues, and `terminate()` closes
both. What a read against an unopened file does is left to the `PortfolioRepository` implementation,
since that is a property of the file system rather than of the program. Pinned by
`ControlFlow.portfolioFileOpenFailure` and `ControlFlow.filesAreClosedAfterAFailedOpen`.

### G14 - the error cutoff stops at 101 errors, and open failures count towards it

`PERFORM 2000-PROCESS-TRANSACTIONS UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100` is a test-before loop,
so the count is inspected before each read: an iteration is entered on a count of exactly 100 and
the loop ends once an iteration has pushed the count past it. The run therefore stops after the
101st error, with 101 records read and the rest of the file unprocessed and unreported - the program
neither says it stopped early nor returns a non-zero code. `WS-ERROR-COUNT` is also the counter
`1000-INITIALIZE` increments for a failed open, so an open failure spends part of the same budget.
Reproduced exactly; pinned by `ControlFlow.errorCutoff`,
`ControlFlow.oneHundredErrorsIsUnderTheLimit` and `ControlFlow.transactionFileOpenFailure`.

## 5. Subroutine contracts

| COBOL | Java | Returns |
| ----- | ---- | ------- |
| `CALL 'AUDPROC' USING AUDIT-RECORD` | `AuditProcessor.process(AuditRecord)` | `0` on write, `8` on failure |
| `CALL 'ERRPROC' USING ERR-MESSAGE`  | `ErrorProcessor.process(ErrorMessage)` | the severity it was given |

Both are interfaces with no production implementation in Phase 0: the file-backed behaviour
(`AUDFILE`, `ERRLOG`) belongs with whichever slice needs to persist. The harness supplies
`RecordingAuditProcessor` and `RecordingErrorProcessor`, which snapshot every record they receive -
necessary because the COBOL programs log out of a single working-storage area that the next
transaction overwrites.

## 6. Test harness

`src/test/java/com/clbs/portfolio/harness/TestData.java` seeds portfolios, transactions and positions
from the documented sample records, adapted where the documentation does not fit the copybooks (G5).
`DocumentedRulesTest` is the oracle: documented ranges, documented validation rules, the documented
error catalogue, and the documentation-versus-copybook disagreements. The remaining tests pin the
storage semantics every later slice depends on - scale, truncation, padding, the `SPACES` flag and
buffer copying.

## 7. Rules for the parallel slices

1. Consume the Phase 0 types. Do not redefine a translated copybook, an enum or a subroutine
   interface, and do not change one in a way that alters existing behaviour; if a slice needs a field
   the shared model lacks, add it and document it here.
2. One slice, one file set. Slices are partitioned so that no two touch the same files.
3. New programs go in `service/` as classes named after the program, with methods named after the
   paragraphs (`2110-CHECK-PORTFOLIO` -> `checkPortfolio`), and a Javadoc line tying each method back
   to its paragraph.
4. Error and audit strings are copied byte for byte from the COBOL, including the trailing detail of
   `STRING` concatenations. Preserve short-circuit ordering exactly.
5. Every discrepancy found gets a `G`-numbered entry in section 4 and a test that pins it.

## 8. `PORTTRAN` - portfolio transaction processing (Child 1)

`src/programs/portfolio/PORTTRAN.cbl` becomes `service/PortfolioTransactionProcessor.java`, tested by
`src/test/java/com/clbs/portfolio/service/PortfolioTransactionProcessorTest.java`. The program's
working storage becomes instance state - one `ERR-MESSAGE` area, one `AUDIT-RECORD` area, one
transaction record area, one portfolio record area and the three counters - reused across
transactions exactly as the COBOL reuses them, which is why the error and audit doubles snapshot
what they are given.

**The main flow validates and does nothing else.** `2000-PROCESS-TRANSACTIONS` performs
`2100-VALIDATE-TRANSACTION` and nothing performs `2200-UPDATE-POSITIONS`, so no portfolio is ever
updated and no audit record is ever written by a run of `main()`; that is G2 and it is reproduced,
not repaired. The update subtree is translated anyway, as public methods nothing calls, so the logic
is captured and can be driven directly by a test.

### Files and subroutines

| COBOL                                     | Java                                                       |
| ----------------------------------------- | ---------------------------------------------------------- |
| `FD TRANSACTION-FILE`, sequential input   | `TransactionSource` - `open()`, `read()` (`null` is `AT END`), `close()` |
| `FD PORTFOLIO-FILE`, indexed I-O          | `PortfolioRepository` - `open()`, `findById()` (`Optional.empty()` is `INVALID KEY`), `update()`, `close()`, `getFileStatus()` |
| `WS-TRAN-STATUS`, `WS-PORT-STATUS`        | the two-character status each interface reports; `WS-PORT-STATUS` is read back through `getFileStatus()` because `2300` branches on it (G7) |
| `CALL 'AUDPROC'`, `CALL 'ERRPROC'`        | the Phase 0 `AuditProcessor` and `ErrorProcessor`           |
| `DISPLAY` in `3000-TERMINATE`             | `getDisplayLines()`                                         |
| `FUNCTION CURRENT-DATE`, `FUNCTION USER-ID` | an injected `Clock` and user id (G10)                     |

All four collaborators are constructor-injected; the class does no I/O, holds no static state and
depends on no framework.

### Paragraphs

| Paragraph                    | Method                    | Notes                                                                 |
| ---------------------------- | ------------------------- | --------------------------------------------------------------------- |
| `0000-MAIN`                  | `main()`                  | initialise, then loop `UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100`, then terminate (G14) |
| `1000-INITIALIZE`            | `initialize()`            | opens both files, logs either failure, only the transaction status gates the loop (G13) |
| `2000-PROCESS-TRANSACTIONS`  | `processTransactions()`   | one `READ`; counts it and validates it - and nothing else (G2)         |
| `2100-VALIDATE-TRANSACTION`  | `validateTransaction()`   | clears `ERR-TEXT`, then 2110, then 2120 and 2130 only while it is still spaces; counts the transaction or calls 9000 |
| `2110-CHECK-PORTFOLIO`       | `checkPortfolio()`        | blank id, else read by key; the key is moved into the record area before the read and survives a failure |
| `2120-CHECK-TRANSACTION-TYPE`| `checkTransactionType()`  | `BU`, `SL`, `TR`, `FE` pass                                            |
| `2130-CHECK-AMOUNTS`         | `checkAmounts()`          | quantity always; price and amount only when the type is not `TR`       |
| `2200-UPDATE-POSITIONS`      | `updatePositions()`       | dispatch then audit; no `WHEN OTHER` (G11); unreachable (G2)           |
| `2210-PROCESS-BUY`           | `processBuy()`            | units += quantity, cost += amount (G1)                                 |
| `2220-PROCESS-SELL`          | `processSell()`           | refuses `units < quantity`, else units -= quantity, cost -= amount     |
| `2230-PROCESS-TRANSFER`      | `processTransfer()`       | sets the text and logs it; no transfer (G3)                            |
| `2240-PROCESS-FEE`           | `processFee()`            | cost -= amount only; units untouched; its own not-found text           |
| `2300-UPDATE-AUDIT-TRAIL`    | `updateAuditTrail()`      | `SUCC` only when the portfolio file status is `00` (G7)                |
| `2310-WRITE-AUDIT-RECORD`    | `writeAuditRecord()`      | non-zero reported status takes the error path (G4)                     |
| `3000-TERMINATE`             | `terminate()`             | closes both files unconditionally and reports the counters (G13)       |
| `9000-ERROR-ROUTINE`         | `errorRoutine()`          | counts, stamps `PR` and `PORTTRAN`, calls `ERRPROC`; no code, no severity (G6) |

### Error strings

Every text is a public constant on the class, spelled as the COBOL spells it. How it reaches the
80-byte `ERR-TEXT` depends on the statement:

| Statement | Text | Rendering |
| --------- | ---- | --------- |
| `MOVE` (2110) | `Portfolio ID is required` | replaces the field, space-padded to 80 |
| `STRING` (2110) | `Invalid Portfolio ID: ` + `TRN-PORTFOLIO-ID` | `DELIMITED BY SIZE`, so all eight bytes of the id, trailing spaces included: `Invalid Portfolio ID: PORT99  ` for an id of `PORT99` |
| `STRING` (2120) | `Invalid Transaction Type: ` + `TRN-TYPE` | both bytes of the type: `Invalid Transaction Type: XX` |
| `MOVE` (2130) | `Quantity must be greater than zero`, `Price must be greater than zero`, `Amount must be greater than zero` | replace the field |
| `MOVE` (2210, 2220) | `Portfolio not found for update` | replaces the field |
| `MOVE` (2220) | `Insufficient units for sale` | replaces the field |
| `MOVE` (2230) | `Transfer processing not implemented` | replaces the field |
| `MOVE` (2240) | `Portfolio not found for fee` | replaces the field |
| `MOVE` (2210, 2220, 2240) | `Error updating portfolio` | replaces the field |
| `MOVE` (2310) | `Error writing audit record` | replaces the field |
| `MOVE` (1000) | `Error opening transaction file`, `Error opening portfolio file` | replace the field |

`STRING ... DELIMITED BY SIZE INTO` overlays the receiver from the left and leaves the rest of it as
it was rather than padding, and neither statement has an `ON OVERFLOW` phrase; the private
`stringInto` helper reproduces exactly that. Because `2100-VALIDATE-TRANSACTION` clears `ERR-TEXT`
first, the residue is spaces on every path the main flow takes.

### The audit message

`AUD-MESSAGE` is built by the same `STRING` mechanism over two `COMP-3` senders, which is not legal
COBOL (G8). The packed values are rendered as `CobolDecimal.image` renders them - a sign followed by
the field's unscaled digits, fifteen for both `TRN-AMOUNT` (`S9(13)V99`) and `TRN-QUANTITY`
(`S9(11)V9(4)`) - so the seeded buy of 100 units for 12,500.00 produces:

```
Transaction: BU Amount: +000000001250000 Units: +000000001000000
```

`AUD-BEFORE-IMAGE` is `PortfolioRecord.toRecordImage()` truncated to its hundred bytes by the group
move, and it is taken after the update has already happened (G12).
