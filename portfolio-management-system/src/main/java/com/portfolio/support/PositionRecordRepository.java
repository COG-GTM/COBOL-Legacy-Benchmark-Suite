package com.portfolio.support;

import com.portfolio.model.PositionRecord;
import com.portfolio.model.PositionRecordKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Position Master table (replaces VSAM POSFILE).
 * Migrated from CICS FILE(POSFILE) definition in PORTDFN.csd lines 69-79.
 */
@Repository
public interface PositionRecordRepository extends JpaRepository<PositionRecord, PositionRecordKey> {

    List<PositionRecord> findByPortfolioId(String portfolioId);

    List<PositionRecord> findByPortfolioIdAndStatus(String portfolioId, String status);

    List<PositionRecord> findBySymbolId(String symbolId);
}
