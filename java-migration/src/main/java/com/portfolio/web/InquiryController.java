package com.portfolio.web;

import com.portfolio.domain.InvestmentPosition;
import com.portfolio.domain.Portfolio;
import com.portfolio.domain.PositionHistory;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.security.AuthorizationService;
import com.portfolio.service.PortfolioService;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class InquiryController {
  private final PortfolioRepository portfolioRepository;
  private final PositionHistoryRepository historyRepository;
  private final PortfolioService portfolioService;
  private final AuthorizationService authorizationService;

  public InquiryController(
      PortfolioRepository portfolioRepository,
      PositionHistoryRepository historyRepository,
      PortfolioService portfolioService,
      AuthorizationService authorizationService) {
    this.portfolioRepository = portfolioRepository;
    this.historyRepository = historyRepository;
    this.portfolioService = portfolioService;
    this.authorizationService = authorizationService;
  }

  @GetMapping("/")
  public String root() {
    return "redirect:/inquiry/menu";
  }

  @GetMapping("/inquiry/menu")
  public String menu(Model model) {
    model.addAttribute("portfolios", portfolioRepository.findAll());
    return "menu";
  }

  @PostMapping("/inquiry")
  public Object dispatch(
      @RequestParam String option,
      @RequestParam(required = false, defaultValue = "") String accountNo) {
    return switch (option) {
      case "1" -> "redirect:/inquiry/position?accountNo=" + accountNo;
      case "2" -> "redirect:/inquiry/history?accountNo=" + accountNo;
      case "3" -> "redirect:/logout";
      default -> error("E008", "Invalid option");
    };
  }

  @GetMapping("/inquiry/position")
  public Object position(@RequestParam String accountNo, Principal principal, Model model) {
    authorizationService.requireAccess(principal.getName(), "INQONLN", "READ");
    Portfolio portfolio = portfolioRepository.findByAccountNo(accountNo).orElse(null);
    if (portfolio == null) {
      return error("E002", "Account not found");
    }
    List<InvestmentPosition> positions = portfolioService.positionsForAccount(accountNo);
    BigDecimal totalCost =
        positions.stream()
            .map(InvestmentPosition::getCostBasis)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalValue =
        positions.stream()
            .map(InvestmentPosition::getMarketValue)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    model.addAttribute("accountNo", accountNo);
    model.addAttribute("clientName", portfolio.getClientName());
    model.addAttribute("positions", positions);
    model.addAttribute("totalCost", totalCost);
    model.addAttribute("totalValue", totalValue);
    return "position";
  }

  @GetMapping("/inquiry/history")
  public String history(@RequestParam String accountNo, Principal principal, Model model) {
    authorizationService.requireAccess(principal.getName(), "INQONLN", "READ");
    List<PositionHistory> history =
        historyRepository.findByIdAccountNoOrderByIdTransDateDescIdTransTimeDesc(
            accountNo, PageRequest.of(0, 10));
    model.addAttribute("accountNo", accountNo);
    model.addAttribute("history", history);
    model.addAttribute("message", history.isEmpty() ? "No history found for account" : "");
    return "history";
  }

  private ModelAndView error(String code, String details) {
    ModelAndView modelAndView = new ModelAndView("error");
    modelAndView.addObject("errorCode", code);
    modelAndView.addObject("errorDetails", details);
    return modelAndView;
  }
}
