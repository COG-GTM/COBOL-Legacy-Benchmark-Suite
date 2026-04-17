import { useLocation, useNavigate, Navigate } from 'react-router-dom';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getTransTypeLabel } from '@/components/ui/StatusBadge';
import { CheckCircle, ArrowRight, Plus } from 'lucide-react';

interface ConfirmationState {
  transId: string;
  portfolioId: string;
  portfolioName: string;
  accountNo: string;
  fundId: string;
  transType: string;
  transDate: string;
  shareQty: number;
  price: number;
  amount: number;
  beforeBalance: number;
  afterBalance: number;
}

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

export function TransactionConfirmation() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as ConfirmationState | null;

  if (!state) {
    return <Navigate to="/transactions/new" replace />;
  }

  return (
    <div>
      <PageHeader title="Transaction Submitted" />

      {/* Success Banner */}
      <div className="mb-6 flex items-center gap-3 rounded-lg bg-emerald-50 p-4 ring-1 ring-inset ring-emerald-200">
        <CheckCircle className="h-6 w-6 shrink-0 text-emerald-600" />
        <div>
          <p className="text-sm font-semibold text-emerald-800">Transaction submitted successfully</p>
          <p className="text-sm text-emerald-700">
            Transaction ID: <span className="font-mono font-bold">{state.transId}</span>
          </p>
        </div>
      </div>

      <Card title="Transaction Details">
        <div className="space-y-6">
          {/* Transaction ID and Status */}
          <div className="flex items-center justify-between rounded-md bg-slate-50 p-4 ring-1 ring-inset ring-slate-200">
            <div>
              <p className="text-xs text-slate-500">Transaction ID</p>
              <p className="text-lg font-bold font-mono text-slate-900">{state.transId}</p>
            </div>
            <StatusBadge label="Pending" variant="warning" />
          </div>

          {/* Details Grid */}
          <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
            <div>
              <dt className="text-xs text-slate-500">Portfolio</dt>
              <dd className="mt-0.5 text-sm font-medium text-slate-900">
                {state.portfolioId} &mdash; {state.portfolioName}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500">Account Number</dt>
              <dd className="mt-0.5 text-sm font-medium text-slate-900">{state.accountNo}</dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500">Transaction Type</dt>
              <dd className="mt-0.5">
                <StatusBadge
                  label={getTransTypeLabel(state.transType)}
                  variant={
                    state.transType === 'BY'
                      ? 'success'
                      : state.transType === 'SL'
                        ? 'error'
                        : 'warning'
                  }
                />
              </dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500">Fund ID</dt>
              <dd className="mt-0.5 text-sm font-medium text-slate-900 font-mono">{state.fundId}</dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500">Transaction Date</dt>
              <dd className="mt-0.5 text-sm font-medium text-slate-900">{state.transDate}</dd>
            </div>
            {state.transType !== 'FE' && (
              <div>
                <dt className="text-xs text-slate-500">Quantity</dt>
                <dd className="mt-0.5 text-sm font-medium text-slate-900">{formatNumber(state.shareQty)}</dd>
              </div>
            )}
            {state.transType !== 'FE' && (
              <div>
                <dt className="text-xs text-slate-500">Price per Share</dt>
                <dd className="mt-0.5 text-sm font-medium text-slate-900">{formatCurrency(state.price)}</dd>
              </div>
            )}
            <div>
              <dt className="text-xs text-slate-500">Total Amount</dt>
              <dd className="mt-0.5 text-lg font-bold text-slate-900">{formatCurrency(state.amount)}</dd>
            </div>
          </dl>

          {/* Position Balance Changes */}
          {state.transType !== 'FE' && (
            <div className="rounded-md bg-slate-50 p-4 ring-1 ring-inset ring-slate-200">
              <h4 className="text-sm font-semibold text-slate-900 mb-3">Position Balance</h4>
              <div className="grid grid-cols-3 gap-6 text-center">
                <div>
                  <p className="text-xs text-slate-500">Before</p>
                  <p className="text-lg font-semibold text-slate-900">{formatNumber(state.beforeBalance)}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Change</p>
                  <p className={`text-lg font-semibold ${
                    state.transType === 'BY' ? 'text-emerald-600' : 'text-red-600'
                  }`}>
                    {state.transType === 'BY' ? '+' : '-'}{formatNumber(state.shareQty)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">After</p>
                  <p className="text-lg font-semibold text-slate-900">{formatNumber(state.afterBalance)}</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </Card>

      {/* Action Buttons */}
      <div className="mt-6 flex items-center justify-end gap-3">
        <button
          type="button"
          onClick={() => navigate('/transactions')}
          className="inline-flex items-center gap-2 rounded-md bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
        >
          View Transaction History
          <ArrowRight className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={() => navigate('/transactions/new')}
          className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          <Plus className="h-4 w-4" />
          Create Another Transaction
        </button>
      </div>
    </div>
  );
}
