import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Check, X, CheckCircle } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { positions } from '@/data/mockData';
import type { Transaction } from '@/data/types';

type TransType = Transaction['transType'];

interface FormData {
  transType: TransType;
  accountNo: string;
  fundId: string;
  units: string;
  price: string;
  amount: string;
}

interface FormErrors {
  transType?: string;
  accountNo?: string;
  fundId?: string;
  units?: string;
  price?: string;
  amount?: string;
}

const TRANS_TYPE_OPTIONS: { value: TransType; label: string }[] = [
  { value: 'BY', label: 'Buy' },
  { value: 'SL', label: 'Sell' },
  { value: 'FE', label: 'Fee' },
];

function getTransTypeLabel(type: TransType): string {
  return TRANS_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? type;
}

function generateTransId(): string {
  return 'TXN' + String(Math.floor(100000 + Math.random() * 900000));
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

const INITIAL_FORM: FormData = {
  transType: 'BY',
  accountNo: '',
  fundId: '',
  units: '',
  price: '',
  amount: '',
};

export function TransactionNewPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<1 | 2>(1);
  const [form, setForm] = useState<FormData>(INITIAL_FORM);
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitted, setSubmitted] = useState(false);
  const [submittedId, setSubmittedId] = useState('');

  const isFee = form.transType === 'FE';

  const calculatedAmount = useMemo(() => {
    if (isFee) return parseFloat(form.amount) || 0;
    const u = parseFloat(form.units) || 0;
    const p = parseFloat(form.price) || 0;
    return u * p;
  }, [form.units, form.price, form.amount, isFee]);

  function setField<K extends keyof FormData>(key: K, value: FormData[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
    setErrors((prev) => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  }

  function validate(): FormErrors {
    const errs: FormErrors = {};

    if (!/^\d{9}$/.test(form.accountNo)) {
      errs.accountNo = 'Account number must be exactly 9 digits';
    }

    if (!form.fundId || form.fundId.length !== 6) {
      errs.fundId = 'Fund ID must be exactly 6 characters';
    }

    if (!isFee) {
      const units = parseFloat(form.units);
      if (!form.units || isNaN(units) || units <= 0) {
        errs.units = 'Units must be a positive number';
      }

      const price = parseFloat(form.price);
      if (!form.price || isNaN(price) || price <= 0) {
        errs.price = 'Price must be a positive number';
      }

      if (form.transType === 'SL' && !errs.units) {
        const units = parseFloat(form.units);
        const position = positions.find(
          (p) =>
            p.accountNo === form.accountNo &&
            p.fundId === form.fundId.toUpperCase() &&
            p.status === 'A',
        );
        if (!position) {
          errs.units = 'No active position found for this account and fund';
        } else if (units > position.shareBalance) {
          errs.units = `Insufficient shares. Current balance: ${position.shareBalance.toLocaleString()}`;
        }
      }
    } else {
      const amt = parseFloat(form.amount);
      if (!form.amount || isNaN(amt) || amt <= 0) {
        errs.amount = 'Amount must be a positive number';
      }
    }

    return errs;
  }

  function handleNext() {
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setStep(2);
  }

  function handleConfirm() {
    const txnId = generateTransId();
    setSubmittedId(txnId);
    setSubmitted(true);
  }

  function handleReset() {
    setForm(INITIAL_FORM);
    setErrors({});
    setStep(1);
    setSubmitted(false);
    setSubmittedId('');
  }

  if (submitted) {
    return (
      <div>
        <PageHeader title="New Transaction" />
        <Card>
          <div className="flex flex-col items-center py-8 text-center">
            <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center mb-4">
              <CheckCircle className="w-8 h-8 text-emerald-600" />
            </div>
            <h2 className="text-xl font-semibold text-slate-900 mb-2">
              Transaction Submitted
            </h2>
            <p className="text-slate-500 mb-1">
              Your {getTransTypeLabel(form.transType).toLowerCase()} transaction has been submitted successfully.
            </p>
            <p className="text-sm font-mono text-slate-700 bg-slate-100 px-3 py-1 rounded mb-6">
              Transaction ID: {submittedId}
            </p>
            <div className="flex gap-3">
              <button
                onClick={handleReset}
                className="px-4 py-2 text-sm font-medium text-blue-600 bg-white border border-blue-300 rounded-lg hover:bg-blue-50 transition-colors"
              >
                New Transaction
              </button>
              <button
                onClick={() => navigate('/transactions')}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
              >
                View Transactions
              </button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <PageHeader title="New Transaction" description="Create a new buy, sell, or fee transaction" />

      <Card>
        {/* Step indicator */}
        <div className="flex items-center mb-8">
          <StepIndicator number={1} label="Enter Details" active={step === 1} completed={step === 2} />
          <div className={`flex-1 h-0.5 mx-4 ${step === 2 ? 'bg-blue-600' : 'bg-slate-200'}`} />
          <StepIndicator number={2} label="Review & Confirm" active={step === 2} completed={false} />
        </div>

        {step === 1 ? (
          <EntryForm
            form={form}
            errors={errors}
            isFee={isFee}
            calculatedAmount={calculatedAmount}
            onFieldChange={setField}
            onNext={handleNext}
            onCancel={() => navigate('/transactions')}
          />
        ) : (
          <ReviewStep
            form={form}
            calculatedAmount={calculatedAmount}
            isFee={isFee}
            onBack={() => setStep(1)}
            onConfirm={handleConfirm}
          />
        )}
      </Card>
    </div>
  );
}

/* ---------- sub-components ---------- */

function StepIndicator({
  number,
  label,
  active,
  completed,
}: {
  number: number;
  label: string;
  active: boolean;
  completed: boolean;
}) {
  const circleClass = completed
    ? 'bg-blue-600 text-white'
    : active
      ? 'bg-blue-600 text-white'
      : 'bg-slate-200 text-slate-500';

  return (
    <div className="flex items-center gap-2">
      <div
        className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold ${circleClass}`}
      >
        {completed ? <Check className="w-4 h-4" /> : number}
      </div>
      <span
        className={`text-sm font-medium ${active || completed ? 'text-slate-900' : 'text-slate-400'}`}
      >
        {label}
      </span>
    </div>
  );
}

function EntryForm({
  form,
  errors,
  isFee,
  calculatedAmount,
  onFieldChange,
  onNext,
  onCancel,
}: {
  form: FormData;
  errors: FormErrors;
  isFee: boolean;
  calculatedAmount: number;
  onFieldChange: <K extends keyof FormData>(key: K, value: FormData[K]) => void;
  onNext: () => void;
  onCancel: () => void;
}) {
  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Transaction Type */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Transaction Type
          </label>
          <select
            value={form.transType}
            onChange={(e) => onFieldChange('transType', e.target.value as TransType)}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none"
          >
            {TRANS_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Account Number */}
        <FieldInput
          label="Account Number"
          value={form.accountNo}
          error={errors.accountNo}
          placeholder="123456789"
          maxLength={9}
          onChange={(v) => onFieldChange('accountNo', v)}
        />

        {/* Fund ID */}
        <FieldInput
          label="Fund ID"
          value={form.fundId}
          error={errors.fundId}
          placeholder="GRWEQF"
          maxLength={6}
          onChange={(v) => onFieldChange('fundId', v.toUpperCase())}
        />

        {/* Units */}
        <FieldInput
          label="Units"
          value={form.units}
          error={errors.units}
          placeholder="0.000"
          type="number"
          disabled={isFee}
          onChange={(v) => onFieldChange('units', v)}
        />

        {/* Price */}
        <FieldInput
          label="Price per Unit"
          value={form.price}
          error={errors.price}
          placeholder="0.00"
          type="number"
          disabled={isFee}
          onChange={(v) => onFieldChange('price', v)}
        />

        {/* Amount */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Amount</label>
          {isFee ? (
            <>
              <input
                type="number"
                value={form.amount}
                onChange={(e) => onFieldChange('amount', e.target.value)}
                placeholder="0.00"
                className={`w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:ring-1 ${
                  errors.amount
                    ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                    : 'border-slate-300 focus:border-blue-500 focus:ring-blue-500'
                }`}
              />
              {errors.amount && (
                <p className="mt-1 text-xs text-red-600">{errors.amount}</p>
              )}
            </>
          ) : (
            <div className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
              {formatCurrency(calculatedAmount)}
              <span className="ml-2 text-xs text-slate-400">(auto-calculated)</span>
            </div>
          )}
        </div>
      </div>

      {/* Buttons */}
      <div className="flex justify-end gap-3 mt-8 pt-6 border-t border-slate-200">
        <button
          onClick={onCancel}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
        >
          <X className="w-4 h-4" />
          Cancel
        </button>
        <button
          onClick={onNext}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
        >
          Next
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}

function FieldInput({
  label,
  value,
  error,
  placeholder,
  type = 'text',
  maxLength,
  disabled,
  onChange,
}: {
  label: string;
  value: string;
  error?: string;
  placeholder?: string;
  type?: string;
  maxLength?: number;
  disabled?: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        maxLength={maxLength}
        disabled={disabled}
        className={`w-full rounded-lg border px-3 py-2 text-sm text-slate-900 outline-none focus:ring-1 ${
          disabled
            ? 'bg-slate-100 border-slate-200 text-slate-400 cursor-not-allowed'
            : error
              ? 'border-red-400 bg-white focus:border-red-500 focus:ring-red-500'
              : 'border-slate-300 bg-white focus:border-blue-500 focus:ring-blue-500'
        }`}
      />
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}

function ReviewStep({
  form,
  calculatedAmount,
  isFee,
  onBack,
  onConfirm,
}: {
  form: FormData;
  calculatedAmount: number;
  isFee: boolean;
  onBack: () => void;
  onConfirm: () => void;
}) {
  const rows: { label: string; value: string }[] = [
    { label: 'Transaction Type', value: getTransTypeLabel(form.transType) },
    { label: 'Account Number', value: form.accountNo },
    { label: 'Fund ID', value: form.fundId },
  ];

  if (!isFee) {
    rows.push(
      { label: 'Units', value: parseFloat(form.units).toLocaleString() },
      { label: 'Price per Unit', value: formatCurrency(parseFloat(form.price)) },
    );
  }

  rows.push({ label: 'Amount', value: formatCurrency(calculatedAmount) });

  return (
    <div>
      <div className="rounded-lg border border-slate-200 overflow-hidden">
        {rows.map((row, i) => (
          <div
            key={row.label}
            className={`flex justify-between px-4 py-3 text-sm ${i % 2 === 0 ? 'bg-slate-50' : 'bg-white'}`}
          >
            <span className="font-medium text-slate-600">{row.label}</span>
            <span className="text-slate-900">{row.value}</span>
          </div>
        ))}
      </div>

      <div className="flex justify-end gap-3 mt-8 pt-6 border-t border-slate-200">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Edit
        </button>
        <button
          onClick={onConfirm}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
        >
          <Check className="w-4 h-4" />
          Confirm Transaction
        </button>
      </div>
    </div>
  );
}
