import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Search } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface HistoryPageProps {
  token: string | null;
}

interface HistoryRecord {
  id: number;
  portfolioId: string;
  historyDate: string;
  historyTime: string;
  recordType: string;
  actionCode: string;
  beforeImage: string;
  afterImage: string;
  processUser: string;
}

export default function HistoryPage({ token }: HistoryPageProps) {
  const [history, setHistory] = useState<HistoryRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [portfolioId, setPortfolioId] = useState('PORT0001');

  useEffect(() => {
    if (portfolioId) {
      fetchHistory();
    }
  }, [portfolioId, token]);

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/api/history/portfolio/${portfolioId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setHistory(data);
      }
    } catch (error) {
      console.error('Error fetching history:', error);
    } finally {
      setLoading(false);
    }
  };

  const getRecordTypeBadge = (type: string) => {
    const variants: Record<string, string> = {
      PT: 'bg-blue-100 text-blue-800',
      PS: 'bg-green-100 text-green-800',
      TR: 'bg-purple-100 text-purple-800',
    };
    const labels: Record<string, string> = {
      PT: 'Portfolio',
      PS: 'Position',
      TR: 'Transaction',
    };
    return (
      <Badge className={variants[type] || 'bg-gray-100'}>
        {labels[type] || type}
      </Badge>
    );
  };

  const getActionBadge = (action: string) => {
    const variants: Record<string, string> = {
      A: 'bg-green-100 text-green-800',
      C: 'bg-yellow-100 text-yellow-800',
      D: 'bg-red-100 text-red-800',
    };
    const labels: Record<string, string> = {
      A: 'Add',
      C: 'Change',
      D: 'Delete',
    };
    return (
      <Badge className={variants[action] || 'bg-gray-100'}>
        {labels[action] || action}
      </Badge>
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">History</h1>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Change History</CardTitle>
            <div className="relative w-64">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Portfolio ID..."
                value={portfolioId}
                onChange={(e) => setPortfolioId(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center h-32">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-900"></div>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b bg-gray-50">
                    <th className="text-left p-4 font-medium text-gray-600">ID</th>
                    <th className="text-left p-4 font-medium text-gray-600">Portfolio</th>
                    <th className="text-left p-4 font-medium text-gray-600">Date</th>
                    <th className="text-left p-4 font-medium text-gray-600">Time</th>
                    <th className="text-center p-4 font-medium text-gray-600">Record Type</th>
                    <th className="text-center p-4 font-medium text-gray-600">Action</th>
                    <th className="text-left p-4 font-medium text-gray-600">User</th>
                    <th className="text-left p-4 font-medium text-gray-600">Changes</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((record) => (
                    <tr key={record.id} className="border-b hover:bg-gray-50">
                      <td className="p-4 font-mono text-sm">{record.id}</td>
                      <td className="p-4 font-mono text-sm">{record.portfolioId}</td>
                      <td className="p-4">{record.historyDate}</td>
                      <td className="p-4">{record.historyTime}</td>
                      <td className="p-4 text-center">{getRecordTypeBadge(record.recordType)}</td>
                      <td className="p-4 text-center">{getActionBadge(record.actionCode)}</td>
                      <td className="p-4 font-mono text-sm">{record.processUser}</td>
                      <td className="p-4 text-sm text-gray-600 max-w-xs truncate">
                        {record.afterImage || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {history.length === 0 && (
                <div className="text-center py-8 text-gray-500">
                  No history records found for this portfolio
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
