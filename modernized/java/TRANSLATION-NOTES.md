# Translation Notes - COBOL to Java

Living record of every mapping decision and every source discrepancy found while translating the
COBOL Legacy Benchmark Suite to Java. Each translated slice appends to this file; nothing here is
rewritten silently.

| Slice                                        | Status | Covers                                                              |
| -------------------------------------------- | ------ | ------------------------------------------------------------------- |
| Phase 0 - shared foundation                   | Landed | `TRNREC`, `POSREC`, `PORTFLIO`, `ERRHAND`, `AUDITLOG`, `AUDPROC`/`ERRPROC` contracts, test harness |
| Child 1 - `PORTTRAN.cbl`                      | Open   | `PortfolioTransactionProcessor`, `PortfolioRepository`               |
| Child 2 - portfolio CRUD                      | Open   | `PORTMSTR`, `PORTADD`, `PORTUPDT`, `PORTDEL`, `PORTREAD`             |
| Child 3 - batch pipeline                      | Open   | `HISTLD00`, transaction validation                                   |
| Child 4 - reporting                           | Open   | `RPTPOS00`, `RPTAUD00`, `RPTSTA00`                                   |

Discrepancies found so far are catalogued as `G1`-`G10` in section 4.

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
| `PIC S9(4) COMP`                     | `int`, capacity deliberately not enforced                    | severity fields, see 3.4 |
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
sign plus their unscaled digits and the image is an approximation by construction. It is **not**
byte-accurate and must not be treated as such: the rendering is 164 characters against the 148 bytes
the copybook occupies, because a 16-character image stands in for each 8-byte packed field.

The class also carries two fields that **no copybook defines** - see G1.

### 3.4 `ERRHAND.cpy` -> `ErrorMessage`, `ErrorCategory`, `ErrorSeverity`, `VsamStatus`

The copybook is a set of constant tables plus the `ERR-MESSAGE` area. Constants become enums
(`ERR-CATEGORIES` -> `ErrorCategory`, `ERR-RETURN-CODES` -> `ErrorSeverity`, `ERR-VSAM-STATUSES` +
`ERR-VSAM-MSGS` -> `VsamStatus`), and the area becomes `ErrorMessage`.

`ERR-TEXT` is the program's error flag, not just a message: `PORTTRAN` decides what to do next with
`IF ERR-TEXT = SPACES`. It is therefore stored padded to its full 80 characters and read through
`isErrTextSpaces()`, with `clearErrText()` for `MOVE SPACES TO ERR-TEXT`. `getErrTextTrimmed()`
exists for assertions and logging only.

`ERR-SEVERITY` is the one numeric field whose picture is **not** enforced: `setErrSeverity` takes an
`int` and stores it, where `PIC S9(4) COMP` holds four digits. This is a deliberate simplification -
the only values any program assigns are the `ERR-RETURN-CODES` constants 0, 4, 8, 12 and 16, and
`ERRPROC` does nothing with the field but copy it into its return code. A slice that starts
computing a severity should clamp it the way the packed fields do.

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

Note when checking this locally: GnuCOBOL 3.1.2 accepts a packed sender in `STRING`, in its default
dialect and under `-std=ibm` alike, so `cobc` does not corroborate G8. The target platform is what
matters here, and the Enterprise COBOL V6.4 rules for `STRING` are explicit - senders must be usage
`DISPLAY`, `DISPLAY-1`, `NATIONAL` or `UTF-8`, and a numeric sender must be an integer - so the
statement is invalid on two independent counts. `cobc` is a weak oracle for IBM-dialect claims
generally; treat a GnuCOBOL result as evidence only when it agrees with the language reference.

### G9 - the portfolio record area is referred to by two different names

`2210`, `2220` and `2240` all write it back with `REWRITE PORTFOLIO-RECORD`
(`PORTTRAN.cbl:194`, `:219`, `:242`), but `2300-UPDATE-AUDIT-TRAIL` reads it as
`MOVE PORT-RECORD TO AUD-BEFORE-IMAGE` (`:278`). `PORT-RECORD` is the `01` level in `PORTFLIO.cpy`;
`PORTFOLIO-RECORD` is presumably what the missing `PORTREC.cpy` declared under the `FD` (G1).
Whichever copybook was intended, one of the two names is undefined and the program cannot compile.

**Decision.** `PortfolioRecord` is the single translated record and both names resolve to it. This
costs nothing behaviourally - the two statements are reading and writing the same file's record area
in any reading of the source - but it is a second, independent symptom of the missing copybook.

### G10 - `FUNCTION USER-ID` is not an intrinsic function

`2300-UPDATE-AUDIT-TRAIL` populates the audit user with `MOVE FUNCTION USER-ID TO AUD-USER-ID`
(`PORTTRAN.cbl:254`). IBM Enterprise COBOL has no `USER-ID` intrinsic (the neighbouring
`FUNCTION CURRENT-DATE` on line 252 is real), so `AUD-USER-ID` has no defined source. The
translation supplies the user id explicitly rather than inventing an equivalent; the slice that owns
`PORTTRAN` documents where it gets the value from.

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
