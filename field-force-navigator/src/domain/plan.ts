import { WEEKDAYS, type ScoredHcp, type Weekday, type WeeklyPlan } from './types'

export const MAX_VISITS_PER_DAY = 6
export const OVERDUE_SEGMENT_A_DAYS = 30

export function emptyPlan(): WeeklyPlan {
  return { Mon: [], Tue: [], Wed: [], Thu: [], Fri: [] }
}

export type AddResult = { ok: true; plan: WeeklyPlan } | { ok: false; plan: WeeklyPlan; error: string }

export function addToPlan(
  plan: WeeklyPlan,
  day: Weekday,
  hcpId: string,
  hcpName: string,
  maxPerDay = MAX_VISITS_PER_DAY,
): AddResult {
  const slots = plan[day]
  if (slots.includes(hcpId)) {
    return { ok: false, plan, error: `${hcpName} is already planned on ${day}.` }
  }
  if (slots.length >= maxPerDay) {
    return {
      ok: false,
      plan,
      error: `${day} already has ${maxPerDay} visits (the daily maximum). Remove a visit or choose another day.`,
    }
  }
  return { ok: true, plan: { ...plan, [day]: [...slots, hcpId] } }
}

export function removeFromPlan(plan: WeeklyPlan, day: Weekday, hcpId: string): WeeklyPlan {
  return { ...plan, [day]: plan[day].filter((id) => id !== hcpId) }
}

export function plannedIds(plan: WeeklyPlan): Set<string> {
  return new Set(WEEKDAYS.flatMap((d) => plan[d]))
}

export function totalVisits(plan: WeeklyPlan): number {
  return WEEKDAYS.reduce((n, d) => n + plan[d].length, 0)
}

export interface DuplicateWarning {
  hcpId: string
  days: Weekday[]
}

export function findDuplicates(plan: WeeklyPlan): DuplicateWarning[] {
  const byHcp = new Map<string, Weekday[]>()
  for (const day of WEEKDAYS) {
    for (const id of plan[day]) byHcp.set(id, [...(byHcp.get(id) ?? []), day])
  }
  return [...byHcp.entries()].filter(([, days]) => days.length > 1).map(([hcpId, days]) => ({ hcpId, days }))
}

export function isOverdueSegmentA(hcp: ScoredHcp, thresholdDays = OVERDUE_SEGMENT_A_DAYS): boolean {
  return hcp.segment === 'A' && (hcp.daysSinceLastVisit === null || hcp.daysSinceLastVisit > thresholdDays)
}

export function findMissingOverdueSegmentA(
  plan: WeeklyPlan,
  territoryHcps: ScoredHcp[],
  thresholdDays = OVERDUE_SEGMENT_A_DAYS,
): ScoredHcp[] {
  const planned = plannedIds(plan)
  return territoryHcps
    .filter((h) => isOverdueSegmentA(h, thresholdDays) && !planned.has(h.id))
    .sort((a, b) => (b.daysSinceLastVisit ?? Infinity) - (a.daysSinceLastVisit ?? Infinity) || a.name.localeCompare(b.name))
}
