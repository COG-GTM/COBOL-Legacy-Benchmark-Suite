package com.portfolio.transaction.controller;

import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BatchTransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void setup() {
        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioId("PORT001");
        portfolio.setAccountNo("ACCT001");
        portfolio.setTotalUnits(new BigDecimal("1000"));
        portfolio.setTotalCost(new BigDecimal("100000"));
        portfolioRepository.save(portfolio);
    }

    @Test
    void shouldProcessBatchTransactions() throws Exception {
        String request = """
            [
                {
                    "portfolioId": "PORT001",
                    "transactionType": "BU",
                    "quantity": 100,
                    "price": 50.00,
                    "amount": 5000.00
                },
                {
                    "portfolioId": "PORT001",
                    "transactionType": "SL",
                    "quantity": 50,
                    "price": 100.00,
                    "amount": 5000.00
                }
            ]
            """;

        mockMvc.perform(post("/api/v1/batch/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRead").value(2))
            .andExpect(jsonPath("$.totalProcessed").value(2))
            .andExpect(jsonPath("$.totalErrors").value(0))
            .andExpect(jsonPath("$.terminatedEarly").value(false));
    }

    @Test
    void shouldCountErrorsInBatch() throws Exception {
        String request = """
            [
                {
                    "portfolioId": "PORT001",
                    "transactionType": "BU",
                    "quantity": 100,
                    "price": 50.00,
                    "amount": 5000.00
                },
                {
                    "portfolioId": "INVALID",
                    "transactionType": "BU",
                    "quantity": 100,
                    "price": 50.00,
                    "amount": 5000.00
                },
                {
                    "portfolioId": "PORT001",
                    "transactionType": "XX",
                    "quantity": 100,
                    "price": 50.00,
                    "amount": 5000.00
                }
            ]
            """;

        mockMvc.perform(post("/api/v1/batch/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRead").value(3))
            .andExpect(jsonPath("$.totalProcessed").value(1))
            .andExpect(jsonPath("$.totalErrors").value(2))
            .andExpect(jsonPath("$.terminatedEarly").value(false));
    }

    @Test
    void shouldReturnStatisticsMatchingCobolBehavior() throws Exception {
        String request = """
            [
                {
                    "portfolioId": "PORT001",
                    "transactionType": "BU",
                    "quantity": 100,
                    "price": 50.00,
                    "amount": 5000.00
                }
            ]
            """;

        mockMvc.perform(post("/api/v1/batch/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRead").exists())
            .andExpect(jsonPath("$.totalProcessed").exists())
            .andExpect(jsonPath("$.totalErrors").exists())
            .andExpect(jsonPath("$.results").isArray());
    }
}
