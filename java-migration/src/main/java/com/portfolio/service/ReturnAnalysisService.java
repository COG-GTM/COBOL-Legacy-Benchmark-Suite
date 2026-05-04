package com.portfolio.service;

import com.portfolio.entity.PositionRecord;
import com.portfolio.entity.TransactionRecord;
import com.portfolio.repository.PositionRepository;
import com.portfolio.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReturnAnalysisService {

    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;

    public ReturnAnalysisService(PositionRepository positionRepository,
                                 TransactionRepository transactionRepository) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
    }

    public Map<String, Object> analyzePortfolioReturns(String portfolioId) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        List<PositionRecord> positions = positionRepository.findActivePositions(portfolioId);

        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;

        for (PositionRecord pos : positions) {
            totalCostBasis = totalCostBasis.add(pos.getCostBasis());
            totalMarketValue = totalMarketValue.add(pos.getMarketValue());
        }

        BigDecimal totalGainLoss = totalMarketValue.subtract(totalCostBasis);
        BigDecimal returnPct = BigDecimal.ZERO;
        if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
            returnPct = totalGainLoss.divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        analysis.put("portfolioId", portfolioId);
        analysis.put("positionCount", positions.size());
        analysis.put("totalCostBasis", totalCostBasis);
        analysis.put("totalMarketValue", totalMarketValue);
        analysis.put("totalGainLoss", totalGainLoss);
        analysis.put("returnPercentage", returnPct);

        List<TransactionRecord> transactions = transactionRepository
                .findHistoryByPortfolioId(portfolioId);
        analysis.put("transactionCount", transactions.size());

        long buyCount = transactions.stream().filter(TransactionRecord::isBuy).count();
        long sellCount = transactions.stream().filter(TransactionRecord::isSell).count();
        analysis.put("buyTransactions", buyCount);
        analysis.put("sellTransactions", sellCount);

        return analysis;
    }
}
