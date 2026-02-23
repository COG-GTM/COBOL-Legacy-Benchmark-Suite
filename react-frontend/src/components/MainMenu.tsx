/**
 * MainMenu Component - Replaces MENMAP (BMS Map, INQSET.bms lines 7-18)
 *
 * Original BMS screen layout:
 * - Line 1: Title "Portfolio Management System" (PROT, BRT)
 * - Line 3: "Select Option:" (PROT)
 * - Line 5: "1. Portfolio Position Inquiry" (PROT)
 * - Line 6: "2. Transaction History" (PROT)
 * - Line 7: "3. Exit" (PROT)
 * - Line 9: OPTION field (1 char, UNPROT, NUM, IC)
 * - Line 23: ERRMSG field (78 chars, PROT, BRT, RED)
 *
 * Controlled by INQONLN P200-DISPLAY-MENU (CICS SEND MAP('INQMNU'))
 */

import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function MainMenu() {
  const [option, setOption] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const { logout } = useAuth();
  const navigate = useNavigate();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErrorMsg('');

    // Mirrors INQONLN EVALUATE WS-COMMAREA-FUNCTION
    switch (option) {
      case '1':
        // WHEN 'INQP' → PERFORM P300-PORTFOLIO-INQUIRY
        navigate('/portfolio');
        break;
      case '2':
        // WHEN 'INQH' → PERFORM P400-HISTORY-INQUIRY
        navigate('/history');
        break;
      case '3':
        // WHEN 'EXIT' → SET SESSION-TERMINATED TO TRUE
        logout();
        navigate('/');
        break;
      default:
        // WHEN OTHER → PERFORM P900-ERROR-ROUTINE
        setErrorMsg('Invalid option. Please select 1, 2, or 3.');
        break;
    }
  }

  return (
    <div className="screen-container">
      {/* Row 1: Title - DFHMDF POS=(1,1), ATTRB=(PROT,BRT) */}
      <h1 className="screen-title">Portfolio Management System</h1>

      {/* Row 3: Label */}
      <p className="screen-label">Select Option:</p>

      {/* Rows 5-7: Menu options */}
      <div className="menu-options">
        <p className="menu-option">1. Portfolio Position Inquiry</p>
        <p className="menu-option">2. Transaction History</p>
        <p className="menu-option">3. Exit</p>
      </div>

      {/* Row 9: OPTION field - DFHMDF LENGTH=1, ATTRB=(UNPROT,NUM,IC) */}
      <form onSubmit={handleSubmit} className="menu-form">
        <label htmlFor="option" className="screen-label">
          Option:
        </label>
        <input
          id="option"
          type="text"
          maxLength={1}
          value={option}
          onChange={(e) => {
            // NUM attribute: only allow numeric input
            const val = e.target.value;
            if (val === '' || /^[0-9]$/.test(val)) {
              setOption(val);
            }
          }}
          className="input-field input-option"
          autoFocus
          aria-label="Menu option selection"
        />
        <button type="submit" className="btn btn-primary">
          Enter
        </button>
      </form>

      {/* Row 23: ERRMSG - DFHMDF LENGTH=78, ATTRB=(PROT,BRT), COLOR=RED */}
      {errorMsg && (
        <div className="error-message" role="alert">
          {errorMsg}
        </div>
      )}
    </div>
  );
}
