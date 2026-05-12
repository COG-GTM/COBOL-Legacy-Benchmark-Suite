package com.portfolio.repository;

import com.portfolio.model.entity.PositionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    Page<PositionHistory> findByAccountNoOrderByTransDateDescTransTimeDesc(
            String accountNo, Pageable pageable);

    List<PositionHistory> findBySecurityIdAndTransDateBetween(
            String securityId, LocalDate startDate, LocalDate endDate);

    List<PositionHistory> findByProcessDateAndProgramId(
            LocalDate processDate, String programId);
}
