package com.cognition.clbs.common.persistence.mapper;

import com.cognition.clbs.common.cobol.CobolDates;
import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.common.persistence.entity.PositionMaster;
import com.cognition.clbs.common.persistence.entity.PositionMasterKey;

/** {@code POSREC.cpy} record to/from the {@code POSITION_MASTER} table. */
public final class PositionMasterMapper {

  private PositionMasterMapper() {}

  /** Maps a VSAM record to an entity. */
  public static PositionMaster toEntity(PositionRecord record) {
    return new PositionMaster(
        new PositionMasterKey(
            record.portfolioId(),
            CobolDates.parseDate(record.positionDate()),
            record.investmentId()),
        record.quantity(),
        record.costBasis(),
        record.marketValue(),
        record.currency(),
        record.status(),
        CobolDates.parseTimestamp(record.lastMaintTimestamp()),
        record.lastMaintUser());
  }

  /** Maps an entity back to the fixed-width record. */
  public static PositionRecord toRecord(PositionMaster entity) {
    return new PositionRecord(
        entity.getId().portfolioId(),
        CobolDates.formatDate(entity.getId().positionDate()),
        entity.getId().investmentId(),
        entity.getQuantity(),
        entity.getCostBasis(),
        entity.getMarketValue(),
        entity.getCurrencyCode(),
        entity.getStatus(),
        CobolDates.formatTimestamp(entity.getLastMaintTimestamp()),
        entity.getLastMaintUser());
  }
}
