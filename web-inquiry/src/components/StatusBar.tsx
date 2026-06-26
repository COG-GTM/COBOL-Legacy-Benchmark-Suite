import { useSession } from '../session/sessionContextValue';

/**
 * Top status line: transaction id (PINQ) + signed-on user indicator
 * (analogous to the SECMGR USERID shown on a 3270 session) with sign-off.
 */
export function StatusBar() {
  const { userId, isAuthenticated, signOff } = useSession();

  return (
    <div className="status-bar">
      <div className="brand">
        <span className="txid">PINQ</span>
        <span>Portfolio Online Inquiry</span>
      </div>
      {isAuthenticated && (
        <div className="user">
          <span className="dot" aria-hidden />
          <span>
            USERID: <strong>{userId}</strong>
          </span>
          <button type="button" className="linklike" onClick={signOff}>
            Sign off
          </button>
        </div>
      )}
    </div>
  );
}
