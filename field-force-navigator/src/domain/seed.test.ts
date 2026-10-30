import { describe, expect, it } from 'vitest'
import { scoreHcp } from './scoring'
import { generateSeed, HCP_COUNT } from './seed'

const asOf = new Date('2026-10-30T00:00:00Z')

describe('synthetic seed', () => {
  const seed = generateSeed(asOf)

  it('has 3 territories, 6 reps and 300 HCPs with unique ids and names', () => {
    expect(seed.territories).toHaveLength(3)
    expect(seed.reps).toHaveLength(6)
    expect(seed.hcps).toHaveLength(HCP_COUNT)
    expect(new Set(seed.hcps.map((h) => h.id)).size).toBe(300)
    expect(new Set(seed.hcps.map((h) => h.name)).size).toBe(300)
  })

  it('every rep belongs to a territory and every territory has HCPs', () => {
    for (const r of seed.reps) expect(seed.territories.map((t) => t.id)).toContain(r.territoryId)
    for (const t of seed.territories) expect(seed.hcps.filter((h) => h.territoryId === t.id).length).toBe(100)
  })

  it('visit history spans up to 12 months and is attributed to reps of the same territory', () => {
    const oldest = asOf.getTime() - 366 * 86_400_000
    for (const h of seed.hcps) {
      for (const v of h.visits) {
        expect(Date.parse(v.date)).toBeGreaterThanOrEqual(oldest)
        expect(seed.reps.find((r) => r.id === v.repId)?.territoryId).toBe(h.territoryId)
      }
    }
    expect(seed.hcps.reduce((n, h) => n + h.visits.length, 0)).toBeGreaterThan(1500)
  })

  it('has a few open follow-ups and all three segments', () => {
    const withFu = seed.hcps.filter((h) => h.openFollowUps.length > 0).length
    expect(withFu).toBeGreaterThan(5)
    expect(withFu).toBeLessThan(60)
    expect(new Set(seed.hcps.map((h) => h.segment))).toEqual(new Set(['A', 'B', 'C']))
  })

  it('is deterministic for a given date', () => {
    expect(generateSeed(asOf)).toEqual(seed)
  })

  it('pins the acceptance scenarios in territory T1', () => {
    const t1 = seed.hcps.filter((h) => h.territoryId === 'T1').map((h) => scoreHcp(h, asOf))
    expect(t1[0]).toMatchObject({ segment: 'A', daysSinceLastVisit: 45, score: 100 })
    expect(t1[1]).toMatchObject({ daysSinceLastVisit: 3, score: 0 })
  })
})
