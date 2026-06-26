import { useEffect, useRef } from 'react';

const ACTIVITY_EVENTS: readonly (keyof WindowEventMap)[] = [
  'mousemove',
  'mousedown',
  'keydown',
  'touchstart',
  'scroll',
];

export interface UseIdleTimerOptions {
  /** Inactivity period (ms) before {@link onIdle} fires. */
  timeoutMs: number;
  /** How long before timeout (ms) {@link onWarning} fires. */
  warningMs: number;
  /** Called once the inactivity period elapses. */
  onIdle: () => void;
  /** Called when entering the warning window before timeout. */
  onWarning: () => void;
  /** Called on any user activity (used to clear an active warning). */
  onActivity?: () => void;
  /** When false the timer is disabled and no listeners are attached. */
  enabled: boolean;
}

/**
 * Tracks user inactivity and fires callbacks for the warning and timeout
 * thresholds. Mirrors the configurable idle-terminal timeout enforced by the
 * legacy CICS session manager.
 */
export function useIdleTimer({
  timeoutMs,
  warningMs,
  onIdle,
  onWarning,
  onActivity,
  enabled,
}: UseIdleTimerOptions): void {
  const idleTimer = useRef<ReturnType<typeof setTimeout>>();
  const warningTimer = useRef<ReturnType<typeof setTimeout>>();

  // Keep latest callbacks in refs so listeners don't need re-binding.
  const onIdleRef = useRef(onIdle);
  const onWarningRef = useRef(onWarning);
  const onActivityRef = useRef(onActivity);
  onIdleRef.current = onIdle;
  onWarningRef.current = onWarning;
  onActivityRef.current = onActivity;

  useEffect(() => {
    if (!enabled) return;

    const clearTimers = () => {
      if (idleTimer.current) clearTimeout(idleTimer.current);
      if (warningTimer.current) clearTimeout(warningTimer.current);
    };

    const start = () => {
      clearTimers();
      const warnDelay = Math.max(timeoutMs - warningMs, 0);
      warningTimer.current = setTimeout(() => onWarningRef.current(), warnDelay);
      idleTimer.current = setTimeout(() => onIdleRef.current(), timeoutMs);
    };

    const handleActivity = () => {
      onActivityRef.current?.();
      start();
    };

    for (const event of ACTIVITY_EVENTS) {
      window.addEventListener(event, handleActivity, { passive: true });
    }
    start();

    return () => {
      clearTimers();
      for (const event of ACTIVITY_EVENTS) {
        window.removeEventListener(event, handleActivity);
      }
    };
  }, [enabled, timeoutMs, warningMs]);
}
