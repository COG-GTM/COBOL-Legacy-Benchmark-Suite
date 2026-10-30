import { act, fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { generateSeed } from './domain/seed'

const asOf = new Date('2026-10-30T00:00:00Z')
const seed = generateSeed(asOf)
const t1 = seed.hcps.filter((h) => h.territoryId === 'T1')
const t2 = seed.hcps.filter((h) => h.territoryId === 'T2')

function rows() {
  return screen.getAllByTestId('hcp-row')
}

async function addVia(user: ReturnType<typeof userEvent.setup>, hcpId: string, day: string) {
  const row = rows().find((r) => r.dataset.hcpId === hcpId)!
  await user.selectOptions(within(row).getByRole('combobox'), day)
  await user.click(within(row).getByRole('button', { name: /^Add / }))
}

beforeEach(() => localStorage.clear())
afterEach(() => vi.restoreAllMocks())

describe('AC1 territory HCP list', () => {
  it('shows only the selected rep territory and filters/search narrow the list', async () => {
    const user = userEvent.setup()
    render(<App asOf={asOf} />)

    expect(screen.getByTestId('territory-name')).toHaveTextContent('Northgate Metro')
    expect(rows()).toHaveLength(100)
    const t1Ids = new Set(t1.map((h) => h.id))
    expect(rows().every((r) => t1Ids.has(r.dataset.hcpId!))).toBe(true)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Rep' }), 'R3')
    expect(screen.getByTestId('territory-name')).toHaveTextContent('Riverbend Valley')
    const t2Ids = new Set(t2.map((h) => h.id))
    expect(rows()).toHaveLength(100)
    expect(rows().every((r) => t2Ids.has(r.dataset.hcpId!))).toBe(true)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Segment' }), 'A')
    const segA = t2.filter((h) => h.segment === 'A').length
    expect(rows()).toHaveLength(segA)
    expect(screen.getByTestId('hcp-count')).toHaveTextContent(`Showing ${segA} of 100`)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Specialty' }), 'Cardiology')
    const segACardio = t2.filter((h) => h.segment === 'A' && h.specialty === 'Cardiology')
    expect(screen.queryAllByTestId('hcp-row')).toHaveLength(segACardio.length)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Segment' }), 'all')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Specialty' }), 'all')
    const target = t2[7]
    fireEvent.change(screen.getByRole('searchbox', { name: 'Search by name' }), { target: { value: target.name.split(' ')[2].toUpperCase() } })
    expect(rows().map((r) => r.dataset.hcpId)).toContain(target.id)
    expect(rows().length).toBeLessThan(100)

    fireEvent.change(screen.getByRole('searchbox', { name: 'Search by name' }), { target: { value: '' } })
    await user.selectOptions(screen.getByRole('combobox', { name: 'Days since last visit' }), '0-7')
    expect(rows().every((r) => Number(within(r).getAllByRole('cell')[2].textContent) <= 7)).toBe(true)
  })

  it('renders 300-HCP dataset and territory list in under 1 second', () => {
    const start = performance.now()
    render(<App asOf={asOf} />)
    expect(rows()).toHaveLength(100)
    expect(performance.now() - start).toBeLessThan(1000)
  })
})

describe('AC2/AC3 scores in the list', () => {
  it('shows score 100 with all three factors and 0 for a recent visit', () => {
    render(<App asOf={asOf} />)
    const a = rows().find((r) => r.dataset.hcpId === t1[0].id)!
    expect(within(a).getByText('100')).toBeInTheDocument()
    expect(within(a).getByText('Segment A, 45 days since last visit, 1 open follow-up')).toBeInTheDocument()
    const recent = rows().find((r) => r.dataset.hcpId === t1[1].id)!
    expect(within(recent).getAllByRole('cell')[3]).toHaveTextContent(/^0$/)
  })
})

describe('AC4 daily cap', () => {
  it('blocks a 7th visit on a day with a message', async () => {
    const user = userEvent.setup()
    render(<App asOf={asOf} />)
    const ids = rows().slice(0, 7).map((r) => r.dataset.hcpId!)
    for (const id of ids.slice(0, 6)) await addVia(user, id, 'Tue')
    expect(within(screen.getByTestId('day-Tue')).getAllByRole('listitem')).toHaveLength(6)
    await addVia(user, ids[6], 'Tue')
    expect(screen.getByRole('status')).toHaveTextContent('Tue already has 6 visits (the daily maximum)')
    expect(within(screen.getByTestId('day-Tue')).getAllByRole('listitem')).toHaveLength(6)
  })
})

describe('AC5 overdue Segment A warning and duplicates', () => {
  it('names an overdue Segment A HCP left off the plan, and clears once planned', async () => {
    const user = userEvent.setup()
    render(<App asOf={asOf} />)
    const overdue = t1[2]
    const warning = () => screen.queryAllByTestId('overdue-warning').find((w) => w.textContent?.includes(overdue.name))
    expect(warning()).toBeDefined()
    expect(warning()).toHaveTextContent(`${overdue.name} (38 days since last visit, more than 30) is not in the plan`)
    await addVia(user, overdue.id, 'Wed')
    expect(warning()).toBeUndefined()
  })

  it('warns when the same HCP is planned on two days', async () => {
    const user = userEvent.setup()
    render(<App asOf={asOf} />)
    await addVia(user, t1[0].id, 'Mon')
    await addVia(user, t1[0].id, 'Thu')
    expect(screen.getByRole('status')).toHaveTextContent('also planned on Mon')
    expect(screen.getByTestId('duplicate-warning')).toHaveTextContent(`${t1[0].name} is planned on Mon, Thu`)
    await addVia(user, t1[0].id, 'Thu')
    expect(screen.getByRole('status')).toHaveTextContent('already planned on Thu')
    await user.click(screen.getByRole('button', { name: `Remove ${t1[0].name} from Thu` }))
    expect(screen.queryByTestId('duplicate-warning')).toBeNull()
  })
})

describe('AC6 CSV export', () => {
  it('downloads a CSV with day, HCP, specialty, score and reason', async () => {
    const user = userEvent.setup()
    let blob: Blob | undefined
    const createObjectURL = vi.fn((b: Blob) => {
      blob = b
      return 'blob:plan'
    })
    Object.assign(URL, { createObjectURL, revokeObjectURL: vi.fn() })
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    render(<App asOf={asOf} />)
    await addVia(user, t1[0].id, 'Mon')
    await addVia(user, t1[2].id, 'Fri')
    await user.click(screen.getByRole('button', { name: 'Export CSV' }))
    expect(click).toHaveBeenCalledOnce()
    const text = await blob!.text()
    const lines = text.trim().split('\r\n')
    expect(lines[0]).toBe('Day,HCP,Specialty,Score,Reason')
    expect(lines[1]).toBe(`Mon,${t1[0].name},Cardiology,100,"Segment A, 45 days since last visit, 1 open follow-up"`)
    expect(lines[2]).toMatch(new RegExp(`^Fri,${t1[2].name},Oncology,78,`))
  })
})

describe('persistence, rules and manager review', () => {
  it('persists the plan per rep and shows it to the district manager', async () => {
    const user = userEvent.setup()
    const { unmount } = render(<App asOf={asOf} />)
    await addVia(user, t1[0].id, 'Mon')
    unmount()
    render(<App asOf={asOf} />)
    expect(within(screen.getByTestId('day-Mon')).getByText(t1[0].name)).toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: 'District manager' }))
    const reviewRows = screen.getAllByTestId('review-row')
    expect(reviewRows).toHaveLength(6)
    expect(reviewRows[0]).toHaveTextContent(t1[0].name)
    await user.click(screen.getByRole('radio', { name: 'Field rep' }))
    await user.click(screen.getByRole('button', { name: 'Clear' }))
    expect(within(screen.getByTestId('day-Mon')).queryAllByRole('listitem')).toHaveLength(0)
  })

  it('applies edited scoring rules and can reset them', async () => {
    const user = userEvent.setup()
    render(<App asOf={asOf} />)
    await user.click(screen.getByText('Scoring rules'))
    const fu = screen.getByRole('spinbutton', { name: 'Open follow-up points' })
    await user.clear(fu)
    expect(screen.getByRole('alert')).toHaveTextContent('Open follow-up points must be a number of 0 or more')
    expect(screen.getByRole('button', { name: 'Apply rules' })).toBeDisabled()
    await user.type(fu, '0')
    const segA = screen.getByRole('spinbutton', { name: 'Segment A weight' })
    await user.clear(segA)
    await user.type(segA, '10')
    await user.click(screen.getByRole('button', { name: 'Apply rules' }))
    const a = () => rows().find((r) => r.dataset.hcpId === t1[0].id)!
    expect(within(a()).getAllByRole('cell')[3]).toHaveTextContent(/^50$/)
    await user.click(screen.getByRole('button', { name: 'Reset to defaults' }))
    expect(within(a()).getAllByRole('cell')[3]).toHaveTextContent(/^100$/)
  })

  it('ignores corrupt storage', () => {
    localStorage.setItem('ffn.plan.R1', '{bad json')
    localStorage.setItem('ffn.rules', JSON.stringify({ daysCap: 20 }))
    act(() => {
      render(<App asOf={asOf} />)
    })
    expect(within(screen.getByTestId('day-Mon')).queryAllByRole('listitem')).toHaveLength(0)
    const a = rows().find((r) => r.dataset.hcpId === t1[0].id)!
    expect(within(a).getAllByRole('cell')[3]).toHaveTextContent(/^80$/)
  })
})
