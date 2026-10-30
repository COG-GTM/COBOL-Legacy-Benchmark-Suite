import type { ScoredHcp, Segment } from './types'

export type DaysBucket = 'any' | '0-7' | '8-30' | '31-60' | '61+' | 'never'

export interface HcpFilters {
  search: string
  specialty: string
  segment: Segment | 'all'
  days: DaysBucket
}

export const EMPTY_FILTERS: HcpFilters = { search: '', specialty: 'all', segment: 'all', days: 'any' }

export const DAYS_BUCKETS: { value: DaysBucket; label: string }[] = [
  { value: 'any', label: 'Any' },
  { value: '0-7', label: '0-7 days' },
  { value: '8-30', label: '8-30 days' },
  { value: '31-60', label: '31-60 days' },
  { value: '61+', label: '61+ days' },
  { value: 'never', label: 'No visit in 12 months' },
]

export function inDaysBucket(days: number | null, bucket: DaysBucket): boolean {
  switch (bucket) {
    case 'any':
      return true
    case 'never':
      return days === null
    case '0-7':
      return days !== null && days <= 7
    case '8-30':
      return days !== null && days >= 8 && days <= 30
    case '31-60':
      return days !== null && days >= 31 && days <= 60
    case '61+':
      return days !== null && days >= 61
  }
}

export function applyFilters(hcps: ScoredHcp[], f: HcpFilters): ScoredHcp[] {
  const q = f.search.trim().toLowerCase()
  return hcps.filter(
    (h) =>
      (q === '' || h.name.toLowerCase().includes(q)) &&
      (f.specialty === 'all' || h.specialty === f.specialty) &&
      (f.segment === 'all' || h.segment === f.segment) &&
      inDaysBucket(h.daysSinceLastVisit, f.days),
  )
}

export function sortByPriority(hcps: ScoredHcp[]): ScoredHcp[] {
  return [...hcps].sort((a, b) => b.score - a.score || a.name.localeCompare(b.name))
}
