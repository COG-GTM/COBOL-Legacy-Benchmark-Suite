import { Component, createContext, type FormEvent, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { Link, NavLink, Navigate, Outlet, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { apiClient } from './api/client'
import type { HistoryEntry, Portfolio, Position, Transaction } from './api/types'
import { validateAccountNumber, validateAmount, validateInvestmentType, validatePortfolioId } from './validation/portvald'

type Role = 'admin' | 'readonly'
interface Session { username: string; role: Role }
interface Toast { kind: 'success' | 'error'; message: string }

const SessionContext = createContext<{ session: Session | null; login: (username: string, role: Role) => void; logout: () => void }>({
  session: null, login: () => undefined, logout: () => undefined,
})
const ApiContext = createContext(apiClient)
const ToastContext = createContext<(toast: Toast) => void>(() => undefined)

export function useSession() { return useContext(SessionContext) }
export function useApi() { return useContext(ApiContext) }
export function useToast() { return useContext(ToastContext) }

class ErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  state = { hasError: false }
  static getDerivedStateFromError() { return { hasError: true } }
  render() {
    return this.state.hasError
      ? <main className="fatal-error"><div className="error-card"><span className="eyebrow">ERRMAP · SYSTEM</span><h1>Something went wrong</h1><p>We could not load this workspace. Refresh the page to try again.</p><button className="button primary" onClick={() => window.location.reload()}>Refresh workspace</button></div></main>
      : this.props.children
  }
}

function App() {
  const [session, setSession] = useState<Session | null>(() => {
    const stored = sessionStorage.getItem('portfolio-session')
    return stored ? JSON.parse(stored) as Session : null
  })
  const [toast, setToast] = useState<Toast | null>(null)
  const login = (username: string, role: Role) => {
    const next = { username, role }
    sessionStorage.setItem('portfolio-session', JSON.stringify(next))
    setSession(next)
  }
  const logout = () => { sessionStorage.removeItem('portfolio-session'); setSession(null) }
  const notify = (nextToast: Toast) => {
    setToast(nextToast)
    window.setTimeout(() => setToast(null), 3500)
  }
  return (
    <ErrorBoundary>
      <SessionContext.Provider value={{ session, login, logout }}>
        <ApiContext.Provider value={apiClient}>
          <ToastContext.Provider value={notify}>
            {toast && <div className={`toast ${toast.kind}`} role="status">{toast.kind === 'success' ? '✓' : '!'} {toast.message}</div>}
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<ProtectedLayout />}><Route element={<Shell />}><Route index element={<Navigate to="/dashboard" replace />} /><Route path="dashboard" element={<Dashboard />} /><Route path="portfolios" element={<Portfolios />} /><Route path="portfolios/new" element={<PortfolioForm />} /><Route path="portfolios/:id" element={<PortfolioDetail />} /><Route path="portfolios/:id/edit" element={<PortfolioForm />} /><Route path="positions" element={<PositionInquiry />} /><Route path="transactions" element={<TransactionHistory />} /><Route path="transactions/new" element={<TransactionEntry />} /><Route path="reports" element={<Reports />} /><Route path="status" element={<SystemStatus />} /></Route></Route>
              <Route path="*" element={<Navigate to={session ? '/dashboard' : '/login'} replace />} />
            </Routes>
          </ToastContext.Provider>
        </ApiContext.Provider>
      </SessionContext.Provider>
    </ErrorBoundary>
  )
}

function LoginPage() {
  const { session, login } = useSession()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>('readonly')
  const [error, setError] = useState('')
  useEffect(() => { if (session) navigate('/dashboard', { replace: true }) }, [session, navigate])
  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!username.trim() || !password.trim()) { setError('ERR-AUTH · Enter a username and password to continue.'); return }
    login(username.trim(), username.trim().toLowerCase() === 'admin' ? 'admin' : role)
    navigate('/dashboard', { replace: true })
  }
  return <main className="login-page"><div className="login-visual"><div className="brand-mark">COBOL<span>↗</span></div><div className="visual-copy"><span className="eyebrow">PORTFOLIO OPERATIONS</span><h1>Clarity for every investment decision.</h1><p>A modern operations console for the Investment Portfolio Management System.</p></div><div className="terminal-lines"><span>SYS · ONLINE</span><span>BATCH · 04:12</span><span>DATA · CURRENT</span></div></div><div className="login-panel"><div className="login-form-wrap"><span className="eyebrow">SECURE ACCESS</span><h2>Welcome back</h2><p className="muted">Sign in to your portfolio workspace.</p><form onSubmit={submit}><label>Username<input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="e.g. analyst" autoComplete="username" /></label><label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Any non-empty password" autoComplete="current-password" /></label><label>Workspace role<select value={role} onChange={(event) => setRole(event.target.value as Role)}><option value="readonly">Read-only analyst</option><option value="admin">Administrator</option></select></label>{error && <ErrorBanner code="ERR-AUTH" message={error.replace('ERR-AUTH · ', '')} />}<button className="button primary wide" type="submit">Sign in <span>→</span></button></form><p className="login-hint">Demo access: use <strong>admin</strong> for full controls, or any other username for read-only access.</p></div></div></main>
}

function ProtectedLayout() {
  const { session } = useSession()
  return session ? <Outlet /> : <Navigate to="/login" replace />
}

const navigation = [
  { to: '/dashboard', label: 'Overview', icon: '◈' },
  { to: '/portfolios', label: 'Portfolios', icon: '▦' },
  { to: '/positions', label: 'Position inquiry', icon: '⌕' },
  { to: '/transactions', label: 'Transactions', icon: '⇄' },
  { to: '/reports', label: 'Reports', icon: '▤' },
  { to: '/status', label: 'System status', icon: '◉' },
]

function Shell() {
  const { session, logout } = useSession()
  const [open, setOpen] = useState(false)
  return <div className="app-shell"><aside className={open ? 'sidebar open' : 'sidebar'}><div className="brand-mark">COBOL<span>↗</span></div><div className="sidebar-label">Workspace</div><nav>{navigation.map((item) => <NavLink key={item.to} to={item.to} onClick={() => setOpen(false)} className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}><span className="nav-icon">{item.icon}</span>{item.label}</NavLink>)}</nav><div className="sidebar-bottom"><div className="system-pill"><span className="status-dot" /> All systems operational</div><button className="user-menu" onClick={logout}><span className="avatar">{session?.username.slice(0, 1).toUpperCase()}</span><span><strong>{session?.username}</strong><small>{session?.role === 'admin' ? 'Administrator' : 'Read-only'}</small></span><span className="logout-icon">↪</span></button></div></aside><button className="mobile-menu" onClick={() => setOpen(!open)} aria-label="Toggle navigation">☰</button><div className="main-column"><header className="topbar"><div><span className="breadcrumb">INVESTMENT PORTFOLIO MANAGEMENT</span><span className="topbar-title">Operations console</span></div><div className="topbar-right"><span className="live-dot" /> <span>Live workspace</span><span className="topbar-date">14 Feb 2026</span></div></header><div className="page-content"><Outlet /></div></div></div>
}

function PageHeader({ eyebrow, title, description, action }: { eyebrow: string; title: string; description?: string; action?: ReactNode }) {
  return <div className="page-header"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1>{description && <p className="muted">{description}</p>}</div>{action && <div className="header-action">{action}</div>}</div>
}
function ErrorBanner({ code, message }: { code: string; message: string }) {
  return <div className="error-banner"><strong>{code}</strong><span>{message}</span></div>
}
function StatCard({ label, value, detail, tone = '' }: { label: string; value: string; detail: string; tone?: string }) {
  return <div className={`stat-card ${tone}`}><span className="stat-label">{label}</span><strong>{value}</strong><span className="stat-detail">{detail}</span></div>
}
function StatusBadge({ status }: { status: string }) {
  const labels: Record<string, string> = { A: 'Active', C: 'Closed', S: 'Suspended', P: 'Pending', D: 'Completed', F: 'Failed', R: 'Reversed', Healthy: 'Healthy', Warning: 'Warning' }
  return <span className={`badge badge-${status.toLowerCase()}`}><span />{labels[status] || status}</span>
}
function formatMoney(value: number) { return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value) }
function prettyDate(value: string) { return value.length === 8 ? `${value.slice(4, 6)}/${value.slice(6, 8)}/${value.slice(0, 4)}` : value }
function typeLabel(type: string) { return ({ BU: 'BUY', SL: 'SELL', TR: 'TRANSFER', FE: 'FEE' } as Record<string, string>)[type] || type }

function Dashboard() {
  const { session } = useSession()
  const [data, setData] = useState<Portfolio[]>([])
  const api = useApi()
  useEffect(() => { api.listPortfolios().then(setData) }, [api])
  const total = data.reduce((sum, item) => sum + item.totalValue, 0)
  return <><PageHeader eyebrow="OVERVIEW · MENMAP" title={`Good morning, ${session?.username}.`} description="Here’s the pulse of your investment operations." action={session?.role === 'admin' && <Link className="button primary" to="/portfolios/new">＋ New portfolio</Link>} /><div className="stats-grid"><StatCard label="Assets under management" value={formatMoney(total)} detail="+4.8% from prior month" tone="blue" /><StatCard label="Active portfolios" value={String(data.filter((item) => item.status === 'A').length).padStart(2, '0')} detail="Across 8 client groups" /><StatCard label="Cash available" value={formatMoney(data.reduce((sum, item) => sum + item.cashBalance, 0))} detail="Ready for allocation" /><StatCard label="Pending actions" value="07" detail="3 require attention" tone="amber" /></div><div className="dashboard-grid"><section className="panel chart-panel"><div className="panel-heading"><div><span className="eyebrow">PORTFOLIO VALUE</span><h2>Assets under management</h2></div><select><option>Last 12 months</option></select></div><div className="chart"><div className="chart-value">$1.43M <span>↑ 4.8%</span></div><div className="chart-bars">{[32, 39, 36, 52, 48, 60, 56, 68, 63, 76, 72, 88].map((height, index) => <div className="chart-bar-wrap" key={index}><div className="chart-bar" style={{ height: `${height}%` }} /><small>{['Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec', 'Jan', 'Feb'][index]}</small></div>)}</div></div></section><section className="panel shortcuts"><div className="panel-heading"><div><span className="eyebrow">QUICK ACTIONS</span><h2>Keep moving</h2></div></div><Link to="/positions" className="shortcut"><span className="shortcut-icon blue-icon">⌕</span><span><strong>Position inquiry</strong><small>Review current holdings</small></span><span>→</span></Link>{session?.role === 'admin' && <Link to="/transactions/new" className="shortcut"><span className="shortcut-icon green-icon">⇄</span><span><strong>Enter transaction</strong><small>Record a new trade</small></span><span>→</span></Link>}<Link to="/reports" className="shortcut"><span className="shortcut-icon purple-icon">▤</span><span><strong>View reports</strong><small>Operational intelligence</small></span><span>→</span></Link></section></div><section className="panel table-panel"><div className="panel-heading"><div><span className="eyebrow">RECENT PORTFOLIOS</span><h2>Portfolio watchlist</h2></div><Link className="text-link" to="/portfolios">View all portfolios →</Link></div><PortfolioTable portfolios={data.slice(0, 5)} compact /></section></>
}

function PortfolioTable({ portfolios, compact = false, onDelete }: { portfolios: Portfolio[]; compact?: boolean; onDelete?: (id: string) => void }) {
  return <div className="table-wrap"><table><thead><tr><th>Portfolio</th><th>Client</th><th>Account</th><th>Total value</th><th>Last activity</th><th>Status</th><th /></tr></thead><tbody>{portfolios.map((item) => <tr key={item.portfolioId}><td><Link className="table-primary" to={`/portfolios/${item.portfolioId}`}>{item.portfolioId}</Link><small>{item.clientType === 'I' ? 'Individual' : item.clientType === 'C' ? 'Corporate' : 'Trust'}</small></td><td>{item.clientName}</td><td className="mono">{item.accountNo}</td><td className="money">{formatMoney(item.totalValue)}</td><td>{prettyDate(item.lastTransDate)}</td><td><StatusBadge status={item.status} /></td><td>{!compact && <div className="row-actions"><Link to={`/portfolios/${item.portfolioId}/edit`}>Edit</Link>{onDelete && <button onClick={() => onDelete(item.portfolioId)}>Delete</button>}</div>}</td></tr>)}</tbody></table>{!portfolios.length && <div className="empty">No portfolios match this search.</div>}</div>
}

function Portfolios() {
  const { session } = useSession()
  const api = useApi()
  const notify = useToast()
  const [data, setData] = useState<Portfolio[]>([])
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState<'portfolioId' | 'totalValue'>('portfolioId')
  const [error, setError] = useState('')
  const refresh = useCallback(() => api.listPortfolios().then(setData).catch(() => setError('Unable to load portfolios.')), [api])
  useEffect(() => { void refresh() }, [refresh])
  const filtered = useMemo(() => [...data].filter((item) => `${item.portfolioId} ${item.clientName} ${item.accountNo}`.toLowerCase().includes(search.toLowerCase())).sort((a, b) => sort === 'totalValue' ? b.totalValue - a.totalValue : a.portfolioId.localeCompare(b.portfolioId)), [data, search, sort])
  const remove = async (id: string) => { if (!window.confirm(`Delete ${id}? This mock record will be removed.`)) return; await api.deletePortfolio(id); notify({ kind: 'success', message: `${id} deleted successfully.` }); refresh() }
  return <><PageHeader eyebrow="PORTFOLIO REGISTRY · PFMAIN" title="Portfolios" description="Manage portfolio masters and review client holdings." action={session?.role === 'admin' && <Link className="button primary" to="/portfolios/new">＋ Add portfolio</Link>} />{error && <ErrorBanner code="ERR-PF-01" message={error} />}<section className="panel table-panel"><div className="toolbar"><div className="search-box"><span>⌕</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search by portfolio, client or account..." /></div><label className="sort-select">Sort by <select value={sort} onChange={(event) => setSort(event.target.value as typeof sort)}><option value="portfolioId">Portfolio ID</option><option value="totalValue">Total value</option></select></label></div><PortfolioTable portfolios={filtered} onDelete={session?.role === 'admin' ? remove : undefined} /></section></>
}

function PortfolioDetail() {
  const { id } = useParams()
  const api = useApi()
  const { session } = useSession()
  const [portfolio, setPortfolio] = useState<Portfolio>()
  const [positionsData, setPositions] = useState<Position[]>([])
  useEffect(() => { if (id) { api.getPortfolio(id).then(setPortfolio); api.listPositions(id).then(setPositions) } }, [api, id])
  if (!portfolio) return <Loading />
  return <><PageHeader eyebrow="PORTFOLIO DETAIL · POSMAP" title={portfolio.portfolioId} description={`${portfolio.clientName} · ${portfolio.accountNo}`} action={<div className="button-row">{session?.role === 'admin' && <Link className="button secondary" to={`/portfolios/${id}/edit`}>Edit portfolio</Link>}<Link className="button ghost" to="/portfolios">← Back</Link></div>} /><div className="detail-grid"><section className="panel"><div className="panel-heading"><div><span className="eyebrow">PORTFOLIO MASTER</span><h2>Account profile</h2></div><StatusBadge status={portfolio.status} /></div><dl className="detail-list"><Detail label="Account number" value={portfolio.accountNo} mono /><Detail label="Client name" value={portfolio.clientName} /><Detail label="Client type" value={portfolio.clientType === 'I' ? 'Individual' : portfolio.clientType === 'C' ? 'Corporate' : 'Trust'} /><Detail label="Created" value={prettyDate(portfolio.createDate)} /><Detail label="Last maintained" value={prettyDate(portfolio.lastMaint)} /><Detail label="Last user" value={portfolio.lastUser} mono /></dl></section><section className="panel highlight-panel"><span className="eyebrow">PORTFOLIO VALUE</span><strong>{formatMoney(portfolio.totalValue)}</strong><div className="value-row"><span>Cash balance</span><b>{formatMoney(portfolio.cashBalance)}</b></div><div className="progress"><span style={{ width: `${Math.min(portfolio.cashBalance / portfolio.totalValue * 100, 100)}%` }} /></div><small>{(portfolio.cashBalance / portfolio.totalValue * 100).toFixed(1)}% held in cash</small></section></div><section className="panel table-panel"><div className="panel-heading"><div><span className="eyebrow">CURRENT HOLDINGS · POSREC</span><h2>Positions</h2></div><span className="muted">{positionsData.length} investments</span></div><div className="table-wrap"><table><thead><tr><th>Fund ID</th><th>Fund name</th><th>Units</th><th>Cost basis</th><th>Market value</th><th>Status</th></tr></thead><tbody>{positionsData.map((position) => <tr key={position.investmentId}><td className="table-primary mono">{position.investmentId}</td><td>{position.fundName}</td><td>{position.quantity.toLocaleString()}</td><td className="money">{formatMoney(position.costBasis)}</td><td className="money">{formatMoney(position.marketValue)}</td><td><StatusBadge status={position.status} /></td></tr>)}</tbody></table></div></section></>
}
function Detail({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) { return <div><dt>{label}</dt><dd className={mono ? 'mono' : ''}>{value}</dd></div> }
function Loading() { return <div className="loading">Loading workspace<span>•••</span></div> }

function PortfolioForm() {
  const { id } = useParams()
  const { session } = useSession()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const api = useApi()
  const notify = useToast()
  const [form, setForm] = useState<Portfolio>({ portfolioId: id || '', accountNo: '', clientName: '', clientType: 'I', createDate: '20260214', lastMaint: '20260214', status: 'A', totalValue: 0, cashBalance: 0, lastUser: 'ADMIN001', lastTransDate: '20260214' })
  const [error, setError] = useState('')
  useEffect(() => { if (id) api.getPortfolio(id).then((item) => item && setForm(item)) }, [api, id])
  if (session?.role !== 'admin') return <Navigate to={id ? `/portfolios/${id}` : '/portfolios'} replace />
  const update = (key: keyof Portfolio, value: string | number) => setForm((previous) => ({ ...previous, [key]: value }))
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const checks = [validatePortfolioId(form.portfolioId), validateAccountNumber(form.accountNo), validateAmount(form.totalValue)]
    const failed = checks.find((result) => result.code)
    if (!form.clientName.trim()) { setError('Client name is required.'); return }
    if (failed) { setError(`${failed.code === 1 ? 'PORT-VAL-001' : failed.code === 2 ? 'PORT-VAL-002' : 'PORT-VAL-004'} · ${failed.message}`); return }
    await api.savePortfolio(form); notify({ kind: 'success', message: `${form.portfolioId} ${isEdit ? 'updated' : 'created'} successfully.` }); navigate(`/portfolios/${form.portfolioId}`)
  }
  return <><PageHeader eyebrow={isEdit ? 'PORTFOLIO MAINTENANCE · UPDATE' : 'PORTFOLIO MAINTENANCE · CREATE'} title={isEdit ? `Edit ${id}` : 'Add portfolio'} description="Portfolio master fields follow the PORTFLIO copybook." action={<Link className="button ghost" to={isEdit ? `/portfolios/${id}` : '/portfolios'}>Cancel</Link>} /><form className="panel form-panel" onSubmit={submit}>{error && <ErrorBanner code="ERR-PORTVAL" message={error} />}<div className="form-section"><span className="eyebrow">IDENTIFICATION</span><div className="form-grid"><Field label="Portfolio ID" value={form.portfolioId} onChange={(v) => update('portfolioId', v)} placeholder="PORT0001" disabled={isEdit} /><Field label="Account number" value={form.accountNo} onChange={(v) => update('accountNo', v)} placeholder="10 digits" /><Field label="Client name" value={form.clientName} onChange={(v) => update('clientName', v)} /><label>Client type<select value={form.clientType} onChange={(e) => update('clientType', e.target.value)}><option value="I">Individual</option><option value="C">Corporate</option><option value="T">Trust</option></select></label></div></div><div className="form-section"><span className="eyebrow">FINANCIAL PROFILE</span><div className="form-grid"><Field label="Total value" type="number" value={String(form.totalValue)} onChange={(v) => update('totalValue', Number(v))} /><Field label="Cash balance" type="number" value={String(form.cashBalance)} onChange={(v) => update('cashBalance', Number(v))} /><label>Status<select value={form.status} onChange={(e) => update('status', e.target.value)}><option value="A">Active</option><option value="S">Suspended</option><option value="C">Closed</option></select></label></div></div><div className="form-actions"><Link className="button ghost" to={isEdit ? `/portfolios/${id}` : '/portfolios'}>Cancel</Link><button className="button primary" type="submit">{isEdit ? 'Save changes' : 'Create portfolio'} <span>→</span></button></div></form></>
}
function Field({ label, value, onChange, placeholder, type = 'text', disabled = false }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string; type?: string; disabled?: boolean }) { return <label>{label}<input type={type} value={value} placeholder={placeholder} disabled={disabled} onChange={(event) => onChange(event.target.value)} /></label> }

function PositionInquiry() {
  const api = useApi()
  const [query, setQuery] = useState('')
  const [submitted, setSubmitted] = useState('')
  const [data, setData] = useState<Position[]>([])
  const [portfolioData, setPortfolioData] = useState<Portfolio[]>([])
  const [page, setPage] = useState(0)
  useEffect(() => { api.listPortfolios().then(setPortfolioData); api.listPositions().then(setData) }, [api])
  const matchedData = submitted && /^\d{10}$/.test(submitted)
    ? data.filter((position) => portfolioData.find((item) => item.portfolioId === position.portfolioId)?.accountNo === submitted)
    : data.filter((position) => !submitted || position.portfolioId === submitted)
  const pageData = matchedData.slice(page * 10, page * 10 + 10)
  return <><PageHeader eyebrow="ONLINE INQUIRY · INQONLN" title="Position inquiry" description="Review holdings by account, with PF7 / PF8 pagination." /><section className="panel inquiry-panel"><form className="inline-search" onSubmit={(event) => { event.preventDefault(); setSubmitted(query.toUpperCase()); setPage(0) }}><label>Account or portfolio ID<div className="input-with-button"><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="PORT0001 or account number" /><button className="button primary" type="submit">Search</button></div></label></form>{submitted && !matchedData.length && <ErrorBanner code="ERR-INQ-04" message="No positions found for this account." />}<div className="table-wrap"><table><thead><tr><th>Portfolio</th><th>Fund ID</th><th>Fund name</th><th>Units</th><th>Cost basis</th><th>Market value</th><th>Status</th></tr></thead><tbody>{pageData.map((position) => <tr key={`${position.portfolioId}-${position.investmentId}`}><td><Link className="table-primary" to={`/portfolios/${position.portfolioId}`}>{position.portfolioId}</Link></td><td className="mono">{position.investmentId}</td><td>{position.fundName}</td><td>{position.quantity.toLocaleString()}</td><td className="money">{formatMoney(position.costBasis)}</td><td className="money">{formatMoney(position.marketValue)}</td><td><StatusBadge status={position.status} /></td></tr>)}</tbody></table></div><Pagination page={page} count={matchedData.length} onChange={setPage} /></section></>
}

function TransactionHistory() {
  const api = useApi()
  const { session } = useSession()
  const [query, setQuery] = useState('')
  const [submitted, setSubmitted] = useState('')
  const [from, setFrom] = useState('2024-01-01')
  const [to, setTo] = useState('2026-12-31')
  const [data, setData] = useState<Transaction[]>([])
  const [portfolioData, setPortfolioData] = useState<Portfolio[]>([])
  const [page, setPage] = useState(0)
  useEffect(() => { api.listTransactions().then(setData); api.listPortfolios().then(setPortfolioData) }, [api])
  const searched = data.filter((item) => !submitted || item.portfolioId === submitted || portfolioData.find((portfolio) => portfolio.portfolioId === item.portfolioId)?.accountNo === submitted)
  const filtered = searched.filter((item) => { const value = `${item.date.slice(0, 4)}-${item.date.slice(4, 6)}-${item.date.slice(6)}`; return value >= from && value <= to })
  return <><PageHeader eyebrow="TRANSACTION LEDGER · TRNHIST" title="Transaction history" description="Search completed and pending activity across your portfolios." action={session?.role === 'admin' && <Link className="button primary" to="/transactions/new">＋ Enter transaction</Link>} /><section className="panel table-panel"><div className="toolbar"><form className="search-box" onSubmit={(event) => { event.preventDefault(); setSubmitted(query.toUpperCase()); setPage(0) }}><span>⌕</span><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search account or portfolio..." /></form><div className="date-filter"><label>From<input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0) }} /></label><span>—</span><label>To<input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0) }} /></label></div></div><div className="table-wrap"><table><thead><tr><th>Date</th><th>Account</th><th>Fund</th><th>Type</th><th>Units</th><th>Price</th><th>Amount</th><th>Status</th></tr></thead><tbody>{filtered.slice(page * 10, page * 10 + 10).map((item) => <tr key={item.sequenceNo}><td>{prettyDate(item.date)}</td><td><Link className="table-primary" to={`/portfolios/${item.portfolioId}`}>{item.portfolioId}</Link></td><td className="mono">{item.investmentId}</td><td><span className={`type-${item.type.toLowerCase()}`}>{typeLabel(item.type)}</span></td><td>{item.quantity.toLocaleString()}</td><td>{formatMoney(item.price)}</td><td className="money">{formatMoney(item.amount)}</td><td><StatusBadge status={item.status} /></td></tr>)}</tbody></table></div><Pagination page={page} count={filtered.length} onChange={setPage} /></section></>
}
function Pagination({ page, count, onChange }: { page: number; count: number; onChange: (page: number) => void }) { const pages = Math.max(1, Math.ceil(count / 10)); return <div className="pagination"><span>Showing {count ? page * 10 + 1 : 0}–{Math.min((page + 1) * 10, count)} of {count}</span><div><button className="button small ghost" disabled={page === 0} onClick={() => onChange(page - 1)}>← Prev</button><span className="page-number">{page + 1} / {pages}</span><button className="button small ghost" disabled={page >= pages - 1} onClick={() => onChange(page + 1)}>Next →</button></div></div> }

function TransactionEntry() {
  const { session } = useSession()
  const api = useApi()
  const notify = useToast()
  const navigate = useNavigate()
  const [portfolioId, setPortfolioId] = useState('PORT0001')
  const [investmentId, setInvestmentId] = useState('AAPL')
  const [investmentType, setInvestmentType] = useState('STK')
  const [type, setType] = useState<Transaction['type']>('BU')
  const [quantity, setQuantity] = useState('25')
  const [price, setPrice] = useState('185')
  const [error, setError] = useState('')
  if (session?.role !== 'admin') return <Navigate to="/transactions" replace />
  const amount = Number(quantity || 0) * Number(price || 0)
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const idCheck = validatePortfolioId(portfolioId)
    const typeCheck = validateInvestmentType(investmentType)
    const amountCheck = validateAmount(amount)
    if (idCheck.code) { setError(`TRN-VAL-001 · ${idCheck.message}`); return }
    if (typeCheck.code) { setError(`TRN-VAL-003 · ${typeCheck.message}`); return }
    if (amountCheck.code) { setError(`TRN-VAL-004 · ${amountCheck.message}`); return }
    await api.createTransaction({ date: '20260214', time: '150000', portfolioId, sequenceNo: String(Date.now()).slice(-6), investmentId: investmentId.toUpperCase(), type, quantity: Number(quantity), price: Number(price), amount, currency: 'USD', status: 'P' })
    notify({ kind: 'success', message: 'Transaction submitted and queued for processing.' }); navigate('/transactions')
  }
  return <><PageHeader eyebrow="TRANSACTION PROCESSING · TRNENTR" title="Enter transaction" description="Submit a trade for mock batch processing." action={<Link className="button ghost" to="/transactions">Cancel</Link>} /><form className="panel form-panel narrow-form" onSubmit={submit}>{error && <ErrorBanner code="ERR-TRNVAL" message={error} />}<div className="notice"><strong>Mock processing environment</strong><span>Transactions are appended to the in-memory ledger and marked Pending.</span></div><div className="form-section"><span className="eyebrow">TRADE DETAILS</span><div className="form-grid"><Field label="Portfolio ID" value={portfolioId} onChange={setPortfolioId} /><Field label="Investment ID" value={investmentId} onChange={setInvestmentId} placeholder="AAPL, VTI..." /><label>Investment type<select value={investmentType} onChange={(e) => setInvestmentType(e.target.value)}><option value="STK">Stock</option><option value="BND">Bond</option><option value="MMF">Money market</option><option value="ETF">ETF</option></select></label><label>Transaction type<select value={type} onChange={(e) => setType(e.target.value as Transaction['type'])}><option value="BU">BUY</option><option value="SL">SELL</option><option value="TR">TRANSFER</option></select></label><Field label="Units" type="number" value={quantity} onChange={setQuantity} /><Field label="Price per unit" type="number" value={price} onChange={setPrice} /></div></div><div className="computed-amount"><span>Computed transaction amount</span><strong>{formatMoney(amount)}</strong></div><div className="form-actions"><Link className="button ghost" to="/transactions">Cancel</Link><button className="button primary" type="submit">Submit transaction <span>→</span></button></div></form></>
}

function Reports() {
  const api = useApi()
  const [active, setActive] = useState('Position report')
  const [data, setData] = useState<HistoryEntry[]>([])
  useEffect(() => { api.listHistory().then(setData) }, [api])
  const reports = ['Position report', 'Audit report', 'Statistics report', 'Return analysis']
  return <><PageHeader eyebrow="REPORTING CENTER · RPTMENU" title="Reports" description="Read-only operational reporting from the modernized data layer." /><div className="report-tabs">{reports.map((report) => <button key={report} className={active === report ? 'report-tab active' : 'report-tab'} onClick={() => setActive(report)}>{report}<span>→</span></button>)}</div><div className="stats-grid report-stats"><StatCard label="Reporting period" value="2026 YTD" detail="As of 14 Feb 2026" /><StatCard label="Records processed" value="12,846" detail="+8.2% vs prior period" tone="blue" /><StatCard label="Exceptions" value="03" detail="2 under review" tone="amber" /></div><section className="panel table-panel"><div className="panel-heading"><div><span className="eyebrow">{active.toUpperCase()} · {active === 'Position report' ? 'RPTPOS00' : active === 'Audit report' ? 'RPTAUD00' : active === 'Statistics report' ? 'RPTSTA00' : 'RTNANA00'}</span><h2>{active}</h2></div><button className="button secondary">Export CSV ↓</button></div>{active === 'Audit report' ? <AuditTable data={data} /> : active === 'Statistics report' ? <StatsTable /> : active === 'Return analysis' ? <ReturnTable /> : <PositionReport />}</section></>
}
function PositionReport() { return <div className="report-cards"><div className="report-summary"><strong>$1.43M</strong><span>Total market value</span></div><div className="report-summary"><strong>38</strong><span>Open positions</span></div><div className="report-summary"><strong>+9.4%</strong><span>Weighted return</span></div><div className="table-wrap"><table><thead><tr><th>Portfolio</th><th>Holdings</th><th>Market value</th><th>Unrealized gain</th><th>As of</th></tr></thead><tbody>{['PORT0001', 'PORT0002', 'PORT0003', 'PORT0004', 'PORT0005'].map((id, index) => <tr key={id}><td className="table-primary mono">{id}</td><td>{3 + index} funds</td><td className="money">{formatMoney(148300 + index * 28400)}</td><td className="positive">+{formatMoney(8200 + index * 1100)}</td><td>02/14/2026</td></tr>)}</tbody></table></div></div> }
function AuditTable({ data }: { data: HistoryEntry[] }) { return <div className="table-wrap"><table><thead><tr><th>Process date</th><th>Portfolio</th><th>Record</th><th>Action</th><th>Reason</th><th>User</th></tr></thead><tbody>{data.map((item) => <tr key={item.sequenceNo}><td>{prettyDate(item.date)}</td><td className="mono">{item.portfolioId}</td><td>{item.recordType === 'PT' ? 'Portfolio' : item.recordType === 'PS' ? 'Position' : 'Transaction'}</td><td><span className="action-pill">{item.actionCode === 'A' ? 'Added' : item.actionCode === 'C' ? 'Changed' : 'Deleted'}</span></td><td className="mono">{item.reasonCode}</td><td className="mono">{item.processUser}</td></tr>)}</tbody></table></div> }
function StatsTable() { return <div className="table-wrap"><table><thead><tr><th>Metric</th><th>Current</th><th>Target</th><th>Trend</th><th>Last measured</th></tr></thead><tbody>{[['Online response time', '142 ms', '< 250 ms', '↓ 12%', '14:45 UTC'], ['Batch throughput', '1,284 rec/min', '> 1,000', '↑ 8%', '04:17 UTC'], ['API availability', '99.98%', '> 99.9%', '↑ 0.02%', '14:45 UTC'], ['Exception rate', '0.14%', '< 0.5%', '↓ 0.08%', '14:45 UTC']].map((row) => <tr key={row[0]}>{row.map((value, index) => <td key={value} className={index === 0 ? 'table-primary' : index === 3 ? 'positive' : ''}>{value}</td>)}</tr>)}</tbody></table></div> }
function ReturnTable() { return <div className="table-wrap"><table><thead><tr><th>Portfolio</th><th>Period</th><th>Starting value</th><th>Ending value</th><th>Total return</th></tr></thead><tbody>{['PORT0001', 'PORT0002', 'PORT0003', 'PORT0004', 'PORT0005'].map((id, index) => <tr key={id}><td className="table-primary mono">{id}</td><td>YTD 2026</td><td>{formatMoney(120000 + index * 10000)}</td><td>{formatMoney(131300 + index * 11600)}</td><td className="positive">+{(7.8 + index * 0.6).toFixed(1)}%</td></tr>)}</tbody></table></div> }

function SystemStatus() {
  const api = useApi()
  const [jobs, setJobs] = useState<{ name: string; schedule: string; status: string; lastRun: string; duration: string }[]>([])
  useEffect(() => { api.getSystemJobs().then(setJobs) }, [api])
  return <><PageHeader eyebrow="SYSTEM MONITOR · UTLMON00" title="System status" description="Live health indicators for batch processing and online services." action={<span className="refresh-label"><span className="live-dot" /> Refreshed just now</span>} /><div className="stats-grid"><StatCard label="Overall health" value="99.98%" detail="All critical services online" tone="blue" /><StatCard label="Batch jobs today" value="04 / 04" detail="Last completed 04:17 UTC" /><StatCard label="Online response" value="142 ms" detail="Within service target" /><StatCard label="Open exceptions" value="03" detail="2 low priority" tone="amber" /></div><section className="panel table-panel"><div className="panel-heading"><div><span className="eyebrow">BATCH CONTROL · JOBMON</span><h2>Scheduled jobs</h2></div><StatusBadge status="Healthy" /></div><div className="table-wrap"><table><thead><tr><th>Job / service</th><th>Schedule</th><th>Last run</th><th>Duration</th><th>Status</th></tr></thead><tbody>{jobs.map((job) => <tr key={job.name}><td className="table-primary">{job.name}</td><td>{job.schedule}</td><td className="mono">{job.lastRun}</td><td>{job.duration}</td><td><StatusBadge status={job.status} /></td></tr>)}</tbody></table></div></section><div className="health-note"><span className="health-icon">✓</span><div><strong>No critical incidents detected</strong><p>All COBOL modernization services are responding within their configured thresholds.</p></div></div></>
}

export default App
