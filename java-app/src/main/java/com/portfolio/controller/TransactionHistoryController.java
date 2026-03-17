package com.portfolio.controller;

import com.portfolio.model.TransactionHistory;
import com.portfolio.repository.TransactionHistoryRepository;
import com.portfolio.service.SecurityManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Transaction History Controller.
 * Replaces: INQHIST.cbl and the HISMAP BMS map.
 *
 * GET /history -> render transaction-history.html
 * GET /history/{portfolioId} -> query repository, render results with pagination
 *
 * The COBOL version uses DB2 cursor (via CURSMGR) to fetch 10 rows at a time.
 * This is replaced by Spring Data pagination.
 */
@Controller
public class TransactionHistoryController {

    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryController.class);
    private static final int PAGE_SIZE = 10;

    private final TransactionHistoryRepository transactionRepository;
    private final SecurityManagerService securityManager;

    public TransactionHistoryController(TransactionHistoryRepository transactionRepository,
                                         SecurityManagerService securityManager) {
        this.transactionRepository = transactionRepository;
        this.securityManager = securityManager;
    }

    /**
     * Shows the transaction history inquiry form or handles form submission.
     * Replaces: HISMAP initial display with Account input field.
     * When portfolioId is provided as query param (from form), redirects to path variable URL.
     */
    @GetMapping("/history")
    public String showHistoryForm(@RequestParam(name = "portfolioId", required = false) String portfolioId) {
        if (portfolioId != null && !portfolioId.trim().isEmpty()) {
            return "redirect:/history/" + portfolioId.trim();
        }
        return "transaction-history";
    }

    /**
     * Displays transaction history for a portfolio.
     * Replaces: INQHIST.cbl P200-GET-HISTORY (DB2 cursor query via CURSMGR)
     * and P300-FORMAT-DISPLAY (SEND MAP('HISMAP')).
     *
     * The COBOL version fetches from POSHIST table using:
     *   SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT
     *   FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC
     */
    @GetMapping("/history/{portfolioId}")
    public String showTransactionHistory(@PathVariable("portfolioId") String portfolioId,
                                          @RequestParam(defaultValue = "0") int page,
                                          Model model) {
        securityManager.logAccess("HISTORY", "READ");

        int safePage = Math.max(page, 0);
        Page<TransactionHistory> transactions = transactionRepository
                .findByPortfolioIdOrderByTransactionDateDesc(
                        portfolioId.trim(),
                        PageRequest.of(safePage, PAGE_SIZE));

        model.addAttribute("portfolioId", portfolioId);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", transactions.getTotalPages());

        log.debug("History inquiry: portfolioId={}, total={}", portfolioId,
                transactions.getTotalElements());
        return "transaction-history";
    }
}
