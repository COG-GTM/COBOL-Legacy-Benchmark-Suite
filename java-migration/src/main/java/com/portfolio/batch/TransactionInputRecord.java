package com.portfolio.batch;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input record structure for Transaction Record migration
 * Maps to COBOL TRNREC.cpy copybook layout
 */
@Data
@NoArgsConstructor
public class TransactionInputRecord {

    private String transactionDate;
    private String transactionTime;
    private String portfolioId;
    private String sequenceNo;
    private String investmentId;
    private String transactionType;
    private String quantity;
    private String price;
    private String amount;
    private String currencyCode;
    private String status;
}
