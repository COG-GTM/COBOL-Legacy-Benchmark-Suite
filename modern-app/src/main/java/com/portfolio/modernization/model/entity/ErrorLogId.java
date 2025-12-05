package com.portfolio.modernization.model.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ErrorLogId implements Serializable {
    private LocalDateTime errorTimestamp;
    private String programId;
}
