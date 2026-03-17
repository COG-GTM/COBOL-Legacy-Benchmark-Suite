package com.portfolio.repository;

import com.portfolio.model.BatchControlKey;
import com.portfolio.model.BatchControlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Batch Control Record entity.
 * Replaces: VSAM BCHCTL file I/O in BCHCTL00.cbl and PRCSEQ00.cbl.
 */
@Repository
public interface BatchControlRepository extends JpaRepository<BatchControlRecord, BatchControlKey> {

    List<BatchControlRecord> findByKeyJobNameAndStatus(String jobName, String status);

    List<BatchControlRecord> findByStatus(String status);

    List<BatchControlRecord> findByKeyProcessDate(String processDate);

    List<BatchControlRecord> findByKeyJobNameAndKeyProcessDate(String jobName, String processDate);
}
