import React, { useState } from 'react';
import ErrorMessage from '../components/common/ErrorMessage';
import { getTransactionHistory } from '../services/api';

/**
 * TransactionHistory Page
 * 
 * Provides form inputs for account number and date range, and displays
 * transaction records. This page mirrors the functionality of INQHIST.cbl
 * which retrieves transaction history from DB2 tables.
 * 
 * CRITICAL LIMITATIONS:
 * - Backend API endpoints need to be created before this frontend is functional
 * - Data format conversions between mainframe and web formats need middleware implementation
 * - Authentication integration with existing COBOL SECMGR needs architectural planning
 * 
 * Mainframe Data Format Considerations:
 * - Account numbers: 9-digit numeric values (PIC 9(9))
 * - Dates: YYYYMMDD format (PIC 9(8)) - need conversion to/from web date format
 * - Transaction types: 2-character codes - BY (Buy), SL (Sell), FE (Fee)
 * - Quantity: Numeric with 3 decimal places (PIC 9(10)V999)
 * - Price: Numeric with 2 decimal places (PIC 9(7)V99)
 * - Amount: Numeric with 2 decimal places (PIC 9(13)V99)
 * 
 * Transaction Type Codes (from COBOL system):
 * - BY: Buy transaction - purchase of fund shares
 * - SL: Sell transaction - redemption of fund shares
 * - FE: Fee transaction - management or service fees
 * 
 * Security Placeholder:
 * The existing system validates user access through SECMGR.cbl before
 * allowing transaction history inquiries. User authentication state should
 * be checked before displaying sensitive financial data.
 * 
 * @param {Object} props - Component props
 * @param {Object} props.user - User authentication state (placeholder)
 * @param {boolean} props.isAuthenticated - Authentication status (placeholder)
 */
const TransactionHistory = ({ user = null, isAuthenticated = false }) => {
  const [accountNumber, setAccountNumber] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [hasSearched, setHasSearched] = useState(false);

  /**
   * Transaction type mapping from mainframe codes to display labels
   */
  const transactionTypes = {
    BY: { label: 'Buy', color: '#28a745' },
    SL: { label: 'Sell', color: '#dc3545' },
    FE: { label: 'Fee', color: '#6c757d' },
  };

  /**
   * Validates account number format
   * Account numbers must be exactly 9 digits (matching mainframe PIC 9(9))
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
   * Validates date range
   * Ensures start date is before or equal to end date
   */
  const validateDateRange = () => {
    if (!startDate || !endDate) {
      return { isValid: false, message: 'Both start and end dates are required' };
    }
    if (new Date(startDate) > new Date(endDate)) {
      return { isValid: false, message: 'Start date must be before or equal to end date' };
    }
    return { isValid: true, message: null };
  };

  /**
   * Converts web date format (YYYY-MM-DD) to mainframe format (YYYYMMDD)
   * This conversion is needed for API calls to the backend
   */
  const convertToMainframeDate = (webDate) => {
    if (!webDate) return '';
    return webDate.replace(/-/g, '');
  };

  /**
   * Converts mainframe date format (YYYYMMDD) to display format
   */
  const formatDisplayDate = (mainframeDate) => {
    if (!mainframeDate || mainframeDate.length !== 8) return '-';
    const year = mainframeDate.substring(0, 4);
    const month = mainframeDate.substring(4, 6);
    const day = mainframeDate.substring(6, 8);
    return `${month}/${day}/${year}`;
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
   * Handles form submission for transaction history inquiry
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const accountValidation = validateAccountNumber(accountNumber);
    if (!accountValidation.isValid) {
      setError({ message: accountValidation.message, severity: 'error' });
      return;
    }

    const dateValidation = validateDateRange();
    if (!dateValidation.isValid) {
      setError({ message: dateValidation.message, severity: 'error' });
      return;
    }

    setLoading(true);
    setError(null);
    setHasSearched(true);

    try {
      const result = await getTransactionHistory(
        accountNumber,
        convertToMainframeDate(startDate),
        convertToMainframeDate(endDate)
      );
      setTransactions(result.transactions || []);
    } catch (err) {
      setError({
        message: err.message || 'Failed to retrieve transaction history',
        errorCode: err.code,
        severity: 'error',
      });
      setTransactions([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Formats share quantity for display
   * Mainframe format: 3 decimal places
   */
  const formatQuantity = (value) => {
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

  /**
   * Gets transaction type display info
   */
  const getTransactionTypeInfo = (typeCode) => {
    return transactionTypes[typeCode] || { label: typeCode, color: '#333' };
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Transaction History</h1>
        <p style={styles.subtitle}>
          View transaction records by account and date range
        </p>
      </div>

      <div style={styles.apiWarning}>
        <strong>Note:</strong> This interface requires backend API endpoints that are not yet implemented.
        The inquiry will return mock data or errors until the API layer is built.
      </div>

      <form onSubmit={handleSubmit} style={styles.form}>
        <div style={styles.formRow}>
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
              />
              <span style={styles.inputCounter}>
                {accountNumber.length}/9
              </span>
            </div>
          </div>
        </div>

        <div style={styles.formRow}>
          <div style={styles.formGroup}>
            <label htmlFor="startDate" style={styles.label}>
              Start Date
              <span style={styles.labelHint}>(YYYYMMDD format internally)</span>
            </label>
            <input
              type="date"
              id="startDate"
              value={startDate}
              onChange={(e) => { setStartDate(e.target.value); setError(null); }}
              style={styles.dateInput}
              required
            />
          </div>

          <div style={styles.formGroup}>
            <label htmlFor="endDate" style={styles.label}>
              End Date
              <span style={styles.labelHint}>(YYYYMMDD format internally)</span>
            </label>
            <input
              type="date"
              id="endDate"
              value={endDate}
              onChange={(e) => { setEndDate(e.target.value); setError(null); }}
              style={styles.dateInput}
              required
            />
          </div>
        </div>

        <div style={styles.transactionTypeLegend}>
          <span style={styles.legendTitle}>Transaction Types:</span>
          {Object.entries(transactionTypes).map(([code, info]) => (
            <span key={code} style={styles.legendItem}>
              <span style={{ ...styles.legendBadge, backgroundColor: info.color }}>
                {code}
              </span>
              {info.label}
            </span>
          ))}
        </div>

        <button 
          type="submit" 
          style={styles.submitButton}
          disabled={loading || accountNumber.length !== 9 || !startDate || !endDate}
        >
          {loading ? 'Searching...' : 'Search Transactions'}
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
            Transaction History for Account: {accountNumber}
          </h2>
          <p style={styles.dateRange}>
            Date Range: {startDate} to {endDate}
          </p>
          
          {transactions.length === 0 ? (
            <div style={styles.noResults}>
              No transactions found for this account in the specified date range.
            </div>
          ) : (
            <div style={styles.tableContainer}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th style={styles.th}>Type</th>
                    <th style={styles.th}>Date</th>
                    <th style={styles.th}>Fund ID</th>
                    <th style={styles.th}>Quantity</th>
                    <th style={styles.th}>Price</th>
                    <th style={styles.th}>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((transaction, index) => {
                    const typeInfo = getTransactionTypeInfo(transaction.transactionType);
                    return (
                      <tr key={transaction.transactionId || index} style={styles.tr}>
                        <td style={styles.td}>
                          <span style={{ 
                            ...styles.typeBadge, 
                            backgroundColor: typeInfo.color 
                          }}>
                            {typeInfo.label}
                          </span>
                        </td>
                        <td style={styles.td}>
                          {formatDisplayDate(transaction.transactionDate)}
                        </td>
                        <td style={styles.td}>{transaction.fundId || '-'}</td>
                        <td style={{ ...styles.td, ...styles.numeric }}>
                          {formatQuantity(transaction.quantity)}
                        </td>
                        <td style={{ ...styles.td, ...styles.numeric }}>
                          {formatCurrency(transaction.price)}
                        </td>
                        <td style={{ ...styles.td, ...styles.numeric }}>
                          {formatCurrency(transaction.amount)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          <div style={styles.dataFormatNote}>
            <strong>Data Format Notes:</strong>
            <ul style={styles.formatList}>
              <li>Transaction Type: BY (Buy), SL (Sell), FE (Fee)</li>
              <li>Date: Stored as YYYYMMDD in mainframe, displayed as MM/DD/YYYY</li>
              <li>Fund ID: 6-character alphanumeric code</li>
              <li>Quantity: Share count with 3 decimal places</li>
              <li>Price: Price per share (2 decimal places)</li>
              <li>Amount: Total transaction value (2 decimal places)</li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

const styles = {
  container: {
    maxWidth: '1000px',
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
  formRow: {
    display: 'flex',
    gap: '1.5rem',
    marginBottom: '1rem',
  },
  formGroup: {
    flex: 1,
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
  dateInput: {
    width: '100%',
    padding: '0.75rem',
    fontSize: '1rem',
    border: '1px solid #ccc',
    borderRadius: '4px',
  },
  inputCounter: {
    position: 'absolute',
    right: '0.75rem',
    color: '#999',
    fontSize: '0.875rem',
    fontFamily: 'monospace',
  },
  transactionTypeLegend: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    marginBottom: '1rem',
    padding: '0.75rem',
    backgroundColor: '#e9ecef',
    borderRadius: '4px',
    fontSize: '0.875rem',
  },
  legendTitle: {
    fontWeight: '500',
    color: '#333',
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.25rem',
  },
  legendBadge: {
    display: 'inline-block',
    padding: '0.125rem 0.375rem',
    borderRadius: '3px',
    color: 'white',
    fontSize: '0.75rem',
    fontWeight: '500',
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
    marginBottom: '0.5rem',
    paddingBottom: '0.5rem',
    borderBottom: '2px solid #1a365d',
  },
  dateRange: {
    color: '#666',
    fontSize: '0.875rem',
    marginBottom: '1rem',
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
  typeBadge: {
    display: 'inline-block',
    padding: '0.25rem 0.5rem',
    borderRadius: '3px',
    color: 'white',
    fontSize: '0.75rem',
    fontWeight: '500',
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

export default TransactionHistory;
