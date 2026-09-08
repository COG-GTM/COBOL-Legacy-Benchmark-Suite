package com.clbs.db2;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Replaces the RTNCODES SQL of RTNCDE00 and RTNANA00. */
public interface ReturnCodeLogRepository extends JpaRepository<ReturnCodeLogEntity, Long> {

    List<ReturnCodeLogEntity> findByProgramId(String programId);

    /** RTNANA00 P200-PROCESS-ANALYSIS cursor. */
    @Query("""
            select r.programId, count(r),
                   sum(case when r.statusCode = 'S' then 1 else 0 end),
                   sum(case when r.statusCode = 'W' then 1 else 0 end),
                   sum(case when r.statusCode = 'E' then 1 else 0 end),
                   sum(case when r.statusCode = 'F' then 1 else 0 end)
            from ReturnCodeLogEntity r
            group by r.programId
            order by r.programId
            """)
    List<Object[]> analyzeByProgram();

    /** RTNCDE00 P500-ANALYZE-CODES. */
    @Query("""
            select count(r), max(r.returnCode), min(r.returnCode)
            from ReturnCodeLogEntity r
            where r.programId = :programId
            """)
    List<Object[]> summarizeForProgram(@Param("programId") String programId);
}
