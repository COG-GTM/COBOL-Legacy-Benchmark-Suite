import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { usePortfolios } from '@/context/PortfolioContext';
import type { Portfolio } from '@/data/types';

interface FormErrors {
  id?: string;
  name?: string;
  status?: string;
  totalValue?: string;
}

export function PortfolioNewPage() {
  const navigate = useNavigate();
  const { addPortfolio, getPortfolio } = usePortfolios();

  const [formData, setFormData] = useState({
    id: '',
    name: '',
    status: 'A' as Portfolio['status'],
    totalValue: '',
  });
  const [errors, setErrors] = useState<FormErrors>({});

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    if (!/^PORT\d{5}$/.test(formData.id)) {
      newErrors.id = 'Portfolio ID must start with "PORT" followed by exactly 5 numeric digits (e.g., PORT00013)';
    } else if (getPortfolio(formData.id)) {
      newErrors.id = 'A portfolio with this ID already exists';
    }

    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    }

    if (!['A', 'I', 'C'].includes(formData.status)) {
      newErrors.status = 'Status must be Active, Inactive, or Closed';
    }

    const value = parseFloat(formData.totalValue);
    if (isNaN(value) || value < 0) {
      newErrors.totalValue = 'Total value must be a valid positive number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const portfolio: Portfolio = {
      id: formData.id,
      name: formData.name.trim(),
      status: formData.status,
      totalValue: parseFloat(formData.totalValue),
      createDate: new Date().toISOString().split('T')[0],
    };

    addPortfolio(portfolio);
    navigate('/portfolios');
  };

  return (
    <div>
      <PageHeader title="Create Portfolio" description="Add a new investment portfolio" />

      <Card>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="portfolioId" className="block text-sm font-medium text-slate-700">
              Portfolio ID
            </label>
            <input
              id="portfolioId"
              type="text"
              value={formData.id}
              onChange={(e) => setFormData({ ...formData, id: e.target.value.toUpperCase() })}
              placeholder="PORT00013"
              className="mt-1 block w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.id && <p className="mt-1 text-sm text-red-600">{errors.id}</p>}
          </div>

          <div>
            <label htmlFor="name" className="block text-sm font-medium text-slate-700">
              Name
            </label>
            <input
              id="name"
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="Portfolio name"
              className="mt-1 block w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name}</p>}
          </div>

          <div>
            <label htmlFor="status" className="block text-sm font-medium text-slate-700">
              Status
            </label>
            <select
              id="status"
              value={formData.status}
              onChange={(e) => setFormData({ ...formData, status: e.target.value as Portfolio['status'] })}
              className="mt-1 block w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="A">Active</option>
              <option value="I">Inactive</option>
              <option value="C">Closed</option>
            </select>
            {errors.status && <p className="mt-1 text-sm text-red-600">{errors.status}</p>}
          </div>

          <div>
            <label htmlFor="totalValue" className="block text-sm font-medium text-slate-700">
              Total Value
            </label>
            <input
              id="totalValue"
              type="text"
              value={formData.totalValue}
              onChange={(e) => setFormData({ ...formData, totalValue: e.target.value })}
              placeholder="0.00"
              className="mt-1 block w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.totalValue && <p className="mt-1 text-sm text-red-600">{errors.totalValue}</p>}
          </div>

          <div className="flex items-center gap-3 pt-4 border-t border-slate-200">
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              Create Portfolio
            </button>
            <button
              type="button"
              onClick={() => navigate('/portfolios')}
              className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Cancel
            </button>
          </div>
        </form>
      </Card>
    </div>
  );
}
