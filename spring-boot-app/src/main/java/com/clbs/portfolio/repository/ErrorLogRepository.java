package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for ErrorLog entities.
 * Replaces DB2 ERRLOG table access patterns.
 */
@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByProgramIdAndProcessDate(String programId, LocalDate processDate);
}
