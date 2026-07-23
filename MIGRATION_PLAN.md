# Mainframe Exit Migration Plan — Investment Portfolio Management System

**Repository:** `COG-GTM/COBOL-Legacy-Benchmark-Suite`
**Workload:** Investment Portfolio Management System (IPMS) — core banking, COBOL on IBM i (AS/400) / IBM z (z/OS)
**Document type:** Migration planning & architecture recommendation (feeds a reading/learning module)
**Audience:** Banking migration steering committee, enterprise architects, risk & compliance

---

## 0. TL;DR — The Recommendation

> **Do NOT start with either "service-first" or "data-first" as a big-bang.**
> The correct order for a *banking* core is: **(1) Discovery & Assessment → (2) Anti-Corruption Layer + parallel-run reconciliation harness → (3) Data platform stood up with CDC/dual-write (data flows first, but the mainframe stays system-of-record) → (4) Domain-by-domain Strangler Fig migration of services → (5) Progressive read-then-write cutover per domain → (6) Decommission.**

In one line: **Establish the data foundation and reconciliation safety net FIRST, then migrate services domain-by-domain using the Strangler Fig pattern, cutting over reads before writes.** This is a *data-first foundation, service-first delivery* hybrid. Pure "convert all data then flip" and pure "rewrite all services then migrate data" both fail the banking test (data gravity, referential integrity, zero-data-loss, auditability, rollback).

Recommended per-component strategy (6 R's): **Refactor** the two core write engines (`POSUPDT`, `PORTTRAN`), **Replatform** the reporting/utility batch, **Retain/encapsulate** stable inquiry logic behind an API façade until its domain is reached, **Retire** the test harness and dead code (e.g. the empty `POSUPDT.cbl` stub), and only **Repurchase (COTS)** if a domain proves to be undifferentiated commodity. **Rehost (lift-and-shift emulation)** is the fallback for schedule pressure, not the goal.

---

## 1. Codebase Assessment

### 1.1 Component Inventory (migration scope)

Counts taken directly from the repo tree.

| Category | Count | Programs / Artifacts |
|---|---|---|
| **Batch programs** | 11 | `BCHCTL00` (batch control), `PRCSEQ00` (process sequencer), `CKPRST` (checkpoint/restart), `RCVPRC00` (recovery), `POSUPDT` *(empty stub — see hotspot)*, `HISTLD00` (history→DB2 load), `RPTPOS00`/`RPTAUD00`/`RPTSTA00` (reports), `RTNANA00`/`RTNCDE00` (return-code analysis) |
| **Online / CICS programs** | 8 | `INQONLN` (menu controller), `INQPORT` (portfolio inquiry), `INQHIST` (history inquiry), `CURSMGR` (cursor), `SECMGR` (security), `ERRHNDL` (error), `DB2ONLN` (DB2 controller), `DB2RECV` (DB2 recovery) |
| **Portfolio (domain core)** | 8 | `PORTMSTR` (master, 287 LOC), `PORTTRAN` (transaction engine, 316 LOC), `PORTADD`, `PORTUPDT`, `PORTDEL`, `PORTREAD`, `PORTVALD` (validation), `PORTTEST` |
| **Common subroutines** | 6 | `DB2CONN`, `DB2CMT` (commit), `DB2ERR`, `DB2STAT`, `AUDPROC` (audit), `ERRPROC` (error) |
| **Utility** | 3 | `UTLMNT00` (file maint/reorg), `UTLVAL00` (data validation/reconciliation), `UTLMON00` (monitor) |
| **Test** | 2 | `TSTGEN00` (data gen), `TSTVAL00` (validation) |
| **Copybooks (shared record layouts)** | 20 | `common/` (PORTFLIO, POSREC, TRNREC, HISTREC, PORTVAL, AUDITLOG, RTNCODE, ERRHAND, RETHND, COMMON), `batch/`, `db2/` (SQLCA, DBTBLS, DBPROC), `online/` (INQCOM, DB2REQ) |
| **JCL** | 15 | batch reports, portfolio CRUD, utility, test orchestration |
| **DB2 schema (SQL)** | 5 | `PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY`, `POSHIST`, `RTNCODES`, `ERRLOG` |
| **VSAM files** | 3 | `PORTMSTR` (KSDS, 400B), `TRANHIST` (KSDS, 300B), `POSHIST` (KSDS, 350B) |
| **BMS map** | 1 | `INQSET.bms` (3270 screens) |
| **CICS CSD** | 1 | `PORTDFN.csd` |

**Technology surface (grep-verified):** `EXEC SQL` (DB2) in **14** programs; `EXEC CICS` (online) in **8**; `COMP-3` packed-decimal in **7** files; VSAM file `ASSIGN` in **25** programs.

### 1.2 Dependency Map

```
                         ┌────────────────────────────────────────┐
                         │            z/OS SCHEDULER                │
                         └───────────────────┬────────────────────┘
                                             │ triggers
                     ┌───────────────────────▼───────────────────────┐
   BATCH CONTROL     │  BCHCTL00 ── PRCSEQ00 ── CKPRST ── RCVPRC00     │
   (checkpoint/      │      (control / sequence / checkpoint / recover)│
    restart spine)   └───────┬──────────────────────────────┬─────────┘
                             │                                │
              ┌──────────────▼─────────┐          ┌──────────▼──────────────┐
   WRITE PATH │  PORTTRAN  → POSUPDT   │          │  HISTLD00 (VSAM→DB2 load)│
   (money     │  (txn engine)(pos upd) │          └──────────┬──────────────┘
    moves)    │        │        │      │                     │
              │  PORTVALD (validate)   │                     │
              └────┬────────┬──────────┘                     │
                   │        │                                │
        ┌──────────▼──┐  ┌──▼──────────┐            ┌────────▼─────────┐
DATA    │ PORTMSTR    │  │ TRANHIST     │           │   DB2 TABLES     │
LAYER   │ (VSAM KSDS) │  │ POSHIST(VSAM)│◄──────────│  PORTFOLIO_MASTER│
        │  400B/COMP-3│  │              │  dual copy │  INVESTMENT_POS  │
        └──────┬──────┘  └──────┬───────┘            │  TRANSACTION_HIST│
               │                │                    └────────┬─────────┘
        ┌──────▼────────────────▼──────┐                      │
REPORT  │ RPTPOS00 / RPTAUD00 / RPTSTA00│◄─────────────────────┘
& UTIL  │ UTLVAL00 (reconcile) UTLMON00 │
        └───────────────────────────────┘

        ┌──────────────────────────────── ONLINE (CICS / 3270) ───────────────┐
        │  User → INQONLN ─┬─ SECMGR (auth)                                    │
        │                  ├─ INQPORT ─┐                                       │
        │                  ├─ INQHIST ─┤→ DB2ONLN → DB2RECV   reads PORTMSTR,  │
        │                  └─ CURSMGR  ┘   (conn/recovery)     TRANHIST, DB2   │
        └──────────────────────────────────────────────────────────────────────┘

   CROSS-CUTTING (used everywhere): DB2CONN·DB2CMT·DB2ERR·DB2STAT (DB2 access),
   AUDPROC (audit trail), ERRPROC/ERRHNDL (errors), RTNCODE framework.
   SHARED COPYBOOKS bind layouts across batch+online: PORTFLIO, POSREC, TRNREC, HISTREC.
```

**Key structural facts that drive the plan:**

- **Dual persistence of the same entities.** Portfolio/position/transaction data lives in **both VSAM** (`PORTMSTR`, `TRANHIST`, `POSHIST`) **and DB2** (`PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY`). `HISTLD00` bridges VSAM→DB2. This is the single biggest data-integrity and reconciliation challenge.
- **Shared copybooks are the coupling.** `PORTFLIO`, `POSREC`, `TRNREC`, `HISTREC` are consumed by batch *and* online programs. A record-layout change ripples across both channels — copybooks are the de-facto contract and become the anti-corruption boundary's schema source.
- **Batch control is a spine, not a leaf.** `BCHCTL00`/`PRCSEQ00`/`CKPRST`/`RCVPRC00` implement checkpoint/restart and ordered dependencies (`TRNVAL → POSUPD → HISTLD → RPT`). Any target must preserve restartability and idempotency.
- **Two channels, one source of truth.** Online (CICS pseudo-conversational, COMMAREA state) reads the same masters that batch writes. Cutover must keep both channels consistent at all times.

### 1.3 Complexity / Risk Hotspots

| Hotspot | Why it's dangerous | Rank |
|---|---|---|
| **`PORTTRAN` (316) + `POSUPDT` write path** | Core money-movement + cost-basis; ACID-critical; `POSUPDT.cbl` is an **empty 0-line stub** → spec/behavior gap that must be reverse-engineered before rewrite | 🔴 Critical |
| **VSAM↔DB2 dual copy** | Same entity in two stores with *different keys* (VSAM PORT key = ID+AcctType+Branch = 12B; DB2 PK = PORTFOLIO_ID only). Reconciliation & key-mapping risk | 🔴 Critical |
| **`COMP-3` packed decimal (7 files)** | Financial precision (`S9(13)V99`); naive binary→decimal conversion loses/round-trips money. Needs exact `DECIMAL(18,2/4)` mapping + penny-level reconciliation | 🔴 Critical |
| **Checkpoint/restart spine** (`BCHCTL00`/`CKPRST`/`RCVPRC00`) | Restart semantics and commit points must be reproduced or batches can double-post | 🟠 High |
| **CICS pseudo-conversational + COMMAREA** | Online state model doesn't map 1:1 to stateless web/API; `SECMGR` auth semantics | 🟠 High |
| **Cross-channel shared copybooks** | Contract coupling; change amplification across batch+online | 🟠 High |
| **Audit trail (`AUDPROC`, `AUDITLOG`, `ERRLOG`)** | Regulatory continuity — must never have a gap across the cutover | 🟠 High |
| Reporting/utility batch (`RPT*`, `UTL*`) | Read-mostly, lower coupling — safe early wins | 🟢 Low |

---

## 2. Candidate Strategies — The 6 R's

Scored 1 (poor) – 5 (excellent) against banking-weighted criteria. Weights reflect that in a *regulated core banking* context, functional fidelity, data integrity, and compliance dominate cost/time.

**Criteria & weights:** Functional Fidelity ×3 · Risk (inverse) ×3 · Data-Integrity ×3 · Regulatory/Compliance ×2 · Cost (inverse) ×1 · Time (inverse) ×1 · Team Skills fit ×2.

| Strategy | FuncFid ×3 | Risk ×3 | DataInteg ×3 | Reg ×2 | Cost ×1 | Time ×1 | Skills ×2 | **Weighted** |
|---|---|---|---|---|---|---|---|---|
| **1. Rehost** (emulator, e.g. Micro Focus / AWS M2, run COBOL as-is off-mainframe) | 5 | 4 | 5 | 4 | 4 | 5 | 4 | **62** |
| **2. Replatform** (recompile COBOL to JVM/.NET or managed COBOL; VSAM→RDBMS; JCL→scheduler) | 4 | 3 | 4 | 3 | 3 | 3 | 3 | **50** |
| **3. Refactor / Rearchitect** (rewrite to Java/Kotlin/Go microservices + modern RDBMS) | 4 | 2 | 3 | 3 | 1 | 1 | 3 | **41** |
| **4. Repurchase / COTS core** (buy a portfolio-mgmt/core-banking package) | 2 | 2 | 2 | 3 | 2 | 2 | 2 | **32** |
| **5. Retain / Encapsulate** (wrap with API façade, leave in place for now) | 5 | 5 | 5 | 4 | 5 | 5 | 4 | **68** |
| **6. Retire** (delete dead/duplicate/test-only code) | n/a — applies per-component |  |  |  |  |  |  | — |

**Weighted score = Σ(rating × weight). Max = 75.**

### 2.1 How to read this table

The high scores for **Retain/Encapsulate (68)** and **Rehost (62)** are *not* a recommendation to stop there — they score high because they are *low-risk and fast*, which is exactly what you want for the **early phases and for domains you have not reached yet**. The lower scores for Refactor (41) reflect its *per-domain* risk/cost, which is acceptable *when applied selectively to the differentiated core*, not to all 42 programs at once.

> **The winning play is not one R — it is a sequenced blend:** Retain/encapsulate everything Day 1 (buy time + safety), Rehost as the schedule-risk fallback, then Refactor the differentiated core and Replatform the commodity batch **domain-by-domain**, Retiring dead code as you go, and considering Repurchase only where a domain is proven undifferentiated.

### 2.2 Per-component 6R disposition

```
                                6-R DISPOSITION MAP
  Differentiated / high value ▲
                              │   REFACTOR                REFACTOR
                              │   PORTTRAN, POSUPDT        PORTMSTR, PORTVALD
                              │   (money movement,         (master + validation)
                              │    cost basis)
        Business value        │───────────────────────────────────────────────
                              │   REPLATFORM               RETAIN → later refactor
                              │   RPT*, HISTLD00,          INQ* online, SECMGR
                              │   UTL* (batch/reports)     (encapsulate behind API)
                              │
                              │   RETIRE
                              │   POSUPDT.cbl (empty stub), TSTGEN/TSTVAL,
                              │   PORTTEST, duplicate VSAM-vs-DB2 once unified
  Commodity / low value  ─────┴───────────────────────────────────────────────►
                                  Low coupling            High coupling
```

---

## 3. The Sequencing Question — Service-First vs Data-First vs Strangler

This is the crux of the user's question. Three archetypes:

### 3.1 Option A — DATA-FIRST ("convert the data, then move logic")

```
  [ Convert ALL VSAM+DB2 → target RDBMS ]──►[ Rewrite services against new DB ]──►[ Flip ]
        big-bang schema + ETL                     services lag data
```
**Pro:** clean target schema early; one data model.
**Con (banking-fatal):**
- Creates a long window where **new DB is authoritative but old services still run** → dual-write or freeze. Freezing a bank core is a non-starter.
- **Data gravity + referential integrity**: `INVESTMENT_POSITIONS`→`PORTFOLIO_MASTER`, `TRANSACTION_HISTORY`→`PORTFOLIO_MASTER` FKs and the VSAM/DB2 key mismatch mean a one-shot conversion is enormously risky.
- No incremental value; huge blast radius; rollback = restore entire core.

### 3.2 Option B — SERVICE/LOGIC-FIRST ("rewrite services, migrate data last")

```
  [ Rewrite services on NEW platform ]──►[ services call OLD data via bridge ]──►[ move data last ]
        logic modernized first                chatty cross-platform I/O
```
**Pro:** business logic modernized early; data untouched (stable).
**Con (banking-fatal):**
- New services calling **legacy VSAM/DB2 across a network** = latency, transaction-boundary and 2-phase-commit nightmares; ACID across platforms is brittle.
- You've rewritten `PORTTRAN`/`POSUPDT` but still depend on mainframe locking/commit — **no real exit**, and double the surface to reconcile.
- Cost of the hardest thing (the write path) is paid up front with the least safety net.

### 3.3 Option C — DOMAIN-BY-DOMAIN STRANGLER FIG  ✅ RECOMMENDED

```
   FAÇADE / ANTI-CORRUPTION LAYER routes each call to OLD or NEW per-domain
   ┌───────────────────────────────────────────────────────────────────────┐
   │  clients / channels  ─────►  API Façade + Router  ─────►  ┌── NEW svc ──┐│
   │                                     │                     │ (migrated)  ││
   │                                     └──────────────►  ┌── OLD COBOL ────┐│
   │                                                        (not yet reached)││
   └───────────────────────────────────────────────────────────────────────┘
   Data kept in sync by CDC / dual-write; reconciliation proves equivalence;
   reads cut over first, writes second, one domain at a time.
```

**Why this wins for a bank:**

| Banking concern | Data-first | Service-first | **Strangler (C)** |
|---|---|---|---|
| Data gravity / referential integrity | ✗ big-bang | ~ bridged | ✅ moved per-domain with FKs intact |
| Transactional consistency (ACID) | ~ freeze | ✗ cross-platform 2PC | ✅ writes stay single-store per domain |
| Reconciliation / dual-run | hard (all at once) | partial | ✅ built-in, continuous, per-domain |
| Downtime tolerance | ✗ large window | ~ | ✅ near-zero, incremental |
| Rollback | ✗ restore everything | ✗ | ✅ re-route one domain back |
| Incremental value / learning | ✗ | ~ | ✅ each domain de-risks the next |
| Auditability continuity | ~ | ~ | ✅ façade logs both sides |

### 3.4 The nuance: "data-first foundation, service-first delivery"

The strangler still forces an order **within each domain**, and here the banking answer is precise:

> **Stand up the data platform and keep it continuously synced FIRST (data foundation), but keep the mainframe as system-of-record. Then migrate the SERVICE and cut over READS before WRITES. Flip write authority last, only after parallel-run reconciliation is green.**

This is neither "convert all data then flip" nor "rewrite all services then move data." It is: **data plumbing first (CDC/dual-write + reconciliation), service logic second (strangled per domain), write-authority cutover last.** Data has gravity, so its *pipes and parity* must exist before any service can safely be trusted; but you do **not** convert-and-flip data wholesale — you let each migrated service pull its domain's write authority across only after reads are proven equivalent.

---

## 4. Recommended Phased Roadmap

```
 PHASE 0        PHASE 1          PHASE 2            PHASE 3           PHASE 4         PHASE 5
 DISCOVERY  →   FOUNDATION   →   DATA PIPES     →   STRANGLE       → CUTOVER      →  DECOMMISSION
 & ASSESS       ACL + Recon      CDC/Dual-write     domain-by-domain  read→write      retire MF
 ─────────      ───────────      ────────────       ───────────────   ──────────      ───────────
 inventory      API façade       target schema      migrate domain    reads first     shut z/OS
 deps map       anti-corruption  CDC from VSAM/DB2  services (ACL      then writes     & AS/400
 risk hotspots  parallel-run     reconciliation     switch routes)     per domain      archive audit
 6R decisions   harness (shadow) engine (penny)     validate parity    rollback ready  final recon
 pilot pick     NO writes moved  MF still SoR       shadow → canary    zero-data-loss  cost-out

    ▲              ▲                 ▲                   ▲                 ▲              ▲
  weeks 0-6      6-14             12-24               ongoing per       per domain     after last
                                                      domain (loop)     (loop)         domain
```

Phases 2–4 **loop per domain**. Recommended domain order (lowest risk → highest, to build the reconciliation muscle before touching money):

```
  DOMAIN MIGRATION ORDER (strangle sequence)
  1. Reporting/Analytics ──► RPTPOS00, RPTSTA00, RTNANA00   (read-only, zero write risk)
  2. Online Inquiry      ──► INQONLN, INQPORT, INQHIST      (read-only, customer-facing)
  3. Reference/Portfolio ──► PORTMSTR, PORTADD/UPDT/DEL      (master data, moderate)
  4. Position keeping    ──► POSUPDT, HISTLD00              (write path, high)
  5. Transaction engine  ──► PORTTRAN, PORTVALD             (money movement, highest — last)
  6. Audit/Compliance    ──► AUDPROC + RPTAUD00 continuity  (verified throughout, retired last)
```

### 4.1 Phase entry / exit criteria & deliverables

| Phase | Entry criteria | Exit criteria (gate) | Key deliverables |
|---|---|---|---|
| **0. Discovery & Assessment** | Executive mandate; repo access | Signed component inventory, dependency map, 6R decisions, target-arch decision, pilot domain chosen | This document; automated dep graph; risk register; **reverse-engineered spec for `POSUPDT` stub** |
| **1. Foundation (ACL + Recon)** | Phase 0 gate | API façade live in front of MF; parallel-run/shadow harness compares MF outputs to (empty) new side; **no writes moved** | Anti-corruption layer; façade API contract from copybooks (`PORTFLIO`/`POSREC`/`TRNREC`); reconciliation harness + dashboards |
| **2. Data Pipes** | Phase 1 gate | Target schema deployed; CDC streaming VSAM+DB2→target; **penny-level reconciliation green** for chosen domain; MF still system-of-record | Target RDBMS schema (COMP-3→DECIMAL map, VSAM/DB2 key unification); CDC/dual-write pipeline; ETL for history backfill; recon engine |
| **3. Strangle (per domain)** | Domain data reconciling clean | New service passes functional-parity + shadow tests vs COBOL; router can send domain reads to new side | Migrated service (Refactor/Replatform per §2.2); parity test suite; canary routing |
| **4. Cutover (per domain)** | Shadow parity ≥ agreed threshold, N days | **Reads** cut to new side, monitored; then **writes** cut over; MF becomes replica for that domain; rollback tested | Read-cutover runbook; write-cutover runbook; rollback runbook; sign-off record |
| **5. Decommission** | All domains cut over; final recon; regulatory sign-off | z/OS + AS/400 powered off; audit archive retained; cost-out realized | Archival package; decommission cert; audit-trail continuity attestation |

### 4.2 Data-migration strategy (detail)

1. **Schema conversion.** VSAM KSDS + DB2 → unified target RDBMS. Map `COMP-3 S9(13)V99` → `DECIMAL(18,2)` (quantities `DECIMAL(18,4)`). **Resolve the VSAM/DB2 key mismatch** (VSAM PORT key ID+AcctType+Branch vs DB2 `PORTFOLIO_ID`) into one canonical key with a documented mapping table. Preserve FKs (`INVESTMENT_POSITIONS`/`TRANSACTION_HISTORY` → `PORTFOLIO_MASTER`).
2. **ETL / initial load.** Bulk extract + transform historical `TRANHIST`/`POSHIST`; validate row counts, control totals, and hash of money columns.
3. **CDC / dual-write.** Change-data-capture from VSAM (log/exit-based) and DB2 into target so it stays live; MF remains system-of-record until per-domain write cutover. Dual-write only at the moment of write-cutover to enable instant rollback.
4. **Reconciliation.** Continuous, automated, **penny-level** balance and row-level parity; exception queue; daily control-total attestation. Reuse the intent of `UTLVAL00` (existing reconciliation utility) as the oracle.
5. **Cutover.** Per domain: quiesce → final delta sync → verify zero lag → switch write authority → keep MF as hot replica for rollback window → decommission after clean days.

### 4.3 Service-migration strategy (detail)

- **Strangler Fig:** the façade/router incrementally redirects traffic from COBOL to new services per domain; the legacy system shrinks until it's gone.
- **Anti-Corruption Layer (ACL):** translates between the legacy copybook/COMMAREA model and the new domain model so neither side leaks its model into the other. Copybooks (`PORTFLIO`, `POSREC`, `TRNREC`, `HISTREC`) are the source of the initial contract.
- **API façade:** exposes stable REST/gRPC over both old and new implementations; also the point where **CICS pseudo-conversational/COMMAREA state** is adapted to stateless services and `SECMGR` auth is mapped to modern IAM.

### 4.4 Target Architecture (conceptual)

```
        ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
        │ Web / Mobile │   │ Branch teller│   │ Batch / feeds│  channels
        └──────┬───────┘   └──────┬───────┘   └──────┬───────┘
               └──────────────────┼──────────────────┘
                          ┌───────▼─────────┐
                          │  API FAÇADE /    │  auth (modern IAM ← SECMGR),
                          │  STRANGLER ROUTER│  audit tap, rate limit
                          └───────┬─────────┘
             per-domain routing   │
        ┌─────────────┬───────────┼───────────┬───────────────┐
        ▼             ▼           ▼           ▼               ▼
  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  ┌──────────────┐
  │Portfolio │ │Positions │ │Txn engine│ │Reporting │  │ ANTI-CORRUPT │
  │ service  │ │ service  │ │ service  │ │ service  │  │ LAYER → COBOL│
  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘  │ (not-yet-    │
       │            │            │            │        │  migrated)   │
       └────────────┴─────┬──────┴────────────┘        └──────┬───────┘
                          ▼                                    ▼
                 ┌──────────────────┐              ┌───────────────────────┐
                 │  TARGET RDBMS     │◄──── CDC ────│  z/OS DB2 + VSAM       │
                 │ (ACID, HA, PITR)  │──── dual ───►│  (system-of-record     │
                 │  DECIMAL money    │    write     │   until cutover)       │
                 └────────┬─────────┘              └───────────────────────┘
                          ▼
                 ┌──────────────────┐
                 │ RECONCILIATION &  │ penny-level parity, control totals,
                 │ AUDIT (append-log)│ immutable audit trail (AUDPROC lineage)
                 └──────────────────┘
```

---

## 5. Banking-Specific Risk & Governance

| Control | Requirement | How it's satisfied here |
|---|---|---|
| **Dual-run reconciliation** | Every migrated domain runs in parallel with MF; outputs compared before trust | Reconciliation harness (Phase 1) + penny-level engine (Phase 2); shadow → canary → cutover |
| **Data-integrity verification** | No money created/destroyed; FKs intact | Control totals, row hashes on money columns, FK validation; `UTLVAL00` logic as oracle |
| **Audit-trail continuity** | Regulatory audit log unbroken across cutover | `AUDPROC`/`AUDITLOG`/`ERRLOG` lineage preserved; façade taps both sides; append-only target audit store |
| **Regulatory sign-off gates** | Compliance approval at each phase gate | Phase exit criteria (§4.1) include explicit reg sign-off before write-cutover and before decommission |
| **Rollback / fallback** | Any domain revertible fast | Router re-points domain to MF; MF kept as hot replica during rollback window; dual-write enables instant fallback |
| **Zero-data-loss cutover** | RPO = 0 for money data | Quiesce → final delta → verify zero CDC lag → switch; no write accepted on new side until lag = 0 |
| **Availability targets** | Meet core-banking SLA (e.g. 99.95%+); batch windows honored | Near-zero-downtime incremental cutover; target RDBMS HA + PITR; preserve checkpoint/restart semantics of `BCHCTL00`/`CKPRST` |
| **Change amplification** | Shared copybook changes ripple batch+online | ACL freezes the contract; changes made in one place, versioned |

**Governance cadence:** risk register reviewed per domain; go/no-go at every read-cutover and write-cutover; independent reconciliation attestation retained for the regulator.

---

## 6. Decision Matrix & Final Recommendation

### 6.1 Sequencing decision matrix

| Approach | Data Gravity | Ref. Integrity | ACID | Recon/Dual-run | Downtime | Rollback | Value cadence | **Verdict** |
|---|---|---|---|---|---|---|---|---|
| Data-first big-bang | ✗ | ✗ | ~ | ✗ | ✗ | ✗ | ✗ | Reject |
| Service/logic-first | ~ | ~ | ✗ | ~ | ~ | ✗ | ~ | Reject |
| **Domain-by-domain Strangler (data-foundation-first, read→write)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **Adopt** |

### 6.2 Final recommendation

1. **Order:** Discovery → Foundation (ACL + reconciliation) → Data pipes (CDC/dual-write, MF stays SoR) → Strangle services domain-by-domain → Cut over **reads then writes** per domain → Decommission. **Not** data-first big-bang; **not** service-first with a legacy data bridge.
2. **Answer to "service-level vs data conversion first?":** **Neither as a wholesale first step.** Build the **data foundation (pipes + reconciliation) first** while the mainframe stays system-of-record, then migrate **services first *within* each domain** and flip **write authority last**. Data plumbing precedes services; data *authority* follows them.
3. **Per-component 6R:** Refactor the differentiated core (`PORTTRAN`, `POSUPDT`, `PORTMSTR`, `PORTVALD`); Replatform commodity batch (`RPT*`, `HISTLD00`, `UTL*`); Retain/encapsulate inquiry until reached; Retire dead/test code; Repurchase only a proven-commodity domain. Rehost is the schedule-risk fallback, not the destination.
4. **First move:** reverse-engineer the **empty `POSUPDT.cbl` stub** and pick **Reporting** as the pilot domain — read-only, lowest blast radius, proves the ACL + reconciliation harness before any money moves.

### 6.3 Assumptions

- The repo is a faithful proxy for the real core (batch + CICS online + VSAM/DB2 dual store).
- Regulatory regime requires demonstrable zero-data-loss, audit continuity, and parallel-run before write-cutover.
- Target platform is a modern HA RDBMS + service runtime (cloud or on-prem) with PITR; exact vendor is a Phase 0 decision.
- Team has COBOL/mainframe SMEs available for reverse-engineering during Phases 0–3 (retention risk is itself a project risk).
- Business can tolerate a multi-quarter, incremental program rather than a single flag-day.

---

*Framework is intentionally reusable: the 6-R scoring table (§2), the three-way sequencing matrix (§3/§6), the phase gate table (§4.1), and the governance controls (§5) can be re-applied to any mainframe-exit workload by re-populating the component inventory in §1.*
