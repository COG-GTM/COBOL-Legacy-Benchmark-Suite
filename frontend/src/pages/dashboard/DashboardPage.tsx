import { ArrowLeftRight, Briefcase, DollarSign, Wallet } from 'lucide-react'
import { Link } from 'react-router-dom'

import { PageHeader } from '@/components/PageHeader'
import { SummaryCard } from '@/components/SummaryCard'
import { Badge, type BadgeProps } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { MOCK_AS_OF_DATE, mockPortfolios, mockTransactions } from '@/data/mockData'
import { formatCurrency, formatDate, formatQuantity } from '@/lib/format'
import { TRANSACTION_STATUS_LABELS, TRANSACTION_TYPE_LABELS } from '@/lib/legacyCodes'
import { buildDashboardSummary, RECENT_WINDOW_DAYS } from '@/services/dashboardService'
import type { TransactionStatus } from '@/types/transaction'

import { MainMenuCard } from './MainMenuCard'

const STATUS_BADGE: Record<TransactionStatus, BadgeProps['variant']> = {
  D: 'success',
  P: 'warning',
  F: 'destructive',
  R: 'secondary',
}

export function DashboardPage() {
  const summary = buildDashboardSummary(mockPortfolios, mockTransactions, MOCK_AS_OF_DATE)
  const { A: active, S: suspended, C: closed } = summary.portfoliosByStatus

  return (
    <>
      <PageHeader title="Dashboard" description={`Summary as of ${formatDate(summary.asOf)} · mock data`} />

      <section aria-label="Summary" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Total portfolios"
          value={String(summary.totalPortfolios)}
          detail={`${active} active · ${suspended} suspended · ${closed} closed`}
          icon={Briefcase}
        />
        <SummaryCard
          title="Total value"
          value={formatCurrency(summary.totalValue)}
          detail="Across all portfolios"
          icon={DollarSign}
        />
        <SummaryCard
          title="Cash balance"
          value={formatCurrency(summary.totalCash)}
          detail="Uninvested cash across all portfolios"
          icon={Wallet}
        />
        <SummaryCard
          title="Recent transactions"
          value={String(summary.recentTransactionCount)}
          detail={`Last ${RECENT_WINDOW_DAYS} days · ${summary.pendingTransactionCount} pending`}
          icon={ArrowLeftRight}
        />
      </section>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
            <div className="space-y-1.5">
              <CardTitle className="text-base">Recent transactions</CardTitle>
              <CardDescription>Latest activity across all portfolios</CardDescription>
            </div>
            <Button asChild variant="outline" size="sm">
              <Link to="/transactions">View all</Link>
            </Button>
          </CardHeader>
          <CardContent>
            <Table aria-label="Recent transactions">
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Portfolio</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead className="hidden md:table-cell">Investment</TableHead>
                  <TableHead className="hidden text-right md:table-cell">Quantity</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {summary.recentTransactions.map((txn) => (
                  <TableRow key={`${txn.portfolioId}-${txn.sequenceNo}`}>
                    <TableCell className="whitespace-nowrap">{formatDate(txn.date)}</TableCell>
                    <TableCell className="font-mono text-xs">{txn.portfolioId}</TableCell>
                    <TableCell>{TRANSACTION_TYPE_LABELS[txn.type]}</TableCell>
                    <TableCell className="hidden font-mono text-xs md:table-cell">{txn.investmentId}</TableCell>
                    <TableCell className="hidden text-right tabular-nums md:table-cell">
                      {formatQuantity(txn.quantity)}
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-right tabular-nums">
                      {formatCurrency(txn.amount, txn.currency)}
                    </TableCell>
                    <TableCell>
                      <Badge variant={STATUS_BADGE[txn.status]}>{TRANSACTION_STATUS_LABELS[txn.status]}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <MainMenuCard />
      </div>
    </>
  )
}
