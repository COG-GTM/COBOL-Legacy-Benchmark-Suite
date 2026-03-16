import { Loader2 } from "lucide-react";

interface LoadingSpinnerProps {
  size?: number;
  message?: string;
}

export default function LoadingSpinner({
  size = 32,
  message = "Loading...",
}: LoadingSpinnerProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12">
      <Loader2 className="animate-spin text-primary" size={size} />
      <p className="text-sm text-gray-500">{message}</p>
    </div>
  );
}
