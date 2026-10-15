import { Link } from 'react-router-dom'

import { PageHeader } from '@/components/PageHeader'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <>
      <PageHeader title="Page not found" description="That screen doesn't exist. Pick an option from the menu." />
      <Button asChild variant="outline">
        <Link to="/">Back to dashboard</Link>
      </Button>
    </>
  )
}
