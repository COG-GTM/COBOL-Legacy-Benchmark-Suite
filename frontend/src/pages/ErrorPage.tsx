import { useSearchParams, useNavigate } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';

export default function ErrorPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const errorCode = params.get('code') || 'UNKNOWN';
  const errorDetail = params.get('detail') || 'An unexpected error has occurred.';

  return (
    <div className="min-h-[60vh] flex items-center justify-center">
      <div className="bg-white rounded-lg shadow-lg border border-red-200 max-w-lg w-full p-8 text-center">
        <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
        <h1 className="text-2xl font-bold text-gray-800 mb-2">System Error</h1>

        <div className="bg-red-50 rounded-md p-4 mb-6 text-left">
          <div className="mb-2">
            <span className="text-sm font-medium text-gray-600">Error Code:</span>
            <span className="ml-2 font-mono text-red-700">{errorCode}</span>
          </div>
          <div>
            <span className="text-sm font-medium text-gray-600">Details:</span>
            <p className="mt-1 text-sm text-red-700">{errorDetail}</p>
          </div>
        </div>

        <button
          onClick={() => navigate('/')}
          className="px-6 py-2.5 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 transition-colors"
        >
          Continue
        </button>

        <p className="mt-4 text-xs text-gray-400">
          Maps to ERRMAP from INQSET.bms — ERRCOUT / ERRDOUT fields
        </p>
      </div>
    </div>
  );
}
