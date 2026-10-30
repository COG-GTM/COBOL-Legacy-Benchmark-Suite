# Field Force Navigator: Territory Call Planner — Requirements

| | |
|---|---|
| Jira | [MBA-3670](https://cog-gtm.atlassian.net/browse/MBA-3670) |
| Phase | Requirements |
| Approver | Product Owner |
| Status | Awaiting approval |

## 1. Problem

Field reps plan their weekly visits to healthcare professionals (HCPs) by hand from spreadsheets. They miss overdue follow-ups and spend planning time that could go to visits. District managers cannot see whether plans cover the right HCPs.

## 2. Goal

A rep can build a prioritised weekly call plan for their territory in **under 5 minutes**.

## 3. Users

| Persona | Needs |
|---|---|
| Field rep | See their territory's HCPs ranked by priority, build a Mon-Fri plan, export it. |
| District manager | Review each rep's plan: volume, average priority, overdue Segment A HCPs left off, duplicates. |

## 4. Functional requirements

### FR-1 Territory HCP list
- FR-1.1 The user selects a rep; the list shows only HCPs in that rep's territory.
- FR-1.2 Filters: specialty, segment (A/B/C), days since last visit (0-7, 8-30, 31-60, 61+, no visit in 12 months).
- FR-1.3 Case-insensitive search by HCP name (substring).
- FR-1.4 Filters and search combine (logical AND). A count shows "Showing X of Y".
- FR-1.5 The list is sorted by priority score (highest first), then name.

### FR-2 Visit priority score
- FR-2.1 Every HCP gets a score from 0 to 100 and a plain-language reason listing segment, days since last visit and open follow-ups, e.g. `Segment A, 45 days since last visit, 1 open follow-up`.
- FR-2.2 Default rules (configurable in the "Scoring rules" panel, saved in the browser):

  | Rule | Default |
  |---|---|
  | Segment weight | A = 40, B = 25, C = 10 |
  | Days since last visit | +1 per day, max 40 |
  | Open follow-up | +20 |
  | Visited in the last 7 days | score = 0 |
  | Total cap | 100 |

- FR-2.3 Invalid rule values (negative, empty) are rejected with a message; "Reset to defaults" restores the table above.

### FR-3 Weekly plan builder
- FR-3.1 The rep adds an HCP to a Mon-Fri day from the list.
- FR-3.2 At most 6 visits a day. Adding a 7th is blocked with a message.
- FR-3.3 Adding the same HCP twice on the same day is blocked; planning the same HCP on two different days is allowed but raises a duplicate warning.
- FR-3.4 The plan shows a warning naming every Segment A HCP more than 30 days since last visit (or with no visit in 12 months) who is not in the plan.
- FR-3.5 The rep can remove a visit or clear the plan. The plan is saved in the browser per rep and per week (Monday start); a new week starts empty.
- FR-3.6 Export CSV with columns `Day, HCP, Specialty, Score, Reason`, ordered Mon-Fri then slot order.

### FR-4 District manager review (supports the DM persona)
- FR-4.1 A "District manager" view lists each rep with territory, visits planned, average score, number of overdue Segment A HCPs not planned (with an example name), duplicates, and the plan by day.

## 5. Acceptance criteria (from MBA-3670)

| ID | Given / When / Then | Covered by |
|---|---|---|
| AC1 | Given a rep is selected, when the HCP list opens, then only HCPs in that rep's territory appear, and the filters and search narrow the list correctly. | FR-1 |
| AC2 | Given an HCP is Segment A, 45 days since last visit, with 1 open follow-up, when its score is calculated, then the score is 100 (40 + 40 cap + 20, capped at 100) and the reason text lists all 3 factors. | FR-2 |
| AC3 | Given an HCP was visited 3 days ago, when its score is calculated, then the score is 0. | FR-2 |
| AC4 | Given a day already has 6 visits, when the rep adds a 7th, then the add is blocked with a message. | FR-3.2 |
| AC5 | Given a Segment A HCP is more than 30 days overdue and not in the plan, when the rep views the plan, then a warning names that HCP. | FR-3.4 |
| AC6 | Given a completed plan, when the rep exports it, then a CSV downloads with day, HCP, specialty, score and reason. | FR-3.6 |

## 6. Non-functional requirements

| ID | Requirement | How it is met |
|---|---|---|
| NFR-1 | Scoring logic ≥ 90% unit test coverage | Vitest coverage threshold on `src/domain/scoring.ts`; CI fails below 90%. |
| NFR-2 | CI runs lint, tests and a container build on every PR | `.github/workflows/field-force-navigator-ci.yml` |
| NFR-3 | HCP list loads in < 1 s for 300 HCPs | All data in memory; scoring is O(n); timing test in `App.test.tsx`. |
| NFR-4 | Basic keyboard accessibility | Native controls only, labelled inputs, visible focus ring, `aria-live` messages, semantic tables. |

## 7. Data

Synthetic, deterministic seed generated in the browser (`src/domain/seed.ts`): 3 territories, 6 reps (2 per territory), 1 district manager, 300 HCPs (100 per territory), 12 months of visit history, a few open follow-ups. All names are invented. No real HCP data or PII.

## 8. Out of scope

Integration with a real CRM (such as Veeva), real HCP data or PII, SSO, mobile app, route optimisation, GenAI features.

## 9. Assumptions (for Product Owner confirmation)

1. "Days since last visit" counts calendar days from the most recent visit to today.
2. "Visited in the last 7 days" includes day 7 (days 0-7 score 0; day 8 is due).
3. Open follow-up is a flat +20 if the HCP has one or more open follow-ups (not +20 each).
4. An HCP with no visit in the last 12 months gets the full days points (40).
5. "More than 30 days overdue" for Segment A means more than 30 days since last visit (31+), or no visit in 12 months.
6. Plans are stored in the browser (localStorage) per rep and per week. There is no server persistence (no CRM integration is in scope).
7. Role selection is a toggle, not authentication (SSO is out of scope).

## 10. Jira sub-tasks

| Key | Phase | Summary |
|---|---|---|
| [MBA-3671](https://cog-gtm.atlassian.net/browse/MBA-3671) | Requirements | docs/requirements.md (Product Owner) |
| [MBA-3676](https://cog-gtm.atlassian.net/browse/MBA-3676) | Design | docs/design.md (Tech Lead) |
| [MBA-3675](https://cog-gtm.atlassian.net/browse/MBA-3675) | Build | Synthetic seed data |
| [MBA-3674](https://cog-gtm.atlassian.net/browse/MBA-3674) | Build | Territory HCP list, filters, search (AC1) |
| [MBA-3678](https://cog-gtm.atlassian.net/browse/MBA-3678) | Build | Visit priority score (AC2, AC3) |
| [MBA-3673](https://cog-gtm.atlassian.net/browse/MBA-3673) | Build | Weekly plan builder, warnings, CSV (AC4-AC6) |
| [MBA-3679](https://cog-gtm.atlassian.net/browse/MBA-3679) | Build | CI: lint, tests, container build |
| [MBA-3677](https://cog-gtm.atlassian.net/browse/MBA-3677) | Verify | docs/test-report.md + recording (QA) |
| [MBA-3672](https://cog-gtm.atlassian.net/browse/MBA-3672) | Release | Gated deploy to non-prod (Release Manager) |
