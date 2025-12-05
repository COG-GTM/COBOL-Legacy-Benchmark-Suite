package com.portfolio.modernization.service;

import com.portfolio.modernization.model.entity.Portfolio;
import com.portfolio.modernization.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public List<Portfolio> findAll() {
        return portfolioRepository.findAll();
    }

    public Optional<Portfolio> findById(String portfolioId) {
        return portfolioRepository.findById(portfolioId);
    }

    public Optional<Portfolio> findByAccountNumber(String accountNumber) {
        return portfolioRepository.findByAccountNumber(accountNumber);
    }

    public List<Portfolio> findByClientId(String clientId) {
        return portfolioRepository.findByClientId(clientId);
    }

    public List<Portfolio> findActivePortfolios() {
        return portfolioRepository.findActivePortfolios(Portfolio.PortfolioStatus.A);
    }

    public List<Portfolio> findByStatus(Portfolio.PortfolioStatus status) {
        return portfolioRepository.findByStatus(status);
    }

    @Transactional
    public Portfolio save(Portfolio portfolio) {
        log.info("Saving portfolio: {}", portfolio.getPortfolioId());
        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public void deleteById(String portfolioId) {
        log.info("Deleting portfolio: {}", portfolioId);
        portfolioRepository.deleteById(portfolioId);
    }
}
