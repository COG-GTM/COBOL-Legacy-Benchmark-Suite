import { useState } from 'react';
import type { PortfolioFormErrors, PortfolioFormValues } from './portfolioData';
import { CLIENT_TYPE_LABELS, STATUS_LABELS } from './portfolioData';

interface PortfolioFormProps {
  initialValues: PortfolioFormValues;
  onSubmit: (values: PortfolioFormValues) => PortfolioFormErrors | null;
  onCancel: () => void;
  submitLabel: string;
  idEditable?: boolean;
}

const inputClass =
  'w-full px-3 py-2 text-sm border rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500';

function fieldBorder(error?: string) {
  return error ? 'border-red-300' : 'border-slate-300';
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="mt-1 text-xs text-red-600">{message}</p>;
}

export function PortfolioForm({
  initialValues,
  onSubmit,
  onCancel,
  submitLabel,
  idEditable = true,
}: PortfolioFormProps) {
  const [values, setValues] = useState<PortfolioFormValues>(initialValues);
  const [errors, setErrors] = useState<PortfolioFormErrors>({});

  const setField = (field: keyof PortfolioFormValues, value: string) => {
    setValues((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const result = onSubmit(values);
    setErrors(result ?? {});
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
        <div>
          <label htmlFor="id" className="block text-sm font-medium text-slate-700 mb-1.5">
            Portfolio ID <span className="text-red-500">*</span>
          </label>
          <input
            id="id"
            type="text"
            value={values.id}
            onChange={(e) => setField('id', e.target.value.toUpperCase())}
            disabled={!idEditable}
            maxLength={8}
            placeholder="PORT0000"
            className={`${inputClass} ${fieldBorder(errors.id)} disabled:bg-slate-100 disabled:text-slate-500`}
          />
          <FieldError message={errors.id} />
          {idEditable && (
            <p className="mt-1 text-xs text-slate-400">Format: PORT followed by 4 digits</p>
          )}
        </div>

        <div>
          <label htmlFor="accountNo" className="block text-sm font-medium text-slate-700 mb-1.5">
            Account Number <span className="text-red-500">*</span>
          </label>
          <input
            id="accountNo"
            type="text"
            value={values.accountNo}
            onChange={(e) => setField('accountNo', e.target.value)}
            maxLength={10}
            placeholder="0000000000"
            className={`${inputClass} ${fieldBorder(errors.accountNo)} font-mono`}
          />
          <FieldError message={errors.accountNo} />
          <p className="mt-1 text-xs text-slate-400">Exactly 10 numeric digits</p>
        </div>

        <div>
          <label htmlFor="clientName" className="block text-sm font-medium text-slate-700 mb-1.5">
            Client Name <span className="text-red-500">*</span>
          </label>
          <input
            id="clientName"
            type="text"
            value={values.clientName}
            onChange={(e) => setField('clientName', e.target.value)}
            maxLength={30}
            placeholder="Client name"
            className={`${inputClass} ${fieldBorder(errors.clientName)}`}
          />
          <FieldError message={errors.clientName} />
        </div>

        <div>
          <label htmlFor="clientType" className="block text-sm font-medium text-slate-700 mb-1.5">
            Client Type <span className="text-red-500">*</span>
          </label>
          <select
            id="clientType"
            value={values.clientType}
            onChange={(e) => setField('clientType', e.target.value)}
            className={`${inputClass} ${fieldBorder(errors.clientType)}`}
          >
            <option value="">Select client type...</option>
            {Object.entries(CLIENT_TYPE_LABELS).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
          <FieldError message={errors.clientType} />
        </div>

        <div>
          <label htmlFor="status" className="block text-sm font-medium text-slate-700 mb-1.5">
            Status <span className="text-red-500">*</span>
          </label>
          <select
            id="status"
            value={values.status}
            onChange={(e) => setField('status', e.target.value)}
            className={`${inputClass} ${fieldBorder(errors.status)}`}
          >
            {Object.entries(STATUS_LABELS).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
          <FieldError message={errors.status} />
        </div>

        <div>
          <label htmlFor="totalValue" className="block text-sm font-medium text-slate-700 mb-1.5">
            Total Value <span className="text-red-500">*</span>
          </label>
          <input
            id="totalValue"
            type="text"
            inputMode="decimal"
            value={values.totalValue}
            onChange={(e) => setField('totalValue', e.target.value)}
            placeholder="0.00"
            className={`${inputClass} ${fieldBorder(errors.totalValue)} tabular-nums`}
          />
          <FieldError message={errors.totalValue} />
        </div>

        <div>
          <label htmlFor="cashBalance" className="block text-sm font-medium text-slate-700 mb-1.5">
            Cash Balance <span className="text-red-500">*</span>
          </label>
          <input
            id="cashBalance"
            type="text"
            inputMode="decimal"
            value={values.cashBalance}
            onChange={(e) => setField('cashBalance', e.target.value)}
            placeholder="0.00"
            className={`${inputClass} ${fieldBorder(errors.cashBalance)} tabular-nums`}
          />
          <FieldError message={errors.cashBalance} />
        </div>
      </div>

      <div className="flex justify-end gap-3 pt-4 border-t border-slate-200">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
        >
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
