# Migration Analysis – Inventory of the COBOL Codebase (AB-285)

This directory is the output of Jira **AB-285 "[01] Inventory and analysis of
COBOL codebase"** (epic AB-276). It is a source-grounded description of what is
*actually in the repository*, cross-checked against
`documentation/technical/system-architecture.md` and
`documentation/technical/data-dictionary.md`. Wherever the source and the
documentation disagree, the disagreement is recorded rather than resolved –
those items are the inputs to the design decisions in the follow-on tickets.

## Documents

| File | Answers |
|---|---|
| [`program-inventory-and-call-graph.md`](program-inventory-and-call-graph.md) | What programs exist, what kind they are, who calls whom (`CALL`, `EXEC CICS LINK`), which copybooks each uses, per-program I/O / SQL / CICS / error behaviour |
| [`data-flow-map.md`](data-flow-map.md) | Which programs read/write which sequential files, VSAM clusters, DB2 tables and CICS resources; end-to-end flows; `TRANHIST` → `POSHIST` field lineage |
| [`copybook-entity-mapping.md`](copybook-entity-mapping.md) | What each of the 20 copybooks becomes in a target domain model (entity / value object / enum / contract / platform glue), with field-level notes |
| [`business-rules-catalog.md`](business-rules-catalog.md) | Every business rule found in code or documentation, where it is enforced, and whether code and documentation agree |
| [`jcl-job-inventory.md`](jcl-job-inventory.md) | Every JCL member, step, DD → dataset binding, and its agreement with program `ASSIGN` clauses; documented vs actual batch cycle |
| this file | Traceability (every artifact → where it is covered) and the consolidated risk register |

## How the pieces fit

```
                     documentation/technical/*.md  (intended design)
                                   │
                                   ▼  compared against
 ┌───────────────┐   ┌────────────────────────┐   ┌───────────────────────┐
 │ src/programs  │──▶│ program-inventory-and- │──▶│ business-rules-       │
 │  38 .cbl      │   │ call-graph.md          │   │ catalog.md            │
 └───────────────┘   └────────────────────────┘   └───────────────────────┘
        │                        │  uses copybooks           ▲ rules live in
        │ ASSIGN / SQL / CICS    ▼                           │ paragraphs of
        ▼            ┌────────────────────────┐              │
 ┌───────────────┐   │ copybook-entity-       │──────────────┘
 │ src/jcl       │   │ mapping.md  (20 .cpy)  │
 │ src/database  │   └────────────────────────┘
 │ src/cics      │                │ record layouts
 │ src/maps      │                ▼
 └───────┬───────┘   ┌────────────────────────┐   ┌───────────────────────┐
         └──────────▶│ data-flow-map.md       │◀──│ jcl-job-inventory.md  │
                     │ (files, VSAM, DB2,     │   │ (DD → DSN bindings)   │
                     │  CICS, lineage)        │   └───────────────────────┘
                     └────────────────────────┘
```

## Traceability

### Programs (`src/programs/`, 38 files – ticket says 37; see inventory §1)

| File | Inventory | Data flow | Rules | JCL |
|---|---|---|---|---|
| `batch/BCHCTL00.cbl` | P-01 | §3 matrix, §2.2 `BCHCTL` | BR-B02–B04 | none (§3) |
| `batch/CKPRST.cbl` | P-02 | §2.2 `CKPTFILE` | BR-B07, B10 | none |
| `batch/HISTLD00.cbl` | P-03 | §2.2, §2.3 `POSHIST`, §5 lineage | BR-B07–B09, BR-D01–D04 | none |
| `batch/POSUPDT.cbl` (empty) | P-04 | §4.3 | BR-B01, BR-T05–T08 (gap) | none |
| `batch/PRCSEQ00.cbl` | P-05 | §2.2 `PRCSEQ` | BR-B01–B03, B11 | none |
| `batch/RCVPRC00.cbl` | P-06 | §2.2 `BCHCTL`, `PRCSEQ` | BR-B05, B06 | none |
| `batch/RPTAUD00.cbl` | P-07 | §2.1 `AUDITLOG`, `ERRLOG` | BR-R02 | J-02 |
| `batch/RPTPOS00.cbl` | P-08 | §2.1/2.2 `POSMSTRE`, `TRANHIST` | BR-R01 | J-03 |
| `batch/RPTSTA00.cbl` | P-09 | §2.1 `DB2STATS`, `BCHSTATS` | BR-R03 | J-04 |
| `batch/RTNANA00.cbl` | P-10 | §2.3 `RTNCODES` | BR-R04 | J-01 |
| `batch/RTNCDE00.cbl` | P-11 | §2.3 `RTNCODES` | BR-D08, BR-S | none |
| `common/AUDPROC.cbl` | P-12 | §2.1 `AUDFILE`, §4.5 | BR-A07, A08 | none |
| `common/DB2CMT.cbl` | P-13 | §3 | BR-D02 | none |
| `common/DB2CONN.cbl` | P-14 | §2.3 `SYSIBM.SYSDUMMY1` | BR-D01 | none |
| `common/DB2ERR.cbl` | P-15 | §2.3 `ERRLOG` | BR-A06, BR-D03, D04 | none |
| `common/DB2STAT.cbl` | P-16 | §2.3 `SESSION.DBSTATS` | — | none |
| `common/ERRPROC.cbl` | P-17 | §2.1 `ERRLOG` (flat), §4.5 | BR-A01–A04 | none |
| `online/CURSMGR.cbl` | P-18 | §2.3 `POSHIST` (dynamic SQL) | BR-O05 | CSD |
| `online/DB2ONLN.cbl` | P-19 | §2.4 `PORTDB2` | BR-D06 | CSD |
| `online/DB2RECV.cbl` | P-20 | §4.4 | BR-D07, BR-O06 | CSD |
| `online/ERRHNDL.cbl` | P-21 | §2.3 `ERRLOG` (8-col insert) | BR-A09, A10 | **not in CSD** |
| `online/INQHIST.cbl` | P-22 | §2.3 `POSHIST`, §4.4 | BR-O05 | CSD |
| `online/INQONLN.cbl` | P-23 | §2.4 `INQSET`, §4.4 | BR-O01–O03 | CSD (`PINQ`) |
| `online/INQPORT.cbl` | P-24 | §2.2 `POSFILE`, §4.4 | BR-O04 | CSD |
| `online/SECMGR.cbl` | P-25 | §2.3 `AUTHFILE`, `AUDITLOG` (DB2) | BR-O01, O02 | CSD |
| `portfolio/PORTADD.cbl` | P-26 | §2.1 `INPTFILE`, §2.2 `PORTFILE`, §4.1 | BR-P01–P06 | J-06 |
| `portfolio/PORTDEL.cbl` | P-27 | §2.1 `DELEFILE`, `AUDFILE`, §4.1 | BR-P08, P09 | J-08 |
| `portfolio/PORTMSTR.cbl` | P-28 | §2.2 `PORTFILE` (100-byte variant) | BR-P01–P03, P06, P10 | none |
| `portfolio/PORTREAD.cbl` | P-29 | §2.2 `PORTFILE` | — | J-09 |
| `portfolio/PORTTEST.cbl` | P-30 | §2.1 `TESTFILE` | — | J-10 |
| `portfolio/PORTTRAN.cbl` | P-31 | §2.1 `TRANFILE`, §4.2 | BR-T01–T08, T12, T13 | none |
| `portfolio/PORTUPDT.cbl` | P-32 | §2.1 `UPDTFILE`, §4.1 | BR-P07 | J-07 |
| `portfolio/PORTVALD.cbl` | P-33 | — (no I/O) | BR-P01, BR-V01–V04 | none |
| `test/TSTGEN00.cbl` | P-34 | §2.1 `TSTCFG`, `PORTOUT`, `TRANOUT`, `RANDSEED` | — | J-11 |
| `test/TSTVAL00.cbl` | P-35 | §2.1 `TESTCASE`, `EXPECTED`, `ACTUAL`, `TESTRPT` | — | J-12 |
| `utility/UTLMNT00.cbl` | P-36 | §2.1 `CTLFILE`, `ARCHFILE` | — | J-13 |
| `utility/UTLMON00.cbl` | P-37 | §2.1/2.2 `MONCFG`, `MONLOG`, `ALERTS`, `DB2STATS` | — | J-14 |
| `utility/UTLVAL00.cbl` | P-38 | §2.1/2.2 `VALCTL`, `POSMSTRE`, `TRANHIST`, `ERRRPT` | BR-R05 | J-15 |

Column references: *Inventory* = `program-inventory-and-call-graph.md` §2/§4;
*Data flow* = `data-flow-map.md`; *Rules* = `business-rules-catalog.md`;
*JCL* = `jcl-job-inventory.md` §2 (`J-nn`) or §3 for programs without JCL.

### Copybooks (`src/copybook/`, 20 files)

| File | Mapping | Also referenced in |
|---|---|---|
| `common/AUDITLOG.cpy` | C-01 → `AuditEvent` | data-flow §2.1 `AUDFILE`; inventory §5 matrix |
| `common/COMMON.cpy` | C-02 → enums/constants | rules BR-P03, BR-T02 |
| `common/ERRHAND.cpy` | C-03 → `ErrorMessage` contract + enums | rules BR-A01–A03, A10 |
| `common/HISTREC.cpy` | C-04 → `ChangeHistory` | data-flow §5 (not the `TH-*` layout) |
| `common/PORTFLIO.cpy` | C-05 → `Portfolio` | data-flow §2.2 `PORTFILE`; JCL J-05, J-10, J-11 |
| `common/PORTVAL.cpy` | C-06 → validation constants | rules BR-V01–V04 |
| `common/POSREC.cpy` | C-07 → `Position` | data-flow §2.2 `POSMSTRE` |
| `common/RETHND.cpy` | C-08 → `OperationResult` contract | rules BR-A05, A06, BR-S |
| `common/RTNCODE.cpy` | C-09 → `ReturnCodeRequest` contract | rules BR-D08, BR-S |
| `common/TRNREC.cpy` | C-10 → `Transaction` | data-flow §2.1 `TRANFILE`, §2.2 `TRANHIST`; JCL J-11 |
| `batch/BCHCON.cpy` | C-11 → batch constants | rules BR-B |
| `batch/BCHCTL.cpy` | C-12 → `BatchJobRun` | data-flow §2.2 `BCHCTL` |
| `batch/CKPRST.cpy` | C-13 → `Checkpoint` | data-flow §2.2 `CKPTFILE`; rules BR-B07, B08, B10 |
| `batch/PRCSEQ.cpy` | C-14 → `ProcessDefinition` | data-flow §2.2 `PRCSEQ`; rules BR-B01–B03 |
| `db2/DBPROC.cpy` | C-15 → platform glue | rules BR-D02 |
| `db2/DBTBLS.cpy` | C-16 → `PositionHistory`, `ErrorLogEntry` | data-flow §2.3, §5 |
| `db2/SQLCA.cpy` | C-17 → platform status | — |
| `online/DB2REQ.cpy` | C-18 → `Db2ConnectionRequest` contract | data-flow §4.4 |
| `online/ERRHND.cpy` | C-19 → `OnlineError` contract | rules BR-A09 |
| `online/INQCOM.cpy` | C-20 → `InquiryRequest` contract | rules BR-O03 |

Copybooks *referenced but absent*: `DB2STAT`, `PORTREC`, `SQLPOS` –
see inventory §4 (P-09, P-24, P-31, P-37) and mapping §5.

### JCL (`src/jcl/`, 16 members + 2 empty READMEs)

All covered in `jcl-job-inventory.md` §2 as J-01 … J-16.

### DB2 (`src/database/db2/`)

| Artifact | Objects | Covered in |
|---|---|---|
| `POSHIST.sql` | DB `POSMVP`, TS `POSHIST` (range-partitioned on `TRANS_DATE`), table `POSHIST` (18 cols), `POSHIST_PK`, `POSHIST_IX1`, `POSHIST_IX2`, comments, grants | data-flow §2.3, §5; mapping C-16 |
| `ERRLOG.sql` | TS `ERRLOG`, table `ERRLOG` (10 cols), `ERRLOG_PK`, `ERRLOG_IX1`, procedure `ERRLOG_CLEANUP`, grants | data-flow §2.3, §4.5; mapping C-16; rules BR-D03/D04 |
| `RTNCODES.sql` | table `RTNCODES` (6 cols), `RTNCODES_PRG_IDX`, `RTNCODES_STS_IDX` | data-flow §2.3; inventory P-10, P-11 |
| `PORTPLAN.sql` | `BIND PLAN PORTPLAN PKLIST(*.PORTPKG.*) ISOLATION(CS) …` | data-flow §2.3, §2.4 (`PORTDB2`) |
| `db2-definitions.sql` | tables `PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY`; indexes `IDX_PORT_MASTER_CLIENT`, `IDX_POSITIONS_DATE`, `IDX_TRANS_HIST_PORT`, `IDX_TRANS_HIST_DATE`; views `ACTIVE_PORTFOLIOS`, `CURRENT_POSITIONS` | data-flow §2.3 (unused by any program) |
| *(no DDL)* `AUTHFILE`, `AUDITLOG` (DB2), `SESSION.DBSTATS` | referenced by SECMGR / DB2STAT only | data-flow §2.3 |

### VSAM (`src/database/vsam/vsam-definitions.txt` + `src/jcl/portfolio/PORTDEF.jcl`)

| Cluster | Covered in |
|---|---|
| `PORTMSTR` / `PORTFOLIO.MASTER.FILE` (400/12 in definitions, 200/18 in PORTDEF) | data-flow §2.2 `PORTFILE`; JCL J-05 |
| `TRANHIST` (KSDS 300/20) | data-flow §2.2 `TRANHIST` |
| `POSHIST` VSAM (KSDS 350/18) | data-flow §2.2 `POSMSTRE` note |
| clusters used by code but defined nowhere: `POSMSTRE`, `BCHCTL`, `PRCSEQ`, `CKPTFILE`, `DB2STATS` (KSDS), CICS `PORTFOLIO.POSITION.VSAM` | data-flow §2.2 |

### CICS / BMS (`src/cics/PORTDFN.csd`, `src/maps/INQSET.bms`)

Transaction `PINQ`; programs `INQONLN`, `INQPORT`, `INQHIST`, `DB2ONLN`,
`CURSMGR`, `DB2RECV`, `SECMGR`; mapset `INQSET` (maps `MENMAP`, `POSMAP`,
`HISMAP`, `ERRMAP`); file `POSFILE`; `DB2ENTRY PORTDB2`; `DB2TRAN PINQ`; group
`PORTGRP`; list `PORTLST` – all in data-flow §2.4 and inventory §3.2.

## Risk register

Consolidated from the five documents; `R-nn` ids are referenced from the
follow-on design tickets. **Severity**: H = blocks a faithful migration until
decided, M = needs a decision but has an obvious default, L = clean-up.

| ID | Sev | Risk | Evidence | Where documented |
|---|---|---|---|---|
| R-01 | H | The documented daily batch cycle (`TRNVAL00 → POSUPD00 → HISTLD00 → RPTGEN00`) does not exist: no source for `TRNVAL00`, `POSUPD00`, `RPTGEN00`; `POSUPDT.cbl` is empty; no JCL runs `HISTLD00` or any orchestration program | `system-architecture.md`, `PRCSEQ.cpy`, `src/jcl/**` | inventory §3.4; data-flow §4.3; JCL §3–4 |
| R-02 | H | No code path connects portfolio/transaction input (`PORTFILE`, `TRANFILE`) to `POSMSTRE`, `TRANHIST` or DB2 `POSHIST`; the reporting side reads stores nothing writes | data-flow §1, §3 | data-flow §4.3 |
| R-03 | H | Transaction position arithmetic (buy/sell/fee/transfer) is dead code: `2200-UPDATE-POSITIONS` is never performed and depends on the missing `PORTREC` copybook | `PORTTRAN.cbl` | rules BR-T05–T08; inventory P-31 |
| R-04 | H | Three different physical definitions of the portfolio master: `PORTDEF.jcl` 200/18, `vsam-definitions.txt` 400/12, `PORTMSTR.cbl` inline 100/10; `PORTFLIO.cpy` computes to 148 | JCL J-05; mapping C-05 | data-flow §2.2; JCL J-05 |
| R-05 | H | Missing copybooks `DB2STAT`, `PORTREC`, `SQLPOS` → RPTSTA00, UTLMON00, PORTTRAN, INQPORT cannot compile as-is; `DB2STAT.cbl` exists only as a program | inventory §4 | inventory §4; mapping §5 |
| R-06 | H | `HISTLD00` uses `TH-*` fields that no copybook defines; the actual `TRANHIST` record layout used for DB2 loading is unknown | `HISTLD00.cbl` vs `HISTREC.cpy` | data-flow §5; mapping C-04 |
| R-07 | M | `TRANHIST` is ESDS (300) in the data dictionary but KSDS in `vsam-definitions.txt`, and the stated key length (20) disagrees with the key-structure comment (28 = `TRN-KEY`) | data-dictionary §3; vsam-definitions | data-flow §2.2 |
| R-08 | M | `POSMSTRE` key is account+fund (data dict) vs portfolio+date+investment (`POSREC.cpy`); no IDCAMS definition; the `POSHIST` VSAM entry appears to be it | data-flow §2.2 | data-flow §2.2; mapping C-07 |
| R-09 | M | Two error sinks (`ERRPROC` flat `ERRLOG` and DB2 `ERRLOG`) and two audit sinks (`AUDFILE` flat, DB2 `AUDITLOG`); `ERRHNDL` inserts 8 positional values into a 10-column table; `AUDFILE` receives two different record layouts (AUDPROC 392 / PORTDEL 80) | data-flow §4.5; JCL J-08 | data-flow §4.5; rules BR-A |
| R-10 | M | Error-code vocabularies conflict: `RETHND.cpy` `E001–E010` technical vs data-dictionary `E001–E004` business; four different portfolio-status vocabularies (`PORTFLIO`, `COMMON`, PORTADD, data dict) | rules BR-P03, BR-S | rules §1, §9 |
| R-11 | M | Online layer name mismatches: `INQONLN` uses maps `INQMAP`/`INQMNU` (not in `INQSET`); `INQHIST` selects `TRANS_UNITS/TRANS_PRICE/TRANS_AMOUNT` (DDL: `QUANTITY/PRICE/AMOUNT`); `INQPORT` reads into an undefined `POSITION-ACCOUNT`; `ERRHNDL` LINKed but not in the CSD | online sources vs `INQSET.bms`, `POSHIST.sql`, `PORTDFN.csd` | data-flow §2.3–2.4; inventory §3.2 |
| R-12 | M | `DB2ONLN` returns the active-connection count in its status field, so callers cannot distinguish success from failure; `DB2RECV` retry logic is therefore unreliable | `DB2ONLN.cbl` | inventory P-19; rules BR-D06, D07 |
| R-13 | M | Checkpoint/restart is documentation-only for most programs: `CKPRST` is a stub, `HISTLD00` re-reads the whole input on restart and relies on `-803` skipping, `PORTTRAN` has no checkpoint at all | rules BR-B | rules §5 |
| R-14 | M | Documented business validations with no code anywhere: future-dated transactions, fund existence, active-position check, average-cost recalculation | data-dictionary §5 | rules BR-T09–T11 |
| R-15 | M | `PRCCTL` (documented 80-byte control file) and the relational `PORTFOLIO_MASTER` / `INVESTMENT_POSITIONS` / `TRANSACTION_HISTORY` model are referenced by nothing in code | data-flow §2.1, §2.3 | data-flow §2 |
| R-16 | L | JCL-level defects: `TSTGEN.jcl` `LRECL=100` for 148/152-byte records; `PROD.DB2.STATISTICS` opened as QSAM by RPTSTA00 and KSDS by UTLMON00; `RTNANA00` needs DB2 but runs as plain `EXEC PGM`; `YOUR.LOADLIB` placeholder; three STEPLIBs | JCL §2 | JCL J-01, J-04, J-06, J-11, J-14 |
| R-17 | L | `PORTVALD` validates `PORT` + 4 digits while `PORTMSTR` validates `PORT` + 5 digits | rules BR-V01 vs BR-P01 | rules §1, §3 |
| R-18 | L | `POSHIST` provenance columns (`PROCESS_DATE`, `PROCESS_TIME`, `PROGRAM_ID`, `USER_ID`) are never populated by `HISTLD00`; the table's range partitions end at 2024-12-31 | `HISTLD00.cbl`, `POSHIST.sql` | data-flow §5 |
| R-19 | L | Several programs are skeletons whose leaf paragraphs are empty or undefined (`CKPRST`, `RPTPOS00` summary, `UTLVAL00`, `UTLMNT00`, `TSTVAL00`, `RPTSTA00`) | inventory §2 status column | inventory §2, §4 |
| R-20 | L | Ticket count (37) vs files (38) – `POSUPDT.cbl` is an empty placeholder | `find src/programs -name '*.cbl'` | inventory §1 |

## Method

* Every relationship was extracted from source text (`CALL '…'`,
  `EXEC CICS LINK PROGRAM('…')`, `COPY` / `EXEC SQL INCLUDE`,
  `SELECT … ASSIGN TO`, `OPEN/READ/WRITE/REWRITE/DELETE`, `EXEC SQL`,
  `EXEC CICS`, JCL `DD`/`DSN`, IDCAMS `DEFINE`, DDL, CSD, BMS macros).
  No relationship is asserted from the architecture prose alone; where the
  prose describes something the source does not implement, it is tagged
  `DOC` / missing.
* Record lengths were computed from `PIC` clauses (COMP-3 = ⌈(digits+1)/2⌉
  bytes, COMP = 2/4/8 bytes) and compared against JCL `LRECL`, IDCAMS
  `RECORDSIZE` and the data dictionary.
* Nothing in `src/` was modified; missing programs and copybooks were **not**
  recreated.
