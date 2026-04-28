import type { Position } from '../types';
import { POSITION_STATUS_LABELS } from '../types';
import { formatCurrency, formatNumber, formatDate } from '../utils/validation';

interface PositionDetailProps {
  position: Position;
}

export function PositionDetail({ position }: PositionDetailProps) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-6">
      <div className="grid grid-cols-2 gap-4">
        <Field label="Portfolio ID" value={position.portfolioId} />
        <Field label="Date" value={formatDate(position.date)} />
        <Field label="Fund ID" value={position.investmentId} />
        <Field label="Fund Name" value={position.fundName} />
        <Field label="Units" value={formatNumber(position.quantity, 4)} />
        <Field label="Cost Basis" value={formatCurrency(position.costBasis)} />
        <Field label="Market Value" value={formatCurrency(position.marketValue)} />
        <Field label="Currency" value={position.currency} />
        <Field label="Status" value={POSITION_STATUS_LABELS[position.status]} />
        <Field label="Last Maintained" value={position.lastMaintUser} />
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-medium text-gray-900">{value}</p>
    </div>
  );
}
