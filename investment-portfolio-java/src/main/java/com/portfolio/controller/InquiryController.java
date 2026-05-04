package com.portfolio.controller;

import com.portfolio.dto.MenuResponse;
import com.portfolio.dto.PortfolioPositionResponse;
import com.portfolio.dto.TransactionHistoryResponse;
import com.portfolio.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/inquiry")
@Tag(name = "Inquiry", description = "Portfolio and transaction history inquiry endpoints")
public class InquiryController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping("/menu")
    @Operation(summary = "Get inquiry menu options")
    public ResponseEntity<MenuResponse> getMenu() {
        MenuResponse menu = new MenuResponse(
                "Portfolio Management System",
                List.of(
                        "1. Portfolio Position Inquiry",
                        "2. Transaction History",
                        "3. Exit"
                )
        );
        return ResponseEntity.ok(menu);
    }

    @GetMapping("/portfolio/{accountNo}")
    @Operation(summary = "Inquire portfolio positions by account number")
    public ResponseEntity<List<PortfolioPositionResponse>> getPortfolioPositions(
            @PathVariable("accountNo") String accountNo) {
        List<PortfolioPositionResponse> positions = inquiryService.getPortfolioPosition(accountNo);
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/history/{accountNo}")
    @Operation(summary = "Inquire transaction history by account number")
    public ResponseEntity<Page<TransactionHistoryResponse>> getTransactionHistory(
            @PathVariable("accountNo") String accountNo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionHistoryResponse> history = inquiryService.getTransactionHistory(
                accountNo, pageable);
        return ResponseEntity.ok(history);
    }
}
