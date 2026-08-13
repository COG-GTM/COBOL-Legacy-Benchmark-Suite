package com.ipms.persistence.repository;

import com.ipms.persistence.entity.ReturnCodeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnCodeLogRepository extends JpaRepository<ReturnCodeLog, ReturnCodeLog.Key> {

    List<ReturnCodeLog> findByProgramIdOrderByTimestampDesc(String programId);
}
