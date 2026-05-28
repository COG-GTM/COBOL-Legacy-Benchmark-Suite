package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    List<TransactionRecord> findByStatus(String status);

    Page<TransactionRecord> findByStatus(String status, Pageable pageable);

    List<TransactionRecord> findByAdjudicationStatus(String adjudicationStatus);

    Page<TransactionRecord> findByAdjudicationStatus(String adjudicationStatus, Pageable pageable);

    List<TransactionRecord> findByPortfolioIdAndInvestmentIdAndTrnDateAndTrnTypeAndAmount(
            String portfolioId, String investmentId, String trnDate,
            com.clbs.portfolio.enums.TransactionType trnType, java.math.BigDecimal amount);
}
