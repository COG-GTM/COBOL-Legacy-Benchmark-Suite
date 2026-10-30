import type { DuplicateWarning } from '../domain/plan'
import { MAX_VISITS_PER_DAY, OVERDUE_SEGMENT_A_DAYS } from '../domain/plan'
import { WEEKDAYS, type ScoredHcp, type Weekday, type WeeklyPlan } from '../domain/types'

interface Props {
  plan: WeeklyPlan
  hcpsById: Map<string, ScoredHcp>
  message: { kind: 'error' | 'info'; text: string } | null
  missingOverdue: ScoredHcp[]
  duplicates: DuplicateWarning[]
  onRemove: (day: Weekday, hcpId: string) => void
  onClear: () => void
  onExport: () => void
}

export function PlanBuilder({ plan, hcpsById, message, missingOverdue, duplicates, onRemove, onClear, onExport }: Props) {
  const total = WEEKDAYS.reduce((n, d) => n + plan[d].length, 0)
  return (
    <section aria-labelledby="plan-heading" className="panel plan-panel">
      <div className="plan-header">
        <h2 id="plan-heading">Weekly plan</h2>
        <span className="plan-total">{total} visits</span>
        <button type="button" onClick={onExport} disabled={total === 0}>
          Export CSV
        </button>
        <button type="button" className="secondary" onClick={onClear} disabled={total === 0}>
          Clear
        </button>
      </div>

      <div role="status" aria-live="assertive" className={message ? `message message-${message.kind}` : 'message'}>
        {message?.text}
      </div>

      <div className="week-grid">
        {WEEKDAYS.map((day) => (
          <div key={day} className="day" data-testid={`day-${day}`}>
            <h3>
              {day}{' '}
              <span className={plan[day].length >= MAX_VISITS_PER_DAY ? 'cap cap-full' : 'cap'}>
                {plan[day].length}/{MAX_VISITS_PER_DAY}
              </span>
            </h3>
            <ol>
              {plan[day].map((id) => {
                const h = hcpsById.get(id)
                if (!h) return null
                return (
                  <li key={id}>
                    <span className="slot-name">{h.name}</span>
                    <span className="slot-meta">
                      {h.specialty} · {h.score}
                    </span>
                    <button
                      type="button"
                      className="link"
                      onClick={() => onRemove(day, id)}
                      aria-label={`Remove ${h.name} from ${day}`}
                    >
                      Remove
                    </button>
                  </li>
                )
              })}
            </ol>
          </div>
        ))}
      </div>

      <div className="warnings" aria-labelledby="warnings-heading">
        <h3 id="warnings-heading">Plan warnings</h3>
        {missingOverdue.length === 0 && duplicates.length === 0 && <p className="ok">No warnings.</p>}
        <ul>
          {missingOverdue.map((h) => (
            <li key={h.id} className="warning" data-testid="overdue-warning">
              Segment A overdue: <strong>{h.name}</strong> (
              {h.daysSinceLastVisit === null ? 'no visit in 12 months' : `${h.daysSinceLastVisit} days since last visit`},
              more than {OVERDUE_SEGMENT_A_DAYS}) is not in the plan.
            </li>
          ))}
          {duplicates.map((d) => (
            <li key={d.hcpId} className="warning" data-testid="duplicate-warning">
              Duplicate: <strong>{hcpsById.get(d.hcpId)?.name ?? d.hcpId}</strong> is planned on {d.days.join(', ')}.
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
