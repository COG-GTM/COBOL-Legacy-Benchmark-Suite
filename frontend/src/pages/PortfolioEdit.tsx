import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { usePortfolio } from '../context/PortfolioContext';
import { useToast } from '../context/ToastContext';
import { usePortfolioForm } from '../hooks/usePortfolioForm';
import FormField from '../components/FormField';

export default function PortfolioEdit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { portfolios, updatePortfolio } = usePortfolio();
  const { addToast } = useToast();

  const portfolio = portfolios.find((p) => p.id === id);

  const { formData, errors, updateField, validate } = usePortfolioForm(
    portfolio
      ? {
          id: portfolio.id,
          accountNo: portfolio.accountNo,
          clientName: portfolio.clientName,
          clientType: portfolio.clientType,
          status: portfolio.status,
          totalValue: String(portfolio.totalValue),
          cashBalance: String(portfolio.cashBalance),
        }
      : undefined,
  );

  if (!portfolio) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-semibold text-gray-700 mb-2">Portfolio Not Found</h2>
        <Link to="/portfolios" className="text-blue-600 hover:underline">
          Back to Portfolios
        </Link>
      </div>
    );
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const now = new Date();
    const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`;

    updatePortfolio(portfolio.id, {
      accountNo: formData.accountNo,
      clientName: formData.clientName,
      clientType: formData.clientType,
      status: formData.status,
      totalValue: Number(formData.totalValue) || 0,
      cashBalance: Number(formData.cashBalance) || 0,
      lastMaintDate: dateStr,
    });

    addToast(`Portfolio ${portfolio.id} updated successfully.`, 'success');
    navigate(`/portfolios/${portfolio.id}`);
  };

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={() => navigate(`/portfolios/${portfolio.id}`)}
          className="p-2 hover:bg-gray-100 rounded-md"
        >
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </button>
        <h1 className="text-2xl font-bold text-gray-800">Edit Portfolio {portfolio.id}</h1>
      </div>

      <div className="bg-white rounded-lg shadow-sm border p-6">
        <form onSubmit={handleSubmit}>
          <FormField label="Portfolio ID" value={formData.id} disabled />
          <FormField
            label="Account Number"
            value={formData.accountNo}
            onChange={(e) => updateField('accountNo', (e.target as HTMLInputElement).value)}
            error={errors.accountNo}
            maxLength={10}
          />
          <FormField
            label="Client Name"
            value={formData.clientName}
            onChange={(e) => updateField('clientName', (e.target as HTMLInputElement).value)}
            error={errors.clientName}
          />
          <FormField
            label="Client Type"
            as="select"
            value={formData.clientType}
            onChange={(e) => updateField('clientType', (e.target as HTMLSelectElement).value)}
          >
            <option value="I">Individual</option>
            <option value="C">Corporate</option>
            <option value="T">Trust</option>
          </FormField>
          <FormField
            label="Status"
            as="select"
            value={formData.status}
            onChange={(e) => updateField('status', (e.target as HTMLSelectElement).value)}
            error={errors.status}
          >
            <option value="A">Active</option>
            <option value="I">Inactive</option>
            <option value="C">Closed</option>
          </FormField>
          <FormField
            label="Total Value"
            type="number"
            step="0.01"
            value={formData.totalValue}
            onChange={(e) => updateField('totalValue', (e.target as HTMLInputElement).value)}
            error={errors.totalValue}
          />
          <FormField
            label="Cash Balance"
            type="number"
            step="0.01"
            value={formData.cashBalance}
            onChange={(e) => updateField('cashBalance', (e.target as HTMLInputElement).value)}
          />

          <div className="flex justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={() => navigate(`/portfolios/${portfolio.id}`)}
              className="px-4 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
