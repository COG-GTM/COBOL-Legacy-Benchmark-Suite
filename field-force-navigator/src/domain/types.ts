export type Segment = 'A' | 'B' | 'C'

export const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'] as const
export type Weekday = (typeof WEEKDAYS)[number]

export interface Territory {
  id: string
  name: string
}

export interface Rep {
  id: string
  name: string
  territoryId: string
}

export interface DistrictManager {
  id: string
  name: string
  territoryIds: string[]
}

export interface Visit {
  date: string
  repId: string
}

export interface FollowUp {
  id: string
  note: string
  createdOn: string
}

export interface Hcp {
  id: string
  name: string
  specialty: string
  segment: Segment
  territoryId: string
  city: string
  visits: Visit[]
  openFollowUps: FollowUp[]
}

export interface ScoringRules {
  segmentWeights: Record<Segment, number>
  pointsPerDay: number
  daysCap: number
  openFollowUpPoints: number
  recentVisitDays: number
  maxScore: number
}

export interface ScoreResult {
  score: number
  reason: string
}

export interface ScoredHcp extends Hcp {
  daysSinceLastVisit: number | null
  score: number
  reason: string
}

export type WeeklyPlan = Record<Weekday, string[]>
