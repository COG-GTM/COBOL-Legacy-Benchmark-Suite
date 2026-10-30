# Field Force Navigator: Territory Call Planner — Design

| | |
|---|---|
| Jira | [MBA-3670](https://cog-gtm.atlassian.net/browse/MBA-3670) |
| Phase | Design |
| Approver | Tech Lead |
| Status | Awaiting approval |
| Requirements | [requirements.md](requirements.md) |

## 1. Overview

A single-page web app (React 18 + TypeScript, built with Vite) served as static files from an nginx container. All data is synthetic and generated in the browser, so there is no backend, database or CRM dependency (all out of scope). Business rules live in pure TypeScript modules under `src/domain/` with no React imports, so they are unit-testable in isolation.

```
field-force-navigator/
├── src/
│   ├── domain/            # pure logic, no UI
│   │   ├── types.ts       # Territory, Rep, Hcp, Visit, ScoringRules, WeeklyPlan
│   │   ├── seed.ts        # deterministic synthetic data generator
│   │   ├── scoring.ts     # calculateScore, scoreHcp, DEFAULT_RULES, validateRules
│   │   ├── filters.ts     # applyFilters, sortByPriority, days buckets
│   │   ├── plan.ts        # addToPlan, removeFromPlan, findDuplicates, findMissingOverdueSegmentA
│   │   ├── csv.ts         # planToCsv, escapeCsv
│   │   ├── storage.ts     # localStorage load/save for plans and rules
│   │   ├── download.ts    # browser CSV download
│   │   └── dates.ts
│   ├── components/        # HcpList, PlanBuilder, RulesPanel, ManagerReview
│   └── App.tsx            # state + wiring
├── Dockerfile, nginx.conf
└── docs/
```

## 2. Data model

```ts
Territory { id, name }
Rep { id, name, territoryId }
DistrictManager { id, name, territoryIds[] }
Hcp { id, name, specialty, segment: 'A'|'B'|'C', territoryId, city, visits: Visit[], openFollowUps: FollowUp[] }
Visit { date: 'YYYY-MM-DD', repId }
FollowUp { id, note, createdOn }
ScoredHcp = Hcp & { daysSinceLastVisit: number | null, score, reason }
WeeklyPlan = Record<'Mon'|'Tue'|'Wed'|'Thu'|'Fri', hcpId[]>
```

### Seed (`seed.ts`)
- Seeded PRNG (mulberry32, seed 1440), so the same "as of" date always yields the same data.
- Dates are generated relative to the "as of" date (today), so "days since last visit" stays realistic whenever the app runs.
- 300 HCPs round-robin across 3 territories (100 each). Segment mix ≈ 20% A / 35% B / 45% C. Visit cadence by segment (A ≈ 21 days, B ≈ 35, C ≈ 60) with jitter, back 365 days, attributed to a rep of the same territory. ~6% of Segment C HCPs have no visit in 12 months. ~7% of HCPs have 1-2 open follow-ups.
- Names are invented (made-up surnames × neutral first names, shuffled, unique).
- Four HCPs in territory T1 are pinned to make the acceptance scenarios reproducible in the UI: Segment A / 45 days / 1 follow-up (score 100); visited 3 days ago (score 0); Segment A / 38 days / no follow-up (overdue warning); Segment A / 5 days / 1 follow-up (recent, score 0).

## 3. Scoring engine (`scoring.ts`)

```
if daysSinceLastVisit != null and daysSinceLastVisit <= recentVisitDays: score = 0
else:
  dayPoints = daysSinceLastVisit == null ? daysCap : min(days * pointsPerDay, daysCap)
  score = clamp(segmentWeights[segment] + dayPoints + (openFollowUps > 0 ? openFollowUpPoints : 0), 0, maxScore)
```

Reason text always lists the three factors in a fixed order: `Segment <S>, <N days since last visit | visited today | no visit in the last 12 months>, <N open follow-up(s) | no open follow-ups>`; zero scores append `(within 7 days, not due)` to the days factor.

Rules are a `ScoringRules` object (defaults in `DEFAULT_RULES`). The Scoring rules panel edits a draft, validates it (`validateRules`), and applies it; rules persist in localStorage. Scores are recomputed with `useMemo` when rules change.

## 4. Plan builder (`plan.ts`)

- `addToPlan(plan, day, hcpId, name)` returns `{ ok: true, plan }` or `{ ok: false, error }`. It blocks: same HCP on the same day; a 7th visit (`MAX_VISITS_PER_DAY = 6`). Inputs are never mutated.
- `findDuplicates(plan)` returns HCPs planned on more than one day.
- `findMissingOverdueSegmentA(plan, territoryHcps)` returns Segment A HCPs with `daysSinceLastVisit > 30` (or no visit) not in the plan, most overdue first (`OVERDUE_SEGMENT_A_DAYS = 30`).
- Plans are kept per rep in a `Map<repId, WeeklyPlan>` and saved to localStorage per rep and ISO week (`ffn.plan.<repId>.<weekStartMonday>`), so a new week starts with an empty plan and past weeks stay available.

## 5. CSV export (`csv.ts`)

Header `Day,HCP,Specialty,Score,Reason`; one row per planned visit Mon→Fri in slot order; RFC 4180 quoting (fields with `,` `"` or newlines are quoted, quotes doubled); CRLF line endings. Downloaded as `call-plan-<repId>-<date>.csv` via a Blob URL.

## 6. UI

- Header: role toggle (Field rep / District manager) and rep selector.
- Rep view: collapsible Scoring rules panel; left, the HCP table (filters, search, count, score badge, reason, day picker + Add per row); right, the Mon-Fri grid with `n/6` counters, Remove buttons, status message (`role="status"`, `aria-live`), Plan warnings, Export CSV and Clear.
- District manager view: one row per rep with plan metrics and the plan by day.

### Accessibility
Native `<select>`, `<input>`, `<button>` and `<table>` only, so everything is reachable and operable by keyboard. Every control has a visible or visually hidden label; row Add/Remove buttons have specific `aria-label`s (e.g. "Add Dr. X to Tue"). Visible focus ring (`:focus-visible`). Messages and the result count are announced with `aria-live`.

### Performance
300 HCPs are scored in memory once per rules change (O(n)); filtering is O(n) per keystroke; the table renders ≤ 100 rows per territory. A test asserts initial render < 1 s in jsdom.

## 7. Testing strategy

| Layer | Tool | Scope |
|---|---|---|
| Unit | Vitest | `scoring`, `plan`, `filters`, `csv`, `seed` (AC2-AC6 rules, edge cases, configurable rules) |
| Component | Vitest + Testing Library (jsdom) | AC1-AC6 through the UI, persistence, rules panel, manager view, < 1 s render |
| Coverage gate | @vitest/coverage-v8 | `src/domain/scoring.ts` ≥ 90% statements/branches/functions/lines; CI fails otherwise |
| E2E | Browser run + recording | AC1-AC6 recorded for [test-report.md](test-report.md) (Verify phase) |

## 8. CI/CD

- **CI** (`.github/workflows/field-force-navigator-ci.yml`, every PR): `npm ci` → `lint` (ESLint, zero warnings) → `typecheck` → `test:coverage` (with the 90% gate) → `docker build` → container smoke test (`/healthz` and index page).
- **Deploy** (`.github/workflows/field-force-navigator-deploy.yml`, push to `main` or manual): build and push `ghcr.io/<owner>/field-force-navigator:<sha>`, then a `deploy-non-prod` job bound to the GitHub Environment **`non-prod`**. The environment must have *Required reviewers* = Release Manager, so the job waits for their approval. On approval, it posts the image tag to `FFN_NON_PROD_DEPLOY_WEBHOOK` (the non-prod runtime's deploy hook); `vars.FFN_NON_PROD_URL` is shown as the environment URL.

### Container
Multi-stage: `node:20-alpine` builds, `nginx:1.27-alpine` serves `dist/` on port 8080 with SPA fallback, `/healthz`, long cache for hashed assets, and basic security headers.

## 9. Decisions and trade-offs

| Decision | Why |
|---|---|
| Client-only, synthetic data | CRM integration and real data are out of scope; zero infrastructure for non-prod. |
| Pure domain modules | Scoring and plan rules are testable without the UI and meet the 90% gate. |
| localStorage persistence | Enough for planning and DM review on the same device; a real store comes with CRM integration later. |
| Role toggle, no auth | SSO is out of scope. |
| App lives in `field-force-navigator/` in this repo | Keeps the phase artefacts, app and path-scoped CI next to each other; the app is self-contained and does not touch the COBOL sources. |
