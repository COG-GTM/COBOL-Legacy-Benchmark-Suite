package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.PortfolioPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link PortfolioPosition}, replacing PORTTRAN's
 * VSAM {@code PORTFOLIO-FILE} access.
 *
 * <ul>
 *   <li>{@code findById}  &rarr; keyed {@code READ PORTFOLIO-FILE} on {@code PORT-ID}</li>
 *   <li>{@code existsById} &rarr; the {@code INVALID KEY} existence check in
 *       {@code 2110-CHECK-PORTFOLIO}</li>
 *   <li>{@code save}      &rarr; {@code REWRITE PORTFOLIO-RECORD}</li>
 * </ul>
 */
@Repository
public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, String> {
}
