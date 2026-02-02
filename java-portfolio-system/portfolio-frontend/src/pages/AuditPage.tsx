import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Search, Shield } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface AuditPageProps {
  token: string | null;
}

interface AuditLog {
  id: number;
  timestamp: string;
  systemId: string;
  userId: string;
  program: string;
  terminal: string;
  auditType: string;
  action: string;
  status: string;
  portfolioId: string;
  message: string;
}

export default function AuditPage({ token }: AuditPageProps) {
  const [audits, setAudits] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchAudits();
  }, [token]);

  const fetchAudits = async () => {
    try {
      const response = await fetch(`${API_URL}/api/audits`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setAudits(data);
      }
    } catch (error) {
      console.error('Error fetching audits:', error);
    } finally {
      setLoading(false);
    }
  };

  const getTypeBadge = (type: string) => {
    const variants: Record<string, string> = {
      TRAN: 'bg-blue-100 text-blue-800',
      USER: 'bg-green-100 text-green-800',
      SYST: 'bg-purple-100 text-purple-800',
    };
    return (
      <Badge className={variants[type] || 'bg-gray-100'}>
        {type}
      </Badge>
    );
  };

  const getActionBadge = (action: string) => {
    const variants: Record<string, string> = {
      CREATE: 'bg-green-100 text-green-800',
      UPDATE: 'bg-yellow-100 text-yellow-800',
      DELETE: 'bg-red-100 text-red-800',
      INQUIRE: 'bg-blue-100 text-blue-800',
      LOGIN: 'bg-purple-100 text-purple-800',
      LOGOUT: 'bg-gray-100 text-gray-800',
    };
    return (
      <Badge className={variants[action] || 'bg-gray-100'}>
        {action}
      </Badge>
    );
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, string> = {
      SUCC: 'bg-green-100 text-green-800',
      FAIL: 'bg-red-100 text-red-800',
      WARN: 'bg-yellow-100 text-yellow-800',
    };
    const labels: Record<string, string> = {
      SUCC: 'Success',
      FAIL: 'Failed',
      WARN: 'Warning',
    };
    return (
      <Badge className={variants[status] || 'bg-gray-100'}>
        {labels[status] || status}
      </Badge>
    );
  };

  const filteredAudits = audits.filter(
    (a) =>
      a.userId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      a.program?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      a.portfolioId?.toLowerCase().includes(searchTerm.toLowerCase())
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
        <div className="flex items-center space-x-3">
          <Shield className="h-8 w-8 text-blue-900" />
          <h1 className="text-3xl font-bold text-gray-900">Audit Log</h1>
        </div>
        <p className="text-gray-500">Security and Access Trail - Migrated from SECMGR</p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Audit Trail</CardTitle>
            <div className="relative w-64">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search audits..."
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
                  <th className="text-left p-4 font-medium text-gray-600">Timestamp</th>
                  <th className="text-left p-4 font-medium text-gray-600">User</th>
                  <th className="text-left p-4 font-medium text-gray-600">Program</th>
                  <th className="text-center p-4 font-medium text-gray-600">Type</th>
                  <th className="text-center p-4 font-medium text-gray-600">Action</th>
                  <th className="text-center p-4 font-medium text-gray-600">Status</th>
                  <th className="text-left p-4 font-medium text-gray-600">Portfolio</th>
                  <th className="text-left p-4 font-medium text-gray-600">Message</th>
                </tr>
              </thead>
              <tbody>
                {filteredAudits.map((audit) => (
                  <tr key={audit.id} className="border-b hover:bg-gray-50">
                    <td className="p-4 text-sm">{audit.timestamp?.replace('T', ' ').substring(0, 19)}</td>
                    <td className="p-4 font-mono text-sm">{audit.userId}</td>
                    <td className="p-4 font-mono text-sm">{audit.program}</td>
                    <td className="p-4 text-center">{getTypeBadge(audit.auditType)}</td>
                    <td className="p-4 text-center">{getActionBadge(audit.action)}</td>
                    <td className="p-4 text-center">{getStatusBadge(audit.status)}</td>
                    <td className="p-4 font-mono text-sm">{audit.portfolioId || '-'}</td>
                    <td className="p-4 text-sm text-gray-600 max-w-xs truncate">{audit.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredAudits.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                No audit records found
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
