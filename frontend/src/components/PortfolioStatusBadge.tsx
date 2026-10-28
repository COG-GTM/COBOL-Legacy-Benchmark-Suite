import { Badge, type BadgeProps } from '@/components/ui/badge'
import { PORTFOLIO_STATUS_LABELS } from '@/lib/legacyCodes'
import type { PortfolioStatus } from '@/types/portfolio'

/** PORT-STATUS 88-levels: Active = green, Closed = gray, Suspended = red. */
const PORTFOLIO_STATUS_BADGE: Record<PortfolioStatus, BadgeProps['variant']> = {
  A: 'success',
  C: 'secondary',
  S: 'destructive',
}

export function PortfolioStatusBadge({ status, className }: { status: PortfolioStatus; className?: string }) {
  return (
    <Badge variant={PORTFOLIO_STATUS_BADGE[status]} className={className} data-status={status}>
      {PORTFOLIO_STATUS_LABELS[status]}
    </Badge>
  )
}
