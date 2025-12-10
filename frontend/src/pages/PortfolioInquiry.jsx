import React, { useState } from 'react';
import ErrorMessage from '../components/common/ErrorMessage';
import { getPortfolioPositions } from '../services/api';

/**
 * PortfolioInquiry Page
 * 
 * Provides form inputs for account number lookup and displays portfolio positions.
 * This page mirrors the functionality of INQPORT.cbl which handles portfolio
 * position inquiries from VSAM files.
 * 
 * CRITICAL LIMITATIONS:
 * - Backend API endpoints need to be created before this frontend is functional
 * - Data format conversions between mainframe and web formats need middleware implementation
 * - Authentication integration with existing COBOL SECMGR needs architectural planning
 * 
 * Mainframe Data Format Considerations:
 * - Account numbers: 9-digit numeric values (PIC 9(9))
 * - Fund IDs: 6-character alphanumeric (PIC X(6))
 * - Share balance: Numeric with 3 decimal places (PIC 9(10)V999)
 * - Cost basis: Numeric with 2 decimal places (PIC 9(13)V99)
 * - Average cost: Numeric with 2 decimal places (PIC 9(7)V99)
 * - Dates: YYYYMMDD format (PIC 9(8))
 * 
 * Security Placeholder:
 * The existing system validates user access through SECMGR.cbl before
 * allowing portfolio inquiries. User authentication state should be
 * checked before displaying sensitive financial data.
 * 
 * @param {Object} props - Component props
 * @param {Object} props.user - User authentication state (placeholder)
 * @param {boolean} props.isAuthenticated - Authentication status (placeholder)
 */
const PortfolioInquiry = ({ user = null, isAuthenticated = false }) => {
  const [accountNumber, setAccountNumber] = useState('');
  const [positions, setPositions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [hasSearched, setHasSearched] = useState(false);

  /**
   * Validates account number format
   * Account numbers must be exactly 9 digits (matching mainframe PIC 9(9))
   * 
   * @param {string} value - The account number to validate
   * @returns {Object} - Validation result with isValid and message
   */
  const validateAccountNumber = (value) => {
    if (!value) {
      return { isValid: false, message: 'Account number is required' };
    }
    
    if (!/^\d+$/.test(value)) {
      return { isValid: false, message: 'Account number must contain only digits' };
    }
    
    if (value.length !== 9) {
      return { 
        isValid: false, 
        message: `Account number must be exactly 9 digits (currently ${value.length})` 
      };
    }
    
    return { isValid: true, message: null };
  };

  /**
   * Handles account number input change
   * Restricts input to numeric characters only
   */
  const handleAccountNumberChange = (e) => {
    const value = e.target.value.replace(/\D/g, '').slice(0, 9);
    setAccountNumber(value);
    setError(null);
  };

  /**
   * Handles form submission for portfolio inquiry
   * Validates input and calls the API service
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const validation = validateAccountNumber(accountNumber);
    if (!validation.isValid) {
      setError({ message: validation.message, severity: 'error' });
      return;
    }

    setLoading(true);
    setError(null);
    setHasSearched(true);

    try {
      const result = await getPortfolioPositions(accountNumber);
      setPositions(result.positions || []);
    } catch (err) {
      setError({
        message: err.message || 'Failed to retrieve portfolio positions',
        errorCode: err.code,
        severity: 'error',
      });
      setPositions([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Formats share quantity for display
   * Mainframe format: 3 decimal places
   */
  const formatShareQuantity = (value) => {
    if (value === null || value === undefined) return '-';
    return Number(value).toLocaleString('en-US', {
      minimumFractionDigits: 3,
      maximumFractionDigits: 3,
    });
  };

  /**
   * Formats currency amount for display
   * Mainframe format: 2 decimal places
   */
  const formatCurrency = (value) => {
    if (value === null || value === undefined) return '-';
    return Number(value).toLocaleString('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Portfolio Inquiry</h1>
        <p style={styles.subtitle}>
          View portfolio positions and balances for an account
        </p>
      </div>

      <div style={styles.apiWarning}>
        <strong>Note:</strong> This interface requires backend API endpoints that are not yet implemented.
        The inquiry will return mock data or errors until the API layer is built.
      </div>

      <form onSubmit={handleSubmit} style={styles.form}>
        <div style={styles.formGroup}>
          <label htmlFor="accountNumber" style={styles.label}>
            Account Number
            <span style={styles.labelHint}>(9-digit numeric)</span>
          </label>
          <div style={styles.inputWrapper}>
            <input
              type="text"
              id="accountNumber"
              value={accountNumber}
              onChange={handleAccountNumberChange}
              placeholder="Enter 9-digit account number"
              style={styles.input}
              maxLength={9}
              pattern="\d{9}"
              required
              aria-describedby="accountNumberHelp"
            />
            <span style={styles.inputCounter}>
              {accountNumber.length}/9
            </span>
          </div>
          <small id="accountNumberHelp" style={styles.helpText}>
            Enter the 9-digit account number to retrieve portfolio positions
          </small>
        </div>

        <button 
          type="submit" 
          style={styles.submitButton}
          disabled={loading || accountNumber.length !== 9}
        >
          {loading ? 'Searching...' : 'Search Portfolio'}
        </button>
      </form>

      {error && (
        <ErrorMessage
          message={error.message}
          errorCode={error.errorCode}
          severity={error.severity}
          onDismiss={() => setError(null)}
          showDetails={true}
        />
      )}

      {hasSearched && !loading && !error && (
        <div style={styles.resultsSection}>
          <h2 style={styles.resultsTitle}>
            Portfolio Positions for Account: {accountNumber}
          </h2>
          
          {positions.length === 0 ? (
            <div style={styles.noResults}>
              No portfolio positions found for this account.
            </div>
          ) : (
            <div style={styles.tableContainer}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th style={styles.th}>Fund ID</th>
                    <th style={styles.th}>Share Balance</th>
                    <th style={styles.th}>Cost Basis</th>
                    <th style={styles.th}>Average Cost</th>
                  </tr>
                </thead>
                <tbody>
                  {positions.map((position, index) => (
                    <tr key={position.fundId || index} style={styles.tr}>
                      <td style={styles.td}>{position.fundId || '-'}</td>
                      <td style={{ ...styles.td, ...styles.numeric }}>
                        {formatShareQuantity(position.shareBalance)}
                      </td>
                      <td style={{ ...styles.td, ...styles.numeric }}>
                        {formatCurrency(position.costBasis)}
                      </td>
                      <td style={{ ...styles.td, ...styles.numeric }}>
                        {formatCurrency(position.averageCost)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div style={styles.dataFormatNote}>
            <strong>Data Format Notes:</strong>
            <ul style={styles.formatList}>
              <li>Fund ID: 6-character alphanumeric code</li>
              <li>Share Balance: Quantity with 3 decimal places</li>
              <li>Cost Basis: Total investment amount (2 decimal places)</li>
              <li>Average Cost: Cost per share (2 decimal places)</li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

const styles = {
  container: {
    maxWidth: '900px',
    margin: '0 auto',
  },
  header: {
    marginBottom: '1.5rem',
  },
  title: {
    margin: 0,
    fontSize: '1.75rem',
    color: '#1a365d',
  },
  subtitle: {
    margin: '0.5rem 0 0',
    color: '#666',
    fontSize: '1rem',
  },
  apiWarning: {
    backgroundColor: '#fff3cd',
    border: '1px solid #ffc107',
    borderRadius: '4px',
    padding: '1rem',
    marginBottom: '1.5rem',
    color: '#856404',
    fontSize: '0.875rem',
  },
  form: {
    backgroundColor: '#f8f9fa',
    padding: '1.5rem',
    borderRadius: '8px',
    marginBottom: '1.5rem',
  },
  formGroup: {
    marginBottom: '1rem',
  },
  label: {
    display: 'block',
    marginBottom: '0.5rem',
    fontWeight: '500',
    color: '#333',
  },
  labelHint: {
    fontWeight: 'normal',
    color: '#666',
    marginLeft: '0.5rem',
    fontSize: '0.875rem',
  },
  inputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
  },
  input: {
    width: '100%',
    padding: '0.75rem',
    paddingRight: '4rem',
    fontSize: '1rem',
    border: '1px solid #ccc',
    borderRadius: '4px',
    fontFamily: 'monospace',
    letterSpacing: '0.1em',
  },
  inputCounter: {
    position: 'absolute',
    right: '0.75rem',
    color: '#999',
    fontSize: '0.875rem',
    fontFamily: 'monospace',
  },
  helpText: {
    display: 'block',
    marginTop: '0.25rem',
    color: '#666',
    fontSize: '0.75rem',
  },
  submitButton: {
    backgroundColor: '#1a365d',
    color: 'white',
    border: 'none',
    padding: '0.75rem 1.5rem',
    fontSize: '1rem',
    borderRadius: '4px',
    cursor: 'pointer',
    fontWeight: '500',
  },
  resultsSection: {
    marginTop: '2rem',
  },
  resultsTitle: {
    fontSize: '1.25rem',
    color: '#333',
    marginBottom: '1rem',
    paddingBottom: '0.5rem',
    borderBottom: '2px solid #1a365d',
  },
  noResults: {
    padding: '2rem',
    textAlign: 'center',
    color: '#666',
    backgroundColor: '#f8f9fa',
    borderRadius: '4px',
  },
  tableContainer: {
    overflowX: 'auto',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    backgroundColor: 'white',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
  },
  th: {
    backgroundColor: '#1a365d',
    color: 'white',
    padding: '0.75rem 1rem',
    textAlign: 'left',
    fontWeight: '500',
    fontSize: '0.875rem',
  },
  tr: {
    borderBottom: '1px solid #eee',
  },
  td: {
    padding: '0.75rem 1rem',
    fontSize: '0.9375rem',
  },
  numeric: {
    textAlign: 'right',
    fontFamily: 'monospace',
  },
  dataFormatNote: {
    marginTop: '1.5rem',
    padding: '1rem',
    backgroundColor: '#e7f3ff',
    borderRadius: '4px',
    fontSize: '0.875rem',
    color: '#0c5460',
  },
  formatList: {
    margin: '0.5rem 0 0 1.5rem',
    padding: 0,
  },
};

export default PortfolioInquiry;
