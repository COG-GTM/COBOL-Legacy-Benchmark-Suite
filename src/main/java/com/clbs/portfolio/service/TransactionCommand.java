package com.clbs.portfolio.service;

import java.math.BigDecimal;

/** Input fields of a TRNREC transaction. */
public record TransactionCommand(String portfolioId, String investmentId, String type, BigDecimal quantity,
                                 BigDecimal price, BigDecimal amount, String currency) {
}
