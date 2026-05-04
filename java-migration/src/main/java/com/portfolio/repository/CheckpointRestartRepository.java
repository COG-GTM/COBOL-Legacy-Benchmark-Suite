package com.portfolio.repository;

import com.portfolio.entity.CheckpointRestart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CheckpointRestartRepository extends JpaRepository<CheckpointRestart, Long> {

    Optional<CheckpointRestart> findByProgramIdAndRunDate(String programId, String runDate);

    Optional<CheckpointRestart> findTopByProgramIdOrderByLastCheckpointTimeDesc(String programId);
}
