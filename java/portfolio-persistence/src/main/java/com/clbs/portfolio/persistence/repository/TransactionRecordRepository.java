package com.clbs.portfolio.persistence.repository;

import com.clbs.portfolio.domain.TransactionRecord;
import com.clbs.portfolio.domain.TransactionRecordKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, TransactionRecordKey> {
    List<TransactionRecord> findByKeyPortfolioIdOrderByKeyTransactionDateDesc(String portfolioId);
}
