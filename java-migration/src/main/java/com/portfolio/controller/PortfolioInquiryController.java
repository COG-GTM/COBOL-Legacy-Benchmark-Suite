package com.portfolio.controller;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.PositionRecord;
import com.portfolio.service.PortfolioMasterService;
import com.portfolio.service.ReturnAnalysisService;
import com.portfolio.repository.PositionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/inquiry/portfolio")
public class PortfolioInquiryController {

    private final PortfolioMasterService portfolioService;
    private final PositionRepository positionRepository;
    private final ReturnAnalysisService returnAnalysisService;

    public PortfolioInquiryController(PortfolioMasterService portfolioService,
                                      PositionRepository positionRepository,
                                      ReturnAnalysisService returnAnalysisService) {
        this.portfolioService = portfolioService;
        this.positionRepository = positionRepository;
        this.returnAnalysisService = returnAnalysisService;
    }

    @GetMapping
    public String showInquiryForm(Model model) {
        model.addAttribute("portfolios", portfolioService.findAllPortfolios());
        return "portfolio-inquiry";
    }

    @GetMapping("/search")
    public String searchPortfolio(@RequestParam(required = false) String portfolioId,
                                  @RequestParam(required = false) String clientId,
                                  Model model) {
        if (portfolioId != null && !portfolioId.isBlank()) {
            Optional<Portfolio> portfolio = portfolioService.readPortfolio(portfolioId.trim());
            if (portfolio.isPresent()) {
                model.addAttribute("portfolio", portfolio.get());
                List<PositionRecord> positions = positionRepository.findActivePositions(portfolioId.trim());
                model.addAttribute("positions", positions);
                Map<String, Object> analysis = returnAnalysisService.analyzePortfolioReturns(portfolioId.trim());
                model.addAttribute("analysis", analysis);
            } else {
                model.addAttribute("errorMessage", "Portfolio not found: " + portfolioId);
            }
        } else if (clientId != null && !clientId.isBlank()) {
            List<Portfolio> portfolios = portfolioService.findByClientId(clientId.trim());
            if (portfolios.isEmpty()) {
                model.addAttribute("errorMessage", "No portfolios found for client: " + clientId);
            }
            model.addAttribute("portfolios", portfolios);
        }
        if (!model.containsAttribute("portfolios")) {
            model.addAttribute("portfolios", portfolioService.findAllPortfolios());
        }
        return "portfolio-inquiry";
    }
}
