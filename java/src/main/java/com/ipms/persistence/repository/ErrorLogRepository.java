package com.ipms.persistence.repository;

import com.ipms.persistence.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, ErrorLog.Key> {

    List<ErrorLog> findByProgramIdOrderByErrorTimestampDesc(String programId);
}
