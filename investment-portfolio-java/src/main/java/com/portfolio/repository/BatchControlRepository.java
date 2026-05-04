package com.portfolio.repository;

import com.portfolio.entity.BatchControl;
import com.portfolio.entity.BatchControlId;
import com.portfolio.entity.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BatchControlRepository extends JpaRepository<BatchControl, BatchControlId> {

    List<BatchControl> findByJobName(String jobName);

    List<BatchControl> findByStatus(BatchStatus status);

    List<BatchControl> findByJobNameAndProcessDate(String jobName, String processDate);
}
