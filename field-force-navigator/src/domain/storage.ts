import { DEFAULT_RULES } from './scoring'
import { emptyPlan } from './plan'
import { WEEKDAYS, type ScoringRules, type WeeklyPlan } from './types'

const PLAN_KEY = (repId: string) => `ffn.plan.${repId}`
const RULES_KEY = 'ffn.rules'

function read<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    return null
  }
}

export function loadPlan(repId: string): WeeklyPlan {
  const stored = read<Partial<WeeklyPlan>>(PLAN_KEY(repId))
  const plan = emptyPlan()
  if (!stored) return plan
  for (const d of WEEKDAYS) if (Array.isArray(stored[d])) plan[d] = stored[d]!.filter((x) => typeof x === 'string')
  return plan
}

export function savePlan(repId: string, plan: WeeklyPlan): void {
  localStorage.setItem(PLAN_KEY(repId), JSON.stringify(plan))
}

export function loadRules(): ScoringRules {
  const stored = read<Partial<ScoringRules>>(RULES_KEY)
  if (!stored) return DEFAULT_RULES
  return {
    ...DEFAULT_RULES,
    ...stored,
    segmentWeights: { ...DEFAULT_RULES.segmentWeights, ...(stored.segmentWeights ?? {}) },
  }
}

export function saveRules(rules: ScoringRules): void {
  localStorage.setItem(RULES_KEY, JSON.stringify(rules))
}
