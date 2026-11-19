package com.clbs.portfolio.web;

import com.clbs.portfolio.service.InquiryService;
import com.clbs.portfolio.web.dto.ErrorResponse;
import com.clbs.portfolio.web.dto.HistoryResponse;
import com.clbs.portfolio.web.dto.PositionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inquiry/accounts/{accountNo}")
@Tag(name = "Inquiry", description = "Online inquiries replacing the CICS INQONLN menu (INQPORT, INQHIST)")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping("/positions")
    @Operation(operationId = "getAccountPositions", summary = "Positions held under an account (INQPORT)")
    @ApiResponse(responseCode = "200", description = "Positions")
    @ApiResponse(responseCode = "404", description = "Position not found for account",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<PositionResponse> positions(@PathVariable @Size(max = 10) String accountNo) {
        return inquiryService.positionsForAccount(accountNo).stream().map(PositionResponse::from).toList();
    }

    @GetMapping("/history")
    @Operation(operationId = "getAccountHistory", summary = "Last 10 history rows for an account, newest first (INQHIST)")
    public List<HistoryResponse> history(@PathVariable @Size(max = 10) String accountNo) {
        return inquiryService.historyForAccount(accountNo).stream().map(HistoryResponse::from).toList();
    }
}
