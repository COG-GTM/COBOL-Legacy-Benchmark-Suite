package com.portfolio.dto;

import com.portfolio.validation.ValidPortfolioId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for transaction processing.
 * Validation rules translated from PORTTRAN.cbl paragraphs 2110-2130.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    /** Validated by 2110-CHECK-PORTFOLIO: must not be SPACES, must exist in portfolio file. */
    @ValidPortfolioId
    private String portfolioId;

    /** TRN-INVESTMENT-ID — PIC X(10). */
    @NotBlank(message = "Investment ID is required")
    @Size(max = 10, message = "Investment ID must not exceed 10 characters")
    private String investmentId;

    /** Validated by 2120-CHECK-TRANSACTION-TYPE: BU, SL, TR, or FE. */
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "BU|SL|TR|FE", message = "Transaction type must be BU (Buy), SL (Sell), TR (Transfer), or FE (Fee)")
    private String transactionType;

    /** Validated by 2130-CHECK-AMOUNTS: must be > 0. */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    /** Validated by 2130-CHECK-AMOUNTS: must be > 0 (except for transfers). */
    @NotNull(message = "Price is required")
    private BigDecimal price;

    /** Validated by 2130-CHECK-AMOUNTS: must be > 0 (except for transfers). */
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    /** TRN-CURRENCY — PIC X(03). Defaults to USD. */
    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency = "USD";
}
