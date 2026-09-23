package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.InvestmentPosition;
import com.cognition.clbs.common.persistence.entity.InvestmentPositionKey;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link InvestmentPosition}. */
public interface InvestmentPositionRepository
    extends JpaRepository<InvestmentPosition, InvestmentPositionKey> {}
