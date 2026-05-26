import { useState, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';
import { portfolios } from '@/data/mockData';

interface FormErrors {
  name?: string;
  clientType?: string;
  status?: string;
}

const clientTypeLabels: Record<string, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export function PortfolioEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const portfolio = useMemo(() => portfolios.find((p) => p.id === id), [id]);

  const [name, setName] = useState(portfolio?.name ?? '');
  const [clientType, setClientType] = useState(portfolio?.clientType ?? '');
  const [status, setStatus] = useState(portfolio?.status ?? '');
  const [errors, setErrors] = useState<FormErrors>({});

  if (!portfolio) {
    return (
      <div>
        <PageHeader title="Portfolio Not Found" />
        <EmptyState
          title="Portfolio not found"
          message={`No portfolio with ID "${id ?? ''}" was found.`}
          action={
            <Link
              to="/portfolios"
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Portfolios
            </Link>
          }
        />
      </div>
    );
  }

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    if (!name.trim()) {
      newErrors.name = 'Portfolio name is required';
    }

    if (!clientType) {
      newErrors.clientType = 'Client type is required';
    } else if (!['I', 'C', 'T'].includes(clientType)) {
      newErrors.clientType = 'Client type must be Individual, Corporate, or Trust';
    }

    if (!status) {
      newErrors.status = 'Status is required';
    } else if (!['A', 'I', 'C'].includes(status)) {
      newErrors.status = 'Status must be Active, Inactive, or Closed';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const idx = portfolios.findIndex((p) => p.id === id);
    if (idx !== -1) {
      portfolios[idx] = {
        ...portfolios[idx],
        name: name.trim(),
        clientType: clientType as 'I' | 'C' | 'T',
        status: status as 'A' | 'I' | 'C',
        lastUser: 'ADMIN01',
        lastTransDate: new Date().toISOString().split('T')[0],
      };
    }

    navigate(`/portfolios/${id}`);
  };

  return (
    <div>
      <PageHeader
        title={`Edit Portfolio ${portfolio.id}`}
        description="Modify portfolio settings"
        actions={
          <Link
            to={`/portfolios/${portfolio.id}`}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Portfolio
          </Link>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card title="Edit Portfolio" className="lg:col-span-2">
          <form onSubmit={handleSubmit} className="space-y-6 max-w-lg">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Portfolio ID
              </label>
              <input
                type="text"
                value={portfolio.id}
                disabled
                className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-slate-50 text-slate-500 font-mono cursor-not-allowed"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Account Number
              </label>
              <input
                type="text"
                value={portfolio.accountNo}
                disabled
                className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-slate-50 text-slate-500 font-mono cursor-not-allowed"
              />
            </div>

            <div>
              <label htmlFor="edit-name" className="block text-sm font-medium text-slate-700 mb-1">
                Portfolio Name
              </label>
              <input
                id="edit-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.name ? 'border-red-300' : 'border-slate-300'
                }`}
              />
              {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name}</p>}
            </div>

            <div>
              <label htmlFor="edit-clientType" className="block text-sm font-medium text-slate-700 mb-1">
                Client Type
              </label>
              <select
                id="edit-clientType"
                value={clientType}
                onChange={(e) => setClientType(e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.clientType ? 'border-red-300' : 'border-slate-300'
                }`}
              >
                <option value="">Select client type</option>
                <option value="I">Individual</option>
                <option value="C">Corporate</option>
                <option value="T">Trust</option>
              </select>
              {errors.clientType && <p className="mt-1 text-sm text-red-600">{errors.clientType}</p>}
            </div>

            <div>
              <label htmlFor="edit-status" className="block text-sm font-medium text-slate-700 mb-1">
                Status
              </label>
              <select
                id="edit-status"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.status ? 'border-red-300' : 'border-slate-300'
                }`}
              >
                <option value="">Select status</option>
                <option value="A">Active</option>
                <option value="I">Inactive</option>
                <option value="C">Closed</option>
              </select>
              {errors.status && <p className="mt-1 text-sm text-red-600">{errors.status}</p>}
            </div>

            <div className="flex items-center gap-3 pt-4 border-t border-slate-200">
              <button
                type="submit"
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
              >
                <Save className="w-4 h-4" />
                Save Changes
              </button>
              <Link
                to={`/portfolios/${portfolio.id}`}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
              >
                Cancel
              </Link>
            </div>
          </form>
        </Card>

        <Card title="Audit Information">
          <dl className="space-y-4">
            <div>
              <dt className="text-sm font-medium text-slate-500">Portfolio ID</dt>
              <dd className="mt-1 text-sm font-mono text-slate-900">{portfolio.id}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Account Number</dt>
              <dd className="mt-1 text-sm font-mono text-slate-900">{portfolio.accountNo}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Client Type</dt>
              <dd className="mt-1 text-sm text-slate-900">{clientTypeLabels[portfolio.clientType] ?? portfolio.clientType}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Created</dt>
              <dd className="mt-1 text-sm text-slate-900">{portfolio.createDate}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Last User</dt>
              <dd className="mt-1 text-sm font-mono text-slate-900">{portfolio.lastUser ?? 'N/A'}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Last Transaction Date</dt>
              <dd className="mt-1 text-sm text-slate-900">{portfolio.lastTransDate ?? 'N/A'}</dd>
            </div>
          </dl>
        </Card>
      </div>
    </div>
  );
}
