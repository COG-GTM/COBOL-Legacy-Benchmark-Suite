package com.portfolio.repository;

import com.portfolio.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository extends JpaRepository<HistoryRecord, HistoryKey> {}
