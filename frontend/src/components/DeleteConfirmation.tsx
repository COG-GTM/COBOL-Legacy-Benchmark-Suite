import { useState } from 'react';
import type { DeleteReasonCode } from '../types';
import { DELETE_REASON_LABELS } from '../types';

/**
 * Maps to audit write in PORTDEL.cbl lines 168-182
 * Delete confirmation dialog with reason code capture
 */
interface DeleteConfirmationProps {
  portfolioId: string;
  onConfirm: (reasonCode: DeleteReasonCode) => void;
  onCancel: () => void;
}

export function DeleteConfirmation({ portfolioId, onConfirm, onCancel }: DeleteConfirmationProps) {
  const [reasonCode, setReasonCode] = useState<DeleteReasonCode>('01');

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50">
      <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-bold text-red-700 mb-2">Confirm Deletion</h3>
        <p className="text-sm text-gray-600 mb-4">
          Are you sure you want to delete portfolio <strong>{portfolioId}</strong>?
          This action cannot be undone.
        </p>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">Reason Code</label>
          <select
            value={reasonCode}
            onChange={e => setReasonCode(e.target.value as DeleteReasonCode)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-red-500"
          >
            {(Object.entries(DELETE_REASON_LABELS) as [DeleteReasonCode, string][]).map(([code, label]) => (
              <option key={code} value={code}>{code} - {label}</option>
            ))}
          </select>
        </div>

        <div className="flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onConfirm(reasonCode)}
            className="px-4 py-2 text-sm bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
          >
            Delete Portfolio
          </button>
        </div>
      </div>
    </div>
  );
}
