import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { messages } from '../i18n/messages';
import { useSession } from '../state/SessionContext';

/** Top bar with app name, user ID and a global Exit (section 9.2), plus skip link and main landmark. */
export function Layout() {
  const { session, canInquire, selectedAccountNo, endSession } = useSession();
  const navigate = useNavigate();

  function exit() {
    endSession();
    navigate('/signed-out', { replace: true });
  }

  const acct = selectedAccountNo ? `/${selectedAccountNo}` : '';

  return (
    <>
      <a href="#main" className="skip-link">
        {messages.app.skipToContent}
      </a>
      <header className="topbar">
        <div className="topbar__inner">
          <Link to="/" className="topbar__brand">
            <span className="topbar__logo" aria-hidden="true">
              PMS
            </span>
            <span>{messages.app.name}</span>
          </Link>
          {session && canInquire && (
            <nav aria-label={messages.app.primaryNav} className="topbar__nav">
              <NavLink to="/" end>
                {messages.menu.breadcrumb}
              </NavLink>
              <NavLink to={`/positions${acct}`}>{messages.positions.breadcrumb}</NavLink>
              <NavLink to={acct ? `/history${acct}?page=1` : '/history'}>{messages.history.breadcrumb}</NavLink>
            </nav>
          )}
          {session && (
            <div className="topbar__user">
              <span>
                <span className="visually-hidden">{messages.app.signedInAs} </span>
                <span className="topbar__userid" data-testid="user-id">
                  {session.userId}
                </span>
              </span>
              <button type="button" className="btn btn--ghost btn--on-dark" onClick={exit}>
                {messages.app.exit}
              </button>
            </div>
          )}
        </div>
      </header>
      <main id="main" className="main" tabIndex={-1}>
        <Outlet />
      </main>
    </>
  );
}
