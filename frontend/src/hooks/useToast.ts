import { useCallback, useSyncExternalStore } from 'react'
import type { AppError, ErrorSeverity } from '@/types/errors'
import { createAppError } from '@/types/errors'

export interface Toast {
  id: string
  error: AppError
  duration: number
  createdAt: number
}

interface ToastOptions {
  duration?: number
}

const DEFAULT_DURATIONS: Record<ErrorSeverity, number> = {
  error: 8000,
  warning: 5000,
  info: 3000,
}

const MAX_TOASTS = 5

let toasts: Toast[] = []
let listeners: Array<() => void> = []
let nextId = 0
const timers = new Map<string, ReturnType<typeof setTimeout>>()

function emitChange() {
  for (const listener of listeners) {
    listener()
  }
}

function subscribe(listener: () => void): () => void {
  listeners = [...listeners, listener]
  return () => {
    listeners = listeners.filter((l) => l !== listener)
  }
}

function getSnapshot(): Toast[] {
  return toasts
}

export function addToast(error: AppError, options?: ToastOptions): string {
  const id = `toast-${++nextId}`
  const duration = options?.duration ?? DEFAULT_DURATIONS[error.severity]

  const toast: Toast = {
    id,
    error,
    duration,
    createdAt: Date.now(),
  }

  const previous = toasts
  toasts = [...toasts, toast].slice(-MAX_TOASTS)

  const currentIds = new Set(toasts.map((t) => t.id))
  for (const t of previous) {
    if (!currentIds.has(t.id)) {
      const evictedTimer = timers.get(t.id)
      if (evictedTimer !== undefined) {
        clearTimeout(evictedTimer)
        timers.delete(t.id)
      }
    }
  }

  emitChange()

  if (duration > 0) {
    const timerId = setTimeout(() => {
      timers.delete(id)
      dismissToast(id)
    }, duration)
    timers.set(id, timerId)
  }

  return id
}

export function dismissToast(id: string): void {
  const timerId = timers.get(id)
  if (timerId !== undefined) {
    clearTimeout(timerId)
    timers.delete(id)
  }
  toasts = toasts.filter((t) => t.id !== id)
  emitChange()
}

export function clearAllToasts(): void {
  timers.forEach((timerId) => clearTimeout(timerId))
  timers.clear()
  toasts = []
  emitChange()
}

export function toast(
  message: string,
  severity: ErrorSeverity = 'info',
  options?: ToastOptions,
): string {
  const error = createAppError('TOAST', message, severity)
  return addToast(error, options)
}

export function useToast() {
  const currentToasts = useSyncExternalStore(subscribe, getSnapshot, getSnapshot)

  const addErrorToast = useCallback((error: AppError, options?: ToastOptions) => {
    return addToast(error, options)
  }, [])

  const showToast = useCallback(
    (message: string, severity: ErrorSeverity = 'info', options?: ToastOptions) => {
      return toast(message, severity, options)
    },
    [],
  )

  const dismiss = useCallback((id: string) => {
    dismissToast(id)
  }, [])

  const clearAll = useCallback(() => {
    clearAllToasts()
  }, [])

  return {
    toasts: currentToasts,
    addToast: addErrorToast,
    toast: showToast,
    dismiss,
    clearAll,
  }
}
