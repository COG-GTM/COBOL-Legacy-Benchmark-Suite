package com.portfolio.repository;

import com.portfolio.domain.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository over the ERRLOG table (written by the ERRPROC migration). */
public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLog.Key> {
}
