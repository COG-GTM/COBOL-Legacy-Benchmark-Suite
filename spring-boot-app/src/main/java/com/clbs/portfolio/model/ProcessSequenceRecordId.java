package com.clbs.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for ProcessSequenceRecord.
 * From COBOL copybook: src/copybook/batch/PRCSEQ.cpy (PSR-KEY).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProcessSequenceRecordId implements Serializable {

    private String processId;
    private Integer version;
}
