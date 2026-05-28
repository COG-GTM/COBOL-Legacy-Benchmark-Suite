package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.TransactionRecord;
import com.clbs.portfolio.model.TransactionRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TransactionRecord entities.
 * Replaces VSAM KSDS access patterns for TRNREC file.
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, TransactionRecordId> {

    List<TransactionRecord> findByPortfolioIdAndTrnDateBetween(String portfolioId, String startDate, String endDate);
}
