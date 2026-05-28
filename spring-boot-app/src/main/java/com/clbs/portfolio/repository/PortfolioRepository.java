package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.Portfolio;
import com.clbs.portfolio.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByStatus(EntityStatus status);
}
