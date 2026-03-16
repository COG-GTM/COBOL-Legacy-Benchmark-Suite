import { useNavigate } from "react-router-dom";
import { Home, AlertTriangle } from "lucide-react";

export default function NotFound() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center py-20">
      <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
        <AlertTriangle size={32} className="text-red-600" />
      </div>
      <h1 className="mb-2 text-2xl font-bold text-gray-900">
        Page Not Found
      </h1>
      <p className="mb-6 text-gray-500">
        The page you are looking for does not exist.
      </p>
      <button
        onClick={() => navigate("/")}
        className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
      >
        <Home size={16} />
        Back to Dashboard
      </button>
    </div>
  );
}
