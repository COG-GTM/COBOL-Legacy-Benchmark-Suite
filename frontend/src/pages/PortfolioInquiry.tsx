import { useEffect, useState } from 'react';
import { InlineError } from '../components/errors';
import { useError } from '../contexts/useError';

/**
 * Stub Portfolio Position Inquiry page.
 * Maps to POSMAP from src/maps/INQSET.bms (lines 23-49).
 */
export default function PortfolioInquiry() {
  const { inlineErrors, setInlineError, clearInlineError } = useError();
  const [accountNumber, setAccountNumber] = useState('');

  useEffect(() => {
    return () => clearInlineError('posAccount');
  }, [clearInlineError]);

  const validate = (value: string) => {
    setAccountNumber(value);
    if (value === '') {
      setInlineError('posAccount', 'Account number is required');
    } else if (!/^\d+$/.test(value)) {
      setInlineError('posAccount', 'Account number must be numeric');
    } else {
      clearInlineError('posAccount');
    }
  };

  return (
    <div style={{ maxWidth: 640, margin: '0 auto', padding: 24 }}>
      <h1>Portfolio Position Inquiry</h1>

      <label htmlFor="pos-account" style={{ display: 'block', marginBottom: 4 }}>
        Account:
      </label>
      <input
        id="pos-account"
        type="text"
        value={accountNumber}
        onChange={(e) => validate(e.target.value)}
        placeholder="Enter account number"
        style={{ padding: 8, width: '100%', boxSizing: 'border-box' }}
      />
      <InlineError
        message={inlineErrors['posAccount'] ?? ''}
        severity="error"
        visible={!!inlineErrors['posAccount']}
      />

      {/* Placeholder for position details */}
      <div style={{ marginTop: 24, color: '#888' }}>
        <p>Fund ID: —</p>
        <p>Fund Name: —</p>
        <p>Units: —</p>
        <p>Cost Basis: —</p>
        <p>Market Value: —</p>
      </div>

      <p style={{ marginTop: 16, fontSize: '0.85rem', color: '#888' }}>
        PF3=Exit &nbsp; PF7=Previous &nbsp; PF8=Next
      </p>
    </div>
  );
}
