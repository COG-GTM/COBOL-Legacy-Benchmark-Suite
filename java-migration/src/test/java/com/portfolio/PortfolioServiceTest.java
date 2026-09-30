package com.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.portfolio.config.ErrorLoggingService;
import com.portfolio.domain.Portfolio;
import com.portfolio.dto.PortfolioDto;
import com.portfolio.repository.AuditLogRepository;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.BusinessException;
import com.portfolio.service.PortfolioService;
import com.portfolio.service.PortfolioValidationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PortfolioServiceTest {
  @Mock PortfolioRepository portfolioRepository;
  @Mock InvestmentPositionRepository positionRepository;
  @Mock AuditLogRepository auditLogRepository;
  @Mock TransactionRepository transactionRepository;
  @Mock ErrorLoggingService errorLoggingService;
  private PortfolioService portfolioService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    portfolioService =
        new PortfolioService(
            portfolioRepository,
            positionRepository,
            auditLogRepository,
            transactionRepository,
            new PortfolioValidationService());
  }

  @Test
  void duplicateAccountNumberThrowsE003() {
    PortfolioDto portfolioDto = validPortfolioDto();
    when(portfolioRepository.existsById("PORT0001")).thenReturn(false);
    when(portfolioRepository.existsByAccountNo("1234567890")).thenReturn(true);

    BusinessException businessException =
        assertThrows(BusinessException.class, () -> portfolioService.create(portfolioDto));

    assertEquals("E003", businessException.getCode());
    assertEquals("Account number already exists", businessException.getMessage());
  }

  @Test
  void deleteWithPositionsThrowsE004() {
    Portfolio portfolio = new Portfolio();
    portfolio.setPortfolioId("PORT0001");
    when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
    when(positionRepository.existsByIdPortfolioId("PORT0001")).thenReturn(true);

    BusinessException businessException =
        assertThrows(BusinessException.class, () -> portfolioService.delete("PORT0001"));

    assertEquals("E004", businessException.getCode());
    assertEquals("Portfolio has positions or transactions", businessException.getMessage());
  }

  private PortfolioDto validPortfolioDto() {
    PortfolioDto portfolioDto = new PortfolioDto();
    portfolioDto.setPortfolioId("PORT0001");
    portfolioDto.setAccountNo("1234567890");
    portfolioDto.setClientName("Test Client");
    portfolioDto.setClientType("I");
    portfolioDto.setStatus("A");
    return portfolioDto;
  }
}
