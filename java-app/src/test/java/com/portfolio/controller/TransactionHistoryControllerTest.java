package com.portfolio.controller;

import com.portfolio.model.TransactionHistory;
import com.portfolio.repository.TransactionHistoryRepository;
import com.portfolio.service.ErrorHandlingService;
import com.portfolio.service.SecurityManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Controller tests for TransactionHistoryController.
 * Uses MockMvc for testing (replacing CICS terminal testing).
 */
@WebMvcTest(TransactionHistoryController.class)
class TransactionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionHistoryRepository transactionRepository;

    @MockBean
    private SecurityManagerService securityManager;

    @MockBean
    private ErrorHandlingService errorHandlingService;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void showHistoryForm_returnsFormView() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction-history"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void showTransactionHistory_existingPortfolio_returnsHistory() throws Exception {
        TransactionHistory txn = new TransactionHistory();
        txn.setTransactionId("TXN00000001");
        txn.setPortfolioId("PORT0001");
        txn.setInvestmentId("INV0000001");
        txn.setTransactionType("BU");
        txn.setTransactionDate(LocalDate.of(2024, 1, 15));
        txn.setTransactionTime(LocalTime.of(10, 0, 0));
        txn.setQuantity(new BigDecimal("100.0000"));
        txn.setPrice(new BigDecimal("25.50"));
        txn.setAmount(new BigDecimal("2550.00"));
        txn.setCurrencyCode("USD");
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("TEST");
        txn.setStatus("P");

        when(transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(
                eq("PORT0001"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(txn)));

        mockMvc.perform(get("/history/PORT0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction-history"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attributeExists("portfolioId"));
    }
}
