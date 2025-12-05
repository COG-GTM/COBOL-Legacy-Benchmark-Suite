package com.portfolio.modernization.model.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BatchControlId implements Serializable {
    private String jobName;
    private String processDate;
    private Integer sequenceNumber;
}
