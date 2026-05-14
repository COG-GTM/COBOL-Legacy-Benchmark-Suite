import { useState, type FormEvent } from 'react';
import { usePortfolio } from '../context/PortfolioContext';
import { useToast } from '../context/ToastContext';
import FormField from '../components/FormField';
import ConfirmDialog from '../components/ConfirmDialog';
import type { Transaction, TransactionType } from '../types';

interface FormErrors {
  portfolioId?: string;
  type?: string;
  quantity?: string;
  price?: string;
  amount?: string;
}

export default function TransactionEntry() {
  const { portfolios, addTransaction } = usePortfolio();
  const { addToast } = useToast();

  const [portfolioId, setPortfolioId] = useState('');
  const [type, setType] = useState<TransactionType>('BU');
  const [investmentId, setInvestmentId] = useState('');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [amount, setAmount] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [showConfirm, setShowConfirm] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    if (!portfolioId.trim()) {
      newErrors.portfolioId = 'Portfolio ID is required';
    } else if (!portfolios.find((p) => p.id === portfolioId)) {
      newErrors.portfolioId = `Invalid Portfolio ID: ${portfolioId}`;
    }

    const validTypes: TransactionType[] = ['BU', 'SL', 'TR', 'FE'];
    if (!validTypes.includes(type)) {
      newErrors.type = 'Transaction type must be one of: BU, SL, TR, FE';
    }

    const qty = Number(quantity);
    if (!quantity || isNaN(qty) || qty <= 0) {
      newErrors.quantity = 'Quantity must be greater than zero';
    }

    const prc = Number(price);
    if (type !== 'TR' && (!price || isNaN(prc) || prc <= 0)) {
      newErrors.price = 'Price must be greater than zero';
    }

    const amt = Number(amount);
    if (type !== 'TR' && (!amount || isNaN(amt) || amt <= 0)) {
      newErrors.amount = 'Amount must be greater than zero';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setShowConfirm(true);
  };

  const handleConfirm = () => {
    const now = new Date();
    const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`;
    const timeStr = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}${String(now.getSeconds()).padStart(2, '0')}`;

    const transaction: Transaction = {
      date: dateStr,
      time: timeStr,
      portfolioId,
      sequenceNo: String(Math.floor(Math.random() * 999999)).padStart(6, '0'),
      investmentId: investmentId || 'FUND001',
      type,
      quantity: Number(quantity),
      price: Number(price) || 0,
      amount: Number(amount) || 0,
      currency: 'USD',
      status: 'D',
      processDate: now.toISOString(),
      processUser: 'USR00001',
    };

    addTransaction(transaction);
    setShowConfirm(false);
    setSubmitted(true);
    addToast('Transaction submitted successfully.', 'success');
  };

  const handleReset = () => {
    setPortfolioId('');
    setType('BU');
    setInvestmentId('');
    setQuantity('');
    setPrice('');
    setAmount('');
    setErrors({});
    setSubmitted(false);
  };

  const typeLabels: Record<TransactionType, string> = {
    BU: 'Buy',
    SL: 'Sell',
    TR: 'Transfer',
    FE: 'Fee',
  };

  if (submitted) {
    return (
      <div className="max-w-2xl mx-auto">
        <div className="bg-green-50 border border-green-200 rounded-lg p-8 text-center">
          <h2 className="text-xl font-semibold text-green-800 mb-2">Transaction Submitted</h2>
          <p className="text-green-600 mb-6">
            Your {typeLabels[type]} transaction for portfolio {portfolioId} has been processed
            successfully.
          </p>
          <button
            onClick={handleReset}
            className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            Enter Another Transaction
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Transaction Entry</h1>

      <div className="bg-white rounded-lg shadow-sm border p-6">
        <form onSubmit={handleSubmit}>
          <FormField
            label="Portfolio ID"
            value={portfolioId}
            onChange={(e) => {
              setPortfolioId((e.target as HTMLInputElement).value.toUpperCase());
              setErrors((prev) => ({ ...prev, portfolioId: undefined }));
            }}
            error={errors.portfolioId}
            placeholder="e.g. PORT0001"
            maxLength={8}
          />
          <FormField
            label="Transaction Type"
            as="select"
            value={type}
            onChange={(e) => setType((e.target as HTMLSelectElement).value as TransactionType)}
            error={errors.type}
          >
            <option value="BU">Buy</option>
            <option value="SL">Sell</option>
            <option value="TR">Transfer</option>
            <option value="FE">Fee</option>
          </FormField>
          <FormField
            label="Investment ID"
            value={investmentId}
            onChange={(e) => setInvestmentId((e.target as HTMLInputElement).value)}
            placeholder="e.g. FUND001"
          />
          <FormField
            label="Quantity"
            type="number"
            step="0.0001"
            value={quantity}
            onChange={(e) => {
              setQuantity((e.target as HTMLInputElement).value);
              setErrors((prev) => ({ ...prev, quantity: undefined }));
            }}
            error={errors.quantity}
            placeholder="0.0000"
          />
          <FormField
            label="Price"
            type="number"
            step="0.01"
            value={price}
            onChange={(e) => {
              setPrice((e.target as HTMLInputElement).value);
              setErrors((prev) => ({ ...prev, price: undefined }));
            }}
            error={errors.price}
            placeholder="0.00"
          />
          <FormField
            label="Amount"
            type="number"
            step="0.01"
            value={amount}
            onChange={(e) => {
              setAmount((e.target as HTMLInputElement).value);
              setErrors((prev) => ({ ...prev, amount: undefined }));
            }}
            error={errors.amount}
            placeholder="0.00"
          />

          <div className="flex justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={handleReset}
              className="px-4 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
            >
              Clear
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Submit Transaction
            </button>
          </div>
        </form>
      </div>

      {showConfirm && (
        <ConfirmDialog
          title="Confirm Transaction"
          message={`Submit ${typeLabels[type]} transaction for portfolio ${portfolioId}?\n\nQuantity: ${quantity}\nPrice: $${price || '0'}\nAmount: $${amount || '0'}`}
          confirmLabel="Submit"
          onConfirm={handleConfirm}
          onCancel={() => setShowConfirm(false)}
        />
      )}
    </div>
  );
}
