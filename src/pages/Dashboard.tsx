import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm, FormProvider } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { PieChart, Pie, Cell, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { ROUTES } from '../types/routes';
import { Container, PageHeader, Card, Button, Alert } from '../components';
import { AccountInput } from '../components/AccountInput';
import { LoadingButton } from '../components';
import { accountFormSchema, type AccountFormData } from '../types/account';
import { fetchDashboard, ApiError } from '../services/api';
import { formatCurrency, formatPercentage, getGainLossColorClass, TRANSACTION_TYPE_LABELS } from '../utils/format';
import type { PortfolioSummary, Transaction } from '../types';

interface DashboardData {
  portfolio: PortfolioSummary;
  transactions: Transaction[];
  allocationData: { name: string; value: number; color: string }[];
  performanceData: { month: string; value: number }[];
}

export default function Dashboard() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  const methods = useForm<AccountFormData>({
    resolver: zodResolver(accountFormSchema),
    mode: 'onChange',
    defaultValues: { accountNumber: '' },
  });

  const { handleSubmit, formState: { isValid } } = methods;

  const onSubmit = async (data: AccountFormData) => {
    setIsSubmitting(true);
    setError(null);
    try {
      const result = await fetchDashboard(data.accountNumber);
      setDashboardData({
        portfolio: result.portfolio,
        transactions: result.transactions,
        allocationData: result.allocationData,
        performanceData: result.performanceData,
      });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('An unexpected error occurred.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setDashboardData(null);
    setError(null);
    methods.reset();
  };

  if (dashboardData) {
    const { portfolio, transactions, allocationData, performanceData } = dashboardData;
    const gainLossColor = getGainLossColorClass(portfolio.totalGainLoss);

    return (
      <div className="min-h-screen bg-background py-8">
        <Container size="xl">
          <div className="space-y-8">
            <div className="flex items-center justify-between">
              <Link to={ROUTES.MAIN_MENU}>
                <Button variant="secondary" size="sm">&larr; Main Menu</Button>
              </Link>
              <div className="flex gap-2">
                <Link to={`${ROUTES.PORTFOLIO_INQUIRY}`}>
                  <Button variant="outline" size="sm">Portfolio Inquiry</Button>
                </Link>
                <Button variant="ghost" size="sm" onClick={resetForm}>New Search</Button>
              </div>
            </div>

            <PageHeader
              title="Portfolio Dashboard"
              subtitle={`Account: ${portfolio.accountNumber}`}
            />

            {/* KPI Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-slide-up">
              <Card padding="md">
                <p className="text-sm text-muted-foreground">Total Value</p>
                <p className="text-2xl font-bold">{formatCurrency(portfolio.totalValue)}</p>
              </Card>
              <Card padding="md">
                <p className="text-sm text-muted-foreground">Total Gain/Loss</p>
                <p className={`text-2xl font-bold ${gainLossColor}`}>{formatCurrency(portfolio.totalGainLoss)}</p>
              </Card>
              <Card padding="md">
                <p className="text-sm text-muted-foreground">Return %</p>
                <p className={`text-2xl font-bold ${gainLossColor}`}>{formatPercentage(portfolio.totalGainLossPercent)}</p>
              </Card>
              <Card padding="md">
                <p className="text-sm text-muted-foreground">Holdings</p>
                <p className="text-2xl font-bold">{portfolio.holdings.length}</p>
              </Card>
            </div>

            {/* Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 animate-fade-in" style={{ animationDelay: '100ms' }}>
              {/* Allocation Pie */}
              <Card>
                <h3 className="text-lg font-semibold mb-4">Asset Allocation</h3>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={allocationData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={110}
                      paddingAngle={3}
                      dataKey="value"
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    >
                      {allocationData.map((entry, idx) => (
                        <Cell key={idx} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(val: number) => formatCurrency(val)} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </Card>

              {/* Performance Area */}
              <Card>
                <h3 className="text-lg font-semibold mb-4">Portfolio Performance</h3>
                <ResponsiveContainer width="100%" height={300}>
                  <AreaChart data={performanceData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`} />
                    <Tooltip formatter={(val: number) => formatCurrency(val)} />
                    <Area type="monotone" dataKey="value" stroke="#06402B" fill="#06402B" fillOpacity={0.15} strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
              </Card>
            </div>

            {/* Holdings Table */}
            <Card className="animate-fade-in" style={{ animationDelay: '200ms' }}>
              <h3 className="text-lg font-semibold mb-4">Holdings Detail</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-muted/50">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium text-muted-foreground">Symbol</th>
                      <th className="px-4 py-3 text-left font-medium text-muted-foreground">Name</th>
                      <th className="px-4 py-3 text-right font-medium text-muted-foreground">Shares</th>
                      <th className="px-4 py-3 text-right font-medium text-muted-foreground">Price</th>
                      <th className="px-4 py-3 text-right font-medium text-muted-foreground">Market Value</th>
                      <th className="px-4 py-3 text-right font-medium text-muted-foreground">Gain/Loss</th>
                      <th className="px-4 py-3 text-right font-medium text-muted-foreground">Return</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {portfolio.holdings.map((h) => (
                      <tr key={h.symbol} className="hover:bg-muted/30 transition-fast">
                        <td className="px-4 py-3 font-semibold">{h.symbol}</td>
                        <td className="px-4 py-3">{h.name}</td>
                        <td className="px-4 py-3 text-right tabular-nums">{h.shares.toLocaleString()}</td>
                        <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(h.currentPrice)}</td>
                        <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(h.marketValue)}</td>
                        <td className={`px-4 py-3 text-right tabular-nums font-medium ${getGainLossColorClass(h.gainLoss)}`}>
                          {formatCurrency(h.gainLoss)}
                        </td>
                        <td className={`px-4 py-3 text-right tabular-nums ${getGainLossColorClass(h.gainLossPercent)}`}>
                          {formatPercentage(h.gainLossPercent)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>

            {/* Recent Transactions */}
            {transactions.length > 0 && (
              <Card className="animate-fade-in" style={{ animationDelay: '300ms' }}>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold">Recent Transactions</h3>
                  <Link to={`${ROUTES.TRANSACTION_HISTORY}?account=${portfolio.accountNumber}`}>
                    <Button variant="ghost" size="sm">View All &rarr;</Button>
                  </Link>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="bg-muted/50">
                      <tr>
                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">Date</th>
                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">Type</th>
                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">Investment</th>
                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">Qty</th>
                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">Amount</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {transactions.slice(0, 5).map((tx) => (
                        <tr key={`${tx.date}-${tx.sequenceNo}`} className="hover:bg-muted/30 transition-fast">
                          <td className="px-4 py-3">{tx.date}</td>
                          <td className="px-4 py-3">
                            <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${
                              tx.type === 'BU' ? 'bg-blue-100 text-blue-800' : 'bg-orange-100 text-orange-800'
                            }`}>
                              {TRANSACTION_TYPE_LABELS[tx.type] || tx.type}
                            </span>
                          </td>
                          <td className="px-4 py-3 font-medium">{tx.investmentId}</td>
                          <td className="px-4 py-3 text-right tabular-nums">{tx.quantity.toLocaleString()}</td>
                          <td className="px-4 py-3 text-right tabular-nums font-medium">{formatCurrency(tx.amount)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Card>
            )}
          </div>
        </Container>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <Container size="sm">
        <div className="space-y-8">
          <div className="flex items-center justify-between">
            <Link to={ROUTES.MAIN_MENU}>
              <Button variant="secondary" size="sm">&larr; Back to Main Menu</Button>
            </Link>
          </div>
          <PageHeader
            title="Portfolio Dashboard"
            subtitle="Enter an account number to view portfolio analytics and charts"
          />

          <main className="space-y-6 animate-slide-up">
            {error && (
              <Alert variant="destructive" className="animate-fade-in">{error}</Alert>
            )}

            <FormProvider {...methods}>
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                <div className="bg-card border border-border rounded-lg p-6 shadow-sm">
                  <AccountInput />
                  <div className="mt-4">
                    <LoadingButton type="submit" loading={isSubmitting} disabled={!isValid} size="lg" className="w-full">
                      Load Dashboard
                    </LoadingButton>
                  </div>
                  <p className="mt-3 text-xs text-muted-foreground text-center">
                    Try account: <code className="px-1.5 py-0.5 bg-muted rounded text-xs">1234567890</code>
                  </p>
                </div>
              </form>
            </FormProvider>
          </main>
        </div>
      </Container>
    </div>
  );
}
