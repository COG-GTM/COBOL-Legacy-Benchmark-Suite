package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.VsamTranhist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VsamTranhistRepository extends JpaRepository<VsamTranhist, String> {
}
