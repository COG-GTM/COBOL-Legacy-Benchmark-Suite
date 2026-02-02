import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Search, Play, Plus } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface TransactionsPageProps {
  token: string | null;
}

interface Transaction {
  id: number;
  transactionDate: string;
  portfolioId: string;
  investmentId: string;
  transactionType: string;
  quantity: number;
  price: number;
  amount: number;
  currency: string;
  status: string;
}

export default function TransactionsPage({ token }: TransactionsPageProps) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    fetchTransactions();
  }, [token]);

  const fetchTransactions = async () => {
    try {
      const response = await fetch(`${API_URL}/api/transactions`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setTransactions(data);
      }
    } catch (error) {
      console.error('Error fetching transactions:', error);
    } finally {
      setLoading(false);
    }
  };

  const processPending = async () => {
    setProcessing(true);
    try {
      const response = await fetch(`${API_URL}/api/transactions/process-pending`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        fetchTransactions();
      }
    } catch (error) {
      console.error('Error processing transactions:', error);
    } finally {
      setProcessing(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value || 0);
  };

  const getTypeBadge = (type: string) => {
    const variants: Record<string, string> = {
      BU: 'bg-green-100 text-green-800',
      SL: 'bg-red-100 text-red-800',
      TR: 'bg-blue-100 text-blue-800',
      FE: 'bg-yellow-100 text-yellow-800',
    };
    const labels: Record<string, string> = {
      BU: 'Buy',
      SL: 'Sell',
      TR: 'Transfer',
      FE: 'Fee',
    };
    return (
      <Badge className={variants[type] || 'bg-gray-100'}>
        {labels[type] || type}
      </Badge>
    );
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, string> = {
      P: 'bg-yellow-100 text-yellow-800',
      D: 'bg-green-100 text-green-800',
      F: 'bg-red-100 text-red-800',
      R: 'bg-gray-100 text-gray-800',
    };
    const labels: Record<string, string> = {
      P: 'Pending',
      D: 'Done',
      F: 'Failed',
      R: 'Reversed',
    };
    return (
      <Badge className={variants[status] || 'bg-gray-100'}>
        {labels[status] || status}
      </Badge>
    );
  };

  const filteredTransactions = transactions.filter(
    (t) =>
      t.portfolioId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.investmentId?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const pendingCount = transactions.filter((t) => t.status === 'P').length;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-900"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Transactions</h1>
        <div className="flex space-x-2">
          {pendingCount > 0 && (
            <Button
              onClick={processPending}
              disabled={processing}
              className="bg-green-600 hover:bg-green-700"
            >
              <Play className="h-4 w-4 mr-2" />
              {processing ? 'Processing...' : `Process Pending (${pendingCount})`}
            </Button>
          )}
          <Button className="bg-blue-900 hover:bg-blue-800">
            <Plus className="h-4 w-4 mr-2" />
            New Transaction
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Transaction List</CardTitle>
            <div className="relative w-64">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search transactions..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="text-left p-4 font-medium text-gray-600">ID</th>
                  <th className="text-left p-4 font-medium text-gray-600">Date</th>
                  <th className="text-left p-4 font-medium text-gray-600">Portfolio</th>
                  <th className="text-left p-4 font-medium text-gray-600">Investment</th>
                  <th className="text-center p-4 font-medium text-gray-600">Type</th>
                  <th className="text-right p-4 font-medium text-gray-600">Quantity</th>
                  <th className="text-right p-4 font-medium text-gray-600">Price</th>
                  <th className="text-right p-4 font-medium text-gray-600">Amount</th>
                  <th className="text-center p-4 font-medium text-gray-600">Status</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransactions.map((transaction) => (
                  <tr key={transaction.id} className="border-b hover:bg-gray-50">
                    <td className="p-4 font-mono text-sm">{transaction.id}</td>
                    <td className="p-4">{transaction.transactionDate}</td>
                    <td className="p-4 font-mono text-sm">{transaction.portfolioId}</td>
                    <td className="p-4 font-mono">{transaction.investmentId}</td>
                    <td className="p-4 text-center">{getTypeBadge(transaction.transactionType)}</td>
                    <td className="p-4 text-right">{transaction.quantity?.toFixed(4)}</td>
                    <td className="p-4 text-right">{formatCurrency(transaction.price)}</td>
                    <td className="p-4 text-right font-medium">{formatCurrency(transaction.amount)}</td>
                    <td className="p-4 text-center">{getStatusBadge(transaction.status)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredTransactions.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                No transactions found
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
