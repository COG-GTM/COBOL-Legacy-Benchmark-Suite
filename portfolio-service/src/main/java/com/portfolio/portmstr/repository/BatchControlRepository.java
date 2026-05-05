package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.BatchControlRecord;
import com.portfolio.portmstr.model.enums.BatchControlStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Batch Control records.
 * Replaces COBOL BCHCTL00 batch control VSAM file operations.
 */
@Repository
public interface BatchControlRepository extends JpaRepository<BatchControlRecord, Long> {

    Optional<BatchControlRecord> findByJobNameAndProcessDateAndSequenceNo(
            String jobName, LocalDate processDate, Integer sequenceNo);

    List<BatchControlRecord> findByJobNameAndProcessDate(String jobName, LocalDate processDate);

    List<BatchControlRecord> findByStatus(BatchControlStatus status);
}
