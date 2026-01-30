package com.portfolio.service;

import com.portfolio.dto.PositionResponse;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Position;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.model.enums.PositionStatus;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionService {

    private final PositionRepository positionRepository;
    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PositionResponse getPositionByAccountNo(String accountNo, String userId) {
        log.info("Querying position for account: {}", accountNo);

        Portfolio portfolio = portfolioRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> {
                    auditService.createInquiryAudit(null, accountNo, userId, 
                            AuditStatus.FAILURE, "Position not found for account");
                    return new PortfolioNotFoundException("Position not found for account: " + accountNo);
                });

        PositionResponse response = buildPositionResponseFromPortfolio(portfolio);

        auditService.createInquiryAudit(portfolio.getPortfolioId(), accountNo, userId, 
                AuditStatus.SUCCESS, "Position inquiry successful");

        log.info("Position found for account: {}", accountNo);
        return response;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> getPositionsByPortfolioId(String portfolioId, String userId) {
        log.info("Querying positions for portfolio: {}", portfolioId);

        if (!portfolioRepository.existsByPortfolioId(portfolioId)) {
            auditService.createInquiryAudit(portfolioId, null, userId, 
                    AuditStatus.FAILURE, "Portfolio not found");
            throw new PortfolioNotFoundException(portfolioId, true);
        }

        List<Position> positions = positionRepository.findByPortfolioId(portfolioId);

        auditService.createInquiryAudit(portfolioId, null, userId, 
                AuditStatus.SUCCESS, "Position inquiry successful - " + positions.size() + " positions found");

        return positions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PositionResponse> getPositionByPortfolioAndInvestment(
            String portfolioId, String investmentId, String userId) {
        log.info("Querying position for portfolio: {}, investment: {}", portfolioId, investmentId);

        Optional<Position> position = positionRepository.findByPortfolioIdAndInvestmentId(portfolioId, investmentId);

        if (position.isPresent()) {
            auditService.createInquiryAudit(portfolioId, null, userId, 
                    AuditStatus.SUCCESS, "Position inquiry successful for investment: " + investmentId);
            return Optional.of(mapToResponse(position.get()));
        } else {
            auditService.createInquiryAudit(portfolioId, null, userId, 
                    AuditStatus.WARNING, "Position not found for investment: " + investmentId);
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> getActivePositions(String portfolioId) {
        return positionRepository.findByPortfolioIdAndStatus(portfolioId, PositionStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> getAllPositions() {
        return positionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PositionResponse createOrUpdatePosition(Position position) {
        Position saved = positionRepository.save(position);
        return mapToResponse(saved);
    }

    private PositionResponse buildPositionResponseFromPortfolio(Portfolio portfolio) {
        return PositionResponse.builder()
                .portfolioId(portfolio.getPortfolioId())
                .positionDate(portfolio.getLastMaintDate())
                .quantity(portfolio.getTotalUnits())
                .costBasis(portfolio.getTotalCost())
                .marketValue(portfolio.getTotalValue())
                .currency("USD")
                .status(portfolio.getStatus() == com.portfolio.model.enums.PortfolioStatus.ACTIVE 
                        ? PositionStatus.ACTIVE : PositionStatus.CLOSED)
                .lastMaintDate(portfolio.getLastMaintDate() != null 
                        ? portfolio.getLastMaintDate().atStartOfDay() : null)
                .lastMaintUser(portfolio.getLastUser())
                .build();
    }

    private PositionResponse mapToResponse(Position position) {
        return PositionResponse.builder()
                .id(position.getId())
                .portfolioId(position.getPortfolioId())
                .positionDate(position.getPositionDate())
                .investmentId(position.getInvestmentId())
                .quantity(position.getQuantity())
                .costBasis(position.getCostBasis())
                .marketValue(position.getMarketValue())
                .currency(position.getCurrency())
                .status(position.getStatus())
                .lastMaintDate(position.getLastMaintDate())
                .lastMaintUser(position.getLastMaintUser())
                .build();
    }
}
