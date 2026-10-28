import { ArrowLeft, ArrowLeftRight, PieChart, SearchX } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'

import { PageHeader } from '@/components/PageHeader'
import { PortfolioStatusBadge } from '@/components/PortfolioStatusBadge'
import { Badge, type BadgeProps } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { mockPortfolios, mockPositions, mockTransactions } from '@/data/mockData'
import { formatCents, formatCurrency, formatDate, formatQuantity } from '@/lib/format'
import {
  CLIENT_TYPE_LABELS,
  POSITION_STATUS_LABELS,
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_TYPE_LABELS,
} from '@/lib/legacyCodes'
import { portfolioPositionsPath, portfolioTransactionsPath } from '@/lib/links'
import { toCents } from '@/lib/money'
import { cn } from '@/lib/utils'
import { buildPortfolioDetail, normalizePortfolioId } from '@/services/portfolioDetailService'
import type { PositionStatus } from '@/types/position'
import type { TransactionStatus } from '@/types/transaction'

const POSITION_STATUS_BADGE: Record<PositionStatus, BadgeProps['variant']> = {
  A: 'success',
  P: 'warning',
  C: 'secondary',
}

const TRANSACTION_STATUS_BADGE: Record<TransactionStatus, BadgeProps['variant']> = {
  D: 'success',
  P: 'warning',
  F: 'destructive',
  R: 'secondary',
}

interface Field {
  label: string
  /** Copybook field the value comes from. */
  source: string
  value: ReactNode
  mono?: boolean
  numeric?: boolean
}

function FieldSection({ title, source, fields }: { title: string; source: string; fields: Field[] }) {
  const headingId = `section-${source.toLowerCase()}`
  return (
    <Card role="region" aria-labelledby={headingId}>
      <CardHeader className="pb-3">
        <CardTitle id={headingId} className="text-base">
          {title}
        </CardTitle>
        <CardDescription className="font-mono text-xs">{source}</CardDescription>
      </CardHeader>
      <CardContent>
        <dl className="divide-y">
          {fields.map((field) => (
            <div key={field.source} className="flex items-center justify-between gap-4 py-2 first:pt-0 last:pb-0">
              <dt className="text-sm text-muted-foreground">
                {field.label}
                <span className="mt-0.5 hidden font-mono text-[10px] uppercase leading-none text-muted-foreground/70 sm:block">
                  {field.source}
                </span>
              </dt>
              <dd
                data-field={field.source}
                className={cn(
                  'text-right text-sm font-medium',
                  field.mono && 'font-mono',
                  field.numeric && 'tabular-nums',
                )}
              >
                {field.value}
              </dd>
            </div>
          ))}
        </dl>
      </CardContent>
    </Card>
  )
}

function SignedAmount({ cents }: { cents: bigint }) {
  const tone = cents > 0n ? 'text-emerald-700' : cents < 0n ? 'text-red-700' : 'text-muted-foreground'
  return (
    <span className={tone}>
      {cents > 0n ? '+' : ''}
      {formatCents(cents)}
    </span>
  )
}

function PortfolioNotFound({ portfolioId }: { portfolioId: string }) {
  return (
    <>
      <PageHeader title="Portfolio not found" description={`No portfolio has the ID ${portfolioId}.`} />
      <Card className="max-w-xl">
        <CardContent className="flex items-start gap-3 pt-6">
          <SearchX className="mt-0.5 h-5 w-5 shrink-0 text-muted-foreground" aria-hidden="true" />
          <p className="text-sm text-muted-foreground">
            Check the portfolio ID and try again. Portfolio IDs are 8 characters, for example{' '}
            <span className="font-mono">PORT0001</span>.
          </p>
        </CardContent>
      </Card>
      <Button asChild variant="outline" className="mt-4">
        <Link to="/portfolios">
          <ArrowLeft /> Back to portfolios
        </Link>
      </Button>
    </>
  )
}

export function PortfolioDetailPage() {
  const { portfolioId = '' } = useParams()
  const detail = buildPortfolioDetail(portfolioId, mockPortfolios, mockPositions, mockTransactions)
  if (!detail) return <PortfolioNotFound portfolioId={normalizePortfolioId(portfolioId)} />

  const { portfolio, positions, recentTransactions } = detail
  const unrealizedCents = detail.positionsMarketValueCents - detail.positionsCostBasisCents

  return (
    <>
      <Button asChild variant="ghost" size="sm" className="-ml-3 mb-2 text-muted-foreground">
        <Link to="/portfolios">
          <ArrowLeft /> Portfolios
        </Link>
      </Button>
      <PageHeader
        title={portfolio.clientName}
        description={`Portfolio ${portfolio.portfolioId} · Account ${portfolio.accountNo}`}
      >
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Status</span>
          <PortfolioStatusBadge status={portfolio.status} className="text-sm" />
        </div>
      </PageHeader>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <FieldSection
          title="Key information"
          source="PORT-KEY"
          fields={[
            { label: 'Portfolio ID', source: 'PORT-ID', value: portfolio.portfolioId, mono: true },
            { label: 'Account number', source: 'PORT-ACCOUNT-NO', value: portfolio.accountNo, mono: true },
          ]}
        />
        <FieldSection
          title="Client information"
          source="PORT-CLIENT-INFO"
          fields={[
            { label: 'Client name', source: 'PORT-CLIENT-NAME', value: portfolio.clientName },
            { label: 'Client type', source: 'PORT-CLIENT-TYPE', value: CLIENT_TYPE_LABELS[portfolio.clientType] },
          ]}
        />
        <FieldSection
          title="Portfolio information"
          source="PORT-PORTFOLIO-INFO"
          fields={[
            { label: 'Created', source: 'PORT-CREATE-DATE', value: formatDate(portfolio.createDate) },
            { label: 'Last maintained', source: 'PORT-LAST-MAINT', value: formatDate(portfolio.lastMaint) },
            { label: 'Status', source: 'PORT-STATUS', value: <PortfolioStatusBadge status={portfolio.status} /> },
          ]}
        />
        <FieldSection
          title="Financial information"
          source="PORT-FINANCIAL-INFO"
          fields={[
            {
              label: 'Total value',
              source: 'PORT-TOTAL-VALUE',
              value: formatCurrency(portfolio.totalValue),
              numeric: true,
            },
            {
              label: 'Cash balance',
              source: 'PORT-CASH-BALANCE',
              value: formatCurrency(portfolio.cashBalance),
              numeric: true,
            },
            {
              label: 'Invested',
              source: 'TOTAL − CASH',
              value: formatCents(detail.investedValueCents),
              numeric: true,
            },
          ]}
        />
        <FieldSection
          title="Audit information"
          source="PORT-AUDIT-INFO"
          fields={[
            { label: 'Last updated by', source: 'PORT-LAST-USER', value: portfolio.lastUser, mono: true },
            { label: 'Last transaction', source: 'PORT-LAST-TRANS', value: formatDate(portfolio.lastTrans) },
          ]}
        />
      </div>

      <div className="mt-6 grid gap-6 2xl:grid-cols-2">
        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
            <div className="space-y-1.5">
              <CardTitle className="text-base">Positions</CardTitle>
              <CardDescription>
                {positions.length} holding{positions.length === 1 ? '' : 's'} · market value{' '}
                {formatCents(detail.positionsMarketValueCents)}
              </CardDescription>
            </div>
            <Button asChild variant="outline" size="sm">
              <Link to={portfolioPositionsPath(portfolio.portfolioId)}>
                <PieChart /> View positions
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {positions.length === 0 ? (
              <p className="text-sm text-muted-foreground">No positions held.</p>
            ) : (
              <Table aria-label="Positions">
                <TableHeader>
                  <TableRow>
                    <TableHead>Investment</TableHead>
                    <TableHead className="text-right">Quantity</TableHead>
                    <TableHead className="hidden text-right sm:table-cell">Cost basis</TableHead>
                    <TableHead className="text-right">Market value</TableHead>
                    <TableHead className="hidden text-right md:table-cell">Gain / loss</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {positions.map((pos) => (
                    <TableRow key={`${pos.investmentId}-${pos.date}`}>
                      <TableCell className="font-mono text-xs">{pos.investmentId}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatQuantity(pos.quantity)}</TableCell>
                      <TableCell className="hidden whitespace-nowrap text-right tabular-nums sm:table-cell">
                        {formatCurrency(pos.costBasis, pos.currency)}
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-right tabular-nums">
                        {formatCurrency(pos.marketValue, pos.currency)}
                      </TableCell>
                      <TableCell className="hidden whitespace-nowrap text-right tabular-nums md:table-cell">
                        <SignedAmount cents={toCents(pos.marketValue) - toCents(pos.costBasis)} />
                      </TableCell>
                      <TableCell>
                        <Badge variant={POSITION_STATUS_BADGE[pos.status]}>{POSITION_STATUS_LABELS[pos.status]}</Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
            {positions.length > 0 && (
              <p className="mt-3 text-right text-xs text-muted-foreground">
                Unrealized gain / loss{' '}
                <span className="font-medium tabular-nums">
                  <SignedAmount cents={unrealizedCents} />
                </span>
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
            <div className="space-y-1.5">
              <CardTitle className="text-base">Recent transactions</CardTitle>
              <CardDescription>
                Latest {recentTransactions.length} of {detail.transactionCount} for this portfolio
              </CardDescription>
            </div>
            <Button asChild variant="outline" size="sm">
              <Link to={portfolioTransactionsPath(portfolio.portfolioId)}>
                <ArrowLeftRight /> View history
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {recentTransactions.length === 0 ? (
              <p className="text-sm text-muted-foreground">No transactions recorded.</p>
            ) : (
              <Table aria-label="Portfolio transactions">
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead className="hidden sm:table-cell">Investment</TableHead>
                    <TableHead className="text-right">Amount</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {recentTransactions.map((txn) => (
                    <TableRow key={txn.sequenceNo}>
                      <TableCell className="whitespace-nowrap">{formatDate(txn.date)}</TableCell>
                      <TableCell>{TRANSACTION_TYPE_LABELS[txn.type]}</TableCell>
                      <TableCell className="hidden font-mono text-xs sm:table-cell">{txn.investmentId}</TableCell>
                      <TableCell className="whitespace-nowrap text-right tabular-nums">
                        {formatCurrency(txn.amount, txn.currency)}
                      </TableCell>
                      <TableCell>
                        <Badge variant={TRANSACTION_STATUS_BADGE[txn.status]}>
                          {TRANSACTION_STATUS_LABELS[txn.status]}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>
    </>
  )
}
