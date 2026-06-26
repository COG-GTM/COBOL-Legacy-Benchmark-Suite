import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ScreenFrame } from '../components/ScreenFrame';
import { useSession } from '../session/sessionContextValue';
import { authenticate } from '../services/inquiryService';

/**
 * Stubbed sign-on screen, analogous to the SECMGR USERID check performed by
 * INQONLN (EXEC CICS ASSIGN USERID + access verification). Any non-empty user
 * id is accepted today; wire `authenticate` to real auth later.
 */
export function LoginScreen() {
  const navigate = useNavigate();
  const { signOn } = useSession();
  const [userId, setUserId] = useState('INQUSER');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    const result = await authenticate(userId);
    setBusy(false);
    if (result.status === 'OK') {
      signOn(result.userId);
      navigate('/menu');
    } else {
      setError(result.errorMsg);
    }
  };

  return (
    <ScreenFrame title="Portfolio Management System" mapId="SIGNON" errorMsg={error}>
      <div className="login-view">
        <p className="sub">Sign on to the online inquiry subsystem (PINQ).</p>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="userid">User ID</label>
            <input
              id="userid"
              value={userId}
              maxLength={8}
              autoFocus
              onChange={(e) => setUserId(e.target.value)}
            />
          </div>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Verifying…' : 'Sign On'}
          </button>
          <p className="hint">
            Stubbed SECMGR check — any user id is accepted in this demo.
          </p>
        </form>
      </div>
    </ScreenFrame>
  );
}
