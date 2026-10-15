import { LogOut } from 'lucide-react'
import { NavLink } from 'react-router-dom'

import { cn } from '@/lib/utils'
import { NAV_ITEMS } from '@/nav/navigation'

interface SidebarNavProps {
  onNavigate?: () => void
  onExit: () => void
  /** Icon-only rail at tablet widths; the mobile drawer always shows labels. */
  collapsible?: boolean
}

export function SidebarNav({ onNavigate, onExit, collapsible = false }: SidebarNavProps) {
  const labelClass = collapsible ? 'sr-only lg:not-sr-only' : undefined
  const detailClass = collapsible ? 'hidden lg:block' : undefined

  return (
    <nav aria-label="Main navigation" className="flex h-full flex-col gap-1 p-3">
      <p className={cn('px-3 pb-1 pt-2 text-xs font-semibold uppercase tracking-wider text-sidebar-muted', detailClass)}>
        Menu
      </p>
      <ul className="flex flex-col gap-1">
        {NAV_ITEMS.map((item) => (
          <li key={item.path}>
            <NavLink
              to={item.path}
              end={item.path === '/'}
              onClick={onNavigate}
              title={item.label}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                  collapsible && 'justify-center lg:justify-start',
                  isActive ? 'bg-sidebar-accent text-white' : 'text-sidebar-foreground/85',
                )
              }
            >
              <item.icon className="h-5 w-5 shrink-0" aria-hidden="true" />
              <span className="flex min-w-0 flex-col">
                <span className={labelClass}>{item.label}</span>
                {item.menmapOption && (
                  <span className={cn('truncate text-xs font-normal text-sidebar-muted', detailClass)}>
                    {item.menmapOption}. {item.menmapLabel}
                  </span>
                )}
              </span>
            </NavLink>
          </li>
        ))}
      </ul>
      <div className="mt-auto border-t border-sidebar-accent pt-3">
        <button
          type="button"
          onClick={onExit}
          title="Exit"
          className={cn(
            'flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-sidebar-foreground/85 transition-colors hover:bg-sidebar-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
            collapsible && 'justify-center lg:justify-start',
          )}
        >
          <LogOut className="h-5 w-5 shrink-0" aria-hidden="true" />
          <span className="flex flex-col items-start">
            <span className={labelClass}>Exit</span>
            <span className={cn('text-xs font-normal text-sidebar-muted', detailClass)}>3. End session</span>
          </span>
        </button>
      </div>
    </nav>
  )
}
