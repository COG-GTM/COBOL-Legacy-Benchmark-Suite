package com.portfolio.repository;

import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.PositionHistoryId;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionHistoryRepository
    extends JpaRepository<PositionHistory, PositionHistoryId> {
  List<PositionHistory> findByIdAccountNoOrderByIdTransDateDescIdTransTimeDesc(
      String accountNo, Pageable pageable);
}
