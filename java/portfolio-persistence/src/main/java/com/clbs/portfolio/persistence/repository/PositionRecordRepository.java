package com.clbs.portfolio.persistence.repository;

import com.clbs.portfolio.domain.PositionRecord;
import com.clbs.portfolio.domain.PositionRecordKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PositionRecordRepository extends JpaRepository<PositionRecord, PositionRecordKey> {
    List<PositionRecord> findByKeyPortfolioIdAndKeyPositionDate(String portfolioId, LocalDate positionDate);
}
