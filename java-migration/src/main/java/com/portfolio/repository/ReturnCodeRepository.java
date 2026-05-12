package com.portfolio.repository;

import com.portfolio.model.entity.ReturnCodeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnCodeRepository extends JpaRepository<ReturnCodeEntry, Long> {

    List<ReturnCodeEntry> findByProgramIdOrderByEntryTimestampDesc(String programId);

    @Query("SELECT r.programId, COUNT(r), MAX(r.returnCode), AVG(r.returnCode) " +
            "FROM ReturnCodeEntry r GROUP BY r.programId ORDER BY r.programId")
    List<Object[]> getReturnCodeAnalysisByProgram();
}
