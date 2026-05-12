package com.portfolio.integration;

import com.portfolio.model.entity.Portfolio;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.repository.PositionRepository;
import com.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @BeforeEach
    void setUp() {
        positionHistoryRepository.deleteAll();
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "INQUIRY")
    void getPortfolio_found() throws Exception {
        createAndSavePortfolio("PORT0001");

        mockMvc.perform(get("/api/portfolios/PORT0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("PORT0001"))
                .andExpect(jsonPath("$.portfolioName").value("Test Portfolio"));
    }

    @Test
    @WithMockUser(roles = "INQUIRY")
    void getPortfolio_notFound() throws Exception {
        mockMvc.perform(get("/api/portfolios/PORT9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "UPDATE")
    void createPortfolio_success() throws Exception {
        String json = """
                {
                    "portfolioId": "PORT0002",
                    "accountType": "GN",
                    "branchId": "01",
                    "clientId": "CLIENT002",
                    "portfolioName": "New Portfolio",
                    "currencyCode": "USD",
                    "riskLevel": "M",
                    "status": "A",
                    "openDate": "2024-01-01",
                    "lastMaintDate": "2024-01-01T00:00:00",
                    "lastMaintUser": "ADMIN",
                    "totalValue": 50000.00,
                    "cashBalance": 5000.00
                }
                """;

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portfolioId").value("PORT0002"));
    }

    @Test
    @WithMockUser(roles = "UPDATE")
    void createPortfolio_duplicateId() throws Exception {
        createAndSavePortfolio("PORT0001");

        String json = """
                {
                    "portfolioId": "PORT0001",
                    "accountType": "GN",
                    "branchId": "01",
                    "clientId": "CLIENT002",
                    "portfolioName": "Duplicate Portfolio",
                    "currencyCode": "USD",
                    "riskLevel": "M",
                    "status": "A",
                    "openDate": "2024-01-01",
                    "lastMaintDate": "2024-01-01T00:00:00",
                    "lastMaintUser": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePortfolio_success() throws Exception {
        createAndSavePortfolio("PORT0001");

        mockMvc.perform(delete("/api/portfolios/PORT0001"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "INQUIRY")
    void deletePortfolio_forbidden() throws Exception {
        createAndSavePortfolio("PORT0001");

        mockMvc.perform(delete("/api/portfolios/PORT0001"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPortfolio_unauthorized() throws Exception {
        mockMvc.perform(get("/api/portfolios/PORT0001"))
                .andExpect(status().isUnauthorized());
    }

    private Portfolio createAndSavePortfolio(String id) {
        Portfolio p = new Portfolio();
        p.setPortfolioId(id);
        p.setAccountType("GN");
        p.setBranchId("01");
        p.setClientId("CLIENT001");
        p.setPortfolioName("Test Portfolio");
        p.setCurrencyCode("USD");
        p.setRiskLevel("M");
        p.setStatus('A');
        p.setOpenDate(LocalDate.of(2024, 1, 1));
        p.setLastMaintDate(LocalDateTime.of(2024, 1, 1, 0, 0));
        p.setLastMaintUser("SYSTEM");
        p.setTotalValue(new BigDecimal("100000.00"));
        p.setCashBalance(new BigDecimal("10000.00"));
        return portfolioRepository.save(p);
    }
}
