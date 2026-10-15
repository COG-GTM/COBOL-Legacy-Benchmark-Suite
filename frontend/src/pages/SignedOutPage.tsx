import { LogOut } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export function SignedOutPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md text-center">
        <CardHeader className="items-center">
          <LogOut className="mb-2 h-8 w-8 text-muted-foreground" aria-hidden="true" />
          <CardTitle>Session ended</CardTitle>
          <CardDescription>
            You have exited the Portfolio Management System. Sign-in arrives with FR-1 (MBA-1436).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/">Return to dashboard</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
