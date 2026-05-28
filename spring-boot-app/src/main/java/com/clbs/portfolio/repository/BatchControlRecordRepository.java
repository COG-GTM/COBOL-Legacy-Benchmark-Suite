package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.BatchControlRecord;
import com.clbs.portfolio.model.BatchControlRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for BatchControlRecord entities.
 * Replaces VSAM access patterns for BCHCTL file.
 */
@Repository
public interface BatchControlRecordRepository extends JpaRepository<BatchControlRecord, BatchControlRecordId> {

    List<BatchControlRecord> findByJobNameAndProcessDate(String jobName, String processDate);

    List<BatchControlRecord> findByStatus(BatchControlRecord.BatchControlStatus status);
}
