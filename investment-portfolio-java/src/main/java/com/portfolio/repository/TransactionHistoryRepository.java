package com.portfolio.repository;

import com.portfolio.entity.TransactionHistory;
import com.portfolio.entity.TransactionStatus;
import com.portfolio.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {

    List<TransactionHistory> findByPortfolioId(String portfolioId);

    Page<TransactionHistory> findByPortfolioIdOrderByTransactionDateDescTransactionTimeDesc(
            String portfolioId, Pageable pageable);

    List<TransactionHistory> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    List<TransactionHistory> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<TransactionHistory> findByPortfolioIdAndTransactionType(
            String portfolioId, TransactionType transactionType);

    List<TransactionHistory> findByStatus(TransactionStatus status);

    @Query("SELECT t FROM TransactionHistory t " +
            "JOIN PortfolioMaster pm ON t.portfolioId = pm.portfolioId " +
            "WHERE pm.accountNo = :accountNo " +
            "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    Page<TransactionHistory> findByAccountNo(
            @Param("accountNo") String accountNo, Pageable pageable);
}
