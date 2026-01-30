import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Send, AlertCircle, CheckCircle } from 'lucide-react';
import { portfolioApi, transactionApi } from '../api';
import type { Portfolio, TransactionRequest } from '../types';

function TransactionForm() {
  const navigate = useNavigate();
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [formData, setFormData] = useState<TransactionRequest>({
    portfolioId: '',
    investmentId: '',
    type: 'BUY',
    quantity: 0,
    price: 0,
    amount: 0,
    currency: 'USD',
    userId: 'WEBUSER',
  });

  useEffect(() => {
    loadPortfolios();
  }, []);

  const loadPortfolios = async () => {
    try {
      setLoading(true);
      const data = await portfolioApi.getAll();
      setPortfolios(data.filter(p => p.status === 'ACTIVE'));
    } catch (err) {
      console.error('Error loading portfolios:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!formData.portfolioId) {
      setError('Please select a portfolio');
      return;
    }

    if (formData.quantity <= 0) {
      setError('Quantity must be greater than 0');
      return;
    }

    if (formData.type !== 'TRANSFER' && (formData.price === undefined || formData.price <= 0)) {
      setError('Price must be greater than 0');
      return;
    }

    try {
      setSubmitting(true);
      const result = await transactionApi.process(formData);
      setSuccess(`Transaction processed successfully. Status: ${result.status}`);
      setFormData({
        portfolioId: '',
        investmentId: '',
        type: 'BUY',
        quantity: 0,
        price: 0,
        amount: 0,
        currency: 'USD',
        userId: 'WEBUSER',
      });
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to process transaction';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'quantity' || name === 'price' || name === 'amount' 
        ? parseFloat(value) || 0 
        : value,
    }));
  };

  useEffect(() => {
    if (formData.quantity > 0 && formData.price && formData.price > 0) {
      setFormData(prev => ({
        ...prev,
        amount: prev.quantity * (prev.price || 0),
      }));
    }
  }, [formData.quantity, formData.price]);

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-white">New Transaction</h1>
        <p className="mt-1 text-[#94A3B8]">Process a buy, sell, or fee transaction</p>
      </div>

      {error && (
        <div className="bg-[#F87171]/10 border border-[#F87171]/30 rounded-xl p-4 flex items-center">
          <AlertCircle className="h-5 w-5 text-[#F87171] mr-3 flex-shrink-0" />
          <span className="text-[#F87171]">{error}</span>
        </div>
      )}

      {success && (
        <div className="bg-[#4ADE80]/10 border border-[#4ADE80]/30 rounded-xl p-4 flex items-center">
          <CheckCircle className="h-5 w-5 text-[#4ADE80] mr-3 flex-shrink-0" />
          <span className="text-[#4ADE80]">{success}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-[#1E293B] rounded-xl border border-[#334155] p-6 space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
              Portfolio
            </label>
            <select
              name="portfolioId"
              value={formData.portfolioId}
              onChange={handleInputChange}
              className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
              disabled={loading}
            >
              <option value="">Select a portfolio</option>
              {portfolios.map(p => (
                <option key={p.portfolioId} value={p.portfolioId}>
                  {p.portfolioId} - {p.clientName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
              Transaction Type
            </label>
            <select
              name="type"
              value={formData.type}
              onChange={handleInputChange}
              className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
            >
              <option value="BUY">Buy (BU)</option>
              <option value="SELL">Sell (SL)</option>
              <option value="FEE">Fee (FE)</option>
              <option value="TRANSFER">Transfer (TR)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
              Investment ID
            </label>
            <input
              type="text"
              name="investmentId"
              value={formData.investmentId}
              onChange={handleInputChange}
              placeholder="e.g., AAPL, GOOGL"
              className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
              Currency
            </label>
            <select
              name="currency"
              value={formData.currency}
              onChange={handleInputChange}
              className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
            >
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
              <option value="GBP">GBP</option>
              <option value="JPY">JPY</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
              Quantity
            </label>
            <input
              type="number"
              name="quantity"
              value={formData.quantity || ''}
              onChange={handleInputChange}
              step="0.0001"
              min="0"
              placeholder="0.0000"
              className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
              Price per Unit
            </label>
            <input
              type="number"
              name="price"
              value={formData.price || ''}
              onChange={handleInputChange}
              step="0.01"
              min="0"
              placeholder="0.00"
              className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]"
            />
          </div>
        </div>

        <div className="bg-[#0F172A] rounded-lg p-4 border border-[#334155]">
          <div className="flex justify-between items-center">
            <span className="text-[#94A3B8]">Total Amount</span>
            <span className="text-2xl font-bold text-white">
              ${formData.amount?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}
            </span>
          </div>
        </div>

        <div className="flex gap-4">
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 flex items-center justify-center px-6 py-3 bg-[#22D3EE] text-[#0F172A] rounded-lg font-medium hover:bg-[#22D3EE]/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Send className="h-4 w-4 mr-2" />
            {submitting ? 'Processing...' : 'Submit Transaction'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/transactions')}
            className="px-6 py-3 bg-[#334155] text-white rounded-lg font-medium hover:bg-[#475569] transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>

      <div className="bg-[#1E293B] rounded-xl border border-[#334155] p-6">
        <h3 className="text-lg font-medium text-white mb-4">Transaction Types</h3>
        <div className="space-y-3 text-sm">
          <div className="flex items-start">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#4ADE80]/20 text-[#4ADE80] mr-3">BUY</span>
            <span className="text-[#CBD5E1]">Purchase units - increases total units and cost basis</span>
          </div>
          <div className="flex items-start">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#F87171]/20 text-[#F87171] mr-3">SELL</span>
            <span className="text-[#CBD5E1]">Sell units - decreases total units (must have sufficient units)</span>
          </div>
          <div className="flex items-start">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#FBBF24]/20 text-[#FBBF24] mr-3">FEE</span>
            <span className="text-[#CBD5E1]">Deduct fees - reduces cost basis without affecting units</span>
          </div>
          <div className="flex items-start">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#60A5FA]/20 text-[#60A5FA] mr-3">TRANSFER</span>
            <span className="text-[#CBD5E1]">Transfer between portfolios (not yet implemented)</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default TransactionForm;
