package com.portfolio.repository;

import com.portfolio.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByProgramId(String programId);

    List<ErrorLog> findByProcessDateBetween(LocalDate startDate, LocalDate endDate);

    List<ErrorLog> findByErrorSeverityGreaterThanEqual(Integer severity);
}
