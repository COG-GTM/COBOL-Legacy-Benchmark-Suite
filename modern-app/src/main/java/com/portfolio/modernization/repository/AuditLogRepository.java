package com.portfolio.modernization.repository;

import com.portfolio.modernization.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPortfolioId(String portfolioId);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByActionType(AuditLog.ActionType actionType);

    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLog> findByUserIdAndTimestampRange(@Param("userId") String userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
