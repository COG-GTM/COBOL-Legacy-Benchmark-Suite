package com.portfolio.service;

import com.portfolio.dto.PortfolioPositionResponse;
import com.portfolio.dto.TransactionHistoryResponse;
import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class InquiryService {

    private final InvestmentPositionRepository positionRepository;
    private final TransactionHistoryRepository transactionRepository;

    public InquiryService(InvestmentPositionRepository positionRepository,
                          TransactionHistoryRepository transactionRepository) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<PortfolioPositionResponse> getPortfolioPosition(String accountNo) {
        List<InvestmentPosition> positions = positionRepository.findByAccountNo(accountNo);
        if (positions.isEmpty()) {
            throw new PortfolioNotFoundException("No positions found for account: " + accountNo);
        }

        return positions.stream()
                .map(pos -> new PortfolioPositionResponse(
                        accountNo,
                        pos.getInvestmentId(),
                        pos.getInvestmentId(),
                        pos.getQuantity(),
                        pos.getCostBasis(),
                        pos.getMarketValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TransactionHistoryResponse> getTransactionHistory(String accountNo,
                                                                   Pageable pageable) {
        Page<TransactionHistory> transactions = transactionRepository.findByAccountNo(
                accountNo, pageable);

        return transactions.map(txn -> new TransactionHistoryResponse(
                txn.getTransactionDate(),
                txn.getTransactionType().getCode(),
                txn.getQuantity(),
                txn.getPrice(),
                txn.getAmount()));
    }
}
