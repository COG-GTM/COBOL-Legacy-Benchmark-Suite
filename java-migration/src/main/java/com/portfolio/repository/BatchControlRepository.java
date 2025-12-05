package com.portfolio.repository;

import com.portfolio.entity.BatchControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Batch Control entity
 * Provides data access methods for batch process control
 */
@Repository
public interface BatchControlRepository extends JpaRepository<BatchControl, UUID> {

    /**
     * Find batch control by composite key
     */
    Optional<BatchControl> findByProcessDateAndProcessId(LocalDate processDate, String processId);

    /**
     * Find batch control by composite key with pessimistic lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BatchControl b WHERE b.processDate = :processDate AND b.processId = :processId")
    Optional<BatchControl> findByKeyWithLock(
            @Param("processDate") LocalDate processDate,
            @Param("processId") String processId);

    /**
     * Find batch controls by process date
     */
    List<BatchControl> findByProcessDate(LocalDate processDate);

    /**
     * Find batch controls by process ID
     */
    List<BatchControl> findByProcessId(String processId);

    /**
     * Find batch controls by status
     */
    List<BatchControl> findByStatus(String status);

    /**
     * Find batch controls by process date and status
     */
    List<BatchControl> findByProcessDateAndStatus(LocalDate processDate, String status);

    /**
     * Find waiting batch controls
     */
    @Query("SELECT b FROM BatchControl b WHERE b.status = 'W' ORDER BY b.processDate, b.processId")
    List<BatchControl> findWaitingBatches();

    /**
     * Find in-progress batch controls
     */
    @Query("SELECT b FROM BatchControl b WHERE b.status = 'P' ORDER BY b.startTime")
    List<BatchControl> findInProgressBatches();

    /**
     * Find completed batch controls for a date
     */
    @Query("SELECT b FROM BatchControl b WHERE b.status = 'C' AND b.processDate = :processDate")
    List<BatchControl> findCompletedBatches(@Param("processDate") LocalDate processDate);

    /**
     * Find error batch controls for a date
     */
    @Query("SELECT b FROM BatchControl b WHERE b.status = 'E' AND b.processDate = :processDate")
    List<BatchControl> findErrorBatches(@Param("processDate") LocalDate processDate);

    /**
     * Check if batch control exists
     */
    boolean existsByProcessDateAndProcessId(LocalDate processDate, String processId);

    /**
     * Count batch controls by status
     */
    long countByStatus(String status);

    /**
     * Count batch controls by process date and status
     */
    long countByProcessDateAndStatus(LocalDate processDate, String status);

    /**
     * Get total record count for a process date
     */
    @Query("SELECT SUM(b.recordCount) FROM BatchControl b WHERE b.processDate = :processDate AND b.status = 'C'")
    Long getTotalRecordCount(@Param("processDate") LocalDate processDate);

    /**
     * Get total error count for a process date
     */
    @Query("SELECT SUM(b.errorCount) FROM BatchControl b WHERE b.processDate = :processDate")
    Long getTotalErrorCount(@Param("processDate") LocalDate processDate);
}
