import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { createPortfolio } from '../lib/api';
import toast from 'react-hot-toast';

export default function NewPortfolio() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    portfolioId: '',
    accountType: 'IN',
    branchId: '01',
    clientId: '',
    portfolioName: '',
    currencyCode: 'USD',
    riskLevel: '3',
  });

  const mutation = useMutation({
    mutationFn: () => createPortfolio(form),
    onSuccess: (data) => {
      toast.success(`Portfolio ${data.data?.portfolioId} created`);
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      navigate(`/portfolios/${data.data?.portfolioId}`);
    },
    onError: (err: { response?: { data?: { error?: { message?: string } } } }) => {
      toast.error(err.response?.data?.error?.message || 'Failed to create portfolio');
    },
  });

  const handleChange = (field: string, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Create New Portfolio</h1>

      <div className="bg-white rounded-xl shadow-sm border p-6">
        <form onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Portfolio ID</label>
              <input
                type="text"
                value={form.portfolioId}
                onChange={(e) => handleChange('portfolioId', e.target.value.toUpperCase())}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
                placeholder="PORT0001"
                maxLength={8}
                pattern="PORT\d{4}"
                title="Must be PORT followed by 4 digits"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Client ID</label>
              <input
                type="text"
                value={form.clientId}
                onChange={(e) => handleChange('clientId', e.target.value.replace(/\D/g, ''))}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
                placeholder="0000000001"
                maxLength={10}
                pattern="\d{10}"
                title="Must be 10 numeric digits"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Portfolio Name</label>
            <input
              type="text"
              value={form.portfolioName}
              onChange={(e) => handleChange('portfolioName', e.target.value)}
              className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
              placeholder="My Investment Portfolio"
              maxLength={50}
              required
            />
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Account Type</label>
              <select
                value={form.accountType}
                onChange={(e) => handleChange('accountType', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
              >
                <option value="IN">Individual</option>
                <option value="CO">Corporate</option>
                <option value="TR">Trust</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Branch</label>
              <select
                value={form.branchId}
                onChange={(e) => handleChange('branchId', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
              >
                <option value="01">Branch 01</option>
                <option value="02">Branch 02</option>
                <option value="03">Branch 03</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Currency</label>
              <select
                value={form.currencyCode}
                onChange={(e) => handleChange('currencyCode', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
              >
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
                <option value="GBP">GBP</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Risk Level</label>
            <div className="flex gap-2">
              {['1', '2', '3', '4', '5'].map((level) => (
                <button
                  key={level}
                  type="button"
                  onClick={() => handleChange('riskLevel', level)}
                  className={`flex-1 py-2 rounded-lg border-2 font-medium transition-colors ${
                    form.riskLevel === level
                      ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'
                  }`}
                >
                  {level}
                </button>
              ))}
            </div>
            <p className="text-xs text-gray-500 mt-1">1 = Conservative, 5 = Aggressive</p>
          </div>

          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full py-3 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50 transition-colors"
          >
            {mutation.isPending ? 'Creating...' : 'Create Portfolio'}
          </button>
        </form>
      </div>
    </div>
  );
}
