package com.portfolio.service;

import com.portfolio.dto.TransactionResponse;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.exception.ResourceNotFoundException;
import com.portfolio.repository.PortfolioMasterRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/**
 * Transaction history service - replaces INQHIST.cbl history inquiry logic.
 * Source: src/programs/online/INQHIST.cbl
 *
 * Key replacement:
 * - HISTORY_CURSOR with array fetch (3000 bytes) → Spring Data Pageable
 * - P200-GET-HISTORY: SQL cursor open/fetch/close → single JPA query with pagination
 * - P250-FETCH-HISTORY: Cursor fetch loop → Pageable results
 */
@Service
@Transactional(readOnly = true)
public class TransactionHistoryService {

    private final TransactionHistoryRepository transactionRepository;
    private final PortfolioMasterRepository portfolioRepository;

    public TransactionHistoryService(TransactionHistoryRepository transactionRepository,
                                     PortfolioMasterRepository portfolioRepository) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * Get transaction history for a portfolio with pagination.
     * Replaces INQHIST P200-GET-HISTORY cursor-based array fetch.
     */
    public Page<TransactionResponse> getHistoryByPortfolioId(String portfolioId, Pageable pageable) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio", "portfolioId", portfolioId);
        }

        Page<TransactionHistory> page = transactionRepository
                .findByPortfolioIdOrderByTransactionDateDesc(portfolioId, pageable);

        return new PageImpl<>(
                page.getContent().stream()
                        .map(TransactionResponse::fromEntity)
                        .toList(),
                pageable,
                page.getTotalElements()
        );
    }

    /**
     * Get transaction history for a portfolio within a date range.
     */
    public Page<TransactionResponse> getHistoryByDateRange(
            String portfolioId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio", "portfolioId", portfolioId);
        }

        Page<TransactionHistory> page = transactionRepository
                .findByPortfolioIdAndDateRange(portfolioId, startDate, endDate, pageable);

        return new PageImpl<>(
                page.getContent().stream()
                        .map(TransactionResponse::fromEntity)
                        .toList(),
                pageable,
                page.getTotalElements()
        );
    }
}
