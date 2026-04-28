import { useState } from 'react';
import { mockHistoryByAccount } from '../mocks/mockData';
import { TransactionTable } from '../components/TransactionTable';
import { InlineError } from '../components/InlineError';
import type { HistoryEntry } from '../types';

/**
 * Maps to HISMAP from INQSET.bms lines 53-85 and INQHIST program
 * Account number search, paginated transaction history with sorting/filtering
 */
export function TransactionHistoryPage() {
  const [accountInput, setAccountInput] = useState('');
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  const handleSearch = () => {
    setError('');
    setSearched(true);
    if (!accountInput.trim()) {
      setError('Account number is required');
      setHistory([]);
      return;
    }

    const records = mockHistoryByAccount[accountInput.trim()];
    if (!records || records.length === 0) {
      setError('No transaction history found for this account');
      setHistory([]);
      return;
    }

    setHistory(records);
  };

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Transaction History Inquiry</h1>

      {/* Search */}
      <div className="flex items-end gap-3 mb-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Account Number</label>
          <input
            type="text"
            value={accountInput}
            onChange={e => setAccountInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSearch(); }}
            maxLength={10}
            placeholder="10-digit account number"
            className="border border-gray-300 rounded-md px-3 py-2 text-sm w-48 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <button
          onClick={handleSearch}
          className="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700 transition-colors"
        >
          Search
        </button>
      </div>

      {error && <InlineError message={error} />}

      {history.length > 0 && <TransactionTable data={history} />}

      {searched && history.length === 0 && !error && (
        <p className="text-gray-500 text-sm">No records found.</p>
      )}
    </div>
  );
}
