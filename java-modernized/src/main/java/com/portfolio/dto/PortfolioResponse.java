package com.portfolio.dto;

import com.portfolio.model.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for portfolio read operations.
 * Maps the fields displayed in PORTREAD.cbl paragraph 2100-DISPLAY-RECORD.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private String portId;
    private String accountNo;
    private String clientName;
    private String clientType;
    private LocalDate createDate;
    private LocalDate lastMaintDate;
    private String status;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private String lastUser;
    private LocalDate lastTransDate;

    public static PortfolioResponse fromEntity(Portfolio entity) {
        PortfolioResponse response = new PortfolioResponse();
        response.setPortId(entity.getPortId());
        response.setAccountNo(entity.getAccountNo());
        response.setClientName(entity.getClientName());
        response.setClientType(entity.getClientType());
        response.setCreateDate(entity.getCreateDate());
        response.setLastMaintDate(entity.getLastMaintDate());
        response.setStatus(entity.getStatus());
        response.setTotalValue(entity.getTotalValue());
        response.setCashBalance(entity.getCashBalance());
        response.setLastUser(entity.getLastUser());
        response.setLastTransDate(entity.getLastTransDate());
        return response;
    }
}
