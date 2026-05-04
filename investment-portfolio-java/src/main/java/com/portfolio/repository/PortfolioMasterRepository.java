package com.portfolio.repository;

import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.PortfolioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    List<PortfolioMaster> findByClientId(String clientId);

    List<PortfolioMaster> findByStatus(PortfolioStatus status);

    List<PortfolioMaster> findByClientIdAndStatus(String clientId, PortfolioStatus status);

    Optional<PortfolioMaster> findByAccountNo(String accountNo);

    @Query("SELECT p FROM PortfolioMaster p WHERE p.status = 'ACTIVE' " +
            "AND (p.closeDate IS NULL OR p.closeDate > :currentDate)")
    List<PortfolioMaster> findActivePortfolios(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT p FROM PortfolioMaster p WHERE p.branchId = :branchId AND p.status = :status")
    List<PortfolioMaster> findByBranchIdAndStatus(
            @Param("branchId") String branchId,
            @Param("status") PortfolioStatus status);
}
