package com.portfolio.service;

import com.portfolio.dto.PortfolioRequest;
import com.portfolio.dto.PortfolioResponse;
import com.portfolio.exception.DuplicatePortfolioException;
import com.portfolio.exception.PortfolioNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for PortfolioService.
 * Tests mirror the COBOL operations:
 * - createPortfolio -> PORTMSTR 2000-CREATE-PORTFOLIO
 * - readPortfolio -> PORTMSTR 3000-READ-PORTFOLIO
 * - updatePortfolio -> PORTMSTR 4000-UPDATE-PORTFOLIO / PORTUPDT 2200-APPLY-UPDATE
 * - deletePortfolio -> PORTMSTR 5000-DELETE-PORTFOLIO / PORTDEL 2200-DELETE-RECORD
 * - findAllPortfolios -> PORTREAD 2000-PROCESS (sequential READ)
 */
@SpringBootTest
@Transactional
class PortfolioServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    @Test
    void testSeedDataLoaded() {
        List<PortfolioResponse> all = portfolioService.findAllPortfolios();
        assertEquals(10, all.size(), "Seed data should load 10 portfolio records");
    }

    @Test
    void testReadPortfolioById() {
        PortfolioResponse response = portfolioService.readPortfolio("PORT0001");
        assertNotNull(response);
        assertEquals("PORT0001", response.getPortId());
        assertEquals("1000000001", response.getAccountNo());
        assertEquals("Acme Corporation", response.getClientName());
        assertEquals("C", response.getClientType());
        assertEquals("A", response.getStatus());
        assertEquals(0, new BigDecimal("1250000.00").compareTo(response.getTotalValue()));
        assertEquals(0, new BigDecimal("125000.00").compareTo(response.getCashBalance()));
    }

    @Test
    void testReadPortfolioNotFound() {
        assertThrows(PortfolioNotFoundException.class, () ->
                portfolioService.readPortfolio("PORT9999"));
    }

    @Test
    void testCreatePortfolio() {
        PortfolioRequest request = new PortfolioRequest();
        request.setPortId("PORT0099");
        request.setAccountNo("9999999999");
        request.setClientName("New Test Client");
        request.setClientType("I");
        request.setStatus("A");
        request.setTotalValue(new BigDecimal("500000.00"));
        request.setCashBalance(new BigDecimal("50000.00"));

        PortfolioResponse response = portfolioService.createPortfolio(request);
        assertNotNull(response);
        assertEquals("PORT0099", response.getPortId());
        assertEquals("New Test Client", response.getClientName());
        assertNotNull(response.getCreateDate());
    }

    @Test
    void testCreateDuplicatePortfolio() {
        PortfolioRequest request = new PortfolioRequest();
        request.setPortId("PORT0001");
        request.setAccountNo("1000000001");
        request.setClientName("Duplicate");
        request.setClientType("I");
        request.setStatus("A");
        request.setTotalValue(BigDecimal.ZERO);
        request.setCashBalance(BigDecimal.ZERO);

        assertThrows(DuplicatePortfolioException.class, () ->
                portfolioService.createPortfolio(request));
    }

    @Test
    void testUpdatePortfolio() {
        PortfolioRequest updateRequest = new PortfolioRequest();
        updateRequest.setPortId("PORT0001");
        updateRequest.setAccountNo("1000000001");
        updateRequest.setClientName("Acme Corp Updated");
        updateRequest.setClientType("C");
        updateRequest.setStatus("A");
        updateRequest.setTotalValue(new BigDecimal("1500000.00"));
        updateRequest.setCashBalance(new BigDecimal("150000.00"));

        PortfolioResponse response = portfolioService.updatePortfolio("PORT0001", updateRequest);
        assertEquals("Acme Corp Updated", response.getClientName());
        assertEquals(0, new BigDecimal("1500000.00").compareTo(response.getTotalValue()));
    }

    @Test
    void testUpdatePortfolioNotFound() {
        PortfolioRequest request = new PortfolioRequest();
        request.setPortId("PORT9999");
        request.setAccountNo("0000000000");
        request.setClientName("Ghost");
        request.setClientType("I");
        request.setStatus("A");

        assertThrows(PortfolioNotFoundException.class, () ->
                portfolioService.updatePortfolio("PORT9999", request));
    }

    @Test
    void testDeletePortfolio() {
        portfolioService.deletePortfolio("PORT0007");
        assertThrows(PortfolioNotFoundException.class, () ->
                portfolioService.readPortfolio("PORT0007"));
    }

    @Test
    void testDeletePortfolioNotFound() {
        assertThrows(PortfolioNotFoundException.class, () ->
                portfolioService.deletePortfolio("PORT9999"));
    }

    @Test
    void testFindByStatus() {
        List<PortfolioResponse> active = portfolioService.findByStatus("A");
        assertTrue(active.size() >= 7, "Should have at least 7 active portfolios");
        active.forEach(p -> assertEquals("A", p.getStatus()));

        List<PortfolioResponse> suspended = portfolioService.findByStatus("S");
        assertEquals(1, suspended.size());
        assertEquals("PORT0005", suspended.get(0).getPortId());
    }

    @Test
    void testFindAllPortfolios() {
        List<PortfolioResponse> all = portfolioService.findAllPortfolios();
        assertEquals(10, all.size());
    }
}
