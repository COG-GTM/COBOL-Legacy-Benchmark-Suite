package com.portfolio.service.inquiry;

import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Position;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PortfolioInquiryService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioInquiryService.class);

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    public PortfolioInquiryService(PortfolioRepository portfolioRepository,
                                   PositionRepository positionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public Portfolio lookupByPortfolioId(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

    @Transactional(readOnly = true)
    public Portfolio lookupByClientId(String clientId) {
        return portfolioRepository.findByClientId(clientId)
                .orElseThrow(() -> new PortfolioNotFoundException("client: " + clientId));
    }

    @Transactional(readOnly = true)
    public List<Position> getPositions(String portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new PortfolioNotFoundException(portfolioId);
        }
        return positionRepository.findByPortfolioId(portfolioId);
    }
}
