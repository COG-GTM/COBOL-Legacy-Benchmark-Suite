"use client";

import { useState, useEffect, useCallback } from "react";
import { X, AlertCircle, AlertTriangle, Info, CheckCircle } from "lucide-react";

export type ToastSeverity = "error" | "warning" | "info" | "success";

export interface ToastMessage {
  id: string;
  severity: ToastSeverity;
  title: string;
  message?: string;
  duration?: number;
}

const TOAST_STYLES: Record<ToastSeverity, { bg: string; icon: string }> = {
  error: { bg: "bg-red-50 border-red-300", icon: "text-red-500" },
  warning: { bg: "bg-amber-50 border-amber-300", icon: "text-amber-500" },
  info: { bg: "bg-blue-50 border-blue-300", icon: "text-blue-500" },
  success: { bg: "bg-emerald-50 border-emerald-300", icon: "text-emerald-500" },
};

const TOAST_ICONS = {
  error: AlertCircle,
  warning: AlertTriangle,
  info: Info,
  success: CheckCircle,
} as const;

interface ToastItemProps {
  toast: ToastMessage;
  onDismiss: (id: string) => void;
}

function ToastItem({ toast, onDismiss }: ToastItemProps) {
  const styles = TOAST_STYLES[toast.severity];
  const Icon = TOAST_ICONS[toast.severity];

  useEffect(() => {
    const duration = toast.duration ?? 5000;
    if (duration <= 0) return;
    const timer = setTimeout(() => onDismiss(toast.id), duration);
    return () => clearTimeout(timer);
  }, [toast.id, toast.duration, onDismiss]);

  return (
    <div
      className={`flex items-start gap-3 rounded-lg border p-4 shadow-lg ${styles.bg} animate-[slideIn_0.3s_ease-out]`}
      role="alert"
    >
      <Icon className={`h-5 w-5 mt-0.5 shrink-0 ${styles.icon}`} />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900">{toast.title}</p>
        {toast.message && (
          <p className="mt-1 text-sm text-gray-600">{toast.message}</p>
        )}
      </div>
      <button
        type="button"
        onClick={() => onDismiss(toast.id)}
        className="shrink-0 rounded p-1 text-gray-400 hover:text-gray-600"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}

interface ToastContainerProps {
  toasts: ToastMessage[];
  onDismiss: (id: string) => void;
}

export default function ToastContainer({ toasts, onDismiss }: ToastContainerProps) {
  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 w-96">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={onDismiss} />
      ))}
    </div>
  );
}

export function useToasts() {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = useCallback(
    (toast: Omit<ToastMessage, "id">) => {
      const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
      setToasts((prev) => [...prev, { ...toast, id }]);
    },
    []
  );

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return { toasts, addToast, dismissToast };
}
