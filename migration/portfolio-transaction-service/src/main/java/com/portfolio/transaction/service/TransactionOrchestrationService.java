package com.portfolio.transaction.service;

import com.portfolio.transaction.audit.AuditService;
import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResponse;
import com.portfolio.transaction.domain.dto.TransactionResult;
import com.portfolio.transaction.domain.dto.ValidationResult;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.entity.Transaction;
import com.portfolio.transaction.domain.enums.TransactionType;
import com.portfolio.transaction.exception.PortfolioNotFoundException;
import com.portfolio.transaction.exception.TransactionException;
import com.portfolio.transaction.repository.PortfolioRepository;
import com.portfolio.transaction.repository.TransactionRepository;
import com.portfolio.transaction.service.processor.TransactionProcessor;
import com.portfolio.transaction.service.processor.TransactionProcessorFactory;
import com.portfolio.transaction.service.validation.ValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
public class TransactionOrchestrationService {

    private final ValidationService validationService;
    private final TransactionProcessorFactory processorFactory;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    public TransactionOrchestrationService(ValidationService validationService,
                                           TransactionProcessorFactory processorFactory,
                                           PortfolioRepository portfolioRepository,
                                           TransactionRepository transactionRepository,
                                           AuditService auditService) {
        this.validationService = validationService;
        this.processorFactory = processorFactory;
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    public TransactionResponse processTransaction(TransactionRequest request) {
        Transaction transaction = createTransactionRecord(request);

        try {
            ValidationResult validationResult = validationService.validateTransaction(request);

            if (!validationResult.isValid()) {
                return handleValidationFailure(transaction, validationResult);
            }

            Portfolio portfolio = portfolioRepository
                .findByIdWithLock(request.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(request.getPortfolioId()));

            Portfolio beforeImage = portfolio.clone();

            TransactionType type = TransactionType.fromCode(request.getTransactionType());
            TransactionProcessor processor = processorFactory.getProcessor(type);
            TransactionResult result = processor.process(request, portfolio);

            auditService.recordTransaction(request, beforeImage, portfolio);

            transaction.setStatus("PROCESSED");
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            return TransactionResponse.success(transaction, portfolio);

        } catch (TransactionException ex) {
            return handleProcessingFailure(transaction, ex);
        }
    }

    private Transaction createTransactionRecord(TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setPortfolioId(request.getPortfolioId());
        transaction.setTransactionType(TransactionType.fromCode(request.getTransactionType()));
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(request.getPrice());
        transaction.setAmount(request.getAmount());
        transaction.setStatus("PENDING");
        return transactionRepository.save(transaction);
    }

    private TransactionResponse handleValidationFailure(Transaction transaction, 
                                                        ValidationResult result) {
        transaction.setStatus("REJECTED");
        transaction.setErrorMessage(result.getErrorMessage());
        transactionRepository.save(transaction);

        return TransactionResponse.failure(transaction, result.getErrorMessage());
    }

    private TransactionResponse handleProcessingFailure(Transaction transaction, 
                                                        TransactionException ex) {
        transaction.setStatus("REJECTED");
        transaction.setErrorMessage(ex.getMessage());
        transactionRepository.save(transaction);

        return TransactionResponse.failure(transaction, ex.getMessage());
    }
}
