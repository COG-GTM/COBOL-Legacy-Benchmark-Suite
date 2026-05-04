package com.portfolio.repository;

import com.portfolio.domain.BatchControlId;
import com.portfolio.domain.BatchControlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Batch Control repository - replaces VSAM batch control file access.
 */
@Repository
public interface BatchControlRepository extends JpaRepository<BatchControlRecord, BatchControlId> {

    List<BatchControlRecord> findByJobName(String jobName);

    List<BatchControlRecord> findByProcessDate(LocalDate processDate);

    List<BatchControlRecord> findByStatus(String status);

    List<BatchControlRecord> findByJobNameAndProcessDate(String jobName, LocalDate processDate);
}
