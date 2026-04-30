"use client";

interface LoadingStateProps {
  message?: string;
}

export function LoadingState({ message = "Loading..." }: LoadingStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
      <p className="mt-3 text-sm text-gray-500">{message}</p>
    </div>
  );
}

export function TableSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <div className="animate-pulse space-y-3">
      <div className="h-10 rounded bg-gray-200" />
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-12 rounded bg-gray-100" />
      ))}
    </div>
  );
}
