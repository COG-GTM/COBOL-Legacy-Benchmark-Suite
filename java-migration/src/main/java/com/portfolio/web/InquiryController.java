package com.portfolio.web;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Position;
import com.portfolio.domain.Transaction;
import com.portfolio.service.online.HistoryInquiryService;
import com.portfolio.service.online.PortfolioInquiryService;
import com.portfolio.service.portfolio.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * Inquiry Controller - migrated from COBOL INQONLN.cbl.
 * Replaces CICS transaction PINQ (defined in PORTDFN.csd).
 *
 * EVALUATE WS-COMMAREA-FUNCTION:
 *   MENU -> GET /menu
 *   INQP -> GET /portfolio/{id}/positions
 *   INQH -> GET /portfolio/{id}/history
 *   EXIT -> logout
 */
@Controller
public class InquiryController {

    private static final Logger log = LoggerFactory.getLogger(InquiryController.class);

    private final PortfolioService portfolioService;
    private final PortfolioInquiryService portfolioInquiryService;
    private final HistoryInquiryService historyInquiryService;

    public InquiryController(PortfolioService portfolioService,
                             PortfolioInquiryService portfolioInquiryService,
                             HistoryInquiryService historyInquiryService) {
        this.portfolioService = portfolioService;
        this.portfolioInquiryService = portfolioInquiryService;
        this.historyInquiryService = historyInquiryService;
    }

    @GetMapping({"/", "/menu"})
    public String showMenu(Model model) {
        List<Portfolio> activePortfolios = portfolioService.getActivePortfolios();
        model.addAttribute("portfolios", activePortfolios);
        return "menu";
    }

    @GetMapping("/portfolio/{accountNo}/positions")
    public String showPositions(@PathVariable String accountNo, Model model) {
        Optional<Portfolio> portfolio = portfolioService.getPortfolio(accountNo);
        List<Position> positions = portfolioInquiryService.getPortfolioPositions(accountNo);

        model.addAttribute("portfolio", portfolio.orElse(null));
        model.addAttribute("positions", positions);
        model.addAttribute("accountNo", accountNo);
        return "positions";
    }

    @GetMapping("/portfolio/{accountNo}/history")
    public String showHistory(@PathVariable String accountNo,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Optional<Portfolio> portfolio = portfolioService.getPortfolio(accountNo);
        Page<Transaction> transactions = historyInquiryService.getTransactionHistory(
                accountNo, page, size);

        model.addAttribute("portfolio", portfolio.orElse(null));
        model.addAttribute("transactions", transactions);
        model.addAttribute("accountNo", accountNo);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        return "history";
    }
}
