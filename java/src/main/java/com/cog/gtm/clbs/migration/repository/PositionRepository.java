package com.cog.gtm.clbs.migration.repository;

import com.cog.gtm.clbs.migration.domain.position.Position;
import com.cog.gtm.clbs.migration.domain.position.PositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionRepository extends JpaRepository<Position, PositionId> {
}
