package com.portfolio.domain.repository;

import com.portfolio.domain.model.ErrorLog;
import com.portfolio.domain.model.ErrorLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLogId> {

    @Query("SELECT e FROM ErrorLog e WHERE e.processDate = :processDate AND e.errorSeverity >= :severity")
    List<ErrorLog> findByProcessDateAndErrorSeverityGreaterThanEqual(
            @Param("processDate") LocalDate processDate,
            @Param("severity") int severity);

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.processDate < :retentionDate")
    void deleteByProcessDateBefore(@Param("retentionDate") LocalDate retentionDate);
}
