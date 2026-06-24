package com.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.dto.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for TransactionController REST endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProcessBuyTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT0001");
        request.setInvestmentId("AMZN");
        request.setTransactionType("BU");
        request.setQuantity(new BigDecimal("50.0000"));
        request.setPrice(new BigDecimal("180.0000"));
        request.setAmount(new BigDecimal("9000.00"));
        request.setCurrency("USD");

        mockMvc.perform(post("/api/portfolio/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("BU"))
                .andExpect(jsonPath("$.status").value("D"));
    }

    @Test
    void testProcessTransactionInvalidPortfolio() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT9999");
        request.setInvestmentId("AAPL");
        request.setTransactionType("BU");
        request.setQuantity(new BigDecimal("10.0000"));
        request.setPrice(new BigDecimal("100.0000"));
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("USD");

        mockMvc.perform(post("/api/portfolio/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTransactionsForPortfolio() throws Exception {
        mockMvc.perform(get("/api/portfolio/PORT0001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }
}
