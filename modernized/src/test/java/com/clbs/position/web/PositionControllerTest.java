package com.clbs.position.web;

import com.clbs.position.entity.Position;
import com.clbs.position.service.PositionUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the position query API (GET /positions, /positions/{id}).
 */
@WebMvcTest(PositionController.class)
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PositionUpdateService service;

    private Position sample() {
        return Position.builder()
                .id(1L)
                .portfolioId("PORT0001")
                .positionDate("20240617")
                .investmentId("SEC0000001")
                .quantity(new BigDecimal("1000.0000"))
                .costBasis(new BigDecimal("50000.00"))
                .marketValue(new BigDecimal("52000.00"))
                .currency("USD")
                .status("A")
                .build();
    }

    @Test
    @DisplayName("GET /positions returns the list with derived analytics")
    void listPositions() throws Exception {
        when(service.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].portfolioId").value("PORT0001"))
                .andExpect(jsonPath("$[0].averageCost").value(50.0))      // 50000 / 1000
                .andExpect(jsonPath("$[0].unrealizedGainLoss").value(2000.0)); // 52000 - 50000
    }

    @Test
    @DisplayName("GET /positions/{id} returns a single position")
    void getByIdFound() throws Exception {
        when(service.findById(eq(1L))).thenReturn(Optional.of(sample()));

        mockMvc.perform(get("/positions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investmentId").value("SEC0000001"));
    }

    @Test
    @DisplayName("GET /positions/{id} returns 404 when not found")
    void getByIdNotFound() throws Exception {
        when(service.findById(eq(99L))).thenReturn(Optional.empty());

        mockMvc.perform(get("/positions/99"))
                .andExpect(status().isNotFound());
    }
}
