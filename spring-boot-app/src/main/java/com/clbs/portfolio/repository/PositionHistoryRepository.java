package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {
}
