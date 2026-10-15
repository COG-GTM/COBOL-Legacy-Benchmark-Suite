import {
  ArrowLeftRight,
  Briefcase,
  FileBarChart,
  LayoutDashboard,
  PieChart,
  ScrollText,
  type LucideIcon,
} from 'lucide-react'

export interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  /** MENMAP option number this item replaces (src/maps/INQSET.bms). */
  menmapOption?: string
  menmapLabel?: string
}

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/', icon: LayoutDashboard },
  { label: 'Portfolios', path: '/portfolios', icon: Briefcase },
  {
    label: 'Positions',
    path: '/positions',
    icon: PieChart,
    menmapOption: '1',
    menmapLabel: 'Portfolio Position Inquiry',
  },
  {
    label: 'Transactions',
    path: '/transactions',
    icon: ArrowLeftRight,
    menmapOption: '2',
    menmapLabel: 'Transaction History',
  },
  { label: 'Reports', path: '/reports', icon: FileBarChart },
  { label: 'Audit Log', path: '/audit-log', icon: ScrollText },
]

export type MenmapOption =
  | { option: string; label: string; kind: 'route'; path: string }
  | { option: string; label: string; kind: 'exit' }

/** MENMAP options, in the order shown on the 3270 screen (INQSET.bms lines 13-18). */
export const MENMAP_OPTIONS: MenmapOption[] = [
  { option: '1', label: 'Portfolio Position Inquiry', kind: 'route', path: '/positions' },
  { option: '2', label: 'Transaction History', kind: 'route', path: '/transactions' },
  { option: '3', label: 'Exit', kind: 'exit' },
]

export const MENMAP_INVALID_OPTION_MESSAGE = 'Invalid option. Enter 1, 2 or 3.'

export interface PlaceholderPageMeta {
  title: string
  description: string
  legacySource: string[]
  plannedIn: { key: string; ticket: string; summary: string }[]
}

export const PLACEHOLDER_PAGES = {
  portfolios: {
    title: 'Portfolios',
    description: 'Browse, open, create and maintain client portfolios.',
    legacySource: ['PORTFLIO.cpy', 'PORTADD / PORTUPDT / PORTDEL'],
    plannedIn: [
      { key: 'FR-3', ticket: 'MBA-1438', summary: 'Portfolio list' },
      { key: 'FR-4', ticket: 'MBA-1439', summary: 'Portfolio detail' },
      { key: 'FR-5', ticket: 'MBA-1440', summary: 'Create / edit portfolio form' },
    ],
  },
  positions: {
    title: 'Portfolio Position Inquiry',
    description: 'Look up the current positions held in an account.',
    legacySource: ['MENMAP option 1', 'POSMAP (INQSET.bms)', 'INQPORT', 'POSREC.cpy'],
    plannedIn: [{ key: 'FR-6', ticket: 'MBA-1441', summary: 'Position inquiry screen' }],
  },
  transactions: {
    title: 'Transaction History',
    description: 'Review and enter portfolio transactions.',
    legacySource: ['MENMAP option 2', 'HISMAP (INQSET.bms)', 'INQHIST', 'TRNREC.cpy'],
    plannedIn: [
      { key: 'FR-7', ticket: 'MBA-1442', summary: 'Transaction history with pagination' },
      { key: 'FR-8', ticket: 'MBA-1443', summary: 'Transaction entry (BUY / SELL / TRANSFER / FEE)' },
    ],
  },
  reports: {
    title: 'Reports',
    description: 'Position, audit and statistics reports.',
    legacySource: ['RPTPOS00', 'RPTAUD00', 'RPTSTA00'],
    plannedIn: [{ key: 'FR-9', ticket: 'MBA-1444', summary: 'Report viewer' }],
  },
  auditLog: {
    title: 'Audit Log',
    description: 'Who changed what, and when, across the portfolio system.',
    legacySource: ['AUDITLOG.cpy', 'AUDPROC'],
    plannedIn: [{ key: 'FR-9', ticket: 'MBA-1444', summary: 'Audit report in the report viewer' }],
  },
} satisfies Record<string, PlaceholderPageMeta>

export type PlaceholderPageId = keyof typeof PLACEHOLDER_PAGES
