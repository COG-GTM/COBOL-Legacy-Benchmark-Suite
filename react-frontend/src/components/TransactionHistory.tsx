/**
 * TransactionHistory Component - Replaces HISMAP (BMS Map, INQSET.bms lines 53-85)
 *
 * Original BMS screen layout:
 * - Line 1: Title "Transaction History Inquiry" (PROT, BRT)
 * - Line 3: HISAIN field (10 chars, UNPROT, IC)
 * - Line 5: Column headers Date/Type/Units/Price/Amount (PROT, BRT)
 * - Lines 7-16: ROW1-ROW10 (65 chars each, PROT, TURQUOISE)
 * - Line 22: "PF3=Exit  PF7=Previous  PF8=Next"
 * - Line 23: HISMSG (78 chars, RED)
 *
 * Controlled by INQHIST program:
 * - P100-INIT-PROGRAM: Initialize, connect DB2
 * - P150-DB2-CONNECT: CICS LINK PROGRAM('DB2ONLN')
 * - P200-GET-HISTORY: SQL SELECT FROM POSHIST via CURSMGR
 * - P250-FETCH-HISTORY: CURSMGR fetch data
 * - P300-FORMAT-DISPLAY: CICS SEND MAP('HISMAP')
 */

import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTransactionHistory } from '../api/portfolioApi';
import { useError } from '../context/ErrorContext';
import type { TransactionHistoryEntry } from '../types';

export default function TransactionHistory() {
  const [accountId, setAccountId] = useState('');
  const [entries, setEntries] = useState<TransactionHistoryEntry[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const { setSystemError } = useError();
  const navigate = useNavigate();

  async function fetchHistory(account: string, page: number) {
    setIsLoading(true);
    setMessage('');

    try {
      const response = await getTransactionHistory(account, page);

      if (response.success && response.data) {
        setEntries(response.data.entries);
        setCurrentPage(response.data.currentPage);
        setTotalPages(response.data.totalPages);
        setTotalCount(response.data.totalCount);
        setHasSearched(true);
      } else {
        setEntries([]);
        setMessage(response.error || 'No transaction history found for account');
        setHasSearched(true);
      }
    } catch {
      // Mirrors INQHIST P999-ERROR-ROUTINE
      setSystemError('INQHIST', 'Error retrieving transaction history');
      setMessage('Error retrieving transaction history');
    } finally {
      setIsLoading(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (accountId.trim()) {
      fetchHistory(accountId, 0);
    } else {
      setMessage('Account number is required');
    }
  }

  // PF7=Previous
  function handlePrevious() {
    if (currentPage > 0) {
      fetchHistory(accountId, currentPage - 1);
    }
  }

  // PF8=Next
  function handleNext() {
    if (currentPage < totalPages - 1) {
      fetchHistory(accountId, currentPage + 1);
    }
  }

  // PF3=Exit
  function handleExit() {
    navigate('/menu');
  }

  return (
    <div className="screen-container">
      {/* Row 1: Title - DFHMDF POS=(1,1), ATTRB=(PROT,BRT) */}
      <h1 className="screen-title">Transaction History Inquiry</h1>

      {/* Row 3: Account input - HISAIN DFHMDF POS=(3,12), LENGTH=10 */}
      <form onSubmit={handleSubmit} className="inquiry-form">
        <label htmlFor="hisAccountId" className="field-label">
          Account:
        </label>
        <input
          id="hisAccountId"
          type="text"
          maxLength={10}
          value={accountId}
          onChange={(e) => setAccountId(e.target.value)}
          className="input-field input-account"
          autoFocus
          placeholder="Enter account #"
          aria-label="Account number"
        />
        <button type="submit" className="btn btn-primary" disabled={isLoading}>
          {isLoading ? 'Loading...' : 'Inquire'}
        </button>
      </form>

      {/* Row 5: Column Headers - DFHMDF ATTRB=(PROT,BRT) */}
      {hasSearched && entries.length > 0 && (
        <>
          <table className="history-table">
            <thead>
              <tr>
                <th className="col-header">Date</th>
                <th className="col-header">Type</th>
                <th className="col-header">Units</th>
                <th className="col-header">Price</th>
                <th className="col-header">Amount</th>
              </tr>
            </thead>
            {/* Rows 7-16: ROW1-ROW10 - COLOR=TURQUOISE */}
            <tbody>
              {entries.map((entry, index) => (
                <tr key={`row-${index}`} className="history-row">
                  <td className="turquoise">{entry.date}</td>
                  <td className="turquoise">{entry.type}</td>
                  <td className="turquoise text-right">{entry.units}</td>
                  <td className="turquoise text-right">{entry.price}</td>
                  <td className="turquoise text-right">{entry.amount}</td>
                </tr>
              ))}
              {/* Pad empty rows to always show 10 rows like BMS ROW1-ROW10 */}
              {Array.from({ length: Math.max(0, 10 - entries.length) }).map((_, index) => (
                <tr key={`empty-${index}`} className="history-row empty-row">
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination info */}
          <div className="pagination-info">
            Page {currentPage + 1} of {totalPages} ({totalCount} total transactions)
          </div>
        </>
      )}

      {/* Row 22: Function keys - PF3=Exit PF7=Previous PF8=Next */}
      <div className="function-keys">
        <button onClick={handleExit} className="btn btn-func" title="PF3=Exit">
          PF3 Exit
        </button>
        <button
          onClick={handlePrevious}
          className="btn btn-func"
          disabled={entries.length === 0 || currentPage <= 0}
          title="PF7=Previous"
        >
          PF7 Previous
        </button>
        <button
          onClick={handleNext}
          className="btn btn-func"
          disabled={entries.length === 0 || currentPage >= totalPages - 1}
          title="PF8=Next"
        >
          PF8 Next
        </button>
      </div>

      {/* Row 23: HISMSG - DFHMDF LENGTH=78, ATTRB=(PROT,BRT), COLOR=RED */}
      {message && (
        <div className="error-message" role="alert">
          {message}
        </div>
      )}
    </div>
  );
}
