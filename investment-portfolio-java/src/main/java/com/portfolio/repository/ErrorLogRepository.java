package com.portfolio.repository;

import com.portfolio.entity.ErrorLog;
import com.portfolio.entity.ErrorLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLogId> {

    List<ErrorLog> findByProcessDateAndErrorSeverityGreaterThanEqual(
            LocalDate processDate, int severity);

    List<ErrorLog> findByProcessDateBetween(LocalDate startDate, LocalDate endDate);

    List<ErrorLog> findByProgramId(String programId);

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.processDate < :retentionDate")
    int deleteByProcessDateBefore(@Param("retentionDate") LocalDate retentionDate);
}
