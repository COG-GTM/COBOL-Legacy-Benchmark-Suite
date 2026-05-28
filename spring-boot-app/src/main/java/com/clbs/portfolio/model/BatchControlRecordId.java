package com.clbs.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for BatchControlRecord.
 * From COBOL copybook: src/copybook/batch/BCHCTL.cpy (BCT-KEY).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BatchControlRecordId implements Serializable {

    private String jobName;
    private String processDate;
    private Integer sequenceNo;
}
