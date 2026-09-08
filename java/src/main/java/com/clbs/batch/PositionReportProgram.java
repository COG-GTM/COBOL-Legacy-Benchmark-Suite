package com.clbs.batch;

import com.clbs.common.ProgramResult;
import com.clbs.domain.PositionRecord;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * RPTPOS00 — daily position report.
 *
 * <p>1200-WRITE-HEADERS, 2100-READ-POSITIONS and 2110-FORMAT-POSITION are migrated as written.
 * 2210-READ-TRANSACTIONS, 2220-SUMMARIZE-ACTIVITY, 2310-WRITE-TOTALS, 2320-WRITE-EXCEPTIONS and
 * 2330-WRITE-METRICS are PERFORMed by the COBOL source but never defined in it, so the
 * corresponding steps here stay empty rather than inventing report content.
 */
@Service
public class PositionReportProgram {

    public static final String PROGRAM = "RPTPOS00";
    public static final int WIDTH = 132;

    private final DatasetCatalog datasets;

    public PositionReportProgram(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    /** 0000-MAIN. */
    public ProgramResult run() {
        ProgramResult result = new ProgramResult(PROGRAM);
        writeHeaders(result);

        long positions = 0;
        for (PositionRecord position : datasets.positionFile().all()) {
            positions++;
            result.display(formatPosition(position));
        }

        result.count("positions", positions);
        result.setReturnCode(BatchConstants.RC_SUCCESS);
        return result;
    }

    /** 1200-WRITE-HEADERS. */
    private void writeHeaders(ProgramResult result) {
        result.display(ReportLines.rule('*', WIDTH));
        result.display(ReportLines.centred("DAILY POSITION REPORT", WIDTH));
        result.display(ReportLines.pad("REPORT DATE:   "
                + LocalDate.now().format(DateTimeFormatter.ISO_DATE), WIDTH));
    }

    /** 2110-FORMAT-POSITION. */
    String formatPosition(PositionRecord position) {
        return ReportLines.pad(ReportLines.pad(position.getPortfolioId(), 10) + "  "
                + ReportLines.pad(position.getDescription(), 30) + "  "
                + String.format("%14.2f", position.getQuantity()) + "  "
                + String.format("%16.2f", position.getMarketValue()) + "  "
                + String.format("%7.2f", changePercent(position)), WIDTH);
    }

    /** COMPUTE WS-POS-CHANGE-PCT: guarded because POS-PREVIOUS-VALUE may be zero. */
    static BigDecimal changePercent(PositionRecord position) {
        BigDecimal previous = position.getPreviousValue();
        if (previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return position.getMarketValue().subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
