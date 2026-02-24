package com.investment.portfolio.reporting;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Daily Position Report Generator (RPTPOS00) - Java equivalent of RPTPOS00.cbl
 *
 * Original COBOL: src/programs/batch/RPTPOS00.cbl
 *
 * Responsibilities:
 * - Generates daily position reports from position master and transaction files
 * - Report sections: headers, position details, transaction summary, exceptions, metrics
 * - Calculates portfolio totals, gain/loss, and exception conditions
 *
 * Files:
 * - POSITION-MASTER      (input):  Current position records
 * - TRANSACTION-HISTORY   (input):  Day's transactions
 * - REPORT-FILE           (output): Formatted position report
 *
 * Report layout:
 *   Page header with date, title, page number
 *   Section 1: Position Details (by portfolio)
 *   Section 2: Transaction Summary
 *   Section 3: Exception Report (negative balances, large movements)
 *   Section 4: Processing Metrics
 */
public class PositionReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(PositionReportGenerator.class.getName());
    private static final String PROGRAM_ID = "RPTPOS00";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter REPORT_DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_WIDTH = 132;
    private static final int LINES_PER_PAGE = 60;

    private final Path positionMasterPath;
    private final Path transactionHistoryPath;
    private final Path reportFilePath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** Report state */
    private int pageNumber;
    private int lineCount;
    private long positionCount;
    private long transactionCount;
    private long exceptionCount;

    /** Accumulators */
    private BigDecimal totalCostBasis;
    private BigDecimal totalMarketValue;
    private BigDecimal totalGainLoss;

    public PositionReportGenerator(Path positionMasterPath, Path transactionHistoryPath,
                                   Path reportFilePath) {
        this.positionMasterPath = positionMasterPath;
        this.transactionHistoryPath = transactionHistoryPath;
        this.reportFilePath = reportFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * PERFORM 2000-PROCESS-POSITIONS
     * PERFORM 3000-PROCESS-TRANSACTIONS
     * PERFORM 4000-PRINT-EXCEPTIONS
     * PERFORM 5000-PRINT-METRICS
     * PERFORM 9000-TERMINATE
     */
    public int execute() {
        LOGGER.info(PROGRAM_ID + " - Position Report generation starting");

        try (FileHandler reportFile = new FileHandler(reportFilePath)) {
            initialize(reportFile);
            processPositions(reportFile);
            processTransactions(reportFile);
            printExceptions(reportFile);
            printMetrics(reportFile);
            terminate(reportFile);
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Open files, print report header.
     */
    private void initialize(FileHandler reportFile) {
        pageNumber = 0;
        lineCount = LINES_PER_PAGE; // Force new page on first write
        positionCount = 0;
        transactionCount = 0;
        exceptionCount = 0;
        totalCostBasis = BigDecimal.ZERO;
        totalMarketValue = BigDecimal.ZERO;
        totalGainLoss = BigDecimal.ZERO;

        reportFile.openOutput();
        printPageHeader(reportFile, "DAILY POSITION REPORT");
    }

    /**
     * 2000-PROCESS-POSITIONS: Read position master and generate detail lines.
     *
     * Maps to:
     *   OPEN INPUT POSITION-MASTER
     *   READ POSITION-MASTER AT END SET WS-EOF TO TRUE
     *   PERFORM UNTIL WS-EOF
     *     PERFORM 2100-PRINT-POSITION-DETAIL
     *     READ POSITION-MASTER AT END SET WS-EOF TO TRUE
     *   END-PERFORM
     */
    private void processPositions(FileHandler reportFile) {
        printSectionHeader(reportFile, "SECTION 1: POSITION DETAILS");
        printLine(reportFile, String.format("%-8s %-10s %-10s %15s %15s %15s %15s %-6s",
                "PortID", "Date", "InvestID", "Quantity", "Cost Basis",
                "Market Value", "Gain/Loss", "Status"));
        printLine(reportFile, "-".repeat(PAGE_WIDTH));

        String currentPortfolio = "";
        BigDecimal portfolioCost = BigDecimal.ZERO;
        BigDecimal portfolioMarket = BigDecimal.ZERO;

        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                printLine(reportFile, "*** NO POSITION DATA AVAILABLE ***");
                return;
            }

            String line;
            while ((line = posFile.readLine()) != null) {
                if (line.length() < 26) continue;

                String portfolioId = line.substring(0, 8).trim();
                String posDate = line.substring(8, 16).trim();
                String investmentId = line.substring(16, 26).trim();

                BigDecimal quantity = parseDecimal(line, 26, 41);
                BigDecimal costBasis = parseDecimal(line, 41, 56);
                BigDecimal marketValue = parseDecimal(line, 56, 71);
                String status = line.length() > 74 ? String.valueOf(line.charAt(74)) : "?";

                BigDecimal gainLoss = marketValue.subtract(costBasis);

                // Portfolio break - print subtotals
                if (!portfolioId.equals(currentPortfolio) && !currentPortfolio.isEmpty()) {
                    printLine(reportFile, String.format("%-8s %-10s %-10s %15s %15s %15s",
                            "", "", "SUBTOTAL:", "", portfolioCost, portfolioMarket));
                    printLine(reportFile, "");
                    portfolioCost = BigDecimal.ZERO;
                    portfolioMarket = BigDecimal.ZERO;
                }
                currentPortfolio = portfolioId;

                // Print detail line
                printLine(reportFile, String.format("%-8s %-10s %-10s %15s %15s %15s %15s %-6s",
                        portfolioId, posDate, investmentId,
                        quantity, costBasis, marketValue, gainLoss, status));

                // Accumulate totals
                totalCostBasis = totalCostBasis.add(costBasis);
                totalMarketValue = totalMarketValue.add(marketValue);
                totalGainLoss = totalGainLoss.add(gainLoss);
                portfolioCost = portfolioCost.add(costBasis);
                portfolioMarket = portfolioMarket.add(marketValue);
                positionCount++;
            }

            // Final portfolio subtotal
            if (!currentPortfolio.isEmpty()) {
                printLine(reportFile, String.format("%-8s %-10s %-10s %15s %15s %15s",
                        "", "", "SUBTOTAL:", "", portfolioCost, portfolioMarket));
            }

        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error processing positions", e);
            returnCode.setCode(ReturnCode.ERROR);
        }

        // Grand totals
        printLine(reportFile, "=".repeat(PAGE_WIDTH));
        printLine(reportFile, String.format("%-29s %15s %15s %15s %15s",
                "GRAND TOTALS:", "", totalCostBasis, totalMarketValue, totalGainLoss));
        printLine(reportFile, "");
    }

    /**
     * 3000-PROCESS-TRANSACTIONS: Summarize day's transactions.
     */
    private void processTransactions(FileHandler reportFile) {
        printSectionHeader(reportFile, "SECTION 2: TRANSACTION SUMMARY");

        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> typeAmounts = new LinkedHashMap<>();
        typeCounts.put("BU", 0); typeCounts.put("SL", 0);
        typeCounts.put("TR", 0); typeCounts.put("FE", 0);
        typeAmounts.put("BU", BigDecimal.ZERO); typeAmounts.put("SL", BigDecimal.ZERO);
        typeAmounts.put("TR", BigDecimal.ZERO); typeAmounts.put("FE", BigDecimal.ZERO);

        try (FileHandler trnFile = new FileHandler(transactionHistoryPath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(trnFile.openInput())) {
                printLine(reportFile, "*** NO TRANSACTION DATA AVAILABLE ***");
                return;
            }

            String line;
            while ((line = trnFile.readLine()) != null) {
                if (line.length() < 40) continue;
                transactionCount++;

                String typeCode = line.substring(38, 40).trim();
                BigDecimal amount = parseDecimal(line, 70, 85);

                typeCounts.merge(typeCode, 1, Integer::sum);
                typeAmounts.merge(typeCode, amount, BigDecimal::add);
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E300", "Error processing transactions", e);
        }

        printLine(reportFile, String.format("%-12s %10s %18s", "Type", "Count", "Total Amount"));
        printLine(reportFile, "-".repeat(42));
        for (String type : typeCounts.keySet()) {
            printLine(reportFile, String.format("%-12s %10d %18s",
                    formatTransType(type), typeCounts.get(type), typeAmounts.get(type)));
        }
        printLine(reportFile, "-".repeat(42));
        printLine(reportFile, String.format("%-12s %10d", "TOTAL", transactionCount));
        printLine(reportFile, "");
    }

    /**
     * 4000-PRINT-EXCEPTIONS: Report exception conditions.
     */
    private void printExceptions(FileHandler reportFile) {
        printSectionHeader(reportFile, "SECTION 3: EXCEPTION REPORT");

        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                printLine(reportFile, "*** UNABLE TO CHECK EXCEPTIONS ***");
                return;
            }

            printLine(reportFile, String.format("%-8s %-10s %-10s %-12s %s",
                    "PortID", "InvestID", "Date", "Condition", "Details"));
            printLine(reportFile, "-".repeat(70));

            String line;
            while ((line = posFile.readLine()) != null) {
                if (line.length() < 41) continue;

                String portfolioId = line.substring(0, 8).trim();
                String investmentId = line.substring(16, 26).trim();
                String posDate = line.substring(8, 16).trim();

                BigDecimal quantity = parseDecimal(line, 26, 41);
                BigDecimal costBasis = parseDecimal(line, 41, 56);
                BigDecimal marketValue = parseDecimal(line, 56, 71);

                // Exception: Negative balance
                if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                    printLine(reportFile, String.format("%-8s %-10s %-10s %-12s Qty=%s",
                            portfolioId, investmentId, posDate, "NEG_BALANCE", quantity));
                    exceptionCount++;
                }

                // Exception: Large unrealized loss (>20%)
                if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lossPct = marketValue.subtract(costBasis)
                            .divide(costBasis, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    if (lossPct.compareTo(new BigDecimal("-20")) < 0) {
                        printLine(reportFile, String.format("%-8s %-10s %-10s %-12s Loss=%s%%",
                                portfolioId, investmentId, posDate, "LARGE_LOSS", lossPct));
                        exceptionCount++;
                    }
                }
            }

            if (exceptionCount == 0) {
                printLine(reportFile, "No exceptions found.");
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E400", "Error checking exceptions", e);
        }
        printLine(reportFile, "");
    }

    /**
     * 5000-PRINT-METRICS: Print processing metrics summary.
     */
    private void printMetrics(FileHandler reportFile) {
        printSectionHeader(reportFile, "SECTION 4: PROCESSING METRICS");

        printLine(reportFile, String.format("  Positions Processed:    %,d", positionCount));
        printLine(reportFile, String.format("  Transactions Processed: %,d", transactionCount));
        printLine(reportFile, String.format("  Exceptions Found:       %,d", exceptionCount));
        printLine(reportFile, String.format("  Total Cost Basis:       %s", totalCostBasis));
        printLine(reportFile, String.format("  Total Market Value:     %s", totalMarketValue));
        printLine(reportFile, String.format("  Total Gain/Loss:        %s", totalGainLoss));
        printLine(reportFile, String.format("  Report Generated:       %s",
                LocalDateTime.now().format(TIMESTAMP_FMT)));
        printLine(reportFile, "");
    }

    /**
     * 9000-TERMINATE: Final page, close files.
     */
    private void terminate(FileHandler reportFile) {
        printLine(reportFile, "=".repeat(PAGE_WIDTH));
        printLine(reportFile, centerText("*** END OF REPORT ***"));
        printLine(reportFile, centerText("Pages: " + pageNumber));

        if (exceptionCount > 0 && returnCode.getCurrentCode() < ReturnCode.WARNING) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        LOGGER.info(PROGRAM_ID + " - Report generated: " + positionCount + " positions, "
                + transactionCount + " transactions, " + exceptionCount + " exceptions");
    }

    // --- Report formatting helpers ---

    private void printPageHeader(FileHandler reportFile, String title) {
        pageNumber++;
        lineCount = 0;
        printLine(reportFile, "=".repeat(PAGE_WIDTH));
        printLine(reportFile, String.format("%-40s %s %40s",
                "DATE: " + LocalDate.now().format(REPORT_DATE_FMT),
                title,
                "PAGE: " + pageNumber));
        printLine(reportFile, String.format("%-40s %s",
                "PROGRAM: " + PROGRAM_ID,
                "INVESTMENT PORTFOLIO MANAGEMENT SYSTEM"));
        printLine(reportFile, "=".repeat(PAGE_WIDTH));
        printLine(reportFile, "");
    }

    private void printSectionHeader(FileHandler reportFile, String title) {
        checkPageBreak(reportFile, 5);
        printLine(reportFile, "");
        printLine(reportFile, title);
        printLine(reportFile, "-".repeat(title.length()));
        printLine(reportFile, "");
    }

    private void printLine(FileHandler reportFile, String line) {
        reportFile.writeLine(line);
        lineCount++;
    }

    private void checkPageBreak(FileHandler reportFile, int linesNeeded) {
        if (lineCount + linesNeeded > LINES_PER_PAGE) {
            printLine(reportFile, "");
            printPageHeader(reportFile, "DAILY POSITION REPORT (CONT.)");
        }
    }

    private String centerText(String text) {
        int pad = (PAGE_WIDTH - text.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + text;
    }

    private String formatTransType(String code) {
        switch (code) {
            case "BU": return "BUY";
            case "SL": return "SELL";
            case "TR": return "TRANSFER";
            case "FE": return "FEE";
            default:   return code;
        }
    }

    private BigDecimal parseDecimal(String line, int start, int end) {
        try {
            if (line.length() > end) {
                return new BigDecimal(line.substring(start, end).trim());
            }
        } catch (NumberFormatException e) {
            // Return zero for unparseable values
        }
        return BigDecimal.ZERO;
    }
}
