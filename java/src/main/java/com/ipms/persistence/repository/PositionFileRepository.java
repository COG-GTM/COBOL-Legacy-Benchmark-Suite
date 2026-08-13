package com.ipms.persistence.repository;

import com.ipms.persistence.entity.PositionFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionFileRepository extends JpaRepository<PositionFile, PositionFile.Key> {

    List<PositionFile> findByPortfolioIdAndPositionDate(String portfolioId, String positionDate);
}
