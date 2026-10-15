import { Construction } from 'lucide-react'

import { PageHeader } from '@/components/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PLACEHOLDER_PAGES, type PlaceholderPageId } from '@/nav/navigation'

export function PlaceholderPage({ pageId }: { pageId: PlaceholderPageId }) {
  const page = PLACEHOLDER_PAGES[pageId]
  return (
    <>
      <PageHeader title={page.title} description={page.description} />
      <Card className="max-w-2xl">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Construction className="h-5 w-5 text-amber-600" aria-hidden="true" />
            <CardTitle className="text-base">Coming soon</CardTitle>
          </div>
          <CardDescription>This screen is a placeholder. It will be built in:</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <ul className="space-y-2">
            {page.plannedIn.map((item) => (
              <li key={item.key + item.summary} className="flex flex-wrap items-center gap-2 text-sm">
                <Badge variant="secondary">{item.key}</Badge>
                <span className="font-mono text-xs text-muted-foreground">{item.ticket}</span>
                <span>{item.summary}</span>
              </li>
            ))}
          </ul>
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Replaces</p>
            <div className="flex flex-wrap gap-2">
              {page.legacySource.map((source) => (
                <Badge key={source} variant="outline" className="font-mono font-normal">
                  {source}
                </Badge>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>
    </>
  )
}
