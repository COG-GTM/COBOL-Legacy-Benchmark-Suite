package com.portfolio.repository;

import com.portfolio.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByProgramIdOrderByErrorTimestampDesc(String programId);

    List<ErrorLog> findByErrorSeverityOrderByErrorTimestampDesc(int severity);

    List<ErrorLog> findByErrorTimestampBetweenOrderByErrorTimestampDesc(
            LocalDateTime start, LocalDateTime end);

    List<ErrorLog> findTop100ByOrderByErrorTimestampDesc();

    long countByErrorSeverity(int severity);
}
