/**
 * Central navigation model.
 *
 * Drives the primary sidebar, the breadcrumb trail, and the keyboard-shortcut
 * map so they never drift out of sync. Replaces the legacy MENMAP menu and
 * PF-key flow from `src/maps/INQSET.bms` (lines 7-19) with standard web
 * navigation.
 */

export interface NavItem {
  /** Absolute route path. */
  readonly path: string;
  /** Human-readable label shown in the sidebar and breadcrumbs. */
  readonly label: string;
  /** Short description used as a tooltip / dashboard card copy. */
  readonly description: string;
  /** Single-key accelerator pressed after the `g` (go to) leader key. */
  readonly shortcut: string;
}

export const NAV_ITEMS: readonly NavItem[] = [
  {
    path: '/',
    label: 'Dashboard',
    description: 'Portfolio overview and key metrics',
    shortcut: 'd',
  },
  {
    path: '/portfolios',
    label: 'Portfolios',
    description: 'Manage portfolio master records',
    shortcut: 'p',
  },
  {
    path: '/transactions',
    label: 'Transactions',
    description: 'Record and review transactions',
    shortcut: 't',
  },
  {
    path: '/history',
    label: 'History',
    description: 'Audit and change history',
    shortcut: 'h',
  },
  {
    path: '/reports',
    label: 'Reports',
    description: 'Positions, audit and statistics reports',
    shortcut: 'r',
  },
] as const;

/** Looks up the human-readable label for a route segment, if any. */
export function labelForPath(path: string): string | undefined {
  return NAV_ITEMS.find((item) => item.path === path)?.label;
}
