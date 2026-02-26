package com.portfolio.support;

import com.portfolio.model.ErrorRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Error Log table.
 * Replaces COBOL ERRPROC sequential error log file access.
 */
@Repository
public interface ErrorRecordRepository extends JpaRepository<ErrorRecord, Long> {

    List<ErrorRecord> findByProcessDate(LocalDate processDate);

    List<ErrorRecord> findByProgramId(String programId);

    List<ErrorRecord> findByErrorSeverityGreaterThanEqual(int severity);

    long countByProcessDate(LocalDate processDate);

    long countByErrorSeverity(int severity);
}
