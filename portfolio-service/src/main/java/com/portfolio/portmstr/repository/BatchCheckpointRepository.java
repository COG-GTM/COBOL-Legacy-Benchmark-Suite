package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.BatchCheckpoint;
import com.portfolio.portmstr.model.BatchCheckpointId;
import com.portfolio.portmstr.model.enums.CheckpointStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Batch Checkpoint records.
 * Replaces COBOL CKPRST checkpoint/restart VSAM file operations.
 */
@Repository
public interface BatchCheckpointRepository extends JpaRepository<BatchCheckpoint, BatchCheckpointId> {

    Optional<BatchCheckpoint> findByProgramIdAndRunDate(String programId, LocalDate runDate);

    List<BatchCheckpoint> findByProgramIdAndStatus(String programId, CheckpointStatus status);

    Optional<BatchCheckpoint> findTopByProgramIdOrderByRunDateDesc(String programId);
}
