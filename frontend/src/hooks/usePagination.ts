import { useState, useCallback } from "react";

interface PaginationState {
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  goToNext: () => void;
  goToPrevious: () => void;
  goToPage: (page: number) => void;
  setTotalPages: (total: number) => void;
  reset: () => void;
}

export function usePagination(initialPage = 1): PaginationState {
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [totalPages, setTotalPagesState] = useState(1);

  const goToNext = useCallback(() => {
    setCurrentPage((prev) => Math.min(prev + 1, totalPages));
  }, [totalPages]);

  const goToPrevious = useCallback(() => {
    setCurrentPage((prev) => Math.max(prev - 1, 1));
  }, []);

  const goToPage = useCallback(
    (page: number) => {
      setCurrentPage(Math.max(1, Math.min(page, totalPages)));
    },
    [totalPages]
  );

  const setTotalPages = useCallback((total: number) => {
    setTotalPagesState(Math.max(1, total));
  }, []);

  const reset = useCallback(() => {
    setCurrentPage(initialPage);
    setTotalPagesState(1);
  }, [initialPage]);

  return {
    currentPage,
    totalPages,
    hasNext: currentPage < totalPages,
    hasPrevious: currentPage > 1,
    goToNext,
    goToPrevious,
    goToPage,
    setTotalPages,
    reset,
  };
}
