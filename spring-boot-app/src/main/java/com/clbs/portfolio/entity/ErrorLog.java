package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_timestamp", nullable = false)
    private LocalDateTime errorTimestamp;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "error_type", length = 1, nullable = false)
    private String errorType;

    @Column(name = "error_severity", nullable = false)
    private Integer errorSeverity;

    @Column(name = "error_code", length = 8, nullable = false)
    private String errorCode;

    @Column(name = "error_message", length = 200, nullable = false)
    private String errorMessage;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Column(name = "user_id", length = 8)
    private String userId;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;
}
