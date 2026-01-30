package com.portfolio.dto;

import com.portfolio.model.enums.PositionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionResponse {

    private Long id;
    private String portfolioId;
    private LocalDate positionDate;
    private String investmentId;
    private BigDecimal quantity;
    private BigDecimal costBasis;
    private BigDecimal marketValue;
    private String currency;
    private PositionStatus status;
    private LocalDateTime lastMaintDate;
    private String lastMaintUser;
}
