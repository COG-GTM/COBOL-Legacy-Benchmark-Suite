# JCL Job Inventory

Part of the AB-285 inventory. Covers every member under `src/jcl/` (16 JCL
members + 2 empty `README.md` placeholders), the program each step runs, the
DD → dataset bindings, and whether the DD names agree with the program's
`SELECT … ASSIGN TO` clauses.

Inputs: `src/jcl/**`, the `ENVIRONMENT DIVISION` of each executed program,
`documentation/technical/system-architecture.md` §"Batch Processing",
`documentation/operations/README.md`.

## 1. Summary

```
 src/jcl/
 ├── RTNANA.jcl            RTNANA00   return-code analysis report (DB2)
 ├── batch/
 │   ├── README.md          (empty)
 │   ├── RPTAUD.jcl        RPTAUD00   audit report
 │   ├── RPTPOS.jcl        RPTPOS00   daily position report
 │   └── RPTSTA.jcl        RPTSTA00   system statistics report
 ├── portfolio/
 │   ├── PORTDEF.jcl       IDCAMS     (re)define PORTFOLIO.MASTER.FILE
 │   ├── PORTADD.jcl       PORTADD    add portfolios
 │   ├── PORTUPDT.jcl      PORTUPDT   update portfolios
 │   ├── PORTDEL.jcl       PORTDEL    delete portfolios (+ audit)
 │   ├── PORTREAD.jcl      PORTREAD   dump portfolios
 │   └── PORTTEST.jcl      PORTTEST   generate portfolio test file
 ├── test/
 │   ├── TSTGEN.jcl        TSTGEN00   generate test data
 │   └── TSTVAL.jcl        TSTVAL00   compare expected vs actual
 └── utility/
     ├── README.md          (empty)
     ├── UTLMNT.jcl        UTLMNT00   file maintenance / archive
     ├── UTLMON.jcl        UTLMON00   system monitor
     └── UTLVAL.jcl        UTLVAL00   data validation
```

Every job is a **single step**. There are no `COND=`, `IF/THEN/ELSE`,
PROC/INCLUDE, scheduler references, or multi-step dependencies anywhere in
`src/jcl/`. Sequencing between jobs therefore exists only in the documentation
(`system-architecture.md`), in `PRCSEQ.cpy` / `BCHCTL.cpy`, and in the
`PRCSEQ00` / `BCHCTL00` / `RCVPRC00` programs – none of which are invoked by
any JCL.

## 2. Job / step detail

`✔` = DD name in JCL matches an `ASSIGN TO` in the program; `✘` = mismatch;
`–` = not applicable.

### J-01 `RTNANA.jcl` → `RTNANA00`

| Item | Value |
|---|---|
| JOB | `RTNANA00 JOB (ACCT),'RETURN CODE ANALYSIS'` |
| Step | `RTNANA EXEC PGM=RTNANA00` |
| STEPLIB | `PROD.LOAD.LIBRARY` |
| DDs | `RPTFILE` → `SYSOUT=*` `RECFM=FBA,LRECL=133` ✔ |
| DB2 | program issues 5 `EXEC SQL` (cursor `PRGCUR` over `RTNCODES`) but the step is a plain `EXEC PGM=` – no `IKJEFT01` / `DSN RUN … PLAN(…)`, so the DB2 attach is unspecified |
| Notes | program `REPORT-RECORD PIC X(133)` matches the 133-byte FBA DD; the only job that writes its report to `SYSOUT` instead of a catalogued dataset |

### J-02 `batch/RPTAUD.jcl` → `RPTAUD00`

| Item | Value |
|---|---|
| JOB | `RPTAUD00 JOB (ACCT#),'AUDIT REPORT'` |
| Step | `STEP01 EXEC PGM=RPTAUD00` |
| DDs | `AUDITLOG` → `PROD.AUDIT.LOG` (SHR) ✔; `ERRLOG` → `PROD.ERROR.LOG` (SHR) ✔; `RPTFILE` → `PROD.AUDIT.REPORT` (NEW,CATLG,DELETE; FB 132) ✔ |
| Notes | `PROD.AUDIT.LOG` is produced by AUDPROC / PORTDEL as `AUDFILE` (`PORTFOLIO.AUDIT.FILE` in the portfolio JCL) – the two DSNs are never reconciled in JCL. `PROD.ERROR.LOG` is the flat file written by ERRPROC (`ERRLOG` DD), not the DB2 `ERRLOG` table |

### J-03 `batch/RPTPOS.jcl` → `RPTPOS00`

| Item | Value |
|---|---|
| JOB | `RPTPOS00 JOB (ACCT#),'DAILY POSITION RPT'` |
| Step | `STEP01 EXEC PGM=RPTPOS00` |
| DDs | `POSMSTRE` → `PROD.POSITION.MASTER` (SHR) ✔; `TRANHIST` → `PROD.TRANSACTION.HISTORY` (SHR) ✔; `RPTFILE` → `PROD.DAILY.POSITION.REPORT` (FB 132) ✔ |
| Notes | no job in the repository *writes* `PROD.POSITION.MASTER` or `PROD.TRANSACTION.HISTORY` (see data-flow-map §4.3) |

### J-04 `batch/RPTSTA.jcl` → `RPTSTA00`

| Item | Value |
|---|---|
| JOB | `RPTSTA00 JOB (ACCT#),'SYSTEM STATS RPT'` |
| Step | `STEP01 EXEC PGM=RPTSTA00` |
| DDs | `DB2STATS` → `PROD.DB2.STATISTICS` (SHR) ✔; `BCHSTATS` → `PROD.BATCH.STATISTICS` (SHR) ✔; `RPTFILE` → `PROD.SYSTEM.STATS.REPORT` (FB 132) ✔ |
| Notes | program opens `DB2STATS` as sequential; `UTLMON.jcl` gives the *same DSN* to UTLMON00, which opens it as KSDS. Record layout comes from the missing `DB2STAT` copybook |

### J-05 `portfolio/PORTDEF.jcl` → IDCAMS

| Item | Value |
|---|---|
| JOB | `PORTDEF JOB (ACCT),'DEFINE PORTFOLIO'` |
| Step | `DEFVSAM EXEC PGM=IDCAMS` |
| SYSIN | `DELETE PORTFOLIO.MASTER.FILE CLUSTER` / `SET MAXCC = 0` / `DEFINE CLUSTER (NAME(PORTFOLIO.MASTER.FILE) INDEXED RECORDSIZE(200 200) KEYS(18 0) CYLINDERS(5 1) FREESPACE(10 10) SHAREOPTIONS(2 3))` |
| Notes | **Conflicts** with `src/database/vsam/vsam-definitions.txt`, which defines the same DSN as `KEYS(12 0) RECORDSIZE(400 400) CYLINDERS(100 20) FREESPACE(20 20)` with explicit DATA/INDEX components. 200/18 matches `PORTFLIO.cpy` (`PORT-KEY` = 18) as used by PORTADD/PORTUPDT/PORTDEL/PORTREAD/PORTTRAN; 400/12 matches the DB2 `PORTFOLIO_MASTER` shape. The job is destructive (DELETE first) |

### J-06 `portfolio/PORTADD.jcl` → `PORTADD`

| Item | Value |
|---|---|
| JOB | `PORTADD JOB (ACCT),'ADD PORTFOLIO'` |
| Step | `STEP1 EXEC PGM=PORTADD`, STEPLIB `YOUR.LOADLIB` (placeholder) |
| DDs | `PORTFILE` → `PORTFOLIO.MASTER.FILE` (SHR) ✔; `INPTFILE` → `PORTFOLIO.INPUT.FILE` (OLD) ✔ |
| Notes | `DISP=SHR` on a VSAM file opened `I-O` for WRITE; relies on `SHAREOPTIONS(2 3)` |

### J-07 `portfolio/PORTUPDT.jcl` → `PORTUPDT`

| Item | Value |
|---|---|
| JOB | `PORTUPDT JOB (ACCT),'UPDATE PORTFOLIO'` |
| Step | `STEP1 EXEC PGM=PORTUPDT` |
| DDs | `PORTFILE` → `PORTFOLIO.MASTER.FILE` (SHR) ✔; `UPDTFILE` → `PORTFOLIO.UPDATE.FILE` (OLD) ✔ |

### J-08 `portfolio/PORTDEL.jcl` → `PORTDEL`

| Item | Value |
|---|---|
| JOB | `PORTDEL JOB (ACCT),'DELETE PORTFOLIO'` |
| Step | `STEP1 EXEC PGM=PORTDEL` |
| DDs | `PORTFILE` → `PORTFOLIO.MASTER.FILE` (SHR) ✔; `DELEFILE` → `PORTFOLIO.DELETE.FILE` (OLD) ✔; `AUDFILE` → `PORTFOLIO.AUDIT.FILE` (**MOD**) ✔ |
| Notes | program does `OPEN OUTPUT AUDIT-FILE`; with `DISP=MOD` this appends, but AUDPROC writes the same file with `OPEN EXTEND` and a different (392-byte `AUDITLOG.cpy`) record layout than PORTDEL's 80-byte inline layout – the file ends up with mixed record shapes |

### J-09 `portfolio/PORTREAD.jcl` → `PORTREAD`

| Item | Value |
|---|---|
| JOB | `PORTREAD JOB (ACCT),'READ PORTFOLIO'` |
| Step | `STEP1 EXEC PGM=PORTREAD` |
| DDs | `PORTFILE` → `PORTFOLIO.MASTER.FILE` (SHR) ✔ |
| Notes | output is `DISPLAY` to `SYSOUT` only |

### J-10 `portfolio/PORTTEST.jcl` → `PORTTEST`

| Item | Value |
|---|---|
| JOB | `PORTTEST JOB (ACCT),'GEN TEST DATA'` |
| Step | `STEP1 EXEC PGM=PORTTEST` |
| DDs | `TESTFILE` → `PORTFOLIO.TEST.FILE` (NEW,CATLG,DELETE; `RECFM=FB,LRECL=200`) ✔ |
| Notes | record written is `PORTFLIO.cpy` (148 bytes computed) into a 200-byte LRECL |

### J-11 `test/TSTGEN.jcl` → `TSTGEN00`

| Item | Value |
|---|---|
| JOB | `TSTGEN00 JOB (ACCT#),'TEST DATA GEN'`, STEPLIB `TEST.LOAD.LIBRARY` |
| Step | `STEP01 EXEC PGM=TSTGEN00` |
| DDs | `TSTCFG` → `TEST.CONFIG.FILE` ✔; `PORTOUT` → `TEST.PORTFOLIO.DATA` (`LRECL=100`) ✔; `TRANOUT` → `TEST.TRANSACTION.DATA` (`LRECL=100`) ✔; `RANDSEED` → `TEST.RANDOM.SEED` ✔ |
| Notes | **LRECL mismatch**: `PORTOUT` record is `PORTFLIO.cpy` (148 bytes) and `TRANOUT` is `TRNREC.cpy` (152 bytes) but both DDs allocate `LRECL=100`; the step would fail on open or truncate |

### J-12 `test/TSTVAL.jcl` → `TSTVAL00`

| Item | Value |
|---|---|
| JOB | `TSTVAL00 JOB (ACCT#),'TEST VALIDATION'` |
| Step | `STEP01 EXEC PGM=TSTVAL00` |
| DDs | `TESTCASE` → `TEST.CASE.FILE` ✔; `EXPECTED` → `TEST.EXPECTED.RESULTS` ✔; `ACTUAL` → `TEST.ACTUAL.RESULTS` ✔; `TESTRPT` → `TEST.VALIDATION.REPORT` (FB 132) ✔ |

### J-13 `utility/UTLMNT.jcl` → `UTLMNT00`

| Item | Value |
|---|---|
| JOB | `UTLMNT00 JOB (ACCT#),'FILE MAINTENANCE'` |
| Step | `STEP01 EXEC PGM=UTLMNT00` |
| DDs | `CTLFILE` → `PROD.CONTROL.FILE` ✔; `ARCHFILE` → `PROD.ARCHIVE.FILE` (`RECFM=VB,LRECL=32756`) ✔; `RPTFILE` → `PROD.MAINTENANCE.REPORT` (FB 132) ✔ |
| Notes | program declares `ARCHFILE` as variable 32 760 – within the VB LRECL |

### J-14 `utility/UTLMON.jcl` → `UTLMON00`

| Item | Value |
|---|---|
| JOB | `UTLMON00 JOB (ACCT#),'SYSTEM MONITOR'` |
| Step | `STEP01 EXEC PGM=UTLMON00` |
| DDs | `MONCFG` → `PROD.MONITOR.CONFIG` ✔; `MONLOG` → `PROD.MONITOR.LOG` (FB 132) ✔; `ALERTS` → `PROD.MONITOR.ALERTS` (FB 132) ✔; `DB2STATS` → `PROD.DB2.STATISTICS` ✔ |
| Notes | `DB2STATS` is opened as **KSDS** here (RPTSTA00 opens the same DSN sequentially); the program `CALL 'ILBOABN0'` on fatal error |

### J-15 `utility/UTLVAL.jcl` → `UTLVAL00`

| Item | Value |
|---|---|
| JOB | `UTLVAL00 JOB (ACCT#),'DATA VALIDATION'` |
| Step | `STEP01 EXEC PGM=UTLVAL00` |
| DDs | `VALCTL` → `PROD.VALIDATION.CONTROL` ✔; `POSMSTRE` → `PROD.POSITION.MASTER` ✔; `TRANHIST` → `PROD.TRANSACTION.HISTORY` ✔; `ERRRPT` → `PROD.VALIDATION.REPORT` (FB 132) ✔ |

### J-16 `batch/README.md`, `utility/README.md`

Zero-byte placeholders; no content.

## 3. Programs with no JCL

| Program | Why it matters |
|---|---|
| `BCHCTL00`, `PRCSEQ00`, `RCVPRC00`, `CKPRST`, `RTNCDE00` | the documented batch-control layer (dependency check, checkpoint, recovery, return-code recording) is never scheduled |
| `HISTLD00` | the only program that loads DB2 `POSHIST`; needs `TRANHIST`, `BCHCTL` DDs and a DB2 attach – none exist |
| `POSUPDT.cbl` | empty file; also the documented `POSUPD00` step |
| `PORTTRAN` | transaction processor (`TRANFILE`, `PORTFILE`) – the core daily step has no job |
| `PORTMSTR`, `PORTVALD` | called subprograms; no caller in repo either |
| `AUDPROC`, `ERRPROC`, `DB2CMT`, `DB2CONN`, `DB2ERR`, `DB2STAT` | called subprograms (all `PROCEDURE DIVISION USING …` / `GOBACK`); reachable only through the programs above |
| all `src/programs/online/*` | CICS; defined in `PORTDFN.csd` instead (except `ERRHNDL`, which is not defined anywhere) |
| documented `TRNVAL00`, `POSUPD00`, `RPTGEN00` | named in `system-architecture.md` and `PRCSEQ.cpy` but have no source or JCL |

## 4. Documented vs actual daily cycle

```
 system-architecture.md / PRCSEQ.cpy               src/jcl (what can actually run)
 ──────────────────────────────────────            ────────────────────────────────────
 TRNVAL00  validate TRANFILE ─┐                    PORTDEF   (IDCAMS, one-off)
 POSUPD00  update POSMSTRE   ─┤ hard deps,          PORTTEST → PORTADD → PORTUPDT → PORTDEL → PORTREAD
 HISTLD00  load POSHIST      ─┤ checkpoints,        (independent single-step jobs, no COND)
 RPTGEN00  reports           ─┘ BCHCTL/RCVPRC       RPTPOS  RPTAUD  RPTSTA  RTNANA
                                                     UTLMNT  UTLMON  UTLVAL
        BCHCTL00 / PRCSEQ00 / RCVPRC00               TSTGEN  TSTVAL
        orchestrate & restart                        (no orchestration, no restart, no DB2 attach)
```

For the migration this means the **job-scheduling model has to be designed
from the documentation and copybooks, not recovered from JCL** – the JCL only
tells us DD → dataset bindings and record formats for the 15 runnable programs.

## 5. Dataset name index

| DSN | Written by (job) | Read by (job) |
|---|---|---|
| `PORTFOLIO.MASTER.FILE` | PORTDEF (define), PORTADD, PORTUPDT, PORTDEL | PORTREAD, PORTADD, PORTUPDT, PORTDEL |
| `PORTFOLIO.INPUT.FILE` | – | PORTADD |
| `PORTFOLIO.UPDATE.FILE` | – | PORTUPDT |
| `PORTFOLIO.DELETE.FILE` | – | PORTDEL |
| `PORTFOLIO.AUDIT.FILE` | PORTDEL | – |
| `PORTFOLIO.TEST.FILE` | PORTTEST | – |
| `PROD.AUDIT.LOG` | – (AUDPROC has no JCL) | RPTAUD |
| `PROD.ERROR.LOG` | – (ERRPROC has no JCL) | RPTAUD |
| `PROD.POSITION.MASTER` | – | RPTPOS, UTLVAL |
| `PROD.TRANSACTION.HISTORY` | – | RPTPOS, UTLVAL |
| `PROD.DB2.STATISTICS` | – | RPTSTA (seq), UTLMON (KSDS) |
| `PROD.BATCH.STATISTICS` | – | RPTSTA |
| `PROD.CONTROL.FILE` | – | UTLMNT |
| `PROD.ARCHIVE.FILE` | UTLMNT | – |
| `PROD.MONITOR.CONFIG` | – | UTLMON |
| `PROD.MONITOR.LOG`, `PROD.MONITOR.ALERTS` | UTLMON | – |
| `PROD.VALIDATION.CONTROL` | – | UTLVAL |
| `PROD.*.REPORT` (AUDIT, DAILY.POSITION, SYSTEM.STATS, MAINTENANCE, VALIDATION) | RPTAUD, RPTPOS, RPTSTA, UTLMNT, UTLVAL | printer |
| `TEST.CONFIG.FILE`, `TEST.RANDOM.SEED` | – | TSTGEN |
| `TEST.PORTFOLIO.DATA`, `TEST.TRANSACTION.DATA` | TSTGEN | – (should feed PORTADD `INPTFILE` / PORTTRAN `TRANFILE`, but no JCL connects them) |
| `TEST.CASE.FILE`, `TEST.EXPECTED.RESULTS`, `TEST.ACTUAL.RESULTS` | – | TSTVAL |
| `TEST.VALIDATION.REPORT` | TSTVAL | – |
| `PORTFOLIO.POSITION.VSAM` | – | CICS `POSFILE` (CSD, not JCL) |
| `PROD.LOAD.LIBRARY`, `TEST.LOAD.LIBRARY`, `YOUR.LOADLIB` | – | STEPLIBs (three different libraries; `YOUR.LOADLIB` is a placeholder) |
