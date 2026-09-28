# PORTVALD: legacy analysis and Java migration slice

| | |
|---|---|
| Legacy program | `src/programs/portfolio/PORTVALD.cbl` (119 lines, unmodified) |
| Copybooks used | `src/copybook/common/PORTVAL.cpy` (the only `COPY` in the program) |
| Java port | `java/` - Maven module, Java 17, JUnit 5, package `com.cognition.portfolio.validation` |
| Parity oracle | GnuCOBOL 3.1.2 build of the unmodified program, 99 input cases |
| Parity status | 99 / 99 cases identical (return code, all 50 bytes of the message, amount conversion) |

---

## 1. Executive summary

**What the program is.** `PORTVALD` is a small shared "checker" in the portfolio system. Another
program hands it one piece of data plus a letter saying what kind of data it is - a portfolio ID,
an account number, an investment type, or a money amount - and `PORTVALD` answers with a numeric
result code (0 = OK) and a short English error message. It keeps no state, reads no files and
touches no database, which makes it a clean first slice to migrate.

**What it actually does today.** Reading the code and then running it showed that only one of the
four checks behaves the way its own comments describe:

| Check | What the comment says | What the program really does |
|---|---|---|
| Investment type | Must be one of the valid codes | **Works as described.** Accepts exactly `STK`, `BND`, `MMF`, `ETF`. |
| Portfolio ID | `PORT` followed by 4 digits | **Rejects every ID**, including correct ones like `PORT1234`. |
| Account number | 10 digits | **Rejects every 10-digit account.** Only a 50-digit number passes. |
| Amount | Must be within the valid range | **Accepts everything**, including text such as `ABC`, which it reads as zero. |

The cause in each case is a mismatch between field sizes, not a business decision (details in
section 5). No program in this repository calls `PORTVALD`, which is the most likely reason the
defects were never noticed (inference - there is no production history to confirm it).

**What was built.** A Java version that reproduces the program's *actual* behaviour exactly,
defects included, and a test harness that compiles the original COBOL, runs it and the Java side by
side on 99 inputs, and fails if they disagree on a single character. The defects were deliberately
**not** fixed: whether `PORT1234` should be accepted is a business decision, and changing it inside
a migration would make it impossible to prove the migration itself is safe.

**What we recommend next.** (1) Business owners decide the intended rules for the three defective
checks. (2) Those corrections are made in Java as separate, reviewed changes, each with its own
test cases. (3) Before any production cut-over, the parity cases are replayed once against the
real mainframe compiler, because this environment uses the open-source GnuCOBOL compiler, which
treats some malformed amounts differently (section 8, gap G1).

---

## 2. Scope and sources

Read in full:

- `src/programs/portfolio/PORTVALD.cbl` - the program.
- `src/copybook/common/PORTVAL.cpy` - return codes, messages, constants, work areas. This is the
  only copybook `PORTVALD` includes.
- `src/copybook/common/PORTFLIO.cpy` and `src/copybook/common/ERRHAND.cpy` - named in the brief;
  **neither is referenced by `PORTVALD`**. They matter only as context:
  - `PORTFLIO.cpy` holds the portfolio master record, whose money fields
    (`PORT-TOTAL-VALUE`, `PORT-CASH-BALANCE`) are `PIC S9(13)V99 COMP-3`. `PORTVALD`'s amount limits
    use the same precision (13 integer, 2 decimal digits) but as zoned `DISPLAY`, not `COMP-3`.
    A caller would have to convert a packed field to text before asking `PORTVALD` to check it.
  - `ERRHAND.cpy` defines the system-wide severity convention, where `4` means *warning*. In
    `PORTVALD`, `4` means *invalid amount*. A caller that routes `PORTVALD`'s code through the
    shared error handler would misclassify it (see risk R7).

Callers: `rg PORTVALD src/` finds only the program itself. No batch job, CICS transaction or JCL in
the repository invokes it. The program is therefore specified entirely by its own source.

`SOURCE-COMPUTER` / `OBJECT-COMPUTER` are `IBM-ZOS`; the source targets IBM Enterprise COBOL on
z/OS. This environment compiles it with GnuCOBOL 3.1.2 on Linux (ASCII). Section 8 covers the
consequences.

---

## 3. Data contract

### 3.1 COBOL interface

`CALL 'PORTVALD' USING LS-VALIDATION-REQUEST` - one 103-byte parameter, passed by reference.

| Offset | Len | Field | PIC | Dir | Meaning |
|---:|---:|---|---|---|---|
| 0 | 1 | `LS-VALIDATE-TYPE` | `X(1)` | in | `'I'` portfolio ID, `'A'` account, `'T'` investment type, `'M'` amount (88-levels). Case-sensitive. |
| 1 | 50 | `LS-INPUT-VALUE` | `X(50)` | in | Value to check, left-justified, space-padded (as any COBOL `MOVE` into `X(50)` leaves it). |
| 51 | 2 | `LS-RETURN-CODE` | `S9(4) COMP` | out | Binary halfword, big-endian on z/OS: 0-4 (section 4). |
| 53 | 50 | `LS-ERROR-MSG` | `X(50)` | out | Message text, space-padded to 50; all spaces on success. |

Signalling:

- Results are returned **only** through `LS-RETURN-CODE` and `LS-ERROR-MSG`. Both are overwritten on
  every path (the harness pre-fills them with `-9999` / `'?'` sentinels to prove this).
- The `RETURN-CODE` special register is not set; the harness observes 0 after every call.
- `LS-VALIDATE-TYPE` and `LS-INPUT-VALUE` are never modified.
- Stateless: `INITIALIZE VAL-WORK-AREAS` on entry, no files, no SQL, no CICS. Safe to call
  concurrently.

### 3.2 Java interface

```java
ValidationResult r = new PortfolioValidator().validate('I', "PORT1234");
r.returnCode();    // int, LS-RETURN-CODE
r.errorMessage();  // String of exactly 50 chars, LS-ERROR-MSG including trailing spaces
```

| COBOL | Java |
|---|---|
| `LS-VALIDATE-TYPE PIC X(1)` | `char validateType`; must be <= U+00FF |
| `LS-INPUT-VALUE PIC X(50)` | `String inputValue`; <= 50 chars, each <= U+00FF; right-padded to 50 with spaces internally |
| `LS-RETURN-CODE PIC S9(4) COMP` | `int ValidationResult.returnCode()` |
| `LS-ERROR-MSG PIC X(50)` | `String ValidationResult.errorMessage()`, length always 50 (enforced by the record) |
| `VAL-TEMP-NUM PIC S9(13)V99` | `BigDecimal`, scale 2 (`DisplayNumericMove`) |
| paragraphs `1000`..`4000` | private methods `validateId`, `validateAccount`, `validateType`, `validateAmount` |

Inputs a COBOL caller cannot produce are rejected rather than silently coerced: `null`
(`NullPointerException`), more than 50 characters, or characters above U+00FF
(`IllegalArgumentException`). See gap G3.

---

## 4. Validation rules (as executed)

Positions are 1-based within `LS-INPUT-VALUE` (50 bytes, space-padded).

| Rule | Paragraph | Condition that fails | RC | `LS-ERROR-MSG` (padded to 50) |
|---|---|---|---:|---|
| D1 | `0000-MAIN` | `LS-VALIDATE-TYPE` not exactly `I`, `A`, `T` or `M` (lowercase, blank, LOW-VALUE, digits all fail) | 1 | `Invalid validation type` |
| I1 | `1000-VALIDATE-ID` | positions 1-4 not exactly `PORT` (case-sensitive, no trimming) | 1 | `Invalid Portfolio ID format` |
| I2 | `1000-VALIDATE-ID` | positions 5-8 moved into a 10-byte field and class-tested `NUMERIC`; the 6 padding spaces always fail - **every input fails** | 1 | `Invalid Portfolio ID format` |
| A1 | `2000-VALIDATE-ACCOUNT` | any of the 50 bytes is not `0`-`9` (spaces, signs, `.`, TAB, LOW-VALUE all fail) | 2 | `Invalid Account Number format` |
| A2 | `2000-VALIDATE-ACCOUNT` | all 50 bytes are `0` | 2 | `Invalid Account Number format` |
| T1 | `3000-VALIDATE-TYPE` | the whole 50-byte field is not `STK`, `BND`, `MMF` or `ETF` followed by 47 spaces (case-sensitive; no left-trim; TAB or LOW-VALUE is not a space) | 3 | `Invalid Investment Type` |
| M1 | `4000-VALIDATE-AMOUNT` | text converted to `S9(13)V99` is `< -9999999999999.99` or `> +9999999999999.99` - **cannot happen**, so M1 never fires | 4 | `Amount outside valid range` |
| OK | all | no rule failed | 0 | 50 spaces |

Consequences worth stating plainly:

- **RC 1 is overloaded**: an invalid portfolio ID and an unknown validation type both return 1. Only
  the message text distinguishes them.
- **RC 4 and `Amount outside valid range` are dead**: no input produces them.
- **`'I'` always returns RC 1**; **`'M'` always returns RC 0**.
- The first failing rule wins; checks inside a paragraph are evaluated in the order listed.

---

## 5. Observed behaviour vs. stated intent

Each paragraph has a one-line comment stating its intent. Three do not match the code. All three
were confirmed by running the compiled program (case IDs refer to
`java/src/test/resources/parity/portvald-cases.tsv`).

### 5.1 Portfolio ID - always rejected (I2; cases ID-01..ID-05)

Comment: *"Portfolio ID must start with 'PORT' and have 4 numeric digits"*.

```cobol
MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK      *> VAL-NUMERIC-CHECK is PIC X(10)
IF VAL-NUMERIC-CHECK IS NOT NUMERIC ...
```

An alphanumeric `MOVE` of 4 bytes into a 10-byte field pads it with 6 spaces. `IS NUMERIC` on a
`PIC X` field is true only if **every** byte is a digit, so it is never true. `PORT1234`,
`PORT0000`, and even `PORT` followed by 46 digits are all rejected. Bytes after position 8 are never
examined. This is standard COBOL semantics, not a compiler quirk; IBM Enterprise COBOL behaves the
same way (inference from the language rules; not run on z/OS).

### 5.2 Account number - 10-digit accounts rejected (A1; cases ACCT-01..ACCT-08)

Comment: *"Account number must be 10 numeric digits"*.

```cobol
IF LS-INPUT-VALUE IS NOT NUMERIC OR LS-INPUT-VALUE = ZEROS ...
```

The class test covers the whole 50-byte field, so a 10-digit account followed by 40 padding spaces
fails. The only accepted shape is exactly 50 digits, not all zero. `0000000000` is rejected by the
`NUMERIC` test, not the `ZEROS` test.

### 5.3 Amount - never rejected (M1; cases AMT-*)

Comment: *"Amount must be within valid range"*.

```cobol
MOVE LS-INPUT-VALUE TO VAL-TEMP-NUM                *> PIC X(50) -> PIC S9(13)V99
IF VAL-TEMP-NUM < VAL-MIN-AMOUNT OR VAL-TEMP-NUM > VAL-MAX-AMOUNT ...
```

`VAL-TEMP-NUM` can only hold -9999999999999.99..+9999999999999.99, which is exactly the range
tested, so the comparison can never be true. Anything too large is truncated on the left *before*
the comparison, and anything that is not a number becomes zero. `ABC`, `$100`, `1e5`, blanks and
`99999999999999999999` (truncated to `9999999999999.00`) are all accepted.

Nothing in the program rejects a non-numeric amount; there is no `IS NUMERIC` or `NUMVAL` check.

---

## 6. Numeric semantics and precision

### 6.1 Where the numeric fields are

| Field | PIC / USAGE | Role |
|---|---|---|
| `LS-RETURN-CODE` | `S9(4) COMP` (binary) | output; values 0-4 only |
| `VAL-SUCCESS`..`VAL-INVALID-AMT` | `S9(4)` zoned `DISPLAY` | constants 0-4, moved to `LS-RETURN-CODE` |
| `VAL-MIN-AMOUNT` / `VAL-MAX-AMOUNT` | `S9(13)V99` zoned `DISPLAY` | range limits, ±9999999999999.99 |
| `VAL-TEMP-NUM` | `S9(13)V99` zoned `DISPLAY` | receives the amount text |

**There is no `COMP-3` (packed decimal) anywhere in `PORTVALD` or `PORTVAL.cpy`.** The only
fixed-point decimal is the zoned `S9(13)V99`; the Java port models it (and the limits) with
`BigDecimal` at scale 2, never `double`. The binary return code has no precision concern because
its values are 0-4 (`TRUNC` compiler options are irrelevant).

### 6.2 `MOVE PIC X(50) TO PIC S9(13)V99` (GnuCOBOL 3.1.2)

This is the only numeric conversion in the program, and it is where GnuCOBOL departs from the
COBOL standard. GnuCOBOL (`libcob/move.c`, `cob_move_alphanum_to_display`) *parses* the text; the
Java class `DisplayNumericMove` reproduces that routine step for step:

1. Skip leading whitespace (C-locale `isspace`: space, TAB, LF, VT, FF, CR - not NO-BREAK SPACE
   0xA0).
2. Accept one leading `+` or `-`.
3. Count the digits before the first `.` anywhere in the rest of the field.
4. If there are more than 13, discard leading digits - **and any bytes between them, unexamined** -
   until 13 remain (high-order truncation). `PORT12341234567890` becomes `2341234567890.00`.
5. Copy digits until the 15-digit receiver is full. Spaces and `,` are ignored anywhere. One `.` is
   accepted. A second `.` or any other byte **zeroes the whole value and drops the sign**.
6. Bytes after the receiver is full are **never examined**. Extra decimals are **truncated, not
   rounded**: `123.999` -> `123.99`; `1.234X` -> `1.23`; but `1.2X` -> `0.00`.
7. `-` or `-0` leaves a *negative zero* in the zoned field. `BigDecimal` has no negative zero; Java
   returns `0.00`. Both compare equal to every limit, so the result code cannot differ (gap G2).

The parity harness compares this conversion directly (the `move_probe` column), not only the
resulting return code, because the return code alone (always 0) would hide any divergence.

---

## 7. Porting risks

| # | Risk | In PORTVALD | How the port handles it |
|---|---|---|---|
| R1 | **Rounding** | None performed. Excess decimals of an amount are truncated (`123.999` -> `123.99`). | `DisplayNumericMove` truncates; no `RoundingMode` is ever applied. Cases AMT-14, AMT-15, AMT-37. |
| R2 | **High-order truncation** | Amounts with > 13 integer digits silently lose leading digits; this is why M1 is dead. | Reproduced; cases AMT-11..13, AMT-39, AMT-40. A naive `new BigDecimal(s)` + range check would *reject* these and break parity. |
| R3 | **Fixed-width text / trailing spaces** | Every `PIC X` compare and class test sees all bytes including padding (causes of 5.1 and 5.2). | Java works on the padded 50-byte field, not on a trimmed `String`. Messages are returned padded to 50. `trim()` appears nowhere. |
| R4 | **Signed fields** | Amount sign only as one leading `+`/`-`; trailing `123-` is invalid -> `0.00`. Negative zero possible. Account number rejects any sign. | Reproduced; cases AMT-04, AMT-05, AMT-19, AMT-24, AMT-25, AMT-29, AMT-32, ACCT-12, ACCT-13. Negative zero: gap G2. |
| R5 | **Blank vs. zero** | Blank amount -> 0.00 -> accepted. Blank account -> rejected (not numeric). All-zero account -> rejected (`= ZEROS`). Blank ID / type -> rejected. | Each covered: AMT-23, ACCT-10, ACCT-04, ID-11, TYPE-10. Java does not map `""` to `null` or vice versa. |
| R6 | **Character set** | Compares only against `PORT`, `STK`/`BND`/`MMF`/`ETF`, digits and spaces - characters present in both ASCII and EBCDIC. | Java accepts only single-byte characters (<= U+00FF). Non-ASCII bytes are exercised only for NO-BREAK SPACE and LOW-VALUE (AMT-34..36). See G1. |
| R7 | **Return-code meaning collides with system convention** | `ERRHAND.cpy` treats 4 as *warning*; `PORTVALD` uses 4 for *invalid amount*. | Out of scope (no callers); codes are preserved numerically. Flagged for whoever integrates the Java class. |
| R8 | **Compiler dialect** | The amount `MOVE` result depends on the compiler (section 6.2 vs. IBM). | Oracle is GnuCOBOL; known gap G1. |

---

## 8. Known gaps

Stated plainly. None of these is papered over in code or tests.

**G1 - The parity oracle is GnuCOBOL on Linux, not IBM Enterprise COBOL on z/OS.** The source
targets z/OS; no mainframe is available here. For rules D1, I1, I2, A1, A2 and T1 this should not
matter: they are character-equality and class tests on characters that exist in both code pages
(inference from the language rules). For the amount `MOVE` (section 6.2) it can matter. The COBOL
standard, which IBM follows, treats an alphanumeric sender as an *unsigned integer* aligned on its
rightmost byte; it does not parse signs, decimal points, commas or leading spaces. Under IBM
Enterprise COBOL a space-padded input such as `123` would therefore place padding bytes into the
receiver, and the subsequent comparison could either be evaluated on invalid zoned data or raise
a data exception (ABEND S0C7), depending on `NUMPROC` and related compile options. **This has not
been verified on z/OS.** The Java port reproduces GnuCOBOL. Verified here: under GnuCOBOL the
COBOL program never returns RC 4 and neither does Java. Not verified: under IBM Enterprise COBOL,
comparing invalid zoned data could in principle produce a different result code, and some amount
inputs might abend the caller instead of returning RC 0. Closing this gap requires replaying `portvald-cases.tsv` through `PVDRIVER` on z/OS and
diffing against `portvald-cobol-golden.tsv`; the harness is designed so that is a data exercise,
not a code change.

**G2 - Negative zero is not representable.** GnuCOBOL can hold `-0.00` in `VAL-TEMP-NUM` (golden
file shows `-0.00` for AMT-24, AMT-25). Java returns `0.00`. The harness compares numerically
(`compareTo`), under which they are equal, as they are in every COBOL numeric comparison. No
output of `PORTVALD` exposes the sign of zero, so this is unobservable to callers.

**G3 - Java rejects inputs that COBOL cannot receive.** A COBOL caller moving 60 characters into
`LS-INPUT-VALUE` would silently truncate to 50; the Java API throws `IllegalArgumentException`
instead, as it does for `null` and characters above U+00FF. This is a deliberate API decision
(silent truncation at a Java boundary would hide caller bugs), and it only affects inputs that do
not exist on the COBOL side. It is covered by `PortfolioValidatorTest`, not by the parity table.

**G4 - The amount conversion is observed through a replay, not inside PORTVALD.** `PORTVALD` does
not expose `VAL-TEMP-NUM`. The test driver `PVDRIVER` repeats the identical `MOVE` into a field
with the identical PIC and records the result. This is the same statement compiled by the same
compiler, but it is technically a second execution, not a read of `PORTVALD`'s own storage.

**G5 - Program-level parity, not integration parity.** There are no callers to test against. The
harness proves the Java class behaves like the COBOL subroutine for every tested input; it does not
test how a future caller interprets the result (see R7).

**G6 - Parity is example-based.** 99 hand-chosen cases cover every rule, every return code, every
branch of the numeric conversion and the edge cases above, but they are not exhaustive. A
property-based/fuzz comparison against the live COBOL driver is a straightforward extension.

**G7 - No CI runs this.** The repository has no CI pipeline for the Java module. Everything above
was run locally in the build environment; see the PR for the exact commands and output.

---

## 9. Migration approach

### 9.1 Strategy

1. **Behaviour-faithful port (this change).** Additive only: no COBOL source modified, no repo
   restructuring. Java reproduces the executed behaviour, including the defects in section 5, and
   the harness proves it against the real compiled program.
2. **Business decision on intended rules.** For I2, A1 and M1, business owners decide the intended
   rule (e.g. "PORT + exactly 4 digits + spaces", "10 digits + spaces", "numeric and within limits,
   reject non-numeric text"). Also decide whether to split RC 1 and revive RC 4.
3. **Corrections as explicit, separate changes.** Each correction changes Java and marks the affected
   rows in the case table as intentional divergences, with its own reviewed PR. Parity then means
   "identical except for the listed, approved differences".
4. **z/OS confirmation (gap G1)** before any caller is switched.
5. **Integration / cut-over.** Callers adopt `PortfolioValidator` directly or via a thin adapter
   mapping to the 103-byte record; `PORTVALD` is retired once no caller remains.

### 9.2 Parity harness

```
portvald-cases.tsv ──► PVDRIVER (COBOL, test-only) ──CALL──► PORTVALD.so (unmodified source)
        │                         │
        │                         └──► results: RC, 50-byte message, RETURN-CODE, amount MOVE
        └──► PortfolioValidator (Java) ──► compared field by field, every case
```

| File | Purpose |
|---|---|
| `java/src/test/resources/parity/portvald-cases.tsv` | **Input case table (data).** 99 rows: id, type, input, category, description. No expected values - those come from COBOL. |
| `java/src/test/resources/parity/portvald-cobol-golden.tsv` | Outputs recorded from the compiled COBOL program. Regenerated only by the script. |
| `java/src/test/cobol/PVDRIVER.cbl` | Test driver: reads fixed 51-byte records, `CALL`s `PORTVALD`, writes fixed 76-byte results. Contains no validation logic. |
| `java/run-parity.sh` | Compiles `PORTVALD.cbl` and `PVDRIVER.cbl` with `cobc`, runs Maven with live parity enabled. |
| `PortvaldParityTest` | `Recorded`: Java vs. golden (always runs). `Live`: Java vs. freshly executed COBOL, plus golden == live (runs when `-Dparity.cobol.driver` is set). Also asserts the golden covers exactly the case table, all four types plus `WHEN OTHER`, and every return code the program can produce. |

Per case, four things must match: `LS-RETURN-CODE`; all 50 bytes of `LS-ERROR-MSG`; COBOL
`RETURN-CODE` is 0; the amount-conversion value.

Coverage:

| Rule group | Cases | VALID | BOUNDARY | FAILURE | QUIRK |
|---|---:|---:|---:|---:|---:|
| Dispatch (D1) | 9 | | | 9 | |
| Portfolio ID (I1, I2) | 13 | | | 8 | 5 |
| Account (A1, A2) | 16 | 1 | 4 | 9 | 2 |
| Investment type (T1) | 17 | 4 | | 13 | |
| Amount (M1, section 6.2) | 44 | 6 | 6 | | 32 |
| **Total** | **99** | **11** | **10** | **39** | **39** |

`QUIRK` marks executed behaviour that contradicts the paragraph's comment or would surprise a
reader; these rows are the ones a correction in step 3 would touch.

### 9.3 Running it

```bash
cd java
mvn test              # Java unit tests + Java vs. recorded COBOL outputs (no COBOL needed)
./run-parity.sh       # compile PORTVALD with GnuCOBOL, run it, compare live (needs cobc)
```

Adding a case: append a row to `portvald-cases.tsv`, run `./run-parity.sh --update-golden`, and
commit both files. The golden diff shows exactly what the COBOL program returned for the new input.
