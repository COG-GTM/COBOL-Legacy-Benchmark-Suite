import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ScreenFrame } from '../components/ScreenFrame';
import { useSession } from '../session/sessionContextValue';

/**
 * Session termination screen, reached via menu option 3 (EXIT) or PF3 on the
 * menu — analogous to INQONLN setting SESSION-TERMINATED and issuing
 * EXEC CICS RETURN. Signs the user off and offers to sign on again.
 */
export function ExitScreen() {
  const navigate = useNavigate();
  const { setFunction, signOff } = useSession();

  useEffect(() => {
    setFunction('EXIT');
    signOff();
  }, [setFunction, signOff]);

  return (
    <ScreenFrame title="Session Ended" mapId="EXIT">
      <div className="error-view">
        <div className="glyph" aria-hidden>
          ⏻
        </div>
        <p style={{ fontFamily: 'var(--mono)', color: 'var(--text-dim)' }}>
          The inquiry session has been terminated.
        </p>
        <button type="button" className="btn primary" onClick={() => navigate('/login')}>
          Sign On Again
        </button>
      </div>
    </ScreenFrame>
  );
}
