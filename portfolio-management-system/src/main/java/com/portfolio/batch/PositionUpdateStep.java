package com.portfolio.batch;

import com.portfolio.model.HistoryRecord;
import com.portfolio.model.PositionRecord;
import com.portfolio.support.BatchExceptions;
import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.ErrorLoggingService;
import com.portfolio.support.HistoryRecordRepository;
import com.portfolio.support.PositionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Position Update Step.
 * Migrated from COBOL POSUPD00.
 * Reads POSITION_MASTER, calculates cost basis,
 * updates POSITION_MASTER + TRANSACTION_HISTORY_VSAM.
 * Maps exit status to RC 0/4/8/12.
 */
@Component
public class PositionUpdateStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(PositionUpdateStep.class);

    private final PositionRecordRepository positionRepository;
    private final HistoryRecordRepository historyRepository;
    private final ErrorLoggingService errorLoggingService;
    private final Db2StatisticsService statisticsService;

    private int recordsProcessed = 0;
    private int recordsError = 0;
    private int recordsWarning = 0;

    public PositionUpdateStep(
            PositionRecordRepository positionRepository,
            HistoryRecordRepository historyRepository,
            ErrorLoggingService errorLoggingService,
            Db2StatisticsService statisticsService) {
        this.positionRepository = positionRepository;
        this.historyRepository = historyRepository;
        this.errorLoggingService = errorLoggingService;
        this.statisticsService = statisticsService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Starting Position Update Step (POSUPD00)");

        // Reset counters for each execution (singleton component reused across runs)
        recordsProcessed = 0;
        recordsError = 0;
        recordsWarning = 0;

        List<PositionRecord> positions = positionRepository.findAll();
        log.info("Found {} positions to update", positions.size());

        AtomicInteger seqCounter = new AtomicInteger(1);

        for (PositionRecord position : positions) {
            try {
                updatePosition(position);
                writeHistoryRecord(position, seqCounter.getAndIncrement());
                recordsProcessed++;
                statisticsService.recordUpdate();
            } catch (BatchExceptions.BatchWarningException e) {
                recordsWarning++;
                errorLoggingService.logWarning("POSUPD00", "POS4",
                        e.getMessage(), "BATCH");
            } catch (Exception e) {
                recordsError++;
                errorLoggingService.logApplicationError("POSUPD00", "POS8",
                        "Position update failed: " + e.getMessage(),
                        position.getPortfolioId() + "/" + position.getSymbolId(), "BATCH");
            }
        }

        int returnCode = determineReturnCode();
        contribution.setExitStatus(new ExitStatus("RC_" + returnCode));

        log.info("Position Update complete: processed={}, warnings={}, errors={}, RC={}",
                recordsProcessed, recordsWarning, recordsError, returnCode);

        return RepeatStatus.FINISHED;
    }

    /**
     * Update position cost basis calculation.
     * Migrated from COBOL POSUPD00 cost basis logic.
     */
    private void updatePosition(PositionRecord position) {
        if (position.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new BatchExceptions.BatchWarningException(
                    "Negative quantity for position " + position.getPortfolioId() + "/" + position.getSymbolId());
        }

        // Recalculate market value using current market price if available,
        // otherwise preserve existing market value. Cost basis is validated separately.
        if (position.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            // If market value was already set by the validation step (from transaction price),
            // keep it. Otherwise, use cost basis as a conservative estimate.
            if (position.getMarketValue() == null || position.getMarketValue().compareTo(BigDecimal.ZERO) == 0) {
                position.setMarketValue(position.getCostBasis());
            }
            // Ensure market value is properly scaled
            position.setMarketValue(position.getMarketValue().setScale(2, RoundingMode.HALF_UP));
        }

        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("BATCH");
        positionRepository.save(position);
    }

    /**
     * Write a history record to TRANSACTION_HISTORY_VSAM table.
     */
    private void writeHistoryRecord(PositionRecord position, int seq) {
        HistoryRecord history = new HistoryRecord();
        history.setPortfolioId(position.getPortfolioId());
        history.setTxnDate(LocalDate.now());
        history.setSeq(seq);
        history.setInvestmentId(position.getSymbolId());
        history.setTransactionType("PS");
        history.setQuantity(position.getQuantity());
        history.setPrice(BigDecimal.ZERO);
        history.setAmount(position.getCostBasis());
        history.setCurrencyCode(position.getCurrencyCode());
        history.setStatus("D");
        history.setCostBasis(position.getCostBasis());
        history.setProcessDate(LocalDateTime.now());
        history.setProcessUser("BATCH");

        historyRepository.save(history);
        statisticsService.recordInsert();
    }

    private int determineReturnCode() {
        if (recordsError > 0) return BatchExceptions.RC_ERROR;
        if (recordsWarning > 0) return BatchExceptions.RC_WARNING;
        return BatchExceptions.RC_SUCCESS;
    }

    public int getRecordsProcessed() { return recordsProcessed; }
    public int getRecordsError() { return recordsError; }
}
