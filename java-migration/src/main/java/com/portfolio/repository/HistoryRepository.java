package com.portfolio.repository;

import com.portfolio.domain.HistoryKey;
import com.portfolio.domain.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository extends JpaRepository<HistoryRecord, HistoryKey> {}
