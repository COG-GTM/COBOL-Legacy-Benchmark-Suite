package com.clbs.position.repository;

import com.clbs.position.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for the position master &mdash; the modern equivalent
 * of the VSAM KSDS {@code POSMSTR} file-control access in the COBOL programs.
 *
 * <ul>
 *   <li>{@code findById} &harr; keyed VSAM {@code READ}
 *       ({@code PORTTRAN.cbl 2110-CHECK-PORTFOLIO}).</li>
 *   <li>{@code findByPortfolioId...} &harr; generic key / alternate-index read.</li>
 *   <li>{@code findAll} &harr; sequential browse
 *       ({@code RPTPOS00.cbl 2100-READ-POSITIONS}).</li>
 * </ul>
 */
public interface PositionRepository extends JpaRepository<Position, Long> {

    /** Keyed read on the natural VSAM composite key {@code POS-KEY}. */
    Optional<Position> findByPortfolioIdAndPositionDateAndInvestmentId(
            String portfolioId, String positionDate, String investmentId);

    /**
     * Resolves the running holding for a portfolio + investment regardless of
     * the as-of date &mdash; the position the update job carries forward and
     * re-marks for the current processing date.
     */
    Optional<Position> findFirstByPortfolioIdAndInvestmentIdOrderByPositionDateDesc(
            String portfolioId, String investmentId);

    /** All holdings for a portfolio (alternate-index style access). */
    List<Position> findByPortfolioId(String portfolioId);

    /** Active positions only (data-dictionary rule 5.2). */
    List<Position> findByStatus(String status);
}
