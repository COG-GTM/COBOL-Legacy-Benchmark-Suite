package com.portfolio.service.online;

import com.portfolio.domain.Transaction;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * History Inquiry Service - migrated from COBOL INQHIST.cbl.
 * DB2 history queries via JPA.
 * DB2 connection/recovery pattern (EXEC CICS LINK PROGRAM('DB2ONLN') with
 * fallback to DB2RECV) -> Spring's connection pool with retry.
 */
@Service
public class HistoryInquiryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryInquiryService.class);
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final TransactionRepository transactionRepository;

    public HistoryInquiryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(String portfolioId, int page, int size) {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            throw new ValidationException("Portfolio ID is required for history inquiry");
        }

        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }

        log.debug("Querying transaction history for portfolio: {}, page: {}, size: {}",
                portfolioId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(
                portfolioId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(String portfolioId, int page) {
        return getTransactionHistory(portfolioId, page, DEFAULT_PAGE_SIZE);
    }
}
