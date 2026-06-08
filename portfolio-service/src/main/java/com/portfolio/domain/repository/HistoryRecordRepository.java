package com.portfolio.domain.repository;

import com.portfolio.domain.model.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {

    List<HistoryRecord> findByPortfolioIdOrderByProcessDateDesc(String portfolioId);
}
