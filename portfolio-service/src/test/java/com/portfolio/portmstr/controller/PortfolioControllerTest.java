package com.portfolio.portmstr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.dto.PortfolioResponse;
import com.portfolio.portmstr.exception.DuplicatePortfolioException;
import com.portfolio.portmstr.exception.PortfolioNotFoundException;
import com.portfolio.portmstr.model.TransactionHistory;
import com.portfolio.portmstr.model.enums.TransactionStatus;
import com.portfolio.portmstr.model.enums.TransactionType;
import com.portfolio.portmstr.service.PortfolioMasterService;
import com.portfolio.portmstr.service.TransactionProcessingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for PortfolioController REST endpoints.
 * Validates HTTP routing matches COBOL EVALUATE TRUE command dispatch.
 */
@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioMasterService portfolioService;

    @MockBean
    private TransactionProcessingService transactionService;

    @MockBean
    private JobLauncher jobLauncher;

    @MockBean
    private Job portfolioProcessingJob;

    private PortfolioResponse makeResponse(String id) {
        return new PortfolioResponse(id, "1000000001", "John Doe", "I", "A",
                new BigDecimal("100000.00"), new BigDecimal("50000.00"), "USD",
                LocalDate.now(), LocalDate.now(), LocalDateTime.now(), "SYSTEM", 0, null);
    }

    @Nested
    @DisplayName("POST /api/portfolios (CREATE-PORT)")
    class CreateTests {

        @Test
        @DisplayName("Returns 201 on successful creation")
        void create_success() throws Exception {
            when(portfolioService.createPortfolio(any())).thenReturn(makeResponse("PORT0001"));

            PortfolioRequest request = new PortfolioRequest(
                    "PORT0001", "1000000001", "John Doe", "I", "A",
                    new BigDecimal("100000.00"), new BigDecimal("50000.00"), "USD");

            mockMvc.perform(post("/api/portfolios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.portfolioId").value("PORT0001"))
                    .andExpect(jsonPath("$.returnCode").value(0));
        }

        @Test
        @DisplayName("Returns 409 on duplicate key")
        void create_duplicate() throws Exception {
            when(portfolioService.createPortfolio(any()))
                    .thenThrow(new DuplicatePortfolioException("PORT0001"));

            PortfolioRequest request = new PortfolioRequest(
                    "PORT0001", "1000000001", "John Doe", "I", "A",
                    BigDecimal.ZERO, BigDecimal.ZERO, "USD");

            mockMvc.perform(post("/api/portfolios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.returnCode").value(8));
        }
    }

    @Nested
    @DisplayName("GET /api/portfolios/{id} (READ-PORT)")
    class ReadTests {

        @Test
        @DisplayName("Returns 200 with portfolio data")
        void read_success() throws Exception {
            when(portfolioService.readPortfolio("PORT0001")).thenReturn(makeResponse("PORT0001"));

            mockMvc.perform(get("/api/portfolios/PORT0001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.portfolioId").value("PORT0001"))
                    .andExpect(jsonPath("$.clientName").value("John Doe"));
        }

        @Test
        @DisplayName("Returns 404 for non-existent portfolio")
        void read_notFound() throws Exception {
            when(portfolioService.readPortfolio("PORT9999"))
                    .thenThrow(new PortfolioNotFoundException("PORT9999"));

            mockMvc.perform(get("/api/portfolios/PORT9999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.returnCode").value(4));
        }
    }

    @Nested
    @DisplayName("PUT /api/portfolios/{id} (UPDATE-PORT)")
    class UpdateTests {

        @Test
        @DisplayName("Returns 200 on successful update")
        void update_success() throws Exception {
            PortfolioResponse updated = new PortfolioResponse(
                    "PORT0001", "1000000001", "Jane Doe", "I", "A",
                    new BigDecimal("200000.00"), new BigDecimal("100000.00"), "EUR",
                    LocalDate.now(), LocalDate.now(), LocalDateTime.now(), "SYSTEM", 0, null);
            when(portfolioService.updatePortfolio(anyString(), any())).thenReturn(updated);

            PortfolioRequest request = new PortfolioRequest(
                    "PORT0001", "1000000001", "Jane Doe", "I", "A",
                    new BigDecimal("200000.00"), new BigDecimal("100000.00"), "EUR");

            mockMvc.perform(put("/api/portfolios/PORT0001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientName").value("Jane Doe"))
                    .andExpect(jsonPath("$.totalValue").value(200000.00));
        }
    }

    @Nested
    @DisplayName("DELETE /api/portfolios/{id} (DELETE-PORT)")
    class DeleteTests {

        @Test
        @DisplayName("Returns 200 on successful deletion")
        void delete_success() throws Exception {
            when(portfolioService.deletePortfolio("PORT0001")).thenReturn(makeResponse("PORT0001"));

            mockMvc.perform(delete("/api/portfolios/PORT0001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.portfolioId").value("PORT0001"));
        }
    }

    @Nested
    @DisplayName("GET /api/portfolios (LIST)")
    class ListTests {

        @Test
        @DisplayName("Returns list of all portfolios")
        void list_success() throws Exception {
            when(portfolioService.listPortfolios())
                    .thenReturn(List.of(makeResponse("PORT0001"), makeResponse("PORT0002")));

            mockMvc.perform(get("/api/portfolios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Returns active portfolios only")
        void listActive_success() throws Exception {
            when(portfolioService.listActivePortfolios())
                    .thenReturn(List.of(makeResponse("PORT0001")));

            mockMvc.perform(get("/api/portfolios/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
