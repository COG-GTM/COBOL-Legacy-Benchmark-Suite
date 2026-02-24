package com.investment.portfolio.online;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.model.PositionRecord;
import com.investment.portfolio.model.PositionRecord.PositionStatus;
import com.investment.portfolio.online.InquiryOnlineHandler.InquiryResponse;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Portfolio Position Inquiry Handler (INQPORT) - Java equivalent of INQPORT.cbl
 *
 * Original COBOL: src/programs/online/INQPORT.cbl
 *
 * Responsibilities:
 * - Retrieves current portfolio positions from the position master file
 * - Formats position data for display
 * - Handles position not found condition
 *
 * CICS mapping:
 * - EXEC CICS READ FILE('POSFILE') → file read with key lookup
 * - EXEC CICS SEND MAP → formatted response output
 *
 * The COBOL version reads from a VSAM KSDS file using the portfolio ID
 * as the key and returns position records for display on a CICS map.
 */
public class PortfolioInquiryHandler {

    private static final Logger LOGGER = Logger.getLogger(PortfolioInquiryHandler.class.getName());
    private static final String PROGRAM_ID = "INQPORT";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path positionFilePath;
    private final ErrorHandler errorHandler;

    public PortfolioInquiryHandler(Path positionFilePath) {
        this.positionFilePath = positionFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
    }

    /**
     * Main inquiry method - maps to COBOL PROCEDURE DIVISION.
     *
     * EXEC CICS READ FILE('POSFILE')
     *   INTO(WS-POS-RECORD)
     *   RIDFLD(WS-PORT-ID)
     *   RESP(WS-CICS-RESP)
     * END-EXEC
     * EVALUATE WS-CICS-RESP
     *   WHEN DFHRESP(NORMAL)   PERFORM 2000-FORMAT-DISPLAY
     *   WHEN DFHRESP(NOTFND)   PERFORM 3000-NOT-FOUND
     *   WHEN OTHER              PERFORM 9000-ERROR-HANDLER
     * END-EVALUATE
     *
     * @param portfolioId  the portfolio to look up
     * @param accountNumber optional account number filter
     * @return inquiry response with position data
     */
    public InquiryResponse inquire(String portfolioId, String accountNumber) {
        LOGGER.info(PROGRAM_ID + " - Portfolio inquiry for: " + portfolioId);

        try {
            List<PositionRecord> positions = readPositions(portfolioId);

            if (positions.isEmpty()) {
                // 3000-NOT-FOUND: Handle position not found
                return handleNotFound(portfolioId);
            }

            // 2000-FORMAT-DISPLAY: Format and return position data
            return formatDisplay(portfolioId, positions);

        } catch (Exception e) {
            // 9000-ERROR-HANDLER
            errorHandler.handleSystemError("E900", "Portfolio inquiry error", e);
            return InquiryResponse.error("Error retrieving portfolio: " + e.getMessage());
        }
    }

    /**
     * Reads all position records for a portfolio.
     * Maps to EXEC CICS READ FILE('POSFILE') with GENERIC key and GTEQ.
     */
    private List<PositionRecord> readPositions(String portfolioId) {
        List<PositionRecord> positions = new ArrayList<>();

        try (FileHandler posFile = new FileHandler(positionFilePath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                errorHandler.handleFileError(posFile.getLastStatus(), positionFilePath.toString());
                return positions;
            }

            String line;
            while ((line = posFile.readLine()) != null) {
                PositionRecord pos = parsePositionRecord(line);
                if (pos != null && portfolioId.equals(pos.getPortfolioId())) {
                    positions.add(pos);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E100", "Error reading position file", e);
        }

        return positions;
    }

    /**
     * 2000-FORMAT-DISPLAY: Formats position records for display.
     *
     * Maps to the CICS SEND MAP with position data fields:
     *   MOVE POS-INVESTMENT-ID TO MAP-INV-ID
     *   MOVE POS-QUANTITY TO MAP-QUANTITY
     *   MOVE POS-COST-BASIS TO MAP-COST
     *   MOVE POS-MARKET-VALUE TO MAP-MKT-VAL
     */
    private InquiryResponse formatDisplay(String portfolioId, List<PositionRecord> positions) {
        InquiryResponse response = new InquiryResponse();
        response.setResponseCode("00");
        response.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FMT));

        StringBuilder data = new StringBuilder();
        data.append(String.format("Portfolio Position Report for: %s%n", portfolioId));
        data.append(String.format("%-10s %-10s %15s %15s %15s %-6s%n",
                "Investment", "Date", "Quantity", "Cost Basis", "Market Value", "Status"));
        data.append(String.format("%s%n", "-".repeat(76)));

        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        int activeCount = 0;

        for (PositionRecord pos : positions) {
            data.append(String.format("%-10s %-10s %15s %15s %15s %-6s%n",
                    pos.getInvestmentId(),
                    pos.getPositionDate(),
                    pos.getQuantity(),
                    pos.getCostBasis(),
                    pos.getMarketValue(),
                    pos.getStatus()));

            if (pos.getCostBasis() != null) {
                totalCostBasis = totalCostBasis.add(pos.getCostBasis());
            }
            if (pos.getMarketValue() != null) {
                totalMarketValue = totalMarketValue.add(pos.getMarketValue());
            }
            if (pos.getStatus() == PositionStatus.ACTIVE) {
                activeCount++;
            }
        }

        data.append(String.format("%s%n", "-".repeat(76)));
        data.append(String.format("%-21s %15s %15s%n", "TOTALS",
                totalCostBasis, totalMarketValue));
        data.append(String.format("Positions: %d total, %d active%n",
                positions.size(), activeCount));

        BigDecimal gainLoss = totalMarketValue.subtract(totalCostBasis);
        data.append(String.format("Unrealized Gain/Loss: %s%n", gainLoss));

        response.setData(data.toString());
        response.setMessage("Portfolio inquiry successful - " + positions.size() + " positions");

        return response;
    }

    /**
     * 3000-NOT-FOUND: Handle position not found condition.
     *
     * Maps to:
     *   MOVE 'Portfolio not found' TO MAP-MESSAGE
     *   EXEC CICS SEND MAP('ERRMAP') MAPSET('INQSET')
     *   END-EXEC
     */
    private InquiryResponse handleNotFound(String portfolioId) {
        LOGGER.info("No positions found for portfolio: " + portfolioId);

        InquiryResponse response = new InquiryResponse();
        response.setResponseCode("01");
        response.setMessage("No positions found for portfolio: " + portfolioId);
        response.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FMT));
        return response;
    }

    /**
     * Parses a fixed-format position record from the master file.
     */
    private PositionRecord parsePositionRecord(String line) {
        try {
            if (line == null || line.length() < 26) return null;

            PositionRecord pos = new PositionRecord();
            pos.setPortfolioId(line.substring(0, 8).trim());
            pos.setPositionDate(line.substring(8, 16).trim());
            pos.setInvestmentId(line.substring(16, 26).trim());

            if (line.length() > 41) {
                pos.setQuantity(new BigDecimal(line.substring(26, 41).trim()));
            }
            if (line.length() > 56) {
                pos.setCostBasis(new BigDecimal(line.substring(41, 56).trim()));
            }
            if (line.length() > 71) {
                pos.setMarketValue(new BigDecimal(line.substring(56, 71).trim()));
            }
            if (line.length() > 74) {
                pos.setCurrency(line.substring(71, 74).trim());
            }
            if (line.length() > 75) {
                pos.setStatus(PositionStatus.fromCode(line.charAt(74)));
            }

            return pos;
        } catch (Exception e) {
            return null;
        }
    }
}
