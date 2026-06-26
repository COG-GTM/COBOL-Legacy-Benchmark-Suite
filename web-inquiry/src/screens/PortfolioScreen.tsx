import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ScreenFrame } from '../components/ScreenFrame';
import { useSession } from '../session/sessionContextValue';
import { usePfKeys } from '../hooks/usePfKeys';
import { getPosition, validateAccount, ACCOUNT_LENGTH } from '../services/inquiryService';
import { formatMoney, formatUnits } from '../utils/format';
import type { Position } from '../types';

const STATUS_LABEL: Record<Position['status'], { text: string; cls: string }> = {
  A: { text: 'Active', cls: 'active' },
  C: { text: 'Closed', cls: 'closed' },
  P: { text: 'Pending', cls: 'pend' },
};

/**
 * Portfolio Position Inquiry (BMS POSMAP). Mirrors INQPORT:
 *  - account input -> READ POSFILE
 *  - FOUND  -> show Fund ID / Fund Name / Units / Cost Basis / Market Value
 *  - NOTFND -> inline "Position not found for account"
 *  - ERROR  -> route to the System Error screen (ERRMAP)
 */
export function PortfolioScreen() {
  const navigate = useNavigate();
  const { comm, setFunction, setAccountNo, setError, clearError } = useSession();
  const [account, setAccount] = useState(comm.accountNo);
  const [position, setPosition] = useState<Position | null>(null);
  const [localError, setLocalError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setFunction('INQP');
  }, [setFunction]);

  const submit = async (event?: React.FormEvent) => {
    event?.preventDefault();
    setPosition(null);

    const validationError = validateAccount(account);
    if (validationError) {
      setLocalError(validationError);
      return;
    }
    setLocalError('');
    setAccountNo(account.trim());
    setBusy(true);
    const result = await getPosition(account);
    setBusy(false);

    if (result.status === 'FOUND') {
      setPosition(result.position);
    } else if (result.status === 'NOT_FOUND') {
      setLocalError(result.errorMsg);
    } else {
      setError(result.errorMsg, result.responseCode);
      navigate('/error');
    }
  };

  const backToMenu = () => {
    clearError();
    navigate('/menu');
  };

  usePfKeys({ onPf3: backToMenu });

  const status = position ? STATUS_LABEL[position.status] : null;

  return (
    <ScreenFrame
      title="Portfolio Position Inquiry"
      mapId="POSMAP"
      errorMsg={localError || undefined}
      functionKeys={[{ pf: 'PF3', label: 'Exit', onClick: backToMenu }]}
    >
      <form className="inquiry-form" onSubmit={submit}>
        <div className="field">
          <label htmlFor="account">Account</label>
          <input
            id="account"
            value={account}
            maxLength={ACCOUNT_LENGTH}
            autoFocus
            placeholder="0000001001"
            onChange={(e) => setAccount(e.target.value)}
          />
        </div>
        <button type="submit" className="btn primary" disabled={busy}>
          {busy ? 'Reading…' : 'Inquire'}
        </button>
      </form>

      {busy && <div className="spinner">Reading POSFILE…</div>}

      {position && !busy && (
        <div className="detail-grid">
          <div className="detail-cell">
            <div className="label">Fund ID</div>
            <div className="value">{position.fundId}</div>
          </div>
          <div className="detail-cell">
            <div className="label">Fund Name</div>
            <div className="value">{position.fundName}</div>
          </div>
          <div className="detail-cell">
            <div className="label">Units</div>
            <div className="value">{formatUnits(position.units)}</div>
          </div>
          <div className="detail-cell">
            <div className="label">Cost Basis</div>
            <div className="value">
              {formatMoney(position.costBasis)} {position.currency}
            </div>
          </div>
          <div className="detail-cell span-2">
            <div className="label">Market Value</div>
            <div className="value">
              {formatMoney(position.marketValue)} {position.currency}
              {status && <span className={`pill ${status.cls}`}>{status.text}</span>}
            </div>
          </div>
        </div>
      )}
    </ScreenFrame>
  );
}
