package com.clbs.portfolio.service;

import com.clbs.portfolio.model.ErrorMessage;

/**
 * Translation of {@code CALL 'ERRPROC' USING ERR-MESSAGE}, the error subroutine in
 * {@code src/programs/common/ERRPROC.cbl}.
 *
 * <p>{@code ERRPROC} timestamps the error, appends it to the sequential {@code ERRLOG} file, echoes
 * it to the operator console and copies the severity it was given into its return code. It never
 * fails the caller: a log-write failure is displayed, not propagated.
 *
 * <h2>Discrepancies preserved by this signature</h2>
 *
 * <ul>
 *   <li>{@code ERRPROC} declares its parameter as {@code LS-ERROR-REQUEST}, which starts at
 *       {@code LS-PROGRAM-ID}, while callers pass {@code ERR-MESSAGE}, which starts with an 18-byte
 *       {@code ERR-TIMESTAMP}. The layouts are offset by that timestamp on the mainframe; the
 *       translated contract passes the typed record instead.</li>
 *   <li>{@code PORTTRAN} leaves {@code ERR-CODE} and {@code ERR-SEVERITY} unset for every error it
 *       raises, so an implementation receives spaces and zero in those fields. That is faithful:
 *       see {@code TRANSLATION-NOTES.md}.</li>
 * </ul>
 */
public interface ErrorProcessor {

    /**
     * Logs one error and returns the subroutine return code, which {@code ERRPROC} sets to the
     * severity it was given ({@code MOVE LS-SEVERITY TO LS-RETURN-CODE}).
     *
     * @param errorMessage the populated {@code ERR-MESSAGE} area
     * @return the severity carried by the message
     */
    int process(ErrorMessage errorMessage);
}
