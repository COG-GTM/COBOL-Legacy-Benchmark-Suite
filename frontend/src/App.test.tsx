import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import App from './App'
import { ROUTER_FUTURE_FLAGS } from './lib/router'
import { MENMAP_INVALID_OPTION_MESSAGE, NAV_ITEMS } from './nav/navigation'

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]} future={ROUTER_FUTURE_FLAGS}>
      <App />
    </MemoryRouter>,
  )
}

const pageHeading = () => screen.getByRole('heading', { level: 2 })
// The desktop sidebar is always rendered; the mobile drawer only mounts when opened.
const mainNav = () => screen.getByRole('navigation', { name: 'Main navigation' })

const ROUTES: [path: string, heading: string][] = [
  ['/', 'Dashboard'],
  ['/portfolios', 'Portfolios'],
  ['/positions', 'Portfolio Position Inquiry'],
  ['/transactions', 'Transaction History'],
  ['/reports', 'Reports'],
  ['/audit-log', 'Audit Log'],
  ['/no-such-page', 'Page not found'],
]

describe('navigation shell', () => {
  it.each(ROUTES)('renders %s with the full navigation', (path, heading) => {
    renderAt(path)
    expect(pageHeading()).toHaveTextContent(heading)
    const nav = mainNav()
    for (const item of NAV_ITEMS) {
      expect(within(nav).getByRole('link', { name: new RegExp(`^${item.label}`) })).toBeInTheDocument()
    }
    expect(within(nav).getByRole('button', { name: /^Exit/ })).toBeInTheDocument()
  })

  it('routes every menu item to its page and marks it active', async () => {
    const user = userEvent.setup()
    renderAt('/')
    for (const [path, heading] of ROUTES.slice(1, -1).concat([ROUTES[0]])) {
      const item = NAV_ITEMS.find((nav) => nav.path === path)!
      const link = within(mainNav()).getByRole('link', { name: new RegExp(`^${item.label}`) })
      await user.click(link)
      expect(pageHeading()).toHaveTextContent(heading)
      expect(link).toHaveAttribute('aria-current', 'page')
    }
  })

  it('labels the MENMAP options on the matching nav items', () => {
    renderAt('/')
    const nav = mainNav()
    expect(within(nav).getByRole('link', { name: /Positions.*1\. Portfolio Position Inquiry/ })).toHaveAttribute(
      'href',
      '/positions',
    )
    expect(within(nav).getByRole('link', { name: /Transactions.*2\. Transaction History/ })).toHaveAttribute(
      'href',
      '/transactions',
    )
  })

  it('asks for confirmation on Exit and ends the session', async () => {
    const user = userEvent.setup()
    renderAt('/reports')
    await user.click(within(mainNav()).getByRole('button', { name: /^Exit/ }))
    const dialog = screen.getByRole('dialog', { name: 'End your session?' })
    await user.click(within(dialog).getByRole('button', { name: 'Stay' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(pageHeading()).toHaveTextContent('Reports')

    await user.click(within(mainNav()).getByRole('button', { name: /^Exit/ }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Exit' }))
    expect(screen.getByText('Session ended')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Main navigation' })).not.toBeInTheDocument()
  })

  it('opens the navigation drawer from the header menu button', async () => {
    const user = userEvent.setup()
    renderAt('/')
    await user.click(screen.getByRole('button', { name: 'Open navigation' }))
    const drawer = screen.getByRole('dialog')
    await user.click(within(drawer).getByRole('link', { name: /^Audit Log/ }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(pageHeading()).toHaveTextContent('Audit Log')
  })
})

describe('dashboard', () => {
  it('shows summary cards from the mock data', () => {
    renderAt('/')
    const card = (name: string) => screen.getByRole('group', { name })
    expect(card('Total portfolios')).toHaveTextContent('8')
    expect(card('Total portfolios')).toHaveTextContent('6 active · 1 suspended · 1 closed')
    expect(card('Total value')).toHaveTextContent('$28,697,950.45')
    expect(card('Cash balance')).toHaveTextContent('$2,025,781.53')
    expect(card('Recent transactions')).toHaveTextContent('9')
    expect(card('Recent transactions')).toHaveTextContent('Last 7 days · 1 pending')
  })

  it('lists the five most recent transactions', () => {
    renderAt('/')
    const rows = within(screen.getByRole('table', { name: 'Recent transactions' })).getAllByRole('row')
    expect(rows).toHaveLength(6)
    expect(rows[1]).toHaveTextContent('PORT0005')
    expect(rows[1]).toHaveTextContent('Pending')
  })
})

describe('MENMAP main menu card', () => {
  it.each([
    ['1', 'Portfolio Position Inquiry'],
    ['2', 'Transaction History'],
  ])('option %s opens %s', async (option, heading) => {
    const user = userEvent.setup()
    renderAt('/')
    await user.type(screen.getByLabelText('Select option'), `${option}{Enter}`)
    expect(pageHeading()).toHaveTextContent(heading)
  })

  it('option 3 asks to exit', async () => {
    const user = userEvent.setup()
    renderAt('/')
    await user.type(screen.getByLabelText('Select option'), '3{Enter}')
    expect(screen.getByRole('dialog', { name: 'End your session?' })).toBeInTheDocument()
  })

  it('shows the error on the message line for an invalid option and clears it on navigation', async () => {
    const user = userEvent.setup()
    renderAt('/')
    const status = screen.getByTestId('status-line')
    expect(status).toHaveTextContent('Ready')

    await user.type(screen.getByLabelText('Select option'), '9{Enter}')
    expect(screen.getByRole('alert')).toHaveTextContent(MENMAP_INVALID_OPTION_MESSAGE)

    await user.click(within(mainNav()).getByRole('link', { name: /^Reports/ }))
    expect(screen.getByTestId('status-line')).toHaveTextContent('Ready')
  })

  it('opens an option when clicked', async () => {
    const user = userEvent.setup()
    renderAt('/')
    await user.click(screen.getByRole('button', { name: /Transaction History/ }))
    expect(pageHeading()).toHaveTextContent('Transaction History')
  })
})
