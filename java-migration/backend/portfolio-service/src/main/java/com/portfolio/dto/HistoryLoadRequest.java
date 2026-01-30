package com.portfolio.dto;

import com.portfolio.model.enums.ActionCode;
import com.portfolio.model.enums.HistoryRecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryLoadRequest {

    @NotBlank(message = "Account number is required")
    @Size(max = 10, message = "Account number must be at most 10 characters")
    private String accountNo;

    @NotBlank(message = "Portfolio ID is required")
    @Size(max = 8, message = "Portfolio ID must be at most 8 characters")
    private String portfolioId;

    private LocalDate transDate;
    private LocalTime transTime;

    @Size(max = 2, message = "Transaction type must be at most 2 characters")
    private String transType;

    @Size(max = 10, message = "Security ID must be at most 10 characters")
    private String securityId;

    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fees;
    private BigDecimal totalAmount;
    private BigDecimal costBasis;
    private BigDecimal gainLoss;

    private HistoryRecordType recordType;
    private ActionCode actionCode;
    private String beforeImage;
    private String afterImage;
    private String reasonCode;
    private String processUser;
}
