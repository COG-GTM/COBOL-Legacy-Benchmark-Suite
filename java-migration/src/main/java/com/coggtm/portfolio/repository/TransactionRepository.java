package com.coggtm.portfolio.repository;

import com.coggtm.portfolio.domain.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {

    List<TransactionRecord> findByPortfolioId(String portfolioId);

    List<TransactionRecord> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);
}
