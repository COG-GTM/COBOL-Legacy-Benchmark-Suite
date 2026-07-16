interface PaginationProps {
  /** Current page, 1-based. */
  page: number;
  /** Total number of pages. */
  pageCount: number;
  onPageChange: (page: number) => void;
}

/**
 * Page navigation control. Replaces the legacy POSMAP PF7 (previous) / PF8
 * (next) function keys with accessible Previous / Next buttons.
 */
export function Pagination({ page, pageCount, onPageChange }: PaginationProps) {
  if (pageCount <= 1) {
    return null;
  }
  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="btn btn--ghost"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 1}
      >
        ‹ Previous
      </button>
      <span className="pagination__status" aria-live="polite">
        Page {page} of {pageCount}
      </span>
      <button
        type="button"
        className="btn btn--ghost"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= pageCount}
      >
        Next ›
      </button>
    </nav>
  );
}
