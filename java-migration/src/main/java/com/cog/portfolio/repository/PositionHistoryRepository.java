package com.cog.portfolio.repository;

import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.domain.PositionHistoryId;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** DB2 POSHIST (HISTLD00 writer, INQHIST reader). */
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, PositionHistoryId> {

    /** INQHIST: SELECT ... FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC */
    Page<PositionHistory> findByIdAccountNoOrderByIdTransDateDescIdTransTimeDesc(String accountNo, Pageable pageable);

    long countByProcessDate(LocalDate processDate);
}
