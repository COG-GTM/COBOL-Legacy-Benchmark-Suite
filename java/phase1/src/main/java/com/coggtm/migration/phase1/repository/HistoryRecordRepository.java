package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, HistoryRecord.HistoryRecordId> {
}
