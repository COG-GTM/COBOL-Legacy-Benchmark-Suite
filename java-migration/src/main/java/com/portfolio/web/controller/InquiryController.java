package com.portfolio.web.controller;

import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Position;
import com.portfolio.model.entity.PositionHistory;
import com.portfolio.service.inquiry.HistoryInquiryService;
import com.portfolio.service.inquiry.PortfolioInquiryService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/inquiry")
@PreAuthorize("hasRole('INQUIRY')")
public class InquiryController {

    private final PortfolioInquiryService portfolioInquiryService;
    private final HistoryInquiryService historyInquiryService;

    public InquiryController(PortfolioInquiryService portfolioInquiryService,
                             HistoryInquiryService historyInquiryService) {
        this.portfolioInquiryService = portfolioInquiryService;
        this.historyInquiryService = historyInquiryService;
    }

    @GetMapping
    public String menu() {
        return "inquiry/menu";
    }

    @GetMapping("/portfolio")
    public String portfolioInquiry(@RequestParam String portfolioId, Model model) {
        Portfolio portfolio = portfolioInquiryService.lookupByPortfolioId(portfolioId);
        List<Position> positions = portfolioInquiryService.getPositions(portfolioId);

        model.addAttribute("portfolio", portfolio);
        model.addAttribute("positions", positions);
        return "inquiry/portfolio-position";
    }

    @GetMapping("/history")
    public String historyInquiry(@RequestParam String accountNo,
                                 @RequestParam(defaultValue = "0") int page,
                                 Model model) {
        Page<PositionHistory> historyPage = historyInquiryService.lookup(accountNo, page);

        model.addAttribute("accountNo", accountNo);
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("currentPage", page);
        return "inquiry/transaction-history";
    }
}
