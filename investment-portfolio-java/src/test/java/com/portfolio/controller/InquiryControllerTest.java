package com.portfolio.controller;

import com.portfolio.config.SecurityConfig;
import com.portfolio.dto.PortfolioPositionResponse;
import com.portfolio.dto.TransactionHistoryResponse;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.service.InquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InquiryController.class)
@Import(SecurityConfig.class)
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InquiryService inquiryService;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getMenu_returns200() throws Exception {
        mockMvc.perform(get("/api/inquiry/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Portfolio Management System"))
                .andExpect(jsonPath("$.options").isArray());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getPortfolioPositions_returns200() throws Exception {
        List<PortfolioPositionResponse> positions = List.of(
                new PortfolioPositionResponse("ACC0000001", "AAPL", "AAPL",
                        BigDecimal.valueOf(100), BigDecimal.valueOf(15000),
                        BigDecimal.valueOf(17500)));
        when(inquiryService.getPortfolioPosition("ACC0000001")).thenReturn(positions);

        mockMvc.perform(get("/api/inquiry/portfolio/ACC0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fundId").value("AAPL"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getPortfolioPositions_notFound_returns404() throws Exception {
        when(inquiryService.getPortfolioPosition("ACC9999999"))
                .thenThrow(new PortfolioNotFoundException("ACC9999999"));

        mockMvc.perform(get("/api/inquiry/portfolio/ACC9999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getTransactionHistory_returns200() throws Exception {
        List<TransactionHistoryResponse> list = List.of(
                new TransactionHistoryResponse(LocalDate.now(), "BU",
                        BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                        BigDecimal.valueOf(15000)));
        Page<TransactionHistoryResponse> page = new PageImpl<>(list, PageRequest.of(0, 10), 1);
        when(inquiryService.getTransactionHistory(eq("ACC0000001"), any())).thenReturn(page);

        mockMvc.perform(get("/api/inquiry/history/ACC0000001")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("BU"));
    }
}
