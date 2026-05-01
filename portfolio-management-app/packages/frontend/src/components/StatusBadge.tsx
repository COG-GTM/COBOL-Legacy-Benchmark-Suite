// Status indicators matching COBOL status codes
// Portfolio: A=Active, C=Closed, S=Suspended
// Transaction: P=Pending, D=Done, F=Failed, R=Reversed
// Batch: R=Ready, A=Active, W=Waiting, D=Done, E=Error

const statusConfig: Record<string, { label: string; className: string }> = {
  A: { label: 'Active', className: 'bg-green-100 text-green-800' },
  C: { label: 'Closed', className: 'bg-gray-100 text-gray-800' },
  S: { label: 'Suspended', className: 'bg-yellow-100 text-yellow-800' },
  P: { label: 'Pending', className: 'bg-yellow-100 text-yellow-800' },
  D: { label: 'Done', className: 'bg-green-100 text-green-800' },
  F: { label: 'Failed', className: 'bg-red-100 text-red-800' },
  R: { label: 'Reversed', className: 'bg-gray-100 text-gray-600' },
  W: { label: 'Waiting', className: 'bg-blue-100 text-blue-800' },
  E: { label: 'Error', className: 'bg-red-100 text-red-800' },
};

export default function StatusBadge({ status }: { status: string }) {
  const config = statusConfig[status] || { label: status, className: 'bg-gray-100 text-gray-800' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}>
      {config.label}
    </span>
  );
}

// Transaction type badges (Buy=green, Sell=red, Transfer=blue, Fee=yellow)
const txnTypeConfig: Record<string, { label: string; className: string }> = {
  BU: { label: 'Buy', className: 'bg-green-100 text-green-800' },
  SL: { label: 'Sell', className: 'bg-red-100 text-red-800' },
  TR: { label: 'Transfer', className: 'bg-blue-100 text-blue-800' },
  FE: { label: 'Fee', className: 'bg-yellow-100 text-yellow-800' },
};

export function TransactionTypeBadge({ type }: { type: string }) {
  const config = txnTypeConfig[type] || { label: type, className: 'bg-gray-100 text-gray-800' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}>
      {config.label}
    </span>
  );
}

export function GainLossDisplay({ value }: { value: number }) {
  const isPositive = value >= 0;
  return (
    <span className={isPositive ? 'text-green-600 font-medium' : 'text-red-600 font-medium'}>
      {isPositive ? '+' : ''}{value.toLocaleString('en-US', { style: 'currency', currency: 'USD' })}
    </span>
  );
}
