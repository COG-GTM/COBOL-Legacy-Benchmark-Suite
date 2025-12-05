package com.portfolio.modernization.service;

import com.portfolio.modernization.model.entity.Transaction;
import com.portfolio.modernization.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public List<Transaction> findByPortfolioId(String portfolioId) {
        return transactionRepository.findByPortfolioIdOrderByDateDesc(portfolioId);
    }

    public List<Transaction> findByPortfolioIdAndDateRange(String portfolioId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByPortfolioIdAndTransactionDateBetween(portfolioId, startDate, endDate);
    }

    public List<Transaction> findByStatus(Transaction.TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    @Transactional
    public Transaction save(Transaction transaction) {
        log.info("Saving transaction: {}", transaction.getTransactionId());
        return transactionRepository.save(transaction);
    }

    @Transactional
    public List<Transaction> saveAll(List<Transaction> transactions) {
        log.info("Saving {} transactions", transactions.size());
        return transactionRepository.saveAll(transactions);
    }
}
