package com.portfolio.domain.repository;

import com.portfolio.domain.model.HistoryRecord;
import com.portfolio.domain.model.HistoryRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, HistoryRecordId> {

    List<HistoryRecord> findByIdPortfolioId(String portfolioId);
}
