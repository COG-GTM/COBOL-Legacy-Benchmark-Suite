package com.cobolbenchmark.online;

import com.cobolbenchmark.db.PoshistRepository;
import com.cobolbenchmark.model.PoshistRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * History Inquiry Service - migrated from INQHIST.cbl.
 * Retrieves transaction history from DB2 POSHIST table.
 */
@Service
public class HistoryInquiryService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryInquiryService.class);

    private final PoshistRepository poshistRepository;

    public HistoryInquiryService(PoshistRepository poshistRepository) {
        this.poshistRepository = poshistRepository;
    }

    /**
     * Get transaction history for a portfolio.
     * From INQHIST.cbl: P200-READ-HISTORY paragraph.
     * Replaces EXEC SQL cursor operations on POSHIST table.
     */
    public HistoryResponse getTransactionHistory(String portfolioId) {
        logger.info("Retrieving transaction history for portfolio: {}", portfolioId);

        List<PoshistRecord> records = poshistRepository.findByPortfolioIdOrderByDateDesc(portfolioId);

        HistoryResponse response = new HistoryResponse();
        response.setPortfolioId(portfolioId);

        List<HistoryResponse.HistoryDetail> details = new ArrayList<>();
        for (PoshistRecord rec : records) {
            HistoryResponse.HistoryDetail detail = new HistoryResponse.HistoryDetail();
            detail.setTransactionDate(rec.getTransDate() != null ? rec.getTransDate().toString() : "");
            detail.setTransactionTime(rec.getTransTime() != null ? rec.getTransTime().toString() : "");
            detail.setTransactionType(rec.getTransType());
            detail.setSecurityId(rec.getSecurityId());
            detail.setQuantity(rec.getQuantity());
            detail.setPrice(rec.getPrice());
            detail.setAmount(rec.getAmount());
            detail.setFees(rec.getFees());
            detail.setTotalAmount(rec.getTotalAmount());
            details.add(detail);
        }

        response.setTransactions(details);
        response.setTotalRecords(details.size());
        response.setMessage("Transaction history retrieved successfully");

        return response;
    }
}
