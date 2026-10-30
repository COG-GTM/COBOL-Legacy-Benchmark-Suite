const MS_PER_DAY = 86_400_000

export function toIsoDate(d: Date): string {
  return d.toISOString().slice(0, 10)
}

export function startOfUtcDay(d: Date): Date {
  return new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()))
}

export function addDays(d: Date, days: number): Date {
  return new Date(d.getTime() + days * MS_PER_DAY)
}

export function daysBetween(fromIso: string, asOf: Date): number {
  const from = Date.parse(`${fromIso}T00:00:00Z`)
  return Math.round((asOf.getTime() - from) / MS_PER_DAY)
}
