# PORTVALD — migration analysis and Java port

**Legacy artefact:** `src/programs/portfolio/PORTVALD.cbl` (COBOL subroutine, 120 lines)
**Copybook:** `src/copybook/common/PORTVAL.cpy`
**Port:** `java/` (Maven module `portvald-port`, package `com.cognition.portfolio.validation`)
**Parity harness:** `parity/` (82 cases, executed against the compiled legacy module)

---

## 1. For the business reader

PORTVALD is the gatekeeper for portfolio data. Every other program in the suite that
accepts a portfolio identifier, an account number, an investment type or a money amount
is supposed to call PORTVALD first and act on the answer it gives back: a numeric code
that means "accepted" or names the reason for rejection, plus a short message.

Four things are checked, one per call:

| # | What is checked | Intended rule (per the source comments) |
|---|-----------------|-----------------------------------------|
| 1 | Portfolio ID | must look like `PORT` followed by four digits, e.g. `PORT1234` |
| 2 | Account number | must be ten digits |
| 3 | Investment type | must be one of stock, bond, money market fund, exchange traded fund |
| 4 | Amount | must be inside the permitted money range |

**The important finding is that the program does not do what those comments say.** Running
the compiled program here shows that, as written today:

* **every** portfolio ID is rejected, including the `PORT1234` example the code comments
  use — rule 1 can never succeed;
* a genuine ten-digit account number is rejected, while a fifty-digit string of digits is
  accepted — rule 2 checks the whole fifty-character input field rather than the first ten
  characters;
* the amount check can never reject anything: the range it tests against is exactly the
  range of the field holding the value, so nothing can ever fall outside it. Text that is
  not a number at all (`ABC`, `$100`) is silently converted to zero and accepted;
* the investment type check is the only one of the four that behaves as documented;
* an unrecognised request is reported with the same numeric code as an invalid portfolio
  ID, so callers that branch on the code alone cannot tell a bad request from bad data.

This is not speculation from reading the code: section 8 lists the exact commands, and the
82 recorded results from the real program are checked into `parity/expected/`.

Two consequences for the modernization programme. First, PORTVALD is almost certainly not
load-bearing in production the way the documentation implies — any caller that relied on it
to accept `PORT1234` would have failed on day one, so the validation is either bypassed,
duplicated elsewhere, or the data never reaches it. That is worth confirming before the
rules are re-implemented anywhere. Second, the port delivered here reproduces the current
behaviour **exactly, defects included**. Fixing the defects is a separate, business-approved
change; conflating "move to Java" with "change the rules" is what makes migrations
unverifiable. The parity harness is what lets that later fix be made safely, because it
pins down what the system does today.

---

## 2. Scope of this slice

In scope: the standalone subroutine PORTVALD and the constants copybook it uses. It has no
CICS, no DB2, no VSAM and no file I/O, which is why it was chosen: it compiles and runs on
Linux under GnuCOBOL, so parity can be demonstrated by execution rather than by argument.

Out of scope: `PORTFLIO.cpy` and `ERRHAND.cpy`. Both were read — the task named them — but
neither is `COPY`d by PORTVALD, and neither influences its behaviour:

* `PORTFLIO.cpy` is the VSAM portfolio record (`PORT-ID PIC X(8)`,
  `PORT-ACCOUNT-NO PIC X(10)`, money fields `PIC S9(13)V99 COMP-3`). It is the *caller's*
  layout and explains the intent of the rules: an eight-byte ID and a ten-byte account
  number are what the record holds, and PORTVALD's amount precision matches the record's
  packed-decimal money fields.
* `ERRHAND.cpy` is the suite-wide error taxonomy (`ERR-RETURN-CODE` values 0/4/8/12/16,
  categories `VS`/`VL`/`PR`/`SY`). PORTVALD does **not** use it; it returns its own
  0–4 codes from `PORTVAL.cpy` instead. Any future consolidation of error handling has to
  reconcile these two incompatible code spaces.

---

## 3. Data contract

`PROCEDURE DIVISION USING LS-VALIDATION-REQUEST` — one 105-byte group, in and out:

| Field | Picture | Bytes | Direction | Meaning |
|-------|---------|-------|-----------|---------|
| `LS-VALIDATE-TYPE` | `PIC X(1)` | 1 | in | `I`, `A`, `T` or `M` (level-88 `LS-VAL-ID`, `LS-VAL-ACCT`, `LS-VAL-TYPE`, `LS-VAL-AMT`) |
| `LS-INPUT-VALUE` | `PIC X(50)` | 50 | in | the value to validate, left justified, space filled |
| `LS-RETURN-CODE` | `PIC S9(4) COMP` | 2 | out | 0 = accepted, 1–4 = rejected |
| `LS-ERROR-MSG` | `PIC X(50)` | 50 | out | spaces when accepted, else a fixed message |

Signalling is by these two output fields only: there is no `RETURN-CODE` special register
use, no abend, no exception, no logging, no state carried between calls. `INITIALIZE
VAL-WORK-AREAS` at entry means the program is re-entrant with respect to its own working
storage, so repeated calls are independent.

Return codes and messages (all from `PORTVAL.cpy`, message fields are `PIC X(50)`):

| Code | Constant | Message |
|------|----------|---------|
| 0 | `VAL-SUCCESS` | *(spaces)* |
| 1 | `VAL-INVALID-ID` | `Invalid Portfolio ID format` |
| 2 | `VAL-INVALID-ACCT` | `Invalid Account Number format` |
| 3 | `VAL-INVALID-TYPE` | `Invalid Investment Type` |
| 4 | `VAL-INVALID-AMT` | `Amount outside valid range` |

Code 1 is additionally reused, with the literal message `Invalid validation type`, for a
`LS-VALIDATE-TYPE` outside `I A T M`.

Callers must observe the fixed-length semantics: anything moved into `LS-INPUT-VALUE` is
padded to 50 bytes with spaces and truncated beyond 50 bytes, and `LS-ERROR-MSG` always
comes back 50 bytes wide, space filled on the right.

---

## 4. Rule table — as coded

Every row below is what the compiled program does, evidenced by the case IDs in
`parity/cases/portvald-cases.psv` and the recorded results in
`parity/expected/portvald-cobol-output.psv`.

| Rule | COBOL | Condition as coded | Code | Message | Cases |
|------|-------|--------------------|------|---------|-------|
| R0 dispatch | `EVALUATE TRUE ... WHEN OTHER` | `LS-VALIDATE-TYPE` not exactly `I`, `A`, `T` or `M` (case sensitive) | 1 | `Invalid validation type` | DSP-01…06 |
| R1 ID prefix | `IF LS-INPUT-VALUE(1:4) NOT = VAL-ID-PREFIX` | bytes 1–4 not exactly `PORT` | 1 | `Invalid Portfolio ID format` | ID-05, ID-07, ID-11, ID-12 |
| R2 ID suffix | `MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK` then `IF ... NOT NUMERIC` | bytes 5–8 are moved into a `PIC X(10)` item, so they are space filled to ten bytes and the class test **always fails** | 1 | `Invalid Portfolio ID format` | ID-01…04, ID-06, ID-08…10, ID-13…16 |
| R3 ID accept | — | unreachable | 0 | *(spaces)* | none |
| R4 account | `IF LS-INPUT-VALUE IS NOT NUMERIC OR LS-INPUT-VALUE = ZEROS` | the class test applies to **all 50 bytes**, so the value is rejected unless all 50 are digits; all-zero 50-digit input is rejected too | 2 | `Invalid Account Number format` | AC-01, AC-03…08, AC-10, AC-12, AC-13 |
| R5 account accept | — | exactly 50 digits, not all zero | 0 | *(spaces)* | AC-02, AC-09, AC-11, AC-14 |
| R6 type | `IF LS-INPUT-VALUE NOT = 'STK' AND NOT = 'BND' AND NOT = 'MMF' AND NOT = 'ETF'` | comparison is space padded to 50 bytes, so `STK` and `STK` + trailing spaces match, but `stk`, ` STK` and `STKX` do not | 3 | `Invalid Investment Type` | TY-05…12 |
| R7 type accept | — | `STK`, `BND`, `MMF`, `ETF` exactly, upper case, no leading space | 0 | *(spaces)* | TY-01…04 |
| R8 amount move | `MOVE LS-INPUT-VALUE TO VAL-TEMP-NUM` | 50 alphanumeric bytes are converted to `S9(13)V99`; see §5 | — | — | AM-01…34 |
| R9 amount range | `IF VAL-TEMP-NUM < VAL-MIN-AMOUNT OR > VAL-MAX-AMOUNT` | the limits are `-9999999999999.99` / `+9999999999999.99`, i.e. exactly the range `S9(13)V99` can represent, so the test is **never true** | 4 | `Amount outside valid range` | none — code 4 is unreachable |
| R10 amount accept | — | every input, including non-numeric text, which converts to zero | 0 | *(spaces)* | AM-01…34 |

### 4.1 The four defects, stated plainly

1. **R2 — portfolio ID validation is dead.** `VAL-NUMERIC-CHECK` is `PIC X(10)`; the source
   moves a four-byte reference-modified slice into it. COBOL pads the move on the right with
   six spaces, and a space is not numeric, so `IS NUMERIC` is false for every possible
   input. Code 0 is unreachable for `I`. Evidence: ID-01 (`PORT1234`) returns 1.
2. **R4 — account validation checks the wrong field width.** The class test is applied to
   the whole `PIC X(50)` field instead of `LS-INPUT-VALUE(1:10)`, inverting the intent: the
   documented ten-digit account number is rejected (AC-01) and a fifty-digit string is
   accepted (AC-09).
3. **R9 — the amount range test can never fire.** The bounds equal the capacity of the
   receiving item, so a value that violated them could not have been stored in the first
   place. Code 4 is dead, and the range check is decorative.
4. **R0 — code 1 is overloaded.** A malformed request and a malformed portfolio ID return
   the same code with different messages; callers that branch on the code cannot separate a
   programming error from a data error.

None of these are corrected in the port. They are reproduced, tested and documented, which
is the only way a later fix can be shown to be the *only* change.

---

## 5. Numeric behaviour

`VAL-TEMP-NUM` is `PIC S9(13)V99`: signed, thirteen integer digits, two implied decimal
places, no rounding anywhere in the program. It is declared `DISPLAY` (unpacked) here; the
corresponding money fields on the portfolio record, `PORTFLIO.cpy`, are
`PIC S9(13)V99 COMP-3` (packed decimal). Same value domain, different storage — a port must
carry the *domain* (fixed scale 2, 15 significant digits, truncation not rounding), which is
why the Java side uses `BigDecimal` with an explicit scale of 2 and never `double`.

`MOVE LS-INPUT-VALUE TO VAL-TEMP-NUM` moves alphanumeric to numeric-display. What happens
when those 50 bytes are not a clean number is **implementation defined**; the standard does
not specify it and IBM Enterprise COBOL does not parse signs, decimal points or separators
at all in this direction. The behaviour ported here is GnuCOBOL 3.1.2's, established by
executing the compiled program (and probe programs) rather than by reading documentation:

1. leading whitespace is skipped;
2. a single leading `+` or `-` sets the sign;
3. the digits before the first `.` are counted. If there are more than thirteen, the scan is
   advanced past the surplus high-order digits — **and past any non-digit bytes mixed in
   among them**, which is why junk in the skipped prefix never raises an error. Otherwise
   the digits are stored right aligned against the implied decimal point;
4. the remaining bytes are consumed until the receiving field is full: digits are stored,
   spaces and `,` are ignored, the first `.` is ignored, and any other byte — including a
   second `.` or a trailing sign — fails the whole move;
5. a failed move stores **zero**, not the digits seen so far;
6. once the field is full the scan stops, so bytes beyond that point cannot fail the move;
7. surplus fraction digits are truncated, never rounded.

Recorded consequences (all in the evidence file):

| Input | Result | Why |
|-------|--------|-----|
| `1000.999` | `1000.99` | fraction truncated, not rounded (AM-07) |
| `0.001` | `0.00` | truncation to zero (AM-14) |
| `99999999999999.99` | `9999999999999.99` | one surplus integer digit dropped from the **left** (AM-06) |
| `1234567890123456789012` | `0123456789012.00` | nine surplus digits dropped; the fraction is untouched and stays `.00` (AM-15) |
| `ABC`, `$100`, `1E3`, `12.3-` | `0.00` | invalid byte, whole move fails, stores zero (AM-08, AM-16, AM-17, AM-11) |
| *(all spaces)* | `0.00` | blank is indistinguishable from zero (AM-09) |
| `AB123456789012345` | `3456789012345.00` | two surplus digits skipped, taking `AB12` with them, so the letters never fail the move (AM-25) |
| `AB12345X` | `0.00` | no surplus digits to skip, so `X` is reached and fails the move (AM-26) |
| `1,000.00` | `1000.00` | `,` ignored (AM-10) |
| ` 42`, `4 2` | `42.00` | spaces ignored anywhere (AM-12, AM-32) |
| `1.23X` | `1.23` | field already full when `X` is reached (AM-29) |
| `1.2X` | `0.00` | field not yet full when `X` is reached (AM-30) |

Because R9 is dead, none of this changes the return code: the amount branch always returns
0. It changes the *value* a caller would compute from the same input, which is why the
parity harness captures the converted value as a third output column even though PORTVALD
itself does not expose it (see §7).

---

## 6. Porting risks

| Risk | How it bites | How the port handles it |
|------|--------------|-------------------------|
| **Padding** | `PIC X(50)` pads and truncates silently; `"STK"` in Java is 3 chars, in COBOL 50 bytes | `CobolAlphanumeric.toFixedLength` normalises every input to 50 bytes before any rule runs; comparisons are made against padded literals |
| **Reference modification** | `LS-INPUT-VALUE(5:4)` is 1-based and unchecked | `CobolAlphanumeric.refMod` is 1-based and operates on the padded field |
| **Widening on MOVE** | the R2 defect exists only because a 4-byte slice lands in a 10-byte item | modelled explicitly: the slice is padded to 10 before the class test, so the defect is reproduced rather than accidentally fixed |
| **Class test semantics** | `IS NUMERIC` on a `PIC X` item is true only if every byte is `0`–`9`; no sign, no space, no decimal point | `CobolAlphanumeric.isNumeric` |
| **Blank vs zero** | all-spaces converts to zero on a numeric move, but is *not* `= ZEROS` in the alphanumeric comparison | the two are separate helpers; `equalsZeros` compares bytes, the numeric move converts |
| **Signed fields** | `S9(4) COMP` is a signed halfword; sign is carried on the numeric move but a *trailing* sign fails it | the return code is an `int`; `CobolNumericMove` accepts a leading sign only |
| **Truncation vs rounding** | COBOL truncates on a `MOVE` without `ROUNDED`; Java's `setScale` rounds by default | digits are assembled positionally, never via `setScale(2)` on a parsed value; no `RoundingMode` other than `UNNECESSARY` appears |
| **Binary floating point** | `double` cannot hold `9999999999999.99` | `BigDecimal` with fixed scale 2 throughout |
| **Implementation-defined conversion** | the alphanumeric→numeric move differs between GnuCOBOL and IBM | ported to GnuCOBOL's behaviour, flagged as a known gap (§9) |
| **Dead branches** | a port that "tidies up" would make code 4 reachable and code 0 reachable for IDs, changing production behaviour under the banner of refactoring | the dead branches are kept, and the parity assertions prove which codes the legacy actually produces |

---

## 7. Migration approach

**Strategy: strangler-style, behaviour-preserving port with executable parity.** The Java
module is a drop-in equivalent of the subroutine, not a redesign. Nothing in the COBOL tree
is modified; `src/` is untouched and the addition is purely additive.

**Java module** (`java/`, Java 17, JUnit 5, no framework):

| Class | Role |
|-------|------|
| `PortfolioValidator` | the port of PORTVALD: `validate(char validateType, String inputValue)` returns a `ValidationResult`. One method per COBOL paragraph, named after it |
| `ValidationResult` | record of `returnCode` + 50-byte `errorMessage`, mirroring the two output fields |
| `ValidationType` | the level-88 conditions as an enum of exact byte values |
| `PortValConstants` | transcription of `PORTVAL.cpy` |
| `CobolAlphanumeric` | `PIC X(n)` semantics: padding, truncation, reference modification, `IS NUMERIC`, `= ZEROS` |
| `CobolNumericMove` | the alphanumeric→`S9(13)V99` move described in §5 |

**Parity harness** (`parity/`):

* `parity/cases/portvald-cases.psv` — the case table, checked in as data. 82 cases:
  6 dispatch, 16 portfolio ID, 14 account, 12 investment type, 34 amount. Every branch of
  every paragraph, both outcomes of every reachable decision, the boundary values
  `VAL-MIN-AMOUNT` / `VAL-MAX-AMOUNT`, and the padding, sign, separator and overflow edges.
* `parity/cobol/PVDRIVER.cbl` — a test driver that reads the case table, `CALL`s the
  **unmodified** `PORTVALD` once per case, and writes the return code, the error message
  (bracketed so trailing spaces are visible) and the converted amount. The driver is test
  scaffolding under `parity/`; it is not part of the legacy application.
* `parity/expected/portvald-cobol-output.psv` — the recorded output of that run, committed
  as evidence so a reviewer can read what the legacy actually produced.
* `PortvaldParityTest` replays every case through the Java port, renders the result in the
  driver's exact format and asserts equality line by line. It also asserts that every row of
  the evidence file corresponds to a case, so the two files cannot silently drift.
* `parity/run-parity.sh` recompiles the COBOL, re-runs it, diffs the fresh output against
  the committed evidence, and then runs `mvn test`. The evidence file therefore cannot go
  stale without the harness failing.

The third column, the converted amount, is deliberately captured even though PORTVALD does
not return it: without it the amount cases would all be indistinguishable (they all return
0), and the numeric semantics — the part of a COBOL port most likely to go wrong — would be
untested.

**Sequencing for a real migration.** (1) Confirm with the business which of the four
defects are load-bearing. (2) Land this port as a behavioural mirror. (3) Route callers
through the Java implementation behind a switch, comparing outputs in production if
possible. (4) Only then fix the rules, as separately reviewed changes with the parity table
updated case by case, so each behavioural change is visible in the diff of
`parity/expected/`.

---

## 8. Evidence — commands and results

Run from the repository root:

```
$ parity/run-parity.sh
== using JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ==
== compiling legacy module PORTVALD (unmodified) ==
src/copybook/common/PORTVAL.cpy:46: warning: line not terminated by a newline [-Wothers]
src/programs/portfolio/PORTVALD.cbl:120: warning: line not terminated by a newline [-Wothers]
== compiling parity driver PVDRIVER ==
== executing PORTVALD over the case table ==
PVDRIVER: cases executed = 00082
== comparing against committed COBOL evidence ==
COBOL output matches parity/expected/portvald-cobol-output.psv
== running the Java port against the same evidence ==
parity harness passed
```

The two compile steps it performs are the blueprint's documented ones:

```
cobc -m -I src/copybook/common -Wall -o build/PORTVALD.so src/programs/portfolio/PORTVALD.cbl
cobc -x -I src/copybook/common -Wall -o build/PVDRIVER parity/cobol/PVDRIVER.cbl
COB_LIBRARY_PATH=build CASEFILE=parity/cases/portvald-cases.psv \
  OUTFILE=build/portvald-cobol-output.psv build/PVDRIVER
```

The two `-Wall` warnings are GnuCOBOL noting that the legacy files do not end with a
newline. Fixing them would mean modifying COBOL source, which this change does not do.

Java side alone:

```
$ JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -f java/pom.xml test
Tests run: 134, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

That is 50 unit tests plus 84 generated parity assertions (one per case, plus two
structural checks).

Result distribution over the 82 cases, from the evidence file: code 0 — 42 cases, code 1 —
22, code 2 — 10, code 3 — 8, code 4 — 0 (unreachable, see R9).

---

## 9. Known gaps

1. **The conversion in §5 is GnuCOBOL's, not IBM's.** The mainframe runtime is the source of
   truth for production behaviour, and for an alphanumeric-to-numeric `MOVE` of data that is
   not a valid number the two differ: IBM Enterprise COBOL treats the sending field as
   unsigned digits and does not recognise `+`, `-`, `.` or `,`, and non-digit bytes give an
   undefined result rather than a defined zero. Cases AM-03, AM-05, AM-10, AM-11, AM-13,
   AM-16…AM-23, AM-25, AM-26, AM-28…AM-33 are therefore parity-verified against GnuCOBOL
   only. This cannot be closed on Linux; it needs one run of `PVDRIVER` against the same
   case table on z/OS, after which `parity/expected/` gains a second recorded column and the
   port takes a runtime flag. **No case that is reachable through a documented input is
   affected** — all of the affected cases are malformed amounts, and every one of them
   returns code 0 under both runtimes because R9 is dead.
2. **`LS-RETURN-CODE` overflow is not modelled.** It is `PIC S9(4) COMP`, a signed halfword;
   only values 0–4 are ever stored, so no overflow or truncation behaviour is exercised or
   ported.
3. **Callers are not analysed.** This slice ports the subroutine, not the programs that
   `CALL` it. Whether any of them depends on the defects in §4.1 — for instance by treating
   code 1 as "always" and skipping the call — is unknown and must be established before the
   defects are fixed.
4. **The amount conversion is observable only through the harness.** PORTVALD discards
   `VAL-TEMP-NUM`, so the third output column is driver-visible state, not part of the
   subroutine's contract. A caller cannot see it, and the port exposes it
   (`PortfolioValidator.amountAsMoved`) purely so the semantics stay under test.
5. **EBCDIC vs ASCII collation is not exercised.** Every comparison in PORTVALD is an
   equality test against digits and upper-case Latin letters, which collate identically in
   both, so no difference is expected — but this has not been demonstrated on z/OS.
