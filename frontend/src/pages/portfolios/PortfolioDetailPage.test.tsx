import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import App from '@/App'
import { ROUTER_FUTURE_FLAGS } from '@/lib/router'

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]} future={ROUTER_FUTURE_FLAGS}>
      <App />
    </MemoryRouter>,
  )
}

const pageHeading = () => screen.getByRole('heading', { level: 2 })
const field = (source: string) => document.querySelector(`[data-field="${source}"]`)

describe('portfolio detail (FR-4)', () => {
  it('shows every PORT-RECORD field with formatting', () => {
    renderAt('/portfolios/PORT0001')
    expect(pageHeading()).toHaveTextContent('Margaret Chen')
    expect(field('PORT-ID')).toHaveTextContent('PORT0001')
    expect(field('PORT-ACCOUNT-NO')).toHaveTextContent('1000000001')
    expect(field('PORT-CLIENT-NAME')).toHaveTextContent('Margaret Chen')
    expect(field('PORT-CLIENT-TYPE')).toHaveTextContent('Individual')
    expect(field('PORT-CREATE-DATE')).toHaveTextContent('Mar 12, 2019')
    expect(field('PORT-LAST-MAINT')).toHaveTextContent('Oct 13, 2026')
    expect(field('PORT-STATUS')).toHaveTextContent('Active')
    expect(field('PORT-TOTAL-VALUE')).toHaveTextContent('$1,284,530.75')
    expect(field('PORT-CASH-BALANCE')).toHaveTextContent('$48,210.15')
    expect(field('PORT-LAST-USER')).toHaveTextContent('OPER01')
    expect(field('PORT-LAST-TRANS')).toHaveTextContent('Oct 14, 2026')
  })

  it('groups fields into the copybook sections', () => {
    renderAt('/portfolios/PORT0001')
    for (const name of [
      'Key information',
      'Client information',
      'Portfolio information',
      'Financial information',
      'Audit information',
    ]) {
      expect(screen.getByRole('region', { name })).toBeInTheDocument()
    }
  })

  it.each([
    ['PORT0002', 'Corporate', 'Active', 'success'],
    ['PORT0003', 'Trust', 'Active', 'success'],
    ['PORT0006', 'Corporate', 'Suspended', 'destructive'],
    ['PORT0008', 'Trust', 'Closed', 'secondary'],
  ])('%s shows client type %s and a %s badge', (id, clientType, status, variantClass) => {
    renderAt(`/portfolios/${id}`)
    expect(field('PORT-CLIENT-TYPE')).toHaveTextContent(clientType)
    const badge = within(field('PORT-STATUS') as HTMLElement).getByText(status)
    const expected = { success: 'bg-emerald-100', destructive: 'bg-destructive', secondary: 'bg-secondary' }[variantClass]!
    expect(badge).toHaveClass(expected)
  })

  it('lists this portfolio’s positions and recent transactions', () => {
    renderAt('/portfolios/PORT0001')
    const positions = within(screen.getByRole('table', { name: 'Positions' })).getAllByRole('row')
    expect(positions).toHaveLength(4)
    expect(positions[1]).toHaveTextContent('VTSAX00001')
    expect(positions[1]).toHaveTextContent('$556,344.27')

    const txns = within(screen.getByRole('table', { name: 'Portfolio transactions' })).getAllByRole('row')
    expect(txns).toHaveLength(3)
    expect(txns[1]).toHaveTextContent('Oct 14, 2026')
    expect(txns[1]).toHaveTextContent('Buy')
    expect(txns[2]).toHaveTextContent('Sell')
  })

  it('links to positions and transaction history filtered to this portfolio', async () => {
    const user = userEvent.setup()
    renderAt('/portfolios/PORT0005')
    await user.click(screen.getByRole('link', { name: /View positions/ }))
    expect(pageHeading()).toHaveTextContent('Portfolio Position Inquiry')
    expect(screen.getByRole('status', { name: 'Portfolio filter' })).toHaveTextContent('PORT0005')

    await user.click(screen.getByRole('link', { name: /Back to portfolio/ }))
    expect(pageHeading()).toHaveTextContent('Lindqvist Pension Trust')

    await user.click(screen.getByRole('link', { name: /View history/ }))
    expect(pageHeading()).toHaveTextContent('Transaction History')
    expect(screen.getByRole('status', { name: 'Portfolio filter' })).toHaveTextContent('PORT0005')
  })

  it('opens from a portfolio ID on the dashboard and keeps Portfolios active in the menu', async () => {
    const user = userEvent.setup()
    renderAt('/')
    const recent = screen.getByRole('table', { name: 'Recent transactions' })
    await user.click(within(recent).getAllByRole('link', { name: 'PORT0002' })[0])
    expect(pageHeading()).toHaveTextContent('Halvorsen Industrial Corp')
    const nav = screen.getByRole('navigation', { name: 'Main navigation' })
    expect(within(nav).getByRole('link', { name: /^Portfolios/ })).toHaveAttribute('aria-current', 'page')
  })

  it('shows empty states for a closed portfolio with no activity', () => {
    renderAt('/portfolios/PORT0008')
    expect(screen.getByText('No transactions recorded.')).toBeInTheDocument()
  })

  it('shows not found for an unknown portfolio ID', () => {
    renderAt('/portfolios/ZZZZ9999')
    expect(pageHeading()).toHaveTextContent('Portfolio not found')
    expect(screen.getByRole('link', { name: /Back to portfolios/ })).toHaveAttribute('href', '/portfolios')
  })
})
