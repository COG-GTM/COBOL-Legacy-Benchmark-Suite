import type { HistoryQuery, HistoryRecord } from '../types/history';

/**
 * Data-access boundary for the transaction history inquiry — the modern
 * replacement for the legacy INQHIST online program (HISMAP screen).
 *
 * The UI depends only on this interface; {@link MockHistoryService} implements
 * it against in-memory fixtures today, and a REST implementation backed by
 * INQHIST / the POSHIST table can be dropped in later without touching the
 * components:
 *
 *   listByAccount -> GET /api/accounts/:accountNo/history
 *   get           -> GET /api/history/:recordKey
 */
export interface HistoryService {
  /**
   * Returns the history rows recorded against the given account number.
   *
   * HISTREC is keyed by portfolio id, so the account is first resolved to its
   * portfolio(s) and the matching history rows are returned. Results are
   * ordered newest first (HIST-DATE, HIST-TIME, HIST-SEQ-NO descending),
   * mirroring the `ORDER BY TRANS_DATE DESC` cursor in INQHIST.
   */
  listByAccount(
    accountNo: string,
    query?: HistoryQuery,
  ): Promise<HistoryRecord[]>;

  /** Returns a single row by its 26-character HIST-KEY, for the detail view. */
  get(recordKey: string): Promise<HistoryRecord | undefined>;
}
