package com.cognition.clbs.common.persistence.mapper;

import com.cognition.clbs.common.cobol.CobolDates;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import com.cognition.clbs.common.persistence.entity.ErrorLog;
import com.cognition.clbs.common.persistence.entity.ErrorLogKey;

/** {@code DBTBLS.cpy ERRLOG-RECORD} host variables to/from the {@code ERRLOG} table. */
public final class ErrorLogMapper {

  private ErrorLogMapper() {}

  /** Maps the host-variable record to an entity. */
  public static ErrorLog toEntity(ErrorLogRecord record) {
    return new ErrorLog(
        new ErrorLogKey(CobolDates.parseTimestamp(record.errorTimestamp()), record.programId()),
        record.errorType(),
        record.severity(),
        record.errorCode(),
        record.errorMessage(),
        CobolDates.parseIsoDate(record.processDate()),
        CobolDates.parseDb2Time(record.processTime()),
        record.userId(),
        record.additionalInfo());
  }

  /** Maps an entity back to the host-variable record. */
  public static ErrorLogRecord toRecord(ErrorLog entity) {
    return new ErrorLogRecord(
        CobolDates.formatTimestamp(entity.getId().errorTimestamp()),
        entity.getId().programId(),
        entity.getErrorType(),
        entity.getSeverity(),
        entity.getErrorCode(),
        entity.getErrorMessage(),
        CobolDates.formatIsoDate(entity.getProcessDate()),
        CobolDates.formatDb2Time(entity.getProcessTime()),
        entity.getUserId(),
        entity.getAdditionalInfo());
  }
}
