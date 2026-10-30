import { useState } from 'react'
import { DEFAULT_RULES, validateRules } from '../domain/scoring'
import type { ScoringRules } from '../domain/types'

interface Props {
  rules: ScoringRules
  onChange: (rules: ScoringRules) => void
}

type Field = { label: string; get: (r: ScoringRules) => number; set: (r: ScoringRules, v: number) => ScoringRules }

const FIELDS: Field[] = [
  { label: 'Segment A weight', get: (r) => r.segmentWeights.A, set: (r, v) => ({ ...r, segmentWeights: { ...r.segmentWeights, A: v } }) },
  { label: 'Segment B weight', get: (r) => r.segmentWeights.B, set: (r, v) => ({ ...r, segmentWeights: { ...r.segmentWeights, B: v } }) },
  { label: 'Segment C weight', get: (r) => r.segmentWeights.C, set: (r, v) => ({ ...r, segmentWeights: { ...r.segmentWeights, C: v } }) },
  { label: 'Points per day since last visit', get: (r) => r.pointsPerDay, set: (r, v) => ({ ...r, pointsPerDay: v }) },
  { label: 'Days points cap', get: (r) => r.daysCap, set: (r, v) => ({ ...r, daysCap: v }) },
  { label: 'Open follow-up points', get: (r) => r.openFollowUpPoints, set: (r, v) => ({ ...r, openFollowUpPoints: v }) },
  { label: 'Recently visited window (days, score = 0)', get: (r) => r.recentVisitDays, set: (r, v) => ({ ...r, recentVisitDays: v }) },
]

export function RulesPanel({ rules, onChange }: Props) {
  const [draft, setDraft] = useState(rules)
  const errors = validateRules(draft)

  return (
    <details className="panel rules-panel">
      <summary>Scoring rules</summary>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (errors.length === 0) onChange(draft)
        }}
      >
        <div className="rules-grid">
          {FIELDS.map((f) => (
            <label key={f.label}>
              {f.label}
              <input
                type="number"
                min={0}
                value={Number.isNaN(f.get(draft)) ? '' : f.get(draft)}
                onChange={(e) => setDraft(f.set(draft, e.target.valueAsNumber))}
              />
            </label>
          ))}
        </div>
        {errors.length > 0 && (
          <ul className="message message-error" role="alert">
            {errors.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        )}
        <div className="rules-actions">
          <button type="submit" disabled={errors.length > 0}>
            Apply rules
          </button>
          <button
            type="button"
            className="secondary"
            onClick={() => {
              setDraft(DEFAULT_RULES)
              onChange(DEFAULT_RULES)
            }}
          >
            Reset to defaults
          </button>
        </div>
      </form>
    </details>
  )
}
