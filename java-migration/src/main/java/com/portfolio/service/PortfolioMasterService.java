package com.portfolio.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.entity.PortfolioMaster;
import com.portfolio.repository.PortfolioMasterRepository;

/**
 * Service layer for PortfolioMaster operations.
 * <p>
 * Maps COBOL PORTMSTR.cbl business operations to Spring-managed transactions:
 * <ul>
 *   <li>CREATE-PORTFOLIO (2000-CREATE-PORTFOLIO) -> {@link #create}</li>
 *   <li>READ-PORTFOLIO   (3000-READ-PORTFOLIO)   -> {@link #findById}</li>
 *   <li>UPDATE-PORTFOLIO  (4000-UPDATE-PORTFOLIO) -> {@link #update}</li>
 *   <li>DELETE-PORTFOLIO  (5000-DELETE-PORTFOLIO) -> {@link #delete}</li>
 *   <li>Sequential READ   (PORTREAD.cbl)          -> {@link #findAll}</li>
 * </ul>
 */
@Service
@Transactional
public class PortfolioMasterService {

    private final PortfolioMasterRepository repository;

    public PortfolioMasterService(PortfolioMasterRepository repository) {
        this.repository = repository;
    }

    /**
     * Mirrors COBOL 3000-READ-PORTFOLIO (keyed READ).
     */
    @Transactional(readOnly = true)
    public Optional<PortfolioMaster> findById(String portId) {
        return repository.findById(portId);
    }

    /**
     * Mirrors COBOL PORTREAD.cbl sequential READ NEXT.
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> findAll() {
        return repository.findAll();
    }

    /**
     * Mirrors COBOL 2000-CREATE-PORTFOLIO (WRITE).
     * Sets create date and last maintenance date if not already set.
     */
    public PortfolioMaster create(PortfolioMaster portfolio) {
        validatePortfolioId(portfolio.getPortId());
        if (repository.existsById(portfolio.getPortId())) {
            throw new IllegalArgumentException("Portfolio ID already exists: " + portfolio.getPortId());
        }
        if (portfolio.getPortCreateDate() == null) {
            portfolio.setPortCreateDate(LocalDate.now());
        }
        if (portfolio.getPortLastMaint() == null) {
            portfolio.setPortLastMaint(LocalDate.now());
        }
        return repository.save(portfolio);
    }

    /**
     * Mirrors COBOL 4000-UPDATE-PORTFOLIO (REWRITE).
     * Updates last maintenance date on each modification.
     */
    public PortfolioMaster update(PortfolioMaster portfolio) {
        validatePortfolioId(portfolio.getPortId());
        if (!repository.existsById(portfolio.getPortId())) {
            throw new IllegalArgumentException("Portfolio not found for update: " + portfolio.getPortId());
        }
        portfolio.setPortLastMaint(LocalDate.now());
        return repository.save(portfolio);
    }

    /**
     * Mirrors COBOL 5000-DELETE-PORTFOLIO (DELETE).
     */
    public void delete(String portId) {
        if (!repository.existsById(portId)) {
            throw new IllegalArgumentException("Portfolio not found for deletion: " + portId);
        }
        repository.deleteById(portId);
    }

    /**
     * Find portfolios by status. Mirrors COBOL level-88 conditions.
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> findByStatus(String status) {
        return repository.findByPortStatus(status);
    }

    /**
     * Find portfolios by client type. Mirrors COBOL level-88 conditions.
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> findByClientType(String clientType) {
        return repository.findByPortClientType(clientType);
    }

    /**
     * Find portfolios by account number.
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> findByAccountNo(String accountNo) {
        return repository.findByPortAccountNo(accountNo);
    }

    /**
     * Mirrors COBOL 2100-VALIDATE-PORTFOLIO in PORTMSTR.cbl
     * and PORTVALD.cbl 1000-VALIDATE-ID:
     * Portfolio ID must start with 'PORT' and have numeric digits following.
     */
    private void validatePortfolioId(String portId) {
        if (portId == null || portId.length() > 8) {
            throw new IllegalArgumentException("Invalid Portfolio ID format");
        }
        if (!portId.startsWith("PORT")) {
            throw new IllegalArgumentException("Invalid Portfolio ID format: must start with 'PORT'");
        }
        String numericPart = portId.substring(4);
        if (numericPart.isEmpty() || !numericPart.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid Portfolio ID format: digits must follow 'PORT'");
        }
    }
}
