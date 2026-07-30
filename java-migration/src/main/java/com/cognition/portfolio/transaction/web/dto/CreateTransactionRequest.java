package com.cognition.portfolio.transaction.web.dto;

import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import com.cognition.portfolio.transaction.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Insert payload, equivalent to building a {@code TRANSACTION-RECORD} before a {@code WRITE}.
 *
 * <p>{@code sequenceNo} is optional: when omitted the service assigns the next value per BR-20
 * ({@code PRCSEQ00 1210-ADD-TO-SEQUENCE}).
 */
@Schema(name = "CreateTransactionRequest", description = "New TRANSACTION-RECORD to write to TRANHIST")
public record CreateTransactionRequest(
    @Schema(description = "TRN-DATE PIC X(08), YYYYMMDD", example = "20240320", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{8}", message = "TRN-DATE must be 8 digits (YYYYMMDD)")
        String transactionDate,
    @Schema(description = "TRN-TIME PIC X(06), HHMMSS", example = "093015", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{6}", message = "TRN-TIME must be 6 digits (HHMMSS)")
        String transactionTime,
    @Schema(description = "TRN-PORTFOLIO-ID PIC X(08)", example = "PORT0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 8) String portfolioId,
    @Schema(description = "TRN-SEQUENCE-NO PIC X(06); assigned automatically when omitted", example = "000001")
        @Pattern(regexp = "\\d{6}", message = "TRN-SEQUENCE-NO must be 6 digits") String sequenceNo,
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
    @Schema(description = "TRN-STATUS PIC X(01); defaults to P (pending)", example = "P",
            allowableValues = {"P", "D", "F", "R"})
        String status,
    @Schema(description = "TRN-PROCESS-DATE PIC X(26)", example = "2024-03-20-09.30.15.123456")
        @Size(max = 26) String processDate,
    @Schema(description = "TRN-PROCESS-USER PIC X(08)", example = "BATCH001") @Size(max = 8) String processUser) {

  /** Builds the entity; {@code type} and {@code status} are resolved against the copybook 88-levels. */
  public PortfolioTransaction toEntity(String resolvedSequenceNo, BigDecimal resolvedAmount) {
    return PortfolioTransaction.builder()
        .trnKey(new TransactionKey(transactionDate, transactionTime, portfolioId, resolvedSequenceNo))
        .trnInvestmentId(investmentId)
        .trnType(
            TransactionType.fromCode(type)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Type: " + type)))
        .trnQuantity(quantity)
        .trnPrice(price)
        .trnAmount(resolvedAmount)
        .trnCurrency(currency)
        .trnStatus(
            status == null || status.isBlank()
                ? TransactionStatus.PENDING
                : TransactionStatus.fromCode(status)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Status: " + status)))
        .trnProcessDate(processDate)
        .trnProcessUser(processUser)
        .build();
  }
}
