package com.cognition.portfolio.transaction.web.dto;

import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Update payload, equivalent to a {@code REWRITE TRANSACTION-RECORD}: the key group
 * {@code TRN-KEY} cannot change, only {@code TRN-DATA} and {@code TRN-AUDIT}.
 */
@Schema(name = "UpdateTransactionRequest", description = "REWRITE payload; TRN-KEY is immutable")
public record UpdateTransactionRequest(
    @Schema(description = "TRN-INVESTMENT-ID PIC X(10)", example = "AAPL000001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 10) String investmentId,
    @Schema(description = "TRN-TYPE PIC X(02)", example = "BU", allowableValues = {"BU", "SL", "TR", "FE"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String type,
    @Schema(description = "TRN-QUANTITY PIC S9(11)V9(4) COMP-3", example = "150.0000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull BigDecimal quantity,
    @Schema(description = "TRN-PRICE PIC S9(11)V9(4) COMP-3", example = "187.4500", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull BigDecimal price,
    @Schema(description = "TRN-AMOUNT PIC S9(13)V9(2) COMP-3; derived from quantity x price when omitted (BR-22)",
            example = "28117.50")
        BigDecimal amount,
    @Schema(description = "TRN-CURRENCY PIC X(03)", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 3) String currency,
    @Schema(description = "TRN-PROCESS-DATE PIC X(26)", example = "2024-03-20-09.30.15.123456")
        @Size(max = 26) String processDate,
    @Schema(description = "TRN-PROCESS-USER PIC X(08)", example = "BATCH001") @Size(max = 8) String processUser) {

  /** Carrier entity holding the mutable groups; the service copies them onto the stored record. */
  public PortfolioTransaction toCarrier(BigDecimal resolvedAmount) {
    return PortfolioTransaction.builder()
        .trnInvestmentId(investmentId)
        .trnType(
            TransactionType.fromCode(type)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Type: " + type)))
        .trnQuantity(quantity)
        .trnPrice(price)
        .trnAmount(resolvedAmount)
        .trnCurrency(currency)
        .trnProcessDate(processDate)
        .trnProcessUser(processUser)
        .build();
  }
}
