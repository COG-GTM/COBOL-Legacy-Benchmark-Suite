package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.CheckpointControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckpointControlRepository extends JpaRepository<CheckpointControl, Long> {
}
