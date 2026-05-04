package com.portfolio.service.utility;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Position;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Validation Service - migrated from COBOL UTLVAL00.cbl.
 * Cross-reference checks, balance reconciliation.
 */
@Service
public class DataValidationService {

    private static final Logger log = LoggerFactory.getLogger(DataValidationService.class);

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    public DataValidationService(PortfolioRepository portfolioRepository,
                                 PositionRepository positionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public List<String> validateAllPortfolios() {
        List<String> errors = new ArrayList<>();
        List<Portfolio> portfolios = portfolioRepository.findAll();

        for (Portfolio portfolio : portfolios) {
            errors.addAll(validatePortfolioData(portfolio));
        }

        if (errors.isEmpty()) {
            log.info("Data validation passed: all portfolios valid");
        } else {
            log.warn("Data validation found {} issues", errors.size());
        }

        return errors;
    }

    public List<String> validatePortfolioData(Portfolio portfolio) {
        List<String> errors = new ArrayList<>();

        if (portfolio.getPortfolioId() == null || portfolio.getPortfolioId().trim().isEmpty()) {
            errors.add("Portfolio has null/empty ID");
        }

        if (portfolio.getClientType() != null) {
            String ct = portfolio.getClientType();
            if (!"I".equals(ct) && !"C".equals(ct) && !"T".equals(ct)) {
                errors.add("Portfolio " + portfolio.getPortfolioId()
                        + ": invalid client type '" + ct + "'");
            }
        }

        if (portfolio.getStatus() != null) {
            String st = portfolio.getStatus();
            if (!"A".equals(st) && !"C".equals(st) && !"S".equals(st)) {
                errors.add("Portfolio " + portfolio.getPortfolioId()
                        + ": invalid status '" + st + "'");
            }
        }

        List<Position> positions = positionRepository.findByPortfolioId(portfolio.getPortfolioId());
        BigDecimal positionTotal = positions.stream()
                .map(Position::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (portfolio.getTotalValue() != null
                && portfolio.getTotalValue().compareTo(BigDecimal.ZERO) > 0
                && positionTotal.compareTo(BigDecimal.ZERO) == 0
                && portfolio.isActive()) {
            errors.add("Portfolio " + portfolio.getPortfolioId()
                    + ": total value set but no positions found");
        }

        return errors;
    }
}
