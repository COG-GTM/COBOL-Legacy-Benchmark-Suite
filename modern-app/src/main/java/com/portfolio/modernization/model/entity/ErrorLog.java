package com.portfolio.modernization.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "error_log", indexes = {
    @Index(name = "idx_errlog_process", columnList = "process_date, error_severity")
})
@IdClass(ErrorLogId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog {

    @Id
    @Column(name = "error_timestamp")
    private LocalDateTime errorTimestamp;

    @Id
    @Column(name = "program_id", length = 8)
    private String programId;

    @Column(name = "error_type", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    @Column(name = "error_severity", nullable = false)
    private Integer errorSeverity;

    @Column(name = "error_code", length = 8, nullable = false)
    private String errorCode;

    @Column(name = "error_message", length = 200, nullable = false)
    private String errorMessage;

    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    public enum ErrorType {
        S, // System
        A, // Application
        D  // Data
    }
}
