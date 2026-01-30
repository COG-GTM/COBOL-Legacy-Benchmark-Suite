package com.portfolio.dto;

import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private Long id;
    private String portfolioId;
    private String accountNo;
    private String clientName;
    private ClientType clientType;
    private LocalDate createDate;
    private LocalDate lastMaintDate;
    private PortfolioStatus status;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private BigDecimal totalUnits;
    private BigDecimal totalCost;
    private String lastUser;
    private LocalDate lastTransDate;
}
