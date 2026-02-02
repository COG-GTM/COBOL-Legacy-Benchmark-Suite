import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Briefcase, TrendingUp, ArrowRightLeft, DollarSign } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface DashboardPageProps {
  token: string | null;
}

interface Statistics {
  totalPortfolios: number;
  activePortfolios: number;
  totalAssetsUnderManagement: number;
  transactionsToday: number;
  buyTransactions: number;
  sellTransactions: number;
  totalTransactionVolume: number;
  portfoliosByType: Record<string, number>;
}

const COLORS = ['#1e3a8a', '#3b82f6', '#60a5fa', '#93c5fd'];

export default function DashboardPage({ token }: DashboardPageProps) {
  const [statistics, setStatistics] = useState<Statistics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStatistics();
  }, [token]);

  const fetchStatistics = async () => {
    try {
      const response = await fetch(`${API_URL}/api/reports/statistics`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setStatistics(data);
      }
    } catch (error) {
      console.error('Error fetching statistics:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const pieData = statistics?.portfoliosByType
    ? Object.entries(statistics.portfoliosByType).map(([name, value]) => ({
        name,
        value,
      }))
    : [];

  const transactionData = [
    { name: 'Buy', value: statistics?.buyTransactions || 0 },
    { name: 'Sell', value: statistics?.sellTransactions || 0 },
  ];

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
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-500">Portfolio Management System - Java Migration</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">Total Portfolios</CardTitle>
            <Briefcase className="h-5 w-5 text-blue-900" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">{statistics?.totalPortfolios || 0}</div>
            <p className="text-sm text-gray-500">{statistics?.activePortfolios || 0} active</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">Assets Under Management</CardTitle>
            <DollarSign className="h-5 w-5 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">
              {formatCurrency(statistics?.totalAssetsUnderManagement || 0)}
            </div>
            <p className="text-sm text-gray-500">Total portfolio value</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">Today's Transactions</CardTitle>
            <ArrowRightLeft className="h-5 w-5 text-orange-500" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">{statistics?.transactionsToday || 0}</div>
            <p className="text-sm text-gray-500">
              {statistics?.buyTransactions || 0} buys, {statistics?.sellTransactions || 0} sells
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">Transaction Volume</CardTitle>
            <TrendingUp className="h-5 w-5 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">
              {formatCurrency(statistics?.totalTransactionVolume || 0)}
            </div>
            <p className="text-sm text-gray-500">Today's volume</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Portfolios by Client Type</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {pieData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Transaction Activity</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={transactionData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="value" fill="#1e3a8a" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>System Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <p className="text-gray-500">Original System</p>
              <p className="font-medium">COBOL/CICS/DB2</p>
            </div>
            <div>
              <p className="text-gray-500">Migrated To</p>
              <p className="font-medium">Java/Spring Boot</p>
            </div>
            <div>
              <p className="text-gray-500">Database</p>
              <p className="font-medium">H2 (Dev) / PostgreSQL</p>
            </div>
            <div>
              <p className="text-gray-500">API Style</p>
              <p className="font-medium">REST/JSON</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
