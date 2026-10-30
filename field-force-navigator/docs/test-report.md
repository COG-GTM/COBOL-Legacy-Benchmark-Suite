# Test report — Field Force Navigator (MBA-3670)

| | |
|---|---|
| Jira | [MBA-3670](https://cog-gtm.atlassian.net/browse/MBA-3670) / Verify sub-task [MBA-3677](https://cog-gtm.atlassian.net/browse/MBA-3677) |
| Status | Local browser AC1–AC6 passed; minor visual observation below; non-prod deployment not tested |
| Build tested | `bc91504fcbc0b9c597de0ba96e9cf080ec1a4dba` |
| QA approval | Awaiting approval |

## Automated tests

| Check | Result |
|---|---|
| `npm run lint` (0 warnings) | Pass |
| `npm run typecheck` | Pass |
| `npm run test:coverage` | 63/63 tests pass (after post-test review fixes) |
| Coverage, `src/domain/scoring.ts` | 100% statements / branches / functions / lines (gate: 90%) |
| Coverage, all files | 99.25% statements, 95.18% branches |

## Browser E2E

Executed in desktop Chrome against the local Vite app at `http://localhost:5173/`.
The app's synthetic-data clock was 2026-10-30 (week of 2026-10-26).
Cleared all `ffn.*` localStorage keys before starting. No authentication or backend
is part of this client-only app. The automated results above are preserved from
the existing report, not claimed as rerun during this browser test.

| AC | Steps | Expected | Observed result |
|---|---|---|---|
| AC1 | Open Ada's list; search Reese; combine specialty, segment A/B/C and days filters. Switch to Cleo and Esme. | Only selected territory's HCPs; filters/search narrow correctly. | **Pass.** Ada shows 100/100; Reese search gives 1. Endocrinology, B/C and 0–7 days exclude Reese; Cardiology/A/31–60 restore him. Nonmatching search gives 0; case-insensitive search restores him. Each territory renders 100 HCPs; supplemental DOM checks found zero out-of-territory IDs among all 300 rows. Cleo's search for Reese returns 0; Esme's full list excludes him. |
| AC2 | Ada: search Dr. Reese Willowmere under default rules. | A, 45 days, one follow-up, score 100; reason lists all factors. | **Pass.** Cardiology, A, 45, 100; reason: `Segment A, 45 days since last visit, 1 open follow-up`. |
| AC3 | Search Dr. Indra Nettlefield. | Visit three days ago scores 0. | **Pass.** B, 3 days, 0; reason includes `within 7 days, not due`. |
| AC4 | Fill Monday with six distinct HCPs; try Dr. Gray Umberfield as seventh. | Block with a message; count stays six. | **Pass.** `Mon already has 6 visits (the daily maximum). Remove a visit or choose another day.` Monday remains 6/6 and total remains 6. |
| AC5 | Observe Reese overdue warning, add him Monday, also add Tuesday, then remove Tuesday. | Named overdue warning clears after planning; cross-day duplicate warning appears and then clears. | **Pass.** Warning initially names Reese at 45 days; clears once planned. Adding Tuesday shows `Duplicate: Dr. Reese Willowmere is planned on Mon, Tue.` Removing Tuesday clears duplicate warning. |
| AC6 | Click Export CSV with six Monday visits; inspect actual downloaded file. | Download with Day, HCP, Specialty, Score, Reason matching plan. | **Pass.** Chrome downloads `call-plan-R1-2026-10-30.csv` (611 bytes). CSV parser comparison confirms exact header and all six rows, including comma-containing quoted reasons. |

### Export contents verified

All rows are Monday; reasons were compared in full against the displayed HCP table.

| HCP | Specialty | Score | Reason |
|---|---|---|---|
| Dr. Reese Willowmere | Cardiology | 100 | Segment A, 45 days since last visit, 1 open follow-up |
| Dr. Noor Elsworthy | Neurology | 85 | Segment B, 42 days since last visit, 1 open follow-up |
| Dr. Sage Ambervale | Cardiology | 82 | Segment A, 22 days since last visit, 2 open follow-ups |
| Dr. Oakley Orchardine | Rheumatology | 80 | Segment A, 44 days since last visit, no open follow-ups |
| Dr. Sage Emberlyn | Neurology | 80 | Segment A, 41 days since last visit, no open follow-ups |
| Dr. Blair Kestrelby | Primary Care | 78 | Segment A, 38 days since last visit, no open follow-ups |

## Additional checks

| Check | Result |
|---|---|
| Keyboard-only visit add | **Pass.** Tab navigation from the search field through filters to row day/Add; Enter adds Reese Monday. |
| Scoring edits and persistence | **Pass.** Segment A weight 40→10 and Apply changes Reese 100→70 in both list and plan. Reload retains weight 10 and score 70. Reset to defaults restores weight 40 and score 100. |
| Plan persistence and isolation | **Pass.** Reload retains Ada's six Monday visits. Cleo starts empty; adding Dr. Lane Driftwell creates her own one-visit plan without changing Ada's six. Both survive the later reload. |
| Manager review | **Pass.** All six reps visible. Ada shows six visits, average 84 and correct six Monday names; Cleo shows one visit, average 85 and Dr. Lane Driftwell Monday; other reps show zero. |
| Same-day duplicate prevention | **Pass.** Re-adding Reese Monday is blocked with `Dr. Reese Willowmere is already planned on Mon.` Count remains six. |
| List responsiveness, seed size 300 | **Pass for local desktop observations.** Each territory displays 100 of the total 300 synthetic HCPs. Trusted UI-input to second-animation-frame measurements were 4.2–30.6 ms for Cleo's name search; rep switches to Cleo and Esme were 49.0 and 59.6 ms. Interactions felt immediate. These are local next-paint proxies, not production load testing. |

### Visual observation

**Sev 3 (minor cosmetic/UX):** the five weekly-plan columns are narrow in the
two-column desktop layout. At a 1422×934 CSS-pixel viewport, long HCP names and
specialty text reach/clip at a card's right edge. For example, Reese's name span
has a 67-pixel client width and 71-pixel scroll width. The screenshot also shows
the long Rheumatology specialty clipped. HCP details remain available in the
list and manager view; add/remove/export remain usable. No functional AC failure
was observed, but layout polish is recommended.

### Post-test fixes

After this browser run, the following were fixed on the PR branch and covered by unit tests (not re-run in the browser):

- Long weekly-plan names/specialties now wrap (`overflow-wrap: anywhere`) instead of clipping (the Sev 3 issue above).
- A mounted app that crosses a week boundary loads the new week's plans instead of saving the old week's plans under the new week's key.
- The ISO week start uses UTC getters, so a Monday stays in its own week in time zones west of UTC.
- The total score cap must be 1–100, and scores are always clamped to 100, even with edited weights.

## Not covered

- **The non-prod deployment cannot be exercised before merge.** Local Vite
  testing does not verify deployment, CI publishing or hosted routing.
- Other browsers, mobile/narrow-screen layouts, screen-reader behavior and
  exhaustive combinations of all scoring-rule fields/day buckets were not tested.
- Production data, backend authorization and cross-device persistence do not
  exist in this synthetic client-only scope and were not tested.
- Local responsiveness measurements are not a production performance benchmark.

## Evidence

One annotated primary-flow recording and key screenshots accompany the QA
handoff/PR comment: default pinned scores, overdue/duplicate warning behavior,
daily-cap error, completed browser download, rule edit/reload/reset and manager
review. No implementation changes or commit were made by this test run.
