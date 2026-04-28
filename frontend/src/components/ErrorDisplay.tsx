/**
 * Maps to ERRMAP from INQSET.bms lines 89-101
 * Shows error code + description with "Press ENTER to continue" pattern
 */
interface ErrorDisplayProps {
  code: string;
  details: string;
  onDismiss?: () => void;
}

export function ErrorDisplay({ code, details, onDismiss }: ErrorDisplayProps) {
  return (
    <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-lg">
      <h2 className="text-lg font-bold text-red-700 mb-4">System Error</h2>
      <div className="mb-3">
        <span className="text-sm text-gray-600">Error Code: </span>
        <span className="font-mono text-red-700 font-bold">{code}</span>
      </div>
      <div className="mb-4">
        <span className="text-sm text-gray-600">Details: </span>
        <span className="text-red-700">{details}</span>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className="text-sm text-blue-600 hover:text-blue-800 underline"
        >
          Press ENTER to continue
        </button>
      )}
    </div>
  );
}
