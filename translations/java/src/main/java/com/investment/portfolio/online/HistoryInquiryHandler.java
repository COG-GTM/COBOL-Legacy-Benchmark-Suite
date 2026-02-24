package com.investment.portfolio.online;

import com.investment.portfolio.common.DatabaseManager;
import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.online.InquiryOnlineHandler.InquiryResponse;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Transaction History Inquiry Handler (INQHIST) - Java equivalent of INQHIST.cbl
 *
 * Original COBOL: src/programs/online/INQHIST.cbl
 *
 * Responsibilities:
 * - Retrieves transaction history from DB2 POSHIST table
 * - Uses cursor-based retrieval (DECLARE, OPEN, FETCH, CLOSE)
 * - Manages DB2 connection lifecycle for online access
 * - Formats history data for display with pagination
 *
 * DB2 operations:
 *   EXEC SQL DECLARE HIST-CURSOR CURSOR FOR
 *     SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT
 *     FROM POSHIST
 *     WHERE ACCOUNT_NO = :WS-ACCOUNT-NO
 *     ORDER BY TRANS_DATE DESC
 *   END-EXEC
 *
 * Online DB2 connection:
 *   EXEC CICS LINK PROGRAM('DB2ONLN') ... — connect
 *   EXEC CICS LINK PROGRAM('DB2RECV') ... — recovery
 */
public class HistoryInquiryHandler {

    private static final Logger LOGGER = Logger.getLogger(HistoryInquiryHandler.class.getName());
    private static final String PROGRAM_ID = "INQHIST";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** SQL cursor query - maps to DECLARE HIST-CURSOR in INQHIST.cbl */
    private static final String HISTORY_QUERY =
            "SELECT TRANS_DATE, TRANS_TYPE, QUANTITY, PRICE, AMOUNT " +
            "FROM POSHIST " +
            "WHERE ACCOUNT_NO = ? " +
            "ORDER BY TRANS_DATE DESC";

    /** Maximum rows to fetch per page - maps to WS-MAX-ROWS */
    private static final int MAX_ROWS_PER_PAGE = 20;

    private final DatabaseManager dbManager;
    private final ErrorHandler errorHandler;

    public HistoryInquiryHandler(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
    }

    /**
     * Main inquiry method - maps to COBOL PROCEDURE DIVISION.
     *
     * PERFORM 1000-CONNECT-DB2
     * PERFORM 2000-OPEN-CURSOR
     * PERFORM 3000-FETCH-RECORDS UNTIL WS-FETCH-DONE = 'Y'
     *   OR WS-ROW-COUNT >= WS-MAX-ROWS
     * PERFORM 4000-CLOSE-CURSOR
     * PERFORM 5000-FORMAT-DISPLAY
     *
     * @param accountNumber the account to query history for
     * @return inquiry response with history data
     */
    public InquiryResponse inquire(String accountNumber) {
        LOGGER.info(PROGRAM_ID + " - History inquiry for account: " + accountNumber);

        try {
            // 1000-CONNECT-DB2: Ensure connection
            // Maps to EXEC CICS LINK PROGRAM('DB2ONLN')
            if (!dbManager.isConnected()) {
                dbManager.connect();
            }

            // 2000/3000/4000: Open cursor, fetch, close
            List<HistoryRow> rows = fetchHistory(accountNumber);

            if (rows.isEmpty()) {
                return handleNotFound(accountNumber);
            }

            // 5000-FORMAT-DISPLAY
            return formatDisplay(accountNumber, rows);

        } catch (Exception e) {
            // DB2 recovery - maps to EXEC CICS LINK PROGRAM('DB2RECV')
            errorHandler.handleSystemError("E900", "History inquiry error", e);
            return InquiryResponse.error("Error retrieving history: " + e.getMessage());
        }
    }

    /**
     * Fetches history records using a cursor-based approach.
     *
     * Maps to:
     *   EXEC SQL OPEN HIST-CURSOR END-EXEC
     *   PERFORM 3000-FETCH-RECORDS UNTIL SQLCODE = 100
     *   EXEC SQL CLOSE HIST-CURSOR END-EXEC
     */
    private List<HistoryRow> fetchHistory(String accountNumber) {
        List<HistoryRow> rows = new ArrayList<>();
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(HISTORY_QUERY)) {
            stmt.setString(1, accountNumber);
            stmt.setMaxRows(MAX_ROWS_PER_PAGE);

            // OPEN CURSOR + FETCH loop
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HistoryRow row = new HistoryRow();
                    row.transDate = rs.getString("TRANS_DATE");
                    row.transType = rs.getString("TRANS_TYPE");
                    row.quantity = rs.getBigDecimal("QUANTITY");
                    row.price = rs.getBigDecimal("PRICE");
                    row.amount = rs.getBigDecimal("AMOUNT");
                    rows.add(row);
                }
            }
            // CLOSE CURSOR is automatic with try-with-resources

        } catch (SQLException e) {
            // Check for NOT FOUND (SQLCODE 100 / SQLSTATE 02000)
            if (DatabaseManager.SQL_NOT_FOUND.equals(e.getSQLState())) {
                LOGGER.info("No history records found for account: " + accountNumber);
            } else {
                dbManager.handleSqlException(e, "FETCH HIST-CURSOR");
                throw new RuntimeException("Database error during history fetch", e);
            }
        }

        return rows;
    }

    /**
     * 5000-FORMAT-DISPLAY: Formats history records for display.
     *
     * Maps to CICS SEND MAP with history data fields.
     */
    private InquiryResponse formatDisplay(String accountNumber, List<HistoryRow> rows) {
        InquiryResponse response = new InquiryResponse();
        response.setResponseCode("00");
        response.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FMT));

        StringBuilder data = new StringBuilder();
        data.append(String.format("Transaction History for Account: %s%n", accountNumber));
        data.append(String.format("%-12s %-6s %15s %15s %15s%n",
                "Date", "Type", "Quantity", "Price", "Amount"));
        data.append(String.format("%s%n", "-".repeat(66)));

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (HistoryRow row : rows) {
            data.append(String.format("%-12s %-6s %15s %15s %15s%n",
                    row.transDate,
                    formatTransType(row.transType),
                    row.quantity,
                    row.price,
                    row.amount));

            if (row.amount != null) {
                totalAmount = totalAmount.add(row.amount);
            }
        }

        data.append(String.format("%s%n", "-".repeat(66)));
        data.append(String.format("Records: %d  Total Amount: %s%n",
                rows.size(), totalAmount));

        if (rows.size() >= MAX_ROWS_PER_PAGE) {
            data.append("(More records available - page down to continue)\n");
        }

        response.setData(data.toString());
        response.setMessage("History inquiry successful - " + rows.size() + " transactions");

        return response;
    }

    /**
     * Handle no records found.
     */
    private InquiryResponse handleNotFound(String accountNumber) {
        LOGGER.info("No history found for account: " + accountNumber);

        InquiryResponse response = new InquiryResponse();
        response.setResponseCode("01");
        response.setMessage("No transaction history found for account: " + accountNumber);
        response.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FMT));
        return response;
    }

    /**
     * Formats transaction type code for display.
     */
    private String formatTransType(String code) {
        if (code == null) return "???";
        switch (code.trim()) {
            case "BU": return "BUY";
            case "SL": return "SELL";
            case "TR": return "XFER";
            case "FE": return "FEE";
            default:   return code;
        }
    }

    /**
     * Internal row structure for cursor fetch results.
     * Maps to the host variables in the FETCH statement.
     */
    private static class HistoryRow {
        String transDate;
        String transType;
        BigDecimal quantity;
        BigDecimal price;
        BigDecimal amount;
    }
}
