package com.portfolio.modernization.migration;

import com.portfolio.modernization.entity.HistoryRecord;
import com.portfolio.modernization.entity.PositionRecord;
import com.portfolio.modernization.entity.TransactionRecord;
import com.portfolio.modernization.repository.HistoryRepository;
import com.portfolio.modernization.repository.PositionRepository;
import com.portfolio.modernization.repository.TransactionRepository;
import com.portfolio.modernization.validation.DataValidationFramework;
import com.portfolio.modernization.validation.DataValidationFramework.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VSAM Migration Service
 * 
 * Provides Java-based migration capabilities for converting VSAM file data
 * to relational database records. This service complements the SQL-based
 * migration scripts with programmatic data transformation and validation.
 * 
 * Migration Process:
 * 1. Parse VSAM record format (fixed-length fields)
 * 2. Transform COBOL data types to Java types
 * 3. Validate transformed data
 * 4. Persist to relational database
 * 5. Track migration lineage
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Service
public class VsamMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(VsamMigrationService.class);

    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final HistoryRepository historyRepository;
    private final DataValidationFramework validationFramework;

    @Autowired
    public VsamMigrationService(PositionRepository positionRepository,
                                TransactionRepository transactionRepository,
                                HistoryRepository historyRepository,
                                DataValidationFramework validationFramework) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.historyRepository = historyRepository;
        this.validationFramework = validationFramework;
    }

    /**
     * Migration result containing statistics and errors
     */
    public static class MigrationResult {
        private final String migrationType;
        private final int totalRecords;
        private final int successfulRecords;
        private final int failedRecords;
        private final List<String> errors;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public MigrationResult(String migrationType, int totalRecords, int successfulRecords,
                              int failedRecords, List<String> errors,
                              LocalDateTime startTime, LocalDateTime endTime) {
            this.migrationType = migrationType;
            this.totalRecords = totalRecords;
            this.successfulRecords = successfulRecords;
            this.failedRecords = failedRecords;
            this.errors = errors;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getMigrationType() { return migrationType; }
        public int getTotalRecords() { return totalRecords; }
        public int getSuccessfulRecords() { return successfulRecords; }
        public int getFailedRecords() { return failedRecords; }
        public List<String> getErrors() { return errors; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public double getSuccessRate() {
            return totalRecords > 0 ? (double) successfulRecords / totalRecords * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("MigrationResult{type='%s', total=%d, success=%d, failed=%d, rate=%.2f%%}",
                    migrationType, totalRecords, successfulRecords, failedRecords, getSuccessRate());
        }
    }

    /**
     * Migrates position records from VSAM format.
     * 
     * @param vsamRecords list of raw VSAM record strings
     * @param processUser user performing the migration
     * @return migration result
     */
    @Transactional
    public MigrationResult migratePositions(List<String> vsamRecords, String processUser) {
        logger.info("Starting position migration for {} records", vsamRecords.size());
        LocalDateTime startTime = LocalDateTime.now();
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (String vsamRecord : vsamRecords) {
            try {
                PositionRecord position = parsePositionRecord(vsamRecord);
                position.setLastMaintUser(processUser);
                position.setVsamMigrationDate(LocalDateTime.now());
                position.setVsamRecordKey(position.createVsamRecordKey());

                ValidationResult validation = validationFramework.validatePosition(position);
                if (!validation.isValid()) {
                    String errorMsg = String.format("Validation failed for position %s: %s",
                            position.getPortfolioId(), validation.getErrors());
                    errors.add(errorMsg);
                    failCount.incrementAndGet();
                    continue;
                }

                positionRepository.save(position);
                successCount.incrementAndGet();
                
            } catch (Exception e) {
                String errorMsg = String.format("Failed to migrate position record: %s", e.getMessage());
                errors.add(errorMsg);
                failCount.incrementAndGet();
                logger.error(errorMsg, e);
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        MigrationResult result = new MigrationResult(
                "POSITION_MIGRATION",
                vsamRecords.size(),
                successCount.get(),
                failCount.get(),
                errors,
                startTime,
                endTime
        );

        logger.info("Position migration completed: {}", result);
        return result;
    }

    /**
     * Migrates transaction records from VSAM format.
     * 
     * @param vsamRecords list of raw VSAM record strings
     * @param processUser user performing the migration
     * @return migration result
     */
    @Transactional
    public MigrationResult migrateTransactions(List<String> vsamRecords, String processUser) {
        logger.info("Starting transaction migration for {} records", vsamRecords.size());
        LocalDateTime startTime = LocalDateTime.now();
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (String vsamRecord : vsamRecords) {
            try {
                TransactionRecord transaction = parseTransactionRecord(vsamRecord);
                transaction.setProcessUser(processUser);
                transaction.setVsamMigrationDate(LocalDateTime.now());
                transaction.setVsamRecordKey(transaction.createVsamRecordKey());

                ValidationResult validation = validationFramework.validateTransaction(transaction);
                if (!validation.isValid()) {
                    String errorMsg = String.format("Validation failed for transaction %s: %s",
                            transaction.getTransactionId(), validation.getErrors());
                    errors.add(errorMsg);
                    failCount.incrementAndGet();
                    continue;
                }

                transactionRepository.save(transaction);
                successCount.incrementAndGet();
                
            } catch (Exception e) {
                String errorMsg = String.format("Failed to migrate transaction record: %s", e.getMessage());
                errors.add(errorMsg);
                failCount.incrementAndGet();
                logger.error(errorMsg, e);
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        MigrationResult result = new MigrationResult(
                "TRANSACTION_MIGRATION",
                vsamRecords.size(),
                successCount.get(),
                failCount.get(),
                errors,
                startTime,
                endTime
        );

        logger.info("Transaction migration completed: {}", result);
        return result;
    }

    /**
     * Migrates history records from VSAM format.
     * 
     * @param vsamRecords list of raw VSAM record strings
     * @param processUser user performing the migration
     * @return migration result
     */
    @Transactional
    public MigrationResult migrateHistory(List<String> vsamRecords, String processUser) {
        logger.info("Starting history migration for {} records", vsamRecords.size());
        LocalDateTime startTime = LocalDateTime.now();
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (String vsamRecord : vsamRecords) {
            try {
                HistoryRecord history = parseHistoryRecord(vsamRecord);
                history.setProcessUser(processUser);
                history.setVsamMigrationDate(LocalDateTime.now());
                history.setVsamRecordKey(history.createVsamRecordKey());

                ValidationResult validation = validationFramework.validateHistory(history);
                if (!validation.isValid()) {
                    String errorMsg = String.format("Validation failed for history record: %s",
                            validation.getErrors());
                    errors.add(errorMsg);
                    failCount.incrementAndGet();
                    continue;
                }

                historyRepository.save(history);
                successCount.incrementAndGet();
                
            } catch (Exception e) {
                String errorMsg = String.format("Failed to migrate history record: %s", e.getMessage());
                errors.add(errorMsg);
                failCount.incrementAndGet();
                logger.error(errorMsg, e);
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        MigrationResult result = new MigrationResult(
                "HISTORY_MIGRATION",
                vsamRecords.size(),
                successCount.get(),
                failCount.get(),
                errors,
                startTime,
                endTime
        );

        logger.info("History migration completed: {}", result);
        return result;
    }

    /**
     * Parses a VSAM position record string into a PositionRecord entity.
     * 
     * VSAM POSREC layout (from POSREC.cpy):
     * - POS-PORTFOLIO-ID: positions 1-8 (X(08))
     * - POS-DATE: positions 9-16 (X(08))
     * - POS-INVESTMENT-ID: positions 17-26 (X(10))
     * - POS-QUANTITY: positions 27-34 (S9(11)V9(4) COMP-3, 8 bytes)
     * - POS-COST-BASIS: positions 35-42 (S9(13)V9(2) COMP-3, 8 bytes)
     * - POS-MARKET-VALUE: positions 43-50 (S9(13)V9(2) COMP-3, 8 bytes)
     * - POS-CURRENCY: positions 51-53 (X(03))
     * - POS-STATUS: position 54 (X(01))
     * - POS-LAST-MAINT-DATE: positions 55-80 (X(26))
     * - POS-LAST-MAINT-USER: positions 81-88 (X(08))
     * 
     * @param vsamRecord raw VSAM record string
     * @return parsed PositionRecord
     */
    private PositionRecord parsePositionRecord(String vsamRecord) {
        PositionRecord position = new PositionRecord();
        
        String portfolioId = extractField(vsamRecord, 0, 8).trim();
        String dateStr = extractField(vsamRecord, 8, 16).trim();
        String investmentId = extractField(vsamRecord, 16, 26).trim();
        
        position.setPortfolioId(portfolioId + "-" + investmentId);
        position.setAccountNumber(portfolioId);
        position.setFundId(investmentId);
        
        position.setPositionDate(parseDate(dateStr));
        
        String quantityStr = extractField(vsamRecord, 26, 41).trim();
        String costBasisStr = extractField(vsamRecord, 41, 56).trim();
        String marketValueStr = extractField(vsamRecord, 56, 71).trim();
        
        position.setUnits(parseDecimal(quantityStr, 4));
        position.setCostBasis(parseDecimal(costBasisStr, 2));
        position.setMarketValue(parseDecimal(marketValueStr, 2));
        
        String currency = extractField(vsamRecord, 71, 74).trim();
        position.setCurrencyCode(currency.isEmpty() ? "USD" : currency);
        
        String status = extractField(vsamRecord, 74, 75).trim();
        position.setStatus(status.isEmpty() ? PositionRecord.STATUS_ACTIVE : status);
        
        String maintDateStr = extractField(vsamRecord, 75, 101).trim();
        position.setLastUpdate(parseTimestamp(maintDateStr));
        
        String maintUser = extractField(vsamRecord, 101, 109).trim();
        position.setLastMaintUser(maintUser.isEmpty() ? "MIGRATION" : maintUser);
        
        return position;
    }

    /**
     * Parses a VSAM transaction record string into a TransactionRecord entity.
     * 
     * VSAM TRNREC layout (from TRNREC.cpy):
     * - TRN-DATE: positions 1-8 (X(08))
     * - TRN-TIME: positions 9-14 (X(06))
     * - TRN-PORTFOLIO-ID: positions 15-22 (X(08))
     * - TRN-SEQUENCE-NO: positions 23-28 (X(06))
     * - TRN-INVESTMENT-ID: positions 29-38 (X(10))
     * - TRN-TYPE: positions 39-40 (X(02))
     * - TRN-QUANTITY: positions 41-48 (S9(11)V9(4) COMP-3, 8 bytes)
     * - TRN-PRICE: positions 49-56 (S9(11)V9(4) COMP-3, 8 bytes)
     * - TRN-AMOUNT: positions 57-64 (S9(13)V9(2) COMP-3, 8 bytes)
     * - TRN-CURRENCY: positions 65-67 (X(03))
     * - TRN-STATUS: position 68 (X(01))
     * 
     * @param vsamRecord raw VSAM record string
     * @return parsed TransactionRecord
     */
    private TransactionRecord parseTransactionRecord(String vsamRecord) {
        TransactionRecord transaction = new TransactionRecord();
        
        String dateStr = extractField(vsamRecord, 0, 8).trim();
        String timeStr = extractField(vsamRecord, 8, 14).trim();
        String portfolioId = extractField(vsamRecord, 14, 22).trim();
        String sequenceNo = extractField(vsamRecord, 22, 28).trim();
        
        transaction.setTransactionDate(parseDate(dateStr));
        transaction.setTransactionTime(parseTime(timeStr));
        transaction.setPortfolioId(portfolioId);
        transaction.setSequenceNo(sequenceNo);
        
        transaction.generateTransactionId();
        
        String investmentId = extractField(vsamRecord, 28, 38).trim();
        transaction.setInvestmentId(investmentId);
        
        String typeCode = extractField(vsamRecord, 38, 40).trim();
        transaction.setTransactionType(TransactionRecord.convertLegacyType(typeCode));
        
        String quantityStr = extractField(vsamRecord, 40, 55).trim();
        String priceStr = extractField(vsamRecord, 55, 70).trim();
        String amountStr = extractField(vsamRecord, 70, 85).trim();
        
        transaction.setUnits(parseDecimal(quantityStr, 4));
        transaction.setPrice(parseDecimal(priceStr, 4));
        transaction.setAmount(parseDecimal(amountStr, 2));
        
        String currency = extractField(vsamRecord, 85, 88).trim();
        transaction.setCurrencyCode(currency.isEmpty() ? "USD" : currency);
        
        String status = extractField(vsamRecord, 88, 89).trim();
        transaction.setStatus(status.isEmpty() ? TransactionRecord.STATUS_DONE : status);
        
        transaction.setProcessDate(LocalDateTime.now());
        
        return transaction;
    }

    /**
     * Parses a VSAM history record string into a HistoryRecord entity.
     * 
     * VSAM HISTREC layout (from HISTREC.cpy):
     * - HIST-PORTFOLIO-ID: positions 1-8 (X(08))
     * - HIST-DATE: positions 9-16 (X(08))
     * - HIST-TIME: positions 17-22 (X(06))
     * - HIST-SEQ-NO: positions 23-26 (X(04))
     * - HIST-RECORD-TYPE: positions 27-28 (X(02))
     * - HIST-ACTION-CODE: position 29 (X(01))
     * - HIST-BEFORE-IMAGE: positions 30-429 (X(400))
     * - HIST-AFTER-IMAGE: positions 430-829 (X(400))
     * - HIST-REASON-CODE: positions 830-833 (X(04))
     * 
     * @param vsamRecord raw VSAM record string
     * @return parsed HistoryRecord
     */
    private HistoryRecord parseHistoryRecord(String vsamRecord) {
        HistoryRecord history = new HistoryRecord();
        
        String portfolioId = extractField(vsamRecord, 0, 8).trim();
        String dateStr = extractField(vsamRecord, 8, 16).trim();
        String timeStr = extractField(vsamRecord, 16, 22).trim();
        String seqNo = extractField(vsamRecord, 22, 26).trim();
        
        history.setPortfolioId(portfolioId);
        history.setHistoryDate(parseDate(dateStr));
        history.setHistoryTime(parseTime(timeStr));
        history.setSequenceNo(seqNo);
        
        String recordType = extractField(vsamRecord, 26, 28).trim();
        String actionCode = extractField(vsamRecord, 28, 29).trim();
        
        history.setRecordType(recordType);
        history.setActionCode(actionCode);
        
        if (vsamRecord.length() > 29) {
            String beforeImage = extractField(vsamRecord, 29, Math.min(429, vsamRecord.length())).trim();
            history.setBeforeImage(beforeImage.isEmpty() ? null : beforeImage);
        }
        
        if (vsamRecord.length() > 429) {
            String afterImage = extractField(vsamRecord, 429, Math.min(829, vsamRecord.length())).trim();
            history.setAfterImage(afterImage.isEmpty() ? null : afterImage);
        }
        
        if (vsamRecord.length() > 829) {
            String reasonCode = extractField(vsamRecord, 829, Math.min(833, vsamRecord.length())).trim();
            history.setReasonCode(reasonCode.isEmpty() ? null : reasonCode);
        }
        
        history.setProcessDate(LocalDateTime.now());
        history.generateChangeSummary();
        
        return history;
    }

    /**
     * Extracts a field from a fixed-length VSAM record.
     */
    private String extractField(String record, int start, int end) {
        if (record == null || start >= record.length()) {
            return "";
        }
        int actualEnd = Math.min(end, record.length());
        return record.substring(start, actualEnd);
    }

    /**
     * Parses a COBOL date string (YYYYMMDD) to LocalDate.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.length() < 8) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse date: {}", dateStr);
            return LocalDate.now();
        }
    }

    /**
     * Parses a COBOL time string (HHMMSS) to LocalTime.
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty() || timeStr.length() < 6) {
            return LocalTime.now();
        }
        try {
            return LocalTime.parse(timeStr.substring(0, 6), DateTimeFormatter.ofPattern("HHmmss"));
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse time: {}", timeStr);
            return LocalTime.now();
        }
    }

    /**
     * Parses a COBOL timestamp string to LocalDateTime.
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            if (timestampStr.length() >= 19) {
                return LocalDateTime.parse(timestampStr.substring(0, 19),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss"));
            } else if (timestampStr.length() >= 14) {
                return LocalDateTime.parse(timestampStr.substring(0, 14),
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse timestamp: {}", timestampStr);
        }
        return LocalDateTime.now();
    }

    /**
     * Parses a numeric string to BigDecimal with specified scale.
     */
    private BigDecimal parseDecimal(String numStr, int scale) {
        if (numStr == null || numStr.trim().isEmpty()) {
            return null;
        }
        try {
            String cleaned = numStr.replaceAll("[^0-9.-]", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            BigDecimal value = new BigDecimal(cleaned);
            return value.setScale(scale, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse decimal: {}", numStr);
            return null;
        }
    }

    /**
     * Verifies migration integrity by comparing source and target counts.
     * 
     * @return map of verification results
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifyMigrationIntegrity() {
        Map<String, Object> results = new LinkedHashMap<>();
        
        long positionCount = positionRepository.count();
        long migratedPositions = positionRepository.findMigratedPositions().size();
        results.put("totalPositions", positionCount);
        results.put("migratedPositions", migratedPositions);
        
        long transactionCount = transactionRepository.count();
        long migratedTransactions = transactionRepository.findMigratedTransactions().size();
        results.put("totalTransactions", transactionCount);
        results.put("migratedTransactions", migratedTransactions);
        
        long historyCount = historyRepository.count();
        long migratedHistory = historyRepository.findMigratedRecords().size();
        results.put("totalHistory", historyCount);
        results.put("migratedHistory", migratedHistory);
        
        results.put("verificationTimestamp", LocalDateTime.now());
        
        return results;
    }
}
