import { useEffect, useMemo, useState } from 'react'
import './App.css'
import { HcpList } from './components/HcpList'
import { ManagerReview } from './components/ManagerReview'
import { PlanBuilder } from './components/PlanBuilder'
import { RulesPanel } from './components/RulesPanel'
import { planToCsv } from './domain/csv'
import { downloadCsv } from './domain/download'
import { startOfUtcDay, toIsoDate } from './domain/dates'
import { applyFilters, EMPTY_FILTERS, sortByPriority, type HcpFilters } from './domain/filters'
import { addToPlan, emptyPlan, findDuplicates, findMissingOverdueSegmentA, removeFromPlan } from './domain/plan'
import { scoreHcp } from './domain/scoring'
import { generateSeed, SPECIALTIES } from './domain/seed'
import { loadPlan, loadRules, savePlan, saveRules } from './domain/storage'
import { WEEKDAYS, type ScoredHcp, type Weekday, type WeeklyPlan } from './domain/types'

type Role = 'rep' | 'manager'
type Message = { kind: 'error' | 'info'; text: string } | null

export default function App({ asOf = startOfUtcDay(new Date()) }: { asOf?: Date }) {
  const asOfKey = toIsoDate(asOf)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const seed = useMemo(() => generateSeed(asOf), [asOfKey])
  const [role, setRole] = useState<Role>('rep')
  const [repId, setRepId] = useState(seed.reps[0].id)
  const [rules, setRules] = useState(loadRules)
  const [filters, setFilters] = useState<HcpFilters>(EMPTY_FILTERS)
  const [plans, setPlans] = useState<Map<string, WeeklyPlan>>(
    () => new Map(seed.reps.map((r) => [r.id, loadPlan(r.id)])),
  )
  const [message, setMessage] = useState<Message>(null)

  const rep = seed.reps.find((r) => r.id === repId)!
  const territory = seed.territories.find((t) => t.id === rep.territoryId)!
  const plan = plans.get(repId) ?? emptyPlan()

  const scoredByTerritory = useMemo(() => {
    const m = new Map<string, ScoredHcp[]>()
    for (const h of seed.hcps) {
      const s = scoreHcp(h, asOf, rules)
      m.set(h.territoryId, [...(m.get(h.territoryId) ?? []), s])
    }
    for (const [k, v] of m) m.set(k, sortByPriority(v))
    return m
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seed, rules, asOfKey])

  const territoryHcps = useMemo(() => scoredByTerritory.get(rep.territoryId) ?? [], [scoredByTerritory, rep.territoryId])
  const hcpsById = useMemo(() => new Map(territoryHcps.map((h) => [h.id, h])), [territoryHcps])
  const visible = useMemo(() => applyFilters(territoryHcps, filters), [territoryHcps, filters])
  const plannedDays = useMemo(() => {
    const m = new Map<string, Weekday[]>()
    for (const d of WEEKDAYS) for (const id of plan[d]) m.set(id, [...(m.get(id) ?? []), d])
    return m
  }, [plan])
  const missingOverdue = useMemo(() => findMissingOverdueSegmentA(plan, territoryHcps), [plan, territoryHcps])
  const duplicates = useMemo(() => findDuplicates(plan), [plan])

  useEffect(() => {
    for (const [id, p] of plans) savePlan(id, p)
  }, [plans])

  const updatePlan = (next: WeeklyPlan) => setPlans((prev) => new Map(prev).set(repId, next))

  const handleAdd = (hcp: ScoredHcp, day: Weekday) => {
    const result = addToPlan(plan, day, hcp.id, hcp.name)
    if (!result.ok) {
      setMessage({ kind: 'error', text: result.error })
      return
    }
    updatePlan(result.plan)
    const otherDays = plannedDays.get(hcp.id)
    setMessage({
      kind: 'info',
      text: otherDays
        ? `Added ${hcp.name} to ${day}. Warning: also planned on ${otherDays.join(', ')}.`
        : `Added ${hcp.name} to ${day}.`,
    })
  }

  const handleExport = () => {
    downloadCsv(`call-plan-${rep.id}-${asOfKey}.csv`, planToCsv(plan, hcpsById))
    setMessage({ kind: 'info', text: 'Plan exported as CSV.' })
  }

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>Field Force Navigator</h1>
          <p className="subtitle">Territory Call Planner · synthetic data · as of {asOfKey}</p>
        </div>
        <div className="header-controls">
          <fieldset className="role-toggle">
            <legend className="visually-hidden">View as</legend>
            <label>
              <input type="radio" name="role" checked={role === 'rep'} onChange={() => setRole('rep')} /> Field rep
            </label>
            <label>
              <input type="radio" name="role" checked={role === 'manager'} onChange={() => setRole('manager')} />{' '}
              District manager
            </label>
          </fieldset>
          {role === 'rep' && (
            <label>
              Rep
              <select
                value={repId}
                onChange={(e) => {
                  setRepId(e.target.value)
                  setFilters(EMPTY_FILTERS)
                  setMessage(null)
                }}
              >
                {seed.reps.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name} — {seed.territories.find((t) => t.id === r.territoryId)?.name}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>
      </header>

      {role === 'rep' ? (
        <main>
          <p className="territory-line">
            Territory: <strong data-testid="territory-name">{territory.name}</strong>
          </p>
          <RulesPanel
            rules={rules}
            onChange={(r) => {
              setRules(r)
              saveRules(r)
            }}
          />
          <div className="layout">
            <HcpList
              hcps={visible}
              totalInTerritory={territoryHcps.length}
              specialties={SPECIALTIES}
              filters={filters}
              onFiltersChange={setFilters}
              plannedDays={plannedDays}
              onAdd={handleAdd}
            />
            <PlanBuilder
              plan={plan}
              hcpsById={hcpsById}
              message={message}
              missingOverdue={missingOverdue}
              duplicates={duplicates}
              onRemove={(day, id) => {
                updatePlan(removeFromPlan(plan, day, id))
                setMessage(null)
              }}
              onClear={() => {
                updatePlan(emptyPlan())
                setMessage(null)
              }}
              onExport={handleExport}
            />
          </div>
        </main>
      ) : (
        <main>
          <ManagerReview
            manager={seed.districtManagers[0]}
            reps={seed.reps}
            territories={seed.territories}
            scoredByTerritory={scoredByTerritory}
            plans={plans}
          />
        </main>
      )}
    </div>
  )
}
