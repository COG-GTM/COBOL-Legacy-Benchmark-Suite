package com.portfolio.service;

import com.portfolio.dto.PortfolioResponse;
import com.portfolio.dto.PositionResponse;
import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.exception.ResourceNotFoundException;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/**
 * Portfolio service - replaces INQPORT.cbl portfolio inquiry logic.
 * Source: src/programs/online/INQPORT.cbl
 *
 * Implements:
 * - P200-GET-POSITION: Portfolio position lookup via VSAM → now JPA query
 * - P300-FORMAT-DISPLAY: Format position data for display → now DTO mapping
 * - P900-NOT-FOUND: Position not found handling → now ResourceNotFoundException
 */
@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioMasterRepository portfolioRepository;
    private final InvestmentPositionRepository positionRepository;

    public PortfolioService(PortfolioMasterRepository portfolioRepository,
                           InvestmentPositionRepository positionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Get portfolio by ID with positions - replaces INQPORT P200-GET-POSITION.
     */
    public PortfolioResponse getPortfolioById(String portfolioId) {
        PortfolioMaster portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "portfolioId", portfolioId));

        List<InvestmentPosition> positions = positionRepository.findActivePositions(portfolioId);
        List<PositionResponse> positionDtos = positions.stream()
                .map(PositionResponse::fromEntity)
                .toList();

        PortfolioResponse response = PortfolioResponse.fromEntity(portfolio);
        response.setPositions(positionDtos);

        // Calculate totals
        BigDecimal totalMarketValue = positions.stream()
                .map(InvestmentPosition::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCostBasis = positions.stream()
                .map(InvestmentPosition::getCostBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalMarketValue(totalMarketValue);
        response.setTotalCostBasis(totalCostBasis);
        response.setTotalGainLoss(totalMarketValue.subtract(totalCostBasis));

        return response;
    }

    /**
     * Get all portfolios - supports the main menu listing.
     */
    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioRepository.findAll().stream()
                .map(PortfolioResponse::fromEntity)
                .toList();
    }

    /**
     * Get active portfolios - replaces DB2 ACTIVE_PORTFOLIOS view.
     */
    public List<PortfolioResponse> getActivePortfolios() {
        return portfolioRepository.findActivePortfolios().stream()
                .map(PortfolioResponse::fromEntity)
                .toList();
    }

    /**
     * Get portfolios by client ID.
     */
    public List<PortfolioResponse> getPortfoliosByClientId(String clientId) {
        return portfolioRepository.findByClientId(clientId).stream()
                .map(PortfolioResponse::fromEntity)
                .toList();
    }
}
