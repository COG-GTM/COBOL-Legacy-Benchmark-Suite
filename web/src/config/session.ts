/**
 * Session configuration.
 *
 * The legacy CICS environment terminates idle terminals after a configurable
 * inactivity interval. We expose the same notion here. Values can be overridden
 * at build time via Vite env vars so the timeout is configurable per
 * environment without code changes.
 */

const DEFAULT_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
const DEFAULT_WARNING_MS = 60 * 1000; // warn 1 minute before timeout

function readNumberEnv(value: string | undefined, fallback: number): number {
  if (value === undefined || value.trim() === '') return fallback;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export interface SessionConfig {
  /** Inactivity period (ms) after which the session is terminated. */
  timeoutMs: number;
  /** How long before timeout (ms) the user is warned. */
  warningMs: number;
}

export const sessionConfig: SessionConfig = {
  timeoutMs: readNumberEnv(
    import.meta.env.VITE_SESSION_TIMEOUT_MS,
    DEFAULT_TIMEOUT_MS,
  ),
  warningMs: readNumberEnv(
    import.meta.env.VITE_SESSION_WARNING_MS,
    DEFAULT_WARNING_MS,
  ),
};

/** System id recorded on audit entries (mirrors AUD-SYSTEM-ID PIC X(8)). */
export const SYSTEM_ID = 'CLBSWEB';
