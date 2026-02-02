import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { FileText, Download } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface ReportsPageProps {
  token: string | null;
}

interface PositionReport {
  reportDate: string;
  portfolioId: string;
  accountNo: string;
  clientName: string;
  status: string;
  positions: Array<{
    investmentId: string;
    quantity: number;
    costBasis: number;
    marketValue: number;
    currency: string;
    gainLoss: number;
  }>;
  totalMarketValue: number;
  totalCostBasis: number;
  totalGainLoss: number;
  cashBalance: number;
  totalPortfolioValue: number;
}

export default function ReportsPage({ token }: ReportsPageProps) {
  const [portfolioId, setPortfolioId] = useState('PORT0001');
  const [report, setReport] = useState<PositionReport | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchReport = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/api/reports/position/${portfolioId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setReport(data);
      }
    } catch (error) {
      console.error('Error fetching report:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, []);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value || 0);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Reports</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="cursor-pointer hover:shadow-lg transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center space-x-4">
              <div className="bg-blue-100 p-3 rounded-lg">
                <FileText className="h-8 w-8 text-blue-900" />
              </div>
              <div>
                <h3 className="font-semibold text-lg">Position Report</h3>
                <p className="text-sm text-gray-500">RPTPOS00</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="cursor-pointer hover:shadow-lg transition-shadow opacity-60">
          <CardContent className="p-6">
            <div className="flex items-center space-x-4">
              <div className="bg-green-100 p-3 rounded-lg">
                <FileText className="h-8 w-8 text-green-900" />
              </div>
              <div>
                <h3 className="font-semibold text-lg">Audit Report</h3>
                <p className="text-sm text-gray-500">RPTAUD00</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="cursor-pointer hover:shadow-lg transition-shadow opacity-60">
          <CardContent className="p-6">
            <div className="flex items-center space-x-4">
              <div className="bg-purple-100 p-3 rounded-lg">
                <FileText className="h-8 w-8 text-purple-900" />
              </div>
              <div>
                <h3 className="font-semibold text-lg">Statistics Report</h3>
                <p className="text-sm text-gray-500">RPTSTA00</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Position Report</CardTitle>
            <div className="flex items-center space-x-2">
              <Input
                placeholder="Portfolio ID"
                value={portfolioId}
                onChange={(e) => setPortfolioId(e.target.value)}
                className="w-40"
              />
              <Button onClick={fetchReport} disabled={loading}>
                Generate
              </Button>
              <Button variant="outline" disabled={!report}>
                <Download className="h-4 w-4 mr-2" />
                Export
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center h-32">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-900"></div>
            </div>
          ) : report ? (
            <div className="space-y-6">
              <div className="bg-gray-50 p-4 rounded-lg">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                  <div>
                    <p className="text-gray-500">Report Date</p>
                    <p className="font-medium">{report.reportDate}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Portfolio ID</p>
                    <p className="font-mono font-medium">{report.portfolioId}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Account No</p>
                    <p className="font-mono font-medium">{report.accountNo}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Client Name</p>
                    <p className="font-medium">{report.clientName}</p>
                  </div>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b bg-gray-50">
                      <th className="text-left p-3 font-medium text-gray-600">Investment</th>
                      <th className="text-right p-3 font-medium text-gray-600">Quantity</th>
                      <th className="text-right p-3 font-medium text-gray-600">Cost Basis</th>
                      <th className="text-right p-3 font-medium text-gray-600">Market Value</th>
                      <th className="text-right p-3 font-medium text-gray-600">Gain/Loss</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.positions?.map((position, index) => (
                      <tr key={index} className="border-b">
                        <td className="p-3 font-mono">{position.investmentId}</td>
                        <td className="p-3 text-right">{position.quantity?.toFixed(4)}</td>
                        <td className="p-3 text-right">{formatCurrency(position.costBasis)}</td>
                        <td className="p-3 text-right">{formatCurrency(position.marketValue)}</td>
                        <td className={`p-3 text-right ${(position.gainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                          {formatCurrency(position.gainLoss)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot className="bg-gray-50 font-medium">
                    <tr className="border-t-2">
                      <td className="p-3">Total</td>
                      <td className="p-3"></td>
                      <td className="p-3 text-right">{formatCurrency(report.totalCostBasis)}</td>
                      <td className="p-3 text-right">{formatCurrency(report.totalMarketValue)}</td>
                      <td className={`p-3 text-right ${(report.totalGainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {formatCurrency(report.totalGainLoss)}
                      </td>
                    </tr>
                    <tr>
                      <td className="p-3">Cash Balance</td>
                      <td className="p-3" colSpan={3}></td>
                      <td className="p-3 text-right">{formatCurrency(report.cashBalance)}</td>
                    </tr>
                    <tr className="border-t-2 text-lg">
                      <td className="p-3">Total Portfolio Value</td>
                      <td className="p-3" colSpan={3}></td>
                      <td className="p-3 text-right font-bold">{formatCurrency(report.totalPortfolioValue)}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              Enter a portfolio ID and click Generate to view the report
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
