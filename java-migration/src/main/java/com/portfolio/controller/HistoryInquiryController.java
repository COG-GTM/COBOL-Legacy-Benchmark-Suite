package com.portfolio.controller;

import com.portfolio.entity.TransactionRecord;
import com.portfolio.service.PortfolioMasterService;
import com.portfolio.service.PortfolioTransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/inquiry/history")
public class HistoryInquiryController {

    private final PortfolioTransactionService transactionService;
    private final PortfolioMasterService portfolioService;

    public HistoryInquiryController(PortfolioTransactionService transactionService,
                                    PortfolioMasterService portfolioService) {
        this.transactionService = transactionService;
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public String showHistoryForm(Model model) {
        model.addAttribute("portfolios", portfolioService.findAllPortfolios());
        return "history-inquiry";
    }

    @GetMapping("/search")
    public String searchHistory(@RequestParam String portfolioId, Model model) {
        if (portfolioId != null && !portfolioId.isBlank()) {
            portfolioService.readPortfolio(portfolioId.trim()).ifPresent(p ->
                    model.addAttribute("portfolio", p));

            List<TransactionRecord> transactions = transactionService
                    .getTransactionHistory(portfolioId.trim());
            model.addAttribute("transactions", transactions);

            if (transactions.isEmpty()) {
                model.addAttribute("infoMessage", "No transaction history found for portfolio: " + portfolioId);
            }
        }
        model.addAttribute("portfolios", portfolioService.findAllPortfolios());
        return "history-inquiry";
    }
}
