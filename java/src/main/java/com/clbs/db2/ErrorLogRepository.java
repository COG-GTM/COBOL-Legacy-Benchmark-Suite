package com.clbs.db2;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Replaces the ERRLOG inserts of ERRHNDL / ERRPROC. */
public interface ErrorLogRepository extends JpaRepository<ErrorLogEntity, Long> {

    List<ErrorLogEntity> findByProgramId(String programId);
}
