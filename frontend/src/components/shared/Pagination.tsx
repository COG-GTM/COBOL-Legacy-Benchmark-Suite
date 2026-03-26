import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  totalItems?: number;
  pageSize?: number;
  /** Override the displayed start item (1-indexed). Used for variable-size pages (e.g. grouped pagination). */
  startItem?: number;
  /** Override the displayed end item. Used for variable-size pages (e.g. grouped pagination). */
  endItem?: number;
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  totalItems,
  pageSize,
  startItem,
  endItem,
}: PaginationProps) {
  const canGoPrevious = currentPage > 1;
  const canGoNext = currentPage < totalPages;

  return (
    <div className="flex items-center justify-between gap-4">
      <div className="text-sm text-[#94A3B8]">
        {totalItems !== undefined && startItem !== undefined && endItem !== undefined ? (
          <span>
            Showing {startItem}-{endItem} of {totalItems} items
          </span>
        ) : totalItems !== undefined && pageSize !== undefined ? (
          <span>
            Showing {Math.min((currentPage - 1) * pageSize + 1, totalItems)}-
            {Math.min(currentPage * pageSize, totalItems)} of {totalItems} items
          </span>
        ) : null}
      </div>
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="icon"
          onClick={() => onPageChange(1)}
          disabled={!canGoPrevious}
          aria-label="First page"
        >
          <ChevronsLeft className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={!canGoPrevious}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <span className="px-3 text-sm text-[#CBD5E1]">
          Page {currentPage} of {totalPages}
        </span>
        <Button
          variant="outline"
          size="icon"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={!canGoNext}
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          onClick={() => onPageChange(totalPages)}
          disabled={!canGoNext}
          aria-label="Last page"
        >
          <ChevronsRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
