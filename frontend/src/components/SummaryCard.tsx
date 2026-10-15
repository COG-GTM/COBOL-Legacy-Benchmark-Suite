import type { LucideIcon } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface SummaryCardProps {
  title: string
  value: string
  detail: string
  icon: LucideIcon
}

export function SummaryCard({ title, value, detail, icon: Icon }: SummaryCardProps) {
  return (
    <Card role="group" aria-label={title}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-semibold tabular-nums">{value}</p>
        <p className="mt-1 text-xs text-muted-foreground">{detail}</p>
      </CardContent>
    </Card>
  )
}
