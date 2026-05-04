package com.portfolio.repository;

import com.portfolio.entity.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<HistoryRecord, Long> {

    List<HistoryRecord> findByPortfolioIdOrderByProcessDateDesc(String portfolioId);

    List<HistoryRecord> findByRecordType(String recordType);

    List<HistoryRecord> findByActionCode(String actionCode);

    List<HistoryRecord> findByPortfolioIdAndRecordType(String portfolioId, String recordType);
}
