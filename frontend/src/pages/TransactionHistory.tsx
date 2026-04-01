import { useState } from 'react';
import { InlineError } from '../components/errors';
import { useError } from '../contexts/useError';

/**
 * Stub Transaction History Inquiry page.
 * Maps to HISMAP from src/maps/INQSET.bms (lines 53-85).
 */
export default function TransactionHistory() {
  const { inlineErrors, setInlineError, clearInlineError } = useError();
  const [accountNumber, setAccountNumber] = useState('');

  const validate = (value: string) => {
    setAccountNumber(value);
    if (value === '') {
      setInlineError('hisAccount', 'Account number is required');
    } else if (!/^\d+$/.test(value)) {
      setInlineError('hisAccount', 'Account number must be numeric');
    } else {
      clearInlineError('hisAccount');
    }
  };

  return (
    <div style={{ maxWidth: 640, margin: '0 auto', padding: 24 }}>
      <h1>Transaction History Inquiry</h1>

      <label htmlFor="his-account" style={{ display: 'block', marginBottom: 4 }}>
        Account:
      </label>
      <input
        id="his-account"
        type="text"
        value={accountNumber}
        onChange={(e) => validate(e.target.value)}
        placeholder="Enter account number"
        style={{ padding: 8, width: '100%', boxSizing: 'border-box' }}
      />
      <InlineError
        message={inlineErrors['hisAccount'] ?? ''}
        severity="error"
        visible={!!inlineErrors['hisAccount']}
      />

      {/* Placeholder table headers matching HISMAP column headers */}
      <table style={{ width: '100%', marginTop: 24, borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ textAlign: 'left', borderBottom: '2px solid #ccc' }}>
            <th>Date</th>
            <th>Type</th>
            <th>Units</th>
            <th>Price</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td colSpan={5} style={{ textAlign: 'center', padding: 24, color: '#888' }}>
              No transactions to display
            </td>
          </tr>
        </tbody>
      </table>

      <p style={{ marginTop: 16, fontSize: '0.85rem', color: '#888' }}>
        PF3=Exit &nbsp; PF7=Previous &nbsp; PF8=Next
      </p>
    </div>
  );
}
