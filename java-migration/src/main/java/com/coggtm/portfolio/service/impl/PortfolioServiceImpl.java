package com.coggtm.portfolio.service.impl;

import com.coggtm.portfolio.domain.Portfolio;
import com.coggtm.portfolio.repository.PortfolioRepository;
import com.coggtm.portfolio.service.PortfolioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public Portfolio createPortfolio(Portfolio portfolio) {
        // TODO: Migrate business logic from PORTMSTR.cbl 1000-CREATE-PORTFOLIO
        return portfolioRepository.save(portfolio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Portfolio> readPortfolio(String portfolioId) {
        // TODO: Migrate business logic from PORTMSTR.cbl 2000-READ-PORTFOLIO
        return portfolioRepository.findById(portfolioId);
    }

    @Override
    public Portfolio updatePortfolio(Portfolio portfolio) {
        // TODO: Migrate business logic from PORTMSTR.cbl 3000-UPDATE-PORTFOLIO
        return portfolioRepository.save(portfolio);
    }

    @Override
    public void deletePortfolio(String portfolioId) {
        // TODO: Migrate business logic from PORTMSTR.cbl 4000-DELETE-PORTFOLIO
        portfolioRepository.deleteById(portfolioId);
    }
}
