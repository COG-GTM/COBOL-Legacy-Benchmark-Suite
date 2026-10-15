import { Menu, UserCircle2 } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { MOCK_AS_OF_DATE } from '@/data/mockData'
import { formatDate } from '@/lib/format'

import { ExitDialog } from './ExitDialog'
import { ShellContext, type ShellContextValue, type StatusMessage } from './shellContext'
import { SidebarNav } from './SidebarNav'
import { StatusLine } from './StatusLine'

export const OPERATOR_ID = 'OPER01'

export function AppLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [message, setMessage] = useState<StatusMessage | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [exitOpen, setExitOpen] = useState(false)

  useEffect(() => {
    setMessage(null)
  }, [pathname])

  const requestExit = useCallback(() => {
    setDrawerOpen(false)
    setExitOpen(true)
  }, [])

  const confirmExit = useCallback(() => {
    setExitOpen(false)
    navigate('/signed-out')
  }, [navigate])

  const shell = useMemo<ShellContextValue>(() => ({ message, setMessage, requestExit }), [message, requestExit])

  return (
    <ShellContext.Provider value={shell}>
      <div className="flex min-h-screen bg-background">
        <aside className="sticky top-0 hidden h-screen shrink-0 flex-col bg-sidebar text-sidebar-foreground md:flex md:w-[4.5rem] lg:w-64">
          <div className="flex h-16 items-center justify-center gap-2 border-b border-sidebar-accent px-4 lg:justify-start">
            <span className="flex h-8 w-8 items-center justify-center rounded bg-primary font-mono text-sm font-bold text-primary-foreground">
              PM
            </span>
            <span className="hidden text-sm font-semibold leading-tight lg:block">
              Portfolio
              <br />
              Management System
            </span>
          </div>
          <SidebarNav collapsible onExit={requestExit} />
        </aside>

        <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
          <SheetContent side="left" className="w-72 bg-sidebar p-0 text-sidebar-foreground">
            <div className="flex h-16 items-center border-b border-sidebar-accent px-4">
              <SheetTitle className="text-sm text-sidebar-foreground">Portfolio Management System</SheetTitle>
              <SheetDescription className="sr-only">Main navigation</SheetDescription>
            </div>
            <SidebarNav onNavigate={() => setDrawerOpen(false)} onExit={requestExit} />
          </SheetContent>
        </Sheet>

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b bg-card/95 px-4 backdrop-blur md:px-6">
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              aria-label="Open navigation"
              onClick={() => setDrawerOpen(true)}
            >
              <Menu />
            </Button>
            <h1 className="truncate text-base font-semibold sm:text-lg">Portfolio Management System</h1>
            <div className="ml-auto flex items-center gap-4 text-sm text-muted-foreground">
              <span className="hidden sm:inline">Business date {formatDate(MOCK_AS_OF_DATE)}</span>
              <span className="flex items-center gap-1.5" aria-label={`Signed in as ${OPERATOR_ID}`}>
                <UserCircle2 className="h-5 w-5" aria-hidden="true" />
                <span className="font-mono">{OPERATOR_ID}</span>
              </span>
            </div>
          </header>

          <main className="flex-1 px-4 py-6 md:px-6 lg:px-8">
            <Outlet />
          </main>

          <StatusLine message={message} />
        </div>
      </div>

      <ExitDialog open={exitOpen} onOpenChange={setExitOpen} onConfirm={confirmExit} />
    </ShellContext.Provider>
  )
}
