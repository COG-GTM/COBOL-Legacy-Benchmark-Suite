import { Link } from 'react-router-dom';
import { apiConfig } from '../api/client';
import { PageHeading } from '../components/PageHeading';
import { messages } from '../i18n/messages';
import { useDocumentTitle } from '../state/useDocumentTitle';

function startSignIn() {
  // Full navigation: a real deployment points VITE_AUTH_LOGIN_URL at the identity provider.
  window.location.assign(apiConfig.loginUrl || '/');
}

/** 403 / missing INQONLN:READ (FR-002). Legacy text "Access denied" (SECMGR.cbl:93-95). */
export function AccessDeniedScreen() {
  useDocumentTitle(messages.accessDenied.title);
  return (
    <section className="card info-screen" aria-labelledby="access-denied-title">
      <div id="access-denied-title">
        <PageHeading focusOnMount>{messages.accessDenied.title}</PageHeading>
      </div>
      <p role="alert" className="message message--error">
        <span className="message__icon" aria-hidden="true">×</span>
        <span>{messages.accessDenied.body}</span>
      </p>
    </section>
  );
}

/** 401 (FR-001): replaces EXEC CICS ASSIGN USERID failing. */
export function SignInScreen() {
  useDocumentTitle(messages.signIn.title);
  return (
    <section className="card info-screen" aria-labelledby="signin-title">
      <div id="signin-title">
        <PageHeading focusOnMount>{messages.signIn.title}</PageHeading>
      </div>
      <p role="alert" className="message message--warning">
        <span className="message__icon" aria-hidden="true">!</span>
        <span>{messages.errors.UNAUTHENTICATED}</span>
      </p>
      <p>{messages.signIn.body}</p>
      <button type="button" className="btn btn--primary" onClick={startSignIn}>
        {messages.signIn.action}
      </button>
    </section>
  );
}

/** FR-003: after Exit (menu option 3 or the top-bar Exit). */
export function SignedOutScreen() {
  useDocumentTitle(messages.signedOut.title);
  return (
    <section className="card info-screen" aria-labelledby="signed-out-title">
      <div id="signed-out-title">
        <PageHeading focusOnMount>{messages.signedOut.title}</PageHeading>
      </div>
      <p>{messages.signedOut.body}</p>
      <button type="button" className="btn btn--primary" onClick={startSignIn}>
        {messages.signedOut.action}
      </button>
    </section>
  );
}

export function NotFoundScreen() {
  useDocumentTitle(messages.app.notFoundTitle);
  return (
    <section className="card info-screen" aria-labelledby="not-found-title">
      <div id="not-found-title">
        <PageHeading focusOnMount>{messages.app.notFoundTitle}</PageHeading>
      </div>
      <p>{messages.app.notFoundBody}</p>
      <Link to="/">{messages.nav.backToMenu}</Link>
    </section>
  );
}
