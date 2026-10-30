import { daysBetween } from './dates'
import type { Hcp, ScoredHcp, ScoreResult, ScoringRules, Segment } from './types'

export const DEFAULT_RULES: ScoringRules = {
  segmentWeights: { A: 40, B: 25, C: 10 },
  pointsPerDay: 1,
  daysCap: 40,
  openFollowUpPoints: 20,
  recentVisitDays: 7,
  maxScore: 100,
}

export interface ScoreInput {
  segment: Segment
  daysSinceLastVisit: number | null
  openFollowUps: number
}

function plural(n: number, word: string): string {
  return `${n} ${word}${n === 1 ? '' : 's'}`
}

function describeDays(days: number | null): string {
  if (days === null) return 'no visit in the last 12 months'
  if (days === 0) return 'visited today'
  return `${plural(days, 'day')} since last visit`
}

function describeFollowUps(count: number): string {
  return count === 0 ? 'no open follow-ups' : plural(count, 'open follow-up')
}

export function calculateScore(input: ScoreInput, rules: ScoringRules = DEFAULT_RULES): ScoreResult {
  const { segment, daysSinceLastVisit: days, openFollowUps } = input
  const segmentText = `Segment ${segment}`
  const daysText = describeDays(days)
  const followUpText = describeFollowUps(openFollowUps)

  if (days !== null && days <= rules.recentVisitDays) {
    return {
      score: 0,
      reason: `${segmentText}, ${daysText} (within ${rules.recentVisitDays} days, not due), ${followUpText}`,
    }
  }

  const segmentPoints = rules.segmentWeights[segment]
  const dayPoints = days === null ? rules.daysCap : Math.min(days * rules.pointsPerDay, rules.daysCap)
  const followUpPoints = openFollowUps > 0 ? rules.openFollowUpPoints : 0
  const score = Math.max(0, Math.min(segmentPoints + dayPoints + followUpPoints, rules.maxScore))

  return { score, reason: `${segmentText}, ${daysText}, ${followUpText}` }
}

export function lastVisitDate(hcp: Hcp): string | null {
  let latest: string | null = null
  for (const v of hcp.visits) {
    if (latest === null || v.date > latest) latest = v.date
  }
  return latest
}

export function scoreHcp(hcp: Hcp, asOf: Date, rules: ScoringRules = DEFAULT_RULES): ScoredHcp {
  const last = lastVisitDate(hcp)
  const daysSinceLastVisit = last === null ? null : daysBetween(last, asOf)
  const { score, reason } = calculateScore(
    { segment: hcp.segment, daysSinceLastVisit, openFollowUps: hcp.openFollowUps.length },
    rules,
  )
  return { ...hcp, daysSinceLastVisit, score, reason }
}

export function validateRules(rules: ScoringRules): string[] {
  const errors: string[] = []
  const nonNegative: [string, number][] = [
    ['Segment A weight', rules.segmentWeights.A],
    ['Segment B weight', rules.segmentWeights.B],
    ['Segment C weight', rules.segmentWeights.C],
    ['Points per day', rules.pointsPerDay],
    ['Days cap', rules.daysCap],
    ['Open follow-up points', rules.openFollowUpPoints],
    ['Recent visit window', rules.recentVisitDays],
  ]
  for (const [label, value] of nonNegative) {
    if (!Number.isFinite(value) || value < 0) errors.push(`${label} must be a number of 0 or more`)
  }
  if (!Number.isFinite(rules.maxScore) || rules.maxScore <= 0) errors.push('Max score must be greater than 0')
  return errors
}
