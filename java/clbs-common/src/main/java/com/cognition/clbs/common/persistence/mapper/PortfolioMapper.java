package com.cognition.clbs.common.persistence.mapper;

import com.cognition.clbs.common.cobol.CobolDates;
import com.cognition.clbs.common.copybook.common.PortfolioRecord;
import com.cognition.clbs.common.persistence.entity.Portfolio;
import com.cognition.clbs.common.persistence.entity.PortfolioKey;

/** {@code PORTFLIO.cpy} record to/from the {@code PORTFOLIO} table. */
public final class PortfolioMapper {

  private PortfolioMapper() {}

  /** Maps a VSAM record to an entity; zero DISPLAY dates become {@code null}. */
  public static Portfolio toEntity(PortfolioRecord record) {
    return new Portfolio(
        new PortfolioKey(record.portfolioId(), record.accountNumber()),
        record.clientName(),
        record.clientType(),
        CobolDates.parseDate(record.createDate()),
        CobolDates.parseDate(record.lastMaintDate()),
        record.status(),
        record.totalValue(),
        record.cashBalance(),
        record.lastUser(),
        CobolDates.parseDate(record.lastTransactionDate()));
  }

  /** Maps an entity back to the fixed-width record; {@code null} dates become zero. */
  public static PortfolioRecord toRecord(Portfolio entity) {
    return new PortfolioRecord(
        entity.getId().portfolioId(),
        entity.getId().accountNumber(),
        entity.getClientName(),
        entity.getClientType(),
        CobolDates.formatDateNumeric(entity.getCreateDate()),
        CobolDates.formatDateNumeric(entity.getLastMaintDate()),
        entity.getStatus(),
        entity.getTotalValue(),
        entity.getCashBalance(),
        entity.getLastUser(),
        CobolDates.formatDateNumeric(entity.getLastTransactionDate()));
  }
}
