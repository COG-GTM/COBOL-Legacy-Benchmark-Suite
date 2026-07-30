package com.cognition.portfolio.transaction.web.dto;

import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** API representation of {@code 01 TRANSACTION-RECORD} ({@code TRNREC.cpy}). */
@Schema(name = "Transaction", description = "Portfolio transaction (COBOL copybook TRNREC, VSAM file TRANHIST)")
public record TransactionResponse(
    @Schema(description = "28 character VSAM key TRN-KEY", example = "20240320093015PORT0001000001")
        String transactionKey,
    @Schema(description = "TRN-DATE PIC X(08), YYYYMMDD", example = "20240320") String transactionDate,
    @Schema(description = "TRN-TIME PIC X(06), HHMMSS", example = "093015") String transactionTime,
    @Schema(description = "TRN-PORTFOLIO-ID PIC X(08)", example = "PORT0001") String portfolioId,
    @Schema(description = "TRN-SEQUENCE-NO PIC X(06)", example = "000001") String sequenceNo,
    @Schema(description = "TRN-INVESTMENT-ID PIC X(10)", example = "AAPL000001") String investmentId,
    @Schema(description = "TRN-TYPE PIC X(02): BU, SL, TR, FE", example = "BU") String type,
    @Schema(description = "TRN-QUANTITY PIC S9(11)V9(4) COMP-3", example = "150.0000") BigDecimal quantity,
    @Schema(description = "TRN-PRICE PIC S9(11)V9(4) COMP-3", example = "187.4500") BigDecimal price,
    @Schema(description = "TRN-AMOUNT PIC S9(13)V9(2) COMP-3", example = "28117.50") BigDecimal amount,
    @Schema(description = "TRN-CURRENCY PIC X(03)", example = "USD") String currency,
    @Schema(description = "TRN-STATUS PIC X(01): P, D, F, R", example = "P") String status,
    @Schema(description = "TRN-PROCESS-DATE PIC X(26)", example = "2024-03-20-09.30.15.123456")
        String processDate,
    @Schema(description = "TRN-PROCESS-USER PIC X(08)", example = "BATCH001") String processUser) {

  public static TransactionResponse from(PortfolioTransaction transaction) {
    return new TransactionResponse(
        transaction.getTrnKey().toKeyString(),
        transaction.getTrnKey().getTrnDate(),
        transaction.getTrnKey().getTrnTime(),
        transaction.getTrnKey().getTrnPortfolioId(),
        transaction.getTrnKey().getTrnSequenceNo(),
        transaction.getTrnInvestmentId(),
        transaction.getTrnType().getCode(),
        transaction.getTrnQuantity(),
        transaction.getTrnPrice(),
        transaction.getTrnAmount(),
        transaction.getTrnCurrency(),
        transaction.getTrnStatus().getCode(),
        transaction.getTrnProcessDate(),
        transaction.getTrnProcessUser());
  }
}
