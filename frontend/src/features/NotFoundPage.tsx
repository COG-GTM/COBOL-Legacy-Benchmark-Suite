import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <section className="page">
      <header className="page__header">
        <h1 className="page__title">Page not found</h1>
        <p className="page__subtitle">
          The page you requested does not exist.
        </p>
      </header>
      <div className="placeholder">
        <Link className="panel__action" to="/">
          Back to dashboard
        </Link>
      </div>
    </section>
  );
}
