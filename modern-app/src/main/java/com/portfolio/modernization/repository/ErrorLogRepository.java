package com.portfolio.modernization.repository;

import com.portfolio.modernization.model.entity.ErrorLog;
import com.portfolio.modernization.model.entity.ErrorLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLogId> {

    List<ErrorLog> findByProgramId(String programId);

    List<ErrorLog> findByErrorSeverity(Integer errorSeverity);

    @Query("SELECT e FROM ErrorLog e WHERE e.processDate BETWEEN :startDate AND :endDate ORDER BY e.errorSeverity DESC, e.errorTimestamp DESC")
    List<ErrorLog> findByProcessDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.processDate < :retentionDate")
    int deleteOlderThan(@Param("retentionDate") LocalDate retentionDate);
}
