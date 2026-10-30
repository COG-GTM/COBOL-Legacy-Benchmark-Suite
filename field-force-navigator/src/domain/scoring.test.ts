import { describe, expect, it, vi } from 'vitest'
import { addDays, startOfIsoWeek, toIsoDate } from './dates'
import { calculateScore, DEFAULT_RULES, lastVisitDate, scoreHcp, validateRules } from './scoring'
import type { Hcp, ScoringRules } from './types'

const asOf = new Date('2026-10-30T00:00:00Z')

function hcp(partial: Partial<Hcp> & { lastVisitDaysAgo?: number | null; followUps?: number }): Hcp {
  const { lastVisitDaysAgo = 20, followUps = 0, ...rest } = partial
  return {
    id: 'HCP-T',
    name: 'Dr. Test Ambervale',
    specialty: 'Cardiology',
    segment: 'A',
    territoryId: 'T1',
    city: 'Northgate',
    visits:
      lastVisitDaysAgo === null
        ? []
        : [
            { date: toIsoDate(addDays(asOf, -(lastVisitDaysAgo + 30))), repId: 'R1' },
            { date: toIsoDate(addDays(asOf, -lastVisitDaysAgo)), repId: 'R1' },
          ],
    openFollowUps: Array.from({ length: followUps }, (_, i) => ({ id: `FU-${i}`, note: 'n', createdOn: '2026-10-01' })),
    ...rest,
  }
}

describe('calculateScore', () => {
  it('AC2: Segment A, 45 days, 1 open follow-up scores 100 and lists all 3 factors', () => {
    const r = calculateScore({ segment: 'A', daysSinceLastVisit: 45, openFollowUps: 1 })
    expect(r.score).toBe(100)
    expect(r.reason).toBe('Segment A, 45 days since last visit, 1 open follow-up')
  })

  it('AC3: visited 3 days ago scores 0', () => {
    const r = calculateScore({ segment: 'A', daysSinceLastVisit: 3, openFollowUps: 2 })
    expect(r.score).toBe(0)
    expect(r.reason).toContain('3 days since last visit')
    expect(r.reason).toContain('not due')
  })

  it('treats exactly 7 days as within the recent-visit window (score 0) and 8 days as due', () => {
    expect(calculateScore({ segment: 'B', daysSinceLastVisit: 7, openFollowUps: 0 }).score).toBe(0)
    expect(calculateScore({ segment: 'B', daysSinceLastVisit: 8, openFollowUps: 0 }).score).toBe(25 + 8)
  })

  it('visited today scores 0 with "visited today" reason', () => {
    const r = calculateScore({ segment: 'C', daysSinceLastVisit: 0, openFollowUps: 0 })
    expect(r).toEqual({ score: 0, reason: 'Segment C, visited today (within 7 days, not due), no open follow-ups' })
  })

  it.each([
    ['A', 10, 0, 50],
    ['B', 20, 0, 45],
    ['C', 30, 0, 40],
    ['C', 39, 1, 69],
    ['B', 40, 1, 85],
    ['C', 120, 0, 50],
    ['B', 90, 1, 85],
  ] as const)('Segment %s, %i days, %i follow-ups => %i', (segment, days, fu, expected) => {
    expect(calculateScore({ segment, daysSinceLastVisit: days, openFollowUps: fu }).score).toBe(expected)
  })

  it('caps day points at 40', () => {
    const a = calculateScore({ segment: 'C', daysSinceLastVisit: 41, openFollowUps: 0 })
    const b = calculateScore({ segment: 'C', daysSinceLastVisit: 300, openFollowUps: 0 })
    expect(a.score).toBe(50)
    expect(b.score).toBe(50)
  })

  it('adds a flat +20 for any open follow-ups and pluralises the reason', () => {
    const r = calculateScore({ segment: 'C', daysSinceLastVisit: 10, openFollowUps: 2 })
    expect(r.score).toBe(10 + 10 + 20)
    expect(r.reason).toBe('Segment C, 10 days since last visit, 2 open follow-ups')
  })

  it('uses singular "day" for 1 day when the recent window is 0', () => {
    const r = calculateScore({ segment: 'B', daysSinceLastVisit: 1, openFollowUps: 0 }, { ...DEFAULT_RULES, recentVisitDays: 0 })
    expect(r).toEqual({ score: 26, reason: 'Segment B, 1 day since last visit, no open follow-ups' })
  })

  it('caps the total at 100', () => {
    expect(calculateScore({ segment: 'A', daysSinceLastVisit: 200, openFollowUps: 3 }).score).toBe(100)
  })

  it('never-visited HCPs get the full day points', () => {
    const r = calculateScore({ segment: 'B', daysSinceLastVisit: null, openFollowUps: 0 })
    expect(r.score).toBe(65)
    expect(r.reason).toBe('Segment B, no visit in the last 12 months, no open follow-ups')
  })

  it('honours configurable rules', () => {
    const rules: ScoringRules = {
      segmentWeights: { A: 50, B: 30, C: 5 },
      pointsPerDay: 2,
      daysCap: 30,
      openFollowUpPoints: 10,
      recentVisitDays: 14,
      maxScore: 80,
    }
    expect(calculateScore({ segment: 'A', daysSinceLastVisit: 10, openFollowUps: 0 }, rules).score).toBe(0)
    expect(calculateScore({ segment: 'C', daysSinceLastVisit: 15, openFollowUps: 0 }, rules).score).toBe(5 + 30)
    expect(calculateScore({ segment: 'B', daysSinceLastVisit: 15, openFollowUps: 1 }, rules).score).toBe(70)
    expect(calculateScore({ segment: 'A', daysSinceLastVisit: 15, openFollowUps: 1 }, rules).score).toBe(80)
    const r = calculateScore({ segment: 'A', daysSinceLastVisit: 10, openFollowUps: 0 }, rules)
    expect(r.reason).toContain('within 14 days')
  })
})

describe('scoreHcp', () => {
  it('derives days since last visit from the most recent visit', () => {
    const s = scoreHcp(hcp({ segment: 'A', lastVisitDaysAgo: 45, followUps: 1 }), asOf)
    expect(s.daysSinceLastVisit).toBe(45)
    expect(s.score).toBe(100)
    expect(s.reason).toBe('Segment A, 45 days since last visit, 1 open follow-up')
  })

  it('AC3 via HCP record: visited 3 days ago => 0', () => {
    expect(scoreHcp(hcp({ lastVisitDaysAgo: 3 }), asOf).score).toBe(0)
  })

  it('handles HCPs with no visits', () => {
    const s = scoreHcp(hcp({ segment: 'C', lastVisitDaysAgo: null }), asOf)
    expect(s.daysSinceLastVisit).toBeNull()
    expect(s.score).toBe(50)
  })

  it('lastVisitDate ignores visit order', () => {
    const h = hcp({ lastVisitDaysAgo: 12 })
    h.visits.reverse()
    expect(lastVisitDate(h)).toBe(toIsoDate(addDays(asOf, -12)))
  })
})

describe('validateRules', () => {
  it('accepts defaults', () => {
    expect(validateRules(DEFAULT_RULES)).toEqual([])
  })

  it('rejects negatives, NaN and non-positive max', () => {
    const errs = validateRules({
      ...DEFAULT_RULES,
      segmentWeights: { A: -1, B: Number.NaN, C: 10 },
      maxScore: 0,
    })
    expect(errs).toEqual([
      'Segment A weight must be a number of 0 or more',
      'Segment B weight must be a number of 0 or more',
      'Max score must be between 1 and 100',
    ])
  })

  it('rejects a max score above 100', () => {
    expect(validateRules({ ...DEFAULT_RULES, maxScore: 150 })).toEqual(['Max score must be between 1 and 100'])
  })
})

describe('score ceiling', () => {
  it('never exceeds 100 even if stored rules raise weights and the cap', () => {
    const rules = { ...DEFAULT_RULES, segmentWeights: { A: 80, B: 25, C: 10 }, maxScore: 150 }
    expect(calculateScore({ segment: 'A', daysSinceLastVisit: 45, openFollowUps: 1 }, rules).score).toBe(100)
  })
})

describe('startOfIsoWeek', () => {
  it('keeps a UTC-midnight Monday in its own week in a western time zone', () => {
    vi.stubEnv('TZ', 'America/Los_Angeles')
    try {
      const monday = new Date('2026-11-02T00:00:00Z')
      expect(monday.getDay()).toBe(0)
      expect(toIsoDate(startOfIsoWeek(monday))).toBe('2026-11-02')
      expect(toIsoDate(startOfIsoWeek(new Date('2026-11-08T00:00:00Z')))).toBe('2026-11-02')
    } finally {
      vi.unstubAllEnvs()
    }
  })
})
