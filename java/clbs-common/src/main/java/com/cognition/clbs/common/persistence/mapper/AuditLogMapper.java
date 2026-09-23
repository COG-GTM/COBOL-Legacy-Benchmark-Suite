package com.cognition.clbs.common.persistence.mapper;

import com.cognition.clbs.common.cobol.CobolDates;
import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.persistence.entity.AuditLog;

/** {@code AUDITLOG.cpy} record to/from the {@code AUDIT_LOG} table. */
public final class AuditLogMapper {

  private AuditLogMapper() {}

  /** Maps a sequential-file record to a new (unsaved) entity. */
  public static AuditLog toEntity(AuditRecord record) {
    return new AuditLog(
        CobolDates.parseTimestamp(record.timestamp()),
        record.systemId(),
        record.userId(),
        record.program(),
        record.terminal(),
        record.type(),
        record.action(),
        record.status(),
        record.portfolioId(),
        record.accountNumber(),
        record.beforeImage(),
        record.afterImage(),
        record.message());
  }

  /** Maps an entity back to the fixed-width record. */
  public static AuditRecord toRecord(AuditLog entity) {
    return new AuditRecord(
        CobolDates.formatTimestamp(entity.getAuditTimestamp()),
        entity.getSystemId(),
        entity.getUserId(),
        entity.getProgram(),
        entity.getTerminal(),
        entity.getType(),
        entity.getAction(),
        entity.getStatus(),
        entity.getPortfolioId(),
        entity.getAccountNumber(),
        entity.getBeforeImage(),
        entity.getAfterImage(),
        entity.getMessage());
  }
}
