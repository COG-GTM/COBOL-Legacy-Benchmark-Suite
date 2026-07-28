package com.clbs.portfolio.service;

import com.clbs.portfolio.model.TransactionRecord;

/**
 * Translation of the sequential {@code TRANSACTION-FILE} that {@code PORTTRAN} declares as
 *
 * <pre>
 * SELECT TRANSACTION-FILE
 *     ASSIGN TO TRANFILE
 *     ORGANIZATION IS SEQUENTIAL
 *     ACCESS MODE IS SEQUENTIAL
 *     FILE STATUS IS WS-TRAN-STATUS.
 * </pre>
 *
 * <p>{@code 0000-MAIN} only ever enters its processing loop when the open leaves {@code '00'} in
 * {@code WS-TRAN-STATUS}, and {@code 2000-PROCESS-TRANSACTIONS} drives the loop off the
 * {@code AT END} phrase of a single {@code READ}, so the contract is deliberately narrow: open,
 * read until the end, close.
 */
public interface TransactionSource {

    /** The status of a completed operation. */
    String STATUS_SUCCESS = "00";

    /**
     * {@code OPEN INPUT TRANSACTION-FILE}, performed by {@code 1000-INITIALIZE}.
     *
     * @return the resulting file status, {@link #STATUS_SUCCESS} when the file opened
     */
    String open();

    /**
     * {@code READ TRANSACTION-FILE}.
     *
     * @return the next record, or {@code null} for the {@code AT END} phrase
     */
    TransactionRecord read();

    /** {@code CLOSE TRANSACTION-FILE}, performed by {@code 3000-TERMINATE}. */
    void close();
}
