package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.ErrorLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Error Log records.
 * Replaces COBOL CALL 'ERRPROC' error logging and DB2 ERRLOG table operations.
 */
@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByProgramId(String programId);

    List<ErrorLog> findByProcessDate(LocalDate processDate);

    void deleteByProcessDateBefore(LocalDate date);
}
