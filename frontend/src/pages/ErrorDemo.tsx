import { useEffect, useState } from 'react';
import { InlineError, ErrorDetailModal, OfflineState } from '../components/errors';
import { useToast } from '../contexts/useToast';
import { useError } from '../contexts/useError';

/**
 * Demo / test page that wires up all error-handling scenarios.
 */
export default function ErrorDemo() {
  const { showToast } = useToast();
  const {
    inlineErrors,
    setInlineError,
    clearInlineError,
    modal,
    showErrorModal,
    dismissErrorModal,
    offline,
    setOffline,
  } = useError();

  const [accountNumber, setAccountNumber] = useState('');

  useEffect(() => {
    return () => clearInlineError('account');
  }, [clearInlineError]);

  const validateAccount = (value: string) => {
    setAccountNumber(value);
    if (value === '') {
      setInlineError('account', 'Account number is required');
    } else if (!/^\d+$/.test(value)) {
      setInlineError('account', 'Account number must be numeric');
    } else {
      clearInlineError('account');
    }
  };

  if (offline) {
    return <OfflineState onRetry={() => setOffline(false)} />;
  }

  return (
    <div style={{ maxWidth: 640, margin: '0 auto', padding: 24 }}>
      <h1>Error Handling Demo</h1>

      {/* Inline validation */}
      <section style={{ marginBottom: 32 }}>
        <h2>Inline Validation</h2>
        <label htmlFor="account-input" style={{ display: 'block', marginBottom: 4 }}>
          Account Number:
        </label>
        <input
          id="account-input"
          type="text"
          value={accountNumber}
          onChange={(e) => validateAccount(e.target.value)}
          placeholder="Enter numeric account number"
          style={{ padding: 8, width: '100%', boxSizing: 'border-box' }}
        />
        <InlineError
          message={inlineErrors['account'] ?? ''}
          severity="error"
          visible={!!inlineErrors['account']}
        />
      </section>

      {/* Toast demo */}
      <section style={{ marginBottom: 32 }}>
        <h2>Toast Notifications</h2>
        <button
          onClick={() =>
            showToast({
              message: 'Position not found for account',
              severity: 'warning',
            })
          }
          style={{ marginRight: 8 }}
        >
          Simulate Not Found
        </button>
        <button
          onClick={() =>
            showToast({ message: 'Operation completed successfully', severity: 'success' })
          }
        >
          Simulate Success
        </button>
      </section>

      {/* Error modal demo */}
      <section style={{ marginBottom: 32 }}>
        <h2>System Error Modal</h2>
        <button
          onClick={() =>
            showErrorModal(
              'SYSERR01',
              'Database connection failed during position inquiry',
            )
          }
        >
          Simulate System Error
        </button>
      </section>

      {/* Offline toggle */}
      <section style={{ marginBottom: 32 }}>
        <h2>Offline State</h2>
        <button onClick={() => setOffline(true)}>Simulate Offline</button>
      </section>

      <ErrorDetailModal
        open={modal.open}
        errorCode={modal.code}
        errorDetails={modal.details}
        onClose={dismissErrorModal}
      />
    </div>
  );
}
