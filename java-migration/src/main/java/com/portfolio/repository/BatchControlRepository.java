package com.portfolio.repository;

import com.portfolio.entity.BatchControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BatchControlRepository extends JpaRepository<BatchControl, Long> {

    List<BatchControl> findByJobName(String jobName);

    List<BatchControl> findByStatus(String status);

    List<BatchControl> findByJobNameAndProcessDate(String jobName, String processDate);

    List<BatchControl> findTop20ByOrderByStartTimeDesc();
}
