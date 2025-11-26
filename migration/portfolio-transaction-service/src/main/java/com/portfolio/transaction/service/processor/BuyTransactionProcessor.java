package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResult;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.enums.TransactionType;
import com.portfolio.transaction.repository.PortfolioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component
public class BuyTransactionProcessor implements TransactionProcessor {

    private final PortfolioRepository portfolioRepository;

    public BuyTransactionProcessor(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public TransactionType getSupportedType() {
        return TransactionType.BUY;
    }

    @Override
    @Transactional
    public TransactionResult process(TransactionRequest request, Portfolio portfolio) {
        BigDecimal newUnits = portfolio.getTotalUnits().add(request.getQuantity());
        portfolio.setTotalUnits(newUnits);

        BigDecimal newCost = portfolio.getTotalCost().add(request.getAmount());
        portfolio.setTotalCost(newCost);

        portfolioRepository.save(portfolio);

        return TransactionResult.success(portfolio);
    }
}
