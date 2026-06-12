package com.benchmark.portfolio.common.repository;

import com.benchmark.portfolio.common.entity.ErrorLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link ErrorLog}, replacing the DB2 ERRLOG table written by
 * the centralized error handler (ERRHAND.cpy).
 *
 * <p>{@code save} replicates ERRHNDL.cbl P200-LOG-ERROR
 * ({@code EXEC SQL INSERT INTO ERRLOG}); the table is insert-only.
 */
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    /**
     * Errors logged by one program, newest first - the review query over the
     * ERRLOG rows inserted by ERRHNDL.cbl P200-LOG-ERROR (keyed by
     * ERR-PROGRAM).
     */
    List<ErrorLog> findByProgramIdOrderByErrorDateDescErrorTimeDesc(String programId);

    /**
     * Errors logged on one processing date in time order - daily error report
     * over rows inserted by ERRHNDL.cbl P200-LOG-ERROR (keyed by ERR-DATE).
     */
    List<ErrorLog> findByErrorDateOrderByErrorTimeAsc(LocalDate errorDate);
}
