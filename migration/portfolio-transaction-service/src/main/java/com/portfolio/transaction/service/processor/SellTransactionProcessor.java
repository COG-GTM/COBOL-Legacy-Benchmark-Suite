package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResult;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.enums.TransactionType;
import com.portfolio.transaction.exception.InsufficientUnitsException;
import com.portfolio.transaction.repository.PortfolioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component
public class SellTransactionProcessor implements TransactionProcessor {

    private final PortfolioRepository portfolioRepository;

    public SellTransactionProcessor(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public TransactionType getSupportedType() {
        return TransactionType.SELL;
    }

    @Override
    @Transactional
    public TransactionResult process(TransactionRequest request, Portfolio portfolio) {
        if (portfolio.getTotalUnits().compareTo(request.getQuantity()) < 0) {
            throw new InsufficientUnitsException(
                "Insufficient units for sale. Available: " + portfolio.getTotalUnits() + 
                ", Requested: " + request.getQuantity());
        }

        BigDecimal newUnits = portfolio.getTotalUnits().subtract(request.getQuantity());
        portfolio.setTotalUnits(newUnits);

        BigDecimal newCost = portfolio.getTotalCost().subtract(request.getAmount());
        portfolio.setTotalCost(newCost);

        portfolioRepository.save(portfolio);

        return TransactionResult.success(portfolio);
    }
}
