import { useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getTransTypeLabel } from '@/components/ui/StatusBadge';
import { useTransactions } from '@/context/TransactionContext';
import { portfolios } from '@/data/mockData';
import { AlertTriangle, ArrowLeft, ArrowRight, Check, X } from 'lucide-react';

type TransactionType = 'BY' | 'SL' | 'FE' | 'TR';

interface FormData {
  portfolioId: string;
  transType: TransactionType;
  fundId: string;
  transDate: string;
  quantity: string;
  price: string;
  amount: string;
}

interface FormErrors {
  portfolioId?: string;
  transType?: string;
  fundId?: string;
  transDate?: string;
  quantity?: string;
  price?: string;
  amount?: string;
  sell?: string;
}

const STEPS = ['Transaction Details', 'Amount Details', 'Review & Confirm'] as const;

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
  }).format(value);
}

function formatNumber(value: number, decimals: number = 3): string {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
}

function getTodayString(): string {
  const d = new Date();
  return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`;
}

const TRANS_TYPE_CONFIG: {
  value: TransactionType;
  label: string;
  code: string;
  color: string;
  bgColor: string;
  ringColor: string;
  disabled?: boolean;
  tooltip?: string;
}[] = [
  { value: 'BY', label: 'Buy', code: 'BY', color: 'text-emerald-700', bgColor: 'bg-emerald-50', ringColor: 'ring-emerald-300' },
  { value: 'SL', label: 'Sell', code: 'SL', color: 'text-red-700', bgColor: 'bg-red-50', ringColor: 'ring-red-300' },
  { value: 'TR', label: 'Transfer', code: 'TR', color: 'text-blue-700', bgColor: 'bg-blue-50', ringColor: 'ring-blue-300', disabled: true, tooltip: 'Coming soon' },
  { value: 'FE', label: 'Fee', code: 'FE', color: 'text-amber-700', bgColor: 'bg-amber-50', ringColor: 'ring-amber-300' },
];

export function TransactionNewPage() {
  const navigate = useNavigate();
  const { getPositionBalance, getAccountNoForPortfolio, submitTransaction } = useTransactions();
  const [currentStep, setCurrentStep] = useState(0);
  const [errors, setErrors] = useState<FormErrors>({});
  const [formData, setFormData] = useState<FormData>({
    portfolioId: '',
    transType: 'BY',
    fundId: '',
    transDate: getTodayString(),
    quantity: '',
    price: '',
    amount: '',
  });

  const activePortfolios = useMemo(
    () => portfolios.filter((p) => p.status === 'A'),
    []
  );

  const accountNo = useMemo(
    () => (formData.portfolioId ? getAccountNoForPortfolio(formData.portfolioId) : null),
    [formData.portfolioId, getAccountNoForPortfolio]
  );

  const currentBalance = useMemo(() => {
    if (!accountNo || !formData.fundId) return 0;
    return getPositionBalance(accountNo, formData.fundId);
  }, [accountNo, formData.fundId, getPositionBalance]);

  const parsedQuantity = parseFloat(formData.quantity) || 0;
  const parsedPrice = parseFloat(formData.price) || 0;
  const parsedAmount = parseFloat(formData.amount) || 0;

  const updateField = useCallback(
    (field: keyof FormData, value: string) => {
      setFormData((prev) => {
        const next = { ...prev, [field]: value };

        if (field === 'quantity' || field === 'price') {
          const qty = parseFloat(field === 'quantity' ? value : prev.quantity) || 0;
          const prc = parseFloat(field === 'price' ? value : prev.price) || 0;
          if (qty > 0 && prc > 0) {
            next.amount = (qty * prc).toFixed(2);
          }
        }

        if (field === 'amount') {
          const qty = parseFloat(prev.quantity) || 0;
          const amt = parseFloat(value) || 0;
          if (qty > 0 && amt > 0) {
            next.price = (amt / qty).toFixed(4);
          }
        }

        return next;
      });
      setErrors((prev) => ({ ...prev, [field]: undefined, sell: undefined }));
    },
    []
  );

  // --- Validation (mirrors COBOL PORTTRAN 2100-VALIDATE-TRANSACTION) ---

  const validateStep1 = useCallback((): boolean => {
    const newErrors: FormErrors = {};

    // 2110-CHECK-PORTFOLIO
    if (!formData.portfolioId) {
      newErrors.portfolioId = 'Portfolio ID is required';
    } else {
      const found = portfolios.find((p) => p.id === formData.portfolioId);
      if (!found) {
        newErrors.portfolioId = `Invalid Portfolio ID: ${formData.portfolioId}`;
      }
    }

    // 2120-CHECK-TRANSACTION-TYPE
    if (formData.transType === 'TR') {
      newErrors.transType = 'Transfer processing is not yet available';
    }

    if (!formData.fundId.trim()) {
      newErrors.fundId = 'Fund ID is required';
    } else if (formData.fundId.trim().length > 6) {
      newErrors.fundId = 'Fund ID must be 6 characters or fewer';
    }

    if (!formData.transDate) {
      newErrors.transDate = 'Transaction date is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData.portfolioId, formData.transType, formData.fundId, formData.transDate]);

  const validateStep2 = useCallback((): boolean => {
    const newErrors: FormErrors = {};
    const isFee = formData.transType === 'FE';

    // 2130-CHECK-AMOUNTS
    if (!isFee && parsedQuantity <= 0) {
      newErrors.quantity = 'Quantity must be greater than zero';
    }

    if (!isFee && parsedPrice <= 0) {
      newErrors.price = 'Price must be greater than zero';
    }

    if (parsedAmount <= 0) {
      newErrors.amount = 'Amount must be greater than zero';
    }

    // 2220-PROCESS-SELL
    if (formData.transType === 'SL' && accountNo && parsedQuantity > 0) {
      if (parsedQuantity > currentBalance) {
        newErrors.sell = `Insufficient units for sale. Available: ${formatNumber(currentBalance)}, Requested: ${formatNumber(parsedQuantity)}`;
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData.transType, parsedQuantity, parsedPrice, parsedAmount, accountNo, currentBalance]);

  const handleNext = useCallback(() => {
    if (currentStep === 0 && validateStep1()) {
      setCurrentStep(1);
    } else if (currentStep === 1 && validateStep2()) {
      setCurrentStep(2);
    }
  }, [currentStep, validateStep1, validateStep2]);

  const handleBack = useCallback(() => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
      setErrors({});
    }
  }, [currentStep]);

  const handleSubmit = useCallback(() => {
    if (!accountNo) {
      setErrors({ portfolioId: 'No account mapping found for this portfolio. Please select a different portfolio.' });
      setCurrentStep(0);
      return;
    }

    const result = submitTransaction({
      accountNo,
      fundId: formData.fundId.trim().toUpperCase(),
      transType: formData.transType as 'BY' | 'SL' | 'FE',
      transDate: formData.transDate,
      shareQty: parsedQuantity,
      price: parsedPrice,
      amount: parsedAmount,
    });

    navigate('/transactions/confirmation', {
      state: {
        transId: result.transId,
        portfolioId: formData.portfolioId,
        portfolioName: portfolios.find((p) => p.id === formData.portfolioId)?.name ?? '',
        accountNo,
        fundId: formData.fundId.trim().toUpperCase(),
        transType: formData.transType,
        transDate: formData.transDate,
        shareQty: parsedQuantity,
        price: parsedPrice,
        amount: parsedAmount,
        beforeBalance: result.beforeBalance,
        afterBalance: result.afterBalance,
      },
    });
  }, [accountNo, formData, parsedQuantity, parsedPrice, parsedAmount, submitTransaction, navigate]);

  const resultingBalance = useMemo(() => {
    if (formData.transType === 'BY') return currentBalance + parsedQuantity;
    if (formData.transType === 'SL') return currentBalance - parsedQuantity;
    return currentBalance;
  }, [formData.transType, currentBalance, parsedQuantity]);

  return (
    <div>
      <PageHeader
        title="New Transaction"
        description="Create a new buy, sell, or fee transaction (PORTTRAN)"
        actions={
          <button
            onClick={() => navigate('/transactions')}
            className="inline-flex items-center gap-2 rounded-md bg-white px-3.5 py-2 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
          >
            <X className="h-4 w-4" />
            Cancel
          </button>
        }
      />

      {/* Step Indicator */}
      <nav className="mb-8" aria-label="Progress">
        <ol className="flex items-center">
          {STEPS.map((step, idx) => {
            const isCompleted = idx < currentStep;
            const isCurrent = idx === currentStep;
            return (
              <li key={step} className={`relative ${idx < STEPS.length - 1 ? 'flex-1 pr-8' : ''}`}>
                <div className="flex items-center">
                  <span
                    className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-medium ${
                      isCompleted
                        ? 'bg-blue-600 text-white'
                        : isCurrent
                          ? 'border-2 border-blue-600 bg-white text-blue-600'
                          : 'border-2 border-slate-300 bg-white text-slate-500'
                    }`}
                  >
                    {isCompleted ? <Check className="h-4 w-4" /> : idx + 1}
                  </span>
                  <span
                    className={`ml-3 text-sm font-medium whitespace-nowrap ${
                      isCurrent ? 'text-blue-600' : isCompleted ? 'text-slate-900' : 'text-slate-500'
                    }`}
                  >
                    {step}
                  </span>
                  {idx < STEPS.length - 1 && (
                    <div
                      className={`ml-4 h-0.5 w-full ${isCompleted ? 'bg-blue-600' : 'bg-slate-200'}`}
                    />
                  )}
                </div>
              </li>
            );
          })}
        </ol>
      </nav>

      {/* Step 1: Transaction Details */}
      {currentStep === 0 && (
        <Card title="Transaction Details">
          <div className="space-y-6">
            <div>
              <label htmlFor="portfolioId" className="block text-sm font-medium text-slate-700 mb-1">
                Portfolio <span className="text-red-500">*</span>
              </label>
              <select
                id="portfolioId"
                value={formData.portfolioId}
                onChange={(e) => updateField('portfolioId', e.target.value)}
                className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${
                  errors.portfolioId ? 'border-red-300 bg-red-50' : 'border-slate-300'
                }`}
              >
                <option value="">Select a portfolio...</option>
                {activePortfolios.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.id} &mdash; {p.name}
                  </option>
                ))}
              </select>
              {errors.portfolioId && (
                <p className="mt-1 text-sm text-red-600">{errors.portfolioId}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Transaction Type <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                {TRANS_TYPE_CONFIG.map((tt) => {
                  const isSelected = formData.transType === tt.value;
                  return (
                    <div key={tt.value} className="relative">
                      <button
                        type="button"
                        disabled={tt.disabled}
                        onClick={() => updateField('transType', tt.value)}
                        title={tt.disabled ? tt.tooltip : undefined}
                        className={`w-full rounded-lg border-2 px-4 py-3 text-center transition-all ${
                          tt.disabled
                            ? 'cursor-not-allowed border-slate-200 bg-slate-50 opacity-60'
                            : isSelected
                              ? `${tt.bgColor} ${tt.color} border-current ring-2 ${tt.ringColor}`
                              : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                        }`}
                      >
                        <div className="text-sm font-semibold">{tt.label}</div>
                        <div className="mt-0.5 text-xs opacity-70">({tt.code})</div>
                      </button>
                      {tt.disabled && tt.tooltip && (
                        <span className="absolute -top-2 right-2 rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                          {tt.tooltip}
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
              {errors.transType && (
                <p className="mt-1 text-sm text-red-600">{errors.transType}</p>
              )}
            </div>

            <div>
              <label htmlFor="fundId" className="block text-sm font-medium text-slate-700 mb-1">
                Fund ID <span className="text-red-500">*</span>
              </label>
              <input
                id="fundId"
                type="text"
                maxLength={6}
                placeholder="e.g. GRWEQF"
                value={formData.fundId}
                onChange={(e) => updateField('fundId', e.target.value.toUpperCase())}
                className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm uppercase placeholder:normal-case focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${
                  errors.fundId ? 'border-red-300 bg-red-50' : 'border-slate-300'
                }`}
              />
              {errors.fundId && (
                <p className="mt-1 text-sm text-red-600">{errors.fundId}</p>
              )}
              <p className="mt-1 text-xs text-slate-400">6-character fund identifier</p>
            </div>

            <div>
              <label htmlFor="transDate" className="block text-sm font-medium text-slate-700 mb-1">
                Transaction Date <span className="text-red-500">*</span>
              </label>
              <input
                id="transDate"
                type="date"
                value={formData.transDate}
                onChange={(e) => updateField('transDate', e.target.value)}
                className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${
                  errors.transDate ? 'border-red-300 bg-red-50' : 'border-slate-300'
                }`}
              />
              {errors.transDate && (
                <p className="mt-1 text-sm text-red-600">{errors.transDate}</p>
              )}
            </div>
          </div>
        </Card>
      )}

      {/* Step 2: Amount Details */}
      {currentStep === 1 && (
        <Card title="Amount Details">
          <div className="space-y-6">
            {formData.transType === 'FE' && (
              <div className="rounded-md bg-amber-50 p-4 text-sm text-amber-800 ring-1 ring-inset ring-amber-200">
                <strong>Fee Transaction:</strong> Only the amount is required. Quantity and price are not applicable.
              </div>
            )}

            {formData.transType !== 'FE' && (
              <>
                <div>
                  <label htmlFor="quantity" className="block text-sm font-medium text-slate-700 mb-1">
                    Quantity (Shares) <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="quantity"
                    type="number"
                    min="0"
                    step="0.001"
                    placeholder="0.000"
                    value={formData.quantity}
                    onChange={(e) => updateField('quantity', e.target.value)}
                    className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${
                      errors.quantity ? 'border-red-300 bg-red-50' : 'border-slate-300'
                    }`}
                  />
                  {errors.quantity && (
                    <p className="mt-1 text-sm text-red-600">{errors.quantity}</p>
                  )}
                  {formData.transType === 'SL' && accountNo && (
                    <p className="mt-1 text-xs text-slate-500">
                      Available balance: {formatNumber(currentBalance)} shares
                    </p>
                  )}
                </div>

                <div>
                  <label htmlFor="price" className="block text-sm font-medium text-slate-700 mb-1">
                    Price per Share <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">$</span>
                    <input
                      id="price"
                      type="number"
                      min="0"
                      step="0.0001"
                      placeholder="0.0000"
                      value={formData.price}
                      onChange={(e) => updateField('price', e.target.value)}
                      className={`block w-full rounded-md border py-2 pl-7 pr-3 text-sm shadow-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${
                        errors.price ? 'border-red-300 bg-red-50' : 'border-slate-300'
                      }`}
                    />
                  </div>
                  {errors.price && (
                    <p className="mt-1 text-sm text-red-600">{errors.price}</p>
                  )}
                </div>
              </>
            )}

            <div>
              <label htmlFor="amount" className="block text-sm font-medium text-slate-700 mb-1">
                Total Amount <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">$</span>
                <input
                  id="amount"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={formData.amount}
                  onChange={(e) => updateField('amount', e.target.value)}
                  className={`block w-full rounded-md border py-2 pl-7 pr-3 text-sm shadow-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${
                    errors.amount ? 'border-red-300 bg-red-50' : 'border-slate-300'
                  }`}
                />
              </div>
              {errors.amount && (
                <p className="mt-1 text-sm text-red-600">{errors.amount}</p>
              )}
              {formData.transType !== 'FE' && parsedQuantity > 0 && parsedPrice > 0 && (
                <p className="mt-1 text-xs text-slate-500">
                  Auto-calculated: {formatNumber(parsedQuantity)} x {formatCurrency(parsedPrice)} = {formatCurrency(parsedQuantity * parsedPrice)}
                </p>
              )}
            </div>

            {errors.sell && (
              <div className="flex items-start gap-3 rounded-md bg-red-50 p-4 ring-1 ring-inset ring-red-200">
                <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-500" />
                <p className="text-sm text-red-700">{errors.sell}</p>
              </div>
            )}

            {formData.transType === 'SL' && parsedQuantity > 0 && parsedQuantity <= currentBalance && accountNo && (
              <div className="rounded-md bg-slate-50 p-4 ring-1 ring-inset ring-slate-200">
                <h4 className="text-sm font-medium text-slate-700 mb-2">Position Impact Preview</h4>
                <div className="grid grid-cols-3 gap-4 text-center">
                  <div>
                    <p className="text-xs text-slate-500">Current Balance</p>
                    <p className="text-sm font-semibold text-slate-900">{formatNumber(currentBalance)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500">Selling</p>
                    <p className="text-sm font-semibold text-red-600">-{formatNumber(parsedQuantity)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500">Resulting Balance</p>
                    <p className="text-sm font-semibold text-slate-900">{formatNumber(resultingBalance)}</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </Card>
      )}

      {/* Step 3: Review & Confirm */}
      {currentStep === 2 && (
        <Card title="Review & Confirm">
          <div className="space-y-6">
            <div className="rounded-md bg-slate-50 p-6 ring-1 ring-inset ring-slate-200">
              <h4 className="text-sm font-semibold text-slate-900 mb-4">Transaction Summary</h4>
              <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
                <div>
                  <dt className="text-xs text-slate-500">Portfolio</dt>
                  <dd className="mt-0.5 text-sm font-medium text-slate-900">
                    {formData.portfolioId} &mdash; {portfolios.find((p) => p.id === formData.portfolioId)?.name}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500">Account Number</dt>
                  <dd className="mt-0.5 text-sm font-medium text-slate-900">{accountNo}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500">Transaction Type</dt>
                  <dd className="mt-0.5">
                    <StatusBadge
                      label={getTransTypeLabel(formData.transType)}
                      variant={
                        formData.transType === 'BY'
                          ? 'success'
                          : formData.transType === 'SL'
                            ? 'error'
                            : 'warning'
                      }
                    />
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500">Fund ID</dt>
                  <dd className="mt-0.5 text-sm font-medium text-slate-900 font-mono">{formData.fundId.toUpperCase()}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500">Transaction Date</dt>
                  <dd className="mt-0.5 text-sm font-medium text-slate-900">{formData.transDate}</dd>
                </div>
                {formData.transType !== 'FE' && (
                  <div>
                    <dt className="text-xs text-slate-500">Quantity</dt>
                    <dd className="mt-0.5 text-sm font-medium text-slate-900">{formatNumber(parsedQuantity)}</dd>
                  </div>
                )}
                {formData.transType !== 'FE' && (
                  <div>
                    <dt className="text-xs text-slate-500">Price per Share</dt>
                    <dd className="mt-0.5 text-sm font-medium text-slate-900">{formatCurrency(parsedPrice)}</dd>
                  </div>
                )}
                <div>
                  <dt className="text-xs text-slate-500">Total Amount</dt>
                  <dd className="mt-0.5 text-lg font-bold text-slate-900">{formatCurrency(parsedAmount)}</dd>
                </div>
              </dl>
            </div>

            {formData.transType === 'SL' && (
              <div className="rounded-md bg-slate-50 p-6 ring-1 ring-inset ring-slate-200">
                <h4 className="text-sm font-semibold text-slate-900 mb-4">Position Impact</h4>
                <div className="grid grid-cols-3 gap-6 text-center">
                  <div>
                    <p className="text-xs text-slate-500">Current Balance</p>
                    <p className="text-lg font-semibold text-slate-900">{formatNumber(currentBalance)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500">Selling</p>
                    <p className="text-lg font-semibold text-red-600">-{formatNumber(parsedQuantity)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500">Resulting Balance</p>
                    <p className={`text-lg font-semibold ${resultingBalance < 0 ? 'text-red-600' : 'text-slate-900'}`}>
                      {formatNumber(resultingBalance)}
                    </p>
                  </div>
                </div>
                {parsedQuantity > currentBalance && (
                  <div className="mt-4 flex items-start gap-3 rounded-md bg-red-50 p-3 ring-1 ring-inset ring-red-200">
                    <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-500" />
                    <p className="text-sm text-red-700">
                      Warning: Selling more units than available. Available: {formatNumber(currentBalance)}, Requested: {formatNumber(parsedQuantity)}
                    </p>
                  </div>
                )}
              </div>
            )}

            {formData.transType === 'BY' && accountNo && (
              <div className="rounded-md bg-slate-50 p-6 ring-1 ring-inset ring-slate-200">
                <h4 className="text-sm font-semibold text-slate-900 mb-4">Position Impact</h4>
                <div className="grid grid-cols-3 gap-6 text-center">
                  <div>
                    <p className="text-xs text-slate-500">Current Balance</p>
                    <p className="text-lg font-semibold text-slate-900">{formatNumber(currentBalance)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500">Buying</p>
                    <p className="text-lg font-semibold text-emerald-600">+{formatNumber(parsedQuantity)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500">Resulting Balance</p>
                    <p className="text-lg font-semibold text-slate-900">{formatNumber(resultingBalance)}</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </Card>
      )}

      {/* Navigation Buttons */}
      <div className="mt-6 flex items-center justify-between">
        <div>
          {currentStep > 0 && (
            <button
              type="button"
              onClick={handleBack}
              className="inline-flex items-center gap-2 rounded-md bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
            >
              <ArrowLeft className="h-4 w-4" />
              Back
            </button>
          )}
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => navigate('/transactions')}
            className="inline-flex items-center gap-2 rounded-md bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
          >
            Cancel
          </button>
          {currentStep < 2 ? (
            <button
              type="button"
              onClick={handleNext}
              className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Next
              <ArrowRight className="h-4 w-4" />
            </button>
          ) : (
            <button
              type="button"
              onClick={handleSubmit}
              className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              <Check className="h-4 w-4" />
              Submit Transaction
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
