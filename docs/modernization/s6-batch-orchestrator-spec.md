# S6 batch-orchestrator — Modernization Specification (Phase 0)

Component: **S6 batch-orchestrator** (per `cobol-java-modernization-plan.md` §2, row S6)
Target: Spring Batch job repository + scheduler; checkpoint/restart → Spring Batch restart semantics.

Sources analyzed (every file read in full):

| File | Lines | Role |
|---|---|---|
| `src/programs/batch/BCHCTL00.cbl` | 127 | Batch Control Processor — skeleton only (see §5) |
| `src/programs/batch/PRCSEQ00.cbl` | 345 | Process Sequence Manager — implemented |
| `src/programs/batch/RCVPRC00.cbl` | 302 | Process Recovery Handler — implemented |
| `src/programs/batch/CKPRST.cbl` | 57 | Checkpoint/Restart — stub (empty paragraphs) |
| `src/copybook/batch/BCHCON.cpy` | 65 | Batch control constants |
| `src/copybook/batch/BCHCTL.cpy` | 49 | Batch control file record (VSAM `BCHCTL`) |
| `src/copybook/batch/CKPRST.cpy` | 76 | Checkpoint control area + checkpoint VSAM record |
| `src/copybook/batch/PRCSEQ.cpy` | 75 | Process sequence record (VSAM `PRCSEQ`) + standard sequences |
| `src/jcl/RTNANA.jcl` | 10 | Runs `RTNANA00` (return-code analysis report) |
| `src/jcl/batch/README.md` | 0 | Empty file |
| `documentation/technical/data-dictionary.md` §2.4–2.6, §7–9 | — | Documented control/checkpoint layouts, RCs, schedule |
| `documentation/technical/system-architecture.md` §1.2.1, §2.1, §4, §5.1.1, §6.2 | — | Context only |

Conventions used below: `file:line` refs point at the COBOL source. Rules that come only from documentation and have no code behind them are tagged **INFERRED FROM DOCS**. Where code is absent or empty this is stated explicitly.

---

## 1. Purpose & scope

The batch orchestrator is the nightly "job scheduler inside the job stream" for the investment-portfolio system. In business terms it:

1. **Builds the nightly run plan.** Given a processing date and a sequence type (initial / main processing / reports / termination), it reads the process-definition catalogue (`PRCSEQ` file) and creates one control record per process for that date, all in status READY (`PRCSEQ00` INIT).
2. **Hands out the next runnable process.** It finds the first READY process in plan order, verifies that every declared upstream dependency has finished with an acceptable return code, and if so marks the process ACTIVE and stamps its start time (`PRCSEQ00` NEXT).
3. **Tracks completion.** It re-reads a process's control record, mirrors status/RC into its in-memory plan, and counts how many processes are still ACTIVE or in ERROR; at the end of the run it rolls these up into a single job return code 0 / 4 / 8 (`PRCSEQ00` STAT/TERM).
4. **Recovers failed processes.** For one process, all processes of a date, or the whole control file, it decides RESTART / BYPASS / TERMINATE based on the process's restartability flag and restart-attempt count, and rewrites the control record accordingly (`RCVPRC00`).
5. **Is intended to** provide per-job control-record lifecycle (`BCHCTL00`) and program-level checkpoint/restart (`CKPRST`), but neither is implemented — only the dispatch skeletons and copybook layouts exist.

Out of scope for S6 (owned elsewhere): the business programs the orchestrator schedules (S2 transaction, S3 position, S7 reporting), the error logger `ERRPROC` and return-code catalogue `RTNCODES`/`RTNANA00` (S8 platform-common).

---

## 2. Business rules

Numbering is `BR-<program>-<n>`. Each rule cites the paragraph and lines that implement it. Return-code constants: `BCT-RC-SUCCESS=+0`, `BCT-RC-WARNING=+4`, `BCT-RC-ERROR=+8`, `BCT-RC-SEVERE=+12`, `BCT-RC-CRITICAL=+16` (`BCHCON.cpy:17-21`). Status constants: `R`eady, `A`ctive, `W`aiting, `D`one, `E`rror (`BCHCON.cpy:9-13`, `BCHCTL.cpy:16-20`).

### 2.1 BCHCTL00 — Batch Control Processor (`BCHCTL00.cbl`)

Only the dispatcher and error routine are coded. Every "detailed procedure" it PERFORMs is listed as *to be implemented* (`BCHCTL00.cbl:116-127`) and **does not exist in the source**. The program would not compile as-is (PERFORM of undefined paragraphs).

| # | Rule | Ref |
|---|---|---|
| BR-BCHCTL-1 | The caller passes a 4-char function code. Accepted values: `INIT`, `CHEK`, `UPDT`, `TERM`. Any other value raises error text `Invalid function code` and returns RC 8. | `BCHCTL00.cbl:49-53, 61-77, 111-115` |
| BR-BCHCTL-2 | `INIT` sets internal mode `I` and is meant to: open the control file, read the control record, validate the process, and update the start status. **Paragraphs 1100/1200/1300/1400 are not implemented; behaviour unknown.** | `BCHCTL00.cbl:62-64, 83-88, 118-121` |
| BR-BCHCTL-3 | `CHEK` reads the control record, checks dependencies, and returns RC 0 if `PREREQS-SATISFIED` (`WS-PREREQ-MET='Y'`) else RC 4 (`WARNING`). **Paragraphs 2100/2200 that would set `WS-PREREQ-MET` are not implemented**, so the flag is never set in existing code. | `BCHCTL00.cbl:38-40, 90-98, 122` |
| BR-BCHCTL-4 | `UPDT` reads the control record, updates the process status and writes it back. **Paragraphs 3100/3200/3300 not implemented.** | `BCHCTL00.cbl:100-104, 123-124` |
| BR-BCHCTL-5 | `TERM` updates completion info and closes files. **Paragraphs 4100/4200 not implemented.** | `BCHCTL00.cbl:106-109, 125-126` |
| BR-BCHCTL-6 | Error path: `ERR-PROGRAM='BCHCTL00'`, `LS-RETURN-CODE=8`, then `CALL 'ERRPROC' USING ERR-MESSAGE`. Control does **not** stop after the error; execution falls through to `GOBACK`. | `BCHCTL00.cbl:111-115` |
| BR-BCHCTL-7 | On exit the linkage return code is copied to the job `RETURN-CODE` special register, so the caller's step RC equals `LS-RETURN-CODE`. `LS-RETURN-CODE` is never initialised by this program; for `INIT`/`UPDT`/`TERM` it echoes whatever the caller passed in. | `BCHCTL00.cbl:79-80` |
| BR-BCHCTL-8 | Control file `BCHCTL` is VSAM KSDS, dynamic access, key `BCT-KEY` (job name + process date + sequence no). | `BCHCTL00.cbl:17-22`, `BCHCTL.cpy:10-13` |

Documented-but-uncoded intent for BCHCTL00 (**INFERRED FROM DOCS**): the system-architecture diagram §2.1 says the scheduler initialises the control record, starts the program, and completion "signals next job"; §4.2 says on start the program reads `BCHCTL`, and if the status is *Restart* it reads the checkpoint, otherwise initialises. None of this is in code.

### 2.2 PRCSEQ00 — Process Sequence Manager (`PRCSEQ00.cbl`)

| # | Rule | Ref |
|---|---|---|
| BR-PRCSEQ-1 | Function code dispatch: `INIT` → build sequence; `NEXT` → get next process; `STAT` → check status; `TERM` → terminate. Any other value → `Invalid function code`, RC 8. Linkage RC is copied to `RETURN-CODE` on exit. | `PRCSEQ00.cbl:64-68, 76-92` |
| BR-PRCSEQ-2 | `INIT`: open `PRCSEQ` and `BCHCTL` both I-O; a non-`00` file status on either raises `Error opening sequence file` / `Error opening control file` (RC 8) but processing continues. | `PRCSEQ00.cbl:94-98, 141-153` |
| BR-PRCSEQ-3 | Build sequence: clear the 100-entry in-memory process table and count; move the 8-char `LS-PROCESS-DATE` into the 10-byte `PSR-KEY` (so `PSR-PROCESS-ID` receives the date text and `PSR-VERSION` is space-padded) and `START ... KEY >= PSR-KEY`. Invalid key → `No sequence found for date`, RC 8. | `PRCSEQ00.cbl:155-166` |
| BR-PRCSEQ-4 | Then read forward until end-of-file (status `10`). Every record whose `PSR-TYPE` equals the requested `LS-SEQUENCE-TYPE` (3 chars) is appended to the plan; other types are skipped. There is no date filter on the record itself, and no bound check against the 100-entry table. | `PRCSEQ00.cbl:168-178, 55-60` |
| BR-PRCSEQ-5 | Appending: `WS-PROCESS-COUNT += 1`; entry gets `PSR-PROCESS-ID`, sequence number = running count (1-based, catalogue read order), status `R`. Plan order = physical key order of the `PRCSEQ` file, **not** `PSR-START-TIME`, `PSR-ACTIVE-DAYS`, `PSR-FREQ` or the standard-sequence constants (those fields are never read). | `PRCSEQ00.cbl:180-186` |
| BR-PRCSEQ-6 | Create control records: for each plan entry write a fresh `BATCH-CONTROL-RECORD` with `BCT-JOB-NAME=process id`, `BCT-PROCESS-DATE=LS-PROCESS-DATE`, `BCT-SEQUENCE-NO=plan seq`, `BCT-STATUS='R'`; all other fields are `INITIALIZE`d (numerics 0, alphanumerics spaces). Duplicate key / write failure → `Error creating control record`, RC 8, loop continues. | `PRCSEQ00.cbl:188-204` |
| BR-PRCSEQ-7 | `NEXT`: find the first entry (lowest seq) with status `R` and return its id in `LS-NEXT-PROCESS`; if none, return spaces. | `PRCSEQ00.cbl:100-106, 206-219` |
| BR-PRCSEQ-8 | Dependency check: read the `PRCSEQ` definition for `LS-NEXT-PROCESS` (only `PSR-PROCESS-ID` is set; `PSR-VERSION` keeps the previous record's value). Not found → `Process definition not found`, RC 8. When `LS-NEXT-PROCESS` is spaces (no ready process) this read is still attempted and fails with the same error. | `PRCSEQ00.cbl:221-228` |
| BR-PRCSEQ-9 | For each of the `PSR-DEP-COUNT` dependencies (max 10), read the dependency's `BCHCTL` record by (`PSR-DEP-ID`, `LS-PROCESS-DATE`); `BCT-SEQUENCE-NO` is **not** set and retains whatever the record buffer held. Not found → `Dependency record not found`, RC 8. Stop at the first dependency that sets a non-zero RC. | `PRCSEQ00.cbl:230-247` |
| BR-PRCSEQ-10 | Dependency evaluation: if the dependency is **not** `D`one: a **hard** dependency (`PSR-DEP-TYPE='H'`) yields RC 4 (wait); a soft dependency yields no change. If it **is** done: `BCT-RETURN-CODE > PSR-DEP-RC` (the max RC tolerated for that dependency) yields RC 8. Comparison is signed integer, strictly greater. | `PRCSEQ00.cbl:249-257`, `PRCSEQ.cpy:26-31` |
| BR-PRCSEQ-11 | Only when the RC is still 0 after the dependency check is the process started: read its `BCHCTL` record (by name + date; sequence no. not set) — not found → `Process record not found`, RC 8 — set `BCT-STATUS='A'`, `ACCEPT WS-CURRENT-TIME FROM TIME STAMP` and move the 26-char timestamp into the 8-char `BCT-START-TIME` (truncated to its first 8 characters), then `REWRITE`. Rewrite failure → `Error updating control record`, RC 8. | `PRCSEQ00.cbl:103-105, 260-279` |
| BR-PRCSEQ-12 | `LS-RETURN-CODE` is never reset to zero inside `NEXT`; the decision `IF LS-RETURN-CODE = ZERO` depends on the caller passing 0 in. | `PRCSEQ00.cbl:103` |
| BR-PRCSEQ-13 | `STAT`: read the `BCHCTL` record of `LS-NEXT-PROCESS` for the date (not found → `Process record not found`, RC 8), then copy its `BCT-STATUS` and `BCT-RETURN-CODE` into the matching in-memory plan entry (first match on process id). | `PRCSEQ00.cbl:108-112, 281-303` |
| BR-PRCSEQ-14 | Completion count: `WS-ACTIVE-COUNT` = number of plan entries with status `A`; `WS-ERROR-COUNT` = entries with status `E`. Entries `R`, `W`, `D` are not counted. `STAT` itself does not change `LS-RETURN-CODE`. | `PRCSEQ00.cbl:305-320` |
| BR-PRCSEQ-15 | `TERM` final RC: any error → 8; else any active → 4; else 0. READY (never-started) processes do **not** affect the final RC. | `PRCSEQ00.cbl:114-117, 322-334` |
| BR-PRCSEQ-16 | Close both files; any non-`00` status → `Error closing files`, RC 8. | `PRCSEQ00.cbl:336-345` |
| BR-PRCSEQ-17 | In-memory plan state (`WS-PROCESS-TABLE`) lives only in WORKING-STORAGE; it is valid across `NEXT`/`STAT`/`TERM` calls only if the same load module stays resident between calls in one run unit. A separate job step calling `NEXT` without a prior `INIT` sees an empty table (count 0 → `LS-NEXT-PROCESS` = spaces). | `PRCSEQ00.cbl:47-60` |
| BR-PRCSEQ-18 | Error routine: `ERR-PROGRAM='PRCSEQ00'`, RC 8, `CALL 'ERRPROC'`; execution continues after the call (no GOBACK/STOP RUN). | `PRCSEQ00.cbl:119-123` |
| BR-PRCSEQ-19 | The loop variable `WS-SUB` used in 2200/2210 is **not declared** anywhere in PRCSEQ00 or its copybooks (`BCHCON`, `ERRHAND`, `BCHCTL`, `PRCSEQ`) — a compile error in the current source. | `PRCSEQ00.cbl:230-240` |

### 2.3 RCVPRC00 — Process Recovery Handler (`RCVPRC00.cbl`)

| # | Rule | Ref |
|---|---|---|
| BR-RCVPRC-1 | Function dispatch: `INIT`, `RECV`, `TERM`; other → `Invalid function code`, RC 8; linkage RC copied to `RETURN-CODE`. | `RCVPRC00.cbl:60-63, 72-86` |
| BR-RCVPRC-2 | `INIT` opens `BCHCTL` I-O and `PRCSEQ` INPUT (read-only); open failures → `Error opening control file` / `Error opening sequence file`, RC 8. | `RCVPRC00.cbl:88-92, 118-130` |
| BR-RCVPRC-3 | Validation: `LS-PROCESS-DATE` must not be spaces (`Process date required`, RC 8). `LS-RECOVERY-TYPE` must be `P` (process), `S` (sequence/date) or `A` (all); otherwise `Invalid recovery type`, RC 8. No format check on the date. | `RCVPRC00.cbl:132-147` |
| BR-RCVPRC-4 | Recovery mode = recovery type. For mode `P`, `LS-PROCESS-ID` must not be spaces (`Process ID required for process recovery`, RC 8). | `RCVPRC00.cbl:149-157` |
| BR-RCVPRC-5 | `RECV` dispatches on the mode saved by `INIT`: `P` → single process, `S` → all processes of the date, `A` → all records. An unset mode (RECV without INIT) does nothing. | `RCVPRC00.cbl:94-103` |
| BR-RCVPRC-6 | Recover one process: read `BCHCTL` by (`LS-PROCESS-ID`, `LS-PROCESS-DATE`) — sequence no. not set — not found → `Process record not found`, RC 8; then determine action and execute it. | `RCVPRC00.cbl:159-171` |
| BR-RCVPRC-7 | Determine action: read `PRCSEQ` definition by `PSR-PROCESS-ID` (version not set) — not found → `Process definition not found`, RC 8. If `PSR-RESTART='Y'` → **RESTART**. Otherwise, if `BCT-RESTART-COUNT > BCT-MAX-RESTARTS` (constant 3) → **TERMINATE**, else → **BYPASS**. Note the restart-count ceiling is only consulted for *non-restartable* processes; a restartable process is restarted without limit. | `RCVPRC00.cbl:173-191`, `BCHCON.cpy:26`, `PRCSEQ.cpy:36-38` |
| BR-RCVPRC-8 | RESTART: `BCT-STATUS='R'`, `BCT-RESTART-COUNT += 1` (PIC 9(2) COMP, unsigned, no overflow check — wraps/truncates at 100 per COBOL truncation rules), `BCT-ATTEMPT-TS` = current 26-char timestamp; `REWRITE`. Return code and error description are left unchanged. | `RCVPRC00.cbl:204-215` |
| BR-RCVPRC-9 | BYPASS: `BCT-STATUS='D'`, `BCT-RETURN-CODE=4`, `BCT-ERROR-DESC='Process bypassed by recovery'`; `REWRITE`. A bypassed process therefore satisfies downstream dependencies whose `PSR-DEP-RC >= 4`. | `RCVPRC00.cbl:217-227` |
| BR-RCVPRC-10 | TERMINATE: `BCT-STATUS='E'`, `BCT-RETURN-CODE=8`, `BCT-ERROR-DESC='Process terminated by recovery'`; `REWRITE`. | `RCVPRC00.cbl:229-239` |
| BR-RCVPRC-11 | Any `REWRITE` failure → `Error updating control record`, RC 8. | `RCVPRC00.cbl:210-214, 222-226, 234-238` |
| BR-RCVPRC-12 | Recover sequence (mode `S`): position with `BCT-PROCESS-DATE=LS-PROCESS-DATE`, `BCT-JOB-NAME=LOW-VALUES`, `START KEY > BCT-KEY` (invalid → `No processes found for date`, RC 8), then read forward to EOF and, for each record whose `BCT-PROCESS-DATE` equals the request date, perform the single-process recovery. Because the key's leading field is job name, the browse effectively scans the whole file. **Defect:** the single-process recovery re-reads by `LS-PROCESS-ID` (the caller's input, not the browsed record's `BCT-JOB-NAME`), so every match recovers the same input process, and the random READ repositions the browse. | `RCVPRC00.cbl:241-261, 160` |
| BR-RCVPRC-13 | Recover all (mode `A`): `START KEY > LOW-VALUES` (invalid → `No processes found`, RC 8), read forward to EOF, set `LS-PROCESS-ID=BCT-JOB-NAME` and recover each record (no date filter; the request date is still used for the re-read key, so records of other dates fail with `Process record not found`). | `RCVPRC00.cbl:263-281` |
| BR-RCVPRC-14 | `TERM`: if `LS-RETURN-CODE = 0` log `Recovery completed successfully`, else `Recovery completed with errors`, via `CALL 'ERRPROC'` (informational use of the error logger); then close both files (`Error closing files`, RC 8 on failure). | `RCVPRC00.cbl:105-108, 283-302` |
| BR-RCVPRC-15 | Error routine: `ERR-PROGRAM='RCVPRC00'`, RC 8, `CALL 'ERRPROC'`; execution continues. | `RCVPRC00.cbl:110-114` |
| BR-RCVPRC-16 | `LS-RECOVERY-PARM` (50 chars) is accepted but never read. | `RCVPRC00.cbl:67` |

### 2.4 CKPRST — Checkpoint/Restart (`CKPRST.cbl`, `CKPRST.cpy`)

| # | Rule | Ref |
|---|---|---|
| BR-CKPRST-1 | Entry dispatch on conditions `ENTRY-POINT-INIT` / `-TAKE` / `-COMMIT` / `-RESTART`. **These condition names are not defined in `CKPRST.cpy` or `RETHND.cpy`**; the program cannot compile. No `WHEN OTHER`. | `CKPRST.cbl:29-38` |
| BR-CKPRST-2 | All four procedures (`PROC-INIT`, `PROC-TAKE-CHECKPOINT`, `PROC-COMMIT-CHECKPOINT`, `PROC-RESTART`) are **empty** — a comment line and a period. No checkpoint is ever written or read. | `CKPRST.cbl:43-57` |
| BR-CKPRST-3 | Checkpoint file `CKPTFILE` is VSAM KSDS, key `CKR-KEY` = program id + run date; payload is an opaque 400-byte `CKR-DATA`. | `CKPRST.cbl:7-12`, `CKPRST.cpy:52-56` |
| BR-CKPRST-4 | `COPY CKPRST` appears in both the FD and LINKAGE SECTION, duplicating every `CK-*`/`CKR-*` name (ambiguous references — compile error). | `CKPRST.cbl:17, 23` |
| BR-CKPRST-5 | Copybook defaults (the only executable-ish semantics): commit frequency 1000 records, max errors 100, max restarts 3; restart modes `N`ormal/`R`estart/re`C`over; phases `00` init, `10` read, `20` proc, `30` update, `40` term; up to 5 tracked files each with name, 50-char position and 2-char status. | `CKPRST.cpy:24-47` |
| BR-CKPRST-6 | The copybook documents four *separate* entry programs `CKPINIT`/`CKPTAKE`/`CKPCMIT`/`CKPRSTR`; none exist in the repo and nothing calls them (or `CKPRST`). | `CKPRST.cpy:59-76` |
| BR-CKPRST-7 (**INFERRED FROM DOCS**) | Checkpoint frequency: transaction processor every 1000 records, position update every 500, history load every 1000, minimum interval 2 minutes. Restart resumes from the last checkpoint position. | data-dictionary §8.3; system-architecture §4.2, §6.2.1 |

### 2.5 BCHCON constants (`BCHCON.cpy`) — rules implied by constants that no code uses

| # | Rule | Ref |
|---|---|---|
| BR-BCHCON-1 | Max prerequisites 10 (matches `OCCURS 10` in `BCHCTL`/`PRCSEQ`); max restarts 3 (used by `RCVPRC00`); wait interval 300 s and max wait 3600 s (**never referenced** — polling loop not implemented). | `BCHCON.cpy:25-28` |
| BR-BCHCON-2 | Process types `INI`/`UPD`/`RPT`/`CLN`; dependency types `R`equired/`O`ptional/e`X`clusive; record types `C`/`P`/`D`/`H`; reserved process names `STARTDAY`, `ENDDAY`, `EMERGENCY` (note: `'EMERGENCY'` is 9 chars in a PIC X(8) — truncated to `EMERGENC`); four 30-char messages. **None are referenced by any program.** | `BCHCON.cpy:31-65` |

### 2.6 Scheduling rules present only in documentation (**INFERRED FROM DOCS**)

| # | Rule | Ref |
|---|---|---|
| BR-DOC-1 | Nightly chain: `TRNVAL00` → `POSUPD00` → `HISTLD00` → `RPTGEN00`, each successor requires predecessor RC ≤ 4 (data dictionary says RPTGEN00 has "None"; SAD §4.1 says RC ≤ 4). | data-dictionary §8.1, §9.1; system-architecture §4.1 |
| BR-DOC-2 | Time windows: TRNVAL00 18:00–18:15 (day must be open), POSUPD00 18:15–19:00, HISTLD00 19:00–19:30, RPTGEN00 19:30–20:00. | data-dictionary §9.1 |
| BR-DOC-3 | Restartable: TRNVAL00, POSUPD00, HISTLD00 yes; RPTGEN00 no. | data-dictionary §8.1 |
| BR-DOC-4 | Return code meanings: 0 continue; 4 review warnings; 8 review errors; 12 critical/abend, immediate action; 16 environment error, system support. | data-dictionary §8.2 |
| BR-DOC-5 | `RTNANA.jcl` runs `RTNANA00` as a single-step job writing a 133-byte FBA report to `RPTFILE` (SYSOUT); it reads DB2 `RTNCODES`. It is a *consumer* of batch return codes, not part of orchestration. | `RTNANA.jcl:1-10`, `RTNANA00.cbl:126-139` |

**Business rule count: 45** (BCHCTL 8, PRCSEQ 19, RCVPRC 16, CKPRST 7, BCHCON 2, DOC 5 — of which 7 are INFERRED FROM DOCS).

### 2.7 Status transition summary (as coded)

```
PRCSEQ00 INIT  : (none) ──create──▶ R
PRCSEQ00 NEXT  : R ──deps ok──▶ A          (start time stamped)
<business pgm>  : A ──▶ D | E               (NOT in S6 code; BCHCTL00 UPDT/TERM unimplemented)
RCVPRC00 RESTART  : any ──▶ R  (restart-count+1, attempt-ts)
RCVPRC00 BYPASS   : any ──▶ D  (rc=4, desc='Process bypassed by recovery')
RCVPRC00 TERMINATE: any ──▶ E  (rc=8, desc='Process terminated by recovery')
'W' (WAITING) is defined but never assigned by any program.
```

---

## 3. Data contracts

Type mapping conventions: `PIC X(n)` → `String` (fixed length n, space-padded on the mainframe; trim on ingest); `PIC 9(n)`/`S9(n) COMP` → `Integer`/`Short`; `PIC X(8)` dates in `YYYYMMDD` text → `LocalDate` (the orchestrator stores dates as `X(8)` text, not `9(8)`; treat as `LocalDate` with `BASIC_ISO_DATE` parse, rejecting non-numeric); 26-char timestamps → `LocalDateTime` (DB2 `TIMESTAMP` layout `YYYY-MM-DD-hh.mm.ss.ffffff`); Level-88 groups → Java enums. No S6 field has an implied decimal (no `V` pictures), so no `BigDecimal` is needed in this component.

### 3.1 `BATCH-CONTROL-RECORD` (`BCHCTL.cpy`) — VSAM KSDS `BCHCTL` (DDNAME `BCHCTL`)

Record length as coded: 20 (key) + 1 + 32 + 2 + 10×14 + 2 + 80 + 2 + 26 + 26 + 50 = **381 bytes**. Documentation says 200 bytes with a different layout (see §5).

| Field | PIC | Semantics | Java type | Target store |
|---|---|---|---|---|
| `BCT-JOB-NAME` | X(8) | Process/job id (= `PSR-PROCESS-ID`) — key part 1 | `String` | `batch_control.job_name` |
| `BCT-PROCESS-DATE` | X(8) | Processing date YYYYMMDD — key part 2 | `LocalDate` | `batch_control.process_date` |
| `BCT-SEQUENCE-NO` | 9(4) | Plan position (1..n) — key part 3 | `Integer` | `batch_control.sequence_no` |
| `BCT-STATUS` | X(1) 88s R/A/W/D/E | Process status | `enum ProcessStatus {READY, ACTIVE, WAITING, DONE, ERROR}` | `batch_control.status` |
| `BCT-STEP-NAME` | X(8) | JCL step name (never set in code) | `String` | `batch_control.step_name` |
| `BCT-PROGRAM-NAME` | X(8) | Program executed (never set in code) | `String` | `batch_control.program_name` |
| `BCT-START-TIME` | X(8) | Start time; receives the first 8 chars of a 26-char timestamp (exact layout of `ACCEPT … FROM TIME STAMP` is compiler-specific; if DB2 layout, this is `YYYY-MM-`) | `LocalDateTime` (store full timestamp) | `batch_control.start_time` |
| `BCT-END-TIME` | X(8) | End time (never set in code) | `LocalDateTime` | `batch_control.end_time` |
| `BCT-PREREQ-COUNT` | 9(2) COMP | Number of prerequisite entries (never set in code) | `Integer` | derived (count of rows) |
| `BCT-PREREQ-NAME` ×10 | X(8) | Prerequisite job name | `String` | `batch_control_prereq.prereq_job_name` |
| `BCT-PREREQ-SEQ` ×10 | 9(4) | Prerequisite sequence | `Integer` | `batch_control_prereq.prereq_seq` |
| `BCT-PREREQ-RC` ×10 | S9(4) COMP | Max acceptable prerequisite RC | `Integer` | `batch_control_prereq.max_rc` |
| `BCT-RETURN-CODE` | S9(4) COMP | Final RC of the process | `enum ReturnCode` (S8) / `int` | `batch_control.return_code` |
| `BCT-ERROR-DESC` | X(80) | Error/bypass description | `String` | `batch_control.error_desc` |
| `BCT-RESTART-COUNT` | 9(2) COMP | Times restarted by recovery | `Integer` | `batch_control.restart_count` |
| `BCT-ATTEMPT-TS` | X(26) | Timestamp of last restart | `LocalDateTime` | `batch_control.attempt_ts` |
| `BCT-COMPLETE-TS` | X(26) | Completion timestamp (never set in code) | `LocalDateTime` | `batch_control.complete_ts` |
| `BCT-FILLER` | X(50) | unused | — | — |

Note: the `BCT-PREREQ-*` array is never read or written by any S6 program; dependencies are evaluated from `PRCSEQ` (`PSR-DEP-*`) instead.

### 3.2 `PROCESS-SEQUENCE-RECORD` (`PRCSEQ.cpy`) — VSAM KSDS `PRCSEQ` (DDNAME `PRCSEQ`)

| Field | PIC | Semantics | Java type | Target store |
|---|---|---|---|---|
| `PSR-PROCESS-ID` | X(8) | Process id — key part 1 | `String` | `process_definition.process_id` |
| `PSR-VERSION` | 9(2) | Definition version — key part 2 (never set by code) | `Integer` | `process_definition.version` |
| `PSR-DESCRIPTION` | X(30) | Description | `String` | `process_definition.description` |
| `PSR-TYPE` | X(3) 88s INI/PRC/RPT/TRM | Sequence type; matched against `LS-SEQUENCE-TYPE` | `enum SequenceType {INIT, PROCESS, REPORT, TERMINATE}` | `process_definition.sequence_type` |
| `PSR-FREQ` | X(1) 88s D/W/M | Frequency (unused by code) | `enum Frequency {DAILY, WEEKLY, MONTHLY}` | `process_definition.frequency` |
| `PSR-START-TIME` | 9(4) | Earliest start HHMM (unused) | `LocalTime` | `process_definition.start_time` |
| `PSR-MAX-TIME` | 9(4) | Max run time (unit unspecified; unused) | `Integer` | `process_definition.max_time` |
| `PSR-DEP-COUNT` | 9(2) COMP | Number of dependencies (0..10) | `Integer` | derived |
| `PSR-DEP-ID` ×10 | X(8) | Dependency process id | `String` | `process_dependency.depends_on` |
| `PSR-DEP-TYPE` ×10 | X(1) 88s H/S | Hard (must be done) / soft | `enum DependencyType {HARD, SOFT}` | `process_dependency.dep_type` |
| `PSR-DEP-RC` ×10 | S9(4) COMP | Max tolerated RC of dependency | `Integer` | `process_dependency.max_rc` |
| `PSR-PROGRAM` | X(8) | Program to execute (unused by code) | `String` | `process_definition.program` |
| `PSR-PARM` | X(50) | Program parm (unused) | `String` | `process_definition.parm` |
| `PSR-MAX-RC` | S9(4) COMP | Max acceptable own RC (unused) | `Integer` | `process_definition.max_rc` |
| `PSR-RESTART` | X(1) 88s Y/N | Restartable | `boolean` (or `enum RestartPolicy`) | `process_definition.restartable` |
| `PSR-ACTIVE-DAYS` | X(7) 88s | Run-day mask Mon..Sun (`YYYYYNN` weekday, `NNNNNYY` weekend, `YYYYYYY` all); unused | `EnumSet<DayOfWeek>` | `process_definition.active_days` |
| `PSR-MONTH-END` | X(1) 88 Y | Month-end only (unused) | `boolean` | `process_definition.month_end_only` |
| `PSR-HOLIDAY-RUN` | X(1) 88s N/Y | Run on holidays (unused) | `boolean` | `process_definition.run_on_holiday` |
| `PSR-RECOVERY-PGM` | X(8) | Recovery program (unused) | `String` | `process_definition.recovery_program` |
| `PSR-RECOVERY-PARM` | X(50) | Recovery parm (unused) | `String` | `process_definition.recovery_parm` |
| `PSR-ERROR-LIMIT` | 9(4) COMP | Error limit (unused) | `Integer` | `process_definition.error_limit` |
| `PSR-CREATE-DATE` / `PSR-UPDATE-DATE` | X(10) | Audit dates (format unspecified, likely YYYY-MM-DD) | `LocalDate` | audit columns |
| `PSR-CREATE-USER` / `PSR-UPDATE-USER` | X(8) | Audit users | `String` | audit columns |
| `PSR-FILLER` | X(50) | unused | — | — |
| `STANDARD-SEQUENCES` | 3×3×X(8) constants | Documented default plan: SOD `INITDAY, CKPCLR, DATEVAL`; MAIN `TRNVAL00, POSUPD00, HISTLD00`; EOD `RPTGEN00, BCKLOD00, ENDDAY` — **never referenced by code** | seed data (`enum` or Flyway seed) | `process_definition` seed rows |

### 3.3 `CHECKPOINT-CONTROL` / `CHECKPOINT-RECORD` (`CKPRST.cpy`) — VSAM KSDS `CKPTFILE`

| Field | PIC | Semantics | Java type | Target store |
|---|---|---|---|---|
| `CK-PROGRAM-ID` | X(8) | Program taking checkpoints | `String` | Spring Batch `BATCH_JOB_INSTANCE.JOB_NAME` |
| `CK-RUN-DATE` | X(8) | Run date YYYYMMDD | `LocalDate` | job parameter `runDate` |
| `CK-RUN-TIME` | X(6) | Run time HHMMSS | `LocalTime` | `BATCH_JOB_EXECUTION.START_TIME` |
| `CK-STATUS` | X(1) 88s I/A/C/F/R | Checkpoint status | `enum CheckpointStatus {INITIAL, ACTIVE, COMPLETE, FAILED, RESTARTED}` | `BATCH_STEP_EXECUTION.STATUS` |
| `CK-RECORDS-READ/PROC/ERROR` | 9(9) COMP | Counters | `long` | `READ_COUNT`, `WRITE_COUNT`, `READ_SKIP_COUNT`/`PROCESS_SKIP_COUNT` |
| `CK-RESTART-COUNT` | 9(2) COMP | Restarts so far | `int` | count of `BATCH_JOB_EXECUTION` rows per instance |
| `CK-LAST-KEY` | X(50) | Last processed key | `String` | `BATCH_STEP_EXECUTION_CONTEXT` |
| `CK-LAST-TIME` | X(26) | Last checkpoint timestamp | `LocalDateTime` | `LAST_UPDATED` |
| `CK-PHASE` | X(2) 88s 00/10/20/30/40 | Processing phase | `enum CheckpointPhase {INIT, READ, PROCESS, UPDATE, TERMINATE}` | step name / execution context |
| `CK-FILE-NAME` ×5 / `CK-FILE-POS` ×5 / `CK-FILE-STATUS` ×5 | X(8)/X(50)/X(2) | Per-file position (note: `CK-FILE-STATUS` name is reused for both the OCCURS group and an elementary item — ambiguous reference) | `List<FilePosition{name,position,status}>` | execution context |
| `CK-COMMIT-FREQ` | 9(5) COMP VALUE 1000 | Commit interval | `int` | chunk size (`commit-interval`) |
| `CK-MAX-ERRORS` | 9(3) COMP VALUE 100 | Skip limit | `int` | `skipLimit` |
| `CK-MAX-RESTARTS` | 9(2) COMP VALUE 3 | Restart limit | `int` | `startLimit` |
| `CK-RESTART-MODE` | X(1) 88s N/R/C | Normal / restart / recover | `enum RestartMode {NORMAL, RESTART, RECOVER}` | job parameter |
| `CKR-PROGRAM-ID`, `CKR-RUN-DATE` | X(8), X(8) | Checkpoint file key | `String`, `LocalDate` | Spring Batch job instance identity |
| `CKR-DATA` | X(400) | Opaque serialized checkpoint | `String`/JSON | `BATCH_STEP_EXECUTION_CONTEXT.SERIALIZED_CONTEXT` |

### 3.4 Linkage (call) contracts

| Program | Field | PIC | Java DTO |
|---|---|---|---|
| BCHCTL00 `LS-CONTROL-REQUEST` | `LS-FUNCTION` X(4) {INIT,CHEK,UPDT,TERM}; `LS-JOB-NAME` X(8); `LS-PROCESS-DATE` X(8); `LS-SEQUENCE-NO` 9(4); `LS-RETURN-CODE` S9(4) COMP | | `ControlRequest{ControlFunction fn; String jobName; LocalDate processDate; int sequenceNo}` → `ReturnCode` |
| PRCSEQ00 `LS-SEQUENCE-REQUEST` | `LS-FUNCTION` X(4) {INIT,NEXT,STAT,TERM}; `LS-PROCESS-DATE` X(8); `LS-SEQUENCE-TYPE` X(3); `LS-NEXT-PROCESS` X(8) (in/out); `LS-RETURN-CODE` S9(4) COMP (in/out) | | `SequenceRequest{SequenceFunction fn; LocalDate processDate; SequenceType type; String processId}` → `SequenceResult{Optional<String> nextProcess; ReturnCode rc}` |
| RCVPRC00 `LS-RECOVERY-REQUEST` | `LS-FUNCTION` X(4) {INIT,RECV,TERM}; `LS-PROCESS-DATE` X(8); `LS-PROCESS-ID` X(8); `LS-RECOVERY-TYPE` X(1) {P,S,A}; `LS-RECOVERY-PARM` X(50); `LS-RETURN-CODE` S9(4) COMP | | `RecoveryRequest{RecoveryScope scope; LocalDate processDate; String processId; String parm}` → `RecoveryResult{ReturnCode rc; List<RecoveryAction>}` |
| CKPRST | `CHECKPOINT-CONTROL` (above) + `RETURN-STATUS` from `RETHND.cpy` (`RETURN-CODE` S9(4) COMP, `REASON-CODE` S9(4) COMP, `MODULE-ID` X(8), `FUNCTION-ID` X(8)) | | replaced by Spring Batch `StepExecution` |

### 3.5 Constants (`BCHCON.cpy`) → Java

`ProcessStatus` (R/A/W/D/E), `ReturnCode` (0/4/8/12/16 — shared with S8 `RTNCODES`), `OrchestratorProperties{maxPrereq=10, maxRestarts=3, waitIntervalSeconds=300, maxWaitSeconds=3600}`, `ProcessType` (INI/UPD/RPT/CLN — conflicts with `PSR-TYPE`, see §5), `DependencyKind` (R/O/X — conflicts with `PSR-DEP-TYPE`, see §5), reserved names `STARTDAY`/`ENDDAY`/`EMERGENC`, record types C/P/D/H, four status messages.

---

## 4. Interfaces

### 4.1 Inbound

| Interface | Kind | Caller (as found) | Java mapping |
|---|---|---|---|
| `CALL 'BCHCTL00' USING LS-CONTROL-REQUEST` | static CALL | **No caller in repo** (grep of `src/` finds none; only SAD §5.2.1 shows BCHCTL00 → POSUPDT/HISTLD00/Reports) | `BatchControlService` bean; exposed as REST `POST /batch/runs/{date}/jobs/{job}:init|check|update|terminate` for the scheduler and as a library call for in-process steps |
| `CALL 'PRCSEQ00' USING LS-SEQUENCE-REQUEST` | static CALL | **No caller in repo** | `ProcessSequenceService` (plan builder / dispatcher); REST `POST /batch/runs/{date}/plan?type=…`, `POST /batch/runs/{date}/next`, `GET /batch/runs/{date}/status`, `POST /batch/runs/{date}:terminate` |
| `CALL 'RCVPRC00' USING LS-RECOVERY-REQUEST` | static CALL | **No caller in repo** | `RecoveryService`; REST `POST /batch/runs/{date}/recover` with body `{scope: PROCESS|SEQUENCE|ALL, processId}` — operator/admin API |
| `CALL 'CKPRST'` (or documented `CKPINIT/CKPTAKE/CKPCMIT/CKPRSTR`) | static CALL | **No caller in repo**; the business programs (`PORTTRAN`, `HISTLD00`, …) do not call it | Retired: replaced by Spring Batch chunk-oriented steps with `JobRepository` persistence (library, no REST) |
| JCL job `RTNANA00` (`RTNANA.jcl`) | JCL EXEC PGM=RTNANA00 | z/OS scheduler | Belongs to S8 (`RTNANA00`); the orchestrator only *schedules* it as a REPORT-type process. Report output → S7/S8 endpoint |
| JCL for BCHCTL00/PRCSEQ00/RCVPRC00 | — | **None exists** (`src/jcl/batch/` contains only `RPT*.jcl` and an empty README) | Scheduler trigger = Spring `@Scheduled`/Quartz or external (Airflow/Control-M) calling the REST API |

### 4.2 Outbound

| Interface | Kind | Where | Java mapping |
|---|---|---|---|
| `CALL 'ERRPROC' USING ERR-MESSAGE` | static CALL | `BCHCTL00.cbl:114`, `PRCSEQ00.cbl:122`, `RCVPRC00.cbl:113, 290` | S8 platform-common library call: `ErrorLogger.log(ErrorMessage)` → writes `ERRLOG` + publishes `BatchErrorLogged` event. The RCVPRC00 §3100 informational use maps to `INFO`-level audit event, not an error |
| VSAM `BCHCTL` (DDNAME `BCHCTL`) | KSDS I-O: READ/WRITE/REWRITE/START/READ NEXT | `BCHCTL00`, `PRCSEQ00`, `RCVPRC00` | DB read/write via JPA `BatchControlRepository` on table `batch_control` (+ `batch_control_prereq`), owned by S6 |
| VSAM `PRCSEQ` (DDNAME `PRCSEQ`) | KSDS I-O (PRCSEQ00) / INPUT (RCVPRC00): READ/START/READ NEXT | `PRCSEQ00.cbl:17-22`, `RCVPRC00.cbl:24-29` | DB read via `ProcessDefinitionRepository` on `process_definition` (+ `process_dependency`), owned by S6. Note the plan calls this store `PRCCTL`; code names it `PRCSEQ` |
| VSAM `CKPTFILE` | KSDS (declared, never accessed) | `CKPRST.cbl:7-12` | Spring Batch `BATCH_*` tables (job repository) |
| `ACCEPT … FROM TIME STAMP` | system clock | `PRCSEQ00.cbl:271`, `RCVPRC00.cbl:207` | `Clock` bean (injectable for tests) |
| `RETURN-CODE` special register | job step RC | all three programs | HTTP status + `ReturnCode` in response body; for CLI/Task runs `ExitCodeGenerator` |
| DB2 | none | No `EXEC SQL` in any S6 program | — |
| CICS | none | No `EXEC CICS` in any S6 program | — |
| Start/complete of the scheduled business programs | *not coded* (no `CALL`/`XCTL`/JCL submit in S6) | SAD §2.1 says "BCF → Start Process", "Signal Next Job" | Outbound **events**: `ProcessReady{processId, processDate}` consumed by S2/S3/S7 job launchers; inbound `ProcessCompleted{processId, processDate, returnCode}` updates control record (replaces the unimplemented BCHCTL00 `UPDT`/`TERM`) |

---

## 5. Discrepancies (code vs documentation / plan)

| # | Discrepancy | Evidence |
|---|---|---|
| D-1 | **`BCHCTL00` is a skeleton**: nine paragraphs it PERFORMs are declared "to be implemented" and absent. Docs (SAD §1.2.1, §5.1.1, §5.2.1) describe it as the working batch controller. | `BCHCTL00.cbl:116-127` |
| D-2 | **`CKPRST` is a stub**: four empty paragraphs, undefined `ENTRY-POINT-*` conditions, duplicated `COPY`. Docs (SAD §4.2, §6.2.1; DD §8.3) describe a working checkpoint/restart framework. The `CKPINIT/CKPTAKE/CKPCMIT/CKPRSTR` programs named in the copybook do not exist. | `CKPRST.cbl:29-57`, `CKPRST.cpy:59-76` |
| D-3 | `BCHCTL` record layout differs completely: DD §2.4 = key `PROCESS-DATE 9(8) + PROCESS-ID X(8)`, statuses `W/P/C/E`, fields `RECORD-COUNT`, `ERROR-COUNT`, `LAST-POS`, `MESSAGE X(50)`, LRECL 200. Code = key `JOB-NAME X(8) + PROCESS-DATE X(8) + SEQUENCE-NO 9(4)`, statuses `R/A/W/D/E`, prereq array, restart stats, LRECL 381. `RECORD-COUNT`/`ERROR-COUNT`/`LAST-POS` are documented but not implemented. | `BCHCTL.cpy:9-39` vs DD §2.4 |
| D-4 | `PRCCTL` (DD §2.5, sequential 80-byte, one `PRC-DEPENDENCY` and `PRC-REQUIRED-RC`) does not exist in code; the equivalent is VSAM KSDS `PRCSEQ` with up to 10 typed dependencies each with its own RC threshold. The modernization plan also uses the name `PRCCTL`. | `PRCSEQ.cpy` vs DD §2.5 |
| D-5 | Checkpoint record: DD §2.6 says it lives "in VSAM BCHCTL" with `CHK-LAST-TRANS-ID/ACCOUNT/FUND/RECORDS-PROC/TIMESTAMP`; code defines a separate `CKPTFILE` with key program-id + run-date and an opaque 400-byte payload. | `CKPRST.cpy:52-56` vs DD §2.6 |
| D-6 | Status code values differ: DD `W`aiting/`P` in-process/`C`omplete/`E`rror vs code `R/A/W/D/E`. Checkpoint statuses `I/A/C/F/R` are undocumented. | `BCHCON.cpy:9-13` vs DD §2.4 |
| D-7 | Process type vocabularies conflict inside the code: `BCHCON` `INI/UPD/RPT/CLN` vs `PRCSEQ` `INI/PRC/RPT/TRM`. Dependency vocabularies conflict: `BCHCON` `R/O/X` vs `PRCSEQ` `H/S`. Only the `PRCSEQ` values are used. | `BCHCON.cpy:31-41`, `PRCSEQ.cpy:12-16, 28-30` |
| D-8 | Process names: DD/SAD use `TRNVAL00`, `POSUPD00`, `RPTGEN00`; `PRCSEQ.cpy` standard sequences add `INITDAY`, `CKPCLR`, `DATEVAL`, `BCKLOD00`, `ENDDAY`; `BCHCON` adds `STARTDAY`, `EMERGENCY`. Actual programs in the repo are `POSUPDT` (empty), `HISTLD00`, `RPTPOS00/RPTAUD00/RPTSTA00`; `TRNVAL00`, `POSUPD00`, `RPTGEN00`, `INITDAY`, `CKPCLR`, `DATEVAL`, `BCKLOD00`, `ENDDAY`, `STARTDAY`, `EMERGENCY` do not exist. | `PRCSEQ.cpy:63-75`, `BCHCON.cpy:44-47`, DD §8.1 |
| D-9 | DD §9.1 time windows, "day must be open", month-end/holiday/active-day scheduling, `BCT-WAIT-INTERVAL`/`BCT-MAX-WAIT-TIME` polling — none implemented; the schedule fields in `PRCSEQ` are never read. | `PRCSEQ00.cbl` (no reference to `PSR-TIMING`/`PSR-SCHEDULE`) |
| D-10 | DD §9.1 says RPTGEN00 prerequisite condition is "None" while SAD §4.1 requires RC ≤ 4 for the same edge; DD §8.1 says RPTGEN00 depends on POSUPD00, DD §9.1 says HISTLD00. | DD §8.1, §9.1; SAD §4.1 |
| D-11 | SAD §5.1.1 says `BCHCTL00` has DB2 access "Write"; no S6 program contains SQL. | SAD §5.1.1 |
| D-12 | `src/jcl/batch/README.md` is empty (0 bytes); there is no JCL for any S6 program. `RTNANA.jcl` is filed under S6 in the plan but runs `RTNANA00`, an S8 program. | `ls src/jcl/batch`, `RTNANA.jcl:4` |
| D-13 | Compile-blocking defects in the "implemented" programs: `WS-SUB` undeclared (`PRCSEQ00.cbl:230-240`); `ACCEPT … FROM TIME STAMP` is not standard Enterprise COBOL syntax (`PRCSEQ00.cbl:271`, `RCVPRC00.cbl:207`); `BCHCTL00` PERFORMs undefined paragraphs; `CKPRST` undefined conditions and duplicate copybook; `RETHND.cpy` defines a data item named `RETURN-CODE`, which clashes with the special register when used in `CKPRST`. | cited lines |
| D-14 | Logical defects vs documented intent: RCVPRC00 sequence recovery recovers the input `LS-PROCESS-ID` instead of each browsed record (BR-RCVPRC-12); restart limit applied only to non-restartable processes (BR-RCVPRC-7); `BCT-START-TIME X(8)` cannot hold the 26-char timestamp (BR-PRCSEQ-11); key reads leave `BCT-SEQUENCE-NO`/`PSR-VERSION` unset (BR-PRCSEQ-8/9/11); `BCHCTL` and `PRCSEQ` error routines never abort so subsequent steps run on failed opens/reads. | cited rules |
| D-15 | Plan §1 says the blueprint references `Makefile`/`tools/preprocess.sh` not in the repo — confirmed (`git ls-files` finds neither); no compile verification was attempted. | repo listing |

---

## 6. Proposed Java design

Module: `services/batch-orchestrator` (Spring Boot 3.x, Spring Batch 5.x, Spring Data JPA, Postgres via F1 Flyway). New service + new data store → run `arb-triage` in Phase 1 (F3) as the plan already requires.

### 6.1 Package layout

```
com.cog.portfolio.batchorch
├── api/                       REST controllers + DTOs (§3.4)
│   ├── RunPlanController      POST /batch/runs/{date}/plan, /next, GET /status, POST :terminate
│   ├── ControlController      /batch/runs/{date}/jobs/{job}:init|check|update|terminate
│   └── RecoveryController     POST /batch/runs/{date}/recover
├── domain/
│   ├── ProcessDefinition, ProcessDependency          (PRCSEQ)
│   ├── BatchControl, BatchControlPrereq              (BCHCTL)
│   ├── ProcessStatus, SequenceType, DependencyType,
│   │   RecoveryScope, RecoveryAction, RestartMode, CheckpointPhase (enums, §3)
│   └── RunPlan (in-memory WS-PROCESS-TABLE replacement, persisted as batch_run_plan)
├── service/
│   ├── ProcessSequenceService   buildPlan(date,type) / nextProcess(date) / recordStatus(...) / finalRc(date)   ← PRCSEQ00
│   ├── DependencyEvaluator      BR-PRCSEQ-9/10 (hard-not-done → WARNING; done-but-rc>max → ERROR)
│   ├── BatchControlService      init/check/update/terminate                                                   ← BCHCTL00 (behaviour to be defined, see §7)
│   ├── RecoveryService          recover(scope,date,processId) → RESTART/BYPASS/TERMINATE                     ← RCVPRC00
│   └── ReturnCodeAggregator     BR-PRCSEQ-15
├── repository/                  BatchControlRepository, ProcessDefinitionRepository, RunPlanRepository (JPA)
├── batch/
│   ├── OrchestratorJobConfig    Spring Batch Job "nightly-run" with a JobExecutionDecider that loops
│   │                            nextProcess → launch → await ProcessCompleted → recordStatus
│   ├── ProcessLaunchTasklet     publishes ProcessReady / calls S2,S3,S7 job launchers
│   └── CheckpointSupport        thin adapter documenting how CK-* concepts map to StepExecutionContext (no custom checkpoint store)
├── messaging/
│   ├── ProcessReadyPublisher    event ProcessReady{processId, processDate, sequenceNo, parm}
│   ├── ProcessCompletedListener event ProcessCompleted{processId, processDate, returnCode, errorDesc} → BatchControl A→D/E, endTime, completeTs
│   └── BatchErrorPublisher      wraps S8 ErrorLogger (ERRPROC replacement)
├── scheduling/
│   └── NightlyRunScheduler      @Scheduled cron (window from DD §9.1, configurable) → buildPlan + start "nightly-run"
└── config/
    └── OrchestratorProperties   maxPrereq=10, maxRestarts=3, waitIntervalSeconds=300, maxWaitSeconds=3600, defaultCommitInterval=1000, maxErrors=100
```

### 6.2 Persistence (Flyway, owned by S6)

- `process_definition(process_id, version, description, sequence_type, frequency, start_time, max_time, program, parm, max_rc, restartable, active_days, month_end_only, run_on_holiday, recovery_program, recovery_parm, error_limit, created_at, created_by, updated_at, updated_by)` PK `(process_id, version)`.
- `process_dependency(process_id, version, ordinal, depends_on, dep_type, max_rc)` — normalises the `OCCURS 10`.
- `batch_control(job_name, process_date, sequence_no, status, step_name, program_name, start_time, end_time, return_code, error_desc, restart_count, attempt_ts, complete_ts)` PK `(job_name, process_date, sequence_no)`; unique index `(process_date, job_name)` so the "read by name+date" pattern (BR-PRCSEQ-9/11) is well-defined.
- `batch_control_prereq(job_name, process_date, sequence_no, ordinal, prereq_job_name, prereq_seq, max_rc)` — kept for layout parity; not used by any coded rule.
- Spring Batch `BATCH_*` tables replace `CKPTFILE`.
- Seed: the `STANDARD-SEQUENCES` constants and DD §8.1/§9.1 chain as `process_definition` rows, tagged as INFERRED FROM DOCS in the migration comment.

### 6.3 Behavioural mapping decisions

- `PRCSEQ00 INIT/NEXT/STAT/TERM` become one Spring Batch `Job` per (date, sequenceType); the `WS-PROCESS-TABLE` becomes a persisted `batch_run_plan` so state survives JVM restarts (fixes BR-PRCSEQ-17).
- Dependency evaluation reproduces BR-PRCSEQ-10 exactly (strict `>` on max RC; hard-not-done → wait). Waiting uses `waitIntervalSeconds`/`maxWaitSeconds` from `BCHCON` (currently dead constants) — flagged in §7.
- Recovery reproduces BR-RCVPRC-7..10 verbatim, including BYPASS → status DONE rc 4; the inverted restart-limit check (BR-RCVPRC-7) is preserved behind a feature flag `recovery.legacyRestartLimit=true` until §7 Q3 is answered.
- Sequence recovery is implemented per the *evident intent* (each browsed record recovered) — deviation from the coded bug BR-RCVPRC-12 must be confirmed (§7 Q4).
- Timestamps are stored at full precision; the `X(8)` truncation of `BCT-START-TIME` is not reproduced (§7 Q6).
- Error handling: every `ERRPROC` call becomes a thrown `BatchOrchestrationException` (S8 hierarchy) mapped to `ReturnCode.ERROR`; unlike COBOL, processing stops at the first error (§7 Q7).
- Checkpoint/restart: business steps in S2/S3/S7 use chunk-oriented steps with `commit-interval` = 1000 (transactions/history) or 500 (positions) per DD §8.3; restart via `JobOperator.restart(executionId)`; `startLimit=3` (`CK-MAX-RESTARTS`). No custom checkpoint store.

---

## 7. Open questions

1. **BCHCTL00 semantics** — the nine unimplemented paragraphs (validate process, check dependencies, update status/completion) have no code. Should the Java `BatchControlService` adopt the `PRCSEQ00` dependency logic, or is a distinct per-job control contract (using `BCT-PREREQ-*`) intended?
2. **Which process catalogue is authoritative** — the documented sequential `PRCCTL` (one dependency, `REQUIRED-RC`) or the coded VSAM `PRCSEQ` (10 typed dependencies, per-dependency RC)? The plan names `PRCCTL`; code only has `PRCSEQ`.
3. **Restart limit** — code applies `BCT-MAX-RESTARTS` (3) only to *non*-restartable processes and restarts restartable ones without limit. Is that intended, or should restartable processes be capped at 3 and non-restartable ones always bypassed/terminated?
4. **Sequence recovery bug** — `RCVPRC00` mode `S` re-reads the caller-supplied process id for every matched record. Implement the evident intent (recover each record for the date) or reproduce the coded behaviour?
5. **BYPASS = DONE with RC 4** — a bypassed process satisfies any downstream dependency with `max_rc >= 4`. Is that the desired business outcome (downstream runs on skipped input), or should bypass be a distinct status?
6. **Timestamp precision** — `BCT-START-TIME` is `X(8)` and receives the first 8 characters of a 26-char timestamp (the date portion only, if the DB2 timestamp layout is assumed). Confirm that Java should store a full `LocalDateTime` and that no downstream consumer depends on the 8-char field.
7. **Error propagation** — COBOL logs via `ERRPROC` and continues (e.g. after a failed OPEN). Java proposal: fail fast with RC 8. Acceptable?
8. **Scheduling rules** — time windows (DD §9.1), "day must be open", active-day mask, month-end and holiday flags exist in docs/copybook but not in code. Are they in scope for S6 Phase 2, and where does the business calendar come from?
9. **RPTGEN00 prerequisite** — DD §8.1 says POSUPD00, DD §9.1 says HISTLD00 with condition "None", SAD §4.1 says HISTLD00 with RC ≤ 4. Which is correct?
10. **Process names** — which actual programs/services back `TRNVAL00`, `POSUPD00`, `RPTGEN00`, `INITDAY`, `CKPCLR`, `DATEVAL`, `BCKLOD00`, `ENDDAY`, `STARTDAY`, `EMERGENCY`? Only `HISTLD00` and `RPT*00` exist in the repo.
11. **Checkpoint scope** — with `CKPRST` empty, is adopting Spring Batch's job repository as the *only* checkpoint mechanism acceptable, or must the `CKPTFILE`/`CKR-DATA` layout be preserved for coexistence with mainframe jobs during migration?
12. **Plan capacity** — `WS-PROCESS-TABLE` holds 100 entries with no overflow check. Is 100 a real business limit or an implementation artefact?
13. **Restart counter overflow** — `BCT-RESTART-COUNT` is `9(2) COMP` (unsigned). Should Java cap restarts or allow unbounded counts?
14. **Ownership of `RTNANA.jcl`** — it is listed under S6 in the plan but runs the S8 program `RTNANA00`. Confirm it moves to S8 (or S7 reporting).
