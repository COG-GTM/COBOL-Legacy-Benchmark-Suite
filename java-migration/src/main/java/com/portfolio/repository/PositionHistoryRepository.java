package com.portfolio.repository;
import com.portfolio.domain.*; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface PositionHistoryRepository extends JpaRepository<PositionHistory,PositionHistoryId> { List<PositionHistory> findByIdAccountNoOrderByIdTransDateDescIdTransTimeDesc(String accountNo, Pageable pageable); }
