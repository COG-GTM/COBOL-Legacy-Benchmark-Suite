package com.ipms.persistence.repository;

import com.ipms.persistence.entity.PortfolioMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    List<PortfolioMaster> findByClientIdAndStatus(String clientId, String status);
}
