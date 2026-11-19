package com.clbs.portfolio.web.dto;

import com.clbs.portfolio.service.TransactionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Transaction (TRNREC). type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee.")
public record TransactionRequest(
        @Size(max = 8) String portfolioId,
        @Size(max = 10) String investmentId,
        @Size(max = 2) String type,
        @Digits(integer = 11, fraction = 4) BigDecimal quantity,
        @Digits(integer = 11, fraction = 4) BigDecimal price,
        @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(min = 3, max = 3) String currency) {

    public TransactionCommand toCommand() {
        return new TransactionCommand(portfolioId, investmentId, type, quantity, price, amount, currency);
    }
}
