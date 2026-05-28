package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.ProcessSequenceRecord;
import com.clbs.portfolio.model.ProcessSequenceRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ProcessSequenceRecord entities.
 * Replaces VSAM access patterns for PRCSEQ file.
 */
@Repository
public interface ProcessSequenceRecordRepository extends JpaRepository<ProcessSequenceRecord, ProcessSequenceRecordId> {

    List<ProcessSequenceRecord> findByTypeOrderByStartTimeAsc(ProcessSequenceRecord.SequenceType type);
}
