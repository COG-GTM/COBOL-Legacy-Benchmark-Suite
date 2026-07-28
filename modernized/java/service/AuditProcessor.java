package com.clbs.portfolio.service;

import com.clbs.portfolio.model.AuditRecord;

/**
 * Translation of {@code CALL 'AUDPROC' USING AUDIT-RECORD}, the audit subroutine in
 * {@code src/programs/common/AUDPROC.cbl}.
 *
 * <p>{@code AUDPROC} appends the record it is handed to the sequential {@code AUDFILE} and reports
 * the outcome in the last field of its linkage area: {@code 0} on a successful write and {@code 8}
 * when the file cannot be opened or written. This interface returns that value so callers can
 * reproduce the caller-side check without a shared linkage buffer.
 *
 * <h2>Discrepancies preserved by this signature</h2>
 *
 * <ul>
 *   <li>{@code AUDPROC} declares its parameter as {@code LS-AUDIT-REQUEST}, which starts at
 *       {@code LS-SYSTEM-INFO} and carries a trailing {@code LS-RETURN-CODE}. {@code AUDIT-RECORD}
 *       starts with a 26-byte {@code AUD-TIMESTAMP} and has no return-code field, so the two layouts
 *       do not line up byte for byte on the mainframe. The translated contract passes the record as
 *       a typed object and returns the status instead of overlaying storage.</li>
 *   <li>{@code PORTTRAN} tests the {@code RETURN-CODE} special register after the call, which
 *       {@code AUDPROC} never sets - it only sets its linkage field. Callers of this interface are
 *       expected to reproduce the intent (checking the value the subroutine reports) rather than the
 *       defect; see {@code TRANSLATION-NOTES.md}.</li>
 * </ul>
 */
public interface AuditProcessor {

    /** The value {@code AUDPROC} reports after a successful write. */
    int RETURN_SUCCESS = 0;

    /** The value {@code AUDPROC} reports when the audit file cannot be opened or written. */
    int RETURN_ERROR = 8;

    /**
     * Writes one audit record and returns the subroutine return code.
     *
     * @param auditRecord the fully built {@code AUDIT-RECORD}
     * @return {@link #RETURN_SUCCESS} or {@link #RETURN_ERROR}
     */
    int process(AuditRecord auditRecord);
}
