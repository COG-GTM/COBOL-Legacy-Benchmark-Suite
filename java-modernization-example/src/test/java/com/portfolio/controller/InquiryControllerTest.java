package com.portfolio.controller;

import com.portfolio.exception.GlobalExceptionHandler;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.Portfolio;
import com.portfolio.model.PortfolioKey;
import com.portfolio.model.PositionHistory;
import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import com.portfolio.service.PortfolioInquiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for InquiryController — the REST API that replaces INQONLN.cbl.
 *
 * These tests verify that:
 * - Portfolio inquiry returns 200 with valid data (mirrors successful VSAM read)
 * - Portfolio inquiry returns 404 for non-existent portfolio (mirrors VSAM NOTFND / status 23)
 * - History inquiry with date range parameters works correctly
 * - Validation errors return 400 (mirrors ERR-CAT-VALID handling)
 * - Menu endpoint returns available options
 */
@WebMvcTest(InquiryController.class)
@Import(GlobalExceptionHandler.class)
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioInquiryService inquiryService;

    @Test
    @DisplayName("GET /api/inquiry/menu - returns menu options (replaces P200-DISPLAY-MENU)")
    void getMenu_returnsMenuOptions() throws Exception {
        mockMvc.perform(get("/api/inquiry/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Portfolio Inquiry System")))
                .andExpect(jsonPath("$.options", hasSize(3)))
                .andExpect(jsonPath("$.options[0].code", is("INQP")))
                .andExpect(jsonPath("$.options[1].code", is("INQH")))
                .andExpect(jsonPath("$.options[2].code", is("EXIT")));
    }

    @Test
    @DisplayName("GET /api/inquiry/portfolio/{id} - returns 200 with portfolio data (replaces P300-PORTFOLIO-INQUIRY)")
    void getPortfolio_returnsPortfolioData() throws Exception {
        Portfolio portfolio = createTestPortfolio("PORT0001", "ACCT000001", "SMITH JOHN R");

        when(inquiryService.getPortfolio("PORT0001"))
                .thenReturn(List.of(portfolio));

        mockMvc.perform(get("/api/inquiry/portfolio/PORT0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].portfolioId", is("PORT0001")))
                .andExpect(jsonPath("$[0].accountNo", is("ACCT000001")))
                .andExpect(jsonPath("$[0].clientName", is("SMITH JOHN R")))
                .andExpect(jsonPath("$[0].clientType", is("INDIVIDUAL")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/inquiry/portfolio/{id} - returns 404 for non-existent portfolio (mirrors VSAM NOTFND status 23)")
    void getPortfolio_notFound_returns404() throws Exception {
        when(inquiryService.getPortfolio("NOTEXIST"))
                .thenThrow(new PortfolioNotFoundException("NOTEXIST"));

        mockMvc.perform(get("/api/inquiry/portfolio/NOTEXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.program", is("INQONLN")))
                .andExpect(jsonPath("$.category", is("VS")))
                .andExpect(jsonPath("$.code", is("0023")))
                .andExpect(jsonPath("$.severity", is("WARNING")));
    }

    @Test
    @DisplayName("GET /api/inquiry/history/{id} - returns history records (replaces P400-HISTORY-INQUIRY)")
    void getHistory_returnsHistoryRecords() throws Exception {
        PositionHistory record = createTestPositionHistory();

        when(inquiryService.getPortfolioHistory(
                eq("PORT0001"),
                eq(LocalDate.of(2024, 3, 1)),
                eq(LocalDate.of(2024, 3, 31))))
                .thenReturn(List.of(record));

        mockMvc.perform(get("/api/inquiry/history/PORT0001")
                        .param("from", "2024-03-01")
                        .param("to", "2024-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].portfolioId", is("PORT0001")))
                .andExpect(jsonPath("$[0].securityId", is("AAPL")))
                .andExpect(jsonPath("$[0].transType", is("BU")));
    }

    @Test
    @DisplayName("GET /api/inquiry/history/{id} - returns 404 for non-existent portfolio")
    void getHistory_notFound_returns404() throws Exception {
        when(inquiryService.getPortfolioHistory(
                eq("NOTEXIST"), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new PortfolioNotFoundException("NOTEXIST"));

        mockMvc.perform(get("/api/inquiry/history/NOTEXIST")
                        .param("from", "2024-03-01")
                        .param("to", "2024-03-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("0023")));
    }

    @Test
    @DisplayName("GET /api/inquiry/history/{id} - returns 400 for invalid date format")
    void getHistory_invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/inquiry/history/PORT0001")
                        .param("from", "not-a-date")
                        .param("to", "2024-03-31"))
                .andExpect(status().isBadRequest());
    }

    // --- Helper methods ---

    private Portfolio createTestPortfolio(String portfolioId, String accountNo, String clientName) {
        Portfolio portfolio = new Portfolio();
        portfolio.setKey(new PortfolioKey(portfolioId, accountNo));
        portfolio.setClientName(clientName);
        portfolio.setClientType(ClientType.INDIVIDUAL);
        portfolio.setCreateDate(LocalDate.of(2024, 1, 15));
        portfolio.setLastMaintDate(LocalDate.of(2024, 3, 20));
        portfolio.setStatus(PortfolioStatus.ACTIVE);
        portfolio.setTotalValue(new BigDecimal("1250000.50"));
        portfolio.setCashBalance(new BigDecimal("75000.00"));
        portfolio.setLastUser("ADMIN01");
        portfolio.setLastTransDate(LocalDate.of(2024, 3, 20));
        return portfolio;
    }

    private PositionHistory createTestPositionHistory() {
        PositionHistory record = new PositionHistory();
        record.setAccountNo("ACCT0001");
        record.setPortfolioId("PORT0001");
        record.setTransDate(LocalDate.of(2024, 3, 20));
        record.setTransTime(LocalTime.of(10, 30, 0));
        record.setTransType("BU");
        record.setSecurityId("AAPL");
        record.setQuantity(new BigDecimal("100.000"));
        record.setPrice(new BigDecimal("175.500"));
        record.setAmount(new BigDecimal("17550.00"));
        record.setFees(new BigDecimal("9.99"));
        record.setTotalAmount(new BigDecimal("17559.99"));
        record.setCostBasis(new BigDecimal("17559.99"));
        record.setGainLoss(new BigDecimal("0.00"));
        record.setProcessDate(LocalDate.of(2024, 3, 20));
        record.setProcessTime(LocalTime.of(10, 31, 0));
        record.setProgramId("HISTLD00");
        record.setUserId("BATCH01");
        record.setAuditTimestamp(Instant.parse("2024-03-20T10:31:00Z"));
        return record;
    }
}
