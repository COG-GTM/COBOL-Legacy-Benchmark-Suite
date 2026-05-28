package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.BatchControlRecord;
import com.clbs.portfolio.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchControlRecordRepository extends JpaRepository<BatchControlRecord, Long> {

    List<BatchControlRecord> findByStatus(BatchStatus status);

    List<BatchControlRecord> findByJobName(String jobName);

    @Query("SELECT b FROM BatchControlRecord b WHERE b.status = :status")
    List<BatchControlRecord> findCompletedRecords(@Param("status") BatchStatus status);

    long countByStatus(BatchStatus status);
}
