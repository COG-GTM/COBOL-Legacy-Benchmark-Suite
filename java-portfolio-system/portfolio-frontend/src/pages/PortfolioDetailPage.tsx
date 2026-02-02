import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, TrendingUp, TrendingDown } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface PortfolioDetailPageProps {
  token: string | null;
}

interface PortfolioSummary {
  portfolioId: string;
  accountNo: string;
  clientName: string;
  status: string;
  totalMarketValue: number;
  totalCostBasis: number;
  cashBalance: number;
  unrealizedGainLoss: number;
  positionCount: number;
}

interface Position {
  portfolioId: string;
  positionDate: string;
  investmentId: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency: string;
  status: string;
}

export default function PortfolioDetailPage({ token }: PortfolioDetailPageProps) {
  const { portfolioId } = useParams<{ portfolioId: string }>();
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [positions, setPositions] = useState<Position[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (portfolioId) {
      fetchPortfolioData();
    }
  }, [portfolioId, token]);

  const fetchPortfolioData = async () => {
    try {
      const [summaryRes, positionsRes] = await Promise.all([
        fetch(`${API_URL}/api/portfolios/${portfolioId}/summary`, {
          headers: { 'Authorization': `Bearer ${token}` },
        }),
        fetch(`${API_URL}/api/portfolios/${portfolioId}/positions`, {
          headers: { 'Authorization': `Bearer ${token}` },
        }),
      ]);

      if (summaryRes.ok) {
        const summaryData = await summaryRes.json();
        setSummary(summaryData);
      }

      if (positionsRes.ok) {
        const positionsData = await positionsRes.json();
        setPositions(positionsData);
      }
    } catch (error) {
      console.error('Error fetching portfolio data:', error);
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

  const formatNumber = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 4,
      maximumFractionDigits: 4,
    }).format(value || 0);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-900"></div>
      </div>
    );
  }

  if (!summary) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500">Portfolio not found</p>
        <Link to="/portfolios">
          <Button className="mt-4">Back to Portfolios</Button>
        </Link>
      </div>
    );
  }

  const gainLossPositive = (summary.unrealizedGainLoss || 0) >= 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <Link to="/portfolios">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
        </Link>
        <h1 className="text-3xl font-bold text-gray-900">Portfolio Details</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Portfolio Information</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-gray-500">Portfolio ID</p>
                <p className="font-mono font-medium">{summary.portfolioId}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Account Number</p>
                <p className="font-mono font-medium">{summary.accountNo}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Client Name</p>
                <p className="font-medium">{summary.clientName}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Status</p>
                <Badge className={summary.status === 'A' ? 'bg-green-100 text-green-800' : 'bg-gray-100'}>
                  {summary.status === 'A' ? 'Active' : summary.status}
                </Badge>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Summary</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <p className="text-sm text-gray-500">Total Market Value</p>
              <p className="text-2xl font-bold">{formatCurrency(summary.totalMarketValue)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Cash Balance</p>
              <p className="text-lg font-medium">{formatCurrency(summary.cashBalance)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Unrealized Gain/Loss</p>
              <div className={`flex items-center ${gainLossPositive ? 'text-green-600' : 'text-red-600'}`}>
                {gainLossPositive ? (
                  <TrendingUp className="h-5 w-5 mr-1" />
                ) : (
                  <TrendingDown className="h-5 w-5 mr-1" />
                )}
                <span className="text-lg font-medium">{formatCurrency(summary.unrealizedGainLoss)}</span>
              </div>
            </div>
            <div>
              <p className="text-sm text-gray-500">Positions</p>
              <p className="font-medium">{summary.positionCount} holdings</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Positions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="text-left p-4 font-medium text-gray-600">Investment</th>
                  <th className="text-right p-4 font-medium text-gray-600">Quantity</th>
                  <th className="text-right p-4 font-medium text-gray-600">Cost Basis</th>
                  <th className="text-right p-4 font-medium text-gray-600">Market Value</th>
                  <th className="text-right p-4 font-medium text-gray-600">Gain/Loss</th>
                  <th className="text-center p-4 font-medium text-gray-600">Currency</th>
                  <th className="text-center p-4 font-medium text-gray-600">Status</th>
                </tr>
              </thead>
              <tbody>
                {positions.map((position, index) => {
                  const gainLoss = (position.marketValue || 0) - (position.costBasis || 0);
                  const isPositive = gainLoss >= 0;
                  return (
                    <tr key={index} className="border-b hover:bg-gray-50">
                      <td className="p-4 font-mono font-medium">{position.investmentId}</td>
                      <td className="p-4 text-right">{formatNumber(position.quantity)}</td>
                      <td className="p-4 text-right">{formatCurrency(position.costBasis)}</td>
                      <td className="p-4 text-right font-medium">{formatCurrency(position.marketValue)}</td>
                      <td className={`p-4 text-right ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                        {formatCurrency(gainLoss)}
                      </td>
                      <td className="p-4 text-center">{position.currency}</td>
                      <td className="p-4 text-center">
                        <Badge className={position.status === 'A' ? 'bg-green-100 text-green-800' : 'bg-gray-100'}>
                          {position.status === 'A' ? 'Active' : position.status}
                        </Badge>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {positions.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                No positions found
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
