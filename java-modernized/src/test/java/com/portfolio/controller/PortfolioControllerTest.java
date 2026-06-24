package com.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.dto.PortfolioRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for PortfolioController REST endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testListAllPortfolios() throws Exception {
        mockMvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    void testGetPortfolioById() throws Exception {
        mockMvc.perform(get("/api/portfolio/PORT0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portId").value("PORT0001"))
                .andExpect(jsonPath("$.clientName").value("Acme Corporation"))
                .andExpect(jsonPath("$.totalValue").value(1250000.00));
    }

    @Test
    void testGetPortfolioNotFound() throws Exception {
        mockMvc.perform(get("/api/portfolio/PORT9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("E002"));
    }

    @Test
    void testCreatePortfolio() throws Exception {
        PortfolioRequest request = new PortfolioRequest();
        request.setPortId("PORT0050");
        request.setAccountNo("5050505050");
        request.setClientName("Controller Test");
        request.setClientType("I");
        request.setStatus("A");
        request.setTotalValue(new BigDecimal("100000.00"));
        request.setCashBalance(new BigDecimal("10000.00"));

        mockMvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portId").value("PORT0050"));
    }

    @Test
    void testCreatePortfolioDuplicate() throws Exception {
        PortfolioRequest request = new PortfolioRequest();
        request.setPortId("PORT0001");
        request.setAccountNo("1000000001");
        request.setClientName("Duplicate");
        request.setClientType("I");
        request.setStatus("A");
        request.setTotalValue(BigDecimal.ZERO);
        request.setCashBalance(BigDecimal.ZERO);

        mockMvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("E003"));
    }

    @Test
    void testCreatePortfolioInvalidId() throws Exception {
        PortfolioRequest request = new PortfolioRequest();
        request.setPortId("BADID001");
        request.setAccountNo("1234567890");
        request.setClientName("Bad ID");
        request.setClientType("I");
        request.setStatus("A");
        request.setTotalValue(BigDecimal.ZERO);
        request.setCashBalance(BigDecimal.ZERO);

        mockMvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("E008"));
    }

    @Test
    void testUpdatePortfolio() throws Exception {
        PortfolioRequest request = new PortfolioRequest();
        request.setPortId("PORT0001");
        request.setAccountNo("1000000001");
        request.setClientName("Acme Updated");
        request.setClientType("C");
        request.setStatus("A");
        request.setTotalValue(new BigDecimal("2000000.00"));
        request.setCashBalance(new BigDecimal("200000.00"));

        mockMvc.perform(put("/api/portfolio/PORT0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Acme Updated"))
                .andExpect(jsonPath("$.totalValue").value(2000000.00));
    }

    @Test
    void testDeletePortfolio() throws Exception {
        mockMvc.perform(delete("/api/portfolio/PORT0007"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/portfolio/PORT0007"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListPortfoliosByStatus() throws Exception {
        mockMvc.perform(get("/api/portfolio").param("status", "S"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].portId").value("PORT0005"));
    }
}
