package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.Portfolio;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EligibilityRuleService {

    private final PortfolioRepository portfolioRepository;

    public AdjudicationResult apply(TransactionRecord transaction) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(transaction.getPortfolioId());

        if (portfolioOpt.isEmpty()) {
            log.warn("Portfolio not found: {}", transaction.getPortfolioId());
            return AdjudicationResult.DENIED;
        }

        Portfolio portfolio = portfolioOpt.get();

        if ("C".equals(portfolio.getStatus()) || "S".equals(portfolio.getStatus())) {
            log.warn("Portfolio {} is not ACTIVE (status={})", portfolio.getPortfolioId(), portfolio.getStatus());
            return AdjudicationResult.DENIED;
        }

        if (!"A".equals(portfolio.getStatus())) {
            log.warn("Portfolio {} has unknown status: {}", portfolio.getPortfolioId(), portfolio.getStatus());
            return AdjudicationResult.NEEDS_REVIEW;
        }

        if (portfolio.getAccountNo() == null || portfolio.getAccountNo().isBlank()) {
            log.warn("Portfolio {} has no account number", portfolio.getPortfolioId());
            return AdjudicationResult.DENIED;
        }

        return AdjudicationResult.APPROVED;
    }
}
