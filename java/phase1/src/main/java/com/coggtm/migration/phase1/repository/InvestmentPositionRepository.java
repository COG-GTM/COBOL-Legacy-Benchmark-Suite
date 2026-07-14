package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.InvestmentPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPosition.InvestmentPositionId> {
}
