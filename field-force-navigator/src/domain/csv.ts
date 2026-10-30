import { WEEKDAYS, type ScoredHcp, type WeeklyPlan } from './types'

export const CSV_HEADER = ['Day', 'HCP', 'Specialty', 'Score', 'Reason'] as const

export function escapeCsv(value: string | number): string {
  const s = String(value)
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

export function planToCsv(plan: WeeklyPlan, hcpsById: Map<string, ScoredHcp>): string {
  const rows: (string | number)[][] = [[...CSV_HEADER]]
  for (const day of WEEKDAYS) {
    for (const id of plan[day]) {
      const h = hcpsById.get(id)
      if (h) rows.push([day, h.name, h.specialty, h.score, h.reason])
    }
  }
  return rows.map((r) => r.map(escapeCsv).join(',')).join('\r\n') + '\r\n'
}
