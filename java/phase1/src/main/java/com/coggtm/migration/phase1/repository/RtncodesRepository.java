package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.Rtncodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RtncodesRepository extends JpaRepository<Rtncodes, Rtncodes.RtncodesId> {
}
