package com.portfolio.service;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Position;
import com.portfolio.exception.PortfolioException;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Portfolio Service - migrated from COBOL INQPORT
 * Handles portfolio inquiries and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    public Portfolio getPortfolioById(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioException(
                        "Portfolio not found: " + portfolioId,
                        PortfolioException.ErrorCode.NOT_FOUND));
    }

    public Portfolio getPortfolioByAccountNo(String accountNo) {
        return portfolioRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new PortfolioException(
                        "Portfolio not found for account: " + accountNo,
                        PortfolioException.ErrorCode.NOT_FOUND));
    }

    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    public List<Portfolio> getActivePortfolios() {
        return portfolioRepository.findByStatus(Portfolio.PortfolioStatus.A);
    }

    @Transactional
    public Portfolio createPortfolio(Portfolio portfolio) {
        if (portfolioRepository.existsByAccountNo(portfolio.getAccountNo())) {
            throw new PortfolioException(
                    "Account already exists: " + portfolio.getAccountNo(),
                    PortfolioException.ErrorCode.DUPLICATE_KEY);
        }
        
        portfolio.setStatus(Portfolio.PortfolioStatus.A);
        portfolio.setCreateDate(LocalDate.now());
        portfolio.setTotalValue(BigDecimal.ZERO);
        portfolio.setCashBalance(BigDecimal.ZERO);
        
        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Portfolio created: id={}, account={}", saved.getPortfolioId(), saved.getAccountNo());
        return saved;
    }

    @Transactional
    public Portfolio updatePortfolio(String portfolioId, Portfolio updates) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        
        if (updates.getClientName() != null) {
            portfolio.setClientName(updates.getClientName());
        }
        if (updates.getClientType() != null) {
            portfolio.setClientType(updates.getClientType());
        }
        if (updates.getCashBalance() != null) {
            portfolio.setCashBalance(updates.getCashBalance());
        }
        
        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Portfolio updated: id={}", portfolioId);
        return saved;
    }

    @Transactional
    public void closePortfolio(String portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        
        List<Position> activePositions = positionRepository.findByPortfolioIdAndStatus(
                portfolioId, Position.PositionStatus.A);
        
        if (!activePositions.isEmpty()) {
            throw new PortfolioException(
                    "Cannot close portfolio with active positions",
                    PortfolioException.ErrorCode.VALIDATION_ERROR);
        }
        
        portfolio.setStatus(Portfolio.PortfolioStatus.C);
        portfolioRepository.save(portfolio);
        log.info("Portfolio closed: id={}", portfolioId);
    }

    public List<Position> getPortfolioPositions(String portfolioId) {
        getPortfolioById(portfolioId);
        return positionRepository.findActivePositionsByPortfolio(portfolioId);
    }

    public PortfolioSummary getPortfolioSummary(String portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        List<Position> positions = positionRepository.findActivePositionsByPortfolio(portfolioId);
        
        BigDecimal totalMarketValue = positions.stream()
                .map(Position::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCostBasis = positions.stream()
                .map(Position::getCostBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal unrealizedGainLoss = totalMarketValue.subtract(totalCostBasis);
        
        PortfolioSummary summary = new PortfolioSummary();
        summary.setPortfolioId(portfolioId);
        summary.setAccountNo(portfolio.getAccountNo());
        summary.setClientName(portfolio.getClientName());
        summary.setStatus(portfolio.getStatus().name());
        summary.setTotalMarketValue(totalMarketValue);
        summary.setTotalCostBasis(totalCostBasis);
        summary.setCashBalance(portfolio.getCashBalance());
        summary.setUnrealizedGainLoss(unrealizedGainLoss);
        summary.setPositionCount(positions.size());
        
        return summary;
    }

    public static class PortfolioSummary {
        private String portfolioId;
        private String accountNo;
        private String clientName;
        private String status;
        private BigDecimal totalMarketValue;
        private BigDecimal totalCostBasis;
        private BigDecimal cashBalance;
        private BigDecimal unrealizedGainLoss;
        private int positionCount;

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getTotalMarketValue() { return totalMarketValue; }
        public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }
        public BigDecimal getTotalCostBasis() { return totalCostBasis; }
        public void setTotalCostBasis(BigDecimal totalCostBasis) { this.totalCostBasis = totalCostBasis; }
        public BigDecimal getCashBalance() { return cashBalance; }
        public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
        public BigDecimal getUnrealizedGainLoss() { return unrealizedGainLoss; }
        public void setUnrealizedGainLoss(BigDecimal unrealizedGainLoss) { this.unrealizedGainLoss = unrealizedGainLoss; }
        public int getPositionCount() { return positionCount; }
        public void setPositionCount(int positionCount) { this.positionCount = positionCount; }
    }
}
