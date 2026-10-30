import { addDays, toIsoDate } from './dates'
import type { DistrictManager, FollowUp, Hcp, Rep, Segment, Territory, Visit } from './types'

// Every name below is invented for this synthetic dataset. No real people, no PII.

export const TERRITORIES: Territory[] = [
  { id: 'T1', name: 'Northgate Metro' },
  { id: 'T2', name: 'Riverbend Valley' },
  { id: 'T3', name: 'Saltmarsh Coast' },
]

export const REPS: Rep[] = [
  { id: 'R1', name: 'Rep Ada Quillon', territoryId: 'T1' },
  { id: 'R2', name: 'Rep Bram Tellish', territoryId: 'T1' },
  { id: 'R3', name: 'Rep Cleo Marrowind', territoryId: 'T2' },
  { id: 'R4', name: 'Rep Dax Pembrook', territoryId: 'T2' },
  { id: 'R5', name: 'Rep Esme Varrow', territoryId: 'T3' },
  { id: 'R6', name: 'Rep Finn Oakhollow', territoryId: 'T3' },
]

export const DISTRICT_MANAGERS: DistrictManager[] = [
  { id: 'DM1', name: 'DM Gwen Halloway', territoryIds: ['T1', 'T2', 'T3'] },
]

export const SPECIALTIES = [
  'Cardiology',
  'Endocrinology',
  'Neurology',
  'Oncology',
  'Primary Care',
  'Rheumatology',
] as const

const CITIES: Record<string, string[]> = {
  T1: ['Northgate', 'Elmstead', 'Copperfield Heights'],
  T2: ['Riverbend', 'Millbrook', 'Ashvale'],
  T3: ['Saltmarsh', 'Gullhaven', 'Tidewater'],
}

const FIRST_NAMES = [
  'Avery', 'Blair', 'Casey', 'Dana', 'Emery', 'Finley', 'Gray', 'Harper', 'Indra', 'Jules',
  'Kai', 'Lane', 'Morgan', 'Noor', 'Oakley', 'Parker', 'Quinn', 'Reese', 'Sage', 'Tatum',
]

const LAST_NAMES = [
  'Ambervale', 'Brackenridge', 'Corrimore', 'Dunmoth', 'Elsworthy', 'Fennimore', 'Glimmerton',
  'Hollowell', 'Ironwick', 'Juniperson', 'Kestrelby', 'Larkspurr', 'Mossgrove', 'Nettlefield',
  'Orchardine', 'Pebblestone', 'Quarrington', 'Rookwood', 'Sablethorn', 'Thistlewood',
  'Umberfield', 'Vexmoor', 'Willowmere', 'Yarrowby', 'Zephyrine', 'Ashcombe', 'Briarwell',
  'Cindermoor', 'Driftwell', 'Emberlyn',
]

const FOLLOW_UP_NOTES = [
  'Send formulary update requested at last visit',
  'Bring patient-support brochure',
  'Answer dosing question from last call',
  'Confirm speaker-programme interest',
  'Drop off sample request form',
]

export const HCP_COUNT = 300
const HISTORY_DAYS = 365
const CADENCE: Record<Segment, number> = { A: 21, B: 35, C: 60 }

function mulberry32(seed: number): () => number {
  let a = seed >>> 0
  return () => {
    a = (a + 0x6d2b79f5) >>> 0
    let t = a
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export interface SeedData {
  territories: Territory[]
  reps: Rep[]
  districtManagers: DistrictManager[]
  hcps: Hcp[]
}

interface Pinned {
  segment: Segment
  lastVisitDaysAgo: number | null
  followUps: number
  specialty?: string
}

// Fixed records in territory T1 so the acceptance scenarios are always reproducible in the UI.
const PINNED_T1: Pinned[] = [
  { segment: 'A', lastVisitDaysAgo: 45, followUps: 1, specialty: 'Cardiology' },
  { segment: 'B', lastVisitDaysAgo: 3, followUps: 0, specialty: 'Endocrinology' },
  { segment: 'A', lastVisitDaysAgo: 38, followUps: 0, specialty: 'Oncology' },
  { segment: 'A', lastVisitDaysAgo: 5, followUps: 1, specialty: 'Neurology' },
]

export function generateSeed(asOf: Date, seed = 1440): SeedData {
  const rand = mulberry32(seed)
  const int = (min: number, max: number) => min + Math.floor(rand() * (max - min + 1))
  const pick = <T,>(xs: readonly T[]): T => xs[Math.floor(rand() * xs.length)]

  const allNames: string[] = []
  for (const f of FIRST_NAMES) for (const l of LAST_NAMES) allNames.push(`Dr. ${f} ${l}`)
  for (let i = allNames.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1))
    ;[allNames[i], allNames[j]] = [allNames[j], allNames[i]]
  }

  const hcps: Hcp[] = []
  let followUpSeq = 0
  for (let i = 0; i < HCP_COUNT; i++) {
    const territory = TERRITORIES[i % TERRITORIES.length]
    const territoryReps = REPS.filter((r) => r.territoryId === territory.id)
    const pinned = territory.id === 'T1' ? PINNED_T1[Math.floor(i / TERRITORIES.length)] : undefined

    const roll = rand()
    const segment: Segment = pinned?.segment ?? (roll < 0.2 ? 'A' : roll < 0.55 ? 'B' : 'C')
    const specialty = pinned?.specialty ?? pick(SPECIALTIES)
    const cadence = CADENCE[segment]

    let lastDaysAgo: number | null
    if (pinned) lastDaysAgo = pinned.lastVisitDaysAgo
    else if (segment === 'C' && rand() < 0.06) lastDaysAgo = null
    else lastDaysAgo = int(0, Math.round(cadence * 2.2))

    const visits: Visit[] = []
    if (lastDaysAgo !== null) {
      for (let d = lastDaysAgo; d <= HISTORY_DAYS; d += Math.max(7, cadence + int(-7, 7))) {
        visits.push({ date: toIsoDate(addDays(asOf, -d)), repId: pick(territoryReps).id })
      }
    }

    const followUpCount = pinned ? pinned.followUps : rand() < 0.07 ? (rand() < 0.2 ? 2 : 1) : 0
    const openFollowUps: FollowUp[] = []
    for (let k = 0; k < followUpCount; k++) {
      followUpSeq++
      openFollowUps.push({
        id: `FU-${String(followUpSeq).padStart(3, '0')}`,
        note: pick(FOLLOW_UP_NOTES),
        createdOn: toIsoDate(addDays(asOf, -int(1, 60))),
      })
    }

    hcps.push({
      id: `HCP-${String(i + 1).padStart(4, '0')}`,
      name: allNames[i],
      specialty,
      segment,
      territoryId: territory.id,
      city: pick(CITIES[territory.id]),
      visits: visits.sort((a, b) => b.date.localeCompare(a.date)),
      openFollowUps,
    })
  }

  return { territories: TERRITORIES, reps: REPS, districtManagers: DISTRICT_MANAGERS, hcps }
}
