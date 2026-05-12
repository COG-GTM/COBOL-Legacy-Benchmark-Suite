package com.portfolio.repository;

import com.portfolio.model.entity.BatchControlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatchControlRepository extends JpaRepository<BatchControlRecord, Long> {

    Optional<BatchControlRecord> findByJobNameAndProcessDate(String jobName, String processDate);

    Optional<BatchControlRecord> findByJobName(String jobName);
}
