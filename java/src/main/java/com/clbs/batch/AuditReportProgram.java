package com.clbs.batch;

import com.clbs.common.ProgramResult;
import com.clbs.db2.ErrorLogEntity;
import com.clbs.db2.ErrorLogRepository;
import com.clbs.domain.AuditRecord;
import com.clbs.store.DatasetCatalog;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * RPTAUD00 — system audit report.
 *
 * <p>The COBOL source defines the headers, the AUDITLOG/ERRLOG detail layouts and the paragraph
 * skeleton; the leaf paragraphs (2110/2120/2210/2220/2310/2320/2330) are PERFORMed but never
 * defined. Detail lines are written from the defined WS-AUDIT-DETAIL / WS-ERROR-DETAIL layouts;
 * the undefined summary paragraphs contribute no lines.
 */
@Service
public class AuditReportProgram {

    public static final String PROGRAM = "RPTAUD00";
    public static final int WIDTH = 132;

    private final DatasetCatalog datasets;
    private final ErrorLogRepository errorLog;

    public AuditReportProgram(DatasetCatalog datasets, ErrorLogRepository errorLog) {
        this.datasets = datasets;
        this.errorLog = errorLog;
    }

    /** 0000-MAIN. */
    public ProgramResult run() {
        ProgramResult result = new ProgramResult(PROGRAM);
        writeHeaders(result);

        long audits = 0;
        for (AuditRecord audit : datasets.auditFile().all()) {
            audits++;
            result.display(auditDetail(audit));
        }

        long errors = 0;
        for (ErrorLogEntity error : errorLog.findAll()) {
            errors++;
            result.display(errorDetail(error));
        }

        result.count("auditRecords", audits).count("errorRecords", errors);
        result.setReturnCode(BatchConstants.RC_SUCCESS);
        return result;
    }

    /** 1200-WRITE-HEADERS. */
    private void writeHeaders(ProgramResult result) {
        result.display(ReportLines.rule('*', WIDTH));
        result.display(ReportLines.centred("SYSTEM AUDIT REPORT", WIDTH));
        result.display(ReportLines.pad("REPORT DATE:   "
                + LocalDate.now().format(DateTimeFormatter.ISO_DATE), WIDTH));
    }

    /** WS-AUDIT-DETAIL. */
    String auditDetail(AuditRecord audit) {
        return ReportLines.pad(ReportLines.pad(audit.getTimestamp(), 26) + "  "
                + ReportLines.pad(audit.getProgramId(), 8) + "  "
                + ReportLines.pad(audit.getType(), 10) + "  "
                + ReportLines.pad(audit.getMessage(), 80), WIDTH);
    }

    /** WS-ERROR-DETAIL. */
    String errorDetail(ErrorLogEntity error) {
        return ReportLines.pad(ReportLines.pad(error.getTimestamp(), 26) + "  "
                + ReportLines.pad(error.getProgramId(), 8) + "  "
                + ReportLines.pad(error.getSeverity(), 4) + "  "
                + ReportLines.pad(error.getMessage(), 80), WIDTH);
    }
}
