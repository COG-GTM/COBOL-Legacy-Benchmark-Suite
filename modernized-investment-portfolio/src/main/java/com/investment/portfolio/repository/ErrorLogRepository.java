package com.investment.portfolio.repository;

import com.investment.portfolio.entity.ErrorLog;
import com.investment.portfolio.entity.ErrorLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for the ErrorLog entity.
 *
 * Provides access patterns matching the original DB2 ERRLOG table
 * index-based queries.
 */
@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLogId> {

    /**
     * Find errors by process date, ordered by severity descending.
     * Replaces DB2 ERRLOG_IX1 index access pattern.
     */
    List<ErrorLog> findByProcessDateOrderByErrorSeverityDesc(LocalDate processDate);

    /**
     * Find errors by error type.
     * Supports filtering by S=System, A=Application, D=Data.
     */
    List<ErrorLog> findByErrorType(String errorType);

    /**
     * Find errors by severity level.
     */
    List<ErrorLog> findByErrorSeverity(Integer errorSeverity);

    /**
     * Find errors by program ID.
     */
    List<ErrorLog> findByIdProgramId(String programId);
}
