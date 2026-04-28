import { useState } from 'react';
import { mockPositions, mockPortfolios } from '../mocks/mockData';
import { PositionDetail } from '../components/PositionDetail';
import { InlineError } from '../components/InlineError';
import type { Position } from '../types';

/**
 * Maps to POSMAP from INQSET.bms lines 23-49 and INQPORT program
 * Account number search, position detail display with PF7/PF8 navigation
 */
export function PositionInquiryPage() {
  const [accountInput, setAccountInput] = useState('');
  const [positions, setPositions] = useState<Position[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  const handleSearch = () => {
    setError('');
    setSearched(true);
    if (!accountInput.trim()) {
      setError('Account number is required');
      setPositions([]);
      return;
    }

    const portfolios = mockPortfolios.filter(p => p.accountNumber === accountInput.trim());
    if (portfolios.length === 0) {
      setError('Position not found');
      setPositions([]);
      return;
    }

    const portfolioIds = portfolios.map(p => p.portfolioId);
    const results = mockPositions.filter(p => portfolioIds.includes(p.portfolioId));
    if (results.length === 0) {
      setError('Position not found');
      setPositions([]);
      return;
    }

    setPositions(results);
    setCurrentIndex(0);
  };

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Portfolio Position Inquiry</h1>

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

      {positions.length > 0 && (
        <>
          <div className="mb-4 text-sm text-gray-500">
            Position {currentIndex + 1} of {positions.length}
          </div>

          <PositionDetail position={positions[currentIndex]} />

          {/* PF7/PF8 navigation */}
          <div className="flex gap-3 mt-4">
            <button
              onClick={() => setCurrentIndex(i => Math.max(0, i - 1))}
              disabled={currentIndex === 0}
              className="bg-gray-200 text-gray-700 px-4 py-2 rounded text-sm hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              PF7 Previous
            </button>
            <button
              onClick={() => setCurrentIndex(i => Math.min(positions.length - 1, i + 1))}
              disabled={currentIndex === positions.length - 1}
              className="bg-gray-200 text-gray-700 px-4 py-2 rounded text-sm hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              PF8 Next
            </button>
          </div>
        </>
      )}

      {searched && positions.length === 0 && !error && (
        <p className="text-gray-500 text-sm">No positions found for this account.</p>
      )}
    </div>
  );
}
