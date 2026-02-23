/**
 * PortfolioInquiry Component - Replaces POSMAP (BMS Map, INQSET.bms lines 23-49)
 *
 * Original BMS screen layout:
 * - Line 1: Title "Portfolio Position Inquiry" (PROT, BRT)
 * - Line 3: Account label + ACCTIN field (10 chars, UNPROT, IC)
 * - Line 5: FUNDOUT (6 chars, TURQUOISE), NAMEOUT (30 chars, TURQUOISE)
 * - Line 7: UNITOUT (15 chars, TURQUOISE)
 * - Line 9: COSTOUT (15 chars, TURQUOISE)
 * - Line 11: VALOUT (15 chars, TURQUOISE)
 * - Line 22: "PF3=Exit  PF7=Previous  PF8=Next"
 * - Line 23: POSMSG (78 chars, RED)
 *
 * Controlled by INQPORT program:
 * - P100-INIT-PROGRAM: Initialize from COMMAREA
 * - P200-GET-POSITION: CICS READ FILE('POSFILE')
 * - P300-FORMAT-DISPLAY: CICS SEND MAP('POSMAP')
 * - P900-NOT-FOUND: 'Position not found for account'
 */

import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPortfolioPositions } from '../api/portfolioApi';
import { useError } from '../context/ErrorContext';
import type { PortfolioPosition } from '../types';

export default function PortfolioInquiry() {
  const [accountId, setAccountId] = useState('');
  const [position, setPosition] = useState<PortfolioPosition | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { setSystemError } = useError();
  const navigate = useNavigate();

  async function fetchPosition(account: string, page: number) {
    setIsLoading(true);
    setMessage('');

    try {
      const response = await getPortfolioPositions(account, page);

      if (response.success && response.data) {
        setPosition(response.data.positions[0]);
        setCurrentIndex(response.data.currentIndex);
        setTotalCount(response.data.totalCount);
      } else {
        // Mirrors INQPORT P900-NOT-FOUND
        setPosition(null);
        setMessage(response.error || 'Position not found for account');
      }
    } catch {
      // Mirrors INQPORT P999-ERROR-ROUTINE
      setSystemError('INQPORT', 'Error accessing position data');
      setMessage('Error accessing position data');
    } finally {
      setIsLoading(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (accountId.trim()) {
      fetchPosition(accountId, 0);
    } else {
      setMessage('Account number is required');
    }
  }

  // PF7=Previous
  function handlePrevious() {
    if (currentIndex > 0) {
      fetchPosition(accountId, currentIndex - 1);
    }
  }

  // PF8=Next
  function handleNext() {
    if (currentIndex < totalCount - 1) {
      fetchPosition(accountId, currentIndex + 1);
    }
  }

  // PF3=Exit
  function handleExit() {
    navigate('/menu');
  }

  return (
    <div className="screen-container">
      {/* Row 1: Title - DFHMDF POS=(1,1), ATTRB=(PROT,BRT) */}
      <h1 className="screen-title">Portfolio Position Inquiry</h1>

      {/* Row 3: Account input - ACCTIN DFHMDF POS=(3,12), LENGTH=10, UNPROT, IC */}
      <form onSubmit={handleSubmit} className="inquiry-form">
        <label htmlFor="accountId" className="field-label">
          Account:
        </label>
        <input
          id="accountId"
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

      {/* Position Details - fields with COLOR=TURQUOISE */}
      {position && (
        <div className="position-details">
          <div className="detail-row">
            <span className="field-label">Fund ID:</span>
            <span className="field-value turquoise">{position.fundId}</span>
            <span className="field-label" style={{ marginLeft: '1rem' }}>Fund Name:</span>
            <span className="field-value turquoise">{position.fundName}</span>
          </div>
          <div className="detail-row">
            <span className="field-label">Units:</span>
            <span className="field-value turquoise">{position.units}</span>
          </div>
          <div className="detail-row">
            <span className="field-label">Cost Basis:</span>
            <span className="field-value turquoise">{position.costBasis}</span>
          </div>
          <div className="detail-row">
            <span className="field-label">Market Value:</span>
            <span className="field-value turquoise">{position.marketValue}</span>
          </div>

          {/* Pagination indicator */}
          <div className="pagination-info">
            Position {currentIndex + 1} of {totalCount}
          </div>
        </div>
      )}

      {/* Row 22: Function keys - PF3=Exit PF7=Previous PF8=Next */}
      <div className="function-keys">
        <button onClick={handleExit} className="btn btn-func" title="PF3=Exit">
          PF3 Exit
        </button>
        <button
          onClick={handlePrevious}
          className="btn btn-func"
          disabled={!position || currentIndex <= 0}
          title="PF7=Previous"
        >
          PF7 Previous
        </button>
        <button
          onClick={handleNext}
          className="btn btn-func"
          disabled={!position || currentIndex >= totalCount - 1}
          title="PF8=Next"
        >
          PF8 Next
        </button>
      </div>

      {/* Row 23: POSMSG - DFHMDF LENGTH=78, ATTRB=(PROT,BRT), COLOR=RED */}
      {message && (
        <div className="error-message" role="alert">
          {message}
        </div>
      )}
    </div>
  );
}
