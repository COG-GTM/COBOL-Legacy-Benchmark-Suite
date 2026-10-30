# Field Force Navigator — Territory Call Planner

Plan a prioritised Mon-Fri call plan for a pharma field rep's territory in a few minutes. Jira: [MBA-1440](https://cog-gtm.atlassian.net/browse/MBA-1440).

- **Territory HCP list** with specialty / segment / days-since-visit filters and name search.
- **Visit priority score** (0-100) with a plain-language reason; rules are configurable in the UI.
- **Weekly plan builder**: max 6 visits/day, duplicate and overdue Segment A warnings, CSV export.
- **District manager review** of every rep's plan.

All data is synthetic and generated in the browser (3 territories, 6 reps, 300 HCPs, 12 months of visits). No real names or PII.

## Run

```bash
npm ci
npm run dev            # http://localhost:5173
npm run lint
npm run typecheck
npm run test:coverage  # fails if src/domain/scoring.ts < 90% coverage
npm run build

docker build -t field-force-navigator .
docker run -p 8080:8080 field-force-navigator   # http://localhost:8080, health: /healthz
```

## Docs

- [Requirements](docs/requirements.md)
- [Design](docs/design.md)
- [Test report](docs/test-report.md)

## CI/CD

- `.github/workflows/field-force-navigator-ci.yml` — lint, typecheck, tests + coverage gate, container build and smoke test on every PR.
- `.github/workflows/field-force-navigator-deploy.yml` — on `main`: push image to GHCR, then deploy to the `non-prod` GitHub Environment after Release Manager approval (configure *Required reviewers* on that environment).
