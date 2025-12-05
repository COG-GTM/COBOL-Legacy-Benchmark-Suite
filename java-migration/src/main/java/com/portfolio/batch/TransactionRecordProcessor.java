package com.portfolio.batch;

import com.portfolio.entity.TransactionRecord;
import com.portfolio.util.CobolDataConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Processor to transform COBOL Transaction record to JPA entity
 * Handles COBOL data type conversions including COMP-3 packed decimal
 */
public class TransactionRecordProcessor implements ItemProcessor<TransactionInputRecord, TransactionRecord> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionRecordProcessor.class);

    @Override
    public TransactionRecord process(TransactionInputRecord input) throws Exception {
        try {
            LocalDate transDate = parseCobolDate(input.getTransactionDate());
            LocalTime transTime = parseCobolTime(input.getTransactionTime());
            
            String transactionId = generateTransactionId(
                    input.getTransactionDate(),
                    input.getTransactionTime(),
                    input.getSequenceNo());

            TransactionRecord transaction = TransactionRecord.builder()
                    .transactionId(transactionId)
                    .portfolioId(input.getPortfolioId().trim())
                    .transactionDate(transDate)
                    .transactionTime(transTime)
                    .sequenceNo(input.getSequenceNo().trim())
                    .investmentId(input.getInvestmentId().trim())
                    .transactionType(input.getTransactionType().trim())
                    .quantity(parseCobolDecimal(input.getQuantity(), 11, 4))
                    .price(parseCobolDecimal(input.getPrice(), 11, 4))
                    .amount(parseCobolDecimal(input.getAmount(), 13, 2))
                    .currencyCode(input.getCurrencyCode() != null ? input.getCurrencyCode().trim() : "USD")
                    .status(input.getStatus() != null ? input.getStatus().trim() : "P")
                    .build();

            logger.debug("Processed transaction: {}", transaction.getTransactionId());
            return transaction;
        } catch (Exception e) {
            logger.error("Error processing transaction record: {} {}", 
                    input.getTransactionDate(), input.getSequenceNo(), e);
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
     * Parse COBOL time format (HHMMSS) to LocalTime
     */
    private LocalTime parseCobolTime(String cobolTime) {
        if (cobolTime == null || cobolTime.trim().isEmpty() || cobolTime.trim().equals("000000")) {
            return LocalTime.now();
        }
        String trimmed = cobolTime.trim();
        if (trimmed.length() != 6) {
            return LocalTime.now();
        }
        try {
            int hour = Integer.parseInt(trimmed.substring(0, 2));
            int minute = Integer.parseInt(trimmed.substring(2, 4));
            int second = Integer.parseInt(trimmed.substring(4, 6));
            return LocalTime.of(hour, minute, second);
        } catch (Exception e) {
            logger.warn("Invalid time format: {}, using current time", cobolTime);
            return LocalTime.now();
        }
    }

    /**
     * Parse COBOL COMP-3 packed decimal to BigDecimal
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
     * Generate transaction ID from date, time, and sequence
     * Format: YYYYMMDDHHMMSS + sequence
     */
    private String generateTransactionId(String date, String time, String sequence) {
        String dateStr = date != null ? date.trim() : "00000000";
        String timeStr = time != null ? time.trim() : "000000";
        String seqStr = sequence != null ? sequence.trim() : "000000";
        return dateStr + timeStr + seqStr;
    }
}
