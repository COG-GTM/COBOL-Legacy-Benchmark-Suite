package com.cog.portfolio.repository;

import com.cog.portfolio.domain.ReturnCodeLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** DB2 RTNCODES (RTNCDE00 writer, RTNANA00 reader). */
public interface ReturnCodeLogRepository extends JpaRepository<ReturnCodeLog, ReturnCodeLog.Key> {

    List<ReturnCodeLog> findByIdProgramIdOrderByIdTimestampDesc(String programId);

    List<ReturnCodeLog> findAllByOrderByIdTimestampDesc();
}
