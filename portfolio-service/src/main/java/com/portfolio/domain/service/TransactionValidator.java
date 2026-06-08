package com.portfolio.domain.service;

import com.portfolio.domain.command.TransactionCommand;
import org.springframework.stereotype.Service;

/**
 * Stub — to be implemented by Child Session 2.
 * Ports PORTTRAN.cbl paragraphs 2110-CHECK-PORTFOLIO, 2120-CHECK-TRANSACTION-TYPE,
 * 2130-CHECK-AMOUNTS and PORTVALD.cbl validation logic.
 */
@Service
public class TransactionValidator {

    public void validate(TransactionCommand command) {
        // TODO: Child Session 2
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
