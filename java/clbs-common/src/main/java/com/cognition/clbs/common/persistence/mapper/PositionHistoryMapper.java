package com.cognition.clbs.common.persistence.mapper;

import com.cognition.clbs.common.cobol.CobolDates;
import com.cognition.clbs.common.copybook.db2.PositionHistoryRecord;
import com.cognition.clbs.common.persistence.entity.PositionHistory;
import com.cognition.clbs.common.persistence.entity.PositionHistoryKey;

/** {@code DBTBLS.cpy POSHIST-RECORD} host variables to/from the {@code POSHIST} table. */
public final class PositionHistoryMapper {

  private PositionHistoryMapper() {}

  /** Maps the host-variable record to an entity. */
  public static PositionHistory toEntity(PositionHistoryRecord record) {
    return new PositionHistory(
        new PositionHistoryKey(
            record.accountNumber(),
            record.portfolioId(),
            CobolDates.parseIsoDate(record.transactionDate()),
            CobolDates.parseDb2Time(record.transactionTime())),
        record.transactionType(),
        record.securityId(),
        record.quantity(),
        record.price(),
        record.amount(),
        record.fees(),
        record.totalAmount(),
        record.costBasis(),
        record.gainLoss(),
        CobolDates.parseIsoDate(record.processDate()),
        CobolDates.parseDb2Time(record.processTime()),
        record.programId(),
        record.userId(),
        CobolDates.parseTimestamp(record.auditTimestamp()));
  }

  /** Maps an entity back to the host-variable record. */
  public static PositionHistoryRecord toRecord(PositionHistory entity) {
    PositionHistoryKey key = entity.getId();
    return new PositionHistoryRecord(
        key.accountNumber(),
        key.portfolioId(),
        CobolDates.formatIsoDate(key.transactionDate()),
        CobolDates.formatDb2Time(key.transactionTime()),
        entity.getTransactionType(),
        entity.getSecurityId(),
        entity.getQuantity(),
        entity.getPrice(),
        entity.getAmount(),
        entity.getFees(),
        entity.getTotalAmount(),
        entity.getCostBasis(),
        entity.getGainLoss(),
        CobolDates.formatIsoDate(entity.getProcessDate()),
        CobolDates.formatDb2Time(entity.getProcessTime()),
        entity.getProgramId(),
        entity.getUserId(),
        CobolDates.formatTimestamp(entity.getAuditTimestamp()));
  }
}
