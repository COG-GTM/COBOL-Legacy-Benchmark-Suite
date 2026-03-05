package com.portfolio.online;

import com.portfolio.model.PositionRecord;
import com.portfolio.model.PortfolioMaster;
import com.portfolio.support.PortfolioMasterRepository;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.SecurityLogRepository;
import com.portfolio.support.TransactionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Tests for PortfolioController.
 * Tests all inquiry endpoints (positions, history with pagination).
 * Security: unauthorized -> 403, authorized -> 200 + audit log entry.
 * Error scenarios: structured JSON error response.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioMasterRepository portfolioRepository;

    @Autowired
    private PositionRecordRepository positionRepository;

    @Autowired
    private TransactionRecordRepository transactionRepository;

    @Autowired
    private SecurityLogRepository securityLogRepository;

    @BeforeEach
    void setUp() {
        securityLogRepository.deleteAll();
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();

        // Set up test data
        PortfolioMaster portfolio = new PortfolioMaster(
                "PORT0001", "IN", "01", "CLIENT001 ",
                "Test Portfolio", "USD", "M", "A",
                LocalDate.now(), LocalDateTime.now(), "TEST    ");
        portfolioRepository.save(portfolio);

        PositionRecord position = new PositionRecord();
        position.setPortfolioId("PORT0001");
        position.setSymbolId("AAPL      ");
        position.setPositionDate(LocalDate.now());
        position.setQuantity(new BigDecimal("100.0000"));
        position.setCostBasis(new BigDecimal("15000.00"));
        position.setMarketValue(new BigDecimal("17500.00"));
        position.setCurrencyCode("USD");
        position.setStatus("A");
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("TEST    ");
        positionRepository.save(position);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    void testGetPositions_ValidPortfolio() throws Exception {
        mockMvc.perform(get("/api/portfolio/PORT0001/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("PORT0001"))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    void testGetPositions_InvalidPortfolio() throws Exception {
        mockMvc.perform(get("/api/portfolio/INVALID1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("INVALID1"))
                .andExpect(jsonPath("$.message").value("No positions found for portfolio INVALID1"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    void testGetHistory_PaginatedResults() throws Exception {
        mockMvc.perform(get("/api/portfolio/PORT0001/history")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("PORT0001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void testUnauthorizedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/portfolio/PORT0001/positions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    void testAuditLogCreatedOnAccess() throws Exception {
        long auditCountBefore = securityLogRepository.count();

        mockMvc.perform(get("/api/portfolio/PORT0001/positions"))
                .andExpect(status().isOk());

        long auditCountAfter = securityLogRepository.count();
        // Audit log entry should be created
        org.assertj.core.api.Assertions.assertThat(auditCountAfter).isGreaterThan(auditCountBefore);
    }
}
