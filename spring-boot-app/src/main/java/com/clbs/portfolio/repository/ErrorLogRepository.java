package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByErrorTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT e.errorSeverity, COUNT(e) FROM ErrorLog e " +
           "WHERE e.errorTimestamp BETWEEN :start AND :end GROUP BY e.errorSeverity")
    List<Object[]> countBySeverity(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("SELECT e.programId, COUNT(e) FROM ErrorLog e " +
           "WHERE e.errorTimestamp BETWEEN :start AND :end GROUP BY e.programId")
    List<Object[]> countByProgram(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("SELECT e.errorCode, COUNT(e) FROM ErrorLog e " +
           "WHERE e.errorTimestamp BETWEEN :start AND :end GROUP BY e.errorCode")
    List<Object[]> countByErrorCode(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query("SELECT e FROM ErrorLog e WHERE e.errorTimestamp < :cutoff")
    List<ErrorLog> findOlderThan(@Param("cutoff") LocalDateTime cutoff);

    long countByErrorTimestampBetween(LocalDateTime start, LocalDateTime end);
}
