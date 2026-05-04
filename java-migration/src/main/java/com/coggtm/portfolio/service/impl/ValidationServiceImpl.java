package com.coggtm.portfolio.service.impl;

import com.coggtm.portfolio.service.ValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ValidationServiceImpl implements ValidationService {

    private static final String PORTFOLIO_ID_PREFIX = "PORT";
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

    @Override
    public boolean validatePortfolioId(String portfolioId) {
        // TODO: Migrate full validation from PORTVALD.cbl
        if (portfolioId == null || portfolioId.length() != 8) {
            return false;
        }
        return portfolioId.startsWith(PORTFOLIO_ID_PREFIX);
    }

    @Override
    public boolean validateAccount(String accountNo) {
        // TODO: Migrate full validation from PORTVALD.cbl
        return accountNo != null && !accountNo.isBlank() && accountNo.length() <= 10;
    }

    @Override
    public boolean validateInvestmentType(String investmentType) {
        // TODO: Migrate full validation from PORTVALD.cbl
        return investmentType != null
                && (investmentType.equals("STK")
                    || investmentType.equals("BND")
                    || investmentType.equals("MMF")
                    || investmentType.equals("ETF"));
    }

    @Override
    public boolean validateAmount(BigDecimal amount) {
        // TODO: Migrate full validation from PORTVALD.cbl (VAL-MIN-AMOUNT / VAL-MAX-AMOUNT)
        if (amount == null) {
            return false;
        }
        return amount.compareTo(MIN_AMOUNT) >= 0 && amount.compareTo(MAX_AMOUNT) <= 0;
    }
}
