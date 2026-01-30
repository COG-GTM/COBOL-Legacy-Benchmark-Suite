package com.portfolio.dto;

import com.portfolio.model.enums.TransactionStatus;
import com.portfolio.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private String portfolioId;
    private String sequenceNo;
    private String investmentId;
    private TransactionType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime processDate;
    private String processUser;
    private String message;
}
