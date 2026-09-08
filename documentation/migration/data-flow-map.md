# Data-Flow Map

Which programs read and write which persistent stores (sequential files, VSAM
clusters, DB2 tables, CICS files, BMS maps), and how data moves between them.
Derived from `SELECT … ASSIGN TO`, `OPEN`, `READ/WRITE/REWRITE/DELETE`, `EXEC SQL`
and `EXEC CICS` statements in `src/programs/`, cross-referenced with the DD
statements in `src/jcl/` and the resource definitions in `src/cics/PORTDFN.csd`.

## 1. System-level picture

```
                    BATCH (JCL)                                   ONLINE (CICS, tran PINQ)
 ┌────────────────────────────────────────────────┐   ┌──────────────────────────────────────────┐
 │                                                │   │  3270 ── INQSET/MENMAP ──► INQONLN        │
 │  INPTFILE ─► PORTADD ─┐                        │   │            │  SECMGR ──► DB2 AUTHFILE (r)  │
 │  UPDTFILE ─► PORTUPDT─┼─► PORTFILE (KSDS) ◄─┐  │   │            │         └─► DB2 AUDITLOG (w)  │
 │  DELEFILE ─► PORTDEL ─┘        │            │  │   │            ├─ INQP ─► INQPORT ─► POSFILE(r)│
 │                 └─► AUDFILE    │            │  │   │            │             └─► POSMAP        │
 │  TRANFILE ─► PORTTRAN ─────────┘ (r/w)      │  │   │            └─ INQH ─► INQHIST              │
 │                 ├─► AUDPROC ─► AUDFILE      │  │   │                        ├─ DB2ONLN (CONNECT)│
 │                 └─► ERRPROC ─► ERRLOG(flat) │  │   │                        ├─ DB2RECV          │
 │                                             │  │   │                        └─ CURSMGR ─► DB2   │
 │  PORTREAD ◄──── PORTFILE ────────────────────┘  │   │                             POSHIST (r)   │
 │  PORTTEST ─► TESTFILE                          │   │                              └─► HISMAP    │
 │                                                │   │  any error ─► ERRHNDL ─► DB2 ERRLOG (w)   │
 │  TRANHIST(KSDS) ─► HISTLD00 ─► DB2 POSHIST (w) │   └──────────────────────────────────────────┘
 │        │             └─► BCHCTL (checkpoint r/w)│
 │        ├─► RPTPOS00 ◄─ POSMSTRE ─► RPTFILE     │       ┌──────────── control plane ───────────┐
 │        └─► UTLVAL00 ◄─ POSMSTRE ─► ERRRPT      │       │ PRCSEQ(KSDS) ─► PRCSEQ00 / RCVPRC00   │
 │                                                │       │ BCHCTL(KSDS) ◄─► BCHCTL00 / PRCSEQ00  │
 │  AUDITLOG + ERRLOG(flat) ─► RPTAUD00 ─► RPTFILE│       │ CKPTFILE(KSDS) ◄─► CKPRST (stub)      │
 │  DB2STATS + BCHSTATS ─► RPTSTA00 ─► RPTFILE    │       │ DB2 RTNCODES ◄─ RTNCDE00 ─► RTNANA00  │
 │  MONCFG + DB2STATS ─► UTLMON00 ─► MONLOG,ALERTS│       │ SESSION.DBSTATS ◄─► DB2STAT           │
 │  CTLFILE ─► UTLMNT00 ─► ARCHFILE, RPTFILE      │       │ DB2 ERRLOG ◄─ DB2ERR                  │
 │  TSTCFG,RANDSEED ─► TSTGEN00 ─► PORTOUT,TRANOUT│       └──────────────────────────────────────┘
 │  TESTCASE,EXPECTED,ACTUAL ─► TSTVAL00 ─► TESTRPT│
 └────────────────────────────────────────────────┘
```

Key observation: **there is no code path from `TRANFILE` / `PORTFILE` to
`POSMSTRE`, `TRANHIST` or DB2 `POSHIST`.** The portfolio programs maintain
`PORTFILE`; the reporting/history programs consume `POSMSTRE` / `TRANHIST`; the
step that should bridge them (`POSUPDT` / `POSUPD00`, plus a `TRNVAL00`-style
validator that writes `TRANHIST`) does not exist. The two halves of the system are
connected only by documentation.

## 2. Store inventory

### 2.1 Sequential (QSAM) files

| DDNAME | Producer(s) | Consumer(s) | Record layout (source) | LRECL | JCL DSN |
|---|---|---|---|---|---|
| `TRANFILE` | *(external / TSTGEN00 `TRANOUT`)* | PORTTRAN (READ) | `TRNREC.cpy` `TRANSACTION-RECORD` | data-dict: 200 FB; copybook sums to 152 (incl. 50-byte filler) | not in any JCL |
| `INPTFILE` | *(external)* | PORTADD (READ) | `PORTFLIO.cpy` | 100 (data dict) / copybook 148 | `PORTFOLIO.INPUT.FILE` |
| `UPDTFILE` | *(external)* | PORTUPDT (READ) | inline `UPDT-KEY X(18), UPDT-ACTION X(1), UPDT-NEW-VALUE X(50)` | 80 | `PORTFOLIO.UPDATE.FILE` |
| `DELEFILE` | *(external)* | PORTDEL (READ) | inline `DEL-KEY X(18), DEL-REASON-CODE X(2)` | 80 | `PORTFOLIO.DELETE.FILE` |
| `TESTFILE` | PORTTEST (WRITE) | — | `PORTFLIO.cpy` | — | `PORTFOLIO.TEST.FILE` |
| `AUDFILE` | AUDPROC (WRITE, EXTEND), PORTDEL (WRITE, OUTPUT) | RPTAUD00 (as `AUDITLOG`) | AUDPROC: `AUDITLOG.cpy` (392 bytes); PORTDEL: 80-byte inline layout | conflicting | `PORTFOLIO.AUDIT.FILE` / `PROD.AUDIT.LOG` |
| `AUDITLOG` | (= `AUDFILE`) | RPTAUD00 (READ) | `AUDITLOG.cpy` | — | `PROD.AUDIT.LOG` |
| `ERRLOG` (flat) | ERRPROC (WRITE) | RPTAUD00 (READ) | ERRPROC `LOG-DATA X(400)` = `ERR-MESSAGE` image | 400 | `PROD.ERROR.LOG` |
| `RPTFILE` | RPTAUD00, RPTPOS00, RPTSTA00, RTNANA00, UTLMNT00 (WRITE) | printer | 132-byte print lines | 132 | `PROD.*.REPORT`, `SYSOUT=*` |
| `PRCCTL` | — | — | documented in data dictionary (80 FB) only; **no program or JCL references it** | 80 | — |
| `BCHSTATS` | *(none in repo)* | RPTSTA00 (READ) | inline `BST-*` | — | `PROD.BATCH.STATISTICS` |
| `DB2STATS` (RPTSTA00) | *(none in repo)* | RPTSTA00 (READ, seq) | `COPY DB2STAT` (missing) | — | `PROD.DB2.STATISTICS` |
| `DB2STATS` (UTLMON00) | *(none in repo)* | UTLMON00 (READ, **KSDS**) | `COPY DB2STAT` (missing) | — | `PROD.DB2.STATISTICS` (same DSN, different organisation) |
| `MONCFG` | *(external)* | UTLMON00 | inline `CFG-*` | — | `PROD.MONITOR.CONFIG` |
| `MONLOG` | UTLMON00 | — | inline `LOG-*` | — | `PROD.MONITOR.LOG` |
| `ALERTS` | UTLMON00 | — | inline `ALERT-*` | — | `PROD.MONITOR.ALERTS` |
| `CTLFILE` | *(external)* | UTLMNT00 | inline `CTL-*` | — | `PROD.CONTROL.FILE` |
| `ARCHFILE` | UTLMNT00 | — | variable, 32 760 | V | `PROD.ARCHIVE.FILE` |
| `VALCTL` | *(external)* | UTLVAL00 | inline `VAL-*` | — | `PROD.VALIDATION.CONTROL` |
| `ERRRPT` | UTLVAL00 | — | 132-byte print | 132 | `PROD.VALIDATION.REPORT` |
| `TSTCFG` | *(external)* | TSTGEN00 | inline `CFG-*` | — | `TEST.CONFIG.FILE` |
| `RANDSEED` | *(external)* | TSTGEN00 | `SEED-VALUE 9(9)` | — | `TEST.RANDOM.SEED` |
| `PORTOUT` | TSTGEN00 | *(→ INPTFILE)* | `PORTFLIO.cpy` | — | `TEST.PORTFOLIO.DATA` |
| `TRANOUT` | TSTGEN00 | *(→ TRANFILE)* | `TRNREC.cpy` | — | `TEST.TRANSACTION.DATA` |
| `TESTCASE`, `EXPECTED`, `ACTUAL` | *(external)* | TSTVAL00 | inline | — | `TEST.CASE.FILE`, `TEST.EXPECTED.RESULTS`, `TEST.ACTUAL.RESULTS` |
| `TESTRPT` | TSTVAL00 | — | 132 | 132 | `TEST.VALIDATION.REPORT` |

### 2.2 VSAM clusters

| DDNAME | Org / access in code | Key in code | Readers | Writers | Definition source(s) |
|---|---|---|---|---|---|
| `PORTFILE` | KSDS; random (PORTADD, PORTDEL, PORTTRAN, PORTUPDT) or dynamic (PORTMSTR, PORTREAD) | `PORT-KEY` (ID 8 + account 10 = 18) in `PORTFLIO`; `PORT-ID` X(10) in PORTMSTR | PORTREAD, PORTTRAN, PORTUPDT, PORTDEL, PORTMSTR | PORTADD (WRITE), PORTTRAN / PORTUPDT / PORTMSTR (REWRITE), PORTDEL / PORTMSTR (DELETE), PORTMSTR (WRITE) | `PORTDEF.jcl`: `KEYS(18 0) RECORDSIZE(200 200)` on `PORTFOLIO.MASTER.FILE`; `vsam-definitions.txt` `PORTMSTR`: `KEYS(12 0) RECORDSIZE(400 400)`, key = portfolio 8 + account type 2 + branch 2 (the `PORTFOLIO_MASTER` DB2 shape, not `PORTFLIO.cpy`) |
| `POSMSTRE` | KSDS, INPUT (RPTPOS00 seq; UTLVAL00 dynamic) | `POS-KEY` (`POSREC.cpy`: portfolio 8 + date 8 + investment 10 = 26) | RPTPOS00, UTLVAL00 | **none in repo** (POSUPDT missing) | data dict: key = account + fund, 250 bytes; `vsam-definitions.txt` has no `POSMSTRE`, but its `POSHIST` VSAM entry (KSDS 350 bytes, key length 18, key comment = portfolio 8 + date 8 + investment 10 = 26) matches `POSREC.cpy`'s key fields |
| `POSFILE` (CICS) | KSDS via `EXEC CICS READ` | `POSITION-ACCOUNT` (undefined) | INQPORT | — | CSD: `PORTFOLIO.POSITION.VSAM`, `RECORDSIZE(200)`, READ/BROWSE only |
| `TRANHIST` | KSDS; seq (HISTLD00, RPTPOS00), dynamic (UTLVAL00) | `TH-KEY` (HISTLD00, undefined), `TRN-KEY` (RPTPOS00), `TRAN-KEY` (UTLVAL00, undefined) | HISTLD00, RPTPOS00, UTLVAL00 | **none in repo** | data dict: ESDS 300 bytes; `vsam-definitions.txt`: KSDS 300 bytes, `KEY LENGTH: 20` but key-structure comment lists date 8 + time 6 + portfolio 8 + sequence 6 = 28 (= `TRNREC.cpy` `TRN-KEY`) |
| `BCHCTL` | KSDS dynamic | `BCT-KEY` (job 8 + date 8 + seq 4) | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00 | BCHCTL00 (intended), HISTLD00 (REWRITE checkpoint), RCVPRC00 (REWRITE) | data dict: key = process date + process id, 200 bytes; `BCHCTL.cpy` ≈ 446 bytes |
| `PRCSEQ` | KSDS dynamic | `PSR-KEY` | PRCSEQ00, RCVPRC00 | — | `PRCSEQ.cpy` only; no IDCAMS / data-dict entry |
| `CKPTFILE` | KSDS dynamic | `CKR-KEY` (`CKPRST.cpy`) | CKPRST (stub) | CKPRST (stub) | `CKPRST.cpy`; data dict "Checkpoint Data" described as part of batch control |
| `DB2STATS` | KSDS dynamic (UTLMON00) | `STAT-KEY` (from missing `DB2STAT` copybook) | UTLMON00 | — | none |

### 2.3 DB2 objects

| Table | DDL | Writers (INSERT/UPDATE) | Readers | Column-shape agreement |
|---|---|---|---|---|
| `POSHIST` | `src/database/db2/POSHIST.sql` (18 cols, PK `ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME`, range-partitioned ×4 on `TRANS_DATE` (2024 quarters)) | HISTLD00 (`INSERT … VALUES (:POSHIST-RECORD)`) | INQHIST (dynamic SELECT via CURSMGR) | HISTLD00 host struct `DBTBLS.cpy POSHIST-RECORD` = 18 fields ✔; INQHIST selects `TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT` ✘ (DDL: `QUANTITY, PRICE, AMOUNT`) |
| `ERRLOG` | `src/database/db2/ERRLOG.sql` (10 cols, PK `ERROR_TIMESTAMP, PROGRAM_ID`) | DB2ERR (`INSERT … VALUES (:WS-ERRLOG-REC)` 10 fields ✔), ERRHNDL (8 positional values ✘) | DB2ERR (`SELECT ERROR_MESSAGE, ERROR_SEVERITY, ADDITIONAL_INFO … MAX(ERROR_TIMESTAMP)`), `ERRLOG_CLEANUP` proc | mixed |
| `RTNCODES` | `src/database/db2/RTNCODES.sql` (6 cols, PK `TIMESTAMP, PROGRAM_ID`) | RTNCDE00 | RTNANA00 (`PRGCUR`), RTNCDE00 (analyze) | ✔ |
| `SESSION.DBSTATS` | declared at run time by DB2STAT (`DECLARE GLOBAL TEMPORARY TABLE`) | DB2STAT | DB2STAT | n/a (session scoped, lost at disconnect) |
| `AUTHFILE` | **no DDL** | — | SECMGR (`SELECT COUNT(*) … WHERE USER_ID, RESOURCE, ACCESS_TYPE`) | — |
| `AUDITLOG` (DB2) | **no DDL** | SECMGR (`INSERT … (TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE)`) | — | name collides with flat-file `AUDITLOG` DD and `AUDITLOG.cpy` |
| `PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY`, views `ACTIVE_PORTFOLIOS`, `CURRENT_POSITIONS` | `src/database/db2/db2-definitions.sql` | **no program** | **no program** | relational target model that the VSAM `PORTFILE` / `POSMSTRE` / `TRANHIST` data maps onto; unused by code |
| `SYSIBM.SYSDUMMY1` | catalog | — | DB2CONN (`SELECT CURRENT SERVER`) | — |
| Plan `PORTPLAN`, DB `POSMVP`, tablespaces `POSHIST`, `ERRLOG` | `PORTPLAN.sql`, `POSHIST.sql`, `ERRLOG.sql` | — | — | `CONNECT TO POSMVP` in `DBPROC.cpy` and `DB2ONLN` |

### 2.4 CICS resources (`src/cics/PORTDFN.csd`, `src/maps/INQSET.bms`)

| Resource | Type | Used by | Notes |
|---|---|---|---|
| `PINQ` | TRANSACTION → `INQONLN` | terminal users | only transaction defined |
| `POSFILE` | FILE → `PORTFOLIO.POSITION.VSAM` (`RECORDSIZE(200)`, READ/BROWSE yes, UPDATE/DELETE no) | INQPORT `READ FILE('POSFILE')` | 200-byte record matches `PORTDEF.jcl`'s `PORTFOLIO.MASTER.FILE`, not the 250-byte `POSMSTRE` of the data dictionary; DSN differs from every batch DD |
| `INQSET` | MAPSET (`MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP`) | INQONLN, INQPORT, INQHIST | INQONLN references `INQMAP` / `INQMNU`, which are **not** in the mapset |
| `PORTDB2` / `PORTPLAN` | DB2ENTRY / plan | all `EXEC SQL` online programs | `CONNECT TO POSMVP` is also issued explicitly by `DB2ONLN` |
| `INQONLN`, `INQPORT`, `INQHIST`, `DB2ONLN`, `CURSMGR`, `DB2RECV`, `SECMGR` | PROGRAM | — | `ERRHNDL` is LINKed but **not defined in the CSD** |
| group `PORTGRP`, list `PORTLST` | GROUP / LIST | — | |

## 3. Program × store access matrix

`R` read, `W` write/insert, `U` rewrite/update, `D` delete, `C` connect/commit-only.
Stores with no program access are omitted (see §2 for those).

| Program | PORTFILE | POSMSTRE/POSFILE | TRANHIST | BCHCTL | PRCSEQ | CKPTFILE | TRANFILE | AUDFILE | ERRLOG flat | RPTFILE | DB2 POSHIST | DB2 ERRLOG | DB2 RTNCODES | DB2 AUTHFILE | DB2 AUDITLOG | SESSION.DBSTATS | BMS INQSET |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| BCHCTL00 | | | | R U W* | | | | | | | | | | | | | |
| CKPRST | | | | | | R W* | | | | | | | | | | | |
| HISTLD00 | | | R | R U | | | | | | | W | | | | | | |
| PRCSEQ00 | | | | R | R | | | | | | | | | | | | |
| RCVPRC00 | | | | R U | R | | | | | | | | | | | | |
| RPTAUD00 | | | | | | | | R | R | W | | | | | | | |
| RPTPOS00 | | R | R | | | | | | | W | | | | | | | |
| RPTSTA00 | | | | | | | | | | W | | | | | | | |
| RTNANA00 | | | | | | | | | | W | | | R | | | | |
| RTNCDE00 | | | | | | | | | | | | | R W | | | | |
| AUDPROC | | | | | | | | W | | | | | | | | | |
| DB2CMT | | | | | | | | | | | C | | | | | | |
| DB2CONN | | | | | | | | | | | C | | | | | | |
| DB2ERR | | | | | | | | | | | | R W | | | | | |
| DB2STAT | | | | | | | | | | | | | | | | R W U | |
| ERRPROC | | | | | | | | | W | | | | | | | | |
| CURSMGR | | | | | | | | | | | R† | | | | | | |
| DB2ONLN | | | | | | | | | | | C | | | | | | |
| DB2RECV | | | | | | | | | | | C | | | | | | |
| ERRHNDL | | | | | | | | | | | | W | | | | | |
| INQHIST | | | | | | | | | | | R (via CURSMGR) | | | | | | send HISMAP |
| INQONLN | | | | | | | | | | | | | | | | | recv INQMAP‡, send INQMNU‡ |
| INQPORT | | R (CICS) | | | | | | | | | | | | | | | send POSMAP |
| SECMGR | | | | | | | | | | | | | | R | W | | |
| PORTADD | W | | | | | | | | | | | | | | | | |
| PORTDEL | R D | | | | | | | W | | | | | | | | | |
| PORTMSTR | R W U D | | | | | | | (via AUDPROC) | (via ERRPROC) | | | | | | | | |
| PORTREAD | R | | | | | | | | | | | | | | | | |
| PORTTRAN | R U | | | | | | R | (via AUDPROC) | (via ERRPROC) | | | | | | | | |
| PORTUPDT | R U | | | | | | | | | | | | | | | | |
| UTLVAL00 | | R | R | | | | | | | | | | | | | | |
| RPTSTA00 / UTLMON00 / UTLMNT00 / TSTGEN00 / TSTVAL00 / PORTTEST | see §2.1 for their private sequential files | | | | | | | | | | | | | | | | |

`*` intended, paragraphs not implemented. `†` executes whatever statement text the caller supplies; in practice `POSHIST`. `‡` map names not present in `INQSET.bms`.

## 4. End-to-end flows

### 4.1 Portfolio maintenance (implemented)

```
 INPTFILE ──READ──► PORTADD ──validate(BR-P01..P03)──WRITE──► PORTFILE
 UPDTFILE ──READ──► PORTUPDT ──READ/REWRITE(S|V|N)──────────► PORTFILE
 DELEFILE ──READ──► PORTDEL ──READ/DELETE──────────────────► PORTFILE
                          └──WRITE 80-byte audit line─────► AUDFILE
 PORTFILE ──READ NEXT──► PORTREAD ──DISPLAY──► SYSOUT
```

### 4.2 Transaction processing (partially implemented)

```
 TRANFILE ──READ──► PORTTRAN
                      ├─ 2100-VALIDATE (BR-T01..T04)  ──fail──► ERRPROC ──► ERRLOG(flat)
                      └─ 2200-UPDATE-POSITIONS  [never PERFORMed]
                            ├─ READ PORTFILE by TRN-PORTFOLIO-ID
                            ├─ BU/SL/FE arithmetic (BR-T05..T08)
                            ├─ REWRITE PORTFILE
                            └─ AUDPROC ──► AUDFILE
```

Note that the record read from `PORTFILE` is described by the missing `PORTREC`
copybook and uses fields `PORT-TOTAL-UNITS`, `PORT-TOTAL-COST`, `PORT-STATUS`,
none of which (except `PORT-STATUS`) exist in `PORTFLIO.cpy`.

### 4.3 Documented daily batch cycle (mostly missing)

```
 TRANFILE ─► [TRNVAL00 / TRNMAIN]  ─► validated TRANFILE      (no source)
          ─► [POSUPD00 / POSUPDT]  ─► POSMSTRE, TRANHIST      (empty file)
 TRANHIST ─► HISTLD00              ─► DB2 POSHIST              (exists)
 POSMSTRE + TRANHIST ─► RPTPOS00   ─► daily position report    (exists)
 AUDITLOG + ERRLOG   ─► RPTAUD00   ─► audit report             (exists)
 DB2STATS + BCHSTATS ─► RPTSTA00   ─► statistics report        (exists)
 RTNCODES            ─► RTNANA00   ─► return-code report       (exists)
 control: PRCSEQ ─► PRCSEQ00 ─► BCHCTL ◄─► BCHCTL00 / RCVPRC00 ; CKPTFILE ◄─► CKPRST
```

### 4.4 Online inquiry (implemented, with name mismatches)

```
 3270 ─PINQ─► INQONLN
               ├─ SECMGR(V) ASSIGN USERID ─► SECMGR(A) SELECT COUNT(*) FROM AUTHFILE ─► SECMGR(L) INSERT AUDITLOG
               ├─ RECEIVE MAP INQMAP
               ├─ INQP ─► INQPORT ─► READ FILE POSFILE RIDFLD(account) ─► SEND POSMAP
               ├─ INQH ─► INQHIST ─► DB2ONLN(C) ─[fail]─► DB2RECV(C) ─► DB2ONLN(C) ×3
               │                    └─► CURSMGR(D,O,F,C) "SELECT … FROM POSHIST WHERE ACCOUNT_NO = ?" ─► SEND HISMAP
               └─ error ─► ERRHNDL ─► INSERT INTO ERRLOG ─► RETURN | CONTINUE | ABEND('IERR')
```

### 4.5 Error and audit sinks (two of each)

```
                 ┌─ batch/common ──► ERRPROC ──► ERRLOG (flat, 400 B, ERR-MESSAGE image)
 errors ─────────┤
                 └─ DB2ERR / ERRHNDL ─────────► ERRLOG (DB2 table, 10 columns)

                 ┌─ AUDPROC ──► AUDFILE (AUDITLOG.cpy image)
 audit ──────────┼─ PORTDEL ──► AUDFILE (80-byte inline layout)
                 └─ SECMGR  ──► AUDITLOG (DB2 table, no DDL)
```

A migration target needs a single error store and a single audit store with a
superset schema; `RPTAUD00` currently reads only the flat-file variants.

## 5. Field-level lineage: `TRANHIST` → `POSHIST`

`HISTLD00 2100-PROCESS-RECORD` (the only implemented cross-store transformation):

| Source (`TH-*`, undefined layout) | Target (`DBTBLS.cpy` `PH-*`) | DB2 column | Transform |
|---|---|---|---|
| `TH-ACCOUNT-NO` | `PH-ACCOUNT-NO` X(8) | `ACCOUNT_NO CHAR(8)` | move |
| `TH-PORTFOLIO-ID` | `PH-PORTFOLIO-ID` X(10) | `PORTFOLIO_ID CHAR(10)` | move |
| `TH-TRANS-DATE` / `TH-TRANS-TIME` | `PH-TRANS-DATE` X(10) / `PH-TRANS-TIME` X(8) | `DATE` / `TIME` | move (ISO string) |
| `TH-TRANS-TYPE` | `PH-TRANS-TYPE` X(2) | `CHAR(2)` | move |
| `TH-SECURITY-ID` | `PH-SECURITY-ID` X(12) | `CHAR(12)` | move |
| `TH-QUANTITY`, `TH-PRICE` | `PH-QUANTITY`, `PH-PRICE` S9(12)V9(3) | `DECIMAL(15,3)` | move |
| `TH-AMOUNT`, `TH-FEES`, `TH-TOTAL-AMOUNT`, `TH-COST-BASIS`, `TH-GAIN-LOSS` | `PH-*` S9(13)V9(2) | `DECIMAL(15,2)` | move |
| — | `PH-PROCESS-DATE`, `PH-PROCESS-TIME`, `PH-PROGRAM-ID`, `PH-USER-ID`, `PH-AUDIT-TIMESTAMP` | `PROCESS_DATE`, `PROCESS_TIME`, `PROGRAM_ID`, `USER_ID`, `AUDIT_TIMESTAMP` | **not set** – left as `INITIALIZE` values (spaces), although the columns are `NOT NULL DATE/TIME/CHAR`; only `AUDIT_TIMESTAMP` has a default |

Every `POSHIST` row is therefore a 1:1 copy of a `TRANHIST` record; no aggregation
happens in the load, and provenance columns are not populated. SQLCODE −803
(duplicate key) is silently skipped, making the load idempotent on re-run.
