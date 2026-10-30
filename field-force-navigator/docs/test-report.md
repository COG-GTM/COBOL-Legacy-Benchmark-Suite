# Test report — Field Force Navigator (MBA-1440)

| | |
|---|---|
| Jira | [MBA-1440](https://cog-gtm.atlassian.net/browse/MBA-1440) / Verify sub-task MBA-3645 |
| PR | [#307](https://github.com/COG-GTM/COBOL-Legacy-Benchmark-Suite/pull/307) |
| Build tested | `8c95773` (full run), `df4569a` (delta: total score cap, per-week plan storage, deploy gated on CI) |
| Environment | Local Vite dev server in Chrome, synthetic seed (as-of 2026-10-30, week of 2026-10-26) |
| Evidence | Annotated recording and screenshots on the PR (see the "Browser E2E" comment) |
| QA approval | Awaiting approval |

## Automated tests

| Check | Result |
|---|---|
| `npm run lint -- --max-warnings 0` | Pass |
| `npm run typecheck` | Pass |
| `npm run test:coverage` | 59/59 tests pass |
| Coverage, `src/domain/scoring.ts` | 100% statements / branches / functions / lines (gate: 90%) |
| Coverage, all files | 99.5% statements, 95.1% branches |
| `docker build` + `/healthz` smoke test | Pass (local and in CI) |
| GitHub Actions on PR #307 | Lint/typecheck/test and Container build green |

## Acceptance criteria (browser, end to end)

| AC | Steps | Expected | Result |
|---|---|---|---|
| AC1 | Switch rep Ada → Cleo; apply Specialty = Cardiology, Segment = A, Days = 31–60, search "Reese", then "Sage" | List switches Northgate Metro → Riverbend Valley; each filter narrows (AND) | **Pass**: territory and list changed; 16 → 6 → 3 → 1 → 0 rows |
| AC2 | Find Dr. Reese Willowmere (Segment A, 45 days, 1 open follow-up) | Score 100; reason lists all 3 factors | **Pass**: score 100, "Segment A, 45 days since last visit, 1 open follow-up" |
| AC3 | Find Dr. Indra Nettlefield (visited 3 days ago) | Score 0 | **Pass**: score 0 |
| AC4 | Add 6 visits to Monday, then a 7th | 6th allowed; 7th blocked with a message | **Pass**: Monday stays at 6; daily-maximum message shown |
| AC5 | Leave Reese (Segment A, 45 days) off the plan; then add Reese | Warning names Reese; clears once planned | **Pass**. Duplicate check also passes: Reese on Mon + Tue shows a named duplicate warning, which clears when Tue is removed |
| AC6 | Export CSV for a 7-visit plan | CSV with Day, HCP, Specialty, Score, Reason | **Pass**: header `Day,HCP,Specialty,Score,Reason`; all 35 data cells match the UI, and comma-containing reasons are quoted |

## Additional checks

| Area | Result |
|---|---|
| Configurable rules | **Pass**: Segment A weight 40 → 0 changed Reese from 100 to 60 and re-ordered the list; the rule persists after reload; Reset restores the defaults; a negative value shows a validation error and disables Apply |
| Total score cap (`df4569a`) | **Pass**: cap 80 limits the top scores to 80; Reset restores 100 |
| Persistence | **Pass**: plans survive reload and are independent per rep (Ada 8, Cleo 1, Bram empty in the same territory as Ada) |
| Per-week storage (`df4569a`) | **Pass**: subtitle shows "week of 2026-10-26"; plan saved under `ffn.plan.R1.2026-10-26` and survives reload. Week rollover is covered by the unit test `keeps plans per week so a new week starts empty` |
| District manager review | **Pass**: shows all 6 reps with territory, plan by day, visit count, average score, overdue Segment A omissions and duplicates |
| Keyboard accessibility | **Pass**: Tab / Shift+Tab, typing, arrow keys and Enter added a visit without using the mouse |
| Performance (NFR-3) | **Pass**: the unit test renders 300 HCPs well under 1 s; no visible delay in the browser |

## Not covered

- An actual change of ISO week in the browser. This is covered by the unit test only.
- The full browser suite was not re-run on `df4569a`; only the delta was re-checked there, and the unit and component suite passes on `df4569a`.
- Deploy to non-prod: this needs the merge to `main`, plus Release Manager reviewers on the `non-prod` environment and the `FFN_NON_PROD_DEPLOY_WEBHOOK` secret.
