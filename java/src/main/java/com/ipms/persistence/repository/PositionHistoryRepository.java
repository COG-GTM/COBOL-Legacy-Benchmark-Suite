package com.ipms.persistence.repository;

import com.ipms.persistence.entity.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PositionHistoryRepository
        extends JpaRepository<PositionHistory, PositionHistory.Key> {

    List<PositionHistory> findBySecurityIdAndTransDate(String securityId, LocalDate transDate);
}
