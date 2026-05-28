package com.clbs.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for HistoryRecord.
 * From COBOL copybook: src/copybook/common/HISTREC.cpy (HIST-KEY).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HistoryRecordId implements Serializable {

    private String portfolioId;
    private String histDate;
    private String histTime;
    private String seqNo;
}
