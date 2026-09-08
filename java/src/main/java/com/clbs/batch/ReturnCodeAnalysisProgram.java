package com.clbs.batch;

import com.clbs.common.ProgramResult;
import com.clbs.db2.ReturnCodeLogRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * RTNANA00 — return code analysis report. The PRGCUR cursor over RTNCODES becomes the
 * {@code analyzeByProgram()} grouping query; the report layout follows WS-DETAIL-HDR /
 * WS-DETAIL-LINE with the same TOTALS trailer.
 */
@Service
public class ReturnCodeAnalysisProgram {

    public static final String PROGRAM = "RTNANA00";
    public static final int WIDTH = 133;

    private final ReturnCodeLogRepository repository;

    public ReturnCodeAnalysisProgram(ReturnCodeLogRepository repository) {
        this.repository = repository;
    }

    /** PROCEDURE DIVISION. */
    public ProgramResult run() {
        ProgramResult result = new ProgramResult(PROGRAM);
        LocalDateTime now = LocalDateTime.now();

        // P210-WRITE-HEADERS
        result.display(ReportLines.rule('-', WIDTH));
        result.display(ReportLines.centred("Return Code Analysis Report", WIDTH));
        result.display(ReportLines.pad("Report Date:   " + now.toLocalDate()
                + "     Report Time:   " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                WIDTH));
        result.display(ReportLines.rule('-', WIDTH));
        result.display(detail("Program", "Total", "Success", "Warning", "Error", "Severe"));
        result.display(ReportLines.rule('-', WIDTH));

        long total = 0;
        long success = 0;
        long warning = 0;
        long error = 0;
        long severe = 0;

        // P220-PROCESS-DETAIL
        List<Object[]> rows = repository.analyzeByProgram();
        for (Object[] row : rows) {
            long rowTotal = number(row[1]);
            long rowSuccess = number(row[2]);
            long rowWarning = number(row[3]);
            long rowError = number(row[4]);
            long rowSevere = number(row[5]);
            result.display(detail(String.valueOf(row[0]), format(rowTotal), format(rowSuccess),
                    format(rowWarning), format(rowError), format(rowSevere)));
            total += rowTotal;
            success += rowSuccess;
            warning += rowWarning;
            error += rowError;
            severe += rowSevere;
        }

        // P300-GENERATE-REPORT
        result.display(ReportLines.rule('-', WIDTH));
        result.display(detail("TOTALS", format(total), format(success), format(warning),
                format(error), format(severe)));
        result.display(ReportLines.rule('-', WIDTH));

        result.count("programs", rows.size()).count("total", total).count("success", success)
                .count("warning", warning).count("error", error).count("severe", severe);
        result.setReturnCode(BatchConstants.RC_SUCCESS);
        return result;
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /** PIC ZZZ,ZZ9. */
    static String format(long value) {
        return String.format("%,7d", value);
    }

    /** WS-DETAIL-LINE / WS-DETAIL-HDR. */
    private static String detail(String program, String total, String success, String warning,
            String error, String severe) {
        return ReportLines.pad(ReportLines.pad(program, 8) + "  "
                + ReportLines.pad(total, 10) + "  "
                + ReportLines.pad(success, 10) + "  "
                + ReportLines.pad(warning, 10) + "  "
                + ReportLines.pad(error, 10) + "  "
                + ReportLines.pad(severe, 10), WIDTH);
    }
}
