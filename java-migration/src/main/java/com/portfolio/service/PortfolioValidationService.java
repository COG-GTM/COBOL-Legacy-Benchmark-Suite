package com.portfolio.service;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.TransactionRecord;
import com.portfolio.util.PortfolioValidation;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PortfolioValidationService {

    public List<String> validatePortfolio(Portfolio portfolio) {
        return PortfolioValidation.validatePortfolio(portfolio);
    }

    public List<String> validateTransaction(TransactionRecord transaction) {
        return PortfolioValidation.validateTransaction(transaction);
    }

    public boolean isValidPortfolioId(String portfolioId) {
        return portfolioId != null && !portfolioId.isBlank() && portfolioId.length() <= 8;
    }

    public boolean isValidAccountNo(String accountNo) {
        return accountNo != null && !accountNo.isBlank() && accountNo.length() <= 10;
    }
}
