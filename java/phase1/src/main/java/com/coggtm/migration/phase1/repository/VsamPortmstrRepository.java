package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.VsamPortmstr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VsamPortmstrRepository extends JpaRepository<VsamPortmstr, VsamPortmstr.VsamPortmstrId> {
}
