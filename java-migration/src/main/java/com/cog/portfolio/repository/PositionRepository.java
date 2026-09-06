package com.cog.portfolio.repository;

import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.PositionId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Replaces VSAM POSFILE access (INQPORT, RPTPOS00). Pageable replaces CURSMGR cursors. */
public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByIdPortfolioIdOrderByIdInvestmentIdAsc(String portfolioId);

    Optional<Position> findFirstByIdPortfolioIdAndIdInvestmentIdOrderByIdPositionDateDesc(
            String portfolioId, String investmentId);

    Page<Position> findAllByOrderByIdPortfolioIdAscIdInvestmentIdAsc(Pageable pageable);

    @Query("""
            select p from Position p
            where p.id.portfolioId in (select f.id.portfolioId from Portfolio f where f.id.accountNo = :accountNo)
            order by p.id.investmentId asc
            """)
    List<Position> findByAccountNo(@Param("accountNo") String accountNo);
}
