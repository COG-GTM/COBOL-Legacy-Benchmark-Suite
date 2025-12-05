package com.portfolio.repository;

import com.portfolio.entity.PortfolioMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Portfolio Master entity
 * Provides data access methods for portfolio operations
 * Supports row-level locking to replicate VSAM record-level locking behavior
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, UUID> {

    /**
     * Find portfolio by composite key (portfolio ID, account type, branch ID)
     * Replicates VSAM KSDS key access pattern
     */
    Optional<PortfolioMaster> findByPortfolioIdAndAccountTypeAndBranchId(
            String portfolioId, String accountType, String branchId);

    /**
     * Find portfolio by composite key with pessimistic lock
     * Replicates VSAM record-level locking for updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PortfolioMaster p WHERE p.portfolioId = :portfolioId " +
            "AND p.accountType = :accountType AND p.branchId = :branchId")
    Optional<PortfolioMaster> findByKeyWithLock(
            @Param("portfolioId") String portfolioId,
            @Param("accountType") String accountType,
            @Param("branchId") String branchId);

    /**
     * Find portfolio by portfolio ID only
     */
    Optional<PortfolioMaster> findByPortfolioId(String portfolioId);

    /**
     * Find all portfolios by client ID
     */
    List<PortfolioMaster> findByClientId(String clientId);

    /**
     * Find all portfolios by client ID and status
     */
    List<PortfolioMaster> findByClientIdAndStatus(String clientId, String status);

    /**
     * Find all portfolios by account number
     */
    List<PortfolioMaster> findByAccountNo(String accountNo);

    /**
     * Find all active portfolios
     */
    @Query("SELECT p FROM PortfolioMaster p WHERE p.status = 'A' " +
            "AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<PortfolioMaster> findAllActivePortfolios();

    /**
     * Find active portfolios with pagination
     */
    @Query("SELECT p FROM PortfolioMaster p WHERE p.status = 'A' " +
            "AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    Page<PortfolioMaster> findAllActivePortfolios(Pageable pageable);

    /**
     * Find portfolios by status
     */
    List<PortfolioMaster> findByStatus(String status);

    /**
     * Find portfolios by status with pagination
     */
    Page<PortfolioMaster> findByStatus(String status, Pageable pageable);

    /**
     * Find portfolios by branch ID
     */
    List<PortfolioMaster> findByBranchId(String branchId);

    /**
     * Find portfolios opened within date range
     */
    List<PortfolioMaster> findByOpenDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find portfolios by client type
     */
    List<PortfolioMaster> findByClientType(String clientType);

    /**
     * Find portfolios by currency code
     */
    List<PortfolioMaster> findByCurrencyCode(String currencyCode);

    /**
     * Count portfolios by status
     */
    long countByStatus(String status);

    /**
     * Count portfolios by client ID
     */
    long countByClientId(String clientId);

    /**
     * Check if portfolio exists by composite key
     */
    boolean existsByPortfolioIdAndAccountTypeAndBranchId(
            String portfolioId, String accountType, String branchId);

    /**
     * Search portfolios by client name (partial match)
     */
    @Query("SELECT p FROM PortfolioMaster p WHERE LOWER(p.clientName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<PortfolioMaster> searchByClientName(@Param("name") String name);

    /**
     * Find portfolios with total value greater than specified amount
     */
    @Query("SELECT p FROM PortfolioMaster p WHERE p.totalValue > :minValue AND p.status = 'A'")
    List<PortfolioMaster> findHighValuePortfolios(@Param("minValue") java.math.BigDecimal minValue);

    /**
     * Get total value sum by client ID
     */
    @Query("SELECT SUM(p.totalValue) FROM PortfolioMaster p WHERE p.clientId = :clientId AND p.status = 'A'")
    java.math.BigDecimal getTotalValueByClientId(@Param("clientId") String clientId);
}
