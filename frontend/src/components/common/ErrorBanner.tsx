import { AlertCircle, AlertTriangle, CheckCircle, Info, X } from "lucide-react";
import type { ErrorBannerProps } from "../../types";

const severityConfig = {
  error: {
    bg: "bg-red-50",
    border: "border-red-500",
    text: "text-red-700",
    icon: AlertCircle,
  },
  warning: {
    bg: "bg-amber-50",
    border: "border-amber-500",
    text: "text-amber-700",
    icon: AlertTriangle,
  },
  info: {
    bg: "bg-blue-50",
    border: "border-blue-500",
    text: "text-blue-700",
    icon: Info,
  },
  success: {
    bg: "bg-green-50",
    border: "border-green-500",
    text: "text-green-700",
    icon: CheckCircle,
  },
};

export default function ErrorBanner({
  message,
  severity,
  onDismiss,
}: ErrorBannerProps) {
  const config = severityConfig[severity];
  const Icon = config.icon;

  return (
    <div
      className={`${config.bg} ${config.border} border-l-4 rounded-md p-4 mb-4 flex items-center justify-between`}
      role="alert"
    >
      <div className="flex items-center gap-3">
        <Icon className={`${config.text} shrink-0`} size={20} />
        <p className={`${config.text} text-sm font-medium`}>{message}</p>
      </div>
      <button
        onClick={onDismiss}
        className="text-gray-400 hover:text-gray-600 transition-colors"
        aria-label="Dismiss"
      >
        <X size={18} />
      </button>
    </div>
  );
}
