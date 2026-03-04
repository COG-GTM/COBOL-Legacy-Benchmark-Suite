package com.cobolbenchmark.online;

import com.cobolbenchmark.common.RecordNotFoundException;
import com.cobolbenchmark.db.PositionRepository;
import com.cobolbenchmark.db.PortfolioMasterRepository;
import com.cobolbenchmark.model.PortfolioMaster;
import com.cobolbenchmark.model.PositionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio Inquiry Service - migrated from INQPORT.cbl.
 * Replaces EXEC CICS READ FILE('POSFILE') with repository calls.
 * MOVE DFHCOMMAREA TO WS-COMMAREA → accepting request DTO parameter.
 */
@Service
public class PortfolioInquiryService {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioInquiryService.class);

    private final PositionRepository positionRepository;
    private final PortfolioMasterRepository portfolioMasterRepository;

    public PortfolioInquiryService(PositionRepository positionRepository,
                                    PortfolioMasterRepository portfolioMasterRepository) {
        this.positionRepository = positionRepository;
        this.portfolioMasterRepository = portfolioMasterRepository;
    }

    /**
     * Get portfolio positions - replaces EXEC CICS READ FILE('POSFILE').
     * From INQPORT.cbl: P200-READ-POSITIONS paragraph.
     */
    public PositionResponse getPortfolioPositions(String portfolioId) {
        logger.info("Retrieving positions for portfolio: {}", portfolioId);

        PortfolioMaster portfolio = portfolioMasterRepository.findById(portfolioId)
                .orElseThrow(() -> new RecordNotFoundException("Portfolio", portfolioId));

        List<PositionRecord> positions = positionRepository.findByPortfolioId(portfolioId);

        PositionResponse response = new PositionResponse();
        response.setPortfolioId(portfolio.getPortfolioId());
        response.setPortfolioName(portfolio.getPortfolioName());
        response.setStatus(portfolio.getStatus());

        List<PositionResponse.PositionDetail> details = new ArrayList<>();
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;

        for (PositionRecord pos : positions) {
            PositionResponse.PositionDetail detail = new PositionResponse.PositionDetail();
            detail.setInvestmentId(pos.getInvestmentId());
            detail.setInvestmentType(pos.getInvestmentType());
            detail.setPositionDate(pos.getPositionDate() != null ? pos.getPositionDate().toString() : "");
            detail.setQuantity(pos.getQuantity());
            detail.setCostBasis(pos.getCostBasis());
            detail.setMarketValue(pos.getMarketValue());
            detail.setGainLoss(pos.getMarketValue().subtract(pos.getCostBasis()));
            detail.setStatus(pos.getStatus());
            details.add(detail);

            totalMarketValue = totalMarketValue.add(pos.getMarketValue());
            totalCostBasis = totalCostBasis.add(pos.getCostBasis());
        }

        response.setPositions(details);
        response.setTotalMarketValue(totalMarketValue);
        response.setTotalCostBasis(totalCostBasis);
        response.setTotalGainLoss(totalMarketValue.subtract(totalCostBasis));
        response.setMessage("Portfolio positions retrieved successfully");

        return response;
    }
}
