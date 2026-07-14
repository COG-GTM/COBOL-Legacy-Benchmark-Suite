package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.VsamPoshist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VsamPoshistRepository extends JpaRepository<VsamPoshist, VsamPoshist.VsamPoshistId> {
}
