package com.clbs.portfolio.web;

import com.clbs.portfolio.domain.Portfolio;
import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.service.DeleteReason;
import com.clbs.portfolio.service.PortfolioService;
import com.clbs.portfolio.web.dto.ErrorResponse;
import com.clbs.portfolio.web.dto.PortfolioRequest;
import com.clbs.portfolio.web.dto.PortfolioResponse;
import com.clbs.portfolio.web.dto.PortfolioUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Portfolios", description = "Portfolio master maintenance (PORTADD, PORTREAD, PORTUPDT, PORTDEL)")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    @Operation(operationId = "listPortfolios", summary = "List all portfolios in key order (PORTREAD)")
    public List<PortfolioResponse> list() {
        return portfolioService.findAll().stream().map(PortfolioResponse::from).toList();
    }

    @GetMapping("/{portfolioId}/accounts/{accountNo}")
    @Operation(operationId = "getPortfolio", summary = "Read one portfolio by key")
    @ApiResponse(responseCode = "200", description = "Portfolio found")
    @ApiResponse(responseCode = "404", description = "Record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PortfolioResponse get(@PathVariable @Size(max = 8) String portfolioId,
                                 @PathVariable @Size(max = 10) String accountNo) {
        return PortfolioResponse.from(portfolioService.get(new PortfolioKey(portfolioId, accountNo)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createPortfolio", summary = "Create a portfolio (PORTADD)")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid record data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Duplicate record",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PortfolioResponse create(@Valid @RequestBody PortfolioRequest request) {
        Portfolio p = new Portfolio(new PortfolioKey(request.portfolioId(), request.accountNo()));
        p.setClientName(request.clientName());
        p.setClientType(request.clientType());
        p.setStatus(request.status());
        p.setTotalValue(scale(request.totalValue()));
        p.setCashBalance(scale(request.cashBalance()));
        return PortfolioResponse.from(portfolioService.create(p));
    }

    @PatchMapping("/{portfolioId}/accounts/{accountNo}")
    @Operation(operationId = "updatePortfolio", summary = "Apply a status, name or value update (PORTUPDT)")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "Record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PortfolioResponse update(@PathVariable @Size(max = 8) String portfolioId,
                                    @PathVariable @Size(max = 10) String accountNo,
                                    @Valid @RequestBody PortfolioUpdateRequest request) {
        return PortfolioResponse.from(portfolioService.update(new PortfolioKey(portfolioId, accountNo),
                request.action().code(), request.newValue()));
    }

    @DeleteMapping("/{portfolioId}/accounts/{accountNo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deletePortfolio", summary = "Delete a portfolio and write an audit record (PORTDEL)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void delete(@PathVariable @Size(max = 8) String portfolioId,
                       @PathVariable @Size(max = 10) String accountNo,
                       @RequestParam DeleteReason reason) {
        portfolioService.delete(new PortfolioKey(portfolioId, accountNo), reason.code());
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.DOWN);
    }
}
