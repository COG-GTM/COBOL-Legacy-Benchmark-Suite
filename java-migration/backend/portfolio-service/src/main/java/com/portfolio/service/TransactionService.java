package com.portfolio.service;

import com.portfolio.dto.BatchProcessingResult;
import com.portfolio.dto.TransactionRequest;
import com.portfolio.dto.TransactionResponse;
import com.portfolio.exception.InsufficientUnitsException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.exception.TransactionValidationException;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Transaction;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.model.enums.TransactionStatus;
import com.portfolio.model.enums.TransactionType;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;

    private static final int MAX_ERRORS = 100;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction for portfolio: {}", request.getPortfolioId());

        validateTransaction(request);

        Portfolio portfolio = portfolioRepository.findByPortfolioId(request.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(request.getPortfolioId(), true));

        String beforeImage = buildPortfolioImage(portfolio);

        Transaction transaction = createTransaction(request);

        try {
            updatePortfolioPositions(portfolio, request);
            
            transaction.setStatus(TransactionStatus.DONE);
            transaction.setProcessDate(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            portfolio.setLastTransDate(LocalDate.now());
            portfolio.setLastUser(request.getUserId());
            portfolioRepository.save(portfolio);

            auditService.createTransactionAudit(transaction, portfolio, beforeImage, 
                    AuditStatus.SUCCESS, "Transaction processed successfully");

            log.info("Transaction processed successfully for portfolio: {}", request.getPortfolioId());
            return mapToResponse(transaction, "Transaction processed successfully");

        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setProcessDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            auditService.createTransactionAudit(transaction, portfolio, beforeImage, 
                    AuditStatus.FAILURE, e.getMessage());

            throw e;
        }
    }

    @Transactional
    public BatchProcessingResult processBatch(List<TransactionRequest> requests) {
        log.info("Processing batch of {} transactions", requests.size());

        BatchProcessingResult result = BatchProcessingResult.builder()
                .recordsRead(0)
                .recordsProcessed(0)
                .errorCount(0)
                .status("IN_PROGRESS")
                .build();

        for (TransactionRequest request : requests) {
            result.incrementRead();

            if (result.getErrorCount() > MAX_ERRORS) {
                log.warn("Maximum error count ({}) exceeded, stopping batch processing", MAX_ERRORS);
                result.setStatus("STOPPED_MAX_ERRORS");
                result.setMessage("Processing stopped: maximum error count exceeded");
                break;
            }

            try {
                TransactionResponse response = processTransaction(request);
                result.addProcessedTransaction(response);
            } catch (Exception e) {
                log.error("Error processing transaction for portfolio {}: {}", 
                        request.getPortfolioId(), e.getMessage());
                result.addError(String.format("Portfolio %s: %s", 
                        request.getPortfolioId(), e.getMessage()));
            }
        }

        if (result.getErrorCount() <= MAX_ERRORS) {
            result.setStatus("COMPLETED");
            result.setMessage(String.format("Batch processing completed. Read: %d, Processed: %d, Errors: %d",
                    result.getRecordsRead(), result.getRecordsProcessed(), result.getErrorCount()));
        }

        log.info("Batch processing completed. Read: {}, Processed: {}, Errors: {}",
                result.getRecordsRead(), result.getRecordsProcessed(), result.getErrorCount());

        return result;
    }

    public List<TransactionResponse> getTransactionsByPortfolioId(String portfolioId) {
        return transactionRepository.findByPortfolioId(portfolioId).stream()
                .map(t -> mapToResponse(t, null))
                .toList();
    }

    public List<TransactionResponse> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate).stream()
                .map(t -> mapToResponse(t, null))
                .toList();
    }

    private void validateTransaction(TransactionRequest request) {
        if (request.getPortfolioId() == null || request.getPortfolioId().isBlank()) {
            throw new TransactionValidationException("Portfolio ID is required");
        }

        if (!portfolioRepository.existsByPortfolioId(request.getPortfolioId())) {
            throw new PortfolioNotFoundException("Invalid Portfolio ID: " + request.getPortfolioId());
        }

        if (request.getType() == null) {
            throw new TransactionValidationException("Transaction type is required");
        }

        TransactionType type = request.getType();
        if (type != TransactionType.BUY && type != TransactionType.SELL && 
            type != TransactionType.TRANSFER && type != TransactionType.FEE) {
            throw new TransactionValidationException("Invalid Transaction Type: " + type);
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionValidationException("Quantity must be greater than zero");
        }

        if (type != TransactionType.TRANSFER) {
            if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionValidationException("Price must be greater than zero");
            }
            if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionValidationException("Amount must be greater than zero");
            }
        }
    }

    private void updatePortfolioPositions(Portfolio portfolio, TransactionRequest request) {
        switch (request.getType()) {
            case BUY -> processBuy(portfolio, request);
            case SELL -> processSell(portfolio, request);
            case FEE -> processFee(portfolio, request);
            case TRANSFER -> throw new TransactionValidationException("Transfer processing not implemented");
        }
    }

    private void processBuy(Portfolio portfolio, TransactionRequest request) {
        log.debug("Processing BUY for portfolio: {}", portfolio.getPortfolioId());
        
        BigDecimal currentUnits = portfolio.getTotalUnits() != null ? portfolio.getTotalUnits() : BigDecimal.ZERO;
        BigDecimal currentCost = portfolio.getTotalCost() != null ? portfolio.getTotalCost() : BigDecimal.ZERO;
        
        portfolio.setTotalUnits(currentUnits.add(request.getQuantity()));
        
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : 
                request.getQuantity().multiply(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        portfolio.setTotalCost(currentCost.add(amount));
    }

    private void processSell(Portfolio portfolio, TransactionRequest request) {
        log.debug("Processing SELL for portfolio: {}", portfolio.getPortfolioId());
        
        BigDecimal currentUnits = portfolio.getTotalUnits() != null ? portfolio.getTotalUnits() : BigDecimal.ZERO;
        BigDecimal currentCost = portfolio.getTotalCost() != null ? portfolio.getTotalCost() : BigDecimal.ZERO;
        
        if (currentUnits.compareTo(request.getQuantity()) < 0) {
            throw new InsufficientUnitsException(portfolio.getPortfolioId(), request.getQuantity(), currentUnits);
        }
        
        portfolio.setTotalUnits(currentUnits.subtract(request.getQuantity()));
        
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : 
                request.getQuantity().multiply(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        portfolio.setTotalCost(currentCost.subtract(amount));
    }

    private void processFee(Portfolio portfolio, TransactionRequest request) {
        log.debug("Processing FEE for portfolio: {}", portfolio.getPortfolioId());
        
        BigDecimal currentCost = portfolio.getTotalCost() != null ? portfolio.getTotalCost() : BigDecimal.ZERO;
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        
        portfolio.setTotalCost(currentCost.subtract(amount));
    }

    private Transaction createTransaction(TransactionRequest request) {
        return Transaction.builder()
                .transactionDate(LocalDate.now())
                .transactionTime(LocalTime.now())
                .portfolioId(request.getPortfolioId())
                .investmentId(request.getInvestmentId())
                .type(request.getType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(TransactionStatus.PENDING)
                .processUser(request.getUserId())
                .build();
    }

    private String buildPortfolioImage(Portfolio portfolio) {
        return String.format("ID=%s,Units=%s,Cost=%s,Value=%s",
                portfolio.getPortfolioId(),
                portfolio.getTotalUnits(),
                portfolio.getTotalCost(),
                portfolio.getTotalValue());
    }

    private TransactionResponse mapToResponse(Transaction transaction, String message) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionDate(transaction.getTransactionDate())
                .transactionTime(transaction.getTransactionTime())
                .portfolioId(transaction.getPortfolioId())
                .sequenceNo(transaction.getSequenceNo())
                .investmentId(transaction.getInvestmentId())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .processDate(transaction.getProcessDate())
                .processUser(transaction.getProcessUser())
                .message(message)
                .build();
    }
}
