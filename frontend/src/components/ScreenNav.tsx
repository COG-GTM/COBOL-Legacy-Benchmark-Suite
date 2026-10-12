import { Link, useNavigate } from 'react-router-dom';
import { messages } from '../i18n/messages';
import { useShortcuts } from '../state/useShortcuts';

/** Breadcrumb plus "Back to menu (PF3)" for S2 and S3 (FR-026). */
export function ScreenNav({ current, onPrevious, onNext }: { current: string; onPrevious?: () => void; onNext?: () => void }) {
  const navigate = useNavigate();
  useShortcuts({ onBack: () => navigate('/'), onPrevious, onNext });

  return (
    <div className="screen-nav">
      <nav aria-label={messages.app.breadcrumb} className="breadcrumb">
        <ol>
          <li>
            <Link to="/">{messages.menu.breadcrumb}</Link>
          </li>
          <li aria-current="page">{current}</li>
        </ol>
      </nav>
      <Link to="/" className="btn btn--ghost">
        <span aria-hidden="true">← </span>
        {messages.nav.backToMenu} <kbd aria-hidden="true">{messages.nav.backHint}</kbd>
      </Link>
    </div>
  );
}

export function ShortcutsHint({ paging }: { paging?: boolean }) {
  return (
    <p className="hint shortcuts-hint">
      {messages.nav.shortcutsHint}
      {paging ? ` · ${messages.nav.pagingShortcutsHint}` : ''}
    </p>
  );
}
