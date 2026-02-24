package com.portfolio.repository;

import com.portfolio.entity.BatchControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchControlRepository extends JpaRepository<BatchControl, Long> {

    Optional<BatchControl> findByProcessDateAndProcessId(LocalDate processDate, String processId);

    List<BatchControl> findByProcessDate(LocalDate processDate);

    List<BatchControl> findByStatus(String status);
}
