package com.portfolio.dto;

import com.portfolio.model.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "Portfolio ID is required")
    @Size(max = 8, message = "Portfolio ID must be at most 8 characters")
    private String portfolioId;

    @Size(max = 10, message = "Investment ID must be at most 10 characters")
    private String investmentId;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be at most 3 characters")
    private String currency;

    @Size(max = 8, message = "User ID must be at most 8 characters")
    private String userId;
}
