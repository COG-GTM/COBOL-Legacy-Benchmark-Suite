import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowLeft, ArrowRight, Check, Info } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { PageHeader } from '@/components/ui/PageHeader';
import { getTransTypeLabel } from '@/components/ui/StatusBadge';
import { portfolios } from '@/data/mockData';
import {
  accountNoForPortfolio,
  processTransaction,
  useTransactionStore,
} from './transactionStore';

type EntryType = 'BU' | 'SL' | 'TR' | 'FE';

const currencyFormat = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

const quantityFormat = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

// Strict numeric validation, mirroring COBOL IS NUMERIC checks
const QUANTITY_PATTERN = /^\d+(\.\d{1,3})?$/;
const MONEY_PATTERN = /^\d+(\.\d{1,2})?$/;

const inputClass =
  'w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500';

const TYPE_CHOICES: { code: EntryType; description: string }[] = [
  { code: 'BU', description: 'Purchase units and add to cost basis' },
  { code: 'SL', description: 'Sell units from an existing position' },
  { code: 'TR', description: 'Transfer between portfolios' },
  { code: 'FE', description: 'Charge a fee against cost basis' },
];

const STEPS = ['Portfolio & Type', 'Amounts', 'Review & Confirm'];

interface FormErrors {
  portfolioId?: string;
  transType?: string;
  quantity?: string;
  price?: string;
  amount?: string;
}

export function TransactionNewPage() {
  const navigate = useNavigate();
  const { positions } = useTransactionStore();

  const [step, setStep] = useState(1);
  const [portfolioId, setPortfolioId] = useState('');
  const [transType, setTransType] = useState<EntryType | ''>('');
  const [fundId, setFundId] = useState('');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [amount, setAmount] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const fundOptions = useMemo(() => {
    const all = new Set(positions.map((p) => p.fundId));
    if (transType === 'BU') return [...all].sort();
    if (!portfolioId) return [];
    const accountNo = accountNoForPortfolio(portfolioId);
    return positions
      .filter((p) => p.accountNo === accountNo && p.status === 'A')
      .map((p) => p.fundId)
      .sort();
  }, [positions, transType, portfolioId]);

  const currentPosition = useMemo(() => {
    if (!portfolioId || !fundId) return null;
    const accountNo = accountNoForPortfolio(portfolioId);
    return positions.find((p) => p.accountNo === accountNo && p.fundId === fundId) ?? null;
  }, [positions, portfolioId, fundId]);

  const isFee = transType === 'FE';

  // 2110-CHECK-PORTFOLIO and 2120-CHECK-TRANSACTION-TYPE
  const validateStep1 = (): boolean => {
    const next: FormErrors = {};
    if (!portfolioId) {
      next.portfolioId = 'Portfolio ID is required';
    } else if (!portfolios.some((p) => p.id === portfolioId)) {
      next.portfolioId = `Invalid Portfolio ID: ${portfolioId}`;
    }
    if (!transType || !['BU', 'SL', 'TR', 'FE'].includes(transType)) {
      next.transType = transType
        ? `Invalid Transaction Type: ${transType}`
        : 'Transaction type is required';
    } else if (transType === 'TR') {
      next.transType = 'Transfer processing not implemented';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  // 2130-CHECK-AMOUNTS
  const validateStep2 = (): boolean => {
    const next: FormErrors = {};
    if (!fundId) {
      next.quantity = 'Fund is required';
    }
    if (!isFee) {
      if (!QUANTITY_PATTERN.test(quantity)) {
        next.quantity = 'Quantity must be numeric (up to 3 decimal places)';
      } else if (Number(quantity) <= 0) {
        next.quantity = 'Quantity must be greater than zero';
      }
      if (!MONEY_PATTERN.test(price)) {
        next.price = 'Price must be numeric (up to 2 decimal places)';
      } else if (Number(price) <= 0) {
        next.price = 'Price must be greater than zero';
      }
    }
    if (!MONEY_PATTERN.test(amount)) {
      next.amount = 'Amount must be numeric (up to 2 decimal places)';
    } else if (Number(amount) <= 0) {
      next.amount = 'Amount must be greater than zero';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleNext = () => {
    if (step === 1 && validateStep1()) {
      setStep(2);
    } else if (step === 2 && validateStep2()) {
      setStep(3);
    }
  };

  const handleBack = () => {
    setSubmitError('');
    setStep((s) => Math.max(1, s - 1));
  };

  const computeAmount = () => {
    if (QUANTITY_PATTERN.test(quantity) && MONEY_PATTERN.test(price)) {
      setAmount((Number(quantity) * Number(price)).toFixed(2));
    }
  };

  // 2200-UPDATE-POSITIONS on confirm
  const handleConfirm = () => {
    setConfirmOpen(false);
    if (!transType || transType === 'TR') return;
    const result = processTransaction({
      portfolioId,
      fundId,
      transType,
      shareQty: isFee ? 0 : Number(quantity),
      price: isFee ? 0 : Number(price),
      amount: Number(amount),
    });
    if (result.ok) {
      navigate('/transactions');
    } else {
      setSubmitError(result.error ?? 'Transaction failed');
    }
  };

  const selectedPortfolio = portfolios.find((p) => p.id === portfolioId);

  return (
    <div className="max-w-3xl">
      <PageHeader
        title="New Transaction"
        description="Create a new buy, sell, or fee transaction"
        actions={
          <Link
            to="/transactions"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to History
          </Link>
        }
      />

      <ol className="flex items-center gap-2 mb-6">
        {STEPS.map((label, i) => {
          const n = i + 1;
          const active = step === n;
          const done = step > n;
          return (
            <li key={label} className="flex items-center gap-2">
              <span
                className={`flex items-center justify-center w-7 h-7 rounded-full text-xs font-semibold ${
                  done
                    ? 'bg-emerald-600 text-white'
                    : active
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-200 text-slate-600'
                }`}
              >
                {done ? <Check className="w-4 h-4" /> : n}
              </span>
              <span className={`text-sm ${active ? 'font-semibold text-slate-900' : 'text-slate-500'}`}>
                {label}
              </span>
              {n < STEPS.length && <span className="w-8 h-px bg-slate-300" />}
            </li>
          );
        })}
      </ol>

      {step === 1 && (
        <Card title="Step 1: Portfolio & Transaction Type">
          <div className="space-y-5">
            <div>
              <label htmlFor="portfolio" className="block text-sm font-medium text-slate-700 mb-1">
                Portfolio
              </label>
              <select
                id="portfolio"
                value={portfolioId}
                onChange={(e) => {
                  setPortfolioId(e.target.value);
                  setFundId('');
                }}
                className={inputClass}
              >
                <option value="">Select a portfolio...</option>
                {portfolios
                  .filter((p) => p.status === 'A')
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.id} — {p.name}
                    </option>
                  ))}
              </select>
              {errors.portfolioId && (
                <p className="mt-1 text-sm text-red-600">{errors.portfolioId}</p>
              )}
            </div>

            <div>
              <span className="block text-sm font-medium text-slate-700 mb-2">
                Transaction Type
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {TYPE_CHOICES.map((choice) => {
                  const disabled = choice.code === 'TR';
                  const selected = transType === choice.code;
                  return (
                    <button
                      key={choice.code}
                      type="button"
                      disabled={disabled}
                      onClick={() => {
                        setTransType(choice.code);
                        setFundId('');
                      }}
                      className={`text-left p-4 rounded-lg border transition-colors ${
                        disabled
                          ? 'border-slate-200 bg-slate-50 opacity-60 cursor-not-allowed'
                          : selected
                            ? 'border-blue-600 bg-blue-50 ring-1 ring-blue-600'
                            : 'border-slate-300 bg-white hover:border-blue-400'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-900">
                          {getTransTypeLabel(choice.code)}{' '}
                          <span className="text-xs font-normal text-slate-400">
                            ({choice.code})
                          </span>
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-slate-500">{choice.description}</p>
                      {disabled && (
                        <p className="mt-2 inline-flex items-center gap-1 text-xs text-amber-700">
                          <Info className="w-3.5 h-3.5" />
                          Transfer processing not implemented
                        </p>
                      )}
                    </button>
                  );
                })}
              </div>
              {errors.transType && <p className="mt-2 text-sm text-red-600">{errors.transType}</p>}
            </div>

            <div className="flex justify-end">
              <button
                onClick={handleNext}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
              >
                Next
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </Card>
      )}

      {step === 2 && (
        <Card title="Step 2: Quantity, Price & Amount">
          <div className="space-y-5">
            <div>
              <label htmlFor="fund" className="block text-sm font-medium text-slate-700 mb-1">
                Fund
              </label>
              <select
                id="fund"
                value={fundId}
                onChange={(e) => setFundId(e.target.value)}
                className={inputClass}
              >
                <option value="">Select a fund...</option>
                {fundOptions.map((f) => (
                  <option key={f} value={f}>
                    {f}
                  </option>
                ))}
              </select>
              {currentPosition && (
                <p className="mt-1 text-xs text-slate-500">
                  Current position: {quantityFormat.format(currentPosition.shareBalance)} units,
                  cost basis {currencyFormat.format(currentPosition.costBasis)}
                </p>
              )}
            </div>

            {!isFee && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="quantity" className="block text-sm font-medium text-slate-700 mb-1">
                    Quantity
                  </label>
                  <input
                    id="quantity"
                    type="text"
                    inputMode="decimal"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    placeholder="0.000"
                    className={inputClass}
                  />
                  {errors.quantity && (
                    <p className="mt-1 text-sm text-red-600">{errors.quantity}</p>
                  )}
                </div>
                <div>
                  <label htmlFor="price" className="block text-sm font-medium text-slate-700 mb-1">
                    Price
                  </label>
                  <input
                    id="price"
                    type="text"
                    inputMode="decimal"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    onBlur={computeAmount}
                    placeholder="0.00"
                    className={inputClass}
                  />
                  {errors.price && <p className="mt-1 text-sm text-red-600">{errors.price}</p>}
                </div>
              </div>
            )}

            <div>
              <label htmlFor="amount" className="block text-sm font-medium text-slate-700 mb-1">
                Amount {isFee ? '(fee amount)' : '(quantity × price)'}
              </label>
              <input
                id="amount"
                type="text"
                inputMode="decimal"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
                className={inputClass}
              />
              {errors.amount && <p className="mt-1 text-sm text-red-600">{errors.amount}</p>}
              {isFee && errors.quantity && (
                <p className="mt-1 text-sm text-red-600">{errors.quantity}</p>
              )}
            </div>

            <div className="flex justify-between">
              <button
                onClick={handleBack}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
              >
                <ArrowLeft className="w-4 h-4" />
                Back
              </button>
              <button
                onClick={handleNext}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
              >
                Next
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </Card>
      )}

      {step === 3 && (
        <Card title="Step 3: Review & Confirm">
          <div className="space-y-5">
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Portfolio</dt>
                <dd className="mt-1 text-sm text-slate-900">
                  {selectedPortfolio ? `${selectedPortfolio.id} — ${selectedPortfolio.name}` : portfolioId}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Account</dt>
                <dd className="mt-1 text-sm text-slate-900">
                  {portfolioId ? accountNoForPortfolio(portfolioId) : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Type</dt>
                <dd className="mt-1 text-sm text-slate-900">
                  {transType ? `${getTransTypeLabel(transType)} (${transType})` : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Fund</dt>
                <dd className="mt-1 text-sm text-slate-900">{fundId || '—'}</dd>
              </div>
              {!isFee && (
                <>
                  <div>
                    <dt className="text-xs font-medium text-slate-500 uppercase">Quantity</dt>
                    <dd className="mt-1 text-sm text-slate-900">
                      {quantityFormat.format(Number(quantity))}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs font-medium text-slate-500 uppercase">Price</dt>
                    <dd className="mt-1 text-sm text-slate-900">
                      {currencyFormat.format(Number(price))}
                    </dd>
                  </div>
                </>
              )}
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Amount</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-900">
                  {currencyFormat.format(Number(amount))}
                </dd>
              </div>
              {currentPosition && (
                <div>
                  <dt className="text-xs font-medium text-slate-500 uppercase">Current Units</dt>
                  <dd className="mt-1 text-sm text-slate-900">
                    {quantityFormat.format(currentPosition.shareBalance)}
                  </dd>
                </div>
              )}
            </dl>

            {transType === 'SL' &&
              currentPosition &&
              currentPosition.shareBalance < Number(quantity) && (
                <div className="flex items-start gap-2 p-3 rounded-lg bg-amber-50 border border-amber-200 text-sm text-amber-800">
                  <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                  Sale quantity exceeds the current position balance. The transaction will be
                  rejected with &quot;Insufficient units for sale&quot;.
                </div>
              )}

            {submitError && (
              <div className="flex items-start gap-2 p-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">
                <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                {submitError}
              </div>
            )}

            <div className="flex justify-between">
              <button
                onClick={handleBack}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
              >
                <ArrowLeft className="w-4 h-4" />
                Back
              </button>
              <button
                onClick={() => setConfirmOpen(true)}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
              >
                <Check className="w-4 h-4" />
                Confirm Transaction
              </button>
            </div>
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title="Confirm Transaction"
        message={`Process ${transType ? getTransTypeLabel(transType) : ''} transaction for ${currencyFormat.format(Number(amount) || 0)} on ${portfolioId}? Positions will be updated and an audit entry created.`}
        confirmLabel="Process Transaction"
        onConfirm={handleConfirm}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
