package com.cobolbenchmark.online;

import com.cobolbenchmark.common.RecordNotFoundException;
import com.cobolbenchmark.db.PositionRepository;
import com.cobolbenchmark.db.PortfolioMasterRepository;
import com.cobolbenchmark.model.PortfolioMaster;
import com.cobolbenchmark.model.PositionRecord;
import com.cobolbenchmark.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Inquiry Service - migrated from INQONLN.cbl.
 * Handles portfolio inquiry business logic.
 * Replaces EXEC CICS LINK/RETURN with method calls.
 */
@Service
public class InquiryService {

    private static final Logger logger = LoggerFactory.getLogger(InquiryService.class);

    private final PositionRepository positionRepository;
    private final PortfolioMasterRepository portfolioMasterRepository;
    private final SecurityService securityService;

    public InquiryService(PositionRepository positionRepository,
                          PortfolioMasterRepository portfolioMasterRepository,
                          SecurityService securityService) {
        this.positionRepository = positionRepository;
        this.portfolioMasterRepository = portfolioMasterRepository;
        this.securityService = securityService;
    }

    /**
     * Process inquiry - main business logic from INQONLN.cbl.
     * Replaces P100-PROCESS-INQUIRY paragraph.
     */
    public PositionResponse processInquiry(InquiryRequest request) {
        logger.info("Processing inquiry for portfolio: {}", request.getPortfolioId());

        // P050-SECURITY-CHECK: Three chained EXEC CICS LINK PROGRAM('SECMGR')
        securityService.performSecurityCheck(
                request.getUserId(),
                request.getPassword(),
                request.getPortfolioId(),
                "PORTFOLIO"
        );

        // Fetch portfolio master
        PortfolioMaster portfolio = portfolioMasterRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new RecordNotFoundException("Portfolio", request.getPortfolioId()));

        // Fetch positions - replaces EXEC CICS READ FILE('POSFILE')
        List<PositionRecord> positions = positionRepository.findByPortfolioId(request.getPortfolioId());

        // Build response - replaces EXEC CICS SEND MAP('POSMAP')
        return buildPositionResponse(portfolio, positions);
    }

    private PositionResponse buildPositionResponse(PortfolioMaster portfolio, List<PositionRecord> positions) {
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
        response.setMessage("Inquiry completed successfully");

        return response;
    }
}
