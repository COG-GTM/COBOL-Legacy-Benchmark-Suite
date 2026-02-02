package com.portfolio.repository;

import com.portfolio.domain.BatchControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BatchControl entity
 * Provides data access operations for batch job control
 */
@Repository
public interface BatchControlRepository extends JpaRepository<BatchControl, Long> {

    Optional<BatchControl> findByJobNameAndProcessDate(String jobName, LocalDate processDate);

    List<BatchControl> findByJobName(String jobName);

    List<BatchControl> findByStatus(BatchControl.BatchStatus status);

    List<BatchControl> findByProcessDate(LocalDate processDate);

    @Query("SELECT b FROM BatchControl b WHERE b.jobName = :jobName " +
           "ORDER BY b.processDate DESC, b.sequenceNo DESC")
    List<BatchControl> findJobHistory(@Param("jobName") String jobName);

    @Query("SELECT b FROM BatchControl b WHERE b.status IN ('R', 'W') " +
           "ORDER BY b.processDate, b.sequenceNo")
    List<BatchControl> findPendingJobs();

    @Query("SELECT b FROM BatchControl b WHERE b.status = 'E' ORDER BY b.processDate DESC")
    List<BatchControl> findFailedJobs();

    @Query("SELECT b FROM BatchControl b WHERE b.processDate = :date AND b.status = 'D' " +
           "ORDER BY b.completeTimestamp")
    List<BatchControl> findCompletedJobsForDate(@Param("date") LocalDate date);
}
