package com.clbs.portfolio.persistence.repository;

import com.clbs.portfolio.domain.HistoryRecord;
import com.clbs.portfolio.domain.HistoryRecordKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, HistoryRecordKey> {
    List<HistoryRecord> findByKeyPortfolioId(String portfolioId);
}
