import { useState } from 'react'
import { DAYS_BUCKETS, type DaysBucket, type HcpFilters } from '../domain/filters'
import { WEEKDAYS, type ScoredHcp, type Segment, type Weekday } from '../domain/types'

interface Props {
  hcps: ScoredHcp[]
  totalInTerritory: number
  specialties: readonly string[]
  filters: HcpFilters
  onFiltersChange: (f: HcpFilters) => void
  plannedDays: Map<string, Weekday[]>
  onAdd: (hcp: ScoredHcp, day: Weekday) => void
}

function scoreClass(score: number): string {
  if (score >= 70) return 'score score-high'
  if (score >= 40) return 'score score-mid'
  return 'score score-low'
}

function AddControl({ hcp, onAdd }: { hcp: ScoredHcp; onAdd: Props['onAdd'] }) {
  const [day, setDay] = useState<Weekday>('Mon')
  return (
    <div className="add-control">
      <label className="visually-hidden" htmlFor={`day-${hcp.id}`}>
        Day for {hcp.name}
      </label>
      <select id={`day-${hcp.id}`} value={day} onChange={(e) => setDay(e.target.value as Weekday)}>
        {WEEKDAYS.map((d) => (
          <option key={d} value={d}>
            {d}
          </option>
        ))}
      </select>
      <button type="button" onClick={() => onAdd(hcp, day)} aria-label={`Add ${hcp.name} to ${day}`}>
        Add
      </button>
    </div>
  )
}

export function HcpList({ hcps, totalInTerritory, specialties, filters, onFiltersChange, plannedDays, onAdd }: Props) {
  const set = <K extends keyof HcpFilters>(key: K, value: HcpFilters[K]) => onFiltersChange({ ...filters, [key]: value })

  return (
    <section aria-labelledby="hcp-list-heading" className="panel hcp-panel">
      <h2 id="hcp-list-heading">Territory HCPs</h2>
      <div className="filters" role="search">
        <label>
          Search by name
          <input
            type="search"
            value={filters.search}
            onChange={(e) => set('search', e.target.value)}
            placeholder="e.g. Quinn"
          />
        </label>
        <label>
          Specialty
          <select value={filters.specialty} onChange={(e) => set('specialty', e.target.value)}>
            <option value="all">All specialties</option>
            {specialties.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label>
          Segment
          <select value={filters.segment} onChange={(e) => set('segment', e.target.value as Segment | 'all')}>
            <option value="all">All segments</option>
            <option value="A">A</option>
            <option value="B">B</option>
            <option value="C">C</option>
          </select>
        </label>
        <label>
          Days since last visit
          <select value={filters.days} onChange={(e) => set('days', e.target.value as DaysBucket)}>
            {DAYS_BUCKETS.map((b) => (
              <option key={b.value} value={b.value}>
                {b.label}
              </option>
            ))}
          </select>
        </label>
      </div>
      <p className="result-count" aria-live="polite" data-testid="hcp-count">
        Showing {hcps.length} of {totalInTerritory} HCPs in territory
      </p>
      <div className="table-wrap">
        <table>
          <caption className="visually-hidden">HCPs in territory, sorted by priority score</caption>
          <thead>
            <tr>
              <th scope="col">HCP</th>
              <th scope="col">Specialty</th>
              <th scope="col">Seg.</th>
              <th scope="col">Days since visit</th>
              <th scope="col">Score</th>
              <th scope="col">Reason</th>
              <th scope="col">Plan</th>
            </tr>
          </thead>
          <tbody>
            {hcps.map((h) => {
              const days = plannedDays.get(h.id)
              return (
                <tr key={h.id} data-testid="hcp-row" data-hcp-id={h.id}>
                  <th scope="row">
                    <span className="hcp-name">{h.name}</span>
                    <span className="hcp-meta">{h.city}</span>
                    {days && <span className="planned-tag">Planned: {days.join(', ')}</span>}
                  </th>
                  <td>{h.specialty}</td>
                  <td>
                    <span className={`seg seg-${h.segment}`}>{h.segment}</span>
                  </td>
                  <td>{h.daysSinceLastVisit ?? '—'}</td>
                  <td>
                    <span className={scoreClass(h.score)}>{h.score}</span>
                  </td>
                  <td className="reason">{h.reason}</td>
                  <td>
                    <AddControl hcp={h} onAdd={onAdd} />
                  </td>
                </tr>
              )
            })}
            {hcps.length === 0 && (
              <tr>
                <td colSpan={7} className="empty">
                  No HCPs match these filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}
