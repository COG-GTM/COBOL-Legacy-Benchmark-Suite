package com.ipms.persistence.repository;

import com.ipms.persistence.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {

    List<TransactionHistory> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate from, LocalDate to);
}
