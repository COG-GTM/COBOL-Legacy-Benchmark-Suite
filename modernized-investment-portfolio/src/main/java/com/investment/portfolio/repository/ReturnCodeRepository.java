package com.investment.portfolio.repository;

import com.investment.portfolio.entity.ReturnCode;
import com.investment.portfolio.entity.ReturnCodeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for the ReturnCode entity.
 *
 * Provides access patterns matching the original DB2 RTNCODES table
 * index-based queries.
 */
@Repository
public interface ReturnCodeRepository extends JpaRepository<ReturnCode, ReturnCodeId> {

    /**
     * Find return codes by program ID, ordered by timestamp.
     * Replaces DB2 RTNCODES_PRG_IDX index access pattern.
     */
    List<ReturnCode> findByIdProgramIdOrderByIdLogTimestampDesc(String programId);

    /**
     * Find return codes by status code, ordered by timestamp.
     * Replaces DB2 RTNCODES_STS_IDX index access pattern.
     */
    List<ReturnCode> findByStatusCodeOrderByIdLogTimestampDesc(String statusCode);
}
