package com.portfolio.service;

import com.portfolio.exception.DuplicatePortfolioException;
import com.portfolio.exception.InvalidPortfolioException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.service.common.AuditService;
import com.portfolio.service.portfolio.PortfolioService;
import com.portfolio.service.portfolio.PortfolioValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AuditService auditService;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        PortfolioValidator validator = new PortfolioValidator();
        portfolioService = new PortfolioService(portfolioRepository, validator, auditService);
    }

    @Test
    void createPortfolio_success() {
        Portfolio portfolio = createTestPortfolio("PORT0001");
        when(portfolioRepository.existsById("PORT0001")).thenReturn(false);
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        Portfolio result = portfolioService.createPortfolio(portfolio);

        assertNotNull(result);
        assertEquals("PORT0001", result.getPortfolioId());
        verify(portfolioRepository).save(any(Portfolio.class));
        verify(auditService).logTransaction(eq("PORT0001"), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createPortfolio_duplicate_throwsException() {
        Portfolio portfolio = createTestPortfolio("PORT0001");
        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);

        assertThrows(DuplicatePortfolioException.class, () ->
                portfolioService.createPortfolio(portfolio));
    }

    @Test
    void createPortfolio_invalidId_throwsException() {
        Portfolio portfolio = createTestPortfolio("INVALID1");

        assertThrows(InvalidPortfolioException.class, () ->
                portfolioService.createPortfolio(portfolio));
    }

    @Test
    void createPortfolio_missingName_throwsException() {
        Portfolio portfolio = createTestPortfolio("PORT0001");
        portfolio.setPortfolioName(null);

        assertThrows(InvalidPortfolioException.class, () ->
                portfolioService.createPortfolio(portfolio));
    }

    @Test
    void readPortfolio_success() {
        Portfolio portfolio = createTestPortfolio("PORT0001");
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        Portfolio result = portfolioService.readPortfolio("PORT0001");

        assertNotNull(result);
        assertEquals("PORT0001", result.getPortfolioId());
    }

    @Test
    void readPortfolio_notFound_throwsException() {
        when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class, () ->
                portfolioService.readPortfolio("PORT9999"));
    }

    @Test
    void updatePortfolio_success() {
        Portfolio existing = createTestPortfolio("PORT0001");
        Portfolio updated = createTestPortfolio("PORT0001");
        updated.setPortfolioName("Updated Name");

        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existing));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(updated);

        Portfolio result = portfolioService.updatePortfolio(updated);

        assertNotNull(result);
        assertEquals("Updated Name", result.getPortfolioName());
        verify(auditService).logPortfolioUpdate(eq("PORT0001"), any(), any());
    }

    @Test
    void updatePortfolio_notFound_throwsException() {
        Portfolio portfolio = createTestPortfolio("PORT9999");
        when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class, () ->
                portfolioService.updatePortfolio(portfolio));
    }

    @Test
    void deletePortfolio_success() {
        Portfolio existing = createTestPortfolio("PORT0001");
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existing));

        portfolioService.deletePortfolio("PORT0001");

        verify(portfolioRepository).delete(existing);
        verify(auditService).logTransaction(eq("PORT0001"), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deletePortfolio_notFound_throwsException() {
        when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class, () ->
                portfolioService.deletePortfolio("PORT9999"));
    }

    private Portfolio createTestPortfolio(String id) {
        Portfolio p = new Portfolio();
        p.setPortfolioId(id);
        p.setAccountType("GN");
        p.setBranchId("01");
        p.setClientId("CLIENT001");
        p.setPortfolioName("Test Portfolio");
        p.setCurrencyCode("USD");
        p.setRiskLevel("M");
        p.setStatus('A');
        p.setOpenDate(LocalDate.now());
        p.setLastMaintDate(LocalDateTime.now());
        p.setLastMaintUser("TESTUSER");
        p.setTotalValue(new BigDecimal("100000.00"));
        p.setCashBalance(new BigDecimal("10000.00"));
        return p;
    }
}
