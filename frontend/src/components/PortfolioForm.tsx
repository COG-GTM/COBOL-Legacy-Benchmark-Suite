import { useState, type FormEvent } from 'react';
import type { Portfolio, ClientType, PortfolioStatus } from '../types';
import { CLIENT_TYPE_LABELS, PORTFOLIO_STATUS_LABELS } from '../types';
import { InlineError } from './InlineError';
import {
  validatePortfolioId,
  validateAccountNumber,
  validateClientName,
  validateAmount,
} from '../utils/validation';

interface PortfolioFormProps {
  initial?: Partial<Portfolio>;
  onSubmit: (data: Omit<Portfolio, 'createDate' | 'lastMaintDate' | 'lastUser' | 'lastTransDate'>) => void;
  onCancel: () => void;
  isEdit?: boolean;
}

export function PortfolioForm({ initial, onSubmit, onCancel, isEdit = false }: PortfolioFormProps) {
  const [portfolioId, setPortfolioId] = useState(initial?.portfolioId ?? '');
  const [accountNumber, setAccountNumber] = useState(initial?.accountNumber ?? '');
  const [clientName, setClientName] = useState(initial?.clientName ?? '');
  const [clientType, setClientType] = useState<ClientType>(initial?.clientType ?? 'I');
  const [status, setStatus] = useState<PortfolioStatus>(initial?.status ?? 'A');
  const [totalValue, setTotalValue] = useState(initial?.totalValue?.toString() ?? '0');
  const [cashBalance, setCashBalance] = useState(initial?.cashBalance?.toString() ?? '0');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!isEdit) {
      const idResult = validatePortfolioId(portfolioId);
      if (!idResult.valid) errs.portfolioId = idResult.error;
    }

    const acctResult = validateAccountNumber(accountNumber);
    if (!acctResult.valid) errs.accountNumber = acctResult.error;

    const nameResult = validateClientName(clientName);
    if (!nameResult.valid) errs.clientName = nameResult.error;

    const totalVal = parseFloat(totalValue);
    const totalResult = validateAmount(totalVal);
    if (!totalResult.valid) errs.totalValue = totalResult.error;

    const cashVal = parseFloat(cashBalance);
    const cashResult = validateAmount(cashVal);
    if (!cashResult.valid) errs.cashBalance = cashResult.error;

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      portfolioId: portfolioId.toUpperCase(),
      accountNumber,
      clientName: clientName.toUpperCase(),
      clientType,
      status,
      totalValue: parseFloat(totalValue),
      cashBalance: parseFloat(cashBalance),
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 max-w-lg">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Portfolio ID</label>
        <input
          type="text"
          value={portfolioId}
          onChange={e => setPortfolioId(e.target.value)}
          maxLength={8}
          disabled={isEdit}
          placeholder="PORT0000"
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full disabled:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <InlineError message={errors.portfolioId ?? ''} />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Account Number</label>
        <input
          type="text"
          value={accountNumber}
          onChange={e => setAccountNumber(e.target.value)}
          maxLength={10}
          placeholder="0000000000"
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <InlineError message={errors.accountNumber ?? ''} />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Client Name</label>
        <input
          type="text"
          value={clientName}
          onChange={e => setClientName(e.target.value)}
          maxLength={30}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <InlineError message={errors.clientName ?? ''} />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Client Type</label>
        <select
          value={clientType}
          onChange={e => setClientType(e.target.value as ClientType)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {(Object.entries(CLIENT_TYPE_LABELS) as [ClientType, string][]).map(([k, v]) => (
            <option key={k} value={k}>{v}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
        <select
          value={status}
          onChange={e => setStatus(e.target.value as PortfolioStatus)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {(Object.entries(PORTFOLIO_STATUS_LABELS) as [PortfolioStatus, string][]).map(([k, v]) => (
            <option key={k} value={k}>{v}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Total Value</label>
        <input
          type="number"
          step="0.01"
          value={totalValue}
          onChange={e => setTotalValue(e.target.value)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <InlineError message={errors.totalValue ?? ''} />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Cash Balance</label>
        <input
          type="number"
          step="0.01"
          value={cashBalance}
          onChange={e => setCashBalance(e.target.value)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <InlineError message={errors.cashBalance ?? ''} />
      </div>

      <div className="flex gap-3 pt-4">
        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700 transition-colors"
        >
          {isEdit ? 'Update Portfolio' : 'Create Portfolio'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="bg-gray-200 text-gray-700 px-4 py-2 rounded-md text-sm hover:bg-gray-300 transition-colors"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
