package com.cobolbenchmark.db;

import com.cobolbenchmark.model.BatchControlKey;
import com.cobolbenchmark.model.BatchControlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Batch Control Repository - replaces VSAM BCHCTL file operations.
 * Provides CRUD access to BATCH_CONTROL table.
 */
@Repository
public interface BatchControlRepository extends JpaRepository<BatchControlRecord, BatchControlKey> {

    List<BatchControlRecord> findByJobName(String jobName);

    @Query("SELECT b FROM BatchControlRecord b WHERE b.jobName = :jobName AND b.processDate = :processDate")
    List<BatchControlRecord> findByJobNameAndProcessDate(
            @Param("jobName") String jobName,
            @Param("processDate") String processDate);

    @Query("SELECT b FROM BatchControlRecord b WHERE b.status = :status")
    List<BatchControlRecord> findByStatus(@Param("status") String status);

    @Query("SELECT b FROM BatchControlRecord b WHERE b.jobName = :jobName AND b.processDate = :processDate AND b.status = :status")
    List<BatchControlRecord> findByJobNameAndProcessDateAndStatus(
            @Param("jobName") String jobName,
            @Param("processDate") String processDate,
            @Param("status") String status);
}
