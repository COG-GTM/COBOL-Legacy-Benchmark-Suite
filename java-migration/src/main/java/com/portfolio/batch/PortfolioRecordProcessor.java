package com.portfolio.batch;

import com.portfolio.entity.PortfolioMaster;
import com.portfolio.util.CobolDataConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Processor to transform COBOL Portfolio record to JPA entity
 * Handles COBOL data type conversions including COMP-3 packed decimal
 */
public class PortfolioRecordProcessor implements ItemProcessor<PortfolioInputRecord, PortfolioMaster> {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioRecordProcessor.class);

    @Override
    public PortfolioMaster process(PortfolioInputRecord input) throws Exception {
        try {
            PortfolioMaster portfolio = PortfolioMaster.builder()
                    .portfolioId(input.getPortfolioId().trim())
                    .accountNo(input.getAccountNo().trim())
                    .clientName(input.getClientName().trim())
                    .clientType(input.getClientType().trim())
                    .status(input.getStatus().trim())
                    .openDate(parseCobolDate(input.getCreateDate()))
                    .totalValue(parseCobolDecimal(input.getTotalValue(), 13, 2))
                    .cashBalance(parseCobolDecimal(input.getCashBalance(), 13, 2))
                    .accountType("00")
                    .branchId("00")
                    .clientId(generateClientId(input.getAccountNo()))
                    .currencyCode("USD")
                    .build();

            logger.debug("Processed portfolio: {}", portfolio.getPortfolioId());
            return portfolio;
        } catch (Exception e) {
            logger.error("Error processing portfolio record: {}", input.getPortfolioId(), e);
            throw e;
        }
    }

    /**
     * Parse COBOL date format (YYYYMMDD) to LocalDate
     */
    private LocalDate parseCobolDate(String cobolDate) {
        if (cobolDate == null || cobolDate.trim().isEmpty() || cobolDate.trim().equals("00000000")) {
            return LocalDate.now();
        }
        String trimmed = cobolDate.trim();
        if (trimmed.length() != 8) {
            return LocalDate.now();
        }
        try {
            int year = Integer.parseInt(trimmed.substring(0, 4));
            int month = Integer.parseInt(trimmed.substring(4, 6));
            int day = Integer.parseInt(trimmed.substring(6, 8));
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            logger.warn("Invalid date format: {}, using current date", cobolDate);
            return LocalDate.now();
        }
    }

    /**
     * Parse COBOL COMP-3 packed decimal to BigDecimal
     * COBOL: PIC S9(precision)V9(scale) COMP-3
     */
    private BigDecimal parseCobolDecimal(String value, int precision, int scale) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return CobolDataConverter.parsePackedDecimal(value, precision, scale);
        } catch (Exception e) {
            logger.warn("Error parsing decimal value: {}, using zero", value);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Generate client ID from account number
     */
    private String generateClientId(String accountNo) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            return "UNKNOWN";
        }
        return "C" + accountNo.trim().substring(0, Math.min(9, accountNo.trim().length()));
    }
}
