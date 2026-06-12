package com.benchmark.portfolio.common.repository;

import com.benchmark.portfolio.common.entity.PortfolioMaster;
import com.benchmark.portfolio.common.entity.PortfolioMasterId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link PortfolioMaster}, replacing VSAM KSDS PORTFILE
 * (PORTFLIO.cpy, RECORD KEY PORT-KEY = PORT-ID + PORT-ACCOUNT-NO).
 *
 * <p>Primary-key CRUD inherited from {@link JpaRepository} replicates the keyed
 * file operations of the portfolio maintenance programs:
 * <ul>
 *   <li>{@code findById} - PORTMSTR.cbl 3000-READ-PORTFOLIO / PORTUPDT.cbl
 *       2100-PROCESS-UPDATE / PORTDEL.cbl 2100-PROCESS-DELETE
 *       ({@code READ PORTFOLIO-FILE} by PORT-KEY)</li>
 *   <li>{@code save} (insert) - PORTADD.cbl 2100-VALIDATE-AND-ADD
 *       ({@code WRITE PORT-RECORD})</li>
 *   <li>{@code save} (update) - PORTUPDT.cbl 2200-APPLY-UPDATE /
 *       PORTMSTR.cbl 4000-UPDATE-PORTFOLIO ({@code REWRITE PORT-RECORD})</li>
 *   <li>{@code deleteById} - PORTDEL.cbl 2200-DELETE-RECORD
 *       ({@code DELETE PORTFOLIO-FILE})</li>
 * </ul>
 */
public interface PortfolioMasterRepository
        extends JpaRepository<PortfolioMaster, PortfolioMasterId> {

    /**
     * Full sequential scan in ascending key order, replicating PORTREAD.cbl
     * 2000-PROCESS ({@code READ PORTFOLIO-FILE NEXT RECORD} loop over the KSDS
     * in PORT-KEY sequence after OPEN).
     */
    List<PortfolioMaster> findAllByOrderByIdPortfolioIdAscIdAccountNoAsc();

    /**
     * Partial-key (key-prefix) read on PORT-ID only, replicating PORTTRAN.cbl
     * 2110-CHECK-PORTFOLIO, 2210-PROCESS-BUY and 2220-PROCESS-SELL
     * ({@code READ PORTFOLIO-FILE} with RECORD KEY IS PORT-ID, i.e. the leading
     * 8 bytes of PORT-KEY).
     */
    List<PortfolioMaster> findByIdPortfolioIdOrderByIdAccountNoAsc(String portfolioId);

    /**
     * Existence check on the PORT-ID key prefix, replicating the INVALID KEY
     * validation branch of PORTTRAN.cbl 2110-CHECK-PORTFOLIO.
     */
    boolean existsByIdPortfolioId(String portfolioId);

    /**
     * Keyed range scan from a starting key value, the STARTBR/READNEXT
     * equivalent: {@code START ... KEY >= ...} followed by
     * {@code READ ... NEXT RECORD} as in PRCSEQ00.cbl 1200-BUILD-SEQUENCE,
     * applied to PORTFILE browsing.
     */
    List<PortfolioMaster> findByIdPortfolioIdGreaterThanEqualOrderByIdPortfolioIdAscIdAccountNoAsc(
            String portfolioId);
}
