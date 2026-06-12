package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.PortfolioMaster;
import com.clbs.portfolio.domain.PortfolioKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the PORTMSTR VSAM KSDS (portfolio master file).
 *
 * <p>Access methods mirror the COBOL file-control patterns:
 * <ul>
 *   <li>{@link #findById} — keyed READ on the full primary key (PORT-KEY).</li>
 *   <li>{@code findByKeyPortId} — partial-key / alternate-index access by PORT-ID.</li>
 *   <li>{@code findByClientTypeAndStatus} — matches IDX_PORT_MASTER_CLIENT scans.</li>
 *   <li>{@code findByKeyPortIdGreaterThanEqual...} — STARTBR/READNEXT browse.</li>
 * </ul>
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, PortfolioKey> {

    List<PortfolioMaster> findByKeyPortId(String portId);

    List<PortfolioMaster> findByStatus(String status);

    List<PortfolioMaster> findByClientTypeAndStatus(String clientType, String status);

    /** STARTBR at the given PORT-ID then READNEXT to end of file. */
    List<PortfolioMaster> findByKeyPortIdGreaterThanEqualOrderByKeyPortIdAscKeyAccountNoAsc(String portId);
}
