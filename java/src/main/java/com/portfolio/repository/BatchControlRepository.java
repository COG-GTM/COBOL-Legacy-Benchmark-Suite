package com.portfolio.repository;

import com.portfolio.domain.BatchControl;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository over the batch control table (VSAM BCHCTL migration). */
public interface BatchControlRepository extends JpaRepository<BatchControl, BatchControl.Key> {
}
