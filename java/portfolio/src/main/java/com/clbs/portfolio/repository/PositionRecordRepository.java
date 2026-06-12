package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.PositionKey;
import com.clbs.portfolio.domain.PositionRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the POSHIST VSAM KSDS (position history file).
 *
 * <p>Primary key is portfolio+date+investment; methods cover position browse by
 * portfolio and by as-of date.
 */
@Repository
public interface PositionRecordRepository extends JpaRepository<PositionRecord, PositionKey> {

    List<PositionRecord> findByKeyPortfolioIdAndKeyPosDateOrderByKeyInvestmentIdAsc(
            String portfolioId, String posDate);

    List<PositionRecord> findByKeyPortfolioIdOrderByKeyPosDateAscKeyInvestmentIdAsc(String portfolioId);

    List<PositionRecord> findByStatus(String status);
}
