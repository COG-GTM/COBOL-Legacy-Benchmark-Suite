package com.portfolio.domain.repository;

import com.portfolio.domain.model.PositionHistory;
import com.portfolio.domain.model.PositionHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, PositionHistoryId> {

    List<PositionHistory> findBySecurityIdAndIdTransDate(String securityId, LocalDate transDate);

    List<PositionHistory> findByProcessDateAndProgramId(LocalDate processDate, String programId);
}
