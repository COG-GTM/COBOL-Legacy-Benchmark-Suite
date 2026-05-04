package com.coggtm.portfolio.service;

import com.coggtm.portfolio.domain.TransactionRecord;

/**
 * Transaction processing — maps to PORTTRAN.cbl 2200-UPDATE-POSITIONS.
 *
 * <p>COBOL source: {@code src/programs/portfolio/PORTTRAN.cbl}</p>
 */
public interface TransactionService {

    TransactionRecord processBuy(TransactionRecord transaction);

    TransactionRecord processSell(TransactionRecord transaction);

    TransactionRecord processTransfer(TransactionRecord transaction);

    TransactionRecord processFee(TransactionRecord transaction);
}
