import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { createTransaction } from '../lib/api';
import toast from 'react-hot-toast';

export default function NewTransaction() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    portfolioId: '',
    investmentId: '',
    transactionType: 'BU',
    quantity: '',
    price: '',
    currencyCode: 'USD',
  });

  const mutation = useMutation({
    mutationFn: () =>
      createTransaction({
        ...form,
        quantity: parseFloat(form.quantity),
        price: parseFloat(form.price),
      }),
    onSuccess: (data) => {
      toast.success(`Transaction created: ${data.data?.transactionId}`);
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      navigate(`/portfolios/${form.portfolioId}`);
    },
    onError: (err: { response?: { data?: { error?: { message?: string } } } }) => {
      toast.error(err.response?.data?.error?.message || 'Transaction failed');
    },
  });

  const amount = (parseFloat(form.quantity) || 0) * (parseFloat(form.price) || 0);

  const handleChange = (field: string, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">New Transaction</h1>

      <div className="bg-white rounded-xl shadow-sm border p-6">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            mutation.mutate();
          }}
          className="space-y-4"
        >
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
              <label className="block text-sm font-medium text-gray-700 mb-1">Investment ID</label>
              <input
                type="text"
                value={form.investmentId}
                onChange={(e) => handleChange('investmentId', e.target.value.toUpperCase())}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
                placeholder="AAPL"
                maxLength={10}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Transaction Type</label>
            <div className="grid grid-cols-4 gap-2">
              {[
                { value: 'BU', label: 'Buy', color: 'green' },
                { value: 'SL', label: 'Sell', color: 'red' },
                { value: 'TR', label: 'Transfer', color: 'blue' },
                { value: 'FE', label: 'Fee', color: 'yellow' },
              ].map(({ value, label, color }) => (
                <button
                  type="button"
                  key={value}
                  onClick={() => handleChange('transactionType', value)}
                  className={`py-2 px-4 rounded-lg border-2 text-sm font-medium transition-colors ${
                    form.transactionType === value
                      ? `border-${color}-500 bg-${color}-50 text-${color}-700`
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
              <input
                type="number"
                value={form.quantity}
                onChange={(e) => handleChange('quantity', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
                placeholder="0.0000"
                step="0.0001"
                min="0.0001"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Price</label>
              <input
                type="number"
                value={form.price}
                onChange={(e) => handleChange('price', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
                placeholder="0.00"
                step="0.01"
                min="0.01"
                required
              />
            </div>
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
              <option value="JPY">JPY</option>
            </select>
          </div>

          {/* Amount preview */}
          <div className="bg-gray-50 rounded-lg p-4">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Total Amount</span>
              <span className="text-2xl font-bold">
                {amount.toLocaleString('en-US', { style: 'currency', currency: form.currencyCode })}
              </span>
            </div>
          </div>

          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full py-3 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50 transition-colors"
          >
            {mutation.isPending ? 'Submitting...' : 'Submit Transaction'}
          </button>
        </form>
      </div>
    </div>
  );
}
