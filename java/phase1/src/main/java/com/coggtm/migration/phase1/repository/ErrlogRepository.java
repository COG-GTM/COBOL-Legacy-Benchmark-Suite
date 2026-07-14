package com.coggtm.migration.phase1.repository;

import com.coggtm.migration.phase1.entity.Errlog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrlogRepository extends JpaRepository<Errlog, Errlog.ErrlogId> {
}
