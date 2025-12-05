package com.portfolio.repository;

import com.portfolio.entity.CheckpointRecord;
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
 * Repository for Checkpoint Record entity
 * Provides data access methods for batch checkpoint/restart support
 */
@Repository
public interface CheckpointRecordRepository extends JpaRepository<CheckpointRecord, UUID> {

    /**
     * Find checkpoint by composite key
     */
    Optional<CheckpointRecord> findByProcessDateAndProcessId(LocalDate processDate, String processId);

    /**
     * Find checkpoint by composite key with pessimistic lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CheckpointRecord c WHERE c.processDate = :processDate AND c.processId = :processId")
    Optional<CheckpointRecord> findByKeyWithLock(
            @Param("processDate") LocalDate processDate,
            @Param("processId") String processId);

    /**
     * Find checkpoints by process date
     */
    List<CheckpointRecord> findByProcessDate(LocalDate processDate);

    /**
     * Find checkpoints by process ID
     */
    List<CheckpointRecord> findByProcessId(String processId);

    /**
     * Find latest checkpoint by process ID
     */
    @Query("SELECT c FROM CheckpointRecord c WHERE c.processId = :processId " +
            "ORDER BY c.checkpointTimestamp DESC LIMIT 1")
    Optional<CheckpointRecord> findLatestByProcessId(@Param("processId") String processId);

    /**
     * Check if checkpoint exists
     */
    boolean existsByProcessDateAndProcessId(LocalDate processDate, String processId);

    /**
     * Delete checkpoints older than specified date
     */
    void deleteByProcessDateBefore(LocalDate retentionDate);

    /**
     * Get total records processed for a date
     */
    @Query("SELECT SUM(c.recordsProcessed) FROM CheckpointRecord c WHERE c.processDate = :processDate")
    Long getTotalRecordsProcessed(@Param("processDate") LocalDate processDate);
}
