import React from 'react';
import { Link, useRouteError } from 'react-router-dom';

/**
 * ErrorPage Component
 * 
 * Handles display of error messages and provides navigation back to main functions.
 * This page handles unhandled routes and general application errors, similar to
 * how the CICS online controller (INQONLN.cbl) handles invalid function codes
 * and error conditions.
 * 
 * CRITICAL LIMITATIONS:
 * - Backend API endpoints need to be created before this frontend is functional
 * - Error logging integration with ERRHNDL.cbl needs backend implementation
 * - Error codes from mainframe need middleware translation
 * 
 * Error Handling Patterns (from COBOL system):
 * - ERRHNDL.cbl logs errors to DB2 and determines recovery actions
 * - Error messages include program name, paragraph name, and error code
 * - User-friendly messages are displayed while technical details are logged
 * 
 * @param {Object} props - Component props
 * @param {string} props.title - Custom error title (optional)
 * @param {string} props.message - Custom error message (optional)
 */
const ErrorPage = ({ title = null, message = null }) => {
  const routeError = useRouteError();
  
  const errorTitle = title || (routeError?.status === 404 
    ? 'Page Not Found' 
    : 'Application Error');
  
  const errorMessage = message || (routeError?.statusText || routeError?.message || 
    'An unexpected error has occurred. Please try again or contact support.');

  const errorCode = routeError?.status || 'ERR-UNKNOWN';

  return (
    <div style={styles.container}>
      <div style={styles.errorCard}>
        <div style={styles.iconContainer}>
          <span style={styles.errorIcon}>!</span>
        </div>
        
        <h1 style={styles.title}>{errorTitle}</h1>
        
        <p style={styles.message}>{errorMessage}</p>
        
        <div style={styles.errorDetails}>
          <span style={styles.errorCode}>Error Code: {errorCode}</span>
        </div>

        <div style={styles.actions}>
          <Link to="/" style={styles.primaryButton}>
            Return to Home
          </Link>
          <Link to="/portfolio" style={styles.secondaryButton}>
            Portfolio Inquiry
          </Link>
          <Link to="/transactions" style={styles.secondaryButton}>
            Transaction History
          </Link>
        </div>

        <div style={styles.helpSection}>
          <h3 style={styles.helpTitle}>Need Help?</h3>
          <p style={styles.helpText}>
            If you continue to experience issues, please contact your system administrator
            with the error code shown above.
          </p>
        </div>

        <div style={styles.systemNote}>
          <strong>System Note:</strong> This frontend application requires backend API 
          endpoints that are not yet implemented. Some errors may occur due to missing 
          API connectivity.
        </div>
      </div>
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '60vh',
    padding: '2rem',
  },
  errorCard: {
    maxWidth: '600px',
    width: '100%',
    backgroundColor: 'white',
    borderRadius: '8px',
    boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
    padding: '2rem',
    textAlign: 'center',
  },
  iconContainer: {
    width: '80px',
    height: '80px',
    borderRadius: '50%',
    backgroundColor: '#fee2e2',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    margin: '0 auto 1.5rem',
  },
  errorIcon: {
    fontSize: '3rem',
    color: '#dc3545',
    fontWeight: 'bold',
  },
  title: {
    margin: '0 0 1rem',
    fontSize: '1.75rem',
    color: '#1a365d',
  },
  message: {
    margin: '0 0 1.5rem',
    fontSize: '1rem',
    color: '#666',
    lineHeight: 1.6,
  },
  errorDetails: {
    marginBottom: '1.5rem',
    padding: '0.75rem',
    backgroundColor: '#f8f9fa',
    borderRadius: '4px',
  },
  errorCode: {
    fontFamily: 'monospace',
    fontSize: '0.875rem',
    color: '#666',
  },
  actions: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    marginBottom: '2rem',
  },
  primaryButton: {
    display: 'block',
    padding: '0.75rem 1.5rem',
    backgroundColor: '#1a365d',
    color: 'white',
    textDecoration: 'none',
    borderRadius: '4px',
    fontWeight: '500',
    transition: 'background-color 0.2s',
  },
  secondaryButton: {
    display: 'block',
    padding: '0.75rem 1.5rem',
    backgroundColor: 'transparent',
    color: '#1a365d',
    textDecoration: 'none',
    borderRadius: '4px',
    border: '1px solid #1a365d',
    fontWeight: '500',
    transition: 'background-color 0.2s',
  },
  helpSection: {
    marginTop: '1.5rem',
    paddingTop: '1.5rem',
    borderTop: '1px solid #eee',
  },
  helpTitle: {
    margin: '0 0 0.5rem',
    fontSize: '1rem',
    color: '#333',
  },
  helpText: {
    margin: 0,
    fontSize: '0.875rem',
    color: '#666',
  },
  systemNote: {
    marginTop: '1.5rem',
    padding: '1rem',
    backgroundColor: '#fff3cd',
    borderRadius: '4px',
    fontSize: '0.75rem',
    color: '#856404',
    textAlign: 'left',
  },
};

export default ErrorPage;
