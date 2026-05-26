import { useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, AlertCircle, TrendingUp, TrendingDown, DollarSign } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { positions } from '@/data/mockData';

type TransType = 'BY' | 'SL' | 'FE';

interface FormData {
  accountNo: string;
  fundId: string;
  transType: TransType;
  transDate: string;
  shareQty: string;
  price: string;
  amount: string;
}

interface FormErrors {
  accountNo?: string;
  fundId?: string;
  transType?: string;
  transDate?: string;
  shareQty?: string;
  price?: string;
  amount?: string;
}

const INITIAL_FORM: FormData = {
  accountNo: '',
  fundId: '',
  transType: 'BY',
  transDate: '',
  shareQty: '',
  price: '',
  amount: '',
};

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatQuantity(value: number): string {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 }).format(value);
}

function todayString(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function TransactionNewPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<FormData>(INITIAL_FORM);
  const [errors, setErrors] = useState<FormErrors>({});
  const [touched, setTouched] = useState<Partial<Record<keyof FormData, boolean>>>({});
  const [showConfirm, setShowConfirm] = useState(false);

  const updateField = useCallback(<K extends keyof FormData>(key: K, value: FormData[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setTouched((prev) => ({ ...prev, [key]: true }));
  }, []);

  const accountPositions = useMemo(() => {
    if (!form.accountNo || form.accountNo.length < 9) return [];
    return positions.filter((p) => p.accountNo === form.accountNo && p.status === 'A');
  }, [form.accountNo]);

  const currentPosition = useMemo(() => {
    if (!form.fundId || !form.accountNo) return null;
    return positions.find(
      (p) => p.accountNo === form.accountNo && p.fundId === form.fundId && p.status === 'A',
    ) ?? null;
  }, [form.accountNo, form.fundId]);

  const isFee = form.transType === 'FE';

  const computedAmount = useMemo(() => {
    if (isFee) return null;
    const qty = parseFloat(form.shareQty);
    const price = parseFloat(form.price);
    if (isNaN(qty) || isNaN(price)) return null;
    return qty * price;
  }, [form.shareQty, form.price, isFee]);

  const positionPreview = useMemo(() => {
    if (!currentPosition) return null;
    const qty = parseFloat(form.shareQty);
    const price = parseFloat(form.price);
    const { shareBalance, costBasis, avgCost } = currentPosition;

    if (form.transType === 'BY') {
      if (isNaN(qty) || isNaN(price) || qty <= 0 || price <= 0) return null;
      const newBalance = shareBalance + qty;
      const newCostBasis = costBasis + qty * price;
      const newAvgCost = newBalance > 0 ? newCostBasis / newBalance : 0;
      return { newBalance, newCostBasis, newAvgCost };
    }

    if (form.transType === 'SL') {
      if (isNaN(qty) || qty <= 0) return null;
      const newBalance = shareBalance - qty;
      const newCostBasis = costBasis - qty * avgCost;
      const newAvgCost = newBalance > 0 ? newCostBasis / newBalance : 0;
      return { newBalance, newCostBasis, newAvgCost };
    }

    return null;
  }, [currentPosition, form.shareQty, form.price, form.transType]);

  const validate = useCallback((): FormErrors => {
    const errs: FormErrors = {};

    if (!/^\d{9,10}$/.test(form.accountNo)) {
      errs.accountNo = 'Account number must be 9-10 digits';
    }

    if (!/^[A-Za-z0-9]{6}$/.test(form.fundId)) {
      errs.fundId = 'Fund ID must be 6 alphanumeric characters';
    }

    if (!form.transDate) {
      errs.transDate = 'Transaction date is required';
    } else if (form.transDate > todayString()) {
      errs.transDate = 'Transaction date cannot be in the future';
    }

    if (isFee) {
      const amt = parseFloat(form.amount);
      if (isNaN(amt) || amt === 0) {
        errs.amount = 'Amount must be non-zero for Fee transactions';
      }
    } else {
      const qty = parseFloat(form.shareQty);
      if (isNaN(qty) || qty <= 0) {
        errs.shareQty = 'Share quantity must be greater than zero';
      }

      const price = parseFloat(form.price);
      if (isNaN(price) || price <= 0) {
        errs.price = 'Price must be greater than zero';
      }

      if (form.transType === 'SL' && currentPosition) {
        if (!isNaN(qty) && qty > currentPosition.shareBalance) {
          errs.shareQty = `Cannot exceed current position balance (${formatQuantity(currentPosition.shareBalance)})`;
        }
      } else if (form.transType === 'SL' && !currentPosition) {
        errs.shareQty = 'Cannot sell without an existing position';
      }
    }

    return errs;
  }, [form, isFee, currentPosition]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const errs = validate();
    setErrors(errs);
    setTouched({
      accountNo: true,
      fundId: true,
      transDate: true,
      shareQty: true,
      price: true,
      amount: true,
    });
    if (Object.keys(errs).length === 0) {
      setShowConfirm(true);
    }
  };

  const handleConfirm = () => {
    setShowConfirm(false);
    navigate('/transactions');
  };

  const summaryAmount = isFee ? parseFloat(form.amount) : computedAmount;

  const fieldClass = (key: keyof FormErrors) =>
    `block w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
      touched[key] && errors[key] ? 'border-red-400 ring-1 ring-red-400' : 'border-slate-300'
    }`;

  return (
    <div>
      <PageHeader
        title="New Transaction"
        description="Create a new buy, sell, or fee transaction"
        actions={
          <button
            onClick={() => navigate('/transactions')}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Transactions
          </button>
        }
      />

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <Card title="Transaction Details">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Account Number <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    maxLength={10}
                    value={form.accountNo}
                    onChange={(e) => updateField('accountNo', e.target.value.replace(/\D/g, ''))}
                    placeholder="e.g. 100000001"
                    className={fieldClass('accountNo')}
                  />
                  {touched.accountNo && errors.accountNo && (
                    <p className="mt-1 text-xs text-red-600 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      {errors.accountNo}
                    </p>
                  )}
                  <p className="mt-1 text-xs text-slate-400">9-10 digit numeric account number</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Fund ID <span className="text-red-500">*</span>
                  </label>
                  {accountPositions.length > 0 ? (
                    <select
                      value={form.fundId}
                      onChange={(e) => updateField('fundId', e.target.value)}
                      className={fieldClass('fundId')}
                    >
                      <option value="">Select a fund...</option>
                      {accountPositions.map((p) => (
                        <option key={p.fundId} value={p.fundId}>
                          {p.fundId} — {formatQuantity(p.shareBalance)} shares
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type="text"
                      maxLength={6}
                      value={form.fundId}
                      onChange={(e) => updateField('fundId', e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))}
                      placeholder="e.g. GRWEQF"
                      className={fieldClass('fundId')}
                    />
                  )}
                  {touched.fundId && errors.fundId && (
                    <p className="mt-1 text-xs text-red-600 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      {errors.fundId}
                    </p>
                  )}
                  <p className="mt-1 text-xs text-slate-400">6-character alphanumeric fund identifier</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Transaction Type <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={form.transType}
                    onChange={(e) => {
                      const newType = e.target.value as TransType;
                      updateField('transType', newType);
                      if (newType === 'FE') {
                        setForm((prev) => ({ ...prev, shareQty: '', price: '' }));
                      } else {
                        setForm((prev) => ({ ...prev, amount: '' }));
                      }
                    }}
                    className={fieldClass('transType')}
                  >
                    <option value="BY">Buy</option>
                    <option value="SL">Sell</option>
                    <option value="FE">Fee</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Transaction Date <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    max={todayString()}
                    value={form.transDate}
                    onChange={(e) => updateField('transDate', e.target.value)}
                    className={fieldClass('transDate')}
                  />
                  {touched.transDate && errors.transDate && (
                    <p className="mt-1 text-xs text-red-600 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      {errors.transDate}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Share Quantity {!isFee && <span className="text-red-500">*</span>}
                  </label>
                  <input
                    type="number"
                    step="0.001"
                    min="0"
                    disabled={isFee}
                    value={form.shareQty}
                    onChange={(e) => updateField('shareQty', e.target.value)}
                    placeholder="0.000"
                    className={`${fieldClass('shareQty')} ${isFee ? 'bg-slate-100 text-slate-400 cursor-not-allowed' : ''}`}
                  />
                  {touched.shareQty && errors.shareQty && (
                    <p className="mt-1 text-xs text-red-600 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      {errors.shareQty}
                    </p>
                  )}
                  {form.transType === 'SL' && currentPosition && (
                    <p className="mt-1 text-xs text-slate-500">
                      Available: {formatQuantity(currentPosition.shareBalance)} shares
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Price per Share {!isFee && <span className="text-red-500">*</span>}
                  </label>
                  <input
                    type="number"
                    step="0.0001"
                    min="0"
                    disabled={isFee}
                    value={form.price}
                    onChange={(e) => updateField('price', e.target.value)}
                    placeholder="0.0000"
                    className={`${fieldClass('price')} ${isFee ? 'bg-slate-100 text-slate-400 cursor-not-allowed' : ''}`}
                  />
                  {touched.price && errors.price && (
                    <p className="mt-1 text-xs text-red-600 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      {errors.price}
                    </p>
                  )}
                </div>
              </div>

              <div className="mt-5 pt-5 border-t border-slate-200">
                <div className="max-w-xs">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Amount {isFee && <span className="text-red-500">*</span>}
                  </label>
                  {isFee ? (
                    <input
                      type="number"
                      step="0.01"
                      value={form.amount}
                      onChange={(e) => updateField('amount', e.target.value)}
                      placeholder="0.00"
                      className={fieldClass('amount')}
                    />
                  ) : (
                    <p className="px-3 py-2 text-sm font-medium text-slate-900 bg-slate-50 border border-slate-200 rounded-lg tabular-nums">
                      {computedAmount != null ? formatCurrency(computedAmount) : '—'}
                    </p>
                  )}
                  {touched.amount && errors.amount && (
                    <p className="mt-1 text-xs text-red-600 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      {errors.amount}
                    </p>
                  )}
                  {!isFee && (
                    <p className="mt-1 text-xs text-slate-400">Auto-calculated: Quantity × Price</p>
                  )}
                </div>
              </div>
            </Card>

            <div className="flex items-center gap-3">
              <button
                type="submit"
                className="px-6 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
              >
                Review & Submit
              </button>
              <button
                type="button"
                onClick={() => navigate('/transactions')}
                className="px-6 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
              >
                Cancel
              </button>
            </div>
          </div>

          <div className="space-y-6">
            {currentPosition && (
              <Card title="Current Position">
                <div className="space-y-3">
                  <div className="flex items-center gap-2 mb-4">
                    <div className="p-2 rounded-lg bg-blue-50 text-blue-600">
                      <DollarSign className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-slate-900">{currentPosition.fundId}</p>
                      <p className="text-xs text-slate-500">Account {currentPosition.accountNo}</p>
                    </div>
                  </div>
                  <DetailRow label="Share Balance" value={formatQuantity(currentPosition.shareBalance)} />
                  <DetailRow label="Cost Basis" value={formatCurrency(currentPosition.costBasis)} />
                  <DetailRow label="Avg Cost/Share" value={formatCurrency(currentPosition.avgCost)} />
                </div>
              </Card>
            )}

            {positionPreview && currentPosition && (
              <Card title="Position After Transaction">
                <div className="space-y-3">
                  <div className="flex items-center gap-2 mb-4">
                    <div className={`p-2 rounded-lg ${form.transType === 'BY' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>
                      {form.transType === 'BY' ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                    </div>
                    <p className="text-sm font-medium text-slate-900">
                      {form.transType === 'BY' ? 'Buy' : 'Sell'} Impact Preview
                    </p>
                  </div>
                  <DetailRow
                    label="Share Balance"
                    value={formatQuantity(positionPreview.newBalance)}
                    delta={positionPreview.newBalance - currentPosition.shareBalance}
                    deltaFormat="qty"
                  />
                  <DetailRow
                    label="Cost Basis"
                    value={formatCurrency(positionPreview.newCostBasis)}
                    delta={positionPreview.newCostBasis - currentPosition.costBasis}
                    deltaFormat="currency"
                  />
                  <DetailRow
                    label="Avg Cost/Share"
                    value={formatCurrency(positionPreview.newAvgCost)}
                  />
                </div>
              </Card>
            )}

            {!currentPosition && form.accountNo.length >= 9 && form.fundId.length === 6 && (
              <Card>
                <div className="text-center py-4">
                  <p className="text-sm text-slate-500">No existing position found for this account and fund combination.</p>
                  {form.transType === 'SL' && (
                    <p className="mt-2 text-xs text-amber-600 font-medium">
                      Warning: Cannot sell without an existing position
                    </p>
                  )}
                </div>
              </Card>
            )}
          </div>
        </div>
      </form>

      <ConfirmDialog
        open={showConfirm}
        title="Confirm Transaction"
        message={`Submit ${form.transType === 'BY' ? 'Buy' : form.transType === 'SL' ? 'Sell' : 'Fee'} transaction for account ${form.accountNo}, fund ${form.fundId}${summaryAmount != null && !isNaN(summaryAmount) ? ` — ${formatCurrency(summaryAmount)}` : ''}?`}
        confirmLabel="Submit Transaction"
        cancelLabel="Go Back"
        onConfirm={handleConfirm}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
}

function DetailRow({
  label,
  value,
  delta,
  deltaFormat,
}: {
  label: string;
  value: string;
  delta?: number;
  deltaFormat?: 'qty' | 'currency';
}) {
  const deltaStr = delta != null
    ? deltaFormat === 'currency'
      ? formatCurrency(Math.abs(delta))
      : formatQuantity(Math.abs(delta))
    : null;

  return (
    <div className="flex justify-between items-baseline">
      <span className="text-sm text-slate-500">{label}</span>
      <div className="text-right">
        <span className="text-sm font-medium text-slate-900 tabular-nums">{value}</span>
        {delta != null && deltaStr && (
          <span className={`ml-2 text-xs font-medium ${delta >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
            {delta >= 0 ? '+' : '−'}{deltaStr}
          </span>
        )}
      </div>
    </div>
  );
}
