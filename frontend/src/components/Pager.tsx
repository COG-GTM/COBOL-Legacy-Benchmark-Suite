import { messages } from '../i18n/messages';

interface Props {
  page: number;
  totalPages: number;
  hasPrevious: boolean;
  hasNext: boolean;
  onPrevious: () => void;
  onNext: () => void;
}

/** PF7 / PF8 equivalents (FR-020). */
export function Pager({ page, totalPages, hasPrevious, hasNext, onPrevious, onNext }: Props) {
  return (
    <nav className="pager" aria-label={messages.paging.nav}>
      <button type="button" className="btn" onClick={onPrevious} disabled={!hasPrevious}>
        <span aria-hidden="true">‹ </span>
        {messages.paging.previous} <kbd aria-hidden="true">{messages.paging.previousHint}</kbd>
      </button>
      <span className="pager__status" aria-live="polite" data-testid="page-indicator">
        {messages.paging.pageOf(page, totalPages)}
      </span>
      <button type="button" className="btn" onClick={onNext} disabled={!hasNext}>
        {messages.paging.next} <kbd aria-hidden="true">{messages.paging.nextHint}</kbd>
        <span aria-hidden="true"> ›</span>
      </button>
    </nav>
  );
}
