package com.cobolbenchmark.db;

import com.cobolbenchmark.model.ProcessSequenceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Process Sequence Repository - replaces VSAM PRCCTL file operations.
 * Provides CRUD access to PROCESS_SEQUENCE table.
 */
@Repository
public interface ProcessSequenceRepository extends JpaRepository<ProcessSequenceRecord, String> {

    @Query("SELECT p FROM ProcessSequenceRecord p WHERE p.processId >= :startKey ORDER BY p.processId")
    List<ProcessSequenceRecord> findByProcessIdGreaterThanEqual(@Param("startKey") String startKey);

    @Query("SELECT p FROM ProcessSequenceRecord p ORDER BY p.processId")
    List<ProcessSequenceRecord> findAllOrdered();
}
