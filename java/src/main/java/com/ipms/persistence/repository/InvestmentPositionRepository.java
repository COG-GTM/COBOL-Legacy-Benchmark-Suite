package com.ipms.persistence.repository;

import com.ipms.persistence.entity.InvestmentPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InvestmentPositionRepository
        extends JpaRepository<InvestmentPosition, InvestmentPosition.Key> {

    List<InvestmentPosition> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);
}
