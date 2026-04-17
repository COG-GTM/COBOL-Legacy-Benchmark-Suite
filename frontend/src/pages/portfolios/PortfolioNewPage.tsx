import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Save } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Toast } from '@/components/ui/Toast';
import { usePortfolioContext } from '@/context/PortfolioContext';
import { validatePortfolioId, validatePortfolioName, validatePortfolioStatus } from '@/utils/validation';

interface FormErrors {
  id?: string;
  name?: string;
  status?: string;
}

export function PortfolioNewPage() {
  const navigate = useNavigate();
  const { addPortfolio, getPortfolio, notification, showNotification, clearNotification } = usePortfolioContext();

  const [formData, setFormData] = useState({
    id: '',
    name: '',
    status: 'A' as 'A' | 'I' | 'C',
  });
  const [errors, setErrors] = useState<FormErrors>({});

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    const idResult = validatePortfolioId(formData.id);
    if (!idResult.valid) {
      newErrors.id = idResult.error ?? undefined;
    } else if (getPortfolio(formData.id.trim())) {
      newErrors.id = 'A portfolio with this ID already exists';
    }

    const nameResult = validatePortfolioName(formData.name);
    if (!nameResult.valid) {
      newErrors.name = nameResult.error ?? undefined;
    }

    const statusResult = validatePortfolioStatus(formData.status);
    if (!statusResult.valid) {
      newErrors.status = statusResult.error ?? undefined;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    addPortfolio({
      id: formData.id.trim().toUpperCase(),
      name: formData.name.trim(),
      status: formData.status,
      totalValue: 0,
      createDate: new Date().toISOString().split('T')[0],
    });

    showNotification(`Portfolio "${formData.name.trim()}" created successfully`, 'success');
    navigate('/portfolios');
  };

  return (
    <div>
      {notification && (
        <Toast message={notification.message} type={notification.type} onClose={clearNotification} />
      )}

      <PageHeader
        title="Create Portfolio"
        description="Add a new investment portfolio"
        actions={
          <button
            onClick={() => navigate('/portfolios')}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Portfolios
          </button>
        }
      />

      <Card className="max-w-2xl">
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label htmlFor="portfolio-id" className="block text-sm font-medium text-slate-700 mb-1">
              Portfolio ID
            </label>
            <input
              id="portfolio-id"
              type="text"
              value={formData.id}
              onChange={(e) => {
                setFormData((prev) => ({ ...prev, id: e.target.value }));
                if (errors.id) setErrors((prev) => ({ ...prev, id: undefined }));
              }}
              placeholder="PORT0001"
              className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                errors.id ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-slate-300'
              }`}
            />
            <p className="mt-1 text-xs text-slate-500">Format: PORT + 4 digits (e.g., PORT0001)</p>
            {errors.id && <p className="mt-1 text-sm text-red-600">{errors.id}</p>}
          </div>

          <div>
            <label htmlFor="portfolio-name" className="block text-sm font-medium text-slate-700 mb-1">
              Portfolio Name
            </label>
            <input
              id="portfolio-name"
              type="text"
              value={formData.name}
              onChange={(e) => {
                setFormData((prev) => ({ ...prev, name: e.target.value }));
                if (errors.name) setErrors((prev) => ({ ...prev, name: undefined }));
              }}
              placeholder="e.g., Growth Equity Fund"
              className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                errors.name ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-slate-300'
              }`}
            />
            {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name}</p>}
          </div>

          <div>
            <label htmlFor="portfolio-status" className="block text-sm font-medium text-slate-700 mb-1">
              Status
            </label>
            <select
              id="portfolio-status"
              value={formData.status}
              onChange={(e) => {
                setFormData((prev) => ({ ...prev, status: e.target.value as 'A' | 'I' | 'C' }));
                if (errors.status) setErrors((prev) => ({ ...prev, status: undefined }));
              }}
              className={`w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                errors.status ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-slate-300'
              }`}
            >
              <option value="A">Active</option>
              <option value="I">Inactive</option>
              <option value="C">Closed</option>
            </select>
            {errors.status && <p className="mt-1 text-sm text-red-600">{errors.status}</p>}
          </div>

          <div className="flex items-center gap-3 pt-2">
            <button
              type="submit"
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              <Save className="w-4 h-4" />
              Create
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
