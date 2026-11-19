package com.clbs.portfolio.web;

import com.clbs.portfolio.service.TransactionService;
import com.clbs.portfolio.web.dto.ErrorResponse;
import com.clbs.portfolio.web.dto.TransactionRequest;
import com.clbs.portfolio.web.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transactions", description = "Transaction processing (PORTTRAN)")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "postTransaction", summary = "Validate and apply a BU/SL/TR/FE transaction")
    @ApiResponse(responseCode = "201", description = "Transaction applied (status D)")
    @ApiResponse(responseCode = "400", description = "Validation failed; nothing stored",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Rejected during processing; stored with status F",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TransactionResponse post(@Valid @RequestBody TransactionRequest request) {
        return TransactionResponse.from(transactionService.process(request.toCommand()));
    }

    @GetMapping("/portfolios/{portfolioId}/transactions")
    @Operation(operationId = "listTransactions", summary = "Transactions of a portfolio, newest first")
    @ApiResponse(responseCode = "200", description = "Transactions")
    @ApiResponse(responseCode = "404", description = "Unknown portfolio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<TransactionResponse> list(@PathVariable @Size(max = 8) String portfolioId) {
        return transactionService.findByPortfolio(portfolioId).stream().map(TransactionResponse::from).toList();
    }
}
