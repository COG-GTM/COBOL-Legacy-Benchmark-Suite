package com.cog.portfolio.repository;

import com.cog.portfolio.domain.History;
import com.cog.portfolio.domain.HistoryId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Replaces VSAM TRANHIST access. */
public interface HistoryRepository extends JpaRepository<History, HistoryId> {

    List<History> findByIdPortfolioIdOrderByIdHistoryDateDescIdHistoryTimeDesc(String portfolioId);
}
