# Modernization Prompt: COBOL Portfolio Management → AWS Serverless

> **How to use this document.** This file is a self-contained migration brief. You can paste the entire document into an AI coding agent (or hand it to a development team) as the specification for re-platforming the legacy COBOL portfolio management suite onto AWS serverless services. Every section is intentionally explicit so the executor needs no prior knowledge of the mainframe system. Where the brief says "you must" / "deliver", treat it as a hard requirement.

---

## 0. The Prompt (copy/paste this to an AI agent)

> You are a senior cloud engineer. Re-platform a legacy IBM z/OS COBOL portfolio management system onto an **AWS serverless architecture**. The legacy system stores portfolio master records in a **VSAM KSDS indexed file** and processes them with a suite of batch and online COBOL programs (`PORTMSTR`, `PORTTRAN`, `PORTADD`, `PORTDEL`, `PORTREAD`, `PORTUPDT`, `PORTVALD`, `PORTTEST`). The full source, record layouts, transaction types, validation rules and reason/action codes are specified in the sections below.
>
> Build a functionally-equivalent serverless system that preserves **every business rule** (ID format, status/type domains, transaction semantics, deletion reason codes, update action types, error thresholds, audit trail). Use:
> - **TypeScript** (Node.js 20.x) **or Python 3.12** for all Lambda functions (pick one and stay consistent).
> - **Infrastructure as Code** — AWS CDK (preferred) **or** AWS SAM/CloudFormation. No console click-ops.
> - **Amazon DynamoDB** as the system of record (replacing VSAM).
> - **Amazon API Gateway (REST)** for synchronous CRUD/query access.
> - **AWS Lambda** for all compute.
> - **AWS Step Functions and/or Amazon SQS** for transaction orchestration.
> - **Amazon S3 + event notifications** for batch file ingestion.
> - **DynamoDB Streams** for the audit trail.
> - **IAM least-privilege** roles (one role per function, scoped to the exact resources/actions used).
> - **Observability**: AWS X-Ray tracing on every Lambda + API, structured JSON logs to CloudWatch, CloudWatch metrics and alarms.
>
> Deliver: a project structure, IaC stacks, Lambda handlers, a shared validation module, an OpenAPI/REST API contract, a seed/test-data generator, unit + integration tests, and a CI/CD deployment pipeline. Honor the **Non-Functional Requirements** and produce the **Deliverables** listed at the end. Document any business rule you cannot preserve 1:1 and propose an equivalent.

The remainder of this document is the supporting specification referenced by the prompt above.

---

## 1. Context

- **Platform of origin:** IBM Enterprise COBOL for z/OS.
- **Data store:** VSAM **KSDS** (Key-Sequenced Data Set) indexed file, keyed on the portfolio identifier. Secondary sequential files are used for batch input (additions, updates, deletes, transactions) and audit output.
- **Execution model:** A mix of **online** access (CRUD via a callable controller with a command code) and **batch** jobs (sequential file → apply to VSAM), scheduled via JCL.
- **Cross-cutting services:** A reusable **validation subroutine** (`PORTVALD`), an error handler (`ERRPROC`), and an audit processor (`AUDPROC`) are `CALL`ed by the business programs.
- **Domain:** Investment portfolio management — portfolio master records plus buy/sell/transfer/fee transactions that adjust portfolio positions.
- **Goal:** Replace the mainframe runtime with a pay-per-use, horizontally-scalable AWS serverless stack while preserving 100% of the business logic and the audit guarantees.

> **Note on the source record layout.** The suite contains **two** portfolio record definitions that diverge. The online controller `PORTMSTR` defines a 100-byte record inline; the shared copybook `PORTFLIO` (used by the batch programs) defines a richer layout. Both are documented in the [Data Model](#5-data-model) section. The migration must reconcile these into a **single canonical DynamoDB item** (superset of fields) and the executor must flag the divergence.

---

## 2. Source Architecture

```
                         ┌─────────────────────────────┐
        Online CRUD ───► │  PORTMSTR (C/R/U/D command)  │ ──┐
                         └─────────────────────────────┘   │
                                                            ▼
   Batch add file ─────► PORTADD  ──────────────────►  ┌──────────────────┐
   Batch update file ──► PORTUPDT ──────────────────►  │  VSAM KSDS        │
   Batch delete file ──► PORTDEL  ──────────────────►  │  Portfolio Master │
   Transaction file ───► PORTTRAN ──────────────────►  │  (keyed by PORT)  │
                         PORTREAD (sequential/keyed) ◄─ └──────────────────┘
                                                            │
   PORTVALD (validation subroutine) ◄── CALLed by ─────────┤
   ERRPROC  (error handler)         ◄── CALLed by ─────────┤
   AUDPROC  (audit processor)       ◄── CALLed by ─────────┘
                                                            │
   PORTTEST (test-data generator) ──► sequential seed file ─┘
                         PORTDEL ──► sequential audit file
```

### 2.1 Program Inventory

| Program | Type | Role | Files (access mode) | Key behaviors |
|---|---|---|---|---|
| **PORTMSTR** | Online (callable) | CRUD controller for portfolio master records | `PORTFILE` VSAM indexed, `DYNAMIC` (I-O) | Dispatches on a 1-char command: `C`reate, `R`ead, `U`pdate, `D`elete. Validates before C/U. Returns a `S9(4)` return code (`0` success / `8` error) and an error text. |
| **PORTTRAN** | Batch | Transaction processing (buy/sell/transfer/fee) | `TRANFILE` sequential (input), `PORTFILE` VSAM indexed `RANDOM` (I-O) | Reads transactions sequentially, validates each (portfolio exists, valid type, positive amounts), applies position changes, writes audit records. **Stops when error count > 100.** |
| **PORTADD** | Batch | Bulk-create portfolios from a sequential input file | `INPTFILE` sequential (input), `PORTFILE` VSAM indexed `RANDOM` (I-O) | Validates each input record, stamps create/maint dates, `WRITE`s. Detects **duplicates** (status `22`) and counts add/dup/error. |
| **PORTDEL** | Batch | Process deletion requests + write audit trail | `DELEFILE` sequential (input), `PORTFILE` VSAM `RANDOM` (I-O), `AUDFILE` sequential (output) | Reads delete requests with a **reason code**, deletes the matching portfolio, writes an audit record (timestamp, action, key, reason, status). |
| **PORTREAD** | Batch/utility | Read portfolios sequentially or by key | `PORTFILE` VSAM indexed, `DYNAMIC` (input) | Demonstrates dynamic access — sequential `READ NEXT` browse and keyed read. Displays records, counts total read. |
| **PORTUPDT** | Batch | Field-level updates | `UPDTFILE` sequential (input), `PORTFILE` VSAM `RANDOM` (I-O) | Reads an update record carrying an **action code** and applies a single-field change (Status / Value / Name), then `REWRITE`s. |
| **PORTVALD** | Subroutine | Reusable field validation | none (LINKAGE only) | Validates by request type: `I`d, `A`ccount, `T`ype, a`M`ount. Returns code + message. |
| **PORTTEST** | Utility | Synthetic test-data generator | `TESTFILE` sequential (output) | Generates up to **100** synthetic portfolio records with pseudo-random client type, status, and financials. |

### 2.2 Supporting copybooks / called modules

- `PORTFLIO` — portfolio master record layout (batch).
- `TRNREC` — transaction record layout.
- `PORTVAL` — validation return codes, error messages, min/max amount constants, `ID` prefix.
- `AUDITLOG` — audit record layout.
- `ERRHAND` — error-handling structures; `ERRPROC` is the called error handler.
- `AUDPROC` — called audit-record writer (used by `PORTTRAN`).

---

## 3. Target Architecture (AWS Serverless)

```
                          Amazon API Gateway (REST)
   client ──HTTPS──►  /portfolios            (POST create, GET list/query)
                      /portfolios/{id}       (GET, PUT, PATCH, DELETE)
                              │
                              ▼
                    ┌───────────────────┐         ┌──────────────────────────┐
                    │  CRUD/Query        │ ──────► │  DynamoDB: Portfolio      │
                    │  Lambda(s)         │ ◄────── │  (PK = PORT#<portfolioId>)│
                    └───────────────────┘         └──────────────────────────┘
                              ▲                               │ Streams (NEW+OLD images)
   shared validation ────────┘                               ▼
   (Lambda layer / module)                         ┌──────────────────────────┐
                                                    │  Audit Lambda            │
   S3 (uploads) ──ObjectCreated──► Batch Lambda ──► │  → Audit table / Logs    │
        (add / update / delete / seed files)        └──────────────────────────┘

   Transactions:  API/S3/SQS ──► SQS queue ──► Worker Lambda
                                      │                 └─► (optional) Step Functions
                                      └─► DLQ            for buy/sell/transfer/fee saga
```

Everything is serverless and event-driven. No always-on compute. All resources are defined in IaC.

---

## 4. Service Mapping

| # | Legacy element | AWS target | Notes |
|---|---|---|---|
| 1 | **VSAM KSDS** portfolio master | **Amazon DynamoDB** table `Portfolio` | Partition key = portfolio ID. KSDS keyed lookup → `GetItem`; sequential browse (`READ NEXT`) → `Query`/paginated `Scan`. |
| 2 | **PORTMSTR** C/R/U/D | **API Gateway + Lambda** | Either one Lambda per verb or a single router Lambda dispatching on HTTP method (mirrors the `C/R/U/D` command dispatch). Validate before create/update. Map COBOL return codes to HTTP status. |
| 3 | **PORTTRAN** transaction processing | **SQS + worker Lambda**, optionally orchestrated by **Step Functions** | Each transaction = one SQS message. Worker validates + applies position changes idempotently. Step Functions models the buy/sell/transfer/fee saga and retries. **Error-threshold (>100)** → CloudWatch alarm + DLQ redrive policy. |
| 4 | **PORTADD** batch create | **S3 (ObjectCreated) → Lambda** | Upload an add-file to S3; the trigger parses records, validates, `PutItem` with a **condition expression** (`attribute_not_exists(PK)`) to detect duplicates. Emit add/dup/error counts as metrics. |
| 5 | **PORTDEL** deletion + audit | **Lambda + DynamoDB Streams** | Delete writes the reason code into the item (or a delete marker) so the Stream record carries it; Stream → Audit Lambda → audit table/CloudWatch. Preserves the **3 reason codes**. |
| 6 | **PORTREAD** queries | **API Gateway + Lambda** | Single-record lookup (`GetItem`) and list/browse (`Query` with pagination tokens, replacing sequential `READ NEXT`). |
| 7 | **PORTUPDT** field-level updates | **API Gateway PATCH → Lambda** | `UpdateItem` with an update expression touching only the changed attribute. Preserves the **Status/Value/Name action types**. |
| 8 | **PORTVALD** validation | **Shared Lambda layer / module** | Port the validation rules into a single reusable module imported by every Lambda (and/or published as a Lambda layer). Same return-code/message contract. |
| 9 | **PORTTEST** test data | **Seed script / Lambda** | Generate up to N synthetic items directly into DynamoDB (batch writes). Used for local/dev/integration environments. |
| 10 | **Audit trail** (`AUDPROC`, `AUDFILE`) | **DynamoDB Streams → Lambda → CloudWatch Logs and/or `PortfolioAudit` table** | Every create/update/delete/transaction produces an immutable audit entry with before/after images. |
| 11 | `ERRPROC` error handler | **Centralized error utility + structured logging** | Common error categories/severities → structured CloudWatch logs, metrics, and consistent API error envelopes. |
| 12 | **JCL job scheduling** | **EventBridge Scheduler** (if periodic batch is still required) | Replace JCL-triggered windows; most batch becomes event-driven via S3/SQS instead. |

---

## 5. Data Model

### 5.1 Legacy record layouts

**`PORTMSTR` inline record (100 bytes, online CRUD):**

| Field | COBOL PIC | Meaning |
|---|---|---|
| `PORT-ID` | `PIC X(10)` | Portfolio identifier (record key) |
| `PORT-NAME` | `PIC X(50)` | Portfolio name |
| `PORT-CREATE-DATE` | `PIC X(10)` | Creation date |
| `PORT-STATUS` | `PIC X(01)` | Status — `A`ctive / `I`nactive / `C`losed |
| `PORT-TOTAL-VALUE` | `PIC S9(13)V99 COMP-3` | Total value (packed decimal) |
| `FILLER` | `PIC X(24)` | Reserved |

**`PORTFLIO` copybook record (batch programs — richer, canonical superset):**

| Field | COBOL PIC | Meaning |
|---|---|---|
| `PORT-ID` | `PIC X(8)` | Portfolio identifier (part of key) |
| `PORT-ACCOUNT-NO` | `PIC X(10)` | Account number (part of key) |
| `PORT-CLIENT-NAME` | `PIC X(30)` | Client name |
| `PORT-CLIENT-TYPE` | `PIC X(1)` | `I`ndividual / `C`orporate / `T`rust |
| `PORT-CREATE-DATE` | `PIC 9(8)` | Creation date (YYYYMMDD) |
| `PORT-LAST-MAINT` | `PIC 9(8)` | Last maintenance date (YYYYMMDD) |
| `PORT-STATUS` | `PIC X(1)` | `A`ctive / `C`losed / `S`uspended |
| `PORT-TOTAL-VALUE` | `PIC S9(13)V99 COMP-3` | Total value |
| `PORT-CASH-BALANCE` | `PIC S9(13)V99 COMP-3` | Cash balance |
| `PORT-LAST-USER` | `PIC X(8)` | Last user |
| `PORT-LAST-TRANS` | `PIC 9(8)` | Last transaction reference |
| `PORT-FILLER` | `PIC X(50)` | Reserved |

> **Divergence to reconcile.** `PORTMSTR` uses `PORT-ID X(10)`, `PORT-NAME X(50)`, `PORT-CREATE-DATE X(10)`, and status domain `A/I/C`; `PORTFLIO` uses `PORT-ID X(8)` + `PORT-ACCOUNT-NO X(10)` as a composite key, `PORT-CLIENT-NAME X(30)`, numeric `PORT-CREATE-DATE 9(8)`, and status domain `A/C/S`. The migration must define **one canonical model** (recommended: superset below) and a mapping for each legacy program. Flag this inconsistency in the migration output.

**`TRNREC` transaction record:**

| Field | COBOL PIC | Meaning |
|---|---|---|
| `TRN-DATE` | `PIC X(08)` | Transaction date (YYYYMMDD) |
| `TRN-TIME` | `PIC X(06)` | Transaction time (HHMMSS) |
| `TRN-PORTFOLIO-ID` | `PIC X(08)` | Target portfolio |
| `TRN-SEQUENCE-NO` | `PIC X(06)` | Sequence number |
| `TRN-INVESTMENT-ID` | `PIC X(10)` | Investment identifier |
| `TRN-TYPE` | `PIC X(02)` | `BU`=Buy, `SL`=Sell, `TR`=Transfer, `FE`=Fee |
| `TRN-QUANTITY` | `PIC S9(11)V9(4) COMP-3` | Quantity / units |
| `TRN-PRICE` | `PIC S9(11)V9(4) COMP-3` | Unit price |
| `TRN-AMOUNT` | `PIC S9(13)V9(2) COMP-3` | Transaction amount |
| `TRN-CURRENCY` | `PIC X(03)` | Currency code |
| `TRN-STATUS` | `PIC X(01)` | `P`ending / `D`one / `F`ailed / `R`eversed |

**`AUDITLOG` audit record:** timestamp (`X(26)`), system/user/program/terminal IDs, type (`TRAN`/`USER`/`SYST`), action (`CREATE`/`UPDATE`/`DELETE`/`INQUIRE`/…), status (`SUCC`/`FAIL`/`WARN`), key info (portfolio ID + account), and **before/after images** (`X(100)` each) plus a free-text message.

### 5.2 Target DynamoDB design

**Recommended canonical `Portfolio` table (single-table-friendly):**

| Attribute | Type | Source | Notes |
|---|---|---|---|
| `PK` | S | `PORT#<portfolioId>` | Partition key (replaces VSAM record key). |
| `SK` | S | `METADATA` (for the master item) | Enables single-table expansion (positions, audit) later. |
| `portfolioId` | S | `PORT-ID` | `PORT` + 4 digits (see validation). |
| `accountNo` | S | `PORT-ACCOUNT-NO` | From the batch layout / composite key. |
| `clientName` / `name` | S | `PORT-CLIENT-NAME` / `PORT-NAME` | Reconcile the two name fields. |
| `clientType` | S | `PORT-CLIENT-TYPE` | `I` / `C` / `T`. |
| `status` | S | `PORT-STATUS` | Canonical domain — see note below. |
| `createDate` | S | `PORT-CREATE-DATE` | ISO-8601 `YYYY-MM-DD`. |
| `lastMaintDate` | S | `PORT-LAST-MAINT` | ISO-8601. |
| `totalValue` | N (string-encoded decimal) | `PORT-TOTAL-VALUE` | **Use a fixed-point decimal representation** — store as a string or scaled integer; **never** a JS `number`/float. |
| `cashBalance` | N | `PORT-CASH-BALANCE` | Same decimal handling. |
| `totalUnits` / `totalCost` | N | position fields used by `PORTTRAN` | Carried for transaction processing. |
| `lastUser` | S | `PORT-LAST-USER` | |
| `version` | N | new | Optimistic-concurrency token (`ConditionExpression`). |

> **Decimal precision is a hard requirement.** `COMP-3 S9(13)V99` is packed fixed-point. Do **not** map monetary fields to IEEE floating point. In TypeScript use a decimal library (e.g. `decimal.js`/`big.js`) and store as strings; in Python use `decimal.Decimal` (DynamoDB's `boto3` supports it natively). Preserve rounding behavior.

> **Status domain reconciliation.** Online (`A/I/C`) vs batch (`A/C/S`) differ. Choose a canonical enum (recommended: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `CLOSED`) and map legacy single-char codes both directions; document the mapping.

**Audit:** either a separate `PortfolioAudit` table (`PK = PORT#<id>`, `SK = AUDIT#<ISO-timestamp>#<ulid>`) populated from DynamoDB Streams, or structured CloudWatch log entries — provide both options and default to the audit table for queryability. Each entry stores action, reason/action code, actor, before/after images, and status.

### 5.3 Enumerations & codes (must be preserved)

- **Transaction types:** `BU` (buy), `SL` (sell), `TR` (transfer), `FE` (fee).
- **Deletion reason codes:** `01` = Closed, `02` = Transferred, `03` = Requested.
- **Update action types:** `S` = Status, `V` = Value, `N` = Name.
- **Validation request types (`PORTVALD`):** `I` = ID, `A` = Account, `T` = Type, `M` = Amount.
- **Investment types (`PORTVALD`):** `STK`, `BND`, `MMF`, `ETF`.
- **Portfolio status (online):** `A` Active, `I` Inactive, `C` Closed. **(batch):** `A` Active, `C` Closed, `S` Suspended.
- **Client type:** `I` Individual, `C` Corporate, `T` Trust.
- **Transaction status:** `P` Pending, `D` Done, `F` Failed, `R` Reversed.

---

## 6. Validation Rules (port from `PORTVALD` / `PORTVAL`)

Implement these in the shared validation module. Each returns a success/failure code and a message (mirror the legacy contract).

1. **Portfolio ID** (`I`): must begin with the literal prefix `PORT` **and** the following **4 characters must be numeric** (e.g. `PORT0001`). Legacy: `VAL-ID-PREFIX = 'PORT'`, positions 5–8 numeric. (Note `PORTMSTR`'s inline check uses `PORT` + position 5 numeric on an `X(10)` id — reconcile to `PORT` + 4 digits.)
2. **Account number** (`A`): must be **all numeric** and **not all zeros** (legacy expects 10 numeric digits).
3. **Investment type** (`T`): must be one of `STK`, `BND`, `MMF`, `ETF`.
4. **Amount** (`M`): must fall within `VAL-MIN-AMOUNT` (`-9999999999999.99`) and `VAL-MAX-AMOUNT` (`+9999999999999.99`).
5. **Portfolio create/update (`PORTMSTR`):** ID format valid, **name required** (non-blank), **status in the valid domain**.
6. **Transaction validation (`PORTTRAN`):** portfolio ID required **and must exist**; transaction type ∈ {`BU`,`SL`,`TR`,`FE`}; `quantity > 0`; for non-transfer (`TR`) transactions `price > 0` and `amount > 0`.
7. **Add validation (`PORTADD`):** ID and client name non-blank; status must be `A` to be added; duplicate key (`22`) is counted, not fatal.
8. **Sell guard (`PORTTRAN`):** cannot sell more units than held (`Insufficient units for sale`).

### Transaction semantics (`PORTTRAN`)
- **`BU` Buy:** `totalUnits += quantity`, `totalCost += amount`.
- **`SL` Sell:** guard sufficient units, then `totalUnits -= quantity`, `totalCost -= amount`.
- **`FE` Fee:** `totalCost -= amount`.
- **`TR` Transfer:** legacy program returns "Transfer processing not implemented." The migration should **implement transfer properly** (move position/value between two portfolios atomically) and explicitly note that this fills a gap in the legacy code.
- **Error threshold:** processing aborts once the error count exceeds **100**. In the serverless design, model this with a DLQ + CloudWatch alarm (and/or a Step Functions failure branch) rather than an in-loop counter.

---

## 7. API Design

Provide an **OpenAPI 3.x** contract. Suggested REST surface (replacing `PORTMSTR`/`PORTREAD`/`PORTUPDT`):

| Method & path | Maps to | Behavior |
|---|---|---|
| `POST /portfolios` | `PORTMSTR` `C` | Create; validate; `409` on duplicate ID. |
| `GET /portfolios/{id}` | `PORTMSTR` `R` / `PORTREAD` keyed | `GetItem`; `404` if not found. |
| `GET /portfolios` | `PORTREAD` browse | `Query`/paginated list; supports `limit` + pagination token (replaces `READ NEXT`). |
| `PUT /portfolios/{id}` | `PORTMSTR` `U` | Full replace; validate; `404` if missing. |
| `PATCH /portfolios/{id}` | `PORTUPDT` | Partial field update by action type (`S`/`V`/`N`) → `UpdateItem`. |
| `DELETE /portfolios/{id}` | `PORTMSTR` `D` / `PORTDEL` | Delete with required `reasonCode` (`01`/`02`/`03`); writes audit entry. |
| `POST /portfolios/{id}/transactions` | `PORTTRAN` | Enqueue a transaction (`BU`/`SL`/`TR`/`FE`) to SQS; async apply. |
| `POST /batch/{kind}` (or S3 upload) | `PORTADD`/`PORTUPDT`/`PORTDEL` | Trigger batch ingest (kind ∈ add/update/delete). Prefer S3 ObjectCreated trigger. |

**Conventions:** JSON request/response; validation errors → `400` with a structured error envelope `{ code, message, field }`; map legacy return code `0`→`2xx`, `8`/validation→`4xx`, infrastructure/VSAM-equivalent failures→`5xx`. Use request validation at the API Gateway layer where possible.

---

## 8. Non-Functional Requirements

### 8.1 Error handling
- Centralized error utility (modeled on `ERRPROC`): consistent categories/severities, structured JSON logs, and a uniform API error envelope.
- Idempotent transaction processing (use `TRN-SEQUENCE-NO` / a dedup key) so SQS at-least-once delivery is safe.
- Optimistic concurrency on DynamoDB writes via `ConditionExpression` + `version` attribute (replaces VSAM's record-level I-O semantics).
- Dead-letter queues for async paths; retry with backoff; preserve the **>100 error threshold** as an alarm/circuit-breaker.

### 8.2 Audit
- Every mutating operation (create/update/delete/transaction) **must** produce an immutable audit entry via DynamoDB Streams → Audit Lambda.
- Capture before/after images, actor, action, reason/action code, timestamp (ISO-8601), and success/failure — mirroring `AUDITLOG`.
- Audit store is append-only; protect with least-privilege (no update/delete permissions for app roles).

### 8.3 Security / IAM
- One IAM role per Lambda, scoped to the **exact** table/queue/bucket ARNs and the minimal actions used (e.g. read Lambdas get `GetItem`/`Query` only).
- Encrypt at rest (DynamoDB + S3 SSE/KMS) and in transit (HTTPS only). No secrets in code — use SSM Parameter Store / Secrets Manager.
- API authorization (IAM/Cognito/API key) — pick one and document it.

### 8.4 Observability
- **AWS X-Ray** active tracing on all Lambdas and API Gateway.
- Structured JSON logging to CloudWatch with correlation IDs.
- CloudWatch **metrics + alarms**: error rate, DLQ depth, transaction error-threshold breach, throttles, latency.
- Dashboards summarizing throughput (replacing `PORTTRAN`/`PORTADD`/`PORTDEL` DISPLAY counters: read/processed/error/dup/deleted counts).

### 8.5 Testing
- **Unit tests** for the validation module and each handler (cover every business rule in §6, every enum in §5.3).
- **Integration tests** against DynamoDB Local / LocalStack (or ephemeral test stacks).
- **Equivalence tests**: feed the same inputs through expected legacy outputs (decimal precision, status/type domains, reason/action codes, sell guard, duplicate handling, >100 error threshold).
- Seed data via the `PORTTEST` equivalent (up to N synthetic items).

### 8.6 Performance & cost
- On-demand (pay-per-request) DynamoDB by default; provisioned + autoscaling if predictable.
- Right-size Lambda memory; keep cold starts low; batch DynamoDB writes for ingestion.

---

## 9. Project Structure (suggested)

```
portfolio-serverless/
├── infra/                      # CDK (or SAM) IaC
│   ├── bin/app.ts
│   └── lib/
│       ├── data-stack.ts       # DynamoDB tables + streams
│       ├── api-stack.ts        # API Gateway + CRUD/query Lambdas
│       ├── batch-stack.ts      # S3 buckets + batch Lambdas
│       ├── tran-stack.ts       # SQS + worker Lambda (+ Step Functions)
│       └── audit-stack.ts      # Stream consumer + audit store
├── src/
│   ├── handlers/               # one folder per Lambda
│   │   ├── portfolio-create/   ├── portfolio-read/   ├── portfolio-list/
│   │   ├── portfolio-update/   ├── portfolio-delete/ ├── transaction-worker/
│   │   ├── batch-add/          ├── batch-update/      ├── batch-delete/
│   │   ├── audit-stream/       └── seed/
│   ├── lib/
│   │   ├── validation/         # PORTVALD port (shared layer/module)
│   │   ├── repository/         # DynamoDB access (GetItem/Query/Put/Update/Delete)
│   │   ├── decimal/            # fixed-point money helpers
│   │   ├── errors/             # ERRPROC-equivalent error utility
│   │   └── audit/              # audit-entry builder
│   └── types/                  # canonical Portfolio / Transaction / Audit models
├── openapi/portfolio.yaml      # REST API contract
├── tests/                      # unit + integration + equivalence
├── pipeline/                   # CI/CD (CodePipeline or GitHub Actions)
└── README.md
```

---

## 10. Deployment Pipeline

- IaC deploy via `cdk deploy` (or `sam deploy`) per environment (`dev`/`staging`/`prod`).
- CI/CD (GitHub Actions or AWS CodePipeline): lint → unit tests → build → `cdk synth`/`diff` → integration tests against an ephemeral stack → deploy → smoke tests.
- Environment isolation via separate stacks/accounts; parameterized config (no hardcoded ARNs).
- Rollback strategy (CloudFormation change sets) and post-deploy smoke tests on the live API.

---

## 11. Deliverables (definition of done)

1. **IaC stacks** (CDK or SAM/CloudFormation) provisioning DynamoDB (+ Streams), API Gateway, all Lambdas, SQS (+ DLQ), S3 ingest buckets, audit store, IAM least-privilege roles, X-Ray, and CloudWatch alarms.
2. **Lambda handlers** for create/read/list/update/delete, transaction worker, batch add/update/delete, audit-stream consumer, and seed generator — in **one** language (TypeScript **or** Python).
3. **Shared validation module/layer** porting all `PORTVALD`/`PORTVAL` rules (§6) with the same code/message contract.
4. **Canonical data model** reconciling the `PORTMSTR` and `PORTFLIO` layouts, with documented field/status mappings and fixed-point decimal handling.
5. **OpenAPI 3.x contract** (`openapi/portfolio.yaml`) covering §7.
6. **Audit subsystem** via DynamoDB Streams with before/after images and reason/action codes.
7. **Tests**: unit + integration + equivalence tests covering every business rule and enumeration; seed/test-data generator (≤ configurable N, default 100).
8. **CI/CD pipeline** definition (§10).
9. **README** with architecture diagram, local-dev instructions, deploy steps, and a **legacy→AWS traceability matrix** (each COBOL program → AWS components → tests).
10. **Migration notes** documenting: the record-layout divergence, the status-domain reconciliation, the unimplemented legacy `TR` transfer (now implemented), and any business rule that could not be preserved 1:1 with the proposed equivalent.

---

### Appendix A — Legacy → AWS traceability (summary)

| COBOL | AWS | API / trigger | Audit |
|---|---|---|---|
| PORTMSTR | CRUD Lambda(s) + DynamoDB | API GW REST `/portfolios` | via Streams |
| PORTTRAN | SQS + worker Lambda (+ Step Functions) | `POST /portfolios/{id}/transactions` | via Streams + AUDPROC port |
| PORTADD | Batch Lambda | S3 ObjectCreated | counts → metrics |
| PORTDEL | Delete Lambda + Streams | `DELETE` / S3 | dedicated audit entry w/ reason code |
| PORTREAD | Query Lambda | `GET /portfolios`, `GET /portfolios/{id}` | n/a (inquiry) |
| PORTUPDT | Update Lambda | `PATCH /portfolios/{id}` | via Streams |
| PORTVALD | Shared validation module/layer | imported by all | n/a |
| PORTTEST | Seed Lambda/script | manual/dev | n/a |
