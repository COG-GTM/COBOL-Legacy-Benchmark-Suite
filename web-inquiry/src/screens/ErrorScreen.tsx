import { useNavigate } from 'react-router-dom';
import { ScreenFrame } from '../components/ScreenFrame';
import { useSession } from '../session/sessionContextValue';
import { usePfKeys } from '../hooks/usePfKeys';

/**
 * System Error screen (BMS ERRMAP). Shows the Error Code (response code) and
 * Details (error message) from the COMMAREA. "Press ENTER to continue" returns
 * to the menu, clearing the error — analogous to the legacy acknowledge flow.
 */
export function ErrorScreen() {
  const navigate = useNavigate();
  const { comm, clearError } = useSession();

  const acknowledge = () => {
    clearError();
    navigate('/menu');
  };

  usePfKeys({ onEnter: acknowledge, onPf3: acknowledge });

  const code = comm.responseCode !== 0 ? String(comm.responseCode) : 'SYS0001';
  const detail = comm.errorMsg || 'An unexpected system error occurred.';

  return (
    <ScreenFrame
      title="System Error"
      mapId="ERRMAP"
      functionKeys={[{ pf: 'PF3', label: 'Continue', onClick: acknowledge }]}
    >
      <div className="error-view">
        <div className="glyph" aria-hidden>
          ⚠
        </div>
        <div>
          <span style={{ color: 'var(--text-dim)', fontFamily: 'var(--mono)' }}>
            Error Code:{' '}
          </span>
          <span className="err-code">{code}</span>
        </div>
        <div className="err-detail">Details: {detail}</div>
        <button type="button" className="btn primary" onClick={acknowledge}>
          Acknowledge — Press ENTER to continue
        </button>
      </div>
    </ScreenFrame>
  );
}
