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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void setup() {
        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioId("PORT001");
        portfolio.setAccountNo("ACCT001");
        portfolio.setTotalUnits(BigDecimal.ZERO);
        portfolio.setTotalCost(BigDecimal.ZERO);
        portfolioRepository.save(portfolio);
    }

    @Test
    void shouldProcessBuyTransaction() throws Exception {
        String request = """
            {
                "portfolioId": "PORT001",
                "transactionType": "BU",
                "quantity": 100,
                "price": 50.00,
                "amount": 5000.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSED"));

        Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
        assertEquals(new BigDecimal("100.0000"), updated.getTotalUnits());
        assertEquals(new BigDecimal("5000.00"), updated.getTotalCost());
    }

    @Test
    void shouldProcessSellTransaction() throws Exception {
        Portfolio portfolio = portfolioRepository.findById("PORT001").orElseThrow();
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000"));
        portfolioRepository.save(portfolio);

        String request = """
            {
                "portfolioId": "PORT001",
                "transactionType": "SL",
                "quantity": 50,
                "price": 100.00,
                "amount": 5000.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSED"));

        Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
        assertEquals(new BigDecimal("50.0000"), updated.getTotalUnits());
        assertEquals(new BigDecimal("5000.00"), updated.getTotalCost());
    }

    @Test
    void shouldRejectSellWithInsufficientUnits() throws Exception {
        String request = """
            {
                "portfolioId": "PORT001",
                "transactionType": "SL",
                "quantity": 100,
                "price": 100.00,
                "amount": 10000.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldProcessFeeTransaction() throws Exception {
        Portfolio portfolio = portfolioRepository.findById("PORT001").orElseThrow();
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000"));
        portfolioRepository.save(portfolio);

        String request = """
            {
                "portfolioId": "PORT001",
                "transactionType": "FE",
                "quantity": 1,
                "price": 50.00,
                "amount": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSED"));

        Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
        assertEquals(new BigDecimal("100.0000"), updated.getTotalUnits());
        assertEquals(new BigDecimal("9950.00"), updated.getTotalCost());
    }

    @Test
    void shouldRejectInvalidPortfolioId() throws Exception {
        String request = """
            {
                "portfolioId": "INVALID",
                "transactionType": "BU",
                "quantity": 100,
                "price": 50.00,
                "amount": 5000.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldRejectInvalidTransactionType() throws Exception {
        String request = """
            {
                "portfolioId": "PORT001",
                "transactionType": "XX",
                "quantity": 100,
                "price": 50.00,
                "amount": 5000.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldGetTransactionHistory() throws Exception {
        String request = """
            {
                "portfolioId": "PORT001",
                "transactionType": "BU",
                "quantity": 100,
                "price": 50.00,
                "amount": 5000.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/transactions/portfolio/PORT001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].portfolioId").value("PORT001"));
    }
}
