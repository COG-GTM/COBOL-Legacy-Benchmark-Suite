# COBOL-to-Python Migration Plan

Version: 1.0
Last Updated: 2026-08-10
Tracking ticket: [MBA-1423](https://cog-gtm.atlassian.net/browse/MBA-1423)

## 1. Purpose and Scope

CLBS exists to benchmark LLM translation of legacy COBOL. The README has so far named
Java and C# as target languages. This document adds Python as a third target and defines
how a Python translation of the suite is produced, verified, and published as benchmark
translation pairs.

Scope of this document:

- Feasibility assessment of translating the CLBS COBOL codebase to Python (section 3)
- Inventory of programs and copybooks in scope, grouped by migration difficulty (section 4)
- Target Python architecture and repository layout (section 5)
- Migration strategy, organised in waves (section 6)
- The first migration step, defined concretely enough to be picked up as-is (section 7)
- Verification approach and definition of a benchmark translation pair (sections 8-9)

Out of scope: producing the translated Python code itself, and any change to the COBOL
sources. The COBOL remains the reference implementation and is never modified to make a
translation easier — doing so would invalidate the benchmark.

## 2. Current Inventory

| Artefact | Count | Notes |
|---|---|---|
| COBOL programs (`src/programs/**/*.cbl`) | 38 | ~6,400 lines |
| Templates (`src/templates/**/*.cbl`) | 4 | Coding-standard skeletons, not runnable programs |
| Copybooks (`src/copybook/**/*.cpy`) | 20 | 4 batch, 10 common, 3 db2, 3 online |
| JCL (`src/jcl/**/*.jcl`) | 15 | Job orchestration and dataset allocation |
| BMS maps (`src/maps/*.bms`) | 1 | `INQSET` — 3270 screen set for the inquiry transaction |
| CICS resource definitions (`src/cics/*.csd`) | 1 | `PORTDFN` |
| DB2 DDL (`src/database/db2/*.sql`) | 5 | Tables, indexes, bind members |
| VSAM definitions | 1 | `src/database/vsam/vsam-definitions.txt` |

Platform dependencies measured across the 38 programs:

- 8 programs issue `EXEC CICS` (all under `src/programs/online/`)
- 14 programs issue `EXEC SQL` (DB2)
- 16 programs declare indexed (KSDS) files
- 4 copybooks declare `COMP-3` packed-decimal fields (`POSREC`, `TRNREC`, `PORTFLIO`, `DBTBLS`)

## 3. Feasibility Assessment

**Verdict: feasible, with the same shape of effort as the Java and C# targets, and one
extra area of care (fixed-point arithmetic).**

### 3.1 What maps cleanly

| COBOL construct | Python equivalent | Risk |
|---|---|---|
| `01`-level record layouts / copybooks | `dataclass` plus a byte-level codec generated from the PICTURE clauses | Low |
| `PERFORM` paragraphs, `EVALUATE`, `IF` | Functions, `match`/`if`, straight-line control flow | Low |
| `CALL 'SUBPROG' USING` (e.g. `PORTVALD`, `ERRPROC`, `AUDPROC`) | Module-level functions taking a request dataclass | Low |
| Sequential file I/O | Fixed-length record reads over a binary file object | Low |
| `DISPLAY`, report line building | `str.format` / f-strings over edited-picture helpers | Low |
| DB2 `EXEC SQL` static SQL | Parameterised SQL via DB-API 2.0 (`sqlite3` for the harness, `psycopg`/`ibm_db` for real targets) | Medium |
| `SQLCA` return-code handling | Exception-to-`sqlcode` translation layer | Medium |

### 3.2 What needs a runtime shim

1. **Fixed-point arithmetic.** `PIC S9(13)V9(2) COMP-3` is packed decimal with defined
   truncation semantics. Python's `float` cannot represent it. Every numeric field must be
   `decimal.Decimal` with an explicit `quantize()` on every store, mirroring the COBOL
   `PICTURE` scale, inside a `decimal.localcontext` using `ROUND_HALF_UP` / truncation to
   match `COMPUTE ... ROUNDED` versus unrounded `COMPUTE`. This is the single largest
   correctness risk and the reason the pilot in section 7 targets numeric validation first.
2. **VSAM KSDS.** 16 programs use keyed reads, `START`/`READ NEXT` browses, and file-status
   codes. A `KsdsFile` class over a sorted key index (backed by a plain file, `sqlite3`, or
   `dbm`) reproducing the two-character `FILE STATUS` codes covers all current usage.
3. **CICS.** The 8 online programs use `EXEC CICS SEND/RECEIVE MAP`, `LINK`, `RETURN
   TRANSID`, `HANDLE CONDITION`, and `COMMAREA` passing. These translate to a small
   pseudo-conversational driver: a `Commarea` dataclass, a map-render function per BMS map,
   and a transaction dispatch table. No CICS emulation product is required to run the
   benchmark, because the benchmark only needs deterministic output, not a 3270 terminal.
4. **JCL.** Job steps become a thin Python runner that sets DD-name-to-path bindings and
   invokes program entry points in order, propagating condition codes.

### 3.3 Why Python is a useful third target

- It exercises translation weaknesses that Java and C# do not: no native fixed-point type,
  no static typing enforced at runtime, and no direct analogue of COBOL's record/`REDEFINES`
  memory model. Divergences show up as silent value drift rather than compile errors, which
  is exactly the failure mode worth benchmarking.
- GnuCOBOL (`cobc`) is already usable in CI for a subset of programs, so COBOL-versus-Python
  differential testing can run on ordinary Linux runners without a mainframe.

## 4. Migration Scope by Difficulty

Programs are grouped by their platform dependencies. The wave plan in section 6 follows this
grouping.

**Tier 1 — no CICS, no DB2, no KSDS (8 programs).** `PORTVALD`, `PORTTEST`, `AUDPROC`,
`ERRPROC`, `POSUPDT`, `TSTGEN00`, `TSTVAL00`, `UTLMNT00`. Pure logic and sequential I/O;
translatable and testable today with GnuCOBOL as the oracle.

**Tier 2 — KSDS only (15 programs).** `BCHCTL00`, `CKPRST`, `PRCSEQ00`, `RCVPRC00`,
`RPTAUD00`, `RPTPOS00`, `RPTSTA00`, `PORTADD`, `PORTDEL`, `PORTMSTR`, `PORTREAD`, `PORTTRAN`,
`PORTUPDT`, `UTLMON00`, `UTLVAL00`. Require the `KsdsFile` shim and file-status parity.

**Tier 3 — DB2 (7 batch/common programs).** `HISTLD00` (also KSDS), `RTNANA00`, `RTNCDE00`,
`DB2CONN`, `DB2CMT`, `DB2ERR`, `DB2STAT`. Require the SQL layer and `SQLCA` shim; the DDL in
`src/database/db2/` provides the schema.

**Tier 4 — CICS online (8 programs).** `INQONLN`, `INQPORT`, `INQHIST`, `CURSMGR`, `DB2ONLN`,
`DB2RECV`, `ERRHNDL`, `SECMGR`, plus the `INQSET` BMS map and `PORTDFN` CSD. Require the
pseudo-conversational driver and map rendering.

**Copybooks** are migrated on demand with the first program that copies them; each becomes
one module under `python/clbs/records/`.

## 5. Target Python Architecture

```
python/
├── clbs/
│   ├── runtime/          # COBOL semantics shims
│   │   ├── decimal_.py   # PIC/COMP-3 scaling, MOVE/COMPUTE truncation, edited pictures
│   │   ├── records.py    # byte-level codec: PICTURE -> pack/unpack
│   │   ├── files.py      # sequential + KSDS access, two-char FILE STATUS codes
│   │   ├── db2.py        # DB-API wrapper exposing SQLCA-style status
│   │   └── cics.py       # COMMAREA, map send/receive, transaction dispatch
│   ├── records/          # one module per copybook (POSREC, TRNREC, PORTFLIO, ...)
│   ├── programs/         # one module per COBOL program, mirroring src/programs/ layout
│   └── jcl/              # job runners mirroring src/jcl/
└── tests/
    ├── unit/             # per-program behavioural tests
    └── parity/           # differential tests vs GnuCOBOL-compiled COBOL
```

Conventions:

- One Python module per COBOL program, same name lowercased (`PORTVALD` -> `portvald.py`),
  exposing a single entry point whose parameters mirror the `LINKAGE SECTION`.
- Paragraph names are preserved as function names (`_1000_validate_id`) so a reviewer can
  diff the translation against the COBOL paragraph-by-paragraph. Readability is deliberately
  traded for traceability — this is benchmark reference material, not idiomatic application
  code.
- No third-party runtime dependencies beyond the standard library for Tier 1 and Tier 2;
  `pytest` for tests. Tier 3 adds a DB-API driver.
- Python 3.11+.

## 6. Migration Strategy

Waves are ordered so that each one is verifiable before the next starts.

| Wave | Content | Exit criterion |
|---|---|---|
| 0 | `clbs.runtime.decimal_` and `clbs.runtime.records`; pilot translation of `PORTVALD` and its `PORTVAL` copybook | Differential tests pass against GnuCOBOL for all validation branches |
| 1 | Remaining Tier 1 programs, sequential file support | Byte-identical output files versus GnuCOBOL runs |
| 2 | `KsdsFile` shim, Tier 2 programs, report programs | Report output and file-status codes match |
| 3 | DB2 shim and Tier 3 programs against the DDL in `src/database/db2/` | Same result sets and `SQLCODE` handling on a seeded database |
| 4 | CICS driver, Tier 4 programs, `INQSET` map rendering | Screen buffers and `COMMAREA` transitions match documented behaviour |
| 5 | JCL runners, end-to-end DAILY-equivalent chain | Full chain reproduces the COBOL chain's outputs |

Each wave is a separate PR and a separate Jira ticket linked to MBA-1423.

## 7. First Migration Step (Wave 0)

The first step is deliberately narrow so the runtime foundations are proven before volume
translation starts.

**Deliverables**

1. `python/clbs/runtime/decimal_.py` — a `Picture` type parsed from a COBOL PICTURE string
   (`S9(11)V9(4) COMP-3` and friends) providing `decode(bytes) -> Decimal`,
   `encode(Decimal) -> bytes`, and `store(Decimal) -> Decimal` applying COBOL truncation.
2. `python/clbs/runtime/records.py` — a record codec that builds pack/unpack functions from
   a copybook field list, covering `PIC X(n)`, `PIC 9(n)`, `COMP`, `COMP-3`, group items, and
   `88`-level condition names.
3. `python/clbs/records/portval.py` — `src/copybook/common/PORTVAL.cpy` translated.
4. `python/clbs/programs/portfolio/portvald.py` — `PORTVALD` translated: the four validation
   branches (ID, account, type, amount) and their return codes and error messages.
5. `python/tests/parity/test_portvald.py` — differential test.

**Why `PORTVALD`** — it is a `CALL`ed subroutine with a `LINKAGE SECTION` interface, no
file, CICS, or DB2 dependency, it exercises `EVALUATE`, reference modification
(`LS-INPUT-VALUE(1:4)`), `IS NUMERIC` class tests and `88`-levels, and it already compiles
under GnuCOBOL (`cobc -m -I src/copybook/common -o build/PORTVALD.so
src/programs/portfolio/PORTVALD.cbl`), so an executable oracle exists on day one.

**Verification for Wave 0** — a pytest that, for a fixed corpus of inputs (valid IDs,
`PORT` prefix violations, non-numeric suffixes, 10-digit and malformed account numbers,
boundary amounts, unknown validation types), calls both the GnuCOBOL-compiled `PORTVALD.so`
and the Python function, and asserts equal `LS-RETURN-CODE` and `LS-ERROR-MSG`. The test
skips with a clear message when `cobc` is unavailable, so the suite still runs on machines
without GnuCOBOL.

**Definition of done** — deliverables merged, differential test green in CI, and
`documentation/technical/python-migration-plan.md` updated with any runtime semantics
discovered during the pilot.

## 8. Verification Strategy

- **Differential (preferred).** Run the GnuCOBOL build and the Python translation over the
  same inputs and compare outputs byte-for-byte. Applies to Tier 1 and Tier 2.
- **Golden files.** For programs that cannot be compiled off-mainframe (all CICS programs,
  and DB2 programs without a local DB2), record expected outputs derived from the COBOL
  source and the documentation, and assert against them.
- **Record-level round trip.** Every copybook codec must satisfy
  `encode(decode(b)) == b` over generated fixtures, which catches `COMP-3` sign-nibble and
  padding mistakes early.
- Test data comes from `documentation/operations/test-data-specs.md` and `TSTGEN00`.

## 9. Benchmark Translation Pairs

A Python translation pair is the unit that consumers of this benchmark actually load:

```
{cobol_source, copybooks[], python_reference, test_inputs, expected_outputs, metadata}
```

`metadata` records tier, COBOL features exercised (`COMP-3`, `EVALUATE`, `KSDS browse`,
`EXEC SQL`, ...), line counts, and whether an executable oracle exists. Pairs are emitted
per program as each wave lands, so the Python pair set grows monotonically and can be
published independently of the Java and C# targets.

## 10. Risks

| Risk | Mitigation |
|---|---|
| Silent decimal drift between COBOL and Python | `Decimal` everywhere with explicit `quantize` on store; round-trip and differential tests in Wave 0 before any volume translation |
| Python translations drift into idiomatic rewrites, weakening the benchmark | Paragraph-preserving convention (section 5) enforced in review |
| CICS and DB2 tiers have no local oracle | Golden-file verification and explicit metadata flagging pairs without an executable oracle |
| COBOL sources modified to ease translation | Prohibited; COBOL is the frozen reference implementation |
