package com.portfolio.dto;

import com.portfolio.model.enums.PortfolioStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioUpdateRequest {

    private PortfolioStatus status;

    @Size(max = 30, message = "Client name must be at most 30 characters")
    private String clientName;

    private BigDecimal totalValue;

    @Size(max = 8, message = "User ID must be at most 8 characters")
    private String userId;
}
