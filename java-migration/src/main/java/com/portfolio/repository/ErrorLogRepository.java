package com.portfolio.repository;

import com.portfolio.domain.ErrorLogId;
import com.portfolio.domain.ErrorLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Error Log repository - replaces DB2 ERRLOG table access.
 */
@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLogRecord, ErrorLogId> {

    List<ErrorLogRecord> findByProgramId(String programId);

    List<ErrorLogRecord> findByProcessDateAndErrorSeverityGreaterThanEqual(
            LocalDate processDate, int severity);

    List<ErrorLogRecord> findByErrorType(String errorType);

    void deleteByProcessDateBefore(LocalDate retentionDate);
}
