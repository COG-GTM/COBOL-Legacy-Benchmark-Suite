package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.PortfolioMaster;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link PortfolioMaster}. */
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {}
