package com.portfolio.batch.report;

import com.portfolio.domain.AuditLog;
import com.portfolio.repository.AuditLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditReportService {
  private final AuditLogRepository auditLogRepository;

  public AuditReportService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  public ReportResult generate() {
    return generate(LocalDate.now(), LocalDate.now());
  }

  public ReportResult generate(LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);
    List<String> lines = new ArrayList<>();
    lines.add("SYSTEM AUDIT REPORT|" + startDate + "|" + endDate);
    for (AuditLog auditLog : auditLogRepository.findByAudTimestampBetween(start, end)) {
      lines.add(
          String.join(
              "|",
              String.valueOf(auditLog.getAudTimestamp()),
              value(auditLog.getProgram()),
              value(auditLog.getAudType()),
              value(auditLog.getMessage())));
    }
    return new ReportResult("RPTAUD00", lines, 0);
  }

  private String value(Object value) {
    return value == null ? "" : value.toString();
  }
}
