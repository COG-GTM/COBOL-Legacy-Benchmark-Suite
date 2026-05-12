package com.portfolio.repository;

import com.portfolio.model.entity.CheckpointControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckpointControlRepository extends JpaRepository<CheckpointControl, Long> {

    Optional<CheckpointControl> findByProgramIdAndRunDate(String programId, String runDate);

    Optional<CheckpointControl> findTopByProgramIdOrderByLastTimeDesc(String programId);
}
