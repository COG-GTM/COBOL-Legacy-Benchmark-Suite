package com.cognition.clbs.common.persistence.mapper;

import com.cognition.clbs.common.cobol.CobolDates;
import com.cognition.clbs.common.copybook.common.TransactionRecord;
import com.cognition.clbs.common.persistence.entity.PortfolioTransaction;
import com.cognition.clbs.common.persistence.entity.PortfolioTransactionKey;

/** {@code TRNREC.cpy} record to/from the {@code PORTFOLIO_TRANSACTION} table. */
public final class PortfolioTransactionMapper {

  private PortfolioTransactionMapper() {}

  /** Maps a VSAM record to an entity. */
  public static PortfolioTransaction toEntity(TransactionRecord record) {
    return new PortfolioTransaction(
        new PortfolioTransactionKey(
            CobolDates.parseDate(record.transactionDate()),
            CobolDates.parseTime(record.transactionTime()),
            record.portfolioId(),
            record.sequenceNumber()),
        record.investmentId(),
        record.type(),
        record.quantity(),
        record.price(),
        record.amount(),
        record.currency(),
        record.status(),
        CobolDates.parseTimestamp(record.processTimestamp()),
        record.processUser());
  }

  /** Maps an entity back to the fixed-width record. */
  public static TransactionRecord toRecord(PortfolioTransaction entity) {
    PortfolioTransactionKey key = entity.getId();
    return new TransactionRecord(
        CobolDates.formatDate(key.transactionDate()),
        CobolDates.formatTime(key.transactionTime()),
        key.portfolioId(),
        key.sequenceNumber(),
        entity.getInvestmentId(),
        entity.getTransactionType(),
        entity.getQuantity(),
        entity.getPrice(),
        entity.getAmount(),
        entity.getCurrencyCode(),
        entity.getStatus(),
        CobolDates.formatTimestamp(entity.getProcessTimestamp()),
        entity.getProcessUser());
  }
}
