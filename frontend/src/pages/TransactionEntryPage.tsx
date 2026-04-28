import { useState, type FormEvent } from 'react';
import { mockPortfolios, mockPositions } from '../mocks/mockData';
import type { TransactionType, InvestmentType } from '../types';
import { TRANSACTION_TYPE_LABELS, INVESTMENT_TYPE_LABELS } from '../types';
import { InlineError } from '../components/InlineError';
import { useToast } from '../hooks/useToast';
import { validatePortfolioId, validateAmount, formatCurrency } from '../utils/validation';

/**
 * Maps to PORTTRAN from PORTTRAN.cbl lines 102-118
 */
export function TransactionEntryPage() {
  const [portfolioId, setPortfolioId] = useState('');
  const [transType, setTransType] = useState<TransactionType>('BU');
  const [investmentType, setInvestmentType] = useState<InvestmentType>('STK');
  const [units, setUnits] = useState('');
  const [price, setPrice] = useState('');
  const [amount, setAmount] = useState('');
  const [targetPortfolio, setTargetPortfolio] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [showConfirm, setShowConfirm] = useState(false);
  const { addToast } = useToast();

  const portfolio = mockPortfolios.find(p => p.portfolioId === portfolioId);
  const positions = mockPositions.filter(p => p.portfolioId === portfolioId);

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    const idResult = validatePortfolioId(portfolioId);
    if (!idResult.valid) errs.portfolioId = idResult.error;
    else if (!portfolio) errs.portfolioId = 'Portfolio not found';

    const unitsVal = parseFloat(units);
    if (isNaN(unitsVal) || unitsVal <= 0) errs.units = 'Quantity must be greater than zero';

    const priceVal = parseFloat(price);
    if (transType !== 'TR' && (isNaN(priceVal) || priceVal <= 0)) {
      errs.price = 'Price must be greater than zero';
    }

    const amountVal = parseFloat(amount);
    const amountResult = validateAmount(amountVal);
    if (!amountResult.valid) errs.amount = amountResult.error;
    if (transType !== 'TR' && (isNaN(amountVal) || amountVal <= 0)) {
      errs.amount = 'Amount must be greater than zero';
    }

    if (transType === 'SL') {
      const totalHoldings = positions.reduce((sum, p) => sum + p.quantity, 0);
      if (unitsVal > totalHoldings) {
        errs.units = `Sell cannot exceed holdings (${totalHoldings} units available)`;
      }
    }

    if (transType === 'TR') {
      if (!targetPortfolio.trim()) errs.targetPortfolio = 'Target portfolio required for transfers';
      else {
        const tResult = validatePortfolioId(targetPortfolio);
        if (!tResult.valid) errs.targetPortfolio = tResult.error;
      }
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setShowConfirm(true);
  };

  const handleConfirm = () => {
    addToast('Transaction submitted successfully (placeholder - no backend)', 'success');
    setShowConfirm(false);
    setPortfolioId('');
    setUnits('');
    setPrice('');
    setAmount('');
    setTargetPortfolio('');
  };

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Transaction Entry</h1>

      <form onSubmit={handleSubmit} className="space-y-4 max-w-lg">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Portfolio ID</label>
          <input
            type="text"
            value={portfolioId}
            onChange={e => setPortfolioId(e.target.value.toUpperCase())}
            maxLength={8}
            placeholder="PORT0000"
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <InlineError message={errors.portfolioId ?? ''} />
          {portfolio && (
            <p className="text-xs text-green-600 mt-1">
              Found: {portfolio.clientName} (Value: {formatCurrency(portfolio.totalValue)})
            </p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Transaction Type</label>
          <select
            value={transType}
            onChange={e => setTransType(e.target.value as TransactionType)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {(Object.entries(TRANSACTION_TYPE_LABELS) as [TransactionType, string][]).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Investment Type</label>
          <select
            value={investmentType}
            onChange={e => setInvestmentType(e.target.value as InvestmentType)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {(Object.entries(INVESTMENT_TYPE_LABELS) as [InvestmentType, string][]).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Units</label>
          <input
            type="number"
            step="0.0001"
            value={units}
            onChange={e => setUnits(e.target.value)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <InlineError message={errors.units ?? ''} />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Price</label>
          <input
            type="number"
            step="0.0001"
            value={price}
            onChange={e => setPrice(e.target.value)}
            disabled={transType === 'TR'}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full disabled:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <InlineError message={errors.price ?? ''} />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Amount</label>
          <input
            type="number"
            step="0.01"
            value={amount}
            onChange={e => setAmount(e.target.value)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <InlineError message={errors.amount ?? ''} />
        </div>

        {transType === 'TR' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Target Portfolio ID</label>
            <input
              type="text"
              value={targetPortfolio}
              onChange={e => setTargetPortfolio(e.target.value.toUpperCase())}
              maxLength={8}
              placeholder="PORT0000"
              className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <InlineError message={errors.targetPortfolio ?? ''} />
          </div>
        )}

        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700 transition-colors"
        >
          Submit Transaction
        </button>
      </form>

      {/* Confirmation dialog */}
      {showConfirm && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-bold mb-4">Confirm Transaction</h3>
            <div className="text-sm space-y-2 mb-4">
              <p>Portfolio: <strong>{portfolioId}</strong></p>
              <p>Type: <strong>{TRANSACTION_TYPE_LABELS[transType]}</strong></p>
              <p>Units: <strong>{units}</strong></p>
              <p>Price: <strong>{formatCurrency(parseFloat(price) || 0)}</strong></p>
              <p>Amount: <strong>{formatCurrency(parseFloat(amount) || 0)}</strong></p>
              {transType === 'TR' && <p>Target: <strong>{targetPortfolio}</strong></p>}
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowConfirm(false)}
                className="px-4 py-2 text-sm bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirm}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
