package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.CheckpointControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CheckpointControlRepository extends JpaRepository<CheckpointControl, Long> {

    @Query("SELECT c FROM CheckpointControl c WHERE c.lastTime < :cutoff")
    List<CheckpointControl> findExpired(@Param("cutoff") LocalDateTime cutoff);
}
