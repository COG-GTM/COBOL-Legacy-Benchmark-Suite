package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    /** INQHIST: WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC, fetched into a 10-row table. */
    List<PositionHistory> findTop10ByAccountNoOrderByTransDateDescTransTimeDescIdDesc(String accountNo);
}
