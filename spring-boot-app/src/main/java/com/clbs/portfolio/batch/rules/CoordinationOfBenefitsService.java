package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.Portfolio;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoordinationOfBenefitsService {

    private final PortfolioRepository portfolioRepository;

    public AdjudicationResult apply(TransactionRecord transaction) {
        if (transaction.getTrnType() != TransactionType.TR) {
            return AdjudicationResult.APPROVED;
        }

        Optional<Portfolio> sourcePortfolio = portfolioRepository.findById(transaction.getPortfolioId());
        if (sourcePortfolio.isEmpty()) {
            log.warn("Source portfolio not found for transfer: {}", transaction.getPortfolioId());
            return AdjudicationResult.DENIED;
        }

        if (!"A".equals(sourcePortfolio.get().getStatus())) {
            log.warn("Source portfolio {} is not active for transfer", transaction.getPortfolioId());
            return AdjudicationResult.DENIED;
        }

        String destinationId = extractDestinationPortfolioId(transaction);
        if (destinationId == null) {
            log.warn("No destination portfolio specified for transfer");
            return AdjudicationResult.NEEDS_REVIEW;
        }

        Optional<Portfolio> destPortfolio = portfolioRepository.findById(destinationId);
        if (destPortfolio.isEmpty()) {
            log.warn("Destination portfolio not found for transfer: {}", destinationId);
            return AdjudicationResult.DENIED;
        }

        if (!"A".equals(destPortfolio.get().getStatus())) {
            log.warn("Destination portfolio {} is not active for transfer", destinationId);
            return AdjudicationResult.DENIED;
        }

        return AdjudicationResult.APPROVED;
    }

    private String extractDestinationPortfolioId(TransactionRecord transaction) {
        String investmentId = transaction.getInvestmentId();
        if (investmentId != null && investmentId.startsWith("PORT")) {
            return investmentId.length() > 8 ? investmentId.substring(0, 8) : investmentId;
        }
        return null;
    }
}
