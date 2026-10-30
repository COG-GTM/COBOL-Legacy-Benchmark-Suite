import { findDuplicates, findMissingOverdueSegmentA, totalVisits } from '../domain/plan'
import { WEEKDAYS, type DistrictManager, type Rep, type ScoredHcp, type Territory, type WeeklyPlan } from '../domain/types'

interface Props {
  manager: DistrictManager
  reps: Rep[]
  territories: Territory[]
  scoredByTerritory: Map<string, ScoredHcp[]>
  plans: Map<string, WeeklyPlan>
}

export function ManagerReview({ manager, reps, territories, scoredByTerritory, plans }: Props) {
  const myReps = reps.filter((r) => manager.territoryIds.includes(r.territoryId))
  return (
    <section aria-labelledby="review-heading" className="panel">
      <h2 id="review-heading">Plan review — {manager.name}</h2>
      <table>
        <thead>
          <tr>
            <th scope="col">Rep</th>
            <th scope="col">Territory</th>
            <th scope="col">Visits planned</th>
            <th scope="col">Avg. score</th>
            <th scope="col">Overdue Segment A not planned</th>
            <th scope="col">Duplicates</th>
            <th scope="col">Plan</th>
          </tr>
        </thead>
        <tbody>
          {myReps.map((rep) => {
            const plan = plans.get(rep.id)!
            const hcps = scoredByTerritory.get(rep.territoryId) ?? []
            const byId = new Map(hcps.map((h) => [h.id, h]))
            const ids = WEEKDAYS.flatMap((d) => plan[d])
            const avg = ids.length ? Math.round(ids.reduce((s, id) => s + (byId.get(id)?.score ?? 0), 0) / ids.length) : 0
            const missing = findMissingOverdueSegmentA(plan, hcps)
            return (
              <tr key={rep.id} data-testid="review-row">
                <th scope="row">{rep.name}</th>
                <td>{territories.find((t) => t.id === rep.territoryId)?.name}</td>
                <td>{totalVisits(plan)}</td>
                <td>{ids.length ? avg : '—'}</td>
                <td className={missing.length ? 'warn-cell' : ''}>
                  {missing.length}
                  {missing.length > 0 && <span className="hcp-meta"> e.g. {missing[0].name}</span>}
                </td>
                <td>{findDuplicates(plan).length}</td>
                <td className="review-plan">
                  {WEEKDAYS.map((d) => (
                    <div key={d}>
                      <strong>{d}:</strong> {plan[d].map((id) => byId.get(id)?.name ?? id).join('; ') || '—'}
                    </div>
                  ))}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </section>
  )
}
