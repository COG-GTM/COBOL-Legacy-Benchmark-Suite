package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.BatchControlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchControlRecordRepository extends JpaRepository<BatchControlRecord, Long> {
}
