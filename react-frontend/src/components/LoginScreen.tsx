/**
 * LoginScreen Component - Replaces SECMGR initial validation
 *
 * In the COBOL system, INQONLN P050-SECURITY-CHECK called SECMGR with:
 * - 'V' (Validate): CICS ASSIGN USERID
 * - 'A' (Authorize): DB2 SELECT FROM AUTHFILE
 * - 'L' (Log): DB2 INSERT INTO AUDITLOG
 *
 * This component provides the user login interface before accessing
 * the main menu, replacing the CICS terminal sign-on process.
 */

import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginScreen() {
  const [userId, setUserId] = useState('');
  const { login, error, isLoading } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const success = await login(userId);
    if (success) {
      navigate('/menu');
    }
  }

  return (
    <div className="screen-container">
      <h1 className="screen-title">Portfolio Management System</h1>
      <p className="screen-subtitle">CICS Terminal Sign-On</p>

      <form onSubmit={handleSubmit} className="login-form">
        <div className="login-field">
          <label htmlFor="userId" className="field-label">
            User ID:
          </label>
          <input
            id="userId"
            type="text"
            maxLength={8}
            value={userId}
            onChange={(e) => setUserId(e.target.value.toUpperCase())}
            className="input-field input-userid"
            autoFocus
            placeholder="Enter User ID"
            aria-label="User ID"
          />
        </div>

        <button type="submit" className="btn btn-primary" disabled={isLoading}>
          {isLoading ? 'Validating...' : 'Sign On'}
        </button>

        <p className="login-hint">
          Valid users: ADMIN, USER01, USER02, ANALYST, DEMO
        </p>
      </form>

      {/* Error message area - mirrors ERRMSG field */}
      {error && (
        <div className="error-message" role="alert">
          {error}
        </div>
      )}
    </div>
  );
}
