package com.portfolio.batch;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input record structure for Portfolio Master migration
 * Maps to COBOL PORTFLIO.cpy copybook layout
 */
@Data
@NoArgsConstructor
public class PortfolioInputRecord {

    private String portfolioId;
    private String accountNo;
    private String clientName;
    private String clientType;
    private String createDate;
    private String lastMaint;
    private String status;
    private String totalValue;
    private String cashBalance;
    private String lastUser;
    private String lastTrans;
}
