import { describe, expect, it } from 'vitest'
import { escapeCsv, planToCsv } from './csv'
import { applyFilters, EMPTY_FILTERS, inDaysBucket, sortByPriority } from './filters'
import {
  addToPlan,
  emptyPlan,
  findDuplicates,
  findMissingOverdueSegmentA,
  isOverdueSegmentA,
  removeFromPlan,
  totalVisits,
} from './plan'
import type { ScoredHcp } from './types'

function s(id: string, over: Partial<ScoredHcp> = {}): ScoredHcp {
  return {
    id,
    name: `Dr. ${id}`,
    specialty: 'Oncology',
    segment: 'B',
    territoryId: 'T1',
    city: 'X',
    visits: [],
    openFollowUps: [],
    daysSinceLastVisit: 10,
    score: 35,
    reason: 'Segment B, 10 days since last visit, no open follow-ups',
    ...over,
  }
}

describe('plan builder', () => {
  it('adds up to 6 visits a day and AC4 blocks the 7th with a message', () => {
    let plan = emptyPlan()
    for (let i = 1; i <= 6; i++) {
      const r = addToPlan(plan, 'Mon', `H${i}`, `Dr. H${i}`)
      expect(r.ok).toBe(true)
      plan = r.plan
    }
    const r = addToPlan(plan, 'Mon', 'H7', 'Dr. H7')
    expect(r.ok).toBe(false)
    if (!r.ok) expect(r.error).toMatch(/Mon already has 6 visits/)
    expect(r.plan.Mon).toHaveLength(6)
    expect(addToPlan(plan, 'Tue', 'H7', 'Dr. H7').ok).toBe(true)
  })

  it('blocks the same HCP twice on the same day', () => {
    const p = addToPlan(emptyPlan(), 'Wed', 'H1', 'Dr. H1').plan
    const r = addToPlan(p, 'Wed', 'H1', 'Dr. H1')
    expect(r.ok).toBe(false)
    if (!r.ok) expect(r.error).toBe('Dr. H1 is already planned on Wed.')
  })

  it('warns about duplicates across days', () => {
    let p = addToPlan(emptyPlan(), 'Mon', 'H1', 'a').plan
    p = addToPlan(p, 'Thu', 'H1', 'a').plan
    p = addToPlan(p, 'Thu', 'H2', 'b').plan
    expect(findDuplicates(p)).toEqual([{ hcpId: 'H1', days: ['Mon', 'Thu'] }])
    expect(totalVisits(p)).toBe(3)
    p = removeFromPlan(p, 'Mon', 'H1')
    expect(findDuplicates(p)).toEqual([])
  })

  it('AC5: names Segment A HCPs more than 30 days overdue that are not planned', () => {
    const hcps = [
      s('A45', { segment: 'A', daysSinceLastVisit: 45 }),
      s('A30', { segment: 'A', daysSinceLastVisit: 30 }),
      s('A31', { segment: 'A', daysSinceLastVisit: 31 }),
      s('Anever', { segment: 'A', daysSinceLastVisit: null }),
      s('B90', { segment: 'B', daysSinceLastVisit: 90 }),
    ]
    expect(isOverdueSegmentA(hcps[1])).toBe(false)
    expect(findMissingOverdueSegmentA(emptyPlan(), hcps).map((h) => h.id)).toEqual(['Anever', 'A45', 'A31'])
    const planned = addToPlan(emptyPlan(), 'Fri', 'A45', 'x').plan
    expect(findMissingOverdueSegmentA(planned, hcps).map((h) => h.id)).toEqual(['Anever', 'A31'])
  })
})

describe('CSV export', () => {
  it('AC6: writes day, HCP, specialty, score and reason in day order', () => {
    const a = s('H1', { name: 'Dr. One', score: 100, reason: 'Segment A, 45 days since last visit, 1 open follow-up' })
    const b = s('H2', { name: 'Dr. Two', specialty: 'Primary Care', score: 0 })
    let p = addToPlan(emptyPlan(), 'Tue', 'H2', '').plan
    p = addToPlan(p, 'Mon', 'H1', '').plan
    p = addToPlan(p, 'Mon', 'ghost', '').plan
    const csv = planToCsv(p, new Map([a, b].map((h) => [h.id, h])))
    expect(csv.split('\r\n')).toEqual([
      'Day,HCP,Specialty,Score,Reason',
      'Mon,Dr. One,Oncology,100,"Segment A, 45 days since last visit, 1 open follow-up"',
      'Tue,Dr. Two,Primary Care,0,"Segment B, 10 days since last visit, no open follow-ups"',
      '',
    ])
  })

  it('escapes quotes and newlines', () => {
    expect(escapeCsv('a "b"')).toBe('"a ""b"""')
    expect(escapeCsv('x\ny')).toBe('"x\ny"')
    expect(escapeCsv(5)).toBe('5')
  })
})

describe('filters', () => {
  const hcps = [
    s('1', { name: 'Dr. Quinn Rookwood', specialty: 'Cardiology', segment: 'A', daysSinceLastVisit: 3, score: 0 }),
    s('2', { name: 'Dr. Sage Mossgrove', specialty: 'Oncology', segment: 'B', daysSinceLastVisit: 20, score: 45 }),
    s('3', { name: 'Dr. Kai Quarrington', specialty: 'Cardiology', segment: 'C', daysSinceLastVisit: 45, score: 50 }),
    s('4', { name: 'Dr. Lane Vexmoor', specialty: 'Neurology', segment: 'A', daysSinceLastVisit: 90, score: 80 }),
    s('5', { name: 'Dr. Noor Ashcombe', specialty: 'Neurology', segment: 'C', daysSinceLastVisit: null, score: 50 }),
  ]
  const ids = (xs: ScoredHcp[]) => xs.map((h) => h.id)

  it('empty filters return everything', () => expect(applyFilters(hcps, EMPTY_FILTERS)).toHaveLength(5))
  it('search is case-insensitive substring on name', () =>
    expect(ids(applyFilters(hcps, { ...EMPTY_FILTERS, search: '  QU ' }))).toEqual(['1', '3']))
  it('specialty', () => expect(ids(applyFilters(hcps, { ...EMPTY_FILTERS, specialty: 'Cardiology' }))).toEqual(['1', '3']))
  it('segment', () => expect(ids(applyFilters(hcps, { ...EMPTY_FILTERS, segment: 'A' }))).toEqual(['1', '4']))
  it.each([
    ['0-7', ['1']],
    ['8-30', ['2']],
    ['31-60', ['3']],
    ['61+', ['4']],
    ['never', ['5']],
  ] as const)('days bucket %s', (days, expected) => expect(ids(applyFilters(hcps, { ...EMPTY_FILTERS, days }))).toEqual(expected))
  it('combined', () =>
    expect(ids(applyFilters(hcps, { search: 'q', specialty: 'Cardiology', segment: 'C', days: '31-60' }))).toEqual(['3']))
  it('null days only match any/never', () => {
    expect(inDaysBucket(null, 'any')).toBe(true)
    expect(inDaysBucket(null, '61+')).toBe(false)
  })
  it('sorts by score desc then name', () => expect(ids(sortByPriority(hcps))).toEqual(['4', '3', '5', '2', '1']))
})
