package com.portfolio.controller;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.InvestmentPositionKey;
import com.portfolio.model.Portfolio;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.service.ErrorHandlingService;
import com.portfolio.service.SecurityManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Controller tests for PortfolioInquiryController.
 * Uses MockMvc for testing (replacing CICS terminal testing).
 */
@WebMvcTest(PortfolioInquiryController.class)
class PortfolioInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioRepository portfolioRepository;

    @MockBean
    private InvestmentPositionRepository positionRepository;

    @MockBean
    private SecurityManagerService securityManager;

    @MockBean
    private ErrorHandlingService errorHandlingService;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void showInquiryForm_returnsFormView() throws Exception {
        mockMvc.perform(get("/portfolio"))
                .andExpect(status().isOk())
                .andExpect(view().name("position-inquiry"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void showPortfolioPositions_existingPortfolio_returnsPositions() throws Exception {
        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setPortfolioName("Test Portfolio");
        portfolio.setStatus("A");
        portfolio.setCurrencyCode("USD");

        InvestmentPosition position = new InvestmentPosition();
        InvestmentPositionKey key = new InvestmentPositionKey();
        key.setPortfolioId("PORT0001");
        key.setInvestmentId("INV0000001");
        key.setPositionDate(LocalDate.of(2024, 1, 1));
        position.setKey(key);
        position.setInvestmentName("US Large Cap Fund");
        position.setQuantity(new BigDecimal("100.0000"));
        position.setCostBasis(new BigDecimal("2500.00"));
        position.setMarketValue(new BigDecimal("2750.00"));
        position.setCurrencyCode("USD");
        position.setStatus("A");
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("TEST");

        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
        when(positionRepository.findByKeyPortfolioId("PORT0001"))
                .thenReturn(List.of(position));

        mockMvc.perform(get("/portfolio/PORT0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("position-inquiry"))
                .andExpect(model().attributeExists("portfolio"))
                .andExpect(model().attributeExists("positions"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void showPortfolioPositions_notFound_returnsError() throws Exception {
        when(portfolioRepository.findById("NOTFOUND")).thenReturn(Optional.empty());

        mockMvc.perform(get("/portfolio/NOTFOUND"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }
}
