package com.portfolio.service.online;

import com.portfolio.domain.Position;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Portfolio Inquiry Service - migrated from COBOL INQPORT.cbl.
 * Replaces: EXEC CICS READ FILE('POSFILE') -> positionRepository.findByPortfolioId()
 * COMMAREA data passing -> method parameters/return types.
 */
@Service
public class PortfolioInquiryService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioInquiryService.class);

    private final PositionRepository positionRepository;

    public PortfolioInquiryService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public List<Position> getPortfolioPositions(String portfolioId) {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            throw new ValidationException("Portfolio ID is required");
        }

        log.debug("Inquiring positions for portfolio: {}", portfolioId);
        List<Position> positions = positionRepository.findActiveByPortfolioId(portfolioId);

        if (positions.isEmpty()) {
            log.info("No active positions found for portfolio: {}", portfolioId);
        }

        return positions;
    }

    @Transactional(readOnly = true)
    public List<Position> getPositionsByAccountNo(String accountNo) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            throw new ValidationException("Account number is required");
        }

        return positionRepository.findByPortfolioId(accountNo);
    }
}
