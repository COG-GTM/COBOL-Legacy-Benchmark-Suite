package com.cog.portfolio.repository;

import com.cog.portfolio.domain.ErrorLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** DB2 ERRLOG. */
public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLog.Key> {

    List<ErrorLog> findBySeverityGreaterThanEqualOrderByIdErrorTimestampDesc(int severity);

    /** ERRLOG.sql ERRLOG_CLEANUP stored procedure. */
    @Modifying
    @Query("delete from ErrorLog e where e.id.errorTimestamp < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
