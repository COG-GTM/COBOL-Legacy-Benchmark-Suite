import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Search, Eye, Plus } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface PortfoliosPageProps {
  token: string | null;
}

interface Portfolio {
  portfolioId: string;
  accountNo: string;
  clientName: string;
  clientType: string;
  status: string;
  totalValue: number;
  cashBalance: number;
}

export default function PortfoliosPage({ token }: PortfoliosPageProps) {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchPortfolios();
  }, [token]);

  const fetchPortfolios = async () => {
    try {
      const response = await fetch(`${API_URL}/api/portfolios`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setPortfolios(data);
      }
    } catch (error) {
      console.error('Error fetching portfolios:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value || 0);
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, string> = {
      A: 'bg-green-100 text-green-800',
      C: 'bg-gray-100 text-gray-800',
      S: 'bg-yellow-100 text-yellow-800',
    };
    const labels: Record<string, string> = {
      A: 'Active',
      C: 'Closed',
      S: 'Suspended',
    };
    return (
      <Badge className={variants[status] || 'bg-gray-100'}>
        {labels[status] || status}
      </Badge>
    );
  };

  const getClientTypeBadge = (type: string) => {
    const labels: Record<string, string> = {
      I: 'Individual',
      C: 'Corporate',
      T: 'Trust',
    };
    return labels[type] || type;
  };

  const filteredPortfolios = portfolios.filter(
    (p) =>
      p.clientName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.accountNo?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.portfolioId?.toLowerCase().includes(searchTerm.toLowerCase())
  );

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
        <h1 className="text-3xl font-bold text-gray-900">Portfolios</h1>
        <Button className="bg-blue-900 hover:bg-blue-800">
          <Plus className="h-4 w-4 mr-2" />
          New Portfolio
        </Button>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Portfolio List</CardTitle>
            <div className="relative w-64">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search portfolios..."
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
                  <th className="text-left p-4 font-medium text-gray-600">Portfolio ID</th>
                  <th className="text-left p-4 font-medium text-gray-600">Account No</th>
                  <th className="text-left p-4 font-medium text-gray-600">Client Name</th>
                  <th className="text-left p-4 font-medium text-gray-600">Type</th>
                  <th className="text-left p-4 font-medium text-gray-600">Status</th>
                  <th className="text-right p-4 font-medium text-gray-600">Total Value</th>
                  <th className="text-right p-4 font-medium text-gray-600">Cash Balance</th>
                  <th className="text-center p-4 font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredPortfolios.map((portfolio) => (
                  <tr key={portfolio.portfolioId} className="border-b hover:bg-gray-50">
                    <td className="p-4 font-mono text-sm">{portfolio.portfolioId}</td>
                    <td className="p-4 font-mono text-sm">{portfolio.accountNo}</td>
                    <td className="p-4">{portfolio.clientName}</td>
                    <td className="p-4">{getClientTypeBadge(portfolio.clientType)}</td>
                    <td className="p-4">{getStatusBadge(portfolio.status)}</td>
                    <td className="p-4 text-right font-medium">{formatCurrency(portfolio.totalValue)}</td>
                    <td className="p-4 text-right">{formatCurrency(portfolio.cashBalance)}</td>
                    <td className="p-4 text-center">
                      <Link to={`/portfolios/${portfolio.portfolioId}`}>
                        <Button variant="ghost" size="sm">
                          <Eye className="h-4 w-4 mr-1" />
                          View
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredPortfolios.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                No portfolios found
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
