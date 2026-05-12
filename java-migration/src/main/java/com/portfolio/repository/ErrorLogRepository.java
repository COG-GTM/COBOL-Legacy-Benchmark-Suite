package com.portfolio.repository;

import com.portfolio.model.entity.ErrorLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLogEntry, Long> {

    List<ErrorLogEntry> findByProcessDateAndErrorSeverityGreaterThanEqual(
            LocalDate processDate, Integer severity);

    @Modifying
    @Query("DELETE FROM ErrorLogEntry e WHERE e.processDate < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}
