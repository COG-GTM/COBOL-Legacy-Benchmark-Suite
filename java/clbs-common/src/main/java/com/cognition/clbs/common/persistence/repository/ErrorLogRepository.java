package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.ErrorLog;
import com.cognition.clbs.common.persistence.entity.ErrorLogKey;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link ErrorLog}. */
public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLogKey> {
  /** Equivalent of the DB2 {@code ERRLOG_CLEANUP(RETENTION_DAYS)} stored procedure. */
  @Modifying
  @Query("delete from ErrorLog e where e.processDate < :cutoff")
  int deleteProcessedBefore(@Param("cutoff") LocalDate cutoff);
}
