package com.portfolio.dto;

import com.portfolio.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for transaction operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private LocalDate transactionDate;
    private String transactionTime;
    private String portfolioId;
    private String sequenceNo;
    private String investmentId;
    private String transactionType;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime processDate;
    private String processUser;

    public static TransactionResponse fromEntity(Transaction entity) {
        TransactionResponse response = new TransactionResponse();
        response.setId(entity.getId());
        response.setTransactionDate(entity.getTransactionDate());
        response.setTransactionTime(entity.getTransactionTime());
        response.setPortfolioId(entity.getPortfolioId());
        response.setSequenceNo(entity.getSequenceNo());
        response.setInvestmentId(entity.getInvestmentId());
        response.setTransactionType(entity.getTransactionType());
        response.setQuantity(entity.getQuantity());
        response.setPrice(entity.getPrice());
        response.setAmount(entity.getAmount());
        response.setCurrency(entity.getCurrency());
        response.setStatus(entity.getStatus());
        response.setProcessDate(entity.getProcessDate());
        response.setProcessUser(entity.getProcessUser());
        return response;
    }
}
