package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.HistoryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {

    List<HistoryRecord> findByStatus(String status);

    Page<HistoryRecord> findByStatus(String status, Pageable pageable);
}
