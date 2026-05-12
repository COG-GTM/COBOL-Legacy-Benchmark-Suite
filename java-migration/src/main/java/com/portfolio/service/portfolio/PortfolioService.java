package com.portfolio.service.portfolio;

import com.portfolio.exception.DuplicatePortfolioException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.enums.AuditAction;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.service.common.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValidator portfolioValidator;
    private final AuditService auditService;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            PortfolioValidator portfolioValidator,
                            AuditService auditService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioValidator = portfolioValidator;
        this.auditService = auditService;
    }

    @Transactional
    public Portfolio createPortfolio(Portfolio portfolio) {
        portfolioValidator.validate(portfolio);

        if (portfolioRepository.existsById(portfolio.getPortfolioId())) {
            throw new DuplicatePortfolioException(portfolio.getPortfolioId());
        }

        portfolio.setLastMaintDate(LocalDateTime.now());
        Portfolio saved = portfolioRepository.save(portfolio);

        auditService.logTransaction(saved.getPortfolioId(), saved.getClientId(),
                AuditAction.CREATE, AuditStatus.SUCCESS, saved.getLastMaintUser(),
                "PORTMSTR", null, saved.toString(), "Portfolio created");

        log.info("Portfolio created: {}", saved.getPortfolioId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Portfolio readPortfolio(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

    @Transactional
    public Portfolio updatePortfolio(Portfolio portfolio) {
        Portfolio existing = portfolioRepository.findById(portfolio.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(portfolio.getPortfolioId()));

        String beforeImage = existing.toString();
        portfolioValidator.validate(portfolio);
        portfolio.setLastMaintDate(LocalDateTime.now());
        Portfolio updated = portfolioRepository.save(portfolio);

        auditService.logPortfolioUpdate(updated.getPortfolioId(),
                updated.getLastMaintUser(), beforeImage);

        log.info("Portfolio updated: {}", updated.getPortfolioId());
        return updated;
    }

    @Transactional
    public void deletePortfolio(String portfolioId) {
        Portfolio existing = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        portfolioRepository.delete(existing);

        auditService.logTransaction(portfolioId, existing.getClientId(),
                AuditAction.DELETE, AuditStatus.SUCCESS, existing.getLastMaintUser(),
                "PORTMSTR", existing.toString(), null, "Portfolio deleted");

        log.info("Portfolio deleted: {}", portfolioId);
    }

    @Transactional(readOnly = true)
    public List<Portfolio> findActivePortfolios() {
        return portfolioRepository.findActivePortfolios();
    }

    @Transactional(readOnly = true)
    public List<Portfolio> findByStatus(Character status) {
        return portfolioRepository.findByStatus(status);
    }
}
