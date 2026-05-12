package com.portfolio.repository;

import com.portfolio.model.entity.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {

    List<HistoryRecord> findByPortfolioIdOrderByHistoryDateDescHistoryTimeDesc(String portfolioId);
}
