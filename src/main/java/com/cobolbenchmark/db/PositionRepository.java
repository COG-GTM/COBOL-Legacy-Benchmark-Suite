package com.cobolbenchmark.db;

import com.cobolbenchmark.model.PositionRecord;
import com.cobolbenchmark.model.PositionRecordKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Position Repository - replaces VSAM POSFILE operations.
 * Provides CRUD access to INVESTMENT_POSITIONS table.
 */
@Repository
public interface PositionRepository extends JpaRepository<PositionRecord, PositionRecordKey> {

    List<PositionRecord> findByPortfolioId(String portfolioId);

    @Query("SELECT p FROM PositionRecord p WHERE p.portfolioId = :portfolioId AND p.status = :status")
    List<PositionRecord> findByPortfolioIdAndStatus(
            @Param("portfolioId") String portfolioId,
            @Param("status") String status);

    @Query("SELECT p FROM PositionRecord p WHERE p.portfolioId = :portfolioId AND p.investmentId = :investmentId")
    List<PositionRecord> findByPortfolioIdAndInvestmentId(
            @Param("portfolioId") String portfolioId,
            @Param("investmentId") String investmentId);

    @Query("SELECT p FROM PositionRecord p WHERE p.portfolioId >= :startKey ORDER BY p.portfolioId")
    List<PositionRecord> findByPortfolioIdGreaterThanEqual(@Param("startKey") String startKey);
}
