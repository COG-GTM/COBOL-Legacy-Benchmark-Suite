# COBOL-to-Python Migration Plan

Version: 1.0
Status: Approved for Wave 1
Applies to: COBOL Legacy Benchmark Suite (CLBS), Investment Portfolio Management System

## 1. Purpose

The suite exists to benchmark modernization tooling, and until now it has only
named Java and C# as target languages. Python is the language most translation
tools are evaluated in first, and it is the language where the gap between a
*plausible* translation and a *behaviour-preserving* one is widest: dynamic
typing, binary floating point and variable-length strings quietly absorb the
COBOL semantics that a correct translation must keep.

This document assesses whether the suite can be translated to Python, defines
how that translation is organised and verified, and specifies the first
migration step. It does not propose replacing the COBOL sources; the COBOL
remains the specification against which Python translations are graded.

## 2. Scope

### 2.1 Inventory

| Group                    | Files | Notes                                                                 |
|--------------------------|-------|-----------------------------------------------------------------------|
| `src/programs/batch`     | 11    | Sequential/report processing, checkpoint-restart, two DB2 programs      |
| `src/programs/common`    | 6     | Error, audit and DB2 service subprograms                                |
| `src/programs/online`    | 8     | CICS transactions, all with `EXEC CICS`, most with `EXEC SQL`           |
| `src/programs/portfolio` | 8     | VSAM KSDS maintenance, validation, transaction posting                  |
| `src/programs/test`      | 2     | Test data generation and validation                                     |
| `src/programs/utility`   | 3     | File maintenance, monitoring, data validation                           |
| `src/copybook`           | 20    | 4 batch, 10 common, 3 DB2, 3 online                                     |
| `src/templates`          | 4     | Coding templates, not executable programs                               |

38 programs, roughly 7,100 lines of COBOL. Subsystem coupling: 8 programs use
CICS, 14 use embedded SQL, 6 open VSAM indexed files, and the remainder are
sequential-file or pure-linkage subprograms.

### 2.2 Out of scope

JCL, BMS maps, CICS CSD definitions, DB2 DDL and VSAM cluster definitions are
not translated. They are inputs to the target design (job orchestration, screen
flow, schema) and are covered by the port descriptions in section 5.3.

## 3. Feasibility assessment

**Verdict: feasible, with the qualification that fidelity comes from a runtime
layer and an oracle, not from the Python language itself.** Every COBOL
construct used in the suite has a Python expression; none of them has a
*default* Python expression that is correct.

### 3.1 Construct mapping

| COBOL construct                    | Python approach                                                                 | Risk |
|------------------------------------|---------------------------------------------------------------------------------|------|
| `PIC S9(n)V9(m)`, `COMP-3`         | `decimal.Decimal` with explicit precision and truncation; never `float`           | High |
| `PIC X(n)` fields and moves        | fixed-width helpers that pad and truncate on assignment                           | High |
| `IS NUMERIC` class test            | explicit digit test over every character position, including padding              | High |
| Alphanumeric-to-numeric `MOVE`     | emulated conversion pinned to an oracle; dialect dependent                        | High |
| Level 88 condition names           | module constants or `enum` members                                                | Low  |
| `REDEFINES`, `OCCURS`              | record codec that reads a byte layout into typed views                            | Medium |
| `PERFORM ... THRU`, `GO TO`        | functions per paragraph; the few `GO TO` uses become structured control flow      | Medium |
| `LINKAGE SECTION` + `CALL USING`   | dataclass mutated in place by the entry point                                     | Low  |
| `INSPECT`, `STRING`, `UNSTRING`    | string helpers in the runtime layer, not ad hoc Python slicing                    | Medium |
| VSAM KSDS `READ`/`REWRITE`/`START` | repository port over an indexed store, with COBOL file status codes preserved     | Medium |
| `EXEC SQL`                         | parameterized SQL through a repository port; `SQLCA` fields mapped from the driver| Medium |
| `EXEC CICS SEND/RECEIVE/RETURN`    | presentation port with explicit pseudo-conversational state                       | High |
| Checkpoint/restart, `COMMIT`       | explicit unit-of-work object around the repository ports                          | Medium |

### 3.2 Baseline findings

Establishing the oracle exposed the state of the corpus. These findings shape
the roadmap and are the reason Wave 0 exists.

1. Only 2 of the 37 non-empty program files (`PORTVALD`, `PORTREAD`) pass
   `cobc -fsyntax-only` unchanged. The 35 failures cluster into four groups:
   - 9 source-format problems: 7 files carry a one-column shift on their first
     comment line, so GnuCOBOL reads the banner as code, and 2 files
     (`ERRHNDL`, `PORTTEST`) use a different column convention altogether;
   - 12 files fail on `src/copybook/db2/SQLCA.cpy`, which contains
     `EXEC SQL INCLUDE SQLCA END-EXEC` and therefore needs a SQL precompiler;
   - 4 files copy members that do not exist in the repository (`PORTREC`,
     `SQLPOS`, and `DB2STAT`, which is a program rather than a copybook);
   - 10 are genuine COBOL defects: missing `FD` entries, undefined identifiers,
     `PICTURE` clauses missing on group items, an invalid copybook layout.
2. `src/programs/batch/POSUPDT.cbl` is empty, although the README lists
   `POSUPD00` as an implemented batch component.
3. Behavioural defects exist in code that compiles. `PORTVALD` rejects every
   portfolio ID and every account number, and accepts every amount, for the
   reasons recorded in the pair's divergence notes. Preserving such behaviour is
   exactly what distinguishes a benchmark translation from a rewrite.

These are properties of the corpus, not blockers for Python. They do mean that
a program cannot be translated before it can be executed, because without
execution there is no oracle.

## 4. Strategy

The migration is **behaviour-preserving and oracle-verified**, executed one
translation pair at a time. It is not a re-architecture: no program is merged,
split or "modernised" while being translated. Design improvements, if any, are
separate work that starts from a passing translation.

A **translation pair** consists of:

1. the COBOL source (unchanged, in `src/`);
2. a Python module under `translations/python/src/clbs/`, mirroring the COBOL
   path so the mapping is mechanical;
3. an oracle driver under `translations/python/oracle/` that calls the COBOL
   program with a case file and records its results;
4. a committed golden file produced by that driver;
5. parity tests that fail unless the Python module reproduces the golden
   results exactly;
6. divergence notes for anything the translation cannot reproduce.

Golden files are committed so the tests run without a COBOL toolchain, while
the driver and the generator script keep them reproducible.

## 5. Target design

### 5.1 Runtime layer

`clbs.runtime` holds COBOL semantics: fixed-width moves, class tests, numeric
conversions, record codecs, file status codes. Program modules contain business
logic only, and any semantic surprise is fixed once, in the runtime, for every
pair.

### 5.2 Program modules

One module per program; one function per paragraph, keeping the paragraph name
in the docstring for traceability; the linkage record as a dataclass mutated in
place. Copybooks become modules of constants and record definitions, imported
wherever the COBOL program has a `COPY`.

### 5.3 Subsystem ports

| Subsystem | Port                                                                  | First implementation |
|-----------|-----------------------------------------------------------------------|----------------------|
| VSAM KSDS | key-addressed repository returning records plus COBOL file status      | SQLite-backed store, byte-faithful record codec |
| DB2       | repository executing parameterized SQL and populating an `SQLCA` view  | SQLite for tests, PostgreSQL for realism |
| CICS      | presentation port with explicit pseudo-conversational state            | in-process driver replaying `COMMAREA` state |
| JCL       | step runner invoking program entry points with the same DD-to-file map | pytest fixtures, later a workflow definition |

Ports are introduced by the wave that first needs them, not up front.

## 6. Roadmap

| Wave | Content | Exit criteria |
|------|---------|---------------|
| 0 | Oracle baseline: raise per-file tickets for the corpus findings in 3.2, add a SQL precompilation path, replace missing copybooks | every in-scope program compiles and can be driven by a case file |
| 1 | Pure-logic subprograms: `PORTVALD`, `ERRPROC`, `AUDPROC`, `CKPRST`, `BCHCTL00`, `PRCSEQ00`, `RCVPRC00` | pairs pass parity; runtime layer covers moves, class tests and numeric conversion |
| 2 | File-based programs: portfolio maintenance, reports, utilities, test data | record codec is byte-faithful for `COMP-3` and signed display fields; VSAM port implements the file status codes the programs branch on |
| 3 | DB2 programs: `DB2*` services, `HISTLD00`, `RTNANA00`, `RTNCDE00`, `RPTSTA00` | `SQLCA` handling and commit/rollback boundaries match the COBOL paths |
| 4 | CICS online: `INQONLN`, `INQPORT`, `INQHIST`, `SECMGR`, `CURSMGR`, `DB2ONLN`, `DB2RECV`, `ERRHNDL` | pseudo-conversational flows reproduce screen state transitions from a recorded session |

Waves are ordered by how much infrastructure a pair needs, so each wave leaves
behind the runtime capability the next one depends on.

## 7. First migration step

**Completed:** the `PORTVALD` pair, in `translations/python/`. It is the
smallest program with a linkage-only interface, so it establishes the pair
mechanics without needing any port.

Delivered: the Python translation, the `PVDRIVR` oracle driver, a 30-case case
file, the generated golden file, and 45 tests (30 parity cases plus runtime
checks). The pair also demonstrates the point of the exercise: a plausible
intent-based translation of `PORTVALD` (regex `PORT\d{4}`, ten-digit account
numbers, parsed decimal amounts) disagrees with the COBOL program on 8 of the
30 cases, because the program rejects the inputs its comments describe as valid
and accepts inputs that are not numbers at all.

**Next:** `ERRPROC` and `AUDPROC`, which extend the runtime layer with the
audit and error record layouts while remaining linkage-only. Both currently
fail to compile, so the Wave 0 tickets for those two files are their
prerequisite.

## 8. Risks

| Risk | Impact | Mitigation |
|------|--------|-----------|
| GnuCOBOL is the oracle, z/OS is the specification | dialect-specific behaviour, notably alphanumeric-to-numeric moves and EBCDIC collation, can be encoded as truth | every dialect-sensitive behaviour is documented in divergence notes and re-verified before a pair depends on it |
| EBCDIC versus ASCII ordering | `SORT` and key ranges can differ | keep comparisons in the runtime layer so a collation switch is a single change |
| Corpus defects | translations preserve defects, which can look like translation errors | golden files make the behaviour explicit and reviewable |
| Scope creep into rewriting | benchmark value is lost if the Python code stops mirroring the COBOL | pair review checks structural correspondence, not only test results |

## 9. Open questions

1. Is a z/OS or Micro Focus oracle available for cross-checking the dialect
   sensitive behaviour listed in section 8? Until then GnuCOBOL 3.1.2 is the
   recorded oracle version.
2. Should Wave 0 fix the corpus defects in place, or should the benchmark ship
   both the defective and the corrected sources as separate cases?
3. Should the Python translations eventually publish an intent-based variant
   alongside the faithful one, so tooling can be scored on both?
