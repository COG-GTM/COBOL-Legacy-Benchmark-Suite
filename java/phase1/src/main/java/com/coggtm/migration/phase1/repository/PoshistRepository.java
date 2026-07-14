package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.Poshist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoshistRepository extends JpaRepository<Poshist, Poshist.PoshistId> {
}
