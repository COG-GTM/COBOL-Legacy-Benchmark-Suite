package com.portfolio.controller;

import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.Portfolio;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.service.SecurityManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * Portfolio Inquiry Controller.
 * Replaces: INQPORT.cbl and the POSMAP BMS map.
 *
 * GET /portfolio -> render position-inquiry.html (input form for account ID)
 * GET /portfolio/{id} -> query repositories, render results
 *
 * Display fields matching POSMAP: Fund ID, Fund Name, Units, Cost Basis, Market Value.
 * Pagination replaces PF7=Previous, PF8=Next from BMS map line 47.
 */
@Controller
public class PortfolioInquiryController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioInquiryController.class);
    private static final int PAGE_SIZE = 10;

    private final PortfolioRepository portfolioRepository;
    private final InvestmentPositionRepository positionRepository;
    private final SecurityManagerService securityManager;

    public PortfolioInquiryController(PortfolioRepository portfolioRepository,
                                       InvestmentPositionRepository positionRepository,
                                       SecurityManagerService securityManager) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.securityManager = securityManager;
    }

    /**
     * Shows the portfolio inquiry form or handles form submission.
     * Replaces: POSMAP initial display with Account input field.
     * When id is provided as query param (from form), redirects to path variable URL.
     */
    @GetMapping("/portfolio")
    public String showInquiryForm(@RequestParam(name = "id", required = false) String id) {
        if (id != null && !id.trim().isEmpty()) {
            return "redirect:/portfolio/" + id.trim();
        }
        return "position-inquiry";
    }

    /**
     * Displays portfolio positions.
     * Replaces: INQPORT.cbl P200-GET-POSITION and P300-FORMAT-DISPLAY.
     *
     * INQPORT reads from VSAM POSFILE via EXEC CICS READ FILE('POSFILE').
     * This method queries the JPA repository instead.
     */
    @GetMapping("/portfolio/{id}")
    public String showPortfolioPositions(@PathVariable("id") String id,
                                          @RequestParam(defaultValue = "0") int page,
                                          Model model) {
        securityManager.logAccess("PORTFOLIO", "READ");

        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(id.trim());
        if (portfolioOpt.isEmpty()) {
            // Replaces INQPORT P900-NOT-FOUND: "Position not found for account"
            throw new PortfolioNotFoundException(id);
        }

        Portfolio portfolio = portfolioOpt.get();
        List<InvestmentPosition> allPositions = positionRepository.findByKeyPortfolioId(id.trim());

        // Pagination (replaces PF7/PF8 scrolling)
        int safePage = Math.max(page, 0);
        int start = Math.min(safePage * PAGE_SIZE, allPositions.size());
        int end = Math.min(start + PAGE_SIZE, allPositions.size());
        List<InvestmentPosition> pageContent = allPositions.subList(start, end);
        Page<InvestmentPosition> positions = new PageImpl<>(
                pageContent, PageRequest.of(safePage, PAGE_SIZE), allPositions.size());

        model.addAttribute("portfolio", portfolio);
        model.addAttribute("positions", positions);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", positions.getTotalPages());

        log.debug("Portfolio inquiry: id={}, positions={}", id, allPositions.size());
        return "position-inquiry";
    }
}
