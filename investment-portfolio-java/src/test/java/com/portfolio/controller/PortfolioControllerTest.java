package com.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.config.SecurityConfig;
import com.portfolio.dto.PortfolioCreateRequest;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.PortfolioStatus;
import com.portfolio.exception.DuplicatePortfolioException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.service.PortfolioService;
import com.portfolio.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
@Import(SecurityConfig.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private TransactionService transactionService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createPortfolio_validRequest_returns201() throws Exception {
        PortfolioCreateRequest request = new PortfolioCreateRequest();
        request.setPortfolioId("PORT0001");
        request.setPortfolioName("Test Portfolio");
        request.setStatus("A");

        PortfolioMaster portfolio = createTestPortfolio();
        when(portfolioService.createPortfolio(any())).thenReturn(portfolio);

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portfolioId").value("PORT0001"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getPortfolio_exists_returns200() throws Exception {
        PortfolioMaster portfolio = createTestPortfolio();
        when(portfolioService.readPortfolio("PORT0001")).thenReturn(portfolio);

        mockMvc.perform(get("/api/portfolios/PORT0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("PORT0001"))
                .andExpect(jsonPath("$.portfolioName").value("Test Portfolio"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getPortfolio_notExists_returns404() throws Exception {
        when(portfolioService.readPortfolio("PORT9999"))
                .thenThrow(new PortfolioNotFoundException("PORT9999"));

        mockMvc.perform(get("/api/portfolios/PORT9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createPortfolio_duplicate_returns409() throws Exception {
        PortfolioCreateRequest request = new PortfolioCreateRequest();
        request.setPortfolioId("PORT0001");
        request.setPortfolioName("Test Portfolio");
        request.setStatus("A");

        when(portfolioService.createPortfolio(any()))
                .thenThrow(new DuplicatePortfolioException("PORT0001"));

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deletePortfolio_returns204() throws Exception {
        mockMvc.perform(delete("/api/portfolios/PORT0001"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void listPortfolios_returns200() throws Exception {
        when(portfolioService.findAll()).thenReturn(List.of(createTestPortfolio()));

        mockMvc.perform(get("/api/portfolios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].portfolioId").value("PORT0001"));
    }

    @Test
    void getPortfolio_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/portfolios/PORT0001"))
                .andExpect(status().isUnauthorized());
    }

    private PortfolioMaster createTestPortfolio() {
        PortfolioMaster portfolio = new PortfolioMaster();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setPortfolioName("Test Portfolio");
        portfolio.setStatus(PortfolioStatus.ACTIVE);
        portfolio.setAccountNo("ACC0000001");
        portfolio.setCreateDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setTotalValue(BigDecimal.valueOf(100000));
        portfolio.setCashBalance(BigDecimal.valueOf(10000));
        return portfolio;
    }
}
