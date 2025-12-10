import React from 'react';

/**
 * ErrorMessage Component
 * 
 * Displays error messages from API calls in a user-friendly format.
 * This component handles the display of errors that would typically be
 * shown in the CICS terminal message area (similar to WS-MESSAGE in INQONLN.cbl).
 * 
 * CRITICAL LIMITATIONS:
 * - Backend API endpoints need to be created before this frontend is functional
 * - Error codes from mainframe need middleware translation to user-friendly messages
 * - COBOL error handling patterns (ERRHNDL.cbl) need API mapping
 * 
 * Error Format Considerations:
 * - Mainframe errors often include program name, paragraph name, and error code
 * - Web errors should be translated to user-friendly messages
 * - Original error codes should be preserved for debugging
 * 
 * @param {Object} props - Component props
 * @param {string} props.message - The error message to display
 * @param {string} props.errorCode - Optional error code from the system
 * @param {string} props.severity - Error severity: 'error', 'warning', 'info'
 * @param {Function} props.onDismiss - Optional callback to dismiss the error
 * @param {boolean} props.showDetails - Whether to show technical details
 */
const ErrorMessage = ({ 
  message, 
  errorCode = null, 
  severity = 'error',
  onDismiss = null,
  showDetails = false 
}) => {
  if (!message) {
    return null;
  }

  const getSeverityStyles = () => {
    switch (severity) {
      case 'warning':
        return {
          backgroundColor: '#fef3cd',
          borderColor: '#ffc107',
          color: '#856404',
        };
      case 'info':
        return {
          backgroundColor: '#d1ecf1',
          borderColor: '#17a2b8',
          color: '#0c5460',
        };
      case 'error':
      default:
        return {
          backgroundColor: '#f8d7da',
          borderColor: '#dc3545',
          color: '#721c24',
        };
    }
  };

  const severityStyles = getSeverityStyles();

  return (
    <div 
      style={{ ...styles.container, ...severityStyles }}
      role="alert"
      aria-live="polite"
    >
      <div style={styles.content}>
        <div style={styles.messageSection}>
          <span style={styles.icon}>
            {severity === 'error' ? '⚠' : severity === 'warning' ? '!' : 'ℹ'}
          </span>
          <span style={styles.message}>{message}</span>
        </div>
        
        {showDetails && errorCode && (
          <div style={styles.details}>
            <span style={styles.errorCode}>Error Code: {errorCode}</span>
          </div>
        )}
      </div>
      
      {onDismiss && (
        <button 
          onClick={onDismiss}
          style={styles.dismissButton}
          aria-label="Dismiss error message"
        >
          ×
        </button>
      )}
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    padding: '1rem',
    borderRadius: '4px',
    borderWidth: '1px',
    borderStyle: 'solid',
    marginBottom: '1rem',
  },
  content: {
    flex: 1,
  },
  messageSection: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  icon: {
    fontSize: '1.25rem',
    fontWeight: 'bold',
  },
  message: {
    fontSize: '1rem',
  },
  details: {
    marginTop: '0.5rem',
    paddingTop: '0.5rem',
    borderTop: '1px solid rgba(0,0,0,0.1)',
    fontSize: '0.875rem',
    fontFamily: 'monospace',
  },
  errorCode: {
    opacity: 0.8,
  },
  dismissButton: {
    background: 'none',
    border: 'none',
    fontSize: '1.5rem',
    cursor: 'pointer',
    padding: '0 0.5rem',
    opacity: 0.5,
    lineHeight: 1,
  },
};

export default ErrorMessage;
