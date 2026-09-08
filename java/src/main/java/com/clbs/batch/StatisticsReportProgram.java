package com.clbs.batch;

import com.clbs.common.ProgramResult;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * RPTSTA00 — system statistics and performance report.
 *
 * <p>Headers and the WS-DB2-DETAIL / WS-BATCH-DETAIL layouts are migrated as written.
 * 2110-ACCUMULATE-DB2-STATS, 2210-ACCUMULATE-BATCH-STATS, 2310/2320-CALC-* and 2410/2420/2430-WRITE-*
 * are PERFORMed but never defined in the COBOL source. Batch counts are therefore derived from the
 * BCHCTL dataset the FD copies (adapted, not source-defined); the DB2 statistics line stays at zero
 * because no DB2STATS dataset exists in this migration.
 */
@Service
public class StatisticsReportProgram {

    public static final String PROGRAM = "RPTSTA00";
    public static final int WIDTH = 132;

    private final DatasetCatalog datasets;

    public StatisticsReportProgram(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    /** 0000-MAIN. */
    public ProgramResult run() {
        ProgramResult result = new ProgramResult(PROGRAM);
        result.display(ReportLines.rule('*', WIDTH));
        result.display(ReportLines.centred("SYSTEM STATISTICS AND PERFORMANCE REPORT", WIDTH));
        result.display(ReportLines.pad("REPORT DATE:   "
                + LocalDate.now().format(DateTimeFormatter.ISO_DATE), WIDTH));

        long jobs = datasets.batchControlFile().all().size();
        long success = datasets.batchControlFile().all().stream()
                .filter(record -> record.getStatus() == BatchControlRecord.DONE
                        && record.getReturnCode() <= BatchConstants.RC_WARNING)
                .count();
        long failed = datasets.batchControlFile().all().stream()
                .filter(record -> record.getStatus() == BatchControlRecord.ERROR)
                .count();

        // WS-DB2-DETAIL
        result.display(ReportLines.pad(ReportLines.pad("DB2 CALLS:", 20)
                + String.format("%11d", 0L)
                + " ".repeat(20) + ReportLines.pad("AVG RESPONSE:", 20)
                + String.format("%10.3f", BigDecimal.ZERO), WIDTH));

        // WS-BATCH-DETAIL
        result.display(ReportLines.pad(ReportLines.pad("BATCH JOBS:", 20)
                + String.format("%9d", jobs)
                + " ".repeat(10) + ReportLines.pad("SUCCESS RATE:", 20)
                + String.format("%6.2f", successRate(success, jobs)) + "%", WIDTH));

        result.count("jobs", jobs).count("success", success).count("failed", failed);
        result.setReturnCode(BatchConstants.RC_SUCCESS);
        return result;
    }

    static BigDecimal successRate(long success, long jobs) {
        if (jobs == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(success)
                .divide(BigDecimal.valueOf(jobs), 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
