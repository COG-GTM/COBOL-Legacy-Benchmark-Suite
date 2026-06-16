package com.clbs.portfolio.service;

import com.clbs.portfolio.domain.PortfolioPosition;

/**
 * Outcome of processing a single transaction.
 *
 * <p>In PORTTRAN success/failure is signalled by whether {@code ERR-TEXT} is left
 * {@code SPACES}. Here, {@code errorText == null} means success; a non-null value
 * is the exact COBOL error message that would have been moved into {@code ERR-TEXT}.</p>
 *
 * @param success     whether the transaction was processed without error
 * @param errorText   the COBOL {@code ERR-TEXT} message, or {@code null} on success
 * @param position    the resulting portfolio position after update, or {@code null}
 *                    if no update was applied
 */
public record TransactionResult(boolean success, String errorText, PortfolioPosition position) {

    public static TransactionResult ok(PortfolioPosition position) {
        return new TransactionResult(true, null, position);
    }

    public static TransactionResult failure(String errorText) {
        return new TransactionResult(false, errorText, null);
    }
}
