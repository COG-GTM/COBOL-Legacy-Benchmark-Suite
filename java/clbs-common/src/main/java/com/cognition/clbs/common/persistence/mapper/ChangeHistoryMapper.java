package com.cognition.clbs.common.persistence.mapper;

import com.cognition.clbs.common.cobol.CobolDates;
import com.cognition.clbs.common.copybook.common.HistoryRecord;
import com.cognition.clbs.common.persistence.entity.ChangeHistory;
import com.cognition.clbs.common.persistence.entity.ChangeHistoryKey;

/** {@code HISTREC.cpy} record to/from the {@code CHANGE_HISTORY} table. */
public final class ChangeHistoryMapper {

  private ChangeHistoryMapper() {}

  /** Maps a VSAM record to an entity. */
  public static ChangeHistory toEntity(HistoryRecord record) {
    return new ChangeHistory(
        new ChangeHistoryKey(
            record.portfolioId(),
            CobolDates.parseDate(record.historyDate()),
            CobolDates.parseTime(record.historyTime()),
            record.sequenceNumber()),
        record.recordType(),
        record.actionCode(),
        record.beforeImage(),
        record.afterImage(),
        record.reasonCode(),
        CobolDates.parseTimestamp(record.processTimestamp()),
        record.processUser());
  }

  /** Maps an entity back to the fixed-width record. */
  public static HistoryRecord toRecord(ChangeHistory entity) {
    ChangeHistoryKey key = entity.getId();
    return new HistoryRecord(
        key.portfolioId(),
        CobolDates.formatDate(key.historyDate()),
        CobolDates.formatTime(key.historyTime()),
        key.sequenceNumber(),
        entity.getRecordType(),
        entity.getActionCode(),
        entity.getBeforeImage(),
        entity.getAfterImage(),
        entity.getReasonCode(),
        CobolDates.formatTimestamp(entity.getProcessTimestamp()),
        entity.getProcessUser());
  }
}
