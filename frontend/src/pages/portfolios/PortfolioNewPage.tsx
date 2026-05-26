import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { portfolios } from '@/data/mockData';

interface FormErrors {
  name?: string;
  accountNo?: string;
  clientType?: string;
  cashBalance?: string;
}

function generatePortfolioId(): string {
  const maxNum = portfolios.reduce((max, p) => {
    const num = parseInt(p.id.replace('PORT', ''), 10);
    return num > max ? num : max;
  }, 0);
  return `PORT${String(maxNum + 1).padStart(4, '0')}`;
}

export function PortfolioNewPage() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [accountNo, setAccountNo] = useState('');
  const [clientType, setClientType] = useState('');
  const [cashBalance, setCashBalance] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    if (!name.trim()) {
      newErrors.name = 'Portfolio name is required';
    }

    if (!accountNo) {
      newErrors.accountNo = 'Account number is required';
    } else if (!/^\d{10}$/.test(accountNo)) {
      newErrors.accountNo = 'Account number must be exactly 10 digits';
    }

    if (!clientType) {
      newErrors.clientType = 'Client type is required';
    } else if (!['I', 'C', 'T'].includes(clientType)) {
      newErrors.clientType = 'Client type must be Individual, Corporate, or Trust';
    }

    const balance = parseFloat(cashBalance);
    if (cashBalance === '') {
      newErrors.cashBalance = 'Initial cash balance is required';
    } else if (isNaN(balance) || balance < 0) {
      newErrors.cashBalance = 'Cash balance must be a non-negative number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const newId = generatePortfolioId();
    const balance = parseFloat(cashBalance);

    portfolios.push({
      id: newId,
      name: name.trim(),
      accountNo,
      clientType: clientType as 'I' | 'C' | 'T',
      cashBalance: balance,
      createDate: new Date().toISOString().split('T')[0],
      status: 'A',
      totalValue: balance,
      lastUser: 'ADMIN01',
      lastTransDate: new Date().toISOString().split('T')[0],
    });

    navigate('/portfolios');
  };

  return (
    <div>
      <PageHeader
        title="Create Portfolio"
        description="Add a new investment portfolio"
        actions={
          <Link
            to="/portfolios"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Portfolios
          </Link>
        }
      />

      <Card title="Portfolio Details">
        <form onSubmit={handleSubmit} className="space-y-6 max-w-lg">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-slate-700 mb-1">
              Portfolio Name
            </label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                errors.name ? 'border-red-300' : 'border-slate-300'
              }`}
              placeholder="Enter portfolio name"
            />
            {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name}</p>}
          </div>

          <div>
            <label htmlFor="accountNo" className="block text-sm font-medium text-slate-700 mb-1">
              Account Number
            </label>
            <input
              id="accountNo"
              type="text"
              value={accountNo}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, '').slice(0, 10);
                setAccountNo(val);
              }}
              className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono ${
                errors.accountNo ? 'border-red-300' : 'border-slate-300'
              }`}
              placeholder="10-digit numeric account number"
              maxLength={10}
            />
            {errors.accountNo && <p className="mt-1 text-sm text-red-600">{errors.accountNo}</p>}
          </div>

          <div>
            <label htmlFor="clientType" className="block text-sm font-medium text-slate-700 mb-1">
              Client Type
            </label>
            <select
              id="clientType"
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
            <label htmlFor="cashBalance" className="block text-sm font-medium text-slate-700 mb-1">
              Initial Cash Balance
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-slate-400">$</span>
              <input
                id="cashBalance"
                type="number"
                step="0.01"
                min="0"
                value={cashBalance}
                onChange={(e) => setCashBalance(e.target.value)}
                className={`w-full pl-7 pr-3 py-2 text-sm border rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.cashBalance ? 'border-red-300' : 'border-slate-300'
                }`}
                placeholder="0.00"
              />
            </div>
            {errors.cashBalance && <p className="mt-1 text-sm text-red-600">{errors.cashBalance}</p>}
          </div>

          <div className="flex items-center gap-3 pt-4 border-t border-slate-200">
            <button
              type="submit"
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              <Save className="w-4 h-4" />
              Create Portfolio
            </button>
            <Link
              to="/portfolios"
              className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Cancel
            </Link>
          </div>
        </form>
      </Card>
    </div>
  );
}
