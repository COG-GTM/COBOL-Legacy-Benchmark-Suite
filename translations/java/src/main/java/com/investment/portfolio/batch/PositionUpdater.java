package com.investment.portfolio.batch;

import com.investment.portfolio.common.DatabaseManager;
import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;
import com.investment.portfolio.model.CheckpointControl;
import com.investment.portfolio.model.CheckpointControl.CheckpointStatus;
import com.investment.portfolio.model.CheckpointControl.ProcessPhase;
import com.investment.portfolio.model.PositionRecord;
import com.investment.portfolio.model.PositionRecord.PositionStatus;
import com.investment.portfolio.model.TransactionRecord;
import com.investment.portfolio.model.TransactionRecord.TransactionType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Position Updater (POSUPD00) - Java equivalent of POSUPDT.cbl
 *
 * Original COBOL: src/programs/batch/POSUPDT.cbl
 *
 * Responsibilities:
 * - Reads validated transactions from TRNVAL00 output
 * - Updates position master records (VSAM KSDS equivalent)
 * - Maintains cost basis calculations
 * - Records transaction history
 * - Supports checkpoint/restart for large volumes
 *
 * Position update rules (from data-dictionary.md Section 5.2):
 * - Share Balance must not go negative
 * - Cost Basis must be updated for every BU/SL
 * - Average Cost must be recalculated for buys
 * - Position Status must be Active for transactions
 */
public class PositionUpdater {

    private static final Logger LOGGER = Logger.getLogger(PositionUpdater.class.getName());
    private static final String PROGRAM_ID = "POSUPD00";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path transactionFilePath;
    private final Path positionMasterPath;
    private final Path historyOutputPath;
    private final Path controlFilePath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;
    private final CheckpointControl checkpoint;

    /** In-memory position cache - maps to VSAM KSDS random access */
    private final Map<String, PositionRecord> positionCache;

    /** Processing counters */
    private long recordsRead;
    private long positionsUpdated;
    private long historyWritten;
    private long errorCount;

    private static final int CHECKPOINT_FREQUENCY = 500;

    public PositionUpdater(Path transactionFilePath, Path positionMasterPath,
                           Path historyOutputPath, Path controlFilePath) {
        this.transactionFilePath = transactionFilePath;
        this.positionMasterPath = positionMasterPath;
        this.historyOutputPath = historyOutputPath;
        this.controlFilePath = controlFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.checkpoint = new CheckpointControl();
        this.checkpoint.setProgramId(PROGRAM_ID);
        this.positionCache = new HashMap<>();
    }

    /**
     * Main entry point - equivalent to COBOL 0000-MAIN.
     */
    public int execute() {
        LOGGER.info(PROGRAM_ID + " - Position Update starting");

        try {
            initialize();
            processTransactions();
            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Open files, initialize checkpoint, load position cache.
     */
    private void initialize() {
        checkpoint.setStatus(CheckpointStatus.INITIAL);
        checkpoint.setPhase(ProcessPhase.INIT);
        checkpoint.setRunDate(LocalDate.now().format(DATE_FMT));

        recordsRead = 0;
        positionsUpdated = 0;
        historyWritten = 0;
        errorCount = 0;

        loadPositionMaster();

        LOGGER.info(PROGRAM_ID + " - Loaded " + positionCache.size() + " position records");
    }

    /**
     * Loads the position master file into the cache.
     * Maps to VSAM KSDS READ with DYNAMIC access mode.
     */
    private void loadPositionMaster() {
        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                LOGGER.info(PROGRAM_ID + " - No existing position master; starting fresh");
                return;
            }

            String line;
            while ((line = posFile.readLine()) != null) {
                PositionRecord pos = parsePositionRecord(line);
                if (pos != null) {
                    positionCache.put(pos.getCompositeKey(), pos);
                }
            }
        } catch (Exception e) {
            LOGGER.info(PROGRAM_ID + " - Position master load: " + e.getMessage());
        }
    }

    /**
     * 2000-PROCESS: Main transaction processing loop.
     */
    private void processTransactions() {
        checkpoint.setStatus(CheckpointStatus.ACTIVE);
        checkpoint.setPhase(ProcessPhase.PROCESS);

        try (FileHandler tranFile = new FileHandler(transactionFilePath);
             FileHandler histFile = new FileHandler(historyOutputPath)) {

            String tranStatus = tranFile.openInput();
            if (!FileHandler.STATUS_SUCCESS.equals(tranStatus)) {
                errorHandler.handleFileError(tranStatus, transactionFilePath.toString());
                returnCode.setCode(ReturnCode.SEVERE);
                return;
            }

            histFile.openOutput();

            String line;
            while ((line = tranFile.readLine()) != null) {
                recordsRead++;
                checkpoint.setRecordsRead(recordsRead);

                TransactionRecord transaction = parseTransactionLine(line);
                if (transaction == null) {
                    errorCount++;
                    continue;
                }

                boolean success = updatePosition(transaction, histFile);
                if (!success) {
                    errorCount++;
                }

                // Checkpoint every N records
                if (recordsRead % CHECKPOINT_FREQUENCY == 0) {
                    takeCheckpoint();
                }

                if (errorCount > checkpoint.getMaxErrors()) {
                    LOGGER.severe("Error threshold exceeded");
                    returnCode.setCode(ReturnCode.ERROR);
                    break;
                }
            }

        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error during position updates", e);
            returnCode.setCode(ReturnCode.SEVERE);
        }
    }

    /**
     * Updates or creates a position record based on the transaction.
     * Implements the core position update business logic.
     */
    private boolean updatePosition(TransactionRecord trn, FileHandler histFile) {
        // Key matches PositionRecord.getCompositeKey(): portfolioId + investmentId
        // (date excluded so transactions on any date find the same position)
        String posKey = trn.getPortfolioId() + trn.getInvestmentId();

        PositionRecord position = positionCache.get(posKey);

        if (position == null) {
            // Create new position for BUY transactions
            if (trn.getType() == TransactionType.BUY) {
                position = createNewPosition(trn);
                positionCache.put(posKey, position);
            } else {
                errorHandler.handleValidationError("E004",
                        "No position found for non-buy transaction",
                        "Key: " + posKey);
                return false;
            }
        } else {
            // Validate position is active
            if (position.getStatus() != PositionStatus.ACTIVE) {
                errorHandler.handleValidationError("E004",
                        "Position not active for transactions",
                        "Key: " + posKey + " Status: " + position.getStatus());
                return false;
            }
        }

        // Record before-image for history
        BigDecimal beforeBalance = position.getQuantity();
        BigDecimal beforeCost = position.getCostBasis();

        // Apply the transaction
        switch (trn.getType()) {
            case BUY:
                applyBuy(position, trn);
                break;
            case SELL:
                if (!applySell(position, trn)) {
                    return false; // Insufficient balance
                }
                break;
            case FEE:
                applyFee(position, trn);
                break;
            case TRANSFER:
                applyTransfer(position, trn);
                break;
        }

        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser(PROGRAM_ID);
        positionsUpdated++;

        // Write history record
        writeHistoryRecord(histFile, trn, beforeBalance, beforeCost,
                position.getQuantity(), position.getCostBasis());
        historyWritten++;

        return true;
    }

    /**
     * Creates a new position record.
     */
    private PositionRecord createNewPosition(TransactionRecord trn) {
        PositionRecord pos = new PositionRecord();
        pos.setPortfolioId(trn.getPortfolioId());
        pos.setPositionDate(trn.getTransactionDate());
        pos.setInvestmentId(trn.getInvestmentId());
        pos.setQuantity(BigDecimal.ZERO);
        pos.setCostBasis(BigDecimal.ZERO);
        pos.setMarketValue(BigDecimal.ZERO);
        pos.setCurrency(trn.getCurrency() != null ? trn.getCurrency() : "USD");
        pos.setStatus(PositionStatus.ACTIVE);
        pos.setLastMaintDate(LocalDateTime.now());
        pos.setLastMaintUser(PROGRAM_ID);
        return pos;
    }

    /**
     * Applies a BUY transaction: increases shares, recalculates average cost.
     */
    private void applyBuy(PositionRecord pos, TransactionRecord trn) {
        BigDecimal newQuantity = pos.getQuantity().add(trn.getQuantity());
        BigDecimal totalCost = pos.getCostBasis().add(trn.getAmount());

        pos.setQuantity(newQuantity);
        pos.setCostBasis(totalCost);

        // Recalculate market value
        if (trn.getPrice() != null && trn.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            pos.setMarketValue(newQuantity.multiply(trn.getPrice())
                    .setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * Applies a SELL transaction: decreases shares, adjusts cost basis.
     * Returns false if insufficient balance.
     */
    private boolean applySell(PositionRecord pos, TransactionRecord trn) {
        BigDecimal newQuantity = pos.getQuantity().subtract(trn.getQuantity());

        // Share Balance must not go negative
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            errorHandler.handleValidationError("E004",
                    "Insufficient position balance for sell",
                    "Current: " + pos.getQuantity() + " Sell: " + trn.getQuantity());
            return false;
        }

        // Proportional cost basis reduction
        if (pos.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = trn.getQuantity()
                    .divide(pos.getQuantity(), 10, RoundingMode.HALF_UP);
            BigDecimal costReduction = pos.getCostBasis().multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
            pos.setCostBasis(pos.getCostBasis().subtract(costReduction));
        }

        pos.setQuantity(newQuantity);

        // Close position if zero balance
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            pos.setStatus(PositionStatus.CLOSED);
        }

        return true;
    }

    /**
     * Applies a FEE transaction: adjusts cost basis.
     */
    private void applyFee(PositionRecord pos, TransactionRecord trn) {
        pos.setCostBasis(pos.getCostBasis().add(trn.getAmount()));
    }

    /**
     * Applies a TRANSFER transaction: adjusts quantity.
     */
    private void applyTransfer(PositionRecord pos, TransactionRecord trn) {
        pos.setQuantity(pos.getQuantity().add(trn.getQuantity()));
    }

    /**
     * Writes a history record capturing before/after position state.
     */
    private void writeHistoryRecord(FileHandler histFile, TransactionRecord trn,
                                    BigDecimal beforeBal, BigDecimal beforeCost,
                                    BigDecimal afterBal, BigDecimal afterCost) {
        String histLine = String.format("%-8s%-8s%-10s%-2s%15s%15s%15s%15s",
                trn.getPortfolioId(),
                trn.getTransactionDate(),
                trn.getInvestmentId(),
                trn.getType().getCode(),
                beforeBal, afterBal,
                beforeCost, afterCost);
        histFile.writeLine(histLine);
    }

    /**
     * 3000-TERMINATE: Write updated positions, close files, display stats.
     */
    private void terminate() {
        checkpoint.setStatus(CheckpointStatus.COMPLETE);
        checkpoint.setPhase(ProcessPhase.TERMINATE);

        writePositionMaster();

        if (errorCount > 0 && returnCode.getCurrentCode() < ReturnCode.WARNING) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        displayStatistics();
    }

    /**
     * Writes the position cache back to the master file.
     */
    private void writePositionMaster() {
        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            posFile.openOutput();
            for (PositionRecord pos : positionCache.values()) {
                posFile.writeLine(formatPosition(pos));
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E300", "Error writing position master", e);
            returnCode.setCode(ReturnCode.SEVERE);
        }
    }

    private String formatPosition(PositionRecord pos) {
        return String.format("%-8s%-8s%-10s%15s%15s%15s%-3s%c",
                pos.getPortfolioId(),
                pos.getPositionDate(),
                pos.getInvestmentId(),
                pos.getQuantity(),
                pos.getCostBasis(),
                pos.getMarketValue(),
                pos.getCurrency(),
                pos.getStatus().getCode());
    }

    private TransactionRecord parseTransactionLine(String line) {
        try {
            if (line == null || line.length() < 28) return null;
            TransactionRecord trn = new TransactionRecord();
            trn.setTransactionDate(line.substring(0, 8).trim());
            trn.setTransactionTime(line.substring(8, 14).trim());
            trn.setPortfolioId(line.substring(14, 22).trim());
            trn.setSequenceNumber(line.substring(22, 28).trim());
            if (line.length() > 38) trn.setInvestmentId(line.substring(28, 38).trim());
            if (line.length() > 40) trn.setType(TransactionType.fromCode(line.substring(38, 40).trim()));
            if (line.length() > 55) trn.setQuantity(new BigDecimal(line.substring(40, 55).trim()));
            if (line.length() > 70) trn.setPrice(new BigDecimal(line.substring(55, 70).trim()));
            if (line.length() > 85) trn.setAmount(new BigDecimal(line.substring(70, 85).trim()));
            if (line.length() > 88) trn.setCurrency(line.substring(85, 88).trim());
            return trn;
        } catch (Exception e) {
            return null;
        }
    }

    private PositionRecord parsePositionRecord(String line) {
        try {
            if (line == null || line.length() < 26) return null;
            PositionRecord pos = new PositionRecord();
            pos.setPortfolioId(line.substring(0, 8).trim());
            pos.setPositionDate(line.substring(8, 16).trim());
            pos.setInvestmentId(line.substring(16, 26).trim());
            if (line.length() > 41) pos.setQuantity(new BigDecimal(line.substring(26, 41).trim()));
            if (line.length() > 56) pos.setCostBasis(new BigDecimal(line.substring(41, 56).trim()));
            if (line.length() > 71) pos.setMarketValue(new BigDecimal(line.substring(56, 71).trim()));
            if (line.length() > 74) pos.setCurrency(line.substring(71, 74).trim());
            if (line.length() > 75) pos.setStatus(PositionStatus.fromCode(line.charAt(74)));
            return pos;
        } catch (Exception e) {
            return null;
        }
    }

    private void takeCheckpoint() {
        checkpoint.setRecordsRead(recordsRead);
        checkpoint.setRecordsProcessed(positionsUpdated);
        checkpoint.setRecordsInError(errorCount);
        LOGGER.info(String.format("Checkpoint at record %d: updated=%d errors=%d",
                recordsRead, positionsUpdated, errorCount));
    }

    private void displayStatistics() {
        LOGGER.info(PROGRAM_ID + " Processing Statistics:");
        LOGGER.info("  Records Read:       " + recordsRead);
        LOGGER.info("  Positions Updated:  " + positionsUpdated);
        LOGGER.info("  History Written:    " + historyWritten);
        LOGGER.info("  Error Count:        " + errorCount);
        LOGGER.info("  Return Code:        " + returnCode.getCurrentCode());
    }
}
