package com.clbs.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for TransactionRecord.
 * From COBOL copybook: src/copybook/common/TRNREC.cpy (TRN-KEY).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TransactionRecordId implements Serializable {

    private String trnDate;
    private String trnTime;
    private String portfolioId;
    private String sequenceNo;
}
