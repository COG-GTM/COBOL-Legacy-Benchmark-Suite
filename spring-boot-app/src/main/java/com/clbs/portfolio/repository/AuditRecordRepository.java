package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditRecord> findByAuditType(String auditType);

    List<AuditRecord> findByAction(String action);

    @Query("SELECT a FROM AuditRecord a WHERE a.timestamp BETWEEN :start AND :end " +
           "AND a.action IN ('LOGIN', 'LOGOUT')")
    List<AuditRecord> findSecurityEvents(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT a FROM AuditRecord a WHERE a.timestamp BETWEEN :start AND :end " +
           "AND a.action IN ('CREATE', 'UPDATE', 'DELETE')")
    List<AuditRecord> findProcessEvents(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("SELECT a.action, COUNT(a) FROM AuditRecord a " +
           "WHERE a.timestamp BETWEEN :start AND :end GROUP BY a.action")
    List<Object[]> countByAction(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
