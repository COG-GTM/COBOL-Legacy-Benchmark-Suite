import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ROUTES } from '../types/routes';
import { Container, PageHeader, Card, Button, SkeletonLoader, Alert } from '../components';
import { fetchTransactions, ApiError } from '../services/api';
import type { Transaction } from '../types';
import { formatCurrency, TRANSACTION_TYPE_LABELS, TRANSACTION_STATUS_LABELS } from '../utils/format';

export default function TransactionHistory() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [transactionData, setTransactionData] = useState<{
    accountNumber: string;
    transactions: Transaction[];
    message: string;
  } | null>(null);
  const location = useLocation();

  useEffect(() => {
    const loadTransactions = async () => {
      const urlParams = new URLSearchParams(location.search);
      const accountNumber = urlParams.get('account');

      if (!accountNumber) {
        setLoading(false);
        return;
      }

      try {
        const data = await fetchTransactions(accountNumber);
        setTransactionData(data);
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.message);
        } else {
          setError('An unexpected error occurred while loading transactions.');
        }
      } finally {
        setLoading(false);
      }
    };

    loadTransactions();
  }, [location.search]);

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'D': return 'bg-green-100 text-green-800';
      case 'P': return 'bg-yellow-100 text-yellow-800';
      case 'F': return 'bg-red-100 text-red-800';
      case 'R': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getTypeBadgeClass = (type: string) => {
    switch (type) {
      case 'BU': return 'bg-blue-100 text-blue-800';
      case 'SL': return 'bg-orange-100 text-orange-800';
      case 'TR': return 'bg-purple-100 text-purple-800';
      case 'FE': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="min-h-screen bg-background py-8">
      <Container size="md">
        <div className="space-y-8">
          <div className="flex items-center justify-between">
            <Link to={ROUTES.MAIN_MENU}>
              <Button variant="secondary" size="sm">
                &larr; Back to Main Menu
              </Button>
            </Link>
          </div>
          <PageHeader
            title="Transaction History"
            subtitle="Review your investment transaction activity"
          />

          <main className="space-y-6 animate-slide-up">
            {error && (
              <Alert variant="destructive" className="animate-fade-in">
                {error}
              </Alert>
            )}

            {loading ? (
              <Card>
                <div className="space-y-4">
                  <SkeletonLoader lines={3} />
                  <SkeletonLoader lines={1} height="h-2" />
                </div>
              </Card>
            ) : transactionData && transactionData.transactions.length > 0 ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-semibold">
                    Transactions for Account {transactionData.accountNumber}
                  </h2>
                  <span className="text-sm text-muted-foreground">
                    {transactionData.transactions.length} transaction{transactionData.transactions.length !== 1 ? 's' : ''}
                  </span>
                </div>

                <div className="overflow-x-auto rounded-lg border border-border">
                  <table className="w-full text-sm">
                    <thead className="bg-muted/50">
                      <tr>
                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">Date</th>
                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">Type</th>
                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">Investment</th>
                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">Qty</th>
                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">Price</th>
                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">Amount</th>
                        <th className="px-4 py-3 text-center font-medium text-muted-foreground">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {transactionData.transactions.map((tx, index) => (
                        <tr key={`${tx.date}-${tx.sequenceNo}`} className="hover:bg-muted/30 transition-fast animate-fade-in" style={{ animationDelay: `${index * 50}ms` }}>
                          <td className="px-4 py-3 whitespace-nowrap">{tx.date}</td>
                          <td className="px-4 py-3">
                            <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${getTypeBadgeClass(tx.type)}`}>
                              {TRANSACTION_TYPE_LABELS[tx.type] || tx.type}
                            </span>
                          </td>
                          <td className="px-4 py-3 font-medium">{tx.investmentId}</td>
                          <td className="px-4 py-3 text-right tabular-nums">{tx.quantity.toLocaleString()}</td>
                          <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(tx.price)}</td>
                          <td className="px-4 py-3 text-right tabular-nums font-medium">{formatCurrency(tx.amount)}</td>
                          <td className="px-4 py-3 text-center">
                            <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${getStatusBadgeClass(tx.status)}`}>
                              {TRANSACTION_STATUS_LABELS[tx.status] || tx.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : transactionData ? (
              <Card>
                <h2 className="text-2xl font-semibold mb-4">
                  Transactions for Account {transactionData.accountNumber}
                </h2>
                <p className="text-muted-foreground mb-4">{transactionData.message}</p>
                <p className="text-sm text-muted-foreground">No transactions found for this account.</p>
              </Card>
            ) : (
              <Card>
                <h2 className="text-2xl font-semibold mb-4">Recent Transactions</h2>
                <p className="text-muted-foreground mb-4">
                  To view transactions, navigate here from a portfolio inquiry or provide an account number in the URL.
                </p>
                <p className="text-sm text-muted-foreground">
                  Try: <code className="px-1.5 py-0.5 bg-muted rounded text-xs">/transaction-history?account=1234567890</code>
                </p>
              </Card>
            )}
          </main>
        </div>
      </Container>
    </div>
  );
}
