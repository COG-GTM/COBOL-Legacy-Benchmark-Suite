/**
 * ErrorDisplay Component - Replaces ERRMAP (BMS Map, INQSET.bms lines 89-101)
 *
 * Original BMS screen layout:
 * - Line 1: Title "System Error" (PROT, BRT)
 * - Line 3: "Error Code:" label + ERRCOUT (8 chars, PROT, RED)
 * - Line 5: "Details:" label + ERRDOUT (65 chars, PROT, RED)
 * - Line 22: "Press ENTER to continue" (PROT)
 *
 * Displayed when ERRHNDL determines an error needs user acknowledgment.
 * The COBOL ERRHNDL P400-DETERMINE-ACTION evaluates severity:
 * - ERR-FATAL → ERR-ABEND (CICS ABEND)
 * - ERR-WARNING → ERR-CONTINUE
 * - ERR-INFO → ERR-CONTINUE
 */

import { useNavigate } from 'react-router-dom';
import { useError } from '../context/ErrorContext';

export default function ErrorDisplay() {
  const { error, clearError } = useError();
  const navigate = useNavigate();

  function handleContinue() {
    clearError();
    navigate('/menu');
  }

  // If no error, redirect to menu
  if (!error) {
    return (
      <div className="screen-container">
        <h1 className="screen-title">System Error</h1>
        <p className="screen-label">No error to display.</p>
        <div className="function-keys">
          <button onClick={() => navigate('/menu')} className="btn btn-primary">
            Return to Menu
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="screen-container">
      {/* Row 1: Title - DFHMDF POS=(1,1), ATTRB=(PROT,BRT) */}
      <h1 className="screen-title">System Error</h1>

      {/* Row 3: Error Code - ERRCOUT DFHMDF POS=(3,12), LENGTH=8, COLOR=RED */}
      <div className="error-detail-row">
        <span className="field-label">Error Code:</span>
        <span className="field-value error-code">{error.code}</span>
      </div>

      {/* Row 5: Error Details - ERRDOUT DFHMDF POS=(5,12), LENGTH=65, COLOR=RED */}
      <div className="error-detail-row">
        <span className="field-label">Details:</span>
        <span className="field-value error-details">{error.details}</span>
      </div>

      {/* Row 22: "Press ENTER to continue" */}
      <div className="function-keys">
        <button onClick={handleContinue} className="btn btn-primary">
          Press ENTER to continue
        </button>
      </div>
    </div>
  );
}
