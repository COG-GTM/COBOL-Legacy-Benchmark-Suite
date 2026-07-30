# COBOL-to-Python Migration Plan

Version: 1.0
Status: Approved for Wave 0
Related: [System Architecture Document](system-architecture.md), [Data Dictionary](data-dictionary.md), [Python translations](../../translations/python/README.md)

## 1. Purpose

CLBS exists to benchmark LLM translation tools. Java and C# were the declared targets;
this plan adds Python and defines how a Python translation of the suite is produced,
reviewed and scored. It covers the feasibility assessment, the inventory in scope, the
migration strategy and the first migration step (already executed, see section 9).

Python is attractive as a benchmark target precisely because it is *not* a natural fit for
COBOL: it has no fixed-length records, no packed decimal, no picture clauses and no
record-level file I/O. Every mismatch is a place where a translation tool can silently
change behaviour, which makes the resulting pairs discriminating test material.

## 2. Scope

### 2.1 Programs by dependency tier

Tiering is by external dependency, because that is what determines how much runtime
scaffolding a translation needs. Counts exclude the four files under `src/templates/`.

| Tier | External dependency | Programs | Count |
|------|---------------------|----------|-------|
| 1 | None (pure business logic) | `PORTVALD`, `POSUPDT` (empty stub) | 2 |
| 2 | Sequential and VSAM (KSDS) file I/O | `AUDPROC`, `BCHCTL00`, `CKPRST`, `ERRPROC`, `PORTADD`, `PORTDEL`, `PORTMSTR`, `PORTREAD`, `PORTTEST`, `PORTTRAN`, `PORTUPDT`, `PRCSEQ00`, `RCVPRC00`, `RPTAUD00`, `RPTPOS00`, `RPTSTA00`, `TSTGEN00`, `TSTVAL00`, `UTLMNT00`, `UTLMON00`, `UTLVAL00` | 21 |
| 3 | Embedded DB2 SQL (batch/common) | `DB2CMT`, `DB2CONN`, `DB2ERR`, `DB2STAT`, `HISTLD00`, `RTNANA00`, `RTNCDE00` | 7 |
| 4 | CICS (with SQL and BMS) | `CURSMGR`, `DB2ONLN`, `DB2RECV`, `ERRHNDL`, `INQHIST`, `INQONLN`, `INQPORT`, `SECMGR` | 8 |

Sixteen of the tier 2/3 programs use `ORGANIZATION IS INDEXED` (VSAM KSDS); the rest are
sequential.

`src/programs/batch/POSUPDT.cbl` is empty in the repository even though the README lists
`POSUPD00` as a batch component. It is out of scope for translation and is tracked as a
gap in the COBOL suite itself.

### 2.2 Non-program artefacts

| Artefact | Location | Count | Disposition |
|----------|----------|-------|-------------|
| Copybooks | `src/copybook/{batch,common,db2,online}` | 20 | Translated to modules under `clbs/copybooks/` (record layouts, return codes, constants) |
| JCL | `src/jcl/**` | 15 | Translated in Wave 4 to a job runner; not part of the per-program pairs |
| BMS map | `src/maps/INQSET.bms` | 1 | Field metadata only; no Python UI is built |
| CICS CSD | `src/cics/PORTDFN.csd` | 1 | Reference material for the online adapter |
| DB2 DDL | `src/database/db2/*.sql` | 5 | Reused as-is against SQLite/PostgreSQL in Wave 3 |
| VSAM definitions | `src/database/vsam/vsam-definitions.txt` | 1 | Drives key/record layout of the KSDS emulation |

## 3. Feasibility assessment

| COBOL construct | Python mapping | Risk | Notes |
|-----------------|----------------|------|-------|
| `PIC S9(n)V9(m)`, `COMP-3` arithmetic | `decimal.Decimal` with explicit truncation | Low | `float` is prohibited; COBOL truncates rather than rounds on `MOVE` |
| Picture-length alphanumerics, `SPACES`, group moves | Fixed-length `str`/`bytes` helpers in `clbs.runtime` | Medium | Space padding is observable through comparisons and returned messages |
| `88` level conditions, `EVALUATE`, `PERFORM` | Constants, `if/elif`, private functions | Low | Mechanical, one paragraph per function |
| Subprogram `CALL ... USING` | Function taking a mutable request object | Low | Mirrors CALL BY REFERENCE semantics |
| Sequential file I/O, `FILE STATUS` | Record codec over a byte stream, status codes preserved | Medium | Records are fixed length; no line endings on the mainframe |
| VSAM KSDS (`READ`/`REWRITE`/`START`/`DELETE`) | Keyed store abstraction over SQLite or an indexed file | Medium | Duplicate-key and not-found status codes must be reproduced |
| Embedded `EXEC SQL` + `SQLCA` | DB-API 2.0 calls behind a `SQLCA`-shaped result object | Medium | DB2 SQLCODEs must be mapped, including `+100` on not-found |
| `EXEC CICS` (`SEND MAP`, `RECEIVE`, `LINK`, `RETURN`, `HANDLE`) | Pseudo-conversational driver with COMMAREA and map dictionaries | High | Terminal semantics have no Python equivalent; adapter is a stub-level emulation |
| Checkpoint/restart, `ABEND` (`ILBOABN0`) | Explicit restart record plus exceptions | Medium | Behaviour under partial failure is part of the benchmark |
| JCL job steps, GDGs, `COND` | Job-runner script with step return codes | Medium | Dataset naming and disposition are simulated |
| EBCDIC collating sequence, `COMP-3` byte layout | Only relevant at the byte-codec boundary | Medium | Translated code works in ASCII; codecs are explicit about encoding |

**Verdict: feasible.** Tiers 1 and 2 are translatable today with a small runtime support
package and no third-party dependencies. Tier 3 needs a thin SQL adapter; the SQL itself is
standard enough to run against SQLite. Tier 4 is feasible only as a behavioural emulation:
the CICS programs can be translated faithfully at the business-logic level, but terminal
and transaction management are replaced by an adapter, so those pairs are scored on
business logic rather than on end-to-end behaviour.

## 4. Target architecture

Translations live in `translations/python/` and never modify the COBOL sources — the COBOL
side remains the reference implementation.

```
translations/python/
├── clbs/
│   ├── copybooks/   # one module per copybook (layouts, codes, constants)
│   ├── runtime/     # COBOL data semantics: picture moves, class tests, file/SQL adapters
│   ├── portfolio/   # one module per COBOL program, mirroring src/programs/<area>/
│   ├── batch/
│   ├── online/
│   └── utility/
├── tests/
│   ├── vectors/     # behaviour vectors recorded from the COBOL originals
│   └── test_<program>*.py
└── tools/
    └── cobol_bridge.py   # calls the compiled COBOL through libcob for differential tests
```

Rules:

- Standard library only in `clbs/`; `pytest`/`ruff` for development and GnuCOBOL for
  differential testing.
- No framework, no ORM, no code generation — a translation must be readable next to its
  COBOL original.
- Module, function and constant names keep the COBOL names (`PORTVALD` → `portvald`,
  `1000-VALIDATE-ID` → `_validate_id`, `VAL-INVALID-ACCT` → `VAL_INVALID_ACCT`).

## 5. Data type mapping

| COBOL | Python | Rule |
|-------|--------|------|
| `PIC X(n)` | `str` of exactly `n` characters | Left justified, space padded, truncated on overflow |
| `PIC 9(n)` | `int` (or `Decimal` when moved from text) | High-order digits are dropped, never an error |
| `PIC S9(n)V9(m)`, `COMP-3` | `Decimal` quantised to `m` places | Truncation toward zero; `float` is prohibited |
| `PIC S9(4) COMP` | `int` | Two-byte big-endian at the record boundary |
| `88` level | Module constant or predicate | Compared against the padded item, not the literal |
| Group item | `@dataclass` with picture-length fields | Field order matches the record layout |
| `OCCURS` | `list` of fixed length | Index base differs: COBOL is 1-based |
| `REDEFINES` | Codec function over the same bytes | No implicit aliasing in Python |

## 6. Behaviour preservation

A translation is scored on equivalence, not on quality of design. Concretely:

1. Reproduce the observable behaviour of the COBOL program, including latent defects.
2. Record every such defect in the module docstring and in the vector file, so the pair
   doubles as documentation of the trap.
3. Never "repair" validation, rounding or truncation while translating; repairs belong in
   the COBOL source, in a separate change.

`PORTVALD` alone contains four traps that a naive translation "fixes", and each one is a
scoring opportunity:

| Trap | Cause | Observable effect |
|------|-------|-------------------|
| No portfolio ID can pass validation | The 4-digit suffix is moved into `VAL-NUMERIC-CHECK PIC X(10)`, so `IS NUMERIC` sees trailing spaces | `PORT0001` returns `VAL-INVALID-ID` |
| No account number can pass validation | The class test runs on `LS-INPUT-VALUE PIC X(50)`, which is space padded | `1234567890` returns `VAL-INVALID-ACCT` |
| No amount can fail the range check | `VAL-MIN-AMOUNT`/`VAL-MAX-AMOUNT` are the extremes of the receiving `S9(13)V99` item | `99999999999999999` returns success after the MOVE truncates it |
| Unknown validation types report an ID error | The `WHEN OTHER` branch moves `VAL-INVALID-ID` | Validation type `Z` returns `1`, not a dedicated code |

## 7. Equivalence testing

Each pair ships a vector file under `tests/vectors/` holding the inputs and the outputs
recorded from the COBOL original, plus two tests:

- a unit test running the vectors against the Python translation;
- a differential test that compiles the COBOL program with GnuCOBOL (`cobc -m`), calls it
  through `libcob` with the raw linkage record and asserts field-for-field equality with
  the Python result. It is skipped when GnuCOBOL is absent.

The differential test is what makes the pair a benchmark rather than a sample: the expected
values cannot drift away from the COBOL, and a candidate translation produced by a
translation tool can be dropped in and scored with the same vectors.

For tier 2 and above the same approach applies at the artefact boundary: input datasets are
generated, both implementations run, and output datasets are compared byte for byte.

## 8. Migration strategy

| Wave | Content | Deliverable | Exit criteria |
|------|---------|-------------|---------------|
| 0 | Conventions plus one reference pair (`PORTVALD`) | `clbs.runtime.picture`, `clbs/copybooks/portval.py`, `clbs/portfolio/portvald.py`, vectors, differential harness | Differential test green; conventions documented (this plan) |
| 1 | Remaining pure logic and the validation/error/audit subprograms (`ERRPROC`, `AUDPROC`, `UTLVAL00`) | Record codecs for `ERRHAND`, `AUDITLOG`, `RTNCODE` | Pairs pass vectors; codecs round-trip fixed-length records |
| 2 | Sequential and VSAM programs (tier 2, 21 programs) | `clbs.runtime.files` (sequential + KSDS emulation, `FILE STATUS`) | Dataset-level equivalence for the portfolio, report and utility programs |
| 3 | DB2 programs (tier 3, 7 programs) | `clbs.runtime.sql` (`SQLCA`, SQLCODE mapping) running the existing DDL on SQLite | Same result sets and SQLCODEs, including `+100` and duplicate-key paths |
| 4 | CICS online programs (tier 4, 8 programs) and JCL job runner | `clbs.runtime.cics` COMMAREA/map adapter, step runner for `src/jcl` | Business-logic equivalence per screen flow; job steps reproduce return codes |

Waves are sequential because each one supplies the runtime that the next depends on, but
programs inside a wave are independent and can be translated in parallel.

## 9. First migration step

**Step 1 (done in this change): translate `PORTVALD` as the reference pair.** It is the only
tier 1 program, it exercises the picture, class-test and decimal-move semantics that every
later wave depends on, and it is small enough to review line by line against the COBOL.

Delivered:

- `translations/python/clbs/runtime/picture.py` — `alphanumeric`, `is_numeric`,
  `move_to_numeric`.
- `translations/python/clbs/copybooks/portval.py` — `PORTVAL.cpy` constants.
- `translations/python/clbs/portfolio/portvald.py` — the translation, one function per
  paragraph.
- `translations/python/tests/vectors/portvald.json` — 23 vectors recorded from GnuCOBOL,
  covering all four validation types, the `WHEN OTHER` branch and the four traps above.
- `translations/python/tools/cobol_bridge.py` plus the differential test.

**Step 2 (next): `ERRPROC` and `AUDPROC`.** Both are called by most tier 2 programs, so the
error and audit record codecs they need are prerequisites for the rest of Wave 1. Their
`FILE-CONTROL` usage is append-only sequential output, which is the simplest file semantics
in the suite.

## 10. Risks and open questions

| Risk | Mitigation |
|------|------------|
| Differential testing pins GnuCOBOL behaviour, not IBM Enterprise COBOL, where some moves are undefined | Vectors record the GnuCOBOL reference explicitly; behaviour that differs by compiler is called out in `clbs.runtime.picture` |
| Emulating VSAM and CICS can grow into a framework and dilute the benchmark | Runtime modules stay minimal and are only extended by what a translated program actually needs |
| Translated code may drift when COBOL sources change | Differential tests fail as soon as the COBOL changes; pairs are updated in the same change |
| Python translations could be mistaken for a modernisation target | `translations/` is documented as benchmark material and is never wired into the COBOL build |
