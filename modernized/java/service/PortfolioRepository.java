package com.clbs.portfolio.service;

import com.clbs.portfolio.model.PortfolioRecord;

import java.util.Optional;

/**
 * Translation of the indexed {@code PORTFOLIO-FILE} that {@code PORTTRAN} declares as
 *
 * <pre>
 * SELECT PORTFOLIO-FILE
 *     ASSIGN TO PORTFILE
 *     ORGANIZATION IS INDEXED
 *     ACCESS MODE IS RANDOM
 *     RECORD KEY IS PORT-ID
 *     FILE STATUS IS WS-PORT-STATUS.
 * </pre>
 *
 * <p>Each method stands for one file operation and, like the COBOL runtime, leaves its outcome in
 * the file status field that {@link #getFileStatus()} reports. That field is load-bearing rather
 * than diagnostic: {@code 2300-UPDATE-AUDIT-TRAIL} decides between {@code SUCC} and {@code FAIL}
 * from the status left by the <em>previous</em> operation, whatever that operation was (G7 in
 * {@code TRANSLATION-NOTES.md}).
 *
 * <p>The {@code INVALID KEY} condition on a {@code READ} is modelled as an empty result; on a
 * {@code REWRITE}, which has no result to be empty, it is modelled as a status in class 2 - see
 * {@link #isInvalidKey(String)}. Implementations are free to be file-backed, database-backed or in
 * memory; {@code PORTTRAN} does no I/O of its own.
 */
public interface PortfolioRepository {

    /** {@code ERR-VSAM-SUCCESS} - the status of a completed operation. */
    String STATUS_SUCCESS = "00";

    /** {@code ERR-VSAM-NOTFND} - the status a random read leaves when the key is absent. */
    String STATUS_NOT_FOUND = "23";

    /**
     * {@code OPEN I-O PORTFOLIO-FILE}, performed by {@code 1000-INITIALIZE}.
     *
     * @return the resulting file status, {@link #STATUS_SUCCESS} when the file opened
     */
    String open();

    /**
     * {@code MOVE ... TO PORT-ID} followed by {@code READ PORTFOLIO-FILE}, the random read in
     * {@code 2110-CHECK-PORTFOLIO}, {@code 2210-PROCESS-BUY}, {@code 2220-PROCESS-SELL} and
     * {@code 2240-PROCESS-FEE}.
     *
     * @param portId the eight-byte record key, space-padded as {@code PORT-ID} holds it
     * @return the record, or empty for the {@code INVALID KEY} path
     */
    Optional<PortfolioRecord> findById(String portId);

    /**
     * {@code REWRITE PORTFOLIO-RECORD}. The caller inspects {@link #getFileStatus()} afterwards to
     * decide whether the {@code INVALID KEY} branch is taken.
     *
     * @param portfolio the record area as the program left it
     */
    void update(PortfolioRecord portfolio);

    /** {@code CLOSE PORTFOLIO-FILE}, performed by {@code 3000-TERMINATE}. */
    void close();

    /**
     * {@code WS-PORT-STATUS} - the file status left by the most recent operation, or spaces before
     * the first one, exactly as the {@code FILE STATUS} field behaves.
     */
    String getFileStatus();

    /**
     * Whether a file status selects the {@code INVALID KEY} branch of a statement that has one.
     * COBOL raises the condition for status class 2 (invalid key: not found, duplicate, boundary
     * violation), so any {@code 2x} status counts and anything else does not.
     */
    static boolean isInvalidKey(String fileStatus) {
        return fileStatus != null && fileStatus.startsWith("2");
    }
}
