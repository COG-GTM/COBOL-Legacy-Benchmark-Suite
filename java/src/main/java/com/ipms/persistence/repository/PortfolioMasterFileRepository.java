package com.ipms.persistence.repository;

import com.ipms.persistence.entity.PortfolioMasterFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioMasterFileRepository
        extends JpaRepository<PortfolioMasterFile, PortfolioMasterFile.Key> {

    List<PortfolioMasterFile> findByPortfolioId(String portfolioId);
}
