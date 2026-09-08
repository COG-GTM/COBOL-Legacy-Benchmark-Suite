package com.clbs.db2;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Replaces the POSHIST inserts of HISTLD00 and the HISTORY_CURSOR of INQHIST. */
public interface PositionHistoryRepository extends JpaRepository<PositionHistoryEntity, Long> {

    List<PositionHistoryEntity> findByAccountNoOrderByTransDateDesc(String accountNo);

    List<PositionHistoryEntity> findByPortfolioIdOrderByTransDateDesc(String portfolioId);
}
