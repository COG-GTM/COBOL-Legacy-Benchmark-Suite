package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.CheckpointControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for CheckpointControl entities.
 * Replaces VSAM access patterns for CKPRST checkpoint file.
 */
@Repository
public interface CheckpointControlRepository extends JpaRepository<CheckpointControl, Long> {

    Optional<CheckpointControl> findByProgramIdAndRunDate(String programId, String runDate);
}
