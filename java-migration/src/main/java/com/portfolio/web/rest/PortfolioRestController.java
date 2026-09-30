package com.portfolio.web.rest;

import com.portfolio.domain.InvestmentPosition;
import com.portfolio.domain.Portfolio;
import com.portfolio.dto.PortfolioDto;
import com.portfolio.security.AuthorizationService;
import com.portfolio.service.PortfolioService;
import com.portfolio.service.PortfolioService.UpdateType;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioRestController {
  private final PortfolioService portfolioService;
  private final AuthorizationService authorizationService;

  public PortfolioRestController(
      PortfolioService portfolioService, AuthorizationService authorizationService) {
    this.portfolioService = portfolioService;
    this.authorizationService = authorizationService;
  }

  @GetMapping
  public List<Portfolio> findAll(Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "READ");
    return portfolioService.findAll();
  }

  @GetMapping("/{portfolioId}")
  public Portfolio findOne(@PathVariable String portfolioId, Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "READ");
    return portfolioService.read(portfolioId);
  }

  @PostMapping
  public Portfolio create(@Valid @RequestBody PortfolioDto portfolioDto, Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "UPDATE");
    return portfolioService.create(portfolioDto);
  }

  @PutMapping("/{portfolioId}/{updateType}")
  public Portfolio update(
      @PathVariable String portfolioId,
      @PathVariable String updateType,
      @RequestBody Map<String, String> body,
      Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "UPDATE");
    return portfolioService.applyUpdate(
        portfolioId, UpdateType.valueOf(updateType.toUpperCase()), body.get("value"));
  }

  @DeleteMapping("/{portfolioId}")
  public ResponseEntity<Void> delete(@PathVariable String portfolioId, Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "UPDATE");
    portfolioService.delete(portfolioId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{portfolioId}/positions")
  public List<InvestmentPosition> positions(@PathVariable String portfolioId, Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "READ");
    portfolioService.read(portfolioId);
    return portfolioService.positionsForPortfolio(portfolioId);
  }
}
