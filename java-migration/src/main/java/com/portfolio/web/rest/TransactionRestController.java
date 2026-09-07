package com.portfolio.web.rest;

import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.Transaction;
import com.portfolio.dto.TransactionDto;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.security.AuthorizationService;
import com.portfolio.service.PortfolioTransactionService;
import com.portfolio.service.PortfolioTransactionService.TransactionResult;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRestController {
  private final PortfolioTransactionService transactionService;
  private final TransactionRepository transactionRepository;
  private final PortfolioRepository portfolioRepository;
  private final PositionHistoryRepository historyRepository;
  private final AuthorizationService authorizationService;

  public TransactionRestController(
      PortfolioTransactionService transactionService,
      TransactionRepository transactionRepository,
      PortfolioRepository portfolioRepository,
      PositionHistoryRepository historyRepository,
      AuthorizationService authorizationService) {
    this.transactionService = transactionService;
    this.transactionRepository = transactionRepository;
    this.portfolioRepository = portfolioRepository;
    this.historyRepository = historyRepository;
    this.authorizationService = authorizationService;
  }

  @PostMapping
  public TransactionResult create(
      @Valid @RequestBody TransactionDto transactionDto, Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "UPDATE");
    Transaction transaction = transactionService.createPending(transactionDto);
    return transactionService.process(transaction);
  }

  @GetMapping
  public List<Transaction> findByPortfolio(@RequestParam String portfolioId, Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "READ");
    return transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolioId);
  }

  @GetMapping("/{portfolioId}/history")
  public List<PositionHistory> history(@PathVariable String portfolioId, Principal principal) {
    authorizationService.requireAccess(principal.getName(), "PORTMSTR", "READ");
    String accountNo =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(
                () -> new com.portfolio.service.ResourceNotFoundException("Portfolio not found"))
            .getAccountNo();
    return historyRepository.findByIdAccountNoOrderByIdTransDateDescIdTransTimeDesc(
        accountNo, PageRequest.of(0, 100));
  }
}
