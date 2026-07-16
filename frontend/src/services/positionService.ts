import type { Position, PositionQuery } from '../types/position';

/**
 * Data-access boundary for portfolio positions — the modern replacement for the
 * legacy INQPORT online inquiry program (POSMAP screen).
 *
 * The UI depends only on this interface; {@link MockPositionService} implements
 * it against in-memory fixtures today, and a REST implementation backed by the
 * COBOL INQPORT program / POSFILE VSAM file can be dropped in later without
 * touching the components.
 */
export interface PositionService {
  /**
   * Returns the positions held under the given account number.
   *
   * POSREC is keyed by portfolio id, so the account is first resolved to its
   * portfolio(s) and the matching positions are returned. Results are ordered
   * by portfolio id then investment id (stable, mirroring a VSAM browse), and
   * optionally filtered by POS-STATUS.
   */
  listByAccount(
    accountNo: string,
    query?: PositionQuery,
  ): Promise<Position[]>;
}
