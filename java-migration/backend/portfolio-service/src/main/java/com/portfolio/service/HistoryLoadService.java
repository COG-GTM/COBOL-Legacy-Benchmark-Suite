package com.portfolio.service;

import com.portfolio.dto.BatchProcessingResult;
import com.portfolio.dto.HistoryLoadRequest;
import com.portfolio.model.entity.PositionHistory;
import com.portfolio.model.enums.ActionCode;
import com.portfolio.model.enums.HistoryRecordType;
import com.portfolio.repository.PositionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryLoadService {

    private final PositionHistoryRepository positionHistoryRepository;

    private static final int COMMIT_THRESHOLD = 1000;
    private static final int MAX_ERRORS = 100;

    @Transactional
    public BatchProcessingResult loadHistoryBatch(List<HistoryLoadRequest> requests) {
        log.info("Starting history load batch processing for {} records", requests.size());

        BatchProcessingResult result = BatchProcessingResult.builder()
                .recordsRead(0)
                .recordsWritten(0)
                .errorCount(0)
                .status("IN_PROGRESS")
                .build();

        AtomicInteger commitCounter = new AtomicInteger(0);

        for (HistoryLoadRequest request : requests) {
            result.incrementRead();

            if (result.getErrorCount() > MAX_ERRORS) {
                log.warn("Maximum error count ({}) exceeded, stopping batch processing", MAX_ERRORS);
                result.setStatus("STOPPED_MAX_ERRORS");
                result.setMessage("Processing stopped: maximum error count exceeded");
                break;
            }

            try {
                loadSingleRecord(request);
                result.incrementWritten();

                int currentCount = commitCounter.incrementAndGet();
                if (currentCount >= COMMIT_THRESHOLD) {
                    log.info("Commit threshold reached ({} records), committing batch", COMMIT_THRESHOLD);
                    commitCounter.set(0);
                }

            } catch (Exception e) {
                if (isDuplicateKeyError(e)) {
                    log.debug("Duplicate key for portfolio {}, skipping", request.getPortfolioId());
                } else {
                    log.error("Error loading history record for portfolio {}: {}", 
                            request.getPortfolioId(), e.getMessage());
                    result.addError(String.format("Portfolio %s: %s", 
                            request.getPortfolioId(), e.getMessage()));
                }
            }
        }

        if (result.getErrorCount() <= MAX_ERRORS) {
            result.setStatus("COMPLETED");
            result.setMessage(String.format("History load completed. Read: %d, Written: %d, Errors: %d",
                    result.getRecordsRead(), result.getRecordsWritten(), result.getErrorCount()));
        }

        log.info("HISTLD00 Processing Statistics:");
        log.info("  Records Read:    {}", result.getRecordsRead());
        log.info("  Records Written: {}", result.getRecordsWritten());
        log.info("  Errors:          {}", result.getErrorCount());

        return result;
    }

    @Transactional
    public PositionHistory loadSingleRecord(HistoryLoadRequest request) {
        PositionHistory history = mapToEntity(request);
        return positionHistoryRepository.save(history);
    }

    public List<PositionHistory> getHistoryByPortfolioId(String portfolioId) {
        return positionHistoryRepository.findByPortfolioId(portfolioId);
    }

    public List<PositionHistory> getHistoryByAccountNo(String accountNo) {
        return positionHistoryRepository.findByAccountNo(accountNo);
    }

    public List<PositionHistory> getHistoryByDateRange(LocalDate startDate, LocalDate endDate) {
        return positionHistoryRepository.findByHistoryDateBetween(startDate, endDate);
    }

    public List<PositionHistory> getHistoryByPortfolioIdAndDateRange(
            String portfolioId, LocalDate startDate, LocalDate endDate) {
        return positionHistoryRepository.findByPortfolioIdAndHistoryDateBetween(portfolioId, startDate, endDate);
    }

    private PositionHistory mapToEntity(HistoryLoadRequest request) {
        return PositionHistory.builder()
                .portfolioId(request.getPortfolioId())
                .accountNo(request.getAccountNo())
                .historyDate(request.getTransDate() != null ? request.getTransDate() : LocalDate.now())
                .historyTime(request.getTransTime() != null ? request.getTransTime() : LocalTime.now())
                .recordType(request.getRecordType() != null ? request.getRecordType() : HistoryRecordType.TRANSACTION)
                .actionCode(request.getActionCode() != null ? request.getActionCode() : ActionCode.ADD)
                .beforeImage(request.getBeforeImage())
                .afterImage(buildAfterImage(request))
                .reasonCode(request.getReasonCode())
                .processDate(LocalDateTime.now())
                .processUser(request.getProcessUser())
                .securityId(request.getSecurityId())
                .build();
    }

    private String buildAfterImage(HistoryLoadRequest request) {
        if (request.getAfterImage() != null) {
            return request.getAfterImage();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Type=").append(request.getTransType());
        sb.append(",Security=").append(request.getSecurityId());
        sb.append(",Qty=").append(request.getQuantity());
        sb.append(",Price=").append(request.getPrice());
        sb.append(",Amount=").append(request.getAmount());
        sb.append(",Fees=").append(request.getFees());
        sb.append(",Total=").append(request.getTotalAmount());
        sb.append(",CostBasis=").append(request.getCostBasis());
        sb.append(",GainLoss=").append(request.getGainLoss());
        return sb.toString();
    }

    private boolean isDuplicateKeyError(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("Duplicate") || 
                message.contains("duplicate") ||
                message.contains("SQLCODE=-803") ||
                message.contains("unique constraint"));
    }
}
